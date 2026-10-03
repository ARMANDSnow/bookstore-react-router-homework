package com.homework.bookstore.service.impl;

import com.homework.bookstore.dto.CustomerBookStatDto;
import com.homework.bookstore.dto.CustomerStatsDto;
import com.homework.bookstore.dto.OrderDto;
import com.homework.bookstore.dto.SalesRankDto;
import com.homework.bookstore.dto.UserSpendRankDto;
import com.homework.bookstore.entity.Book;
import com.homework.bookstore.entity.CartItem;
import com.homework.bookstore.entity.Order;
import com.homework.bookstore.entity.OrderItem;
import com.homework.bookstore.entity.OrderStatus;
import com.homework.bookstore.entity.User;
import com.homework.bookstore.repository.CartItemRepository;
import com.homework.bookstore.repository.OrderRepository;
import com.homework.bookstore.repository.UserRepository;
import com.homework.bookstore.service.OrderService;
import com.homework.bookstore.service.exception.BusinessException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * <h2>OrderServiceImpl —— 整个项目最有代表性的类</h2>
 *
 * <p>{@link #placeOrder} 是答辩里讲事务、讲链路、讲分层都必提的核心方法。
 *
 * <h3>构造器注入（Constructor Injection）</h3>
 * 三个依赖（{@code OrderRepository}、{@code CartItemRepository}、{@code UserRepository}）
 * 通过构造器传入，全部声明为 {@code final}。这是 Spring 推荐的做法：
 * <ul>
 *   <li>依赖不可变</li>
 *   <li>启动时强制满足，避免运行时 NPE</li>
 *   <li>单测时手动 new 即可，不依赖反射</li>
 * </ul>
 * 单构造器的类可以省略 {@code @Autowired}。
 *
 * <h3>事务（Transaction）原理</h3>
 * {@code @Transactional} 基于 AOP + JDK 动态代理：Spring 启动时给这个类生成一个代理对象。
 * 外部调用 {@link #placeOrder} 时实际走到代理，代理在方法前后插入：
 * <pre>
 *   connection.setAutoCommit(false);
 *   try { 执行方法体; connection.commit(); }
 *   catch (RuntimeException e) { connection.rollback(); throw e; }
 *   finally { connection.close(); }   // 还连接池
 * </pre>
 *
 * <h3>事务的一个常见陷阱</h3>
 * <b>同类内部方法互相调用 {@code @Transactional} 不生效</b> ——
 * 因为没经过代理。本类 {@code placeOrder} 是 Controller 从外部调进来的，所以正常。
 */
@Service  // 标记为 Spring 容器管理的 Bean，启动时自动实例化
public class OrderServiceImpl implements OrderService {

    private static final BigDecimal FREE_SHIPPING_THRESHOLD = new BigDecimal("99.00");
    private static final BigDecimal SHIPPING_FEE = new BigDecimal("12.00");

    // 三个依赖都用 final，由构造器一次性注入，之后不可变
    private final OrderRepository orderRepository;       // 操作 orders 表
    private final CartItemRepository cartItemRepository; // 操作 cart_items 表（下单要清空）
    private final UserRepository userRepository;         // 校验用户是否存在

    // 构造器注入：Spring 启动时自动把三个 Repository 代理对象传进来
    // 单构造器可省略 @Autowired 注解
    public OrderServiceImpl(OrderRepository orderRepository,
                            CartItemRepository cartItemRepository,
                            UserRepository userRepository) {
        this.orderRepository = orderRepository;
        this.cartItemRepository = cartItemRepository;
        this.userRepository = userRepository;
    }

    /**
     * 下单 —— 整个项目最核心的事务方法。
     *
     * <h3>事务边界</h3>
     * {@code @Transactional} 圈住整段：方法正常返回则提交，抛任何 RuntimeException 则全部回滚。
     *
     * <h3>SQL 序列（开 {@code logging.level.org.hibernate.SQL=DEBUG} 后能在日志里看到）</h3>
     * <ol>
     *   <li>{@code select * from users where id=?}                           —— 校验用户存在</li>
     *   <li>{@code select * from cart_items where user_id=? order by ...}    —— 取购物车</li>
     *   <li>(对每件商品) {@code select * from books where id=?}              —— 拿书籍信息（含价格快照）</li>
     *   <li>{@code insert into orders (...) values (...)}                    —— 写主订单</li>
     *   <li>(对每件商品) {@code insert into order_items (...) values (...)}  —— 级联写明细（Cascade.ALL）</li>
     *   <li>{@code delete from cart_items where user_id=?}                   —— 清空购物车（{@code @Modifying @Query}）</li>
     * </ol>
     *
     * <h3>关键设计</h3>
     * <ul>
     *   <li><b>订单金额：</b>商品小计大于零且不足 99 元时加 12 元运费，满 99 元或零元免运费；
     *       商品单价快照不包含运费，历史订单按已保存的总额读取</li>
     *   <li><b>价格快照：</b>{@code OrderItem} 单独存 {@code bookTitle / bookImage / unitPrice}，
     *       即便日后书改名/改价，历史订单不受影响</li>
     *   <li><b>级联：</b>{@code Order.items} 上 {@code Cascade.ALL}，
     *       保存 Order 时自动 INSERT 所有 OrderItem，无需手动 save</li>
     *   <li><b>异常即回滚：</b>购物车为空、用户不存在都抛 {@code BusinessException}（继承 RuntimeException），
     *       会触发事务回滚</li>
     * </ul>
     *
     * @param userId 下单用户 ID
     * @return 包含订单 ID、总额、明细列表的 DTO
     * @throws com.homework.bookstore.service.exception.BusinessException 用户不存在 (40401) 或购物车为空 (40001)
     */
    @Override
    @Transactional  // ★ 关键：整段被事务包裹，任何异常都会让前面所有 SQL 全部回滚
    public OrderDto placeOrder(Long userId) {
        // 【步骤 1】查询用户是否存在；不存在抛业务异常（→ 40401）
        // 对应 SQL: select * from users where id = ?
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(40401, "用户不存在"));

        // 【步骤 2】查这个用户的购物车明细；空购物车直接报错
        // 对应 SQL: select * from cart_items where user_id = ? order by created_at asc
        List<CartItem> cartItems = cartItemRepository.findByUser_IdOrderByCreatedAtAsc(userId);
        if (cartItems.isEmpty()) {
            throw new BusinessException(40001, "购物车为空，无法下单");
        }

        // 【步骤 3】构造主订单对象（还没写库）
        Order order = new Order();
        order.setUser(user);
        order.setStatus(OrderStatus.PAID);  // 简化：下单即支付完成

        // 【步骤 4】遍历购物车，把每一项"翻译"成订单明细，并累加商品小计
        BigDecimal subtotal = BigDecimal.ZERO;
        for (CartItem cartItem : cartItems) {
            Book book = cartItem.getBook();
            ensureEnoughStock(book, cartItem.getQuantity());
            if (book.getStock() != null) {
                book.setStock(book.getStock() - cartItem.getQuantity());
            }

            OrderItem oi = new OrderItem();
            oi.setBook(book);

            // ★★ 价格快照：把书的当前标题/图片/单价"复印"到订单明细
            // 这样将来书改名/涨价，历史订单仍保留下单那一刻的数据
            oi.setBookTitle(book.getTitle());
            oi.setBookImage(book.getImage());
            oi.setUnitPrice(book.getPrice());
            oi.setQuantity(cartItem.getQuantity());

            // 双向关联：把明细加进订单，同时设置反向指针（item.order = this）
            order.addItem(oi);

            // 累加：单价 × 数量，BigDecimal 不能用 + 号
            subtotal = subtotal.add(book.getPrice()
                    .multiply(BigDecimal.valueOf(cartItem.getQuantity())));
        }
        // 按整单商品小计计算一次运费，订单总额包含运费。
        BigDecimal shipping = subtotal.signum() > 0
                && subtotal.compareTo(FREE_SHIPPING_THRESHOLD) < 0
                ? SHIPPING_FEE : BigDecimal.ZERO;
        order.setTotalAmount(subtotal.add(shipping));

        // 【步骤 5】保存订单 —— 一句 save 触发 N+1 条 INSERT
        // 因为 Order.items 上配了 Cascade.ALL，所有 OrderItem 会被级联 INSERT
        // 对应 SQL: insert into orders (...) + N×insert into order_items (...)
        Order saved = orderRepository.save(order);

        // 【步骤 6】清空购物车 —— @Modifying @Query 一句 SQL 批量删除
        // 对应 SQL: delete from cart_items where user_id = ?
        cartItemRepository.deleteByUserId(userId);

        // 【步骤 7】把实体转 DTO 返给 Controller（不直接暴露 JPA 实体）
        return OrderDto.from(saved);
        // 方法到这里正常返回 → 事务提交 → 所有写入对外可见
    }

    @Override
    @Transactional(readOnly = true)  // 只读事务：Hibernate 跳过脏检查，性能更好
    public List<OrderDto> listOrders(Long userId) {
        return listOrders(userId, null, null, null);
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderDto> listOrders(Long userId, LocalDateTime startAt, LocalDateTime endAt, String bookName) {
        // 先校验用户存在，避免拿一个不存在的 userId 查出空列表
        if (userId != null && !userRepository.existsById(userId)) {
            throw new BusinessException(40401, "用户不存在");
        }
        return orderRepository.findWithFilters(userId, startAt, endAt, normalize(bookName))
                .stream()
                .map(OrderDto::from)  // 每个实体转 DTO
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<OrderDto> getOrder(Long orderId) {
        // Optional.map：找到了就转 DTO，找不到返回 Optional.empty()
        return orderRepository.findById(orderId).map(OrderDto::from);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SalesRankDto> listSalesRank(LocalDateTime startAt, LocalDateTime endAt) {
        Map<String, BookAccumulator> map = new HashMap<>();
        for (Order order : orderRepository.findWithFilters(null, startAt, endAt, null)) {
            for (OrderItem item : order.getItems()) {
                String key = item.getBook().getId();
                map.computeIfAbsent(key, k -> new BookAccumulator(key, item.getBookTitle())).add(item);
            }
        }
        return map.values().stream()
                .sorted(Comparator
                        .comparingInt(BookAccumulator::getQuantity).reversed()
                        .thenComparing(BookAccumulator::getTotalAmount, Comparator.reverseOrder()))
                .map(acc -> new SalesRankDto(acc.bookId, acc.bookTitle, acc.quantity, acc.totalAmount))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserSpendRankDto> listUserSpendRank(LocalDateTime startAt, LocalDateTime endAt) {
        Map<Long, UserSpendAccumulator> map = new HashMap<>();
        for (Order order : orderRepository.findWithFilters(null, startAt, endAt, null)) {
            Long key = order.getUser().getId();
            map.computeIfAbsent(key, k -> new UserSpendAccumulator(key, order.getUser().getUsername())).add(order);
        }
        return map.values().stream()
                .sorted(Comparator
                        .comparing(UserSpendAccumulator::getTotalAmount, Comparator.reverseOrder())
                        .thenComparing(UserSpendAccumulator::getOrderCount, Comparator.reverseOrder()))
                .map(acc -> new UserSpendRankDto(acc.userId, acc.username, acc.orderCount, acc.totalAmount))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public CustomerStatsDto getCustomerStats(Long userId, LocalDateTime startAt, LocalDateTime endAt) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(40401, "用户不存在"));
        Map<String, BookAccumulator> map = new HashMap<>();
        int totalBooks = 0;
        BigDecimal totalAmount = BigDecimal.ZERO;
        for (Order order : orderRepository.findWithFilters(userId, startAt, endAt, null)) {
            totalAmount = totalAmount.add(order.getTotalAmount());
            for (OrderItem item : order.getItems()) {
                totalBooks += item.getQuantity();
                String key = item.getBook().getId();
                map.computeIfAbsent(key, k -> new BookAccumulator(key, item.getBookTitle())).add(item);
            }
        }
        List<CustomerBookStatDto> books = map.values().stream()
                .sorted(Comparator
                        .comparingInt(BookAccumulator::getQuantity).reversed()
                        .thenComparing(BookAccumulator::getTotalAmount, Comparator.reverseOrder()))
                .map(acc -> new CustomerBookStatDto(acc.bookId, acc.bookTitle, acc.quantity, acc.totalAmount))
                .collect(Collectors.toList());
        return new CustomerStatsDto(user.getId(), user.getUsername(), totalBooks, totalAmount, books);
    }

    private void ensureEnoughStock(Book book, int quantity) {
        Integer stock = book.getStock();
        if (stock != null && stock < quantity) {
            throw new BusinessException(40004, "库存不足：" + book.getTitle());
        }
    }

    private String normalize(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private static class BookAccumulator {
        private final String bookId;
        private final String bookTitle;
        private int quantity;
        private BigDecimal totalAmount = BigDecimal.ZERO;

        private BookAccumulator(String bookId, String bookTitle) {
            this.bookId = bookId;
            this.bookTitle = bookTitle;
        }

        private void add(OrderItem item) {
            quantity += item.getQuantity();
            totalAmount = totalAmount.add(item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity())));
        }

        private int getQuantity() {
            return quantity;
        }

        private BigDecimal getTotalAmount() {
            return totalAmount;
        }
    }

    private static class UserSpendAccumulator {
        private final Long userId;
        private final String username;
        private int orderCount;
        private BigDecimal totalAmount = BigDecimal.ZERO;

        private UserSpendAccumulator(Long userId, String username) {
            this.userId = userId;
            this.username = username;
        }

        private void add(Order order) {
            orderCount++;
            totalAmount = totalAmount.add(order.getTotalAmount());
        }

        private int getOrderCount() {
            return orderCount;
        }

        private BigDecimal getTotalAmount() {
            return totalAmount;
        }
    }
}

package com.homework.bookstore.service;

import com.homework.bookstore.dto.CartItemDto;
import java.util.List;

/**
 * 购物车业务接口。
 *
 * <p>Controller 只调用本接口，不知道购物车数据最终是存在 MySQL、Redis 还是远程服务里。
 * 当前实现类 {@link com.homework.bookstore.service.impl.CartServiceImpl} 使用 JPA 操作
 * {@code cart_items} 表，并负责"同一本书重复加入时数量累加"这类业务规则。
 */
public interface CartService {

    /** 查询某用户购物车，按加入时间升序返回；用户不存在时抛业务异常。 */
    List<CartItemDto> listCart(Long userId);

    /**
     * 加入购物车。
     *
     * <p>如果该用户已加入过同一本书，则更新数量；否则插入新行。
     * 这个语义属于业务规则，所以放在 Service 层，而不是 Controller 或 Repository 层。
     */
    CartItemDto addToCart(Long userId, String bookId, int quantity);

    /** 修改购物车条目数量，数量下限由请求 DTO 的 {@code @Min(1)} 和前端控件共同保证。 */
    CartItemDto updateQuantity(Long itemId, int quantity);

    /** 删除单个购物车条目。 */
    void removeItem(Long itemId);

    /** 清空某用户购物车；下单成功后 {@code OrderServiceImpl.placeOrder} 也会调用同一条逻辑。 */
    void clearCart(Long userId);
}

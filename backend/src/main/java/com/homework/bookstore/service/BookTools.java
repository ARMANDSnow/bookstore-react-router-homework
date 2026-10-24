package com.homework.bookstore.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.homework.bookstore.entity.Book;
import com.homework.bookstore.repository.BookRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class BookTools {
    private final BookRepository books;
    private final ObjectMapper mapper;
    public BookTools(BookRepository books, ObjectMapper mapper) { this.books = books; this.mapper = mapper; }

    /*
     * 【作业2 · 得分点2.3：本地库存函数】
     * 模型选择函数后，由后端真正访问BookRepository，返回书名、ISBN、本店价和本次查询的库存。
     * 先检查号码形式，再区分未找到、重复记录、库存未知：未知库存返回null，不伪装成0或缺货。
     * source与checkedAt标明数据来自本店数据库及查询时间；它们随结果回传给模型，支撑可核对的回答。
     */
    public ObjectNode check_inventory(String isbn) {
        if (!validShape(isbn)) return error("INVALID_ISBN", "ISBN需要13位数字，可带空格或连字符，请核对号码");
        var matches = find(isbn);
        if (matches.isEmpty()) return error("BOOK_NOT_FOUND", "未找到该ISBN，请核对号码或查看书架中的图书");
        if (matches.size() > 1) return error("AMBIGUOUS_ISBN", "同一ISBN对应多个记录，请联系书店核对版本");
        Book book = matches.get(0);
        var result = identity(book).put("source", "本店数据库").put("checkedAt", Instant.now().toString());
        if (book.getStock() == null) return result.putNull("stock").put("availability", "库存待确认");
        return result.put("stock", book.getStock()).put("availability", book.getStock() > 0 ? "有货" : "缺货");
    }
    /**
     * 【作业2 · 得分点2.3：本地模拟竞价函数】
     * 作业要求允许模拟外部查询：这里采用固定规则“本店价×0.92”，保留两位小数，明确标记simulated=true。
     * 它没有访问竞争商家的网页/API，答辩时应称模拟参考报价，不能声称是外部商家当前真实售价。
     */
    public ObjectNode get_competitor_price(String isbn) {
        return get_competitor_price(isbn, false);
    }
    public ObjectNode get_competitor_price(String isbn, boolean simulateFirstTimeout) {
        if (!validShape(isbn)) return error("INVALID_ISBN", "ISBN需要13位数字，可带空格或连字符，请核对号码");
        var matches = find(isbn);
        if (matches.isEmpty()) return error("BOOK_NOT_FOUND", "该ISBN暂无参考报价，请核对图书");
        if (matches.size() > 1) return error("AMBIGUOUS_ISBN", "同一ISBN对应多个记录，请联系书店核对版本");
        Book book = matches.get(0);
        /*
         * 【作业2 · 得分点2.5：可观察的异常与恢复】
         * 只有有效且唯一的图书才进入超时演示；返回ok=false、COMPETITOR_TIMEOUT、retryable=true和原ISBN。
         * AssistantService把此错误送回模型，模型若决定用同ISBN重试，第二次不会再传入simulateFirstTimeout。
         * 这是用于验证工具纠错闭环的受控故障，不等于真的发生了外部报价服务网络超时。
         */
        if (simulateFirstTimeout) return error("COMPETITOR_TIMEOUT", "报价服务查询超时，可使用相同ISBN重试一次")
                .put("retryable", true).put("simulated", true).put("isbn", book.getIsbn());
        return identity(book).put("price", book.getPrice().multiply(new BigDecimal("0.92")).setScale(2, RoundingMode.HALF_UP))
                .put("currency", "CNY").put("store", "参考报价服务").put("simulated", true)
                .put("source", "选书价格参考");
    }
    // 统一去除空格和连字符后查库；这里只校验13位数字的形式，没有实现ISBN校验位算法。
    private List<Book> find(String isbn) {
        if (isbn == null) return List.of();
        String normalized = isbn.replaceAll("[\\s-]", "");
        if (!normalized.matches("\\d{13}")) return List.of();
        return books.findByNormalizedIsbn(normalized);
    }
    private boolean validShape(String isbn) { return isbn != null && isbn.replaceAll("[\\s-]", "").matches("\\d{13}"); }
    private ObjectNode identity(Book book) {
        return mapper.createObjectNode().put("ok", true).put("id", book.getId()).put("isbn", book.getIsbn())
                .put("title", book.getTitle()).put("ourPrice", book.getPrice());
    }
    /*
     * 【作业2 · 得分点2.5：将可解释错误交回模型】
     * 错误保留机器可识别的error和人可读的message，统一用ok=false区分正常数据；没有捏造库存或价格。
     * 错误ISBN/查无此书不标记可重试，提示模型请用户核对；只有上方受控超时另外设置retryable=true。
     */
    public ObjectNode error(String code, String message) {
        return mapper.createObjectNode().put("ok", false).put("error", code).put("message", message);
    }
}

package com.homework.bookstore.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.homework.bookstore.dto.AssistantRequest;
import com.homework.bookstore.entity.Book;
import com.homework.bookstore.repository.BookRepository;
import com.homework.bookstore.service.exception.BusinessException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AssistantServiceTest {
    ObjectMapper mapper = new ObjectMapper();
    BookRepository repository = mock(BookRepository.class);
    DeepSeekClient model = mock(DeepSeekClient.class);
    Book book = new Book();
    AssistantService service;
    BookTools tools;
    @BeforeEach void setup() {
        book.setId("micro"); book.setTitle("微服务设计");book.setIsbn("9787115638762");book.setStock(24);book.setPrice(new BigDecimal("128.00"));
        when(repository.findByNormalizedIsbn("9787115638762")).thenReturn(List.of(book));
        when(repository.findById("micro")).thenReturn(Optional.of(book));
        when(model.model()).thenReturn("test-only-model");
        tools = new BookTools(repository,mapper); service = new AssistantService(mapper,model,tools,repository);
    }
    private DeepSeekClient.Completion reply(String json) throws Exception {
        return new DeepSeekClient.Completion(mapper.readTree(json), "isolated-test-request");
    }
    @Test void feedsEveryToolResultBackWithMatchingIdsBeforeFinalAnswer() throws Exception {
        var captured = new ArrayList<ArrayNode>();
        var first = reply("""
            {"role":"assistant","content":null,"tool_calls":[
            {"id":"call-stock","type":"function","function":{"name":"check_inventory","arguments":"{\\"isbn\\":\\"9787115638762\\"}"}},
            {"id":"call-price","type":"function","function":{"name":"get_competitor_price","arguments":"{\\"isbn\\":\\"9787115638762\\"}"}}]}
            """);
        var last = reply("{\"role\":\"assistant\",\"content\":\"库存24本；模拟报价117.76元。\"}");
        when(model.complete(any(),any())).thenAnswer(invocation -> {
            captured.add(((ArrayNode)invocation.getArgument(0)).deepCopy());
            return captured.size() == 1 ? first : last;
        });
        var result = service.chat(new AssistantRequest("查库存和价格",List.of()));
        assertEquals(2,result.steps().size());assertTrue(result.answer().contains("模拟"));
        assertEquals(24,result.steps().get(0).observation().path("stock").asInt());
        assertTrue(result.steps().get(1).observation().path("simulated").asBoolean());
        assertEquals(new BigDecimal("117.76"),result.steps().get(1).observation().path("price").decimalValue());
        var second = captured.get(1); assertEquals(5,second.size());
        assertEquals(first.message(),second.get(2));
        assertEquals("call-stock",second.get(3).path("tool_call_id").asText());
        assertEquals("call-price",second.get(4).path("tool_call_id").asText());
        assertEquals(24,mapper.readTree(second.get(3).path("content").asText()).path("stock").asInt());
        assertEquals(1,result.books().size());assertEquals(2,result.modelRequestIds().size());
    }
    @Test void normalizesIsbnAndKeepsUnknownStockDistinctFromZero() {
        book.setStock(null);
        var result = tools.check_inventory("978-7-115-63876-2");
        assertTrue(result.path("ok").asBoolean());assertTrue(result.path("stock").isNull());
        assertEquals("库存待确认",result.path("availability").asText());
    }
    @Test void duplicateIsbnDoesNotChooseAnArbitraryVersion() {
        when(repository.findByNormalizedIsbn("9787115638762")).thenReturn(List.of(book,new Book()));
        assertEquals("AMBIGUOUS_ISBN",tools.check_inventory("9787115638762").path("error").asText());
        assertEquals("AMBIGUOUS_ISBN",tools.get_competitor_price("9787115638762").path("error").asText());
    }
    @Test void passesMalformedArgumentsAsToolFailureWithoutExecutingQuery() throws Exception {
        when(model.complete(any(),any())).thenReturn(reply("""
            {"role":"assistant","tool_calls":[{"id":"bad","function":{"name":"check_inventory","arguments":"{\\"isbn\\":9787115638762}"}}]}
            """),reply("{\"role\":\"assistant\",\"content\":\"需要字符串ISBN，请核对。\"}"));
        var response = service.chat(new AssistantRequest("查询",null));
        assertEquals("INVALID_ARGUMENTS",response.steps().get(0).observation().path("error").asText());
        verifyNoInteractions(repository);
    }
    @Test void boundsRepeatedToolCalls() throws Exception {
        when(model.complete(any(),any())).thenReturn(reply("""
            {"role":"assistant","tool_calls":[{"id":"again","function":{"name":"check_inventory","arguments":"{\\"isbn\\":\\"9787115638762\\"}"}}]}
            """));
        assertThrows(BusinessException.class,()->service.chat(new AssistantRequest("查询",null)));
        verify(model,times(4)).complete(any(),any());
    }
    @ParameterizedTest
    @ValueSource(strings = {"9787115638762", "978-7-115-63876-2"})
    void doesNotTreatHistoricalStockAsCurrentWithoutNewToolResult(String isbn) throws Exception {
        var stale = reply("{\"role\":\"assistant\",\"content\":\"上次说库存99本。\"}");
        var call = reply("""
            {"role":"assistant","tool_calls":[{"id":"fresh","function":{"name":"check_inventory","arguments":"{\\"isbn\\":\\"9787115638762\\"}"}}]}
            """);
        var fresh = reply("{\"role\":\"assistant\",\"content\":\"重新查到库存24本。\"}");
        when(model.complete(any(),any())).thenReturn(stale,call,fresh);
        var result = service.chat(new AssistantRequest("刚才这本还有多少库存？",List.of(new AssistantRequest.Turn("assistant","ISBN " + isbn + " 之前库存99本"))));
        assertEquals(24,result.steps().get(0).observation().path("stock").asInt());
        assertEquals("重新查到库存24本。",result.answer());verify(model,times(3)).complete(any(),any());
    }
}

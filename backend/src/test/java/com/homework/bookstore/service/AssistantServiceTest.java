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
        verify(repository,times(2)).findByNormalizedIsbn("9787115638762");
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
    @Test void wrongIsbnShapeDoesNotQueryDatabase() {
        assertEquals("INVALID_ISBN",tools.check_inventory("12345").path("error").asText());
        assertEquals("INVALID_ISBN",tools.get_competitor_price("ISBN12345").path("error").asText());
        verifyNoInteractions(repository);
    }
    @Test void missingBookIsDistinctFromInvalidShape() {
        assertEquals("BOOK_NOT_FOUND",tools.check_inventory("9780000000000").path("error").asText());
        verify(repository).findByNormalizedIsbn("9780000000000");
    }
    @Test void simulatedTimeoutIsReturnedToModelThenSameIsbnRetryRecoversPerRequest() throws Exception {
        var observed = new ArrayList<ArrayNode>();
        var call = reply("""
            {"role":"assistant","tool_calls":[{"id":"quote","function":{"name":"get_competitor_price","arguments":"{\\"isbn\\":\\"9787115638762\\"}"}}]}
            """);
        var done = reply("{\"role\":\"assistant\",\"content\":\"模拟超时后重试成功，模拟报价117.76元。\"}");
        for (int request = 0; request < 2; request++) {
            observed.clear();
            doAnswer(invocation -> {
                observed.add(((ArrayNode)invocation.getArgument(0)).deepCopy());
                return observed.size() < 3 ? call : done;
            }).when(model).complete(any(),any());
            var result = service.chat(new AssistantRequest("ISBN 9787115638762 查询竞价",null,"competitor-timeout-once"));
            assertEquals(2,result.steps().size());
            assertEquals("COMPETITOR_TIMEOUT",result.steps().get(0).observation().path("error").asText());
            assertTrue(result.steps().get(0).observation().path("retryable").asBoolean());
            assertTrue(result.steps().get(0).observation().path("simulated").asBoolean());
            assertTrue(result.steps().get(1).observation().path("ok").asBoolean());
            assertEquals(2,result.steps().get(1).observation().path("attempt").asInt());
            var toolMessage = observed.get(1).get(3);
            assertEquals("quote",toolMessage.path("tool_call_id").asText());
            assertEquals("COMPETITOR_TIMEOUT",mapper.readTree(toolMessage.path("content").asText()).path("error").asText());
        }
        assertEquals(24,book.getStock());verify(repository,times(4)).findByNormalizedIsbn("9787115638762");
    }
    @Test void thirdIdenticalToolAttemptStopsExecutingTheLocalFunction() throws Exception {
        var call = reply("""
            {"role":"assistant","tool_calls":[{"id":"quote","function":{"name":"get_competitor_price","arguments":"{\\"isbn\\":\\"9787115638762\\"}"}}]}
            """);
        when(model.complete(any(),any())).thenReturn(call,call,call,reply("{\"role\":\"assistant\",\"content\":\"已到重试上限。\"}"));
        var result = service.chat(new AssistantRequest("查竞价",null));
        assertEquals("TOOL_RETRY_LIMIT",result.steps().get(2).observation().path("error").asText());
        assertFalse(result.steps().get(2).observation().path("retryable").asBoolean());
        verify(repository,times(2)).findByNormalizedIsbn("9787115638762");
    }
    @Test void malformedCurrentIsbnDoesNotForceAQueryUsingHistoricalBook() throws Exception {
        when(model.complete(any(),any())).thenReturn(reply("{\"role\":\"assistant\",\"content\":\"请核对12345并提供完整13位ISBN。\"}"));
        var result = service.chat(new AssistantRequest("帮我查ISBN为12345的库存",List.of(new AssistantRequest.Turn("assistant","ISBN 9787115638762 库存24本"))));
        assertEquals(0,result.steps().size());assertEquals(0,result.books().size());
        verify(model,times(1)).complete(any(),any());verifyNoInteractions(repository);
    }
}

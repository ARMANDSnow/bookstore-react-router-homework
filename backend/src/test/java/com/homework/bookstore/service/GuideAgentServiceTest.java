package com.homework.bookstore.service;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.node.*;
import com.homework.bookstore.dto.GuideRequest;
import com.homework.bookstore.entity.Book;
import com.homework.bookstore.repository.BookRepository;
import com.homework.bookstore.service.exception.BusinessException;
import org.junit.jupiter.api.*;
import java.util.*;
import java.math.BigDecimal;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;
class GuideAgentServiceTest {
 final ObjectMapper mapper=new ObjectMapper();DeepSeekClient model;BookRepository catalog;PolicyRagClient policies;GuideAgentService service;
 @BeforeEach void setup(){model=mock(DeepSeekClient.class);catalog=mock(BookRepository.class);policies=mock(PolicyRagClient.class);service=new GuideAgentService(mapper,model,catalog,policies);when(model.model()).thenReturn("test-model");}
 DeepSeekClient.Completion tool(String id,String name,String query){var reply=mapper.createObjectNode().put("role","assistant");reply.putNull("content");var call=reply.putArray("tool_calls").addObject().put("id",id).put("type","function");call.putObject("function").put("name",name).put("arguments",mapper.createObjectNode().put("query",query).put("purpose","查询公开依据").toString());return new DeepSeekClient.Completion(reply,"test-response");}
 DeepSeekClient.Completion answer(String text){return new DeepSeekClient.Completion(mapper.createObjectNode().put("role","assistant").put("content",text),"test-response");}
 @Test void passesEveryToolResultWithMatchingIdAndRetainsBookAndPolicy() throws Exception {
  Book b=new Book();b.setId("micro");b.setTitle("微服务设计");b.setAuthor("Sam Newman");b.setSummary("微服务架构");b.setDescription("服务拆分");b.setPrice(BigDecimal.TEN);when(catalog.findAll()).thenReturn(List.of(b));when(catalog.findById("micro")).thenReturn(Optional.of(b));
  when(policies.query("拆封能退吗")).thenReturn(mapper.readTree("{\"ok\":true,\"matched\":true,\"chunks\":[{\"id\":\"policy-2\",\"text\":\"非质量退货不接受已拆封图书\"}]}"));
  when(model.complete(any(),any())).thenAnswer(new org.mockito.stubbing.Answer<DeepSeekClient.Completion>(){int round=0;public DeepSeekClient.Completion answer(org.mockito.invocation.InvocationOnMock invocation){ArrayNode messages=invocation.getArgument(0);if(round==0){round++;return tool("book-call","search_book_catalog","微服务");}if(round==1){assertTrue(messages.toString().contains("book-call"));assertTrue(messages.toString().contains("微服务设计"));round++;return tool("policy-call","query_store_policy","拆封能退吗");}assertEquals("policy-call",messages.get(messages.size()-1).path("tool_call_id").asText());return GuideAgentServiceTest.this.answer("推荐微服务设计；policy-2说明已拆封非质量退货不受理。");}});
  var reply=service.chat(new GuideRequest("推荐微服务书，拆封能退吗",List.of()));assertEquals(2,reply.steps().size());assertEquals(1,reply.books().size());assertEquals("policy-2",reply.policyChunks().get(0).path("id").asText());assertEquals(3,reply.modelRequestIds().size());verify(catalog,never()).save(any());
 }
 @Test void unknownToolNeverDispatches() {when(model.complete(any(),any())).thenReturn(tool("unknown","delete_database","全部"),answer("工具不可用"));var reply=service.chat(new GuideRequest("试试",List.of()));assertEquals("UNKNOWN_TOOL",reply.steps().get(0).observation().path("errorCode").asText());verifyNoInteractions(catalog,policies);}
 @Test void policyOutageIsObservationWithoutInventedChunks(){when(policies.query("会员")).thenThrow(new BusinessException(50330,"离线"));when(model.complete(any(),any())).thenReturn(tool("policy","query_store_policy","会员"),answer("政策暂不可用，请人工核实"));var reply=service.chat(new GuideRequest("会员",List.of()));assertEquals("POLICY_UNAVAILABLE",reply.steps().get(0).observation().path("errorCode").asText());assertTrue(reply.policyChunks().isEmpty());verifyNoInteractions(catalog);}
 @Test void batchCallsBothReceiveRejectionWithoutExecution(){var first=tool("a","search_book_catalog","微服务").message().deepCopy();((ArrayNode)first.get("tool_calls")).add(tool("b","query_store_policy","拆封").message().get("tool_calls").get(0));when(model.complete(any(),any())).thenAnswer(new org.mockito.stubbing.Answer<DeepSeekClient.Completion>(){int round=0;public DeepSeekClient.Completion answer(org.mockito.invocation.InvocationOnMock i){if(round++==0)return new DeepSeekClient.Completion(first,"test-response");ArrayNode messages=i.getArgument(0);assertEquals("b",messages.get(messages.size()-1).path("tool_call_id").asText());assertEquals("a",messages.get(messages.size()-2).path("tool_call_id").asText());return GuideAgentServiceTest.this.answer("请将问题分开查询");}});var reply=service.chat(new GuideRequest("问问",List.of()));assertEquals(2,reply.steps().size());assertTrue(reply.steps().stream().allMatch(s->s.observation().path("errorCode").asText().equals("SINGLE_ACTION_REQUIRED")));verifyNoInteractions(catalog,policies);}
 @Test void repeatedPolicyCallsAreBounded(){when(policies.query("会员")).thenReturn(mapper.createObjectNode().put("ok",false));when(model.complete(any(),any())).thenReturn(tool("same","query_store_policy","会员"));assertThrows(BusinessException.class,()->service.chat(new GuideRequest("会员",List.of())));verify(policies,times(2)).query("会员");}
 @Test void literalSearchReturnsEmptyForUnknownTopic(){when(catalog.findAll()).thenReturn(List.of());var result=service.search_book_catalog("量子引力");assertTrue(result.path("ok").asBoolean());assertFalse(result.path("hasMatches").asBoolean());assertTrue(result.path("books").isEmpty());}
 @Test void prematurePolicyAnswerRequiresActualPolicyQuery() throws Exception {
  when(catalog.findAll()).thenReturn(List.of());when(policies.query("拆封")).thenReturn(mapper.readTree("{\"ok\":true,\"chunks\":[{\"id\":\"policy-2\"}]}"));
  when(model.complete(any(),any())).thenReturn(tool("book","search_book_catalog","微服务"),answer("拆封可以退"),tool("policy","query_store_policy","拆封"),answer("没有匹配书目；policy-2说明拆封非质量退货不受理。"));
  var reply=service.chat(new GuideRequest("推荐微服务书，拆封能退吗",List.of()));assertEquals(4,reply.modelRequestIds().size());assertEquals(2,reply.steps().size());verify(policies).query("拆封");
 }
 @Test void inventedPolicyIdRequiresCorrection() throws Exception {
  when(policies.query("会员")).thenReturn(mapper.readTree("{\"ok\":true,\"chunks\":[{\"id\":\"policy-5\"}]}"));
  when(model.complete(any(),any())).thenReturn(tool("policy","query_store_policy","会员"),answer("会员按policy-99执行。"),answer("请参照policy-5的会员条件。"));
  var reply=service.chat(new GuideRequest("会员条件",List.of()));assertEquals(3,reply.modelRequestIds().size());assertFalse(reply.answer().contains("policy-99"));
 }
 @Test void rejectedBatchDoesNotConsumeActualRetryBudget() throws Exception {
  var batch=tool("a","query_store_policy","会员").message().deepCopy();((ArrayNode)batch.get("tool_calls")).add(tool("b","search_book_catalog","会员").message().get("tool_calls").get(0));
  when(policies.query("会员")).thenThrow(new BusinessException(50330,"离线")).thenReturn(mapper.readTree("{\"ok\":true,\"chunks\":[{\"id\":\"policy-5\"}]}"));
  when(model.complete(any(),any())).thenReturn(new DeepSeekClient.Completion(batch,"test-response"),tool("first","query_store_policy","会员"),tool("retry","query_store_policy","会员"),answer("会员条件见policy-5。"));
  var reply=service.chat(new GuideRequest("会员条件",List.of()));assertEquals(4,reply.steps().size());assertTrue(reply.steps().get(3).observation().path("ok").asBoolean());verify(policies,times(2)).query("会员");verifyNoInteractions(catalog);
 }
}

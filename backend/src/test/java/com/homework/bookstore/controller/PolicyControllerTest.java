package com.homework.bookstore.controller;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.homework.bookstore.service.PolicyRagClient;
import com.homework.bookstore.service.exception.BusinessException;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import org.springframework.http.MediaType;
@WebMvcTest(PolicyController.class)
@AutoConfigureMockMvc(addFilters=false)
class PolicyControllerTest {
 @Autowired MockMvc mvc;
 @Autowired ObjectMapper mapper;
 @MockBean PolicyRagClient policies;
 @Test void returnsOnlyRetrievedPolicy() throws Exception {
  when(policies.query("拆封能退吗")).thenReturn(mapper.readTree("{\"matched\":true,\"chunks\":[{\"id\":\"policy-2\",\"text\":\"已拆封非质量退货不受理\"}]}"));
  mvc.perform(post("/api/policies/search").contentType(MediaType.APPLICATION_JSON).content("{\"query\":\" 拆封能退吗 \"}")).andExpect(status().isOk()).andExpect(jsonPath("$.data.chunks[0].id").value("policy-2"));
  verify(policies).query("拆封能退吗");
 }
 @Test void rejectsBlankAndOverlongBeforeQuery() throws Exception {
  for(String q:new String[]{" ","字".repeat(601)}) mvc.perform(post("/api/policies/search").contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(java.util.Map.of("query",q)))).andExpect(status().isBadRequest());
  verifyNoInteractions(policies);
 }
 @Test void outageIsServiceUnavailableNotEmptyPolicy() throws Exception {
  when(policies.query("会员")).thenThrow(new BusinessException(50330,"政策检索暂时不可用，请稍后重试。"));
  mvc.perform(post("/api/policies/search").contentType(MediaType.APPLICATION_JSON).content("{\"query\":\"会员\"}")).andExpect(status().isServiceUnavailable()).andExpect(jsonPath("$.code").value(50330)).andExpect(jsonPath("$.data").isEmpty());
 }
 @Test void preservesNoEvidenceResult() throws Exception {
  when(policies.query("其他事项")).thenReturn(mapper.readTree("{\"matched\":false,\"chunks\":[]}"));
  mvc.perform(post("/api/policies/search").contentType(MediaType.APPLICATION_JSON).content("{\"query\":\"其他事项\"}")).andExpect(status().isOk()).andExpect(jsonPath("$.data.matched").value(false));
 }
}

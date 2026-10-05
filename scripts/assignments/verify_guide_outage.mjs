import fs from 'node:fs/promises';import assert from 'node:assert/strict';
const base=process.env.BOOKSTORE_API_BASE||'http://127.0.0.1:8080';
const question='课程会员积分有效期多久？';
const response=await fetch(base+'/api/guide/chat',{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify({message:question,history:[]}),signal:AbortSignal.timeout(250000)});const body=await response.json();
assert.equal(response.status,200,JSON.stringify(body));const reply=body.data;
assert.ok(reply.steps.some(s=>s.action==='query_store_policy'&&s.observation.errorCode==='POLICY_UNAVAILABLE'));
assert.equal(reply.policyChunks.length,0);assert.ok(reply.modelRequestIds.length>=2);assert.match(reply.answer,/无法|未知|不能确认|暂时|不可用/);assert.doesNotMatch(reply.answer,/policy-\d+/);
await fs.writeFile(new URL('../../docs/assignments/evidence/3b-policy-outage.json',import.meta.url),JSON.stringify({testedAt:new Date().toISOString(),environment:'主动停止本任务Embedding伴随服务，真实DeepSeek继续运行',question,http:response.status,response:body,complete:true},null,2)+'\n');console.log('真实政策服务中断：工具返回明确不可用，最终答复不编造政策引用。');

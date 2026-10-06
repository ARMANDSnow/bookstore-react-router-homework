import fs from 'node:fs/promises';
import assert from 'node:assert/strict';
const base = process.env.BOOKSTORE_API_BASE || 'http://127.0.0.1:8080';
const file = (process.env.BOOKSTORE_EVIDENCE_FILE || new URL('../../docs/assignments/evidence/2b-real-model.json',import.meta.url));
const evidence = { at:new Date().toISOString(), environment:'真实DeepSeek + 本机MySQL；竞价首次超时为显式课程模拟', complete:false, cases:[] };
async function chat(message,scenario='normal',history=[]) {
  const response = await fetch(base+'/api/assistant/chat',{method:'POST',headers:{'Content-Type':'application/json',Origin:'http://127.0.0.1:5173'},body:JSON.stringify({message,history,scenario}),signal:AbortSignal.timeout(240000)});
  const body = await response.json();
  evidence.cases.push({message,scenario,history,status:response.status,body});
  await fs.writeFile(file,JSON.stringify(evidence,null,2)+'\n');
  assert.equal(response.status,200);assert.equal(body.code,0);return body.data;
}
const original = (await (await fetch(base+'/api/books/building-microservices')).json()).data;
const recovered = await chat(`查询ISBN ${original.isbn}的库存，并比较本店售价和竞价。`,'competitor-timeout-once');
const quotes = recovered.steps.filter(step=>step.name==='get_competitor_price');
assert.equal(quotes.length,2);assert.equal(quotes[0].observation.error,'COMPETITOR_TIMEOUT');
assert.equal(quotes[0].observation.simulated,true);assert.equal(quotes[0].observation.retryable,true);
assert.equal(quotes[1].arguments.isbn,quotes[0].arguments.isbn);assert.equal(quotes[1].observation.ok,true);
assert.equal(quotes[1].observation.attempt,2);assert.equal(quotes[1].observation.simulated,true);
assert.equal(recovered.steps.find(step=>step.name==='check_inventory').observation.stock,original.stock);
assert.match(recovered.answer,/参考报价/);assert.ok(recovered.answer.includes(String(quotes[1].observation.price)));
assert.ok(recovered.modelRequestIds.length>=3);
const notFound = await chat('请核对ISBN 9780000000000的库存，不要换其他号码。');
assert.equal(notFound.steps[0].observation.error,'BOOK_NOT_FOUND');assert.equal(notFound.books.length,0);
assert.match(notFound.answer,/核对|确认|未找到|找不到/);
assert.ok(notFound.steps.every(step=>step.arguments.isbn==='9780000000000'));
const malformed = await chat('请核对ISBN为12345的库存。','normal',[{role:'user',content:`ISBN ${original.isbn} 库存是多少？`},{role:'assistant',content:`ISBN ${original.isbn} 当前库存24本。`}]);
assert.equal(malformed.books.length,0);assert.match(malformed.answer,/13|十三|核对|完整/);
if (malformed.steps.length) assert.equal(malformed.steps[0].observation.error,'INVALID_ISBN');
assert.ok(malformed.steps.every(step=>step.arguments.isbn==='12345'));
// The malformed input may be clarified before a tool call; retain the actual behavior.
evidence.malformedBehavior = malformed.steps.length ? '工具明确拒绝格式错误' : '真实模型先要求完整ISBN，未伪造工具调用';
const invalid = await fetch(base+'/api/assistant/chat',{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify({message:'查库存',scenario:'unknown'})});
assert.equal(invalid.status,400);evidence.invalidScenario={status:invalid.status,body:await invalid.json()};
const after = (await (await fetch(base+'/api/books/building-microservices')).json()).data;
assert.deepEqual(after,original);evidence.databaseUnchanged=true;
evidence.complete=true;await fs.writeFile(file,JSON.stringify(evidence,null,2)+'\n');
console.log({complete:true,timeoutRecovered:true,malformedBehavior:evidence.malformedBehavior,bookUnchanged:true});

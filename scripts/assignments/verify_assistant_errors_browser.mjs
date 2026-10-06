import fs from 'node:fs/promises';
import assert from 'node:assert/strict';
const root=decodeURIComponent(new URL('../../',import.meta.url).pathname).replace(/\/$/,'');
const task=await taskSpace(Number(process.env.EGO_TASK_SPACE || 11));const page=task.page('p1');
const evidence={at:new Date().toISOString(),desktopOnly:true,cases:{},functionalComplete:false,visualReview:'待检查',complete:false};
await page.goto('http://127.0.0.1:5173/assistant?book=building-microservices');
await page.waitForSelector('#assistant-book');
await page.evaluate(()=>{
  window.__errorsQa=[];const original=window.fetch.bind(window);
  window.fetch=async(url,options)=>{const entry=String(url).includes('/api/assistant/chat')?{input:JSON.parse(options.body)}:null;if(entry)window.__errorsQa.push(entry);try{const response=await original(url,options);if(entry){entry.status=response.status;entry.body=await response.clone().json();}return response;}catch(error){if(entry)entry.networkError=true;throw error;}};
});
async function state(){return page.evaluate(()=>({turns:[...document.querySelectorAll('.assistant-turn')].map(x=>x.textContent),count:document.querySelectorAll('.assistant-turn.assistant').length,error:document.querySelector('.assistant-error')?.textContent,scenario:window.__errorsQa.at(-1)?.input.scenario||'normal',demoVisible:!!document.querySelector('.assistant-demo'),requests:window.__errorsQa,disabled:document.querySelector('#assistant-input').disabled,overflow:document.documentElement.scrollWidth>innerWidth}));}
async function action(kind,selector,value){try{await page[kind](selector,value);}catch(error){if(!error.message.includes('not visible in the viewport'))throw error;const visible=await page.evaluate(selector=>{const node=document.querySelector(selector);if(!node)return false;node.scrollIntoView({block:'center',behavior:'instant'});return true;},selector);assert.equal(visible,true);await page[kind](selector,value);}}
async function send(text,count){await action('fill','#assistant-input',text);await action('click','button[aria-label="发送问题"]');assert.equal((await state()).disabled,true);await page.waitForFunction(n=>document.querySelectorAll('.assistant-turn.assistant').length===n||document.querySelector('.assistant-error'),count,{timeout:240000});const result=await state();assert.equal(result.error,undefined);assert.equal(result.count,count);return result;}
try{
  const initial=await state();assert.equal(initial.scenario,'normal');assert.equal(initial.demoVisible,false);
  const normal=await send('这本书还有库存吗？',1);assert.equal(normal.requests.at(-1).body.data.steps[0].observation.stock,24);
  evidence.cases.defaultNormalQuery=true;
  const question='帮我查一下 ISBN为12345 的书还有多少本';
  const malformed=await send(question,2);
  assert.equal(malformed.requests.at(-1).input.message,question);
  assert.equal(malformed.requests.at(-1).body.data.books.length,0);
  assert.match(malformed.turns.at(-1),/13|十三|核对|完整/);
  assert.equal(malformed.requests.at(-1).input.history.length,2);
  evidence.cases.explicitWrongIsbnPreserved=true;
  await action('click','.assistant-chat-header button');
  await page.goto('http://127.0.0.1:5173/assistant?book=building-microservices&scenario=competitor-timeout-once');await page.waitForSelector('#assistant-input');await page.evaluate(()=>{window.__errorsQa=[];const original=window.fetch.bind(window);window.fetch=async(url,options)=>{const entry=String(url).includes('/api/assistant/chat')?{input:JSON.parse(options.body)}:null;if(entry)window.__errorsQa.push(entry);try{const response=await original(url,options);if(entry){entry.status=response.status;entry.body=await response.clone().json();}return response;}catch(error){if(entry)entry.networkError=true;throw error;}};});
  await page.cdp('Network.enable');await page.cdp('Network.emulateNetworkConditions',{offline:true,latency:0,downloadThroughput:-1,uploadThroughput:-1});
  await action('fill','#assistant-input','查库存，并比较本店售价和竞价。');await action('click','button[aria-label="发送问题"]');
  await page.waitForSelector('.assistant-error');assert.match((await state()).error,/连接暂时中断/);
  await page.cdp('Network.emulateNetworkConditions',{offline:false,latency:0,downloadThroughput:-1,uploadThroughput:-1});
  await action('click','.assistant-error button:first-of-type');
  await page.waitForFunction(()=>document.querySelector('.assistant-turn.assistant')||document.querySelector('.assistant-error'),undefined,{timeout:240000});
  const recovered=await state();assert.equal(recovered.error,undefined);assert.equal(recovered.count,1);
  assert.equal(recovered.scenario,'competitor-timeout-once');assert.equal(recovered.turns.length,2);
  const reply=recovered.requests.at(-1).body.data;const quotes=reply.steps.filter(x=>x.name==='get_competitor_price');
  assert.equal(quotes.length,2);assert.equal(quotes[0].observation.error,'COMPETITOR_TIMEOUT');assert.equal(quotes[1].observation.ok,true);
  assert.equal(quotes[1].observation.attempt,2);assert.match(reply.answer,/参考报价/);
  assert.match(await page.evaluate(()=>document.querySelector('.assistant-recovered')?.textContent),/查询曾超时.*重试后/);
  assert.equal(recovered.overflow,false);evidence.cases.retryPreservesOriginalScenarioAndQuestion=true;
  evidence.cases.realModelTimeoutRecovery=true;evidence.requests=recovered.requests;
  await page.evaluate(()=>{document.querySelector('.assistant-tool-details').open=true;const log=document.querySelector('.assistant-messages');log.scrollTop=log.scrollHeight;window.scrollTo(0,290);});
  const alternate=await page.evaluate(()=>[...document.querySelector('#assistant-book').options].find(o=>o.value!==document.querySelector('#assistant-book').value).value);await page.selectOption('#assistant-book',alternate);await action('click','.assistant-chat-header button');assert.equal(await page.evaluate(()=>new URL(location.href).searchParams.has('scenario')),false);assert.equal(await page.evaluate(()=>document.querySelector('#assistant-book').value),alternate);evidence.cases.newConversationPreservesSelectedBook=true;const fresh=await send('这本书的参考报价是多少？',1);assert.equal(fresh.requests.at(-1).input.scenario,'normal');assert.equal(fresh.requests.at(-1).body.data.steps.filter(s=>s.name==='get_competitor_price').length,1);evidence.cases.newConversationRestoresNormal=true;
  evidence.functionalComplete=true;
}catch(error){evidence.failure=error.message;throw error;}
finally{await page.cdp('Network.emulateNetworkConditions',{offline:false,latency:0,downloadThroughput:-1,uploadThroughput:-1});evidence.requests=await page.evaluate(()=>window.__errorsQa);await fs.writeFile((process.env.BOOKSTORE_EVIDENCE_FILE || root+'/docs/assignments/evidence/2b-browser.json'),JSON.stringify(evidence,null,2)+'\n');}
console.log({functionalComplete:evidence.functionalComplete,cases:evidence.cases});console.log(await page.snapshot());

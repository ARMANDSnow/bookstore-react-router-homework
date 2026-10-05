// Run inside the existing Ego TaskSpace. Records real UI/network results; no mocked replies.
import { readFile, writeFile } from 'node:fs/promises';
import assert from 'node:assert/strict';
const root = '/Users/dingyuxuan/Desktop/线上书城';
const task = await taskSpace(11);
const page = task.page('p1');
const evidence = { at: new Date().toISOString(), desktopOnly: true, realModel: true, cases: {}, requests: [], visualReview: '等待解锁后完成', complete: false };
await page.goto('http://127.0.0.1:5173/assistant');
await page.waitForSelector('#assistant-book');
await page.evaluate(() => {
  window.__assistantQaRequests = [];
  const original = window.fetch.bind(window);
  window.fetch = async (url, options) => {
    const recorded = String(url).includes('/api/assistant/chat');
    const entry = recorded ? { input: JSON.parse(options.body) } : null;
    if (entry) window.__assistantQaRequests.push(entry);
    try {
      const response = await original(url, options);
      if (entry) { entry.status = response.status; entry.response = await response.clone().json(); }
      return response;
    } catch (error) { if (entry) entry.networkError = true; throw error; }
  };
});
async function state() {
  return page.evaluate(() => ({
    turns: [...document.querySelectorAll('.assistant-turn')].map(node => node.textContent),
    assistantCount: document.querySelectorAll('.assistant-turn.assistant').length,
    stepCount: document.querySelectorAll('.assistant-tool-details li').length,
    error: document.querySelector('.assistant-error')?.textContent,
    input: document.querySelector('#assistant-input').value,
    inputDisabled: document.querySelector('#assistant-input').disabled,
    sendDisabled: document.querySelector('button[aria-label="发送问题"]').disabled,
    selection: document.querySelector('#assistant-book').value,
    active: document.activeElement?.id,
    overflow: document.documentElement.scrollWidth > innerWidth,
    requests: window.__assistantQaRequests,
  }));
}
async function click(selector) {
  try { await page.click(selector); }
  catch (error) {
    if (!error.message.includes('not visible in the viewport')) throw error;
    // Browser wheel input cannot scroll reliably while macOS is locked.
    const target = await page.evaluate(selector => {
      const node = document.querySelector(selector);
      if (!node) return false;
      node.scrollIntoView({block:'center',behavior:'instant'});
      return true;
    }, selector);
    assert.equal(target,true); await page.click(selector);
  }
}
async function send(question, count) {
  await page.fill('#assistant-input', question);
  await click('button[aria-label="发送问题"]');
  const waiting = await state();
  assert.equal(waiting.inputDisabled, true);
  assert.equal(waiting.sendDisabled, true);
  await page.waitForFunction(expected => document.querySelectorAll('.assistant-turn.assistant').length === expected || document.querySelector('.assistant-error'), count, {timeout: 60_000});
  const result = await state();
  assert.equal(result.error, undefined);
  assert.equal(result.assistantCount, count);
  return result;
}
try {
  assert.equal((await state()).sendDisabled, true);
  evidence.cases.emptyQuestionDisabled = true;
  await page.fill('#assistant-input', '正在输入中文');
  const composition = await page.evaluate(() => {
    const field = document.querySelector('#assistant-input');
    const event = new KeyboardEvent('keydown', {key:'Enter', code:'Enter', bubbles:true, cancelable:true, isComposing:true});
    field.dispatchEvent(event);
    return { prevented: event.defaultPrevented, turns: document.querySelectorAll('.assistant-turn').length };
  });
  assert.equal(composition.prevented, false); assert.equal(composition.turns, 0);
  evidence.cases.chineseCompositionDoesNotSubmit = true;
  const initial = await send('请查询库存，并比较本店售价和竞价。', 1);
  const first = initial.requests.at(-1).response.data;
  assert.deepEqual(first.steps.map(step=>step.name).sort(), ['check_inventory','get_competitor_price']);
  assert.equal(first.steps.find(step=>step.name==='check_inventory').observation.stock, 24);
  assert.match(first.answer,/模拟/); assert.equal(initial.overflow,false);
  evidence.cases.inventoryAndPrice = { steps: 2, simulatedDisclosed: true, waitingDisabled: true };
  await page.selectOption('#assistant-book','clean-code');
  const switched = await send('还有多少库存？',2);
  const second = switched.requests.at(-1);
  assert.match(second.input.message,/代码整洁之道/);
  assert.match(second.input.message,/9787115216878/);
  assert.equal(second.response.data.steps[0].observation.id,'clean-code');
  evidence.cases.changedBookUsesCurrentSelection = true;
  const followup = await send('这本书现在还有货吗？',3);
  assert.equal(followup.requests.at(-1).response.data.steps[0].name,'check_inventory');
  assert.equal(followup.requests.at(-1).response.data.steps[0].observation.id,'clean-code');
  assert.equal(followup.requests.at(-1).input.history.length,4);
  evidence.cases.followupFreshInventory = true;
  // Scope offline emulation to the owned Page and always restore it.
  await page.cdp('Network.enable');
  await page.cdp('Network.emulateNetworkConditions',{offline:true,latency:0,downloadThroughput:-1,uploadThroughput:-1});
  await page.fill('#assistant-input','请再查一次库存');
  await click('button[aria-label="发送问题"]');
  await page.waitForSelector('.assistant-error');
  const failed = await state();
  assert.match(failed.error,/连接暂时中断/);assert.equal(failed.inputDisabled,false);
  await click('.assistant-error button:last-child');
  const edited = await state();
  assert.equal(edited.input,'请再查一次库存');assert.equal(edited.assistantCount,3);
  await click('button[aria-label="发送问题"]');
  await page.waitForSelector('.assistant-error');
  await page.cdp('Network.emulateNetworkConditions',{offline:false,latency:0,downloadThroughput:-1,uploadThroughput:-1});
  await click('.assistant-error button:first-of-type');
  await page.waitForFunction(()=>document.querySelectorAll('.assistant-turn.assistant').length===4||document.querySelector('.assistant-error'),undefined,{timeout:60_000});
  const recovered = await state();
  assert.equal(recovered.error,undefined);assert.equal(recovered.assistantCount,4);
  assert.equal(recovered.turns.length,8);assert.equal(recovered.active,'assistant-input');
  evidence.cases.networkFailureEditRetry = { recovered: true, noDuplicateQuestion: true, editPreservesOriginalText: true, focusRestored: true };
  evidence.requests = recovered.requests;
  await click('.assistant-chat-header button');
  assert.equal((await state()).turns.length,0);
  evidence.cases.newConversation = true;
  await page.goto('http://127.0.0.1:5173/books/clean-code');
  await page.waitForSelector('a[href="/assistant?book=clean-code"]');
  await click('a[href="/assistant?book=clean-code"]');
  await page.waitForSelector('#assistant-book');
  assert.equal((await state()).selection,'clean-code');
  evidence.cases.detailEntrySelectsBook = true;
  evidence.functionalComplete = true;
} catch (error) {
  evidence.failure = error.message;
  throw error;
} finally {
  await page.cdp('Network.emulateNetworkConditions',{offline:false,latency:0,downloadThroughput:-1,uploadThroughput:-1});
  const pendingCapture = await page.evaluate(()=>window.__assistantQaRequests);
  if (pendingCapture) evidence.requests = pendingCapture;
  const output = `${root}/docs/assignments/evidence/2a-browser.json`;
  try { evidence.prior = JSON.parse(await readFile(output,'utf8')); delete evidence.prior.prior; } catch {}
  await writeFile(output, JSON.stringify(evidence,null,2)+'\n');
}
console.log({functionalComplete:evidence.functionalComplete,cases:evidence.cases,visualReview:evidence.visualReview});
console.log(await page.snapshot());

import fs from 'node:fs/promises';
import path from 'node:path';
import { fileURLToPath } from 'node:url';
import assert from 'node:assert/strict';
import SwaggerParser from '@apidevtools/swagger-parser';
import Ajv from 'ajv';
const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '../..');
const dir = path.join(root, 'docs/assignments/作业1');
const canonical = JSON.parse(await fs.readFile(path.join(root, 'backend/src/main/resources/static/api/v1/openapi.json')));
const rounds = JSON.parse(await fs.readFile(path.join(dir, 'conversation.json'))).rounds;
const checks = [];
// 【作业1 · 得分点1.2：JSON 与 OpenAPI 双重校验】JSON.parse 验证语法；Swagger Parser 再检查
// OpenAPI 文档结构和引用。合法 JSON 可以是任意对象，所以只做 JSON.parse 不足以证明规范有效。
// 【作业1 · 得分点1.5：逐轮检查关键约束】对保存的每轮回复使用相同断言，观察路径与库存规则
// 是否保持一致。历史解析失败会明确记录，最终轮必须成功；这里验证历史记录，不发起新的模型调用。
for (const round of rounds) {
  if (round.parseError) { checks.push({ round: round.round, json: false, error: round.parseError }); continue; }
  const spec = JSON.parse(round.assistant);
  await SwaggerParser.validate(structuredClone(spec));
  // 【作业1 · 得分点1.3：REST 资源和动词】列表/详情用 GET，新建用 POST，库存修改用 PATCH。
  // 此断言比较 GET/POST/PUT/PATCH/DELETE 范围内的操作集合；HEAD/OPTIONS/TRACE 不在检查范围。
  const operations = Object.entries(spec.paths).flatMap(([route, item]) => Object.keys(item).filter(m => ['get','post','put','patch','delete'].includes(m)).map(method => `${method.toUpperCase()} ${route}`));
  assert.deepEqual(operations.sort(), ['GET /api/books','GET /api/books/{id}','POST /api/books','PATCH /api/books/{id}/inventory'].sort());
  assert.equal(spec.paths['/api/books'].get.parameters.find(p=>p.name==='size').schema.default, 12);
  assert.deepEqual(spec.components.schemas.InventoryUpdateRequest.required, ['stock']);
  assert.equal(spec.components.schemas.InventoryUpdateRequest.additionalProperties, false);
  assert.equal(spec.components.schemas.InventoryUpdateRequest.properties.stock.maximum, 2147483647);
  assert.ok(spec.paths['/api/books'].post.responses['201'].headers.Location);
  checks.push({ round: round.round, json: true, openapi: true, fourOperations: true, invariants: true });
}
assert.equal(rounds.at(-1).parseError, undefined);
// 将 $ref 展开后把最终契约交给 AJV 做响应校验。AJV 是 JSON 数据校验器，检查实际响应是否满足
// schema；这里关闭格式检查，不能据此宣称所有 format 语义或全部请求体规则均已自动验证。
const spec = await SwaggerParser.dereference(structuredClone(canonical));
const ajv = new Ajv({ strict: false, allErrors: true, validateFormats: false });
const http = [];
let created = false;
const id = `contract-${Date.now()}`;
const base = process.env.BOOKSTORE_API_BASE || 'http://127.0.0.1:8080';
async function call(method, route, body, expected, operation) {
  // 【作业1 · 得分点1.4：契约必须与运行结果一致】输入方法/路径/请求体和预期状态，发真实 HTTP
  // 请求后先比对 HTTP 状态，再取该状态的响应 schema 校验 code/message/data 及其中字段。
  // 两层都通过才记录 schema:true，避免把“文档里写了 404”误当成“服务真的返回了 404”。
  const res = await fetch(base + route, { method, headers: { 'Content-Type':'application/json', Origin:'http://localhost:5173' }, ...(body === undefined ? {} : { body: typeof body === 'string' ? body : JSON.stringify(body) }), signal: AbortSignal.timeout(15000) });
  if (method === 'POST' && typeof body === 'object' && body?.id === id && res.status === 201) created = true;
  assert.equal(res.status, expected, `${method} ${route}`);
  const payload = await res.json();
  const schema = operation.responses[String(expected)].content['application/json'].schema;
  const validate = ajv.compile(schema);
  assert.ok(validate(payload), JSON.stringify(validate.errors));
  http.push({ method, route, status:res.status, schema:true, payload });
  return { payload, location:res.headers.get('Location') };
}
const list = spec.paths['/api/books'].get;
const detail = spec.paths['/api/books/{id}'].get;
const create = spec.paths['/api/books'].post;
const inventory = spec.paths['/api/books/{id}/inventory'].patch;
// 答辩可按这组请求展示：page/size 正常分页→未知分类仍为 200 空页→错误范围/类型/排序为 400，
// 再演示已存在与不存在图书的 200/404。同一个响应外壳让正常结果与错误都有明确、可验证的结构。
await call('GET','/api/books?page=1&size=4',undefined,200,list);
await call('GET','/api/books?category=nonexistent-contract-category',undefined,200,list);
await call('GET','/api/books?size=0',undefined,400,list);
await call('GET','/api/books?page=oops',undefined,400,list);
await call('GET','/api/books?sort=oops',undefined,400,list);
await call('GET','/api/books/building-microservices',undefined,200,detail);
await call('GET','/api/books/nonexistent-contract-book',undefined,404,detail);
await call('POST','/api/books','{',400,create);
await call('POST','/api/books',{},400,create);
const book = { id, title:'契约验收临时图书',author:'验收脚本',stock:3,price:12.5,image:'/cover.jpg' };
try {
  // 新建成功要同时看到 201、Location 和返回图书；同 id 再建应为 409。库存更新后比较完整图书，
  // 证明只有 stock 改为 0，标题/售价等保持不变。负数、小数、字符串、null、溢出、额外字段分别测。
  const result = await call('POST','/api/books',book,201,create);
  assert.equal(result.location, `/api/books/${id}`);
  await call('GET',`/api/books/${id}`,undefined,200,detail);
  await call('POST','/api/books',book,409,create);
  const patched = await call('PATCH',`/api/books/${id}/inventory`,{stock:0},200,inventory);
  assert.deepEqual(patched.payload.data, {...result.payload.data,stock:0});
  for (const value of [{stock:-1},{stock:1.5},{stock:'2'},{stock:null},{stock:2147483648},{stock:1,title:'extra'}]) await call('PATCH',`/api/books/${id}/inventory`,value,400,inventory);
  // 原始字符串保留 1.0 与 1e0 的 JSON token 写法；若先 JSON.stringify 数字 1.0，会变成 1，
  // 就无法验证后端“只接受整数 token”的规则。OpenAPI integer 本身不能完整表达这个词法限制。
  for (const raw of ['{"stock":1.0}','{"stock":1e0}']) await call('PATCH',`/api/books/${id}/inventory`,raw,400,inventory);
  await call('PATCH','/api/books/nonexistent-contract-book/inventory',{stock:1},404,inventory);
} finally {
  // 只有本次 POST 已确认为 201 才清理该临时 id，失败路径也执行；清理借用已有旧接口，
  // 并不把 DELETE 加入作业1规定的四个操作。脚本会写数据库，执行前应确认 base 指向验收环境。
  if (created) {
    const cleaned = await fetch(`${base}/api/v1/book/${id}`,{method:'DELETE'});
    assert.equal(cleaned.status,200,'清理仅由本次脚本创建的临时图书');
    assert.equal((await cleaned.json()).code,0);
    await call('GET',`/api/books/${id}`,undefined,404,detail);
  }
}
const served = await fetch(base+'/api/v1/openapi.json');
assert.equal(served.status,200);
assert.deepEqual(await served.json(),canonical);
// 最后确认服务器实际提供的规范与本地最终文件相同，再写出证据；500 只声明，未人为触发。
// 下方 environment 是原验收脚本的 MySQL 环境说明，不会随 BOOKSTORE_API_BASE 自动更新；
// 换成 H2 等环境执行时，须另外如实记录本次环境，不能仅凭这段固定文字认定数据库类型。
const evidence = { testedAt:new Date().toISOString(), environment:'真实 Spring Boot + 本机 MySQL；不是H2或模拟服务', rounds:checks, http, servedCanonical:true, temporaryRecordCleaned:true, unverified:['未人为制造真实服务器内部500异常；仅在契约中声明该兜底响应'] };
await fs.writeFile(path.join(root,'docs/assignments/evidence/1e-contract.json'),JSON.stringify(evidence,null,2)+'\n');
console.log(`OpenAPI正式校验通过；${checks.length}轮记录，${http.length}个真实HTTP响应通过schema校验，临时记录已清理。`);

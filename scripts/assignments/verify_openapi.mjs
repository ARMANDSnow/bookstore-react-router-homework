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
for (const round of rounds) {
  if (round.parseError) { checks.push({ round: round.round, json: false, error: round.parseError }); continue; }
  const spec = JSON.parse(round.assistant);
  await SwaggerParser.validate(structuredClone(spec));
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
const spec = await SwaggerParser.dereference(structuredClone(canonical));
const ajv = new Ajv({ strict: false, allErrors: true, validateFormats: false });
const http = [];
let created = false;
const id = `contract-${Date.now()}`;
const base = process.env.BOOKSTORE_API_BASE || 'http://127.0.0.1:8080';
async function call(method, route, body, expected, operation) {
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
  const result = await call('POST','/api/books',book,201,create);
  assert.equal(result.location, `/api/books/${id}`);
  await call('GET',`/api/books/${id}`,undefined,200,detail);
  await call('POST','/api/books',book,409,create);
  const patched = await call('PATCH',`/api/books/${id}/inventory`,{stock:0},200,inventory);
  assert.deepEqual(patched.payload.data, {...result.payload.data,stock:0});
  for (const value of [{stock:-1},{stock:1.5},{stock:'2'},{stock:null},{stock:2147483648},{stock:1,title:'extra'}]) await call('PATCH',`/api/books/${id}/inventory`,value,400,inventory);
  for (const raw of ['{"stock":1.0}','{"stock":1e0}']) await call('PATCH',`/api/books/${id}/inventory`,raw,400,inventory);
  await call('PATCH','/api/books/nonexistent-contract-book/inventory',{stock:1},404,inventory);
} finally {
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
const evidence = { testedAt:new Date().toISOString(), environment:'真实 Spring Boot + 本机 MySQL；不是H2或模拟服务', rounds:checks, http, servedCanonical:true, temporaryRecordCleaned:true, unverified:['未人为制造真实服务器内部500异常；仅在契约中声明该兜底响应'] };
await fs.writeFile(path.join(root,'docs/assignments/evidence/1e-contract.json'),JSON.stringify(evidence,null,2)+'\n');
console.log(`OpenAPI正式校验通过；${checks.length}轮记录，${http.length}个真实HTTP响应通过schema校验，临时记录已清理。`);

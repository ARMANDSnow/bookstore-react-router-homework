// Real provider transcript: headers and credentials are never written to evidence.
import fs from 'node:fs/promises';
import path from 'node:path';
import { fileURLToPath } from 'node:url';
const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '../..');
const dir = path.join(root, 'docs/assignments/作业1');
const env = await fs.readFile(path.join(root, 'backend/.env'), 'utf8').catch(() => '');
const key = process.env.DEEPSEEK_API_KEY || env.match(/^DEEPSEEK_API_KEY=(.+)$/m)?.[1]?.trim();
if (!key) throw new Error('请在 backend/.env 或环境变量配置 DEEPSEEK_API_KEY');
const messages = [
  { role: 'system', content: await fs.readFile(path.join(dir, 'system-prompt.txt'), 'utf8') },
  { role: 'user', content: await fs.readFile(path.join(dir, 'user-prompt.txt'), 'utf8') },
];
let transcript = { provider: 'DeepSeek', model: 'deepseek-flash', endpoint: 'https://api.deepseek.com/chat/completions', startedAt: new Date().toISOString(), rounds: [] };
const prior = await fs.readFile(path.join(dir, 'conversation.json'), 'utf8').catch(() => null);
if (prior) {
  transcript = JSON.parse(prior);
  messages.splice(1);
  for (const round of transcript.rounds) messages.push({ role: 'user', content: round.user }, { role: 'assistant', content: round.assistant });
}
const reviews = [
  null,
  '第二轮契约审查：逐项确认只有四个操作、201的Location、404/409错误、query默认值和范围、必填字段、int32库存最大值、库存additionalProperties:false、创建请求允许未知字段、nullable旧数据。非空白文本pattern应为含\\S的正确JSON转义；ErrorResponse的data必须允许null，但不能使用OpenAPI不支持的type:null或const。修复发现的问题，输出完整JSON，不加解释。',
  '第三轮稳定性检查：保留路径、operationId、schema名称和全部行为，只修复不符合OpenAPI3.0.3或遗漏的字段。确认所有$ref可以解析，所有响应有description，所有路径参数required:true，schema中不用3.1专有关键字。price/stock的课程演示示例不能冒充真实销售数据。重新输出完整JSON。',
  '第四轮真实校验反馈：前三轮通过Swagger Parser结构校验，但AJV编译ErrorResponse失败，错误为 nullable cannot be used without type。请将ErrorResponse.properties.data明确设为 {"type":"object","nullable":true,"enum":[null]}，以表达错误data只能为null。代码审查确认历史Book响应stock可能为null，请仅为Book.properties.stock增加nullable:true；创建与更新请求stock仍必填且不得null。重复ID真实业务码为40903，请修正所有重复ID响应示例；之前40901仅为误写示例。重新输出完整JSON，其余接口保持一致。',
  '第五轮真实复核反馈，仅修正两处文字：BookCreateRequest.stock的description写不接受额外字段，与创建schema的additionalProperties:true冲突；请删去创建stock描述中的额外字段限制，库存更新对象仍拒绝额外字段。BadRequest响应示例的code仍为40001，而图书接口实际一般参数错误为40000，请将该示例改为40000。其他字段、schema及四个操作保留，输出完整JSON。',
];
for (let i = transcript.rounds.length; i < reviews.length; i++) {
  if (reviews[i]) messages.push({ role: 'user', content: reviews[i] + (transcript.rounds.at(-1)?.parseError ? ` 上轮JSON解析失败：${transcript.rounds.at(-1).parseError}。请完整修正括号和逗号。` : '') });
  const res = await fetch(transcript.endpoint, {
    method: 'POST', headers: { Authorization: `Bearer ${key}`, 'Content-Type': 'application/json' },
    body: JSON.stringify({ model: transcript.model, messages, thinking: { type: 'disabled' }, temperature: 0, max_tokens: 12000, response_format: { type: 'json_object' } }),
    signal: AbortSignal.timeout(180000),
  });
  if (!res.ok) throw new Error(`DeepSeek HTTP ${res.status}，未保存可能含敏感内容的错误正文`);
  const payload = await res.json();
  const reply = payload.choices?.[0];
  if (reply?.finish_reason !== 'stop' || !reply.message?.content) throw new Error('模型响应不完整');
  const content = reply.message.content;
  let spec, parseError;
  try { spec = JSON.parse(content); } catch (error) { parseError = error.message; }
  messages.push({ role: 'assistant', content });
  transcript.rounds.push({ round: i + 1, requestId: payload.id, created: payload.created, usage: payload.usage, user: messages.at(-2).content, assistant: content, ...(parseError ? { parseError } : {}) });
  if (spec) await fs.writeFile(path.join(dir, `round-${i + 1}.json`), JSON.stringify(spec, null, 2) + '\n');
  await fs.writeFile(path.join(dir, 'conversation.json'), JSON.stringify({ ...transcript, system: messages[0].content }, null, 2) + '\n');
  console.log(`真实 DeepSeek 第 ${i + 1} 轮完成，finish_reason=stop，JSON解析${spec ? '通过' : '失败，已保存原始响应'}`);
}
if (transcript.rounds.at(-1).parseError) throw new Error('最终JSON解析失败，必须继续修复后才能交付');
await fs.mkdir(path.join(root, 'backend/src/main/resources/static/api/v1'), { recursive: true });
await fs.copyFile(path.join(dir, `round-${transcript.rounds.length}.json`), path.join(root, 'backend/src/main/resources/static/api/v1/openapi.json'));

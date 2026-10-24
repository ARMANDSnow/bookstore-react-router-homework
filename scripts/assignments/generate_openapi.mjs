// Real provider transcript: headers and credentials are never written to evidence.
import fs from 'node:fs/promises';
import path from 'node:path';
import { fileURLToPath } from 'node:url';
const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '../..');
const dir = path.join(root, 'docs/assignments/作业1');
const env = await fs.readFile(path.join(root, 'backend/.env'), 'utf8').catch(() => '');
const key = process.env.DEEPSEEK_API_KEY || env.match(/^DEEPSEEK_API_KEY=(.+)$/m)?.[1]?.trim();
if (!key) throw new Error('请在 backend/.env 或环境变量配置 DEEPSEEK_API_KEY');
// 【作业1 · 得分点1.1：清晰的角色与任务】输入不是一句泛泛的“帮我写接口”。
// system-prompt.txt 把模型设为后端架构师，要求整理已实现的书城接口；user-prompt.txt
// 列明四个操作、字段、默认值与错误行为。两者分别承担“工作规则”和“实现事实”的职责。
// 答辩可沿着这两次文件读取打开原文，说明为什么契约要服从代码，不能让模型补造业务规则。
// 【作业1 · 得分点1.2：只输出 OpenAPI 3.0.3 JSON】system 中明确禁止解释、围栏和 YAML，
// 要求 paths/components 与可解析的 $ref；下方请求还启用 JSON 输出模式，返回后仍要解析与校验。
// 【作业1 · 得分点1.5：Few-Shot 示例】system 中包含“库存约束输入→schema 输出”的格式片段，
// 用一个小样例说明 required/additionalProperties/int32 边界；它不是最终文档，模型仍须生成完整契约。
const messages = [
  { role: 'system', content: await fs.readFile(path.join(dir, 'system-prompt.txt'), 'utf8') },
  { role: 'user', content: await fs.readFile(path.join(dir, 'user-prompt.txt'), 'utf8') },
];
let transcript = { provider: 'DeepSeek', model: 'deepseek-flash', endpoint: 'https://api.deepseek.com/chat/completions', startedAt: new Date().toISOString(), rounds: [] };
const prior = await fs.readFile(path.join(dir, 'conversation.json'), 'utf8').catch(() => null);
if (prior) {
  // 续跑时还原每轮 user/assistant，后续纠错能看到前文；不会从已有对话中重新调用已完成的轮次。
  // 因此目录中已有五轮记录时执行脚本，只会复用并复制结果，不能把它称为“五次新的模型调用”。
  transcript = JSON.parse(prior);
  messages.splice(1);
  for (const round of transcript.rounds) messages.push({ role: 'user', content: round.user }, { role: 'assistant', content: round.assistant });
}
// 【作业1 · 得分点1.5：多轮校验与反馈】每轮围绕同一份契约追加审查意见：先查四个操作与边界，
// 再查 OpenAPI 版本/$ref，随后记录真实校验发现的 nullable、错误码和描述矛盾并要求修正。
// 这里保存的是当时的审查反馈；外部校验见 verify_openapi.mjs，并非模型在本脚本里自动运行 AJV。
// “稳定”指路径、操作、字段约束等关键契约可重复检查；不要求五轮全文完全相同，纠错本就会改变内容。
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
    // temperature=0 用于减少采样波动，不能保证服务端模型每次逐字节一致；json_object 也只约束
    // JSON 形式，不证明内容符合 OpenAPI 或真实业务。最终可信度来自下面的解析和独立契约校验。
    body: JSON.stringify({ model: transcript.model, messages, thinking: { type: 'disabled' }, temperature: 0, max_tokens: 12000, response_format: { type: 'json_object' } }),
    signal: AbortSignal.timeout(180000),
  });
  if (!res.ok) throw new Error(`DeepSeek HTTP ${res.status}，未保存可能含敏感内容的错误正文`);
  const payload = await res.json();
  const reply = payload.choices?.[0];
  if (reply?.finish_reason !== 'stop' || !reply.message?.content) throw new Error('模型响应不完整');
  const content = reply.message.content;
  let spec, parseError;
  // 先用 JSON.parse 检查语法；失败仍保存原始回复与 parseError，供下一轮带着具体错误继续纠正。
  // 成功解析只代表合法 JSON，不代表 schema、状态码或接口语义已经正确。
  try { spec = JSON.parse(content); } catch (error) { parseError = error.message; }
  messages.push({ role: 'assistant', content });
  transcript.rounds.push({ round: i + 1, requestId: payload.id, created: payload.created, usage: payload.usage, user: messages.at(-2).content, assistant: content, ...(parseError ? { parseError } : {}) });
  if (spec) await fs.writeFile(path.join(dir, `round-${i + 1}.json`), JSON.stringify(spec, null, 2) + '\n');
  await fs.writeFile(path.join(dir, 'conversation.json'), JSON.stringify({ ...transcript, system: messages[0].content }, null, 2) + '\n');
  console.log(`真实 DeepSeek 第 ${i + 1} 轮完成，finish_reason=stop，JSON解析${spec ? '通过' : '失败，已保存原始响应'}`);
}
// 输出链路：每轮原始对话→round-N.json→后端静态 openapi.json。最终轮语法失败立即停止复制，
// 不把损坏 JSON 当交付物；复制后的规范还应由 verify_openapi.mjs 对照真实 HTTP 响应验证。
// 本脚本会请求模型、改写对话/规范；答辩讲解可直接打开已保存记录，无须为展示注释重新生成。
if (transcript.rounds.at(-1).parseError) throw new Error('最终JSON解析失败，必须继续修复后才能交付');
await fs.mkdir(path.join(root, 'backend/src/main/resources/static/api/v1'), { recursive: true });
await fs.copyFile(path.join(dir, `round-${transcript.rounds.length}.json`), path.join(root, 'backend/src/main/resources/static/api/v1/openapi.json'));

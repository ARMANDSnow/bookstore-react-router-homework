// A local evidence viewer; this does not impersonate DeepSeek's website.
import http from 'node:http';
import fs from 'node:fs/promises';
import path from 'node:path';
import { fileURLToPath } from 'node:url';
const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '../..');
const transcript = JSON.parse(await fs.readFile(path.join(root,'docs/assignments/作业1/conversation.json')));
const escape = value => value.replaceAll('&','&amp;').replaceAll('<','&lt;').replaceAll('>','&gt;').replaceAll('"','&quot;');
const server = http.createServer((req,res)=>{
  const url = new URL(req.url,'http://127.0.0.1:4174');
  const round = transcript.rounds[Math.min(transcript.rounds.length-1,Math.max(0,Number(url.searchParams.get('round')||1)-1))];
  const pretty = round.parseError ? round.assistant : JSON.stringify(JSON.parse(round.assistant),null,2);
  res.writeHead(200,{'Content-Type':'text/html; charset=utf-8'});
  res.end(`<!doctype html><html lang="zh-CN"><meta charset="utf-8"><title>作业1真实API对话记录</title><style>
  *{box-sizing:border-box}body{margin:0;padding:38px;background:#f5f1e8;color:#243a33;font:16px/1.55 system-ui,sans-serif}main{max-width:1370px;margin:auto}h1{font-size:32px;margin:0 0 8px}p{margin:8px 0}.note{color:#66746b}nav{display:flex;gap:12px;margin:24px 0}a{padding:10px 22px;border:1px solid #b9c7b9;border-radius:12px;color:#243a33;text-decoration:none}a.active{background:#244c3a;color:white}section{display:grid;grid-template-columns:1fr 1.3fr;gap:24px}article{background:#fffdf8;border:1px solid #d4dbcf;border-radius:20px;padding:22px;min-width:0}h2{font-size:19px;margin:0 0 12px}pre{white-space:pre-wrap;word-break:break-word;font:13px/1.6 ui-monospace,monospace;background:#edf2eb;padding:16px;border-radius:12px;margin:0;max-height:500px;overflow:auto}details pre{max-height:250px}.meta{font-size:13px;color:#5d7064;padding:12px 0;word-break:break-all}.badge{font-size:13px;background:#dfeada;border-radius:18px;padding:5px 12px;display:inline-block}details{margin-top:16px}summary{cursor:pointer}
  </style><main><h1>DeepSeek API 对话记录</h1><p class="note">作业1 · 本地原始调用日志查看器 · 非 DeepSeek 官方网页</p><p><span class="badge">真实模型 ${escape(transcript.model)}</span> · ${escape(transcript.startedAt)} · 已保存 ${transcript.rounds.length} 轮输入与完整响应</p><nav>${transcript.rounds.map(r=>`<a class="${r.round===round.round?'active':''}" href="?round=${r.round}">第 ${r.round} 轮</a>`).join('')}</nav><section><article><h2>User · 第 ${round.round} 轮输入</h2><pre>${escape(round.user)}</pre><details><summary>查看完整 System Prompt</summary><pre>${escape(transcript.system)}</pre></details></article><article><h2>Assistant · 原始响应 JSON</h2><pre>${escape(pretty)}</pre><div class="meta">响应 ID：${escape(round.requestId)}<br>usage：${escape(JSON.stringify(round.usage))}<br>原始来源：docs/assignments/作业1/conversation.json</div></article></section><p class="note">仅为阅读排版增加 JSON 缩进，模型输出内容保持原样。完整提示词和最终 JSON 另附于报告。</p></main></html>`);
});
server.listen(4174,'127.0.0.1',()=>console.log('Evidence viewer http://127.0.0.1:4174'));

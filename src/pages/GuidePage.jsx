import {useEffect,useRef,useState} from 'react';
import {Link} from 'react-router-dom';
import {ArrowUpOutlined,CompassOutlined} from '@ant-design/icons';
import {askGuide} from '../api/bookstoreApi.js';
import './assistant.css';
import './guide.css';
const examples=['推荐一本学习微服务的书，拆了塑封不喜欢还能退吗？','想学Python，适合从哪本书开始？','书已经拆封，但发现缺页了，怎么办？'];
export default function GuidePage(){
 const [input,setInput]=useState(''),[turns,setTurns]=useState([]),[busy,setBusy]=useState(false);const running=useRef(null),log=useRef(null),nearBottom=useRef(true);
 useEffect(()=>()=>running.current?.abort(),[]);
 useEffect(()=>{if(nearBottom.current&&log.current)log.current.scrollTop=log.current.scrollHeight;},[turns,busy]);
 async function send(question=input,retryIndex=null){
  if(running.current||!question.trim())return;const controller=new AbortController();running.current=controller;setBusy(true);if(retryIndex===null)setInput('');
  const target=retryIndex??turns.length;
  const history=turns.slice(0,target).filter(t=>t.reply).flatMap(t=>[{role:'user',content:t.question},{role:'assistant',content:t.reply.answer.slice(0,4000)}]).slice(-20);
  nearBottom.current=true;if(retryIndex!==null)setTurns(t=>t.map((v,i)=>i===target?{question:v.question}:v));else setTurns(t=>[...t,{question}]);
  try{const reply=await askGuide(question,history,controller.signal);if(!controller.signal.aborted)setTurns(t=>t.map((v,i)=>i===target?{...v,reply}:v));}
  catch(e){if(!controller.signal.aborted)setTurns(t=>t.map((v,i)=>i===target?{...v,error:e instanceof TypeError?'连接暂时中断，请确认网络和书城服务可用后重试。':e.message||'暂时无法完成咨询，请重试。'}:v));}
  finally{if(!controller.signal.aborted)setBusy(false);running.current=null;}
 }
 return <div className="assistant-page guide-page">
  <header className="assistant-heading"><div><span className="assistant-eyebrow">选到好书，也买得安心</span><h1>从想读什么，聊到放心带走。</h1><p>查本店书目，读服务条款，把选书与售后一起问明白。</p></div><Link className="guide-policy-link" to="/policies">完整服务政策 ↗</Link></header>
  <div className="guide-workspace">
   <aside className="assistant-sidebar guide-sidebar"><div className="assistant-guide-icon"><CompassOutlined/></div><h2>一场有依据的选书对话。</h2><p>告诉我兴趣、学习目标，或你担心的退换问题。</p><div className="guide-examples">{examples.map(q=><button type="button" key={q} disabled={busy} onClick={()=>send(q)}>{q}<span aria-hidden="true">↗</span></button>)}</div><Link to="/assistant">想查库存或参考报价？前往阅读助手 ↗</Link></aside>
   <section className="assistant-conversation guide-conversation" aria-label="与购书向导对话">
    <div className="assistant-chat-header"><div><span className="assistant-avatar"><CompassOutlined/></span><div><strong>知页购书向导</strong><small>陪你选书，也帮你看懂条件</small></div></div><button type="button" disabled={busy||!turns.length} onClick={()=>{setTurns([]);setInput('');}}>开启新对话</button></div>
    <div className="assistant-messages guide-messages" role="log" aria-live="polite" aria-busy={busy} ref={log} onScroll={()=>{const e=log.current;nearBottom.current=e.scrollHeight-e.scrollTop-e.clientHeight<60;}}>
     {!turns.length&&<div className="assistant-welcome"><span aria-hidden="true">✳</span><h2>先说说，你想读些什么。</h2><p>一本书的推荐，或者一个售后问题。<br/>我们从这里开始。</p></div>}
     {turns.map((turn,index)=><div className="guide-turn" key={index}><p className="assistant-question">{turn.question}</p>{turn.reply?<div className="assistant-answer"><span className="assistant-answer-label">购书向导</span><p className="guide-answer-text">{turn.reply.answer}</p>
      {!!turn.reply.books.length&&<div className="guide-book-results">{turn.reply.books.map(b=><Link key={b.id} to={`/books/${encodeURIComponent(b.id)}`}><img referrerPolicy="no-referrer" src={b.image} alt=""/><span><strong>{b.title}</strong><small>{b.author} · ¥{Number(b.price).toFixed(2)} · {b.stock==null?'库存待确认':`库存${b.stock}本`}</small></span><span aria-hidden="true">↗</span></Link>)}</div>}
      {!!turn.reply.policyChunks.length&&<details className="guide-sources"><summary>查看政策依据 · {turn.reply.policyChunks.length} 条</summary>{turn.reply.policyChunks.map(c=><article key={c.id}><strong>{c.title} · {c.id}</strong><p>{c.text}</p><small>退换货与会员政策 · 更新于 {c.version?.replace(/^course-/,'')}</small></article>)}</details>}
      {!!turn.reply.steps.length&&<details className="guide-trace"><summary>查看行动记录 · {turn.reply.steps.length} 个步骤</summary><ol>{turn.reply.steps.map(s=><li key={s.index}><div><span>Thought · 行动目的</span><p>{s.thought}</p></div><div><span>Action · 调用工具</span><code>{s.action}({JSON.stringify({query:s.arguments.query})})</code></div><div><span>Observation · 查询结果</span><p>{s.observation.ok?s.action==='search_book_catalog'?s.observation.hasMatches?`本店匹配 ${s.observation.books.length} 本：${s.observation.books.map(b=>b.title).join('、')}`:s.observation.message:s.observation.matched?`政策依据：${s.observation.chunks.map(c=>`${c.title}（${c.id}）`).join('、')}`:s.observation.message:s.observation.message}</p></div></li>)}</ol></details>}
     </div>:turn.error?<div className="assistant-error" role="alert"><strong>咨询暂时无法完成</strong><p>{turn.error}</p><button disabled={busy} onClick={()=>send(turn.question,index)}>重试这个问题</button><button disabled={busy} onClick={()=>{setInput(turn.question);}}>编辑问题</button></div>:<div className="assistant-thinking" role="status">正在查找书目与相关依据…</div>}</div>)}
    </div>
    <form className="assistant-composer" onSubmit={e=>{e.preventDefault();send();}}><label className="search-label" htmlFor="guide-input">向购书向导提问</label><textarea id="guide-input" disabled={busy} value={input} maxLength={1200} rows={2} placeholder="想学微服务，也想知道拆封后的退换条件…" onChange={e=>setInput(e.target.value)} onKeyDown={e=>{if(e.key==='Enter'&&!e.shiftKey&&!e.nativeEvent.isComposing&&e.keyCode!==229){e.preventDefault();send();}}}/><div><span>Enter 发送 · Shift + Enter 换行 <small>{input.length}/1200</small></span><button type="submit" aria-label="发送购书问题" disabled={busy||!input.trim()}><ArrowUpOutlined/></button></div></form>
   </section>
  </div>
 </div>;
}

import { useEffect, useRef, useState } from 'react';
import { Link } from 'react-router-dom';
import { Alert, Button, Skeleton } from 'antd';
import { ArrowRightOutlined, FileTextOutlined, SafetyOutlined } from '@ant-design/icons';
import { queryPolicies, policyDocument } from '../api/bookstoreApi.js';
import './policy.css';
const suggestions=['图书拆了塑封，但不喜欢了，还能退吗？','发现缺页，已经拆封了怎么处理？','会员积分怎么获得和使用？'];
export default function PolicyPage(){
 const [input,setInput]=useState('');const [result,setResult]=useState(null);const [error,setError]=useState(null);const [busy,setBusy]=useState(false);const [document,setDocument]=useState(null);const [documentError,setDocumentError]=useState(false);const [retry,setRetry]=useState(0);const running=useRef(null);
 useEffect(()=>{const abort=new AbortController();setDocumentError(false);policyDocument(abort.signal).then(setDocument).catch(()=>{if(!abort.signal.aborted)setDocumentError(true);});return()=>abort.abort();},[retry]);
 useEffect(()=>()=>running.current?.abort(),[]);
 async function search(question=input){
  if(running.current||!question.trim())return;const abort=new AbortController();running.current=abort;setInput(question);setBusy(true);setError(null);setResult(null);
  try{setResult(await queryPolicies(question.trim(),abort.signal));}catch(e){if(!abort.signal.aborted)setError(e.message||'政策暂时无法查询，请重试。');}finally{if(!abort.signal.aborted)setBusy(false);running.current=null;}
 }
 return <div className="policy-page">
  <header className="policy-heading"><div><p className="eyebrow">安心选书 · 服务政策</p><h1>买之前的顾虑，<br/>在这里问明白。</h1><p>退换条件、质量问题、会员权益，查看有据可查的条款。</p></div><div className="policy-heading-mark" aria-hidden="true"><SafetyOutlined/></div></header>
  <div className="policy-layout">
   <aside className="policy-sidebar"><FileTextOutlined/><h2>先读条件，再做决定。</h2><p>每次查询都会附上政策原文。请留意期限、图书状态与例外条件。</p><div className="policy-note"><strong>课程演示政策</strong><p>这些条款用于作业问答练习。页面提供咨询，不执行退款、积分抵扣或客服审批。</p></div><Link to="/books">回书架继续选书 <ArrowRightOutlined/></Link></aside>
   <section className="policy-query" aria-label="查询服务政策">
    <form onSubmit={e=>{e.preventDefault();search();}}><label htmlFor="policy-question">你想了解什么？</label><div className="policy-search-row"><input id="policy-question" value={input} disabled={busy} onChange={e=>setInput(e.target.value)} maxLength={600} placeholder="例如：已拆封但发现缺页，可以换货吗？" onKeyDown={e=>{if(e.key==='Enter'&&e.nativeEvent.isComposing)e.preventDefault();}}/><Button htmlType="submit" type="primary" loading={busy} disabled={!input.trim()}>查找条款</Button></div></form>
    <div className="policy-suggestions">{suggestions.map(q=><button key={q} type="button" disabled={busy} onClick={()=>search(q)}>{q}<span aria-hidden="true">↗</span></button>)}</div>
    <div className="policy-results" aria-live="polite" aria-busy={busy}>
     {busy?<div role="status"><p>正在查找相关政策依据…</p><Skeleton active paragraph={{rows:4}}/></div>:error?<Alert type="error" showIcon title="查询暂时无法完成" description={error} action={<Button onClick={()=>search()}>重试</Button>}/>:result?<><p className="policy-result-label">{result.matched?'相关条款 · 请结合条件阅读':'本政策暂无相关依据'}</p><h2>{result.query}</h2>{result.matched?result.chunks.map(chunk=><article key={chunk.id} className="policy-clause"><p className="policy-clause-id">课程政策 · {chunk.id}</p><h3>{chunk.title}</h3><p>{chunk.text}</p><small>来源：{chunk.source} · {chunk.version}</small></article>):<p>{result.message}</p>}</>:<div className="policy-welcome"><span aria-hidden="true">✳</span><h2>不确定，就先问一问。</h2><p>从一句自然的提问开始，找到与你的问题相关的完整条款。</p></div>}
    </div>
   </section>
  </div>
  <section className="policy-full" aria-label="课程政策全文"><h2>也可以，完整读一遍。</h2>{document?<details><summary>查看退换货与会员政策全文 <span>course-2026-10-06</span></summary><div className="policy-full-body">{document.chunks.map(c=><section key={c.id}><h3>{c.title}</h3><p>{c.text}</p></section>)}</div></details>:documentError?<p>政策全文暂时无法加载。<Button type="link" onClick={()=>setRetry(v=>v+1)}>重新加载</Button></p>:<p role="status">正在载入政策全文…</p>}</section>
 </div>;
}

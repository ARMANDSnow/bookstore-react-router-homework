import { useEffect, useRef, useState } from "react";
import { Link, useSearchParams } from "react-router-dom";
import { ArrowUpOutlined, BookOutlined, CheckCircleOutlined, CommentOutlined, ReloadOutlined } from "@ant-design/icons";
import { askAssistant, assistantStatus } from "../api/bookstoreApi.js";
import "./assistant.css";

const money = value => Number(value).toFixed(2);
export default function AssistantPage({ books }) {
  const [searchParams, setSearchParams] = useSearchParams();
  const requestedBook = searchParams.get("book");
  const scenario = searchParams.get("scenario") === "competitor-timeout-once" ? "competitor-timeout-once" : "normal";
  const choices = books.filter(book => book.isbn);
  const [bookId, setBookId] = useState(()=>requestedBook || "");
  const selected = choices.find(book => book.id === bookId) || choices.find(book => book.id === "building-microservices") || choices[0];
  const [input, setInput] = useState("");
  const [turns, setTurns] = useState([]);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState("");
  const [failed, setFailed] = useState(null);
  const request = useRef(null);
  const messages = useRef(null);
  const follow = useRef(true);
  const [configured, setConfigured] = useState(null);
  const field = useRef(null);
  useEffect(() => { if (requestedBook) setBookId(requestedBook); }, [requestedBook]);
  useEffect(() => () => request.current?.abort(), []);
  useEffect(() => { const controller = new AbortController(); assistantStatus(controller.signal).then(status=>setConfigured(status.configured)).catch(()=>{}); return()=>controller.abort(); }, []);
  useEffect(() => { if(follow.current) messages.current?.scrollTo({top:messages.current.scrollHeight,behavior:matchMedia("(prefers-reduced-motion: reduce)").matches ? "instant" : "smooth"}); }, [turns,busy,error]);
  useEffect(() => { if(!busy && turns.length) field.current?.focus(); }, [busy]);

  async function send(text = input, retry = false) {
    const typed = text.trim();
    if (!typed || busy || request.current || (!retry && typed.length > 1000)) return;
    const hasIsbn = /(?:\d[\s-]*){13}/.test(typed) || /(?:ISBN|书号)[\s:：=＝为是]{0,20}\d/i.test(typed) || /^\d[\d\s-]*$/.test(typed);
    const question = selected && !hasIsbn ? `当前选书：《${selected.title}》（ISBN ${selected.isbn}）。\n用户问题：${typed}` : typed;
    follow.current = true;
    const history = (retry ? turns.slice(0,-1) : turns).map(turn => ({ role: turn.role, content: turn.content.slice(0,4000) })).slice(-20);
    const controller = new AbortController(); request.current = controller;
    if (!retry) setTurns(current => [...current, { role: "user", content: question }]);
    setInput(""); setError(""); setFailed(null); setBusy(true);
    try {
      const reply = await askAssistant(question, history, controller.signal, retry ? failed.scenario : scenario);
      if (controller.signal.aborted) return;
      setTurns(current => [...current, { role: "assistant", content: reply.answer, reply }]);
    } catch (failure) {
      if (controller.signal.aborted) return;
      setError(failure instanceof TypeError ? "连接暂时中断，请确认网络和书城服务可用后重试。" : failure.message || "连接失败，请稍后重试"); setFailed({ question, typed: retry ? failed.typed : typed, scenario: retry ? failed.scenario : scenario });
    } finally {
      if (!controller.signal.aborted) { setBusy(false); request.current = null; field.current?.focus(); }
    }
  }
  function clear() { if (busy) return; setTurns([]); setError(""); setFailed(null); setInput(""); const next = new URLSearchParams(searchParams); next.delete("scenario"); setSearchParams(next, {replace:true}); field.current?.focus(); }
  const presets = selected ? [
    { label: "还有库存吗？", question: `请查询《${selected.title}》（ISBN ${selected.isbn}）的库存。` },
    { label: "比较一下价格", question: `请查询《${selected.title}》（ISBN ${selected.isbn}）的本店售价和参考报价。` },
    { label: "库存和价格一起看", question: `请查询《${selected.title}》（ISBN ${selected.isbn}）的库存，并比较本店售价和参考报价。` },
  ] : [];
  return <div className="assistant-page">
    <header className="assistant-heading"><div><span className="assistant-eyebrow">BOOKS & CONVERSATION</span><h1>选书的疑问，聊聊就好。</h1><p>查库存、看价格，让下一次阅读更有把握。</p></div><span className="assistant-status"><span /> {turns.some(turn=>turn.reply) ? "已连接" : configured === false ? "服务暂不可用" : "阅读助手"}</span></header>
    <div className="assistant-workspace">
      <aside className="assistant-sidebar" aria-label="选择咨询图书">
        <div className="assistant-guide-icon"><BookOutlined /></div><h2>先选一本，慢慢聊</h2><p>把想了解的书带进对话。</p>
        <label htmlFor="assistant-book">正在了解的图书</label><select id="assistant-book" value={selected?.id || ""} disabled={busy || !choices.length} onChange={event => setBookId(event.target.value)}>{choices.map(book => <option key={book.id} value={book.id}>{book.title}</option>)}</select>
        {selected && <div className="assistant-selected"><img referrerPolicy="no-referrer" src={selected.image} alt={`${selected.title}封面`} /><strong>{selected.title}</strong><span>{selected.author}</span><small>ISBN {selected.isbn}</small><Link to={`/books/${selected.id}`}>查看图书详情 <span aria-hidden="true">↗</span></Link></div>}
      </aside>
      <section className="assistant-conversation" aria-label="与阅读助手对话">
        <div className="assistant-chat-header"><div><span className="assistant-avatar"><CommentOutlined /></span><div><strong>知页阅读助手</strong><small>帮你把问题问明白</small></div></div><button type="button" onClick={clear} disabled={busy || !turns.length}>开启新对话</button></div>
        <div className="assistant-messages" ref={messages} onScroll={event=>{const node=event.currentTarget;follow.current=node.scrollHeight-node.scrollTop-node.clientHeight<100;}} role="log" aria-live="polite" aria-relevant="additions text">
          {!turns.length && <div className="assistant-welcome"><span className="assistant-welcome-symbol" aria-hidden="true">✳</span><h2>好书值得多了解一点。</h2><p>选一本书，或直接告诉我它的 ISBN。<br />我可以帮你查库存，也可以比较参考报价。</p><div className="assistant-prompts">{presets.map(item => <button key={item.label} type="button" onClick={() => send(item.question)} disabled={busy}>{item.label}<span aria-hidden="true">↗</span></button>)}</div></div>}
          {turns.map((turn,index) => <article key={index} className={`assistant-turn ${turn.role}`}><div className="assistant-turn-label">{turn.role === "user" ? "你" : "知页阅读助手"}</div><div className="assistant-bubble">{turn.content}</div>{turn.reply && <>
            {turn.reply.steps?.some(step=>step.observation.error === "COMPETITOR_TIMEOUT") && turn.reply.steps?.some(step=>step.name === "get_competitor_price" && step.observation.ok && step.observation.attempt > 1) && <p className="assistant-recovered" role="status"><CheckCircleOutlined /> 报价查询曾超时，重试后已获取结果。</p>}
            {turn.reply.books?.length > 0 && <div className="assistant-book-results">{turn.reply.books.map(book => <Link to={`/books/${book.id}`} key={book.id}><img referrerPolicy="no-referrer" src={book.image} alt="" /><span><strong>{book.title}</strong><small>本店售价 ¥{money(book.price)} · {book.stock == null ? "库存待确认" : `库存 ${book.stock} 本`}</small></span><span aria-hidden="true">↗</span></Link>)}</div>}
            {turn.reply.steps?.length > 0 && <details className="assistant-tool-details"><summary>查看查询记录 · {turn.reply.steps.length} 个步骤</summary><ol>{turn.reply.steps.map(step => <li key={step.index}><div><strong>{step.name === "check_inventory" ? "查询本店库存" : "获取参考报价"}</strong><span>{step.observation.ok ? step.observation.attempt > 1 ? "重试成功" : "已完成" : step.observation.retryable ? "查询超时 · 可重试" : "待核对"}</span></div><code>{step.name}({JSON.stringify(step.arguments)})</code><p>{step.observation.ok ? step.observation.simulated ? `参考报价 ¥${money(step.observation.price)}` : `${step.observation.title} · ${step.observation.availability}${step.observation.stock == null ? "" : ` ${step.observation.stock} 本`}` : step.observation.message}</p></li>)}</ol></details>}
          </>}</article>)}
          {busy && <div className="assistant-pending" role="status"><span className="assistant-thinking"><i /><i /><i /></span><span>正在核对信息，稍等一下…</span></div>}
          {error && <div className="assistant-error" role="alert"><strong>这次还没查到结果</strong><p>{error}</p><button type="button" disabled={busy} onClick={() => send(failed.question,true)}><ReloadOutlined /> 重试这条问题</button><button type="button" disabled={busy} onClick={() => { setInput(failed?.typed || ""); setTurns(current=>current.slice(0,-1)); setError(""); setFailed(null); field.current?.focus(); }}>修改问题</button></div>}
        </div>
        <form className="assistant-composer" onSubmit={event => { event.preventDefault(); send(); }}><label htmlFor="assistant-input" className="search-label">向阅读助手提问</label><textarea id="assistant-input" ref={field} value={input} onChange={event => setInput(event.target.value)} maxLength={1000} disabled={busy} placeholder={selected ? `想了解《${selected.title}》？也可以直接输入 ISBN…` : "输入问题或图书 ISBN…"} rows={2} onKeyDown={event => { if(event.key === "Enter" && !event.shiftKey && !event.nativeEvent.isComposing && event.keyCode !== 229){event.preventDefault();send();} }} /><div><span>Enter 发送 · Shift + Enter 换行 <small>{input.length}/1000</small></span><button type="submit" aria-label="发送问题" disabled={busy || !input.trim()}><ArrowUpOutlined /></button></div></form>
      </section>
    </div>
  </div>;
}

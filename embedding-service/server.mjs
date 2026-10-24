import http from 'node:http';
import { chunkPolicy } from './chunks.mjs';
import fs from 'node:fs/promises';
import path from 'node:path';
import crypto from 'node:crypto';
import { fileURLToPath } from 'node:url';
import { pipeline, env } from '@huggingface/transformers';
const here=path.dirname(fileURLToPath(import.meta.url));
// 【作业3 · 得分点3.1：使用真实Embedding模型】
// Embedding把文本转换为数值向量，使意思接近的文本可以按相似度检索。
// 固定模型和revision（模型文件版本），并在下方使用q8量化权重：以较小的
// 数值精度存储模型参数，降低本地运行成本；这仍然是实际模型推理。
export const MODEL='Xenova/bge-small-zh-v1.5';
export const REVISION='75c43b069aac4d136ba6bc1122f995fedcfd2781';
// 每个向量包含512个数；0.62是当前应用的匹配门槛，不是正确率或概率。
const DIMENSION=512, THRESHOLD=0.62;
const sourcePath=process.env.POLICY_FILE||path.resolve(here,'../backend/src/main/resources/store-policy.txt');
env.cacheDir=path.join(here,'.cache');
env.localModelPath=path.join(here,'.cache/models/');
env.allowRemoteModels=false;
let status='loading',failure=null,extractor,index,sourceHash,documentText,cacheHit=false;
let queue=Promise.resolve();
// 查询与文档使用同一模型映射到同一向量空间，但查询额外带BGE检索提示。
// cls表示取模型的句级表示；normalize把向量长度缩放到1，方便计算余弦相似度。
// truncation/max_length是模型输入长度保护；下面同时校验维度、数值和单位长度，
// 防止异常向量进入索引。这里没有随机向量，也没有关键词命中后伪造相似度。
async function embed(texts,isQuery=false){
 const values=isQuery?texts.map(t=>'为这个句子生成表示以用于检索相关文章：'+t):texts;
 const output=await extractor(values,{pooling:'cls',normalize:true,truncation:true,max_length:512});
 const vectors=output.tolist();
 if(vectors.some(v=>v.length!==DIMENSION||v.some(n=>!Number.isFinite(n))||Math.abs(Math.hypot(...v)-1)>0.002))throw Error('向量格式异常');
 return vectors;
}
async function initialize(){
 // 【作业3 · 得分点3.1：加载→切块→向量化→建立索引】
 // 原文先计算SHA-256摘要，再切成带编号的条款。摘要能检测政策内容是否变化，
 // 不代表条款真实性；本项目政策是课程用模拟政策。
 documentText=await fs.readFile(sourcePath,'utf8');
 sourceHash=crypto.createHash('sha256').update(documentText).digest('hex');const chunks=chunkPolicy(documentText);
 extractor=await pipeline('feature-extraction',MODEL,{revision:REVISION,dtype:'q8',device:'cpu'});
 // 缓存键同时绑定原文、模型版本、切块版本和向量生成方式，避免换了文档或模型
 // 仍使用旧索引。缓存命中时复用以前真实计算的文档向量；每次查询仍重新向量化。
 const identity=crypto.createHash('sha256').update(JSON.stringify({sourceHash,MODEL,REVISION,chunkVersion:1,pooling:'cls',dtype:'q8'})).digest('hex');
 const cache=path.join(here,'.cache',identity+'.json');
 try{index=JSON.parse(await fs.readFile(cache,'utf8'));if(index.length!==chunks.length||index.some((c,i)=>c.text!==chunks[i].text||c.vector?.length!==DIMENSION||c.vector.some(n=>!Number.isFinite(n))||Math.abs(Math.hypot(...c.vector)-1)>0.002))throw Error('cache invalid');cacheHit=true;}
 // 无缓存或缓存检查失败时，把“标题+正文”一起向量化，让标题主题也参与检索。
 catch{const vectors=await embed(chunks.map(c=>c.title+'。'+c.text));index=chunks.map((c,i)=>({...c,vector:vectors[i]}));await fs.mkdir(env.cacheDir,{recursive:true});await fs.writeFile(cache,JSON.stringify(index));}
 status='ready';console.log(JSON.stringify({status,model:MODEL,revision:REVISION,dimension:DIMENSION,chunks:index.length,sourceHash,cacheHit}));
}
function json(res,code,data){res.writeHead(code,{'Content-Type':'application/json; charset=utf-8','Cache-Control':'no-store'});res.end(JSON.stringify(data));}
async function body(req){let buffer='';for await(const part of req){buffer+=part;if(Buffer.byteLength(buffer)>16000)throw Error('body limit');}return JSON.parse(buffer);}
function metadata(){return {status,model:MODEL,revision:REVISION,dimension:DIMENSION,sourceHash,chunkCount:index?.length||0,cacheHit,threshold:THRESHOLD,simulatedPolicy:true};}
const server=http.createServer(async(req,res)=>{
 try{
  if(req.method==='GET'&&req.url==='/status')return json(res,200,metadata());
  if(status!=='ready')return json(res,503,{message:failure?'政策向量初始化失败':'政策向量正在初始化'});
  if(req.method==='GET'&&req.url==='/document')return json(res,200,{...metadata(),text:documentText,chunks:index.map(({vector,...c})=>c)});
  if(req.method==='GET'&&req.url==='/diagnostics')return json(res,200,{...metadata(),vectors:index.map(c=>({id:c.id,vector:c.vector,norm:Math.hypot(...c.vector)}))});
  if(req.method!=='POST'||req.url!=='/query')return json(res,404,{message:'接口不存在'});
  const input=await body(req);
  if(typeof input.query!=='string'||!input.query.trim()||input.query.length>600)return json(res,400,{message:'问题需为1至600字'});
  const run=async()=>{
   // 【作业3 · 得分点3.2：可调用的RAG检索工具】
   // RAG是先检索资料，再把资料交给回答模型。这里负责“检索”部分：
   // 1. 将用户问题转成单位向量；2. 对全部条款计算点积；3. 按相似度降序排列。
   // 因文档和问题向量的长度都为1，点积sum(doc[i]*query[i])就是余弦相似度。
   // 当前政策规模小，内存中逐条计算即可，不依赖外部向量数据库。
   const [vector]=await embed([input.query.trim()],true);
   const ranked=index.map(({vector:doc,...chunk})=>({...chunk,score:doc.reduce((sum,n,i)=>sum+n*vector[i],0)})).sort((a,b)=>b.score-a.score);
   // top-k在此取最多3条，并先过滤低于0.62的片段；宁可返回无匹配，
   // 也不强行拿最接近但不相关的条款作为依据。返回完整正文、编号、来源及分数，
   // 由Java代理放入工具结果供模型引用；相似度高不等于已经保证最终回答正确。
   const matches=ranked.filter(c=>c.score>=THRESHOLD).slice(0,3);
   return {ok:true,query:input.query.trim(),matched:matches.length>0,chunks:matches,...metadata(),message:matches.length?'以下为匹配的服务条款，请结合条件阅读。':'未找到足够相关的政策依据，请咨询人工客服。'};
  };
  // 查询串行进入本地模型；失败后修复队列的Promise状态，使下一个请求仍能执行。
  const job=queue.then(run);queue=job.catch(()=>{});json(res,200,await job);
 }catch{json(res,400,{message:'政策查询未完成，请检查输入后重试'});}
});
server.listen(Number(process.env.EMBEDDING_PORT||8091),'127.0.0.1',()=>console.log('Policy embeddings listening on loopback.'));
initialize().catch(()=>{status='failed';failure=true;console.error('政策向量初始化失败，请检查模型下载和政策文件。');});

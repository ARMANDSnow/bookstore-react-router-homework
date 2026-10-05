import http from 'node:http';
import { chunkPolicy } from './chunks.mjs';
import fs from 'node:fs/promises';
import path from 'node:path';
import crypto from 'node:crypto';
import { fileURLToPath } from 'node:url';
import { pipeline, env } from '@huggingface/transformers';
const here=path.dirname(fileURLToPath(import.meta.url));
export const MODEL='Xenova/bge-small-zh-v1.5';
export const REVISION='75c43b069aac4d136ba6bc1122f995fedcfd2781';
const DIMENSION=512, THRESHOLD=0.62;
const sourcePath=process.env.POLICY_FILE||path.resolve(here,'../backend/src/main/resources/store-policy.txt');
env.cacheDir=path.join(here,'.cache');
env.localModelPath=path.join(here,'.cache/models/');
env.allowRemoteModels=false;
let status='loading',failure=null,extractor,index,sourceHash,documentText,cacheHit=false;
let queue=Promise.resolve();
async function embed(texts,isQuery=false){
 const values=isQuery?texts.map(t=>'为这个句子生成表示以用于检索相关文章：'+t):texts;
 const output=await extractor(values,{pooling:'cls',normalize:true,truncation:true,max_length:512});
 const vectors=output.tolist();
 if(vectors.some(v=>v.length!==DIMENSION||v.some(n=>!Number.isFinite(n))||Math.abs(Math.hypot(...v)-1)>0.002))throw Error('向量格式异常');
 return vectors;
}
async function initialize(){
 documentText=await fs.readFile(sourcePath,'utf8');
 sourceHash=crypto.createHash('sha256').update(documentText).digest('hex');const chunks=chunkPolicy(documentText);
 extractor=await pipeline('feature-extraction',MODEL,{revision:REVISION,dtype:'q8',device:'cpu'});
 const identity=crypto.createHash('sha256').update(JSON.stringify({sourceHash,MODEL,REVISION,chunkVersion:1,pooling:'cls',dtype:'q8'})).digest('hex');
 const cache=path.join(here,'.cache',identity+'.json');
 try{index=JSON.parse(await fs.readFile(cache,'utf8'));if(index.length!==chunks.length||index.some((c,i)=>c.text!==chunks[i].text||c.vector?.length!==DIMENSION||c.vector.some(n=>!Number.isFinite(n))||Math.abs(Math.hypot(...c.vector)-1)>0.002))throw Error('cache invalid');cacheHit=true;}
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
   const [vector]=await embed([input.query.trim()],true);
   const ranked=index.map(({vector:doc,...chunk})=>({...chunk,score:doc.reduce((sum,n,i)=>sum+n*vector[i],0)})).sort((a,b)=>b.score-a.score);
   const matches=ranked.filter(c=>c.score>=THRESHOLD).slice(0,3);
   return {ok:true,query:input.query.trim(),matched:matches.length>0,chunks:matches,...metadata(),message:matches.length?'以下为检索到的课程政策原文，请结合条件阅读。':'未找到足够相关的政策依据，请咨询人工客服。'};
  };
  const job=queue.then(run);queue=job.catch(()=>{});json(res,200,await job);
 }catch{json(res,400,{message:'政策查询未完成，请检查输入后重试'});}
});
server.listen(Number(process.env.EMBEDDING_PORT||8091),'127.0.0.1',()=>console.log('Policy embeddings listening on loopback.'));
initialize().catch(()=>{status='failed';failure=true;console.error('政策向量初始化失败，请检查模型下载和政策文件。');});

import fs from 'node:fs/promises';import os from 'node:os';import path from 'node:path';import {fileURLToPath} from 'node:url';import {spawn} from 'node:child_process';import assert from 'node:assert/strict';import crypto from 'node:crypto';
const root=fileURLToPath(new URL('../../',import.meta.url)),temporary=await fs.mkdtemp(path.join(os.tmpdir(),'bookstore-policy-qa-'));
async function run(policy,port){
 const child=spawn(process.execPath,['server.mjs'],{cwd:path.join(root,'embedding-service'),env:{...process.env,EMBEDDING_PORT:String(port),...(policy?{POLICY_FILE:policy}:{})},stdio:'ignore'});
 try{
  const deadline=Date.now()+45000;let status;
  while(Date.now()<deadline){try{status=await(await fetch(`http://127.0.0.1:${port}/status`)).json();if(status.status==='ready')break;if(status.status==='failed')throw Error('模型服务初始化失败');}catch(e){if(child.exitCode!==null)throw e;}await new Promise(r=>setTimeout(r,250));}
  assert.equal(status?.status,'ready');return status;
 }finally{child.kill('SIGTERM');if(child.exitCode===null)await new Promise(r=>child.once('exit',r));}
}
try{
 const warm=await run(null,8092);assert.equal(warm.cacheHit,true);
 const text=await fs.readFile(path.join(root,'backend/src/main/resources/store-policy.txt'),'utf8');const changed=text+'\n## 验收专用附加条款\n验收用条款，确认文档变化触发索引重建。\n';const policy=path.join(temporary,'policy.txt');await fs.writeFile(policy,changed);const rebuilt=await run(policy,8093);assert.equal(rebuilt.cacheHit,false);assert.notEqual(rebuilt.sourceHash,warm.sourceHash);assert.equal(rebuilt.sourceHash,crypto.createHash('sha256').update(changed).digest('hex'));assert.equal(rebuilt.chunkCount,7);assert.equal(warm.chunkCount,6);
 await fs.writeFile(path.join(root,'docs/assignments/evidence/3a-cache.json'),JSON.stringify({testedAt:new Date().toISOString(),warm,rebuilt,checks:['真实模型离线缓存启动','同政策向量缓存复用','政策内容改变后生成新索引','正式政策全文未修改'],complete:true},null,2)+'\n');console.log('真实模型离线缓存复用与政策内容改变触发索引重建通过。');
}finally{await fs.rm(temporary,{recursive:true,force:true});}

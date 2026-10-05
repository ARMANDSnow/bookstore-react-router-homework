import fs from 'node:fs/promises';import path from 'node:path';import crypto from 'node:crypto';import {fileURLToPath} from 'node:url';import {execFileSync} from 'node:child_process';
const model='Xenova/bge-small-zh-v1.5',revision='75c43b069aac4d136ba6bc1122f995fedcfd2781';
const directory=path.join(path.dirname(fileURLToPath(import.meta.url)),'.cache/models',model);
const expected='15b717c382bcb518ba457b93ea6850ede7f4f1cd8937454aa06972366cd19bcc';
await fs.mkdir(path.join(directory,'onnx'),{recursive:true});
for(const name of ['config.json','tokenizer.json','tokenizer_config.json','onnx/model_quantized.onnx']){
 const output=path.join(directory,name);let exists=false;try{exists=(await fs.stat(output)).size>0;}catch{}
 if(!exists){execFileSync('curl',['-fsSL','--retry','2','--connect-timeout','15','--max-time','120',`https://huggingface.co/${model}/resolve/${revision}/${name}`,'-o',output],{stdio:'inherit'});}
}
const weight=await fs.readFile(path.join(directory,'onnx/model_quantized.onnx'));const sha256=crypto.createHash('sha256').update(weight).digest('hex');
if(sha256!==expected)throw Error('模型权重哈希与官方固定版本不符，请删除损坏的缓存文件后重试');
for(const name of ['config.json','tokenizer.json','tokenizer_config.json'])JSON.parse(await fs.readFile(path.join(directory,name),'utf8'));
console.log(JSON.stringify({model,revision,bytes:weight.length,sha256,verified:true}));

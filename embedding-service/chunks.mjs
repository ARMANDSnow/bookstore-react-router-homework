export function chunkPolicy(text){
 const chunks=[...text.matchAll(/^## (.+)\n([\s\S]*?)(?=^## |$(?![\s\S]))/gm)].map((m,index)=>({id:`policy-${index+1}`,title:m[1].trim(),text:m[2].trim(),source:'store-policy.txt',version:'course-2026-10-06'}));
 if(!chunks.length||chunks.some(c=>!c.text||c.text.length>420))throw Error('政策分块为空或过长');
 return chunks;
}

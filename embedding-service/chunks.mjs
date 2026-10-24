// 【作业3 · 得分点3.1：按完整条款切分政策文档】
// 输入是从 store-policy.txt 读取的全文；以每个“## 标题”作为边界，
// 将标题与其后的整段正文保留在同一个片段中，避免把退货条件和例外拆散。
// policy-N 是后续检索、模型引用和前端展示共同使用的片段编号；
// source/version 记录来源和版本，便于答辩时从最终回答追溯到原条款。
// 这里的编号取决于文档中的条款顺序；政策重新排序后需要重新建立索引。
export function chunkPolicy(text){
 const chunks=[...text.matchAll(/^## (.+)\n([\s\S]*?)(?=^## |$(?![\s\S]))/gm)].map((m,index)=>({id:`policy-${index+1}`,title:m[1].trim(),text:m[2].trim(),source:'store-policy.txt',version:'course-2026-10-06'}));
 // 此检查拒绝空文档、空片段和超过420个字符串长度单位的正文，
 // 不会偷偷裁剪政策。模型还有独立的token（模型分词单位）上限，两者不是同一单位。
 if(!chunks.length||chunks.some(c=>!c.text||c.text.length>420))throw Error('政策分块为空或过长');
 return chunks;
}

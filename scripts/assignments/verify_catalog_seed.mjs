import fs from 'node:fs/promises';
import assert from 'node:assert/strict';
const base=process.env.BOOKSTORE_API_BASE||'http://127.0.0.1:8080';
const root=new URL('../../',import.meta.url);
const books=JSON.parse(await fs.readFile(new URL('src/data/Data.json',root)));
const sources=JSON.parse(await fs.readFile(new URL('src/data/bookSources.json',root)));
async function get(path){const response=await fetch(base+path);const body=await response.json();assert.equal(response.status,200);assert.equal(body.code,0);return body.data;}
assert.equal(books.length,16);assert.equal(new Set(books.map(b=>b.id)).size,16);assert.equal(new Set(books.map(b=>b.isbn)).size,16);
const checks=[];
for(const book of books){
 assert.match(book.isbn,/^\d{13}$/);
 assert.equal([...book.isbn].reduce((total,digit,index)=>total+Number(digit)*(index%2?3:1),0)%10,0,book.id);
 const live=await get('/api/books/'+book.id);assert.equal(live.isbn,book.isbn);assert.equal(live.title,book.title);assert.equal(live.author,book.author);
 if(sources[book.id]){assert.equal(sources[book.id].isbn,live.isbn);assert.match(sources[book.id].url,/^https:\/\//);}
 checks.push({id:live.id,title:live.title,isbn:live.isbn,stock:live.stock,price:live.price,source:sources[book.id]||null});
}
const pages=[];for(let page=1;page<=4;page++){const result=await get('/api/books?page='+page+'&size=4');assert.equal(result.totalItems,16);assert.equal(result.items.length,4);pages.push(result);}
assert.equal(new Set(pages.flatMap(p=>p.items.map(b=>b.id))).size,16);
const categoryChecks=[];for(const category of ['tech','design','business','literature']){const result=await get('/api/books?category='+category+'&size=100');assert.equal(result.totalItems,books.filter(b=>b.category===category).length);categoryChecks.push({category,total:result.totalItems});}
const search=await get('/api/books?keyword=Kleppmann');assert.equal(search.items[0].id,'data-intensive-applications');
const evidence={testedAt:new Date().toISOString(),environment:'真实MySQL与HTTP，价格库存为课程值',checks,pages:pages.map(p=>({page:p.page,total:p.totalItems,ids:p.items.map(b=>b.id)})),categoryChecks,search,complete:true};
await fs.writeFile(new URL('docs/assignments/evidence/catalog-real-api.json',root),JSON.stringify(evidence,null,2)+'\n');console.log('16本真实书目的ISBN、详情、四页分页、全部分类及作者搜索验收通过。');

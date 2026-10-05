import fs from 'node:fs/promises';
import assert from 'node:assert/strict';
const root=new URL('../../',import.meta.url);
const task=await taskSpace(11);const page=task.page('p1');
const cases=[];
async function ready(){await page.waitForFunction(()=>document.querySelectorAll('.book-card').length>0&&!document.querySelector('.book-skeleton'),undefined,{timeout:20000});}
async function covers(){await page.evaluate(()=>window.scrollTo(0,document.body.scrollHeight));await page.waitForFunction(()=>[...document.querySelectorAll('.book-card img')].every(i=>i.complete&&i.naturalWidth>0),undefined,{timeout:30000});return page.evaluate(()=>[...document.querySelectorAll('.book-card')].map(c=>({id:c.querySelector('h3').id.replace('book-title-',''),title:c.querySelector('h3').textContent,coverLoaded:c.querySelector('img').naturalWidth>0,overflow:c.scrollWidth>c.clientWidth+1})));}
for(let p=1;p<=4;p++){await page.goto('http://127.0.0.1:5173/books?size=4&page='+p);await ready();const cards=await covers();assert.equal(cards.length,4);assert.ok(cards.every(b=>!b.overflow));cases.push({case:'catalog-page-'+p,cards});}
assert.equal(new Set(cases.flatMap(c=>c.cards.map(b=>b.id))).size,16);
await page.goto('http://127.0.0.1:5173/books?category=design&size=8');await ready();assert.equal(await page.evaluate(()=>document.querySelectorAll('.book-card').length),3);cases.push({case:'design-filter',cards:await covers()});
await page.goto('http://127.0.0.1:5173/books?keyword=Kleppmann');await ready();assert.match(await page.evaluate(()=>document.querySelector('.book-title').textContent),/数据密集型/);cases.push({case:'author-search',cards:await covers()});
for(const id of ['js-advanced','design','thinking-with-type']){
 await page.goto('http://127.0.0.1:5173/books/'+id);await page.waitForFunction(()=>document.querySelector('.detail-info')&&!document.querySelector('.detail-actions .ant-btn-loading'),undefined,{timeout:20000});await page.waitForFunction(()=>document.querySelector('.detail-cover img')?.complete&&document.querySelector('.detail-cover img').naturalWidth>0,undefined,{timeout:30000});
 const detail=await page.evaluate(()=>({title:document.querySelector('.detail-info h1').textContent,body:document.querySelector('.detail-info').textContent,source:[...document.querySelectorAll('.detail-metadata a')].map(a=>({href:a.href,target:a.target,rel:a.rel})),overflow:document.documentElement.scrollWidth>innerWidth+1}));assert.equal(detail.overflow,false);assert.equal(detail.source.length,1);assert.match(detail.body,/课程演示/);assert.doesNotMatch(detail.body,/豆瓣评分/);assert.equal(detail.source[0].target,'_blank');assert.match(detail.source[0].rel,/noopener/);cases.push({case:'detail-'+id,...detail});
}
await page.goto('http://127.0.0.1:5173/assistant?book=thinking-with-type');await page.waitForFunction(()=>document.querySelector('#assistant-book')?.value==='thinking-with-type',undefined,{timeout:20000});
const selected=await page.evaluate(()=>({selected:document.querySelector('#assistant-book').value,count:document.querySelector('#assistant-book').options.length,text:document.querySelector('.assistant-selected').textContent}));assert.equal(selected.count,16);assert.match(selected.text,/9781797226828/);cases.push({case:'new-book-assistant-context',...selected});
await page.goto('http://127.0.0.1:5173/books?size=8');await ready();await covers();await page.evaluate(()=>window.scrollTo(0,document.querySelector('.catalog-results').offsetTop-100));
await fs.writeFile(new URL('docs/assignments/evidence/catalog-browser.json',root),JSON.stringify({testedAt:new Date().toISOString(),cases,functionalComplete:true,visualReview:{complete:false},complete:false},null,2)+'\n');console.log('16本书四页封面、分类、搜索、校正版详情来源与新书助手入口通过。');

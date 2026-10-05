"""在专用临时MySQL库中验证两份种子；仅清理本脚本新建的数据库。"""
import json, pathlib, subprocess, datetime
root=pathlib.Path(__file__).resolve().parents[2]
def mysql(sql):
    return subprocess.check_output(['mysql','-uroot','-N','--default-character-set=utf8mb4'],input=sql.encode()).decode()
def books(db):
    return [json.loads(l) for l in mysql(f"USE {db}; SELECT JSON_OBJECT('id',id,'title',title,'author',author,'isbn',isbn,'stock',stock,'price',price,'image',image) FROM books ORDER BY id;").splitlines()]
reports=[]
schema=(root/'backend/database/bookstore.sql').read_text().split('INSERT INTO books')[0]
schema=schema[schema.index('-- 1. 用户表'):]
for index,path in enumerate(['backend/src/main/resources/data.sql','backend/database/bookstore.sql']):
    db='bookstore_catalog_qa_'+str(index)+'_'+str(int(datetime.datetime.now().timestamp()))
    assert not mysql(f"SHOW DATABASES LIKE '{db}';").strip(), '拒绝复用已有数据库'
    mysql(f'CREATE DATABASE {db} CHARACTER SET utf8mb4;')
    try:
        mysql(f'USE {db};'+schema)
        text=(root/path).read_text();seed=text[text.index('INSERT INTO books'):]
        mysql(f'USE {db};'+seed)
        initial=books(db);assert len(initial)==16
        mysql(f'USE {db};'+seed);assert books(db)==initial
        mysql(f"USE {db}; UPDATE books SET stock=7,price=123.45,title='验收自定义标题',author='验收自定义作者',image='/images/custom.jpg' WHERE id='design';")
        custom=books(db);mysql(f'USE {db};'+seed);assert books(db)==custom
        mysql(f"USE {db}; UPDATE books SET isbn='9787508648335',title='设计心理学' WHERE id='design'; UPDATE books SET isbn='9787550274864',title='深度工作' WHERE id='deep-work'; UPDATE books SET title='JavaScript 高级程序设计',author='Nicholas C. Zakas',image='/images/高级程序设计.JPG' WHERE id='js-advanced';")
        mysql(f'USE {db};'+seed)
        result={b['id']:b for b in books(db)}
        assert result['design']['isbn']=='9787508648330'
        assert result['design']['author']=='验收自定义作者'
        assert result['design']['image']=='/images/custom.jpg'
        assert result['design']['stock']==7 and result['design']['price']==123.45
        assert result['deep-work']['isbn']=='9787210093213'
        assert result['js-advanced']['author']=='Matt Frisbie'
        assert result['js-advanced']['image']!='/images/高级程序设计.JPG'
        reports.append({'seed':path,'count':len(initial),'checks':['空库16本','重复执行完全幂等','自定义业务字段保留','已知旧ISBN与作者封面条件纠正','纠错保留库存售价及自定义封面'],'complete':True})
    finally: mysql(f'DROP DATABASE {db};')
(root/'docs/assignments/evidence/catalog-seed-mysql.json').write_text(json.dumps({'testedAt':datetime.datetime.now().isoformat(),'environment':'真实MySQL专用临时库，两份SQL分别执行','reports':reports,'complete':True},ensure_ascii=False,indent=2)+'\n')
print('两份种子空库、幂等、条件纠错及业务字段保留通过；临时库已清理。')

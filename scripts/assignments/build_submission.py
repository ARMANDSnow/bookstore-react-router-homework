"""仅选择源码、必要配置与对应作业证据；固定ZIP时间以保证可复核。"""
import hashlib,json,pathlib,subprocess,zipfile,sys
root=pathlib.Path(__file__).resolve().parents[2]
assignment=sys.argv[1] if len(sys.argv)>1 else '2'
assert assignment in ('2','3')
files=set()
for folder in ['src','backend/src','public/images']:
 files.update(p for p in (root/folder).rglob('*') if p.is_file())
for path in ['package.json','package-lock.json','index.html','vite.config.js','backend/pom.xml','backend/.env.example','backend/database/bookstore.sql','docs/assignments/书目来源.md','scripts/assignments/build_submission.py']:
 files.add(root/path)
patterns=['2a-*','2b-*','catalog-*'] if assignment=='2' else ['3a-*','3b-*','3c-*']
for pattern in patterns:files.update((root/'docs/assignments/evidence').glob(pattern))
for pattern in ['verify_assistant*.mjs','verify_catalog*.mjs','verify_seed_mysql.py'] if assignment=='2' else ['verify_policy*.mjs','verify_shopping*.mjs']:
 files.update((root/'scripts/assignments').glob(pattern))
if assignment=='3':
 files.update(p for p in (root/'embedding-service').rglob('*') if p.is_file() and not set(p.parts)&{'node_modules','.cache','models'})
readme=root/f'docs/assignments/作业{assignment}-运行说明.md'
entries={str(p.relative_to(root)):p.read_bytes() for p in sorted(files)}
entries['README.md']=readme.read_bytes()
for path,data in entries.items():
 assert not any(part in path.split('/') for part in ['.git','.env','node_modules','target','dist']),path
 assert not path.endswith(('.jar','.class','.log')),path
local_env=root/'backend/.env'
keys=[l.split('=',1)[1].strip().strip('\"\'') for l in (local_env.read_text() if local_env.exists() else '').splitlines() if l.startswith('DEEPSEEK_API_KEY=')]
assert not any(k and k.encode() in data for k in keys for data in entries.values()),'凭证检测失败'
revision=subprocess.check_output(['git','rev-parse','HEAD'],cwd=root).decode().strip() if (root/'.git').exists() else json.loads((root/'MANIFEST.json').read_text())['sourceRevision']
manifest={'assignment':assignment,'student':'524031910745 丁宇轩','sourceRevision':revision,'files':[{ 'path':p,'bytes':len(data),'sha256':hashlib.sha256(data).hexdigest()} for p,data in entries.items()]}
entries['MANIFEST.json']=(json.dumps(manifest,ensure_ascii=False,indent=2)+'\n').encode()
out=root/f'docs/assignments/提交/524031910745-作业{assignment}-源码.zip'
with zipfile.ZipFile(out,'w',compression=zipfile.ZIP_DEFLATED,compresslevel=9) as archive:
 for path,data in entries.items():
  info=zipfile.ZipInfo(path,(1980,1,1,0,0,0));info.compress_type=zipfile.ZIP_DEFLATED;info.external_attr=0o100644<<16;archive.writestr(info,data)
with zipfile.ZipFile(out) as archive:
 assert archive.testzip() is None
 for item in manifest['files']:assert hashlib.sha256(archive.read(item['path'])).hexdigest()==item['sha256']
print(json.dumps({'path':str(out),'files':len(entries),'bytes':out.stat().st_size,'sha256':hashlib.sha256(out.read_bytes()).hexdigest()},ensure_ascii=False))

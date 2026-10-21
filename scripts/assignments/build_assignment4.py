"""选择作业4的自编源码依赖闭包，生成可独立运行的精简 ZIP。"""
import hashlib
import json
import subprocess
import zipfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
JAVA = 'backend/src/main/java/com/homework/bookstore/'
CLASSES = [
    'BookstoreBackendApplication', 'config/SecurityConfig', 'config/WebConfig',
    'controller/TokenController', 'controller/SecureBookController', 'controller/GlobalExceptionHandler',
    'security/JwtTokenService', 'security/JwtAuthenticationFilter', 'security/JsonAuthenticationEntryPoint',
    'service/BookService', 'service/UserService', 'service/impl/BookServiceImpl',
    'service/impl/UserServiceImpl', 'service/exception/BusinessException',
    'repository/BookRepository', 'repository/UserRepository', 'entity/Book', 'entity/User', 'entity/UserRole',
    'dto/ApiResponse', 'dto/BookDto', 'dto/BookPageDto', 'dto/BookRequest', 'dto/StrictIntegerDeserializer',
    'dto/LoginRequest', 'dto/RegisterRequest', 'dto/UserResponse',
]
TESTS = ['security/StatelessHttpTest', 'repository/BookRepositoryTest',
         'service/impl/BookServiceImplTest', 'service/impl/UserServiceImplTest']
paths = [JAVA + name + '.java' for name in CLASSES]
paths += ['backend/src/test/java/com/homework/bookstore/' + name + '.java' for name in TESTS]
paths += [
    'backend/pom.xml', 'backend/src/main/resources/application.yml',
    'backend/src/main/resources/application-assignment4.yml', 'backend/src/main/resources/data-assignment4.sql',
    'backend/src/main/resources/static/assignment4.html', 'backend/src/test/resources/application.yml',
    'backend/src/test/resources/mockito-extensions/org.mockito.plugins.MockMaker',
    'scripts/assignments/run_assignment4.sh', 'scripts/assignments/verify_assignment4.py',
    'scripts/assignments/build_assignment4.py', 'docs/assignments/作业4-运行说明.md',
]
EVIDENCE = ROOT / 'docs/assignments/evidence/assignment4'
paths += [str(p.relative_to(ROOT)) for p in sorted(EVIDENCE.glob('*')) if p.suffix in {'.png', '.json'}]
entries = {name: (ROOT / name).read_bytes() for name in sorted(paths)}
entries['README.md'] = (ROOT / 'docs/assignments/作业4-运行说明.md').read_bytes()
# 提交包仅保留本作业配置；无须 MySQL、.env、历史接口或其他作业。
entries['backend/src/main/resources/application.yml'] = b'''spring:
  application:
    name: bookstore-assignment4
  profiles:
    default: assignment4
  threads:
    virtual:
      enabled: true
  jpa:
    defer-datasource-initialization: true
    open-in-view: false
bookstore:
  jwt:
    secret: ${JWT_SECRET:}
    ttl-seconds: ${JWT_TTL_SECONDS:3600}
  catalog:
    simulated-delay-ms: ${CATALOG_DELAY_MS:0}
'''
assert not any(set(name.split('/')) & {'.git', '.env', 'node_modules', 'target', 'dist'} for name in entries)
assert not any(name.endswith(('.jar', '.class', '.log', '.zip')) for name in entries)
assert json.loads(entries['docs/assignments/evidence/assignment4/http.json'])['complete']
assert all('docs/assignments/evidence/assignment4/' + name in entries
           for name in ['401.png', '200.png', 'login.png', 'tests.json'])
local_env = ROOT / 'backend/.env'
if local_env.exists():
    for line in local_env.read_text().splitlines():
        if '=' in line and any(word in line.split('=', 1)[0] for word in ['KEY', 'SECRET', 'PASSWORD']):
            value = line.split('=', 1)[1].strip().strip('\"\'')
            if len(value) >= 8:
                assert not any(value.encode() in data for data in entries.values()), '检测到本地凭证'
try:
    revision = subprocess.check_output(['git', 'rev-parse', 'HEAD'], cwd=ROOT, stderr=subprocess.DEVNULL).decode().strip()
except subprocess.CalledProcessError:
    revision = json.loads((ROOT / 'MANIFEST.json').read_text())['baseRevision']
manifest = {'assignment': '4', 'student': '524031910745 丁宇轩', 'baseRevision': revision,
    'sourceOfTruth': '逐文件 SHA-256；基础配置按本脚本精简，其他源码逐字节复制',
    'files': [{'path': name, 'bytes': len(data), 'sha256': hashlib.sha256(data).hexdigest()}
              for name, data in entries.items()]}
entries['MANIFEST.json'] = (json.dumps(manifest, ensure_ascii=False, indent=2)+'\n').encode()
output = ROOT / 'docs/assignments/提交/524031910745-作业4-源码.zip'
output.parent.mkdir(parents=True, exist_ok=True)
with zipfile.ZipFile(output, 'w', compression=zipfile.ZIP_DEFLATED, compresslevel=9) as archive:
    for name, data in entries.items():
        info = zipfile.ZipInfo(name, (1980, 1, 1, 0, 0, 0))
        info.compress_type = zipfile.ZIP_DEFLATED
        info.external_attr = (0o100755 if name.endswith('.sh') else 0o100644) << 16
        archive.writestr(info, data)
with zipfile.ZipFile(output) as archive:
    assert archive.testzip() is None
    for item in manifest['files']:
        assert hashlib.sha256(archive.read(item['path'])).hexdigest() == item['sha256']
print(json.dumps({'path': str(output), 'files': len(entries), 'bytes': output.stat().st_size,
    'sha256': hashlib.sha256(output.read_bytes()).hexdigest()}, ensure_ascii=False))

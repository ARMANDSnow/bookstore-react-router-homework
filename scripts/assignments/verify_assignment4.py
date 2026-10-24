"""真实 HTTP 验收。仅用 Python 标准库，不记录完整 JWT 或密码。"""
import argparse
import concurrent.futures
import json
import time
import urllib.error
import urllib.request
from datetime import datetime, timezone
from pathlib import Path


def verify(base, output, count):
    results = []

    def call(path, token=None, payload=None):
        headers = {'Content-Type': 'application/json'}
        if token is not None:
            headers['Authorization'] = 'Bearer ' + token
        request = urllib.request.Request(base + path, headers=headers,
            data=json.dumps(payload).encode() if payload is not None else None)
        start = time.perf_counter()
        try:
            response = urllib.request.urlopen(request, timeout=20)
        except urllib.error.HTTPError as error:
            response = error
        with response:
            return {'status': response.status, 'headers': dict(response.headers),
                'body': json.loads(response.read()), 'ms': round((time.perf_counter()-start)*1000, 2)}

    def check(name, response, expected):
        assert response['status'] == expected, (name, response)
        assert not any(key.lower() == 'set-cookie' for key in response['headers']), name
        results.append({'name': name, **response})

    # 【作业4 · 得分点4.5：401/200的真实HTTP验收顺序】
    # 先无凭证/坏凭证查询401，再登录拿JWT，篡改签名应401，原JWT应200；
    # 这验证的是服务器实际状态码，而不是只检查JSON业务码或静态页面文字。
    # 截图由assignment4.html用相同接口展示并保存；本脚本提供可复跑的JSON佐证。
    resource = '/api/secure/books/assignment4-book'
    check('missing-token', call(resource), 401)
    check('invalid-token', call(resource, 'invalid.jwt.token'), 401)
    check('incorrect-password', call('/api/auth/login', payload={'username':'demo','password':'incorrect'}), 401)
    login = call('/api/auth/login', payload={'username':'demo','password':'123456'})
    assert login['status'] == 200, login
    assert not any(key.lower() == 'set-cookie' for key in login['headers'])
    token = login['body']['data']['accessToken']
    assert token.count('.') == 2
    # 删去凭证后才把登录证据写到磁盘。
    login['body']['data']['accessToken'] = '[redacted]'
    check('login', login, 200)
    parts = token.split('.')
    parts[2] = ('B' if parts[2][0] == 'A' else 'A') + parts[2][1:]
    check('tampered-token', call(resource, '.'.join(parts)), 401)
    success = call(resource, token)
    check('valid-token', success, 200)
    headers = {key.lower(): value for key, value in success['headers'].items()}
    assert headers['x-thread-virtual'] == 'true', headers
    assert headers['x-java-version'].split('.')[0] == '21', headers
    assert success['body']['data']['id'] == 'assignment4-book'
    # 成功后再去掉Token仍应401。本脚本未用CookieJar；同一Cookie客户端不能继承
    # 登录状态的更强检查在StatelessHttpTest里，不能只凭这行声称已测试Cookie复用。
    check('no-session-after-success', call(resource), 401)
    check('authorized-not-found', call('/api/secure/books/missing', token), 404)
    # 【作业4 · 得分点4.2：并发请求验证】
    # Python线程只负责同时发HTTP；是否使用Java虚拟线程由每个服务端响应头证明。
    # 启动时可配置200ms慢I/O，确认多请求可共同推进；没有平台线程基线，不计算加速倍数。
    start = time.perf_counter()
    with concurrent.futures.ThreadPoolExecutor(max_workers=count) as executor:
        responses = list(executor.map(lambda _: call(resource, token), range(count)))
    elapsed = round((time.perf_counter()-start)*1000, 2)
    assert all(r['status'] == 200 and {k.lower():v for k,v in r['headers'].items()}.get('x-thread-virtual') == 'true' for r in responses)
    assert all(not any(k.lower() == 'set-cookie' for k in r['headers']) for r in responses)
    report = {'complete': True, 'at': datetime.now(timezone.utc).isoformat(), 'baseUrl': base,
        'checks': results, 'concurrency': {'requests': count, 'allStatus200': True,
        'allVirtual': True, 'totalMs': elapsed,
        'minRequestMs': min(r['ms'] for r in responses), 'maxRequestMs': max(r['ms'] for r in responses)},
        'note': '并发慢I/O功能验证；未与平台线程对照，不据此声称性能提升倍数。'}
    output.parent.mkdir(parents=True, exist_ok=True)
    output.write_text(json.dumps(report, ensure_ascii=False, indent=2)+'\n')
    print(json.dumps({'complete': True, 'checks': len(results), 'concurrency': report['concurrency'], 'output': str(output)}, ensure_ascii=False))


if __name__ == '__main__':
    parser = argparse.ArgumentParser()
    parser.add_argument('--base-url', default='http://127.0.0.1:8084')
    parser.add_argument('--output', type=Path, default=Path('docs/assignments/evidence/assignment4/http.json'))
    parser.add_argument('--concurrency', type=int, choices=range(1, 257), default=64)
    args = parser.parse_args()
    verify(args.base_url.rstrip('/'), args.output, args.concurrency)

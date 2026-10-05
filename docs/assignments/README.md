# 应用系统体系架构作业 1～3

依据 `课程文件/作业要求文档/作业1.docx`、`作业2.docx`、`作业3.docx`，学号 524031910745，姓名丁宇轩。

推进规则：每个功能完成后，验证后端测试、真实接口与数据库、前端构建和真实浏览器（桌面、成功/空/失败状态；按用户要求不验收手机比例），然后 commit + push；确认远端提交后进入下一项。测试替身与 H2 测试不作为真实模型或 MySQL 验收。

| 功能 | 状态 |
| --- | --- |
| 1A 分页与分类书架 | 已实现、已通过本地前后端验收 |
| 1B REST 单本详情 | 已实现、已通过本地前后端验收 |
| 1C REST 新增图书 | 已实现、已通过本地前后端验收 |
| 1D 独立库存更新 | 已实现、已通过本地前后端验收 |
| 1E OpenAPI 3.0 JSON、完整 Prompt、Few-Shot、多轮记录和模板报告 | 已实现、已通过本地前后端验收 |
| 2A 真实模型库存与模拟竞价工具、前端助手 | 已实现、已通过本地前后端验收 |
| 2B 错误 ISBN、超时与自纠正 | 已实现；62项后端测试、真实模型/MySQL与桌面功能及视觉验收通过 |
| 书目扩充及种子数据保留 | 已实现；62项后端测试、构建、真实MySQL/模型与桌面功能及视觉验收通过 |
| 2C 精简源码提交包 | 已生成；解压后依赖安装、62项后端测试、构建与独立前后端启动通过 |
| 2C 精简源码提交包 | 待开始 |
| 3A 政策加载、分块、向量化与 RAG | 待开始 |
| 3B 导购 Agent、多工具循环和公开行动记录 | 待开始 |
| 3C 复杂案例与精简源码提交包 | 待开始 |

## 1A 接口

`GET /api/books?page=1&size=4&category=tech&keyword=代码&sort=price-low`

- `page` 从 1 开始，`size` 为 1～100，默认 12。
- `category` 精确匹配分类编码；`keyword` 匹配书名或作者，前后空格会去掉。
- `sort` 为 `recommended`（按 ID）、`price-low`、`price-high`；价格相同按 ID 保持稳定顺序。
- 所有过滤和排序先在数据库完成，再分页；响应为 `{code,message,data:{items,page,size,totalItems,totalPages}}`。
- 不存在的分类或超出范围的页码返回空列表；非法页码、数量、类型、排序返回 HTTP 400。
- 旧 `/api/v1/books` 保留数组契约。书架 URL 保存分类、关键词、排序、页码和每页数量，筛选后回第一页；请求失败可重试。

## 1A 验收 2026-10-06

- `cd backend && mvn test`：36 个测试通过（原有 26 个 + 新增 10 个 HTTP/数据库集成测试），H2 隔离测试。
- `npm run build`：通过；已有大包体积提示，后续助手页面将采用按路由加载。
- 真实 Spring Boot + 本机 MySQL：10 个 HTTP 检查通过；极大 offset 额外实测返回 400。
- 真实桌面浏览器：排序、翻页、分类重置页码、分类叠加关键词、空结果、断网反馈及恢复后重试均通过；无横向溢出。
- 证据：`evidence/1a-http.json`、`evidence/1a-browser.json`、`evidence/1a-desktop.png`。截图来自真实浏览器，包含浏览器自身的控制提示。
- 复核后补充 SQL 通配符字面匹配、超大分页 offset、无 hero 页的一级标题和加载时保留分页控件。

## 1B 验收 2026-10-06

`GET /api/books/{id}` 返回统一图书 DTO，不存在返回 40404 / HTTP 404。详情页直接访问和刷新走此接口；连接失败时明确提示展示缓存信息，并禁用加购直到重新加载成功。路由切换不会沿用其他图书的数据。

- 后端 38 个测试通过，前端构建通过。
- 真实 MySQL HTTP：有效 ID 为 200，不存在 ID 为 404。
- 桌面浏览器：直接打开、刷新、断网缓存提示/禁用加购、恢复重试、404 与返回书架均通过。见 `evidence/1b-browser.json`。

## 1C 验收 2026-10-06

`POST /api/books` 创建成功返回 HTTP 201 和 Location；重复 ID 返回 409；格式错误 JSON / 缺字段 / 负值 / 不可寻址 ID 返回 400。管理员保存失败保留输入并显示具体错误，保存中禁止取消，避免不确定结果。

- 后端 44 个测试通过，前端构建通过。
- 管理员通过真实桌面界面提交重复 ID，收到错误且输入保留；修改后成功新增《微服务设计（第2版）》，管理表、分类搜索、详情与 MySQL 记录一致，官方封面实际加载成功。
- 元数据与封面来源：[O’Reilly 北京官方书页](https://www.oreilly.com.cn/index.php?func=book&isbn=978-7-115-63876-2)。售价 128、定价 158、库存 24 是本项目课程演示值，不代表出版社报价或实际库存。
- 真实 HTTP 验证错误 JSON 400、重复 ID 409 及新增书持久化。证据为 `evidence/1c-browser.json`、`evidence/1c-http.json`。

## 1D 验收 2026-10-06

`PATCH /api/books/{id}/inventory` 仅接受 `{ "stock": 非负 JSON 整数 }`，不接受小数、数字字符串、null、溢出和额外字段。新增图书的 stock 也使用同样的严格整数解析。

- 后端 48 个测试通过，前端构建通过。
- 真实 MySQL HTTP：null、负数、小数、数字字符串、整数溢出均为 400，原记录不变。
- 桌面管理页将微服务书库存设为 0，书架显示缺货且禁用加购；随后通过同一界面恢复为 24，逐字段比较确认其他信息一致。
- 证据：`evidence/1d-http.json`、`evidence/1d-browser.json`。

### 1D 浏览器验收修正

首次浏览器保存库存返回 403，初始提交中的通过记录过早；原因是 WebConfig 的 allowedMethods 缺少 PATCH，Vite 代理保留的 Origin 触发了后端检查。已允许 PATCH，增加来源头和预检集成测试；重新通过真实桌面页面将库存 24 → 0 → 24，确认缺货禁用加购、其他字段逐一一致，并保存完整 `1d-browser.json`。新证据之后才进入下一功能。

## 1E 验收 2026-10-06

- 真实 DeepSeek `deepseek-flash` 五轮对话：完整 System/User Prompt、Few-Shot、审查输入和原始输出在 `作业1/`。JSON模式和低温度不保证字节一致；各轮检查保持四个操作及核心约束。第四轮基于真实校验错误补充错误 data 的类型、历史 stock 可空和重复 ID 的40903示例；第五轮修正创建字段描述和40000示例。
- 最终规范仅一份：`backend/src/main/resources/static/api/v1/openapi.json`，HTTP `/api/v1/openapi.json` 可下载；报告含完整可选择 JSON 全文。各轮文件为真实历史记录。
- Swagger Parser 校验全部五轮结构与引用；最终 AJV schema 对23个真实 Spring Boot/MySQL 响应验证成功，包含201+Location、400、404、409、严格库存token拒绝及临时数据清理后404。未人为触发真实500，报告已限定证据边界。
- 后端48个测试通过，前端构建通过；桌面书架从Vite代理下载最终规范成功，四张书卡、无横向溢出。证据：`1e-contract.json`、`1e-browser.json`和三张原生现场对话截图。
- 模板报告：`提交/524031910745-作业1.docx`，保留模板身份区、样式、编号、页脚与页系统，15页全部渲染检查通过；原模板哈希不变，未修改部件逐字节一致。
- 重跑校验：`node scripts/assignments/verify_openapi.mjs`（需后端启动）；重新真实生成：`node scripts/assignments/generate_openapi.mjs`，从忽略的`backend/.env`读取密钥，已有完整记录时不重复请求。
- 模型配置依据：[DeepSeek官方文档](https://api-docs.deepseek.com/zh-cn/)；格式依据：[OpenAPI3.0.3](https://spec.openapis.org/oas/v3.0.3.html)。

## 2A 验收 2026-10-06

`POST /api/assistant/chat` 接收问题和至多20条用户/助手文本历史。服务器提供 `check_inventory(isbn)`、`get_competitor_price(isbn)` 两种 JSON Schema；真实 DeepSeek 返回 `tool_calls` 后，按白名单调用本地函数，将每一个结果以原始 `tool_call_id` 回传，等待模型生成自然语言答案。一次最多4次模型请求、6个工具步骤，同时最多2个请求，防止无限循环。库存读取真实本机 MySQL；竞价按本店价格乘0.92模拟，工具结果与回答均明确标记模拟。

新页面 `/assistant` 可选书、输入问题、查看结果卡片和公开查询记录；详情页可带图书进入助手。选书信息在手输问题中明确展示，连续库存追问必须重新查库。聊天区域保持固定高度，加载期间禁止重复发送；失败保留问题，可重试或恢复原始文字修改。按用户要求只进行桌面验收。

- 后端56个测试通过、前端构建通过。隔离测试涵盖工具消息与ID配对、非法参数、同ISBN多记录、未知库存、循环上限及两种ISBN历史的刷新；这些是测试替身证据。
- `evidence/2a-real-model.json` 记录真实服务商响应ID、双工具调用与自然回复，库存与本机数据库一致；带连字符ISBN的历史错误库存99本也重新查库得到当前24本。真实HTTP拒绝空内容、系统角色、null历史项、超过20条历史、超过1200字问题。
- `evidence/2a-browser.json` 的 `functionalComplete: true` 记录真实桌面页面的空问题禁用、中文输入法保护、双工具查询、A→B选书切换、追问重新查库、断网修改和恢复重试、新对话、详情入口。网络记录来自真实fetch观察，没有替换响应。
- 解锁后使用真实窗口检查欢迎、双工具答复、查询记录展开和断网错误四个版面，保存四张 `2a-desktop-*.png`，浏览器证据整体 `complete: true`。先前测试画布比原生窗口大，视觉检查时匹配真实桌面窗口到1450×750 CSS视口；按用户要求未测试手机比例。

启动模型配置：仅在忽略的 `backend/.env` 写入 `DEEPSEEK_API_KEY`，Spring Boot 会从该文件加载；URL默认 `https://api.deepseek.com`，模型默认 `deepseek-flash`。密钥不进入前端、Git、截图或报告。服务商配置遵循[DeepSeek工具调用文档](https://api-docs.deepseek.com/zh-cn/guides/tool_calls/)。状态接口只说明服务器已配置，不代表探测成功；页面在首次真实答复后才显示已连接。

复测真实接口：`node scripts/assignments/verify_assistant.mjs`。桌面复测使用已存在的Ego TaskSpace 11，通过 `ego-browser nodejs` 的heredoc入口导入 `scripts/assignments/verify_assistant_browser.mjs`；保存部分失败证据并在退出时恢复页面联网。

## 2B 异常处理

作业2原文异常项为“如模拟接口超时或传入错误的 ISBN 时模型的 Self-Correction”。本项以真实模型完成“竞价首次模拟超时 → 同一ISBN重试 → 报价成功”的闭环；工具错误以 `role=tool` 回传，未在服务器直接重试后伪装成模型行动。模拟场景由可选 `scenario=competitor-timeout-once` 显式开启，正常查询为默认；其他场景值HTTP400。计数仅在本次请求内生效，同一工具和归一化ISBN最多执行两次，仍保留4轮模型与6工具步骤上限。

ISBN只校验13位形状和数据库精确记录，不机械猜测校验位或替换书籍。格式错误为`INVALID_ISBN`，有效形状但未找到为`BOOK_NOT_FOUND`，多个记录为`AMBIGUOUS_ISBN`。用户明确输入“ISBN为12345”“ISBN 是 12345”等时保留原文；即使历史中有正确ISBN，库存刷新保护也允许澄清当前错误号码。

- 后端62项测试通过，覆盖模拟超时回传与ID配对、同ISBN重试、每次请求重置、第三次不执行函数、非法场景、错误形状不查库和带历史的错误ISBN澄清；前端构建通过。
- 真实模型证据 `evidence/2b-real-model.json` 保留每个问题、场景、HTTP响应、公开工具步骤和真实服务商请求ID。短ISBN可能由模型先澄清，此时不会伪造工具调用。竞价与工具超时均为课程模拟，库存来自本机真实MySQL。
- 桌面证据 `2b-browser.json` 记录默认正常查询、错误号码原文、断网后重试恢复原场景、同ISBN模型重试和新对话恢复正常。另有一次真实模型连接失败，经界面重试恢复，记录在 `2b-browser-provider-retry.json`，不将其称为课程工具模拟。
- 页面依据真实工具观察展示“重试后已获取报价”，并公开首次模拟超时与再次成功的三个步骤。四张桌面截图已检查，文字清晰、输入区稳定，未进行手机比例测试。
- 重跑：`node scripts/assignments/verify_assistant_errors.mjs`；Ego同空间导入 `scripts/assignments/verify_assistant_errors_browser.mjs`。`2b-browser.json` 的功能与视觉验收均已完成。

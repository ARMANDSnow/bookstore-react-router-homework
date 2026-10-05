-- ============================================================================
--  Spring Boot 启动时自动执行的种子数据脚本
--  表结构由 JPA (ddl-auto: update) 根据 @Entity 自动维护，本文件只负责 INSERT
-- ============================================================================

-- ---------------------------------------------------------------------------
-- 1. 书籍数据
-- ---------------------------------------------------------------------------
INSERT INTO books
(id, title, author, isbn, publisher, stock, price, original_price, category, category_name, category_label, image, rating, badge, description, summary, highlight, audience, review)
VALUES
('clean-code', '代码整洁之道', 'Robert C. Martin', '9787115216878', '人民邮电出版社', 32, 79, 99, 'tech', '计算机与技术', '计算机与技术 / 软件工程', '/images/代码整洁之道.JPG', '4.8 / 5.0', '限时 8 折', '聚焦高可读性与可维护性的经典软件工程实践。', '本书围绕可读性、可维护性、可演进性展开，适合正在学习程序设计、软件工程与项目实践的读者。内容覆盖命名、函数设计、注释、对象与数据结构、错误处理、测试等核心主题。', '通过大量代码案例解释坏味道与重构思路，帮助读者建立工程级代码审美。', '适合学习 Java、JavaScript、Python 等语言的开发者，也适合作为课程设计与项目作业参考书。', '不仅讲语法，更讲判断标准；看完之后写代码会更在意结构与命名。'),
('design', '设计心理学1（增订版）：日常的设计', 'Donald A. Norman', '9787508648330', '中信出版社', 25, 65, 82, 'design', '设计创意', '设计创意 / 用户体验', 'https://img4.xinhuashudian.com/bookbasepic/C/02005/3077626-fm.jpg', '4.7 / 5.0', '设计经典', '理解用户、界面与行为之间关系的入门经典。', '从可见性、反馈、映射和容错等角度解释为什么有些产品天然顺手，有些产品却让人困惑，非常适合界面设计与交互设计入门。', '用大量生活化案例解释好设计如何降低理解成本并提升使用效率。', '适合产品、设计、前端开发以及任何对用户体验有兴趣的学习者。', '读完之后会重新审视按钮、门把手和页面布局背后的设计逻辑。'),
('deep-work', '深度工作：如何有效使用每一点脑力', 'Cal Newport', '9787210093213', '江西人民出版社', 30, 58, 72, 'business', '经济管理', '经济管理 / 时间管理', '/images/深度工作.JPG', '4.6 / 5.0', '效率提升', '帮助你建立专注习惯，提高学习与输出效率。', '帮助读者建立专注习惯，减少分心成本，适合备考、写代码、做课程作业和进行长期项目训练。', '强调高质量专注输出的重要性，并提供可执行的训练策略。', '适合学生、自学者以及希望提高学习效率和工作专注度的人群。', '不是空泛鸡汤，而是可以立即实践的专注方法论。'),
('three-body', '三体', '刘慈欣', '9787536692930', '重庆出版社', 40, 49, 59, 'literature', '文学小说', '文学小说 / 科幻小说', '/images/三体.JPG', '4.9 / 5.0', '现象级作品', '宏大的宇宙叙事与冷峻的科学想象交织在一起。', '以恢弘宇宙视角展开文明碰撞与生存抉择，兼具科学想象力与叙事张力，是中文科幻的重要代表作。', '将宏大宇宙命题与人物命运并置，阅读体验极具冲击力。', '适合喜欢科幻、社会议题和宏大叙事的读者。', '从地球文明的局限，到宇宙尺度的残酷，这本书都写得极具震撼力。'),
('js-advanced', 'JavaScript高级程序设计（第4版）', 'Matt Frisbie', '9787115545381', '人民邮电出版社', 26, 88, 108, 'tech', '计算机与技术', '计算机与技术 / JavaScript', 'https://file.ituring.com.cn/LargeCover/20080d81a4fb8a268c40', '4.8 / 5.0', '前端进阶', '系统理解 JavaScript 语言机制与工程实践。', '系统讲解 JavaScript 核心概念、执行机制和常见 API，适合作为前端课程、项目开发与面试准备的长期参考书。', '内容覆盖语言基础、浏览器环境、异步编程与工程实践，体系完整。', '适合前端初学者、进阶开发者以及准备面试的学生。', '内容扎实、覆盖面广，是理解 JavaScript 的高频参考书。'),
('sapiens', '人类简史', '尤瓦尔·赫拉利', '9787508647357', '中信出版社', 34, 62, 78, 'literature', '文学小说', '文学小说 / 历史人文', '/images/人类简史.JPG', '4.7 / 5.0', '畅销人文', '以通俗方式重新梳理文明演化的关键节点。', '从认知革命、农业革命到科学革命，重新梳理人类社会的形成与演变，视角开阔且表达通俗。', '将历史、经济、宗教与制度变迁串联起来，帮助读者建立宏观认知框架。', '适合对历史、人类文明和社会演化感兴趣的读者。', '读完之后看待社会、国家与制度的角度都会更宏观。'),
('building-microservices', '微服务设计（第2版）', 'Sam Newman', '9787115638762', '人民邮电出版社', 24, 128, 158, 'tech', '计算机与技术', '计算机与技术 / 技术实践', 'https://www.oreilly.com.cn/images/bookcover/Building-Microservices-2e_cvr_l.gif', '未提供评分', '进阶阅读', '从服务拆分到测试部署，理解微服务架构的关键取舍。', '围绕服务边界、通信、部署、测试与监控，帮助开发者理解微服务何时适用，以及如何从单体系统逐步演进。', '从服务拆分到测试部署，理解微服务架构的关键取舍。', '适合对计算机与技术感兴趣，希望拓展阅读视野的读者。', '可结合自身阅读目标，先了解目录与内容简介，再决定是否加入书单。'),
('data-intensive-applications', '数据密集型应用系统设计（第二版）', 'Martin Kleppmann、Chris Riccomini', '9787523915264', '中国电力出版社', 18, 138, 168, 'tech', '计算机与技术', '计算机与技术 / 技术实践', 'https://www.oreilly.com.cn/images/bookcover/Design-Data-IntensiveAppli_2e_cvr_l.gif', '未提供评分', '进阶阅读', '从数据模型到分布式系统，建立可靠应用的设计判断。', '以数据系统为主线，讨论存储、复制、分区、事务与数据处理，理解可靠性、一致性和可扩展性之间的取舍。', '从数据模型到分布式系统，建立可靠应用的设计判断。', '适合对计算机与技术感兴趣，希望拓展阅读视野的读者。', '可结合自身阅读目标，先了解目录与内容简介，再决定是否加入书单。'),
('head-first-python', 'Head First Python实战（第三版）', 'Paul Barry', '9787519889890', '中国电力出版社', 22, 99, 128, 'tech', '计算机与技术', '计算机与技术 / 技术实践', 'https://www.oreilly.com.cn/images/bookcover/HF-python-3e_cvr_l.gif', '未提供评分', '进阶阅读', '通过图解和实际项目，循序渐进掌握 Python。', '从数据结构、函数和文件处理出发，逐步进入 Web 开发、数据分析与数据库集成，适合希望动手完成项目的初学者。', '通过图解和实际项目，循序渐进掌握 Python。', '适合对计算机与技术感兴趣，希望拓展阅读视野的读者。', '可结合自身阅读目标，先了解目录与内容简介，再决定是否加入书单。'),
('dont-make-me-think', 'Don''t Make Me Think, Revisited（英文原版）', 'Steve Krug', '9780321965516', 'New Riders', 16, 86, 106, 'design', '设计创意', '设计创意 / 英文原版', 'https://www.peachpit.com/ShowCover.aspx?isbn=9780321965516&type=f', '未提供评分', '英文原版', '用简单直接的方法，让网站更容易理解和使用。', '第三版通过导航、信息层级、移动体验与可用性测试案例，帮助设计师发现用户操作中的阻碍，并改进页面交互。', '用简单直接的方法，让网站更容易理解和使用。', '适合对设计创意感兴趣，希望拓展阅读视野的读者。', '可结合自身阅读目标，先了解目录与内容简介，再决定是否加入书单。'),
('thinking-with-type', 'Thinking with Type（英文原版·第三版）', 'Ellen Lupton', '9781797226828', 'Chronicle Books', 12, 118, 148, 'design', '设计创意', '设计创意 / 英文原版', 'https://www.chroniclebooks.com/cdn/shop/files/9781797226828_1_1200x1200.jpg?v=1699639207', '未提供评分', '英文原版', '从字体、间距与网格开始，让文字成为清晰的视觉语言。', '结合案例、图解和练习，讲解字体选择、排版结构、视觉平衡与可读性，适合从事界面、出版或平面设计的读者。', '从字体、间距与网格开始，让文字成为清晰的视觉语言。', '适合对设计创意感兴趣，希望拓展阅读视野的读者。', '可结合自身阅读目标，先了解目录与内容简介，再决定是否加入书单。'),
('nineteen-eighty-four', '1984（英文原版）', 'George Orwell', '9780451524935', 'Signet', 28, 45, 59, 'literature', '文学小说', '文学小说 / 英文原版', 'https://images2.penguinrandomhouse.com/cover/9780451524935', '未提供评分', '英文原版', '在被监视与改写的世界里，寻找思想和记忆的自由。', '以温斯顿的生活与选择为线索，呈现权力、语言、真相与个人意志之间的冲突，是具有持久讨论价值的反乌托邦小说。', '在被监视与改写的世界里，寻找思想和记忆的自由。', '适合对文学小说感兴趣，希望拓展阅读视野的读者。', '可结合自身阅读目标，先了解目录与内容简介，再决定是否加入书单。'),
('great-gatsby', 'The Great Gatsby（英文原版）', 'F. Scott Fitzgerald', '9780743273565', 'Scribner', 20, 48, 62, 'literature', '文学小说', '文学小说 / 英文原版', 'https://d28hgpri8am2if.cloudfront.net/book_images/onix/cvr9780743273565/the-great-gatsby-9780743273565_hr.jpg', '未提供评分', '英文原版', '华丽宴会与遥远绿灯之间，一场关于梦想和失落的追寻。', '通过盖茨比与黛西的故事，描绘爵士时代的财富、阶层和情感，追问人能否借助成功重拾过去。', '华丽宴会与遥远绿灯之间，一场关于梦想和失落的追寻。', '适合对文学小说感兴趣，希望拓展阅读视野的读者。', '可结合自身阅读目标，先了解目录与内容简介，再决定是否加入书单。'),
('psychology-of-money', 'The Psychology of Money（英文原版）', 'Morgan Housel', '9780857197689', 'Harriman House', 26, 76, 98, 'business', '经济管理', '经济管理 / 英文原版', 'https://ik.imagekit.io/panmac/tr:f-jpg,di-placeholder_portrait_aMjPtD9YZ.jpg,w-350/edition/9780857197689.jpg', '未提供评分', '英文原版', '从人的经历与行为出发，理解财富、风险和金钱选择。', '以短篇故事讨论人们如何看待金钱，关注耐心、风险认知和个人经历对财务决策的影响。', '从人的经历与行为出发，理解财富、风险和金钱选择。', '适合对经济管理感兴趣，希望拓展阅读视野的读者。', '可结合自身阅读目标，先了解目录与内容简介，再决定是否加入书单。'),
('naked-economics', 'Naked Economics（英文原版）', 'Charles Wheelan', '9780393356496', 'W. W. Norton & Company', 19, 68, 88, 'business', '经济管理', '经济管理 / 英文原版', 'https://cdn2.wwnorton.com/wwnproducts/TRADE/6/9/9780393356496/9780393356496_300.jpeg', '未提供评分', '英文原版', '从生活中的选择出发，轻松理解经济学如何解释世界。', '面向普通读者讲解经济学问题，并讨论贸易、自动化、收入差距与金融危机，适合希望建立经济思维的入门读者。', '从生活中的选择出发，轻松理解经济学如何解释世界。', '适合对经济管理感兴趣，希望拓展阅读视野的读者。', '可结合自身阅读目标，先了解目录与内容简介，再决定是否加入书单。'),
('thinking-fast-slow', 'Thinking, Fast and Slow（英文原版）', 'Daniel Kahneman', '9780374533557', 'Farrar, Straus and Giroux', 23, 85, 108, 'business', '经济管理', '经济管理 / 英文原版', 'https://mpd-biblio-covers.imgix.net/9780374533557.jpg?w=300', '未提供评分', '英文原版', '理解直觉与审慎思考，重新观察日常判断中的偏差。', '通过心理学研究讨论快速直觉与缓慢思考如何共同影响判断，帮助读者认识决策中的过度自信、认知偏差与风险感受。', '理解直觉与审慎思考，重新观察日常判断中的偏差。', '适合对经济管理感兴趣，希望拓展阅读视野的读者。', '可结合自身阅读目标，先了解目录与内容简介，再决定是否加入书单。')
-- 已有书籍保留业务数据，尤其是管理员调整后的库存与价格。
ON DUPLICATE KEY UPDATE id = VALUES(id);

-- 只纠正已知课程旧版书目，保留用户修改过的标题、作者及所有价格/库存。
UPDATE books SET title = CASE WHEN title = '设计心理学' THEN '设计心理学1（增订版）：日常的设计' ELSE title END, isbn = '9787508648330' WHERE id = 'design' AND isbn = '9787508648335';
UPDATE books SET title = CASE WHEN title = '深度工作' THEN '深度工作：如何有效使用每一点脑力' ELSE title END, isbn = '9787210093213' WHERE id = 'deep-work' AND isbn = '9787550274864';
UPDATE books SET title = CASE WHEN title = 'JavaScript 高级程序设计' THEN 'JavaScript高级程序设计（第4版）' ELSE title END, author = 'Matt Frisbie' WHERE id = 'js-advanced' AND isbn = '9787115545381' AND author = 'Nicholas C. Zakas';
UPDATE books SET image = 'https://file.ituring.com.cn/LargeCover/20080d81a4fb8a268c40' WHERE id = 'js-advanced' AND isbn = '9787115545381' AND image = '/images/高级程序设计.JPG';
UPDATE books SET image = 'https://img4.xinhuashudian.com/bookbasepic/C/02005/3077626-fm.jpg' WHERE id = 'design' AND isbn = '9787508648330' AND image = '/images/设计心理学.JPG';

-- ---------------------------------------------------------------------------
-- 2. demo 用户（答辩演示账号：demo / 123456）
--
-- 迭代三：密码列存的是 BCrypt(123456) 的哈希，不再是明文。
--   * 结构：$2a(算法版本) + $10(成本因子=2^10轮) + 22字符盐 + 31字符哈希，共60字符
--   * 该哈希由本机 Spring Security 的 BCrypt.hashpw("123456", gensalt(10)) 生成，
--     并被单元测试 UserServiceImplTest 守护（matches("123456", 此串) 必须为 true），
--     防止有人误改这行导致 demo 账号登录不上
--   * ON DUPLICATE KEY UPDATE：username 撞唯一索引时改走 UPDATE 分支（幂等），
--     即每次应用启动都会把 demo 的密码"自愈"回这个已知哈希——演示账号永远可登录
-- ---------------------------------------------------------------------------
INSERT INTO users (username, password, email, phone, role, enabled, created_at)
VALUES ('demo', '$2a$10$KxEpa.B.KXa2jPqIVfNc2.o4UGEmLgIKh25lvwuMk2BqytkfHQSw2', 'demo@bookstore.com', '13800000000', 'CUSTOMER', TRUE, NOW())
ON DUPLICATE KEY UPDATE password = VALUES(password), role = VALUES(role), enabled = VALUES(enabled);

INSERT INTO users (username, password, email, phone, role, enabled, created_at)
VALUES ('admin', '$2a$10$KxEpa.B.KXa2jPqIVfNc2.o4UGEmLgIKh25lvwuMk2BqytkfHQSw2', 'admin@bookstore.com', '13900000000', 'ADMIN', TRUE, NOW())
ON DUPLICATE KEY UPDATE password = VALUES(password), role = VALUES(role), enabled = VALUES(enabled);

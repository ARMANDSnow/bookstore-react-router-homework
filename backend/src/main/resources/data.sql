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
('clean-code', '代码整洁之道', 'Robert C. Martin', '9787115216878', '人民邮电出版社', 32, 79.00, 99.00, 'tech', '计算机与技术', '计算机与技术 / 软件工程', '/images/代码整洁之道.JPG', '4.8 / 5.0', '限时 8 折', '聚焦高可读性与可维护性的经典软件工程实践。', '本书围绕可读性、可维护性、可演进性展开，适合正在学习程序设计、软件工程与项目实践的读者。内容覆盖命名、函数设计、注释、对象与数据结构、错误处理、测试等核心主题。', '通过大量代码案例解释坏味道与重构思路，帮助读者建立工程级代码审美。', '适合学习 Java、JavaScript、Python 等语言的开发者，也适合作为课程设计与项目作业参考书。', '不仅讲语法，更讲判断标准；看完之后写代码会更在意结构与命名。'),
('design', '设计心理学', 'Donald A. Norman', '9787508648335', '中信出版社', 25, 65.00, 82.00, 'design', '设计创意', '设计创意 / 用户体验', '/images/设计心理学.JPG', '4.7 / 5.0', '设计经典', '理解用户、界面与行为之间关系的入门经典。', '从可见性、反馈、映射和容错等角度解释为什么有些产品天然顺手，有些产品却让人困惑，非常适合界面设计与交互设计入门。', '用大量生活化案例解释好设计如何降低理解成本并提升使用效率。', '适合产品、设计、前端开发以及任何对用户体验有兴趣的学习者。', '读完之后会重新审视按钮、门把手和页面布局背后的设计逻辑。'),
('deep-work', '深度工作', 'Cal Newport', '9787550274864', '江西人民出版社', 30, 58.00, 72.00, 'business', '经济管理', '经济管理 / 时间管理', '/images/深度工作.JPG', '4.6 / 5.0', '效率提升', '帮助你建立专注习惯，提高学习与输出效率。', '帮助读者建立专注习惯，减少分心成本，适合备考、写代码、做课程作业和进行长期项目训练。', '强调高质量专注输出的重要性，并提供可执行的训练策略。', '适合学生、自学者以及希望提高学习效率和工作专注度的人群。', '不是空泛鸡汤，而是可以立即实践的专注方法论。'),
('three-body', '三体', '刘慈欣', '9787536692930', '重庆出版社', 40, 49.00, 59.00, 'literature', '文学小说', '文学小说 / 科幻小说', '/images/三体.JPG', '4.9 / 5.0', '现象级作品', '宏大的宇宙叙事与冷峻的科学想象交织在一起。', '以恢弘宇宙视角展开文明碰撞与生存抉择，兼具科学想象力与叙事张力，是中文科幻的重要代表作。', '将宏大宇宙命题与人物命运并置，阅读体验极具冲击力。', '适合喜欢科幻、社会议题和宏大叙事的读者。', '从地球文明的局限，到宇宙尺度的残酷，这本书都写得极具震撼力。'),
('js-advanced', 'JavaScript 高级程序设计', 'Nicholas C. Zakas', '9787115545381', '人民邮电出版社', 26, 88.00, 108.00, 'tech', '计算机与技术', '计算机与技术 / JavaScript', '/images/高级程序设计.JPG', '4.8 / 5.0', '前端进阶', '系统理解 JavaScript 语言机制与工程实践。', '系统讲解 JavaScript 核心概念、执行机制和常见 API，适合作为前端课程、项目开发与面试准备的长期参考书。', '内容覆盖语言基础、浏览器环境、异步编程与工程实践，体系完整。', '适合前端初学者、进阶开发者以及准备面试的学生。', '内容扎实、覆盖面广，是理解 JavaScript 的高频参考书。'),
('sapiens', '人类简史', '尤瓦尔·赫拉利', '9787508647357', '中信出版社', 34, 62.00, 78.00, 'literature', '文学小说', '文学小说 / 历史人文', '/images/人类简史.JPG', '4.7 / 5.0', '畅销人文', '以通俗方式重新梳理文明演化的关键节点。', '从认知革命、农业革命到科学革命，重新梳理人类社会的形成与演变，视角开阔且表达通俗。', '将历史、经济、宗教与制度变迁串联起来，帮助读者建立宏观认知框架。', '适合对历史、人类文明和社会演化感兴趣的读者。', '读完之后看待社会、国家与制度的角度都会更宏观。')
ON DUPLICATE KEY UPDATE
title = VALUES(title),
author = VALUES(author),
isbn = VALUES(isbn),
publisher = VALUES(publisher),
stock = VALUES(stock),
price = VALUES(price),
original_price = VALUES(original_price),
category = VALUES(category),
category_name = VALUES(category_name),
category_label = VALUES(category_label),
image = VALUES(image),
rating = VALUES(rating),
badge = VALUES(badge),
description = VALUES(description),
summary = VALUES(summary),
highlight = VALUES(highlight),
audience = VALUES(audience),
review = VALUES(review);

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

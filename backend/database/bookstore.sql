-- ============================================================================
--  在线书城 · 数据库脚本（迭代二）
--  数据库：MySQL 8+
--  字符集：utf8mb4 / utf8mb4_unicode_ci
--  迭代二新增：cart_items / orders / order_items 三张表 + demo 用户
-- ============================================================================

CREATE DATABASE IF NOT EXISTS bookstore
  DEFAULT CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

USE bookstore;

-- ---------------------------------------------------------------------------
-- 1. 用户表
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS users (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  username VARCHAR(60) NOT NULL UNIQUE,
  password VARCHAR(120) NOT NULL,
  email VARCHAR(120) NOT NULL UNIQUE,
  phone VARCHAR(30),
  role VARCHAR(20) DEFAULT 'CUSTOMER',
  enabled BOOLEAN DEFAULT TRUE,
  created_at DATETIME NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ---------------------------------------------------------------------------
-- 2. 书籍表
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS books (
  id VARCHAR(80) PRIMARY KEY,
  title VARCHAR(120) NOT NULL,
  author VARCHAR(120) NOT NULL,
  isbn VARCHAR(40),
  publisher VARCHAR(120),
  stock INT DEFAULT 0,
  price DECIMAL(10, 2) NOT NULL,
  original_price DECIMAL(10, 2) NOT NULL,
  category VARCHAR(60) NOT NULL,
  category_name VARCHAR(80) NOT NULL,
  category_label VARCHAR(120) NOT NULL,
  image VARCHAR(255) NOT NULL,
  rating VARCHAR(40) NOT NULL,
  badge VARCHAR(80),
  description VARCHAR(255) NOT NULL,
  summary TEXT NOT NULL,
  highlight TEXT NOT NULL,
  audience TEXT NOT NULL,
  review TEXT NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ---------------------------------------------------------------------------
-- 3. 购物车明细表
--    每条记录 = 一位用户、一本书、数量
--    (user_id, book_id) 联合唯一，再次加车时数量累加
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS cart_items (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  book_id VARCHAR(80) NOT NULL,
  quantity INT NOT NULL DEFAULT 1,
  created_at DATETIME NOT NULL,
  updated_at DATETIME NOT NULL,
  UNIQUE KEY uk_user_book (user_id, book_id),
  CONSTRAINT fk_cart_user FOREIGN KEY (user_id) REFERENCES users(id),
  CONSTRAINT fk_cart_book FOREIGN KEY (book_id) REFERENCES books(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ---------------------------------------------------------------------------
-- 4. 订单主表
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS orders (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  total_amount DECIMAL(10, 2) NOT NULL,
  status VARCHAR(20) NOT NULL,
  created_at DATETIME NOT NULL,
  CONSTRAINT fk_order_user FOREIGN KEY (user_id) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ---------------------------------------------------------------------------
-- 5. 订单明细表（书名、单价同步快照，避免历史订单受书籍价格变动影响）
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS order_items (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  order_id BIGINT NOT NULL,
  book_id VARCHAR(80) NOT NULL,
  book_title VARCHAR(120) NOT NULL,
  book_image VARCHAR(255),
  unit_price DECIMAL(10, 2) NOT NULL,
  quantity INT NOT NULL,
  CONSTRAINT fk_item_order FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE CASCADE,
  CONSTRAINT fk_item_book FOREIGN KEY (book_id) REFERENCES books(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ---------------------------------------------------------------------------
-- 6. 书籍示例数据
--    注：作业项目，密码以明文存储，仅用于课程演示
-- ---------------------------------------------------------------------------
INSERT INTO books
(id, title, author, isbn, publisher, stock, price, original_price, category, category_name, category_label, image, rating, badge, description, summary, highlight, audience, review)
VALUES
('clean-code', '代码整洁之道', 'Robert C. Martin', '9787115216878', '人民邮电出版社', 32, 79.00, 99.00, 'tech', '计算机与技术', '计算机与技术 / 软件工程', '/images/代码整洁之道.JPG', '4.8 / 5.0', '限时 8 折', '聚焦高可读性与可维护性的经典软件工程实践。', '本书围绕可读性、可维护性、可演进性、测试和重构展开，适合正在学习程序设计和软件工程实践的读者。', '通过代码案例解释坏味道与重构思路，帮助读者建立工程级代码审美。', '适合学习 Java、JavaScript、Python 等语言的开发者。', '不仅讲语法，更讲判断标准；看完之后写代码会更在意结构与命名。'),
('design', '设计心理学', 'Donald A. Norman', '9787508648335', '中信出版社', 25, 65.00, 82.00, 'design', '设计创意', '设计创意 / 用户体验', '/images/设计心理学.JPG', '4.7 / 5.0', '设计经典', '理解用户、界面与行为之间关系的入门经典。', '从可见性、反馈、映射和容错等角度解释产品为何顺手或令人困惑。', '用生活化案例解释好设计如何降低理解成本并提升使用效率。', '适合产品、设计、前端开发以及对用户体验感兴趣的学习者。', '读完之后会重新审视按钮、门把手和页面布局背后的设计逻辑。'),
('deep-work', '深度工作', 'Cal Newport', '9787550274864', '江西人民出版社', 30, 58.00, 72.00, 'business', '经济管理', '经济管理 / 时间管理', '/images/深度工作.JPG', '4.6 / 5.0', '效率提升', '帮助你建立专注习惯，提高学习与输出效率。', '帮助读者建立专注习惯，减少分心成本，适合备考、写代码和长期项目训练。', '强调高质量专注输出的重要性，并提供可执行的训练策略。', '适合学生、自学者以及希望提高学习效率和工作专注度的人群。', '不是空泛鸡汤，而是可以立即实践的专注方法论。'),
('three-body', '三体', '刘慈欣', '9787536692930', '重庆出版社', 40, 49.00, 59.00, 'literature', '文学小说', '文学小说 / 科幻小说', '/images/三体.JPG', '4.9 / 5.0', '现象级作品', '宏大的宇宙叙事与冷峻的科学想象交织在一起。', '以恢弘宇宙视角展开文明碰撞与生存抉择，是中文科幻的重要代表作。', '将宏大宇宙命题与人物命运并置，阅读体验极具冲击力。', '适合喜欢科幻、社会议题和宏大叙事的读者。', '从地球文明的局限，到宇宙尺度的残酷，这本书都写得极具震撼力。'),
('js-advanced', 'JavaScript 高级程序设计', 'Nicholas C. Zakas', '9787115545381', '人民邮电出版社', 26, 88.00, 108.00, 'tech', '计算机与技术', '计算机与技术 / JavaScript', '/images/高级程序设计.JPG', '4.8 / 5.0', '前端进阶', '系统理解 JavaScript 语言机制与工程实践。', '系统讲解 JavaScript 核心概念、执行机制和常见 API，适合作为前端课程和面试准备参考书。', '内容覆盖语言基础、浏览器环境、异步编程与工程实践，体系完整。', '适合前端初学者、进阶开发者以及准备面试的学生。', '内容扎实、覆盖面广，是理解 JavaScript 的高频参考书。'),
('sapiens', '人类简史', '尤瓦尔·赫拉利', '9787508647357', '中信出版社', 34, 62.00, 78.00, 'literature', '文学小说', '文学小说 / 历史人文', '/images/人类简史.JPG', '4.7 / 5.0', '畅销人文', '以通俗方式重新梳理文明演化的关键节点。', '从认知革命、农业革命到科学革命，重新梳理人类社会的形成与演变。', '将历史、经济、宗教与制度变迁串联起来，帮助读者建立宏观认知框架。', '适合对历史、人类文明和社会演化感兴趣的读者。', '读完之后看待社会、国家与制度的角度都会更宏观。')
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
-- 7. demo 用户（答辩演示账号 demo / 123456）
--    迭代三起密码存 BCrypt 哈希（$2a$10$... 共 60 字符），不再是明文。
--    此哈希 = BCrypt("123456")，与 src/main/resources/data.sql 保持一致。
-- ---------------------------------------------------------------------------
INSERT INTO users (username, password, email, phone, role, enabled, created_at)
VALUES ('demo', '$2a$10$KxEpa.B.KXa2jPqIVfNc2.o4UGEmLgIKh25lvwuMk2BqytkfHQSw2', 'demo@bookstore.com', '13800000000', 'CUSTOMER', TRUE, NOW())
ON DUPLICATE KEY UPDATE password = VALUES(password), role = VALUES(role), enabled = VALUES(enabled);

INSERT INTO users (username, password, email, phone, role, enabled, created_at)
VALUES ('admin', '$2a$10$KxEpa.B.KXa2jPqIVfNc2.o4UGEmLgIKh25lvwuMk2BqytkfHQSw2', 'admin@bookstore.com', '13900000000', 'ADMIN', TRUE, NOW())
ON DUPLICATE KEY UPDATE password = VALUES(password), role = VALUES(role), enabled = VALUES(enabled);

-- 作业4可重复的本地图书及演示用户；密码 123456 的 BCrypt 哈希。
INSERT INTO books (id, title, author, stock, price, original_price, image, category,
 category_name, category_label, rating, description, audience, highlight, review, summary)
VALUES ('assignment4-book', 'Java 21 虚拟线程与无状态服务', '书城课程示例', 20, 59.00, 59.00,
 '/cover.jpg', 'tech', '计算机与技术', '技术实践', '未提供评分', '用于验证图书 REST API。',
 '课程学习者', 'Java 21 Virtual Threads', 'JWT 无状态认证示例', '通过受保护接口读取数据库图书。');
INSERT INTO users (username, password, email, role, enabled, created_at)
VALUES ('demo', '$2a$10$KxEpa.B.KXa2jPqIVfNc2.o4UGEmLgIKh25lvwuMk2BqytkfHQSw2', 'demo@bookstore.com', 'CUSTOMER', TRUE, CURRENT_TIMESTAMP);

/**
 * <h2>Config 层 —— 横切配置</h2>
 *
 * <p>用 {@code @Configuration} 注册 Spring Bean，做框架级的全局配置。
 *
 * <h3>本包成员</h3>
 * <ul>
 *   <li>{@link com.homework.bookstore.config.WebConfig} —— <b>CORS 跨域配置</b>，
 *       允许 {@code http://127.0.0.1:5173} 和 {@code http://localhost:5173}
 *       （Vite 开发服务器）访问 {@code /api/**}</li>
 * </ul>
 *
 * <h3>跨域为什么需要这个？</h3>
 * 前端跑在 5173，后端跑在 8080，端口不同算跨域。
 * 浏览器同源策略默认会拦截响应——除非后端在响应头里加 {@code Access-Control-Allow-Origin}。
 * <p>开发模式下 Vite 的 {@code server.proxy} 已经做了同源转发，CORS 配置在生产部署时（前端打成静态文件直连后端）才真正生效，写在这里是为了双保险。
 */
package com.homework.bookstore.config;

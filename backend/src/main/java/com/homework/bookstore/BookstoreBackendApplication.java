package com.homework.bookstore;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 后端工程的启动入口（main 方法所在）。
 *
 * <h2>{@code @SpringBootApplication} 注解干了什么？</h2>
 * 它是一个组合注解，等价于以下三个的合并：
 * <ul>
 *   <li>{@code @Configuration}         —— 声明本类是配置类，可以定义 Bean</li>
 *   <li>{@code @EnableAutoConfiguration} —— <b>自动装配</b>：扫描 classpath，按存在的依赖自动配置组件
 *       <ul>
 *         <li>看到 {@code mysql-connector-j} + {@code spring-jdbc} → 自动建 HikariCP 数据源</li>
 *         <li>看到 {@code spring-boot-starter-data-jpa}            → 自动配 Hibernate + EntityManager</li>
 *         <li>看到 {@code spring-boot-starter-web}                 → 自动启 Tomcat（8080）</li>
 *       </ul>
 *   </li>
 *   <li>{@code @ComponentScan}         —— 扫描本包及子包下所有 {@code @Component}/{@code @Service}/{@code @Repository}/{@code @Controller}/{@code @RestController}</li>
 * </ul>
 *
 * <h2>启动流程</h2>
 * <ol>
 *   <li>{@link SpringApplication#run} 创建 IoC 容器</li>
 *   <li>加载 {@code application.yml} 中的配置</li>
 *   <li>自动配置：建数据源、连接池、JPA EntityManagerFactory、Tomcat</li>
 *   <li>扫描组件、实例化 Bean、构造器注入完成依赖关系</li>
 *   <li>Hibernate 比对 {@code @Entity} 和数据库表结构，按 {@code ddl-auto: update} 同步</li>
 *   <li>执行 {@code data.sql} 种子数据（{@code spring.sql.init.mode: always}）</li>
 *   <li>启动 Tomcat 监听 8080，注册所有 {@code @RestController} 的端点</li>
 *   <li>打印 {@code Started BookstoreBackendApplication in X seconds}</li>
 * </ol>
 *
 * <h2>启动命令</h2>
 * <pre>
 *   mvn spring-boot:run               # 开发模式
 *   mvn package &amp;&amp; java -jar target/bookstore-backend-0.0.1-SNAPSHOT.jar  # 生产模式
 * </pre>
 */
@SpringBootApplication
public class BookstoreBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(BookstoreBackendApplication.class, args);
    }
}

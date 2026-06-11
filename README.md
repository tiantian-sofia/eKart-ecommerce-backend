# eKart E-commerce Backend

[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=ved-asole_eKart-ecommerce-backend&metric=alert_status)](https://sonarcloud.io/summary/new_code?id=ved-asole_eKart-ecommerce-backend)
[![Maven Checks](https://github.com/ved-asole/eKart-ecommerce-backend/actions/workflows/maven-checks.yml/badge.svg)](https://github.com/ved-asole/eKart-ecommerce-backend/actions/workflows/maven-checks.yml)
[![Better Stack Badge](https://uptime.betterstack.com/status-badges/v1/monitor/1h7k6.svg)](https://coders-arena.betteruptime.com)
[![Lines of Code](https://sonarcloud.io/api/project_badges/measure?project=ved-asole_eKart-ecommerce-backend&metric=ncloc)](https://sonarcloud.io/summary/new_code?id=ved-asole_eKart-ecommerce-backend)

**Live Demo**: [ekart.vedasole.cloud](https://ekart.vedasole.cloud)

---

## 目录

- [项目简介](#项目简介)
- [功能概览](#功能概览)
- [技术栈](#技术栈)
- [前置依赖](#前置依赖)
- [本地环境变量与配置项](#本地环境变量与配置项)
- [数据库初始化](#数据库初始化)
- [构建与运行](#构建与运行)
- [运行测试](#运行测试)
- [API 端点概览](#api-端点概览)
- [项目目录结构](#项目目录结构)
- [架构说明](#架构说明)
- [截图预览](#截图预览)
- [ER 图](#er-图)
- [CI/CD](#cicd)
- [License](#license)

---

## 项目简介

eKart 是一个电商后端 REST API 项目，基于 **Java 17** 和 **Spring Boot 3.2.11** 构建，采用 MVC 架构。它为前端（React）提供完整的电商业务接口，涵盖用户认证、商品管理、购物车、订单、支付（Stripe）和邮件通知等核心功能。

项目支持多环境运行：开发环境使用 H2 内存数据库，无需额外安装数据库即可快速启动；生产/UAT/Docker 环境使用 PostgreSQL。

- **作者**: Ved Asole（[ved-asole](https://github.com/ved-asole)）
- **许可证**: Apache-2.0

---

## 功能概览

| 模块 | 功能 |
|------|------|
| **用户认证** | 注册、登录（JWT）、密码重置（邮件发送重置链接） |
| **客户管理** | CRUD、分页查询、数量统计 |
| **商品管理** | CRUD、按名称/描述搜索、按分类筛选、分页查询 |
| **分类管理** | CRUD、支持父子分类（自引用树形结构）、分页查询 |
| **购物车** | 创建购物车、增删改查购物车项 |
| **订单管理** | 创建/更新/删除订单、按客户查询、分页、收入统计、按月收入分析 |
| **支付集成** | Stripe Checkout Session 创建、Stripe Webhook 处理 |
| **邮件通知** | 欢迎邮件、订单确认邮件、密码重置邮件（Thymeleaf 模板） |
| **权限控制** | `ROLE_USER` / `ROLE_ADMIN` 两级角色，方法级安全（`@PreAuthorize`） |
| **缓存** | Spring Cache + Redis（生产环境），Simple Cache（开发/测试环境） |
| **API 文档** | springdoc OpenAPI 3（Swagger UI），内嵌 JWT Bearer 认证方案 |

---

## 技术栈

| 层次 | 技术 | 版本 |
|------|------|------|
| 语言 | Java | 17 |
| 框架 | Spring Boot | 3.2.11 |
| Web | Spring Web MVC | 由 Boot 管理 |
| 安全 | Spring Security + JWT（jjwt） | jjwt 0.11.5 |
| ORM | Spring Data JPA / Hibernate | 由 Boot 管理 |
| 数据库（生产） | PostgreSQL | 运行时驱动由 Boot 管理 |
| 数据库（开发/测试） | H2（内存） | 2.2.224 |
| 缓存 | Redis（Spring Data Redis） | 由 Boot 管理 |
| 支付 | Stripe Java SDK | 25.13.0 |
| 邮件 | Spring Mail + Thymeleaf | 由 Boot 管理 |
| API 文档 | springdoc-openapi-starter-webmvc-ui | 2.5.0 |
| HATEOAS | Spring HATEOAS | 由 Boot 管理 |
| 校验 | Spring Validation（Jakarta） | 由 Boot 管理 |
| 对象映射 | ModelMapper | 3.2.0 |
| 序列化 | Gson | 由 Boot 管理 |
| 代码简化 | Lombok | 1.18.38 |
| 日志 | Logback + Logtail（Better Stack） | logtail 0.3.4 |
| 构建工具 | Maven（含 Maven Wrapper） | maven-compiler 3.11.0 |
| CI/CD | GitHub Actions | — |
| 代码质量 | SonarCloud | — |

---

## 前置依赖

在本地运行本项目，你需要安装以下软件：

| 依赖 | 最低版本 | 说明 |
|------|----------|------|
| **JDK** | 17 | `pom.xml` 中 `<java.version>17</java.version>` 指定 |
| **Maven** | 3.6+ | 也可直接使用项目自带的 `./mvnw`（Maven Wrapper），无需单独安装 |

> **开发模式（`dev` profile）下无需安装 PostgreSQL 或 Redis**——项目使用 H2 内存数据库和 Simple 内存缓存。
>
> 如需以 `prod`/`uat`/`docker` profile 运行，则还需：
> - **PostgreSQL**（具体版本视部署环境而定）
> - **Redis**（默认连接 `localhost:6379`）

---

## 本地环境变量与配置项

### Profile 概览

项目通过 `spring.profiles.active` 切换环境，默认值为 `dev`（在 `application.properties` 中配置）。

| Profile | 数据库 | 缓存 | 适用场景 |
|---------|--------|------|----------|
| `dev` | H2 内存库（`ekartdb`） | Simple（内存） | 本地开发，零依赖快速启动 |
| `test` | H2 内存库（`testdb`） | Simple（内存） | 自动化测试（Redis 自动排除） |
| `prod` | PostgreSQL | Redis | 生产部署 |
| `uat` | PostgreSQL | Redis | UAT 测试 |
| `docker` | PostgreSQL | Redis（`host.docker.internal`） | Docker 容器化部署 |

### 环境变量一览

以下环境变量在配置文件（`application*.properties`）中被引用。**开发模式下所有变量均有内置默认值，可不设置任何环境变量直接启动。**

| 环境变量 | 用途 | 默认值 | 影响的 Profile |
|----------|------|--------|----------------|
| `jsonSecretKey` | JWT 签名密钥（HS256，Base64 编码） | `4u1ChBccGbf/9KZ9ph51V0lQpoKSUc1RmhZsHnQYdW0=` | 全部 |
| `adminPassword` | 自动创建的管理员密码 | `Admin@123` | 全部 |
| `STRIPE_API_KEY` | Stripe API 密钥 | `sk_test_dummy` | 全部 |
| `STRIPE_ENDPOINT_SECRET` | Stripe Webhook 签名密钥 | `dummy` | 全部 |
| `emailUsername` | SMTP 发件邮箱地址 | `dummy@gmail.com` | `prod`/`uat`/`docker` |
| `emailPassword` | SMTP 发件邮箱密码 | `dummy` | `prod`/`uat`/`docker` |
| `neonEkartDBUrl` | 生产 PostgreSQL JDBC URL | 无（必填） | `prod` |
| `neonEkartDBUsername` | 生产数据库用户名 | 无（必填） | `prod` |
| `neonEkartDBPassword` | 生产数据库密码 | 无（必填） | `prod` |
| `uatDBUrl` | UAT PostgreSQL JDBC URL | 无（必填） | `uat` |
| `uatDBUsername` | UAT 数据库用户名 | 无（必填） | `uat` |
| `uatDBPassword` | UAT 数据库密码 | 无（必填） | `uat` |
| `dbUrl` | Docker PostgreSQL JDBC URL | 无（必填） | `docker` |
| `dbUsername` | Docker 数据库用户名 | 无（必填） | `docker` |
| `dbPassword` | Docker 数据库密码 | 无（必填） | `docker` |
| `REDIS_HOST` | Redis 主机地址 | `host.docker.internal` | `docker` |
| `logtailSourceToken` | Better Stack（Logtail）日志源令牌 | 无 | `prod`（可选） |

### 关键配置项（application.properties）

| 配置项 | 值 | 说明 |
|--------|-----|------|
| `server.port` | `8000` | 服务监听端口 |
| `spring.profiles.active` | `dev` | 默认激活的 profile |
| `spring.cache.redis.time-to-live` | `900000`（15 分钟） | Redis 缓存 TTL |
| `spring.mail.host` | `smtp.gmail.com` | SMTP 服务器 |
| `spring.mail.port` | `587` | SMTP 端口（STARTTLS） |
| `server.compression.enabled` | `true` | 响应压缩 |

### 开发模式特殊行为

- **邮件**：`DevMailConfig` 提供 No-op `JavaMailSender`，仅打印日志而不真正发送邮件。
- **数据库 DDL**：`create-drop`，每次启动重建表结构。
- **日志级别**：`DEBUG`（`com.vedasole.ekartecommercebackend` 包）。

---

## 数据库初始化

### 开发模式（dev profile）——无需手动操作

启动应用后，H2 内存数据库会根据 JPA 实体自动创建表结构（`spring.jpa.hibernate.ddl-auto=create-drop`），无需任何手动初始化步骤。

### 生产/UAT/Docker 模式

1. **创建 PostgreSQL 数据库**：在目标 PostgreSQL 实例中创建数据库（如 `ekart`）。
2. **执行 schema.sql**（可选）：项目根目录提供了 `schema.sql`，包含以下 10 张表的 DDL：

   | 表名 | 说明 |
   |------|------|
   | `_user` | 用户账号（email 唯一，角色 USER/ADMIN） |
   | `customer` | 客户信息（1:1 关联 `_user` 和 `address`） |
   | `address` | 地址 |
   | `category` | 商品分类（自引用 `parent_category_id` 支持树形结构） |
   | `product` | 商品（关联 `category`，SKU 唯一，含 name/description 索引） |
   | `cart` | 购物车（1:1 关联 `customer`） |
   | `cart_item` | 购物车项（关联 `cart` 和 `product`） |
   | `"order"` | 订单（关联 `customer` 和 `address`，含 customer 索引） |
   | `order_item` | 订单项（关联 `order` 和 `product`） |
   | `password_reset_token` | 密码重置令牌 |

   ```bash
   psql -h <host> -U <user> -d ekart -f schema.sql
   ```

3. **配置环境变量**：设置对应 profile 的数据库连接变量（见上表）。
4. **自动建表**：`prod`/`uat`/`docker` profile 的 `ddl-auto=update`，Hibernate 会自动同步实体变更到数据库，因此也可以跳过手动执行 `schema.sql`。
5. **管理员账号**：`prod` 和 `uat` profile 下，`ApplicationInitializer` 会在首次启动时自动创建管理员用户 `admin@ekart.com`（密码由环境变量 `adminPassword` 决定）。

---

## 构建与运行

项目根目录包含 Maven Wrapper（`mvnw` / `mvnw.cmd`），无需本地安装 Maven 即可构建。以下命令均在项目根目录执行。

### 安装依赖

```bash
./mvnw install
```

### 启动应用（开发模式）

```bash
./mvnw spring-boot:run
```

应用启动后监听 **http://localhost:8000**。控制台会打印类似日志：

> `Started EkartEcommerceBackendApplication ... on port 8000`

### 指定 Profile 启动

```bash
# 生产模式（需提前配置 PostgreSQL + Redis 环境变量）
./mvnw spring-boot:run -Dspring-boot.run.profiles=prod

# Docker 模式
./mvnw spring-boot:run -Dspring-boot.run.profiles=docker
```

### 构建可部署 JAR

```bash
./mvnw clean package -DskipTests
```

生成的 JAR 文件位于 `target/ekart-ecommerce-backend-0.0.1-SNAPSHOT.jar`，运行方式：

```bash
java -jar target/ekart-ecommerce-backend-0.0.1-SNAPSHOT.jar
```

### Swagger UI（API 文档）

应用启动后访问 **http://localhost:8000/swagger-ui.html** 查看交互式 API 文档。所有接口按 Controller 分组，需要认证的接口支持在 UI 中输入 JWT Bearer Token。

---

## 运行测试

### 执行全部测试

```bash
./mvnw test
```

### 执行单个测试类

```bash
./mvnw test -Dtest=CategoryControllerTest
```

### 测试环境说明

测试使用 `src/test/resources/application.properties` 中的配置：

| 配置 | 值 | 说明 |
|------|-----|------|
| 数据库 | H2 内存库（`testdb`） | `create-drop` DDL |
| 缓存 | Simple（内存） | Redis 自动排除 |
| 管理员账号 | `admin@ekart.com` / `testAdminPassword` | 测试预置 |
| 普通用户 | `normal-user@ekart.com` / `normalUserTestPassword` | 测试预置 |
| Stripe 密钥 | `sk_test_dummy` / `dummy` | 测试占位值 |

### 测试覆盖范围（共 17 个测试文件）

| 类别 | 数量 | 示例 |
|------|------|------|
| Repository 层测试 | 6 | CategoryRepo、ProductRepo、UserRepo 等 |
| Controller 层测试 | 1 | CategoryControllerTest |
| 集成测试 | 1 | CategoryControllerITTest |
| Entity 测试 | 2 | AddressTest、CategoryTest |
| Config 测试 | 2 | ApplicationConfigTest、WebSecurityConfigTest |
| Service 测试 | 1 | UserServiceImplTest |
| Payload 测试 | 1 | CustomerDtoTest |
| Utility 测试 | 1 | TestApplicationInitializer |
| 上下文加载测试 | 1 | EkartEcommerceBackendApplicationTests |

---

## API 端点概览

所有接口前缀为 `/api/v1/`。GET 请求默认公开；POST/PUT/DELETE 需要 JWT 认证（特殊标注除外）。

### Auth（`/api/v1/auth`）— 全部公开

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/authenticate` | 登录，JWT 通过 `Authorization` 响应头返回 |
| GET | `/check-token` | 校验当前 Token 是否有效 |
| POST | `/generate-reset-token` | 发送密码重置邮件 |
| POST | `/reset-password` | 使用重置令牌修改密码 |
| POST | `/validate-reset-token` | 校验重置令牌是否有效 |

### Customers（`/api/v1/customers`）

| 方法 | 路径 | 认证 | 说明 |
|------|------|------|------|
| POST | `/` | **公开** | 注册新客户（返回 JWT） |
| GET | `/` | 公开 | 获取全部客户 |
| GET | `/page` | 公开 | 分页获取客户 |
| GET | `/{customerId}` | 公开 | 按 ID 获取客户 |
| PUT | `/{customerId}` | 需认证 | 更新客户信息 |
| DELETE | `/{customerId}` | **ADMIN** | 删除客户 |
| GET | `/count` | 公开 | 客户总数 |

### Products（`/api/v1/products`）

| 方法 | 路径 | 认证 | 说明 |
|------|------|------|------|
| POST | `/` | 需认证 | 创建商品 |
| GET | `/` | 公开 | 获取全部商品 |
| GET | `/page` | 公开 | 分页获取商品 |
| GET | `/{productId}` | 公开 | 按 ID 获取商品 |
| PUT | `/{productId}` | 需认证 | 更新商品 |
| DELETE | `/{productId}` | 需认证 | 删除商品 |
| GET | `/search?searchKey=` | 公开 | 按名称/描述搜索 |
| GET | `/category/{categoryId}` | 公开 | 按分类获取商品 |
| GET | `/category/{categoryId}/page` | 公开 | 按分类分页获取 |
| GET | `/count` | 公开 | 商品总数 |

### Categories（`/api/v1/categories`）

| 方法 | 路径 | 认证 | 说明 |
|------|------|------|------|
| POST | `/` | 需认证 | 创建分类 |
| GET | `/` | 公开 | 获取全部分类 |
| GET | `/page` | 公开 | 分页获取分类 |
| GET | `/{categoryId}` | 公开 | 按 ID 获取分类 |
| PUT | `/{categoryId}` | 需认证 | 更新分类 |
| DELETE | `/{categoryId}` | 需认证 | 删除分类 |
| GET | `/parent` | 公开 | 仅获取顶级分类 |
| GET | `/parent/page` | 公开 | 分页获取顶级分类 |
| GET | `/count` | 公开 | 分类总数 |

### Shopping Cart（`/api/v1/shopping-cart`）

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/customer/{customerId}` | 为客户创建购物车 |
| PUT | `/{cartId}` | 更新购物车 |
| GET | `/customer/{customerId}` | 按客户获取购物车 |
| DELETE | `/customer/{customerId}` | 删除购物车 |

### Shopping Cart Items（`/api/v1/shopping-cart/{cartId}/items`）

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/` | 添加购物车项 |
| PUT | `/` | 更新购物车项 |
| GET | `/` | 获取购物车全部商品项 |
| GET | `/{cartItemId}` | 获取指定购物车项 |
| DELETE | `/{cartItemId}` | 删除指定购物车项 |
| DELETE | `/` | 清空购物车所有项 |

### Orders（`/api/v1/orders`）

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/` | 创建订单 |
| PUT | `/{orderId}` | 更新订单 |
| GET | `/` | 获取全部订单 |
| GET | `/page` | 分页获取订单 |
| GET | `/{orderId}` | 按 ID 获取订单 |
| DELETE | `/{orderId}` | 删除订单 |
| GET | `/customer/{customerId}` | 按客户获取订单 |
| GET | `/customer/{customerId}/page` | 按客户分页获取订单 |
| GET | `/count` | 订单总数 |
| GET | `/income` | 总收入 |
| GET | `/income-by-month` | 按月收入明细 |

### Order Items（`/api/v1/order/{orderId}/items`）

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/` | 添加订单项 |
| PUT | `/` | 更新订单项 |
| GET | `/` | 获取订单全部项 |
| GET | `/{orderItemId}` | 获取指定订单项 |
| DELETE | `/{orderItemId}` | 删除指定订单项 |
| DELETE | `/` | 清空订单所有项 |

### Payments（`/api/v1/payment`）

| 方法 | 路径 | 认证 | 说明 |
|------|------|------|------|
| POST | `/create-checkout-session` | 需认证 | 创建 Stripe Checkout Session |
| POST | `/webhook/stripe` | **公开** | Stripe Webhook 回调 |

---

## 项目目录结构

```
eKart-ecommerce-backend/
├── .github/
│   ├── ISSUE_TEMPLATE/                    # GitHub Issue 模板
│   └── workflows/
│       └── maven-checks.yml               # CI 流水线配置
├── .mvn/wrapper/                          # Maven Wrapper 运行时
├── src/
│   ├── main/
│   │   ├── java/com/vedasole/ekartecommercebackend/
│   │   │   ├── EkartEcommerceBackendApplication.java   # 应用入口
│   │   │   ├── config/                                 # 配置类
│   │   │   │   ├── ApplicationConfig.java              # 通用 Bean 配置
│   │   │   │   ├── DevMailConfig.java                  # 开发环境邮件 No-op
│   │   │   │   └── SwaggerConfig.java                  # OpenAPI 3 文档配置
│   │   │   ├── controller/                             # REST 控制器（9 个）
│   │   │   │   ├── AuthController.java
│   │   │   │   ├── CategoryController.java
│   │   │   │   ├── CustomerController.java
│   │   │   │   ├── OrderController.java
│   │   │   │   ├── OrderItemController.java
│   │   │   │   ├── PaymentController.java
│   │   │   │   ├── ProductController.java
│   │   │   │   ├── ShoppingCartController.java
│   │   │   │   └── ShoppingCartItemController.java
│   │   │   ├── entity/                                 # JPA 实体（10 个）
│   │   │   ├── exception/                              # 全局异常处理
│   │   │   ├── payload/                                # DTO 对象（17 个）
│   │   │   ├── repository/                             # Spring Data 仓库（10 个）
│   │   │   ├── security/                               # JWT + Spring Security 配置
│   │   │   │   ├── JWTAuthenticationEntryPoint.java
│   │   │   │   ├── JWTAuthenticationFilter.java
│   │   │   │   ├── JwtService.java
│   │   │   │   └── WebSecurityConfig.java
│   │   │   ├── service/                                # 业务逻辑
│   │   │   │   ├── service_interface/                  # 服务接口（11 个）
│   │   │   │   └── service_impl/                       # 服务实现（12 个）
│   │   │   └── utility/                                # 工具类
│   │   │       ├── AppConstant.java                    # 常量定义
│   │   │       └── ApplicationInitializer.java         # 启动初始化（自动创建管理员）
│   │   └── resources/
│   │       ├── application.properties                  # 主配置（端口 8000，默认 dev）
│   │       ├── application-dev.properties              # 开发环境
│   │       ├── application-prod.properties             # 生产环境
│   │       ├── application-uat.properties              # UAT 环境
│   │       ├── application-docker.properties           # Docker 环境
│   │       ├── logback.xml                             # 日志配置
│   │       └── templates/                              # Thymeleaf 邮件模板
│   │           ├── welcome.html
│   │           ├── orderConfirmation.html
│   │           └── resetPassword.html
│   └── test/
│       ├── java/com/vedasole/ekartecommercebackend/    # 测试类（17 个文件）
│       └── resources/
│           └── application.properties                  # 测试专用配置
├── schema.sql                                           # PostgreSQL DDL 参考
├── pom.xml                                              # Maven 构建配置
├── mvnw / mvnw.cmd                                      # Maven Wrapper 脚本
├── LICENSE                                              # Apache-2.0
└── README.md
```

---

## 架构说明

本项目采用 **MVC（Model-View-Controller）** 分层架构：

```
Client (React)
    │
    ▼
Controller 层（REST API，/api/v1/*）
    │
    ▼
Service 层（接口 + 实现分离）
    │
    ▼
Repository 层（Spring Data JPA）
    │
    ▼
Database（H2 / PostgreSQL）
```

**安全层**：Spring Security + JWT 无状态认证，CSRF 已禁用，Session 策略为 `STATELESS`。JWT 令牌有效期 24 小时，使用 HS256 签名。

**订单状态流转**：`ORDER_CREATED` → `ORDER_PLACED` → `ORDER_DISPATCHED` → `ORDER_SHIPPED` → `ORDER_DELIVERED` → `ORDER_COMPLETED`（另有 `EXPIRED`、`CANCELLED`、`FAILED` 终态）。

---

## 截图预览

| 页面 | 截图 |
|------|------|
| Home Page | ![Home Page](eKart_shopping_home.png) |
| All Products | ![All Products](eKart_shopping_all-products.png) |
| Shopping Cart | ![Cart](eKart_shopping_cart.png) |
| Orders | ![Orders](eKart_shopping_orders.png) |
| Admin Dashboard | ![Admin Panel](eKart_shopping_admin-panel.png) |
| Payment Gateway | ![Payment Gateway](eKart_shopping_payment-gateway.png) |

---

## ER 图

![eKart ER Diagram](ekartdb-backend-ER-diagram.png)

---

## CI/CD

项目使用 GitHub Actions 进行持续集成（`.github/workflows/maven-checks.yml`）：

- **触发条件**：push 到 `master`、`develop`、`EK*` 分支，以及所有 Pull Request。
- **流程**：
  1. 启动 Redis 服务容器
  2. `mvn clean install -DskipTests` — 编译构建
  3. `mvn verify` — 验证构建产物
  4. `mvn test` — 运行测试
- **代码质量**：SonarCloud 集成分析。
- **日志监控**：生产环境通过 Logtail 将日志发送至 Better Stack。

---

## License

本项目基于 [Apache-2.0 License](https://github.com/ved-asole/eKart-ecommerce-app/blob/master/LICENSE) 开源。

# eKart 后端部署与运维手册

> 适用仓库：`com.vedasole:ekart-ecommerce-backend:0.0.1-SNAPSHOT`
> 技术栈：Java 17 / Spring Boot 3.2.11 / Maven / PostgreSQL / Redis

---

## 目录

1. [打包构建](#1-打包构建)
2. [容器化部署](#2-容器化部署)
3. [必需外部依赖的接入](#3-必需外部依赖的接入)
4. [生产环境配置与密钥管理](#4-生产环境配置与密钥管理)
5. [健康检查与启动验证](#5-健康检查与启动验证)
6. [日志与常见故障排查](#6-日志与常见故障排查)
7. [回滚要点](#7-回滚要点)

---

## 1. 打包构建

### 1.1 前置要求

| 工具 | 最低版本 | 说明 |
|------|---------|------|
| JDK  | 17      | `pom.xml` 中 `<java.version>17</java.version>` |
| Maven | 3.9+   | 仓库自带 Maven Wrapper（`mvnw` / `mvnw.cmd`），可免安装 |

### 1.2 构建可执行 JAR

```bash
# 使用仓库自带的 Maven Wrapper
./mvnw clean package -DskipTests
```

- `spring-boot-maven-plugin` 会将项目打包为可执行 fat JAR。
- 产物路径：`target/ekart-ecommerce-backend-0.0.1-SNAPSHOT.jar`

如需同时运行测试（需要 Redis 可用于非 dev/test profile）：

```bash
./mvnw clean package
```

### 1.3 仅编译 + 校验（CI 流程参考）

仓库 `.github/workflows/maven-checks.yml` 中的 CI 步骤为：

```bash
./mvnw clean install -DskipTests   # 编译 + 安装
./mvnw verify                       # 校验
./mvnw test                         # 单元/集成测试
```

### 1.4 直接运行（开发/调试）

```bash
./mvnw spring-boot:run
```

默认激活 `dev` profile（`spring.profiles.active=dev`，见 `application.properties`），使用 H2 内存数据库，无需外部依赖。

---

## 2. 容器化部署

### 2.1 当前仓库状态

仓库包含 `.dockerignore` 和 `entrypoint.sh`，但 **不含 Dockerfile** 和 `docker-compose.yml`。仓库已有 `application-docker.properties` profile 供容器环境使用。

### 2.2 推荐的 Dockerfile

以下为基于仓库现有结构的推荐 Dockerfile（需自行在项目根目录创建 `Dockerfile`）：

```dockerfile
# ---- 构建阶段 ----
FROM eclipse-temurin:17-jdk AS build
WORKDIR /app
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN ./mvnw dependency:resolve -DskipTests
COPY src/ src/
RUN ./mvnw clean package -DskipTests

# ---- 运行阶段 ----
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/target/ekart-ecommerce-backend-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8000
ENTRYPOINT ["java", "-jar", "app.jar"]
```

> **注意**：仓库中的 `entrypoint.sh` 使用 `mvn spring-boot:run` 以 dev profile 启动，并在容器内启动 Redis 守护进程。此脚本仅适用于开发环境，**不建议用于生产部署**。

### 2.3 构建与运行镜像

```bash
docker build -t ekart-backend:latest .

docker run -d \
  --name ekart-backend \
  -p 8000:8000 \
  -e spring_profiles_active=docker \
  -e dbUrl="jdbc:postgresql://<DB_HOST>:5432/ekartdb" \
  -e dbUsername="<DB_USER>" \
  -e dbPassword="<DB_PASS>" \
  -e REDIS_HOST="<REDIS_HOST>" \
  -e jsonSecretKey="<JWT_SECRET>" \
  -e STRIPE_API_KEY="<STRIPE_KEY>" \
  -e STRIPE_ENDPOINT_SECRET="<STRIPE_WEBHOOK_SECRET>" \
  -e emailUsername="<GMAIL_USER>" \
  -e emailPassword="<GMAIL_APP_PASSWORD>" \
  -e logtailSourceToken="<LOGTAIL_TOKEN>" \
  -e adminPassword="<ADMIN_PASSWORD>" \
  ekart-backend:latest
```

### 2.4 推荐的 docker-compose.yml

以下为含 PostgreSQL 和 Redis 的完整编排参考（需自行在项目根目录创建 `docker-compose.yml`）：

```yaml
version: "3.9"
services:
  ekart-backend:
    build: .
    ports:
      - "8000:8000"
    environment:
      spring_profiles_active: docker
      dbUrl: jdbc:postgresql://postgres:5432/ekartdb
      dbUsername: ekart
      dbPassword: ${DB_PASSWORD}
      REDIS_HOST: redis
      jsonSecretKey: ${JWT_SECRET}
      STRIPE_API_KEY: ${STRIPE_API_KEY}
      STRIPE_ENDPOINT_SECRET: ${STRIPE_ENDPOINT_SECRET}
      emailUsername: ${EMAIL_USERNAME}
      emailPassword: ${EMAIL_PASSWORD}
      logtailSourceToken: ${LOGTAIL_TOKEN}
      adminPassword: ${ADMIN_PASSWORD}
    depends_on:
      postgres:
        condition: service_healthy
      redis:
        condition: service_healthy

  postgres:
    image: postgres:16-alpine
    environment:
      POSTGRES_DB: ekartdb
      POSTGRES_USER: ekart
      POSTGRES_PASSWORD: ${DB_PASSWORD}
    volumes:
      - pgdata:/var/lib/postgresql/data
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U ekart -d ekartdb"]
      interval: 5s
      retries: 5

  redis:
    image: redis:alpine
    ports:
      - "6379:6379"
    healthcheck:
      test: ["CMD", "redis-cli", "ping"]
      interval: 5s
      retries: 5

volumes:
  pgdata:
```

---

## 3. 必需外部依赖的接入

### 3.1 PostgreSQL 数据库

| 项目 | 说明 |
|------|------|
| 用途 | 主数据存储（用户、商品、订单等） |
| 生产环境 | Neon 托管 PostgreSQL（见 `application-prod.properties`） |
| Docker 环境 | 自建 PostgreSQL（见 `application-docker.properties`） |
| DDL 策略 | `spring.jpa.hibernate.ddl-auto=update`（Hibernate 自动增量更新表结构） |
| 参考 Schema | 仓库中 `schema.sql` 提供完整 DDL 参考（不会被 Spring 自动加载） |

**核心表**：`_user`、`customer`、`category`、`product`、`address`、`cart`、`cart_item`、`"order"`、`order_item`

**首次部署**：Hibernate 会根据 JPA 实体自动建表。`schema.sql` 可用于手动建表或校对。

### 3.2 Redis 缓存

| 项目 | 说明 |
|------|------|
| 用途 | 应用级缓存（`@EnableCaching`） |
| 配置文件 | `application.properties` |
| 配置键 | `spring.data.redis.host`（默认 `localhost`）、`spring.data.redis.port`（`6379`） |
| TTL | `spring.cache.redis.time-to-live=15m` |
| Docker 覆盖 | `REDIS_HOST` 环境变量（默认 `host.docker.internal`） |

> **dev/test profile 不需要 Redis**：`application-dev.properties` 通过 `spring.cache.type=simple` 和 `spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration` 关闭了 Redis 依赖。

### 3.3 邮件服务（Gmail SMTP）

| 配置键 | 值 / 环境变量 |
|--------|---------------|
| `spring.mail.host` | `smtp.gmail.com` |
| `spring.mail.port` | `587` |
| `spring.mail.username` | `${emailUsername}` |
| `spring.mail.password` | `${emailPassword}` |
| `spring.mail.properties.mail.smtp.auth` | `true` |
| `spring.mail.properties.mail.smtp.starttls.enable` | `true` |

- 需要 Gmail 应用专用密码（非账户密码）。
- 邮件模板位于 `src/main/resources/templates/`：`welcome.html`、`orderConfirmation.html`、`resetPassword.html`。
- dev profile 下使用无操作的 `DevMailConfig`，仅打印日志不发送邮件。

### 3.4 Stripe 支付网关

| 配置键 | 环境变量 |
|--------|---------|
| `stripeApiKey` | `${STRIPE_API_KEY}` |
| `stripe.endpoint.secret` | `${STRIPE_ENDPOINT_SECRET}` |

- Stripe Checkout Sessions 模式（重定向式支付流程）。
- Webhook 接收端点：`POST /api/v1/payment/webhook/stripe`。
- 货币单位：INR。
- 支付成功/取消回调至前端：`${frontendDomainUrl}`（默认 `http://localhost:5173`）。

**生产接入**：在 Stripe Dashboard 中配置 Webhook 指向 `https://<YOUR_DOMAIN>/api/v1/payment/webhook/stripe`，事件类型选择 `checkout.session.completed`。

### 3.5 Logtail / Better Stack（远程日志）

| 配置项 | 值 |
|--------|-----|
| 配置文件 | `src/main/resources/logback.xml` |
| Appender | `com.logtail.logback.LogtailAppender` |
| App 名称 | `ekart_logs` |
| Ingest URL | `https://s996098.eu-fsn-3.betterstackdata.com` |
| Token 环境变量 | `logtailSourceToken` |

---

## 4. 生产环境配置与密钥管理

### 4.1 Profile 选择

仓库包含四个 Spring Profile，通过 `spring.profiles.active` 切换：

| Profile | 配置文件 | 数据库 | 缓存 | 适用场景 |
|---------|---------|--------|------|---------|
| `dev` | `application-dev.properties` | H2 内存 | Simple（内存） | 本地开发 |
| `uat` | `application-uat.properties` | PostgreSQL | Redis | 预发布测试 |
| `prod` | `application-prod.properties` | PostgreSQL (Neon) | Redis | 生产环境 |
| `docker` | `application-docker.properties` | PostgreSQL | Redis | 容器化部署 |

**生产启动方式**：

```bash
java -jar target/ekart-ecommerce-backend-0.0.1-SNAPSHOT.jar \
  --spring.profiles.active=prod
```

或通过环境变量：

```bash
export spring_profiles_active=prod
java -jar target/ekart-ecommerce-backend-0.0.1-SNAPSHOT.jar
```

### 4.2 完整环境变量清单

以下为所有需要在生产环境中设置的环境变量：

| 环境变量 | 用于 Profile | 用途 | 是否必填 |
|---------|-------------|------|---------|
| `neonEkartDBUrl` | prod | PostgreSQL JDBC URL | **必填** |
| `neonEkartDBUsername` | prod | 数据库用户名 | **必填** |
| `neonEkartDBPassword` | prod | 数据库密码 | **必填** |
| `dbUrl` | docker | PostgreSQL JDBC URL | **必填** |
| `dbUsername` | docker | 数据库用户名 | **必填** |
| `dbPassword` | docker | 数据库密码 | **必填** |
| `uatDBUrl` | uat | PostgreSQL JDBC URL | **必填** |
| `uatDBUsername` | uat | 数据库用户名 | **必填** |
| `uatDBPassword` | uat | 数据库密码 | **必填** |
| `jsonSecretKey` | 全部 | JWT HS256 签名密钥（Base64，>=256 bits） | **必填** |
| `adminPassword` | prod, uat | 管理员初始密码 | **必填** |
| `STRIPE_API_KEY` | 全部 | Stripe API 密钥 | **必填** |
| `STRIPE_ENDPOINT_SECRET` | 全部 | Stripe Webhook 签名密钥 | **必填** |
| `emailUsername` | 全部 | Gmail 发件地址 | **必填** |
| `emailPassword` | 全部 | Gmail 应用专用密码 | **必填** |
| `logtailSourceToken` | 全部 | Better Stack / Logtail 日志令牌 | 推荐 |
| `frontendDomainUrl` | 全部 | 前端域名（Stripe 回调用） | 推荐 |
| `REDIS_HOST` | docker | Redis 主机地址 | docker 必填 |
| `root.level` | 全部 | logback 根日志级别 | 可选 |

### 4.3 密钥管理最佳实践

1. **切勿使用默认值**：`application.properties` 中的默认值（如 `Admin@123`、`sk_test_dummy`）仅供开发使用，生产环境必须覆盖。
2. **JWT 密钥**：`jsonSecretKey` 必须为 Base64 编码、至少 256 bits（32 字节）的随机值。生成方式：
   ```bash
   openssl rand -base64 32
   ```
3. **不要将密钥写入配置文件**：通过环境变量、K8s Secret、或云平台密钥管理服务注入。
4. **CORS 白名单**：生产环境中 `cors.allowed.origins`（在 `application.properties` 中）限定了允许的前端域名，需确认是否需要覆盖。

### 4.4 管理员账户自动初始化

`ApplicationInitializer` 在 `prod` 和 `uat` profile 下自动运行（`@Profile({"prod","uat"})`）：

- 检查 `admin@ekart.com` 是否已存在。
- 若不存在，则使用 `${admin.password}` 环境变量创建管理员账户，角色为 `ROLE_ADMIN`。

---

## 5. 健康检查与启动验证

### 5.1 当前状态

仓库 **未引入 `spring-boot-starter-actuator`**，没有内置的 `/actuator/health` 端点。

### 5.2 启动成功标志

应用启动成功后，`EkartEcommerceBackendApplication` 会在日志中输出：

```
EkartEcommerceBackendApplication started
Application name: ekart-ecommerce-backend, Port:8000
Cors allowed origins: http://localhost:5173,...
```

### 5.3 手动验证方式

```bash
# 检查应用是否响应（公开端点，无需认证）
curl -s -o /dev/null -w "%{http_code}" http://localhost:8000/api/v1/

# 验证 Swagger UI 可访问
curl -s -o /dev/null -w "%{http_code}" http://localhost:8000/swagger-ui.html

# 验证认证端点
curl -X POST http://localhost:8000/api/v1/auth/authenticate \
  -H "Content-Type: application/json" \
  -d '{"email":"admin@ekart.com","password":"<ADMIN_PASSWORD>"}'
```

### 5.4 容器健康检查配置

在 Dockerfile 或编排工具中添加健康检查：

```dockerfile
HEALTHCHECK --interval=30s --timeout=5s --retries=3 \
  CMD curl -f http://localhost:8000/api/v1/ || exit 1
```

### 5.5 Redis 连通性验证

```bash
redis-cli -h <REDIS_HOST> -p 6379 ping
# 期望返回：PONG
```

### 5.6 数据库连通性验证

```bash
psql -h <DB_HOST> -U <DB_USER> -d ekartdb -c "SELECT 1;"
```

---

## 6. 日志与常见故障排查

### 6.1 日志架构

日志框架为 Logback，配置文件 `src/main/resources/logback.xml` 定义了两个 appender：

| Appender | 类型 | 输出目标 |
|----------|------|---------|
| `Console` | `ConsoleAppender` | 标准输出（stdout） |
| `Logtail` | `LogtailAppender` | Better Stack 远程日志平台 |

**日志格式**（Console）：
```
2024-01-15 10:30:45.123 1234 [main] INFO  c.v.e.EkartEcommerceBackendApp - message
```

**日志级别控制**：
- 根级别由 `${root.level}` 环境变量控制。
- 各 profile 覆盖：dev/uat 为 `DEBUG`，prod 未显式设置（默认为 `root.level` 的值）。
- 生产推荐设置：`root.level=INFO` 或 `root.level=WARN`。

**MDC 字段**（自动添加到 Logtail 日志）：`requestId`（string）、`requestTime`（int）。

### 6.2 常见故障与排查

#### 故障 1：启动失败 — 数据库连接超时

**症状**：
```
com.zaxxer.hikari.pool.HikariPool - Failed to connect to database
org.postgresql.util.PSQLException: Connection refused
```

**排查**：
1. 确认 PostgreSQL 服务已启动且端口可达。
2. 确认环境变量正确（`neonEkartDBUrl` / `dbUrl` 格式须为 `jdbc:postgresql://<host>:<port>/<dbname>`）。
3. 确认数据库用户名和密码正确。
4. 若使用 Neon，确认网络白名单和 SSL 设置。

#### 故障 2：启动失败 — Redis 连接拒绝

**症状**：
```
org.springframework.data.redis.RedisConnectionFailureException:
Unable to connect to Redis
```

**排查**：
1. 确认 Redis 服务已启动：`redis-cli ping`。
2. 若使用 docker profile，确认 `REDIS_HOST` 环境变量指向正确主机。
3. 确认 Redis 端口 `6379` 未被防火墙阻断。
4. 若不需要 Redis，可使用 `dev` profile（自动排除 Redis）。

#### 故障 3：JWT 认证失败 — 401 Unauthorized

**症状**：所有需认证的 API 返回 401。

**排查**：
1. 确认请求头包含 `Authorization: Bearer <token>`。
2. 确认 `jsonSecretKey` 在签发 token 和验证 token 的实例间一致。
3. Token 有效期为 24 小时，过期后需重新通过 `/api/v1/auth/authenticate` 获取。

#### 故障 4：邮件发送失败

**症状**：
```
javax.mail.AuthenticationFailedException: 535-5.7.8 Username and Password not accepted
```

**排查**：
1. 确认使用 Gmail 应用专用密码而非账户密码。
2. 确认 Gmail 账户已开启"两步验证"并生成了应用专用密码。
3. dev profile 下邮件由 `DevMailConfig` 处理，仅打日志不实际发送——这是正常行为。

#### 故障 5：Stripe Webhook 签名验证失败

**症状**：
```
com.stripe.exception.SignatureVerificationException
```

**排查**：
1. 确认 `STRIPE_ENDPOINT_SECRET` 与 Stripe Dashboard 中的 Webhook Signing Secret 一致。
2. 确认请求体未被反向代理修改（Stripe 签名基于原始 body）。

#### 故障 6：Hibernate DDL 更新失败

**症状**：
```
org.hibernate.tool.schema.spi.SchemaManagementException
```

**排查**：
1. 检查数据库用户是否有 DDL 权限（`CREATE TABLE`、`ALTER TABLE`）。
2. 若字段类型变更不兼容，可能需要手动修改数据库。参考 `schema.sql` 了解完整表结构。
3. `spring.jpa.properties.hibernate.globally_quoted_identifiers=true` 确保保留字（如 `order`、`desc`）被正确引用。

#### 故障 7：Logtail 远程日志不可见

**排查**：
1. 确认 `logtailSourceToken` 环境变量已设置且有效。
2. 确认网络可达 `https://s996098.eu-fsn-3.betterstackdata.com`。
3. Console appender 不受影响，可通过 `docker logs` 或 stdout 查看本地日志。

---

## 7. 回滚要点

### 7.1 JAR 包回滚

保留每次部署的 JAR 文件（含版本/时间戳），回滚时替换 JAR 并重启：

```bash
# 停止当前实例
kill $(pgrep -f ekart-ecommerce-backend)

# 替换为上一版本的 JAR
cp /opt/ekart/releases/ekart-ecommerce-backend-<PREV_VERSION>.jar \
   /opt/ekart/current/ekart-ecommerce-backend-0.0.1-SNAPSHOT.jar

# 重新启动
java -jar /opt/ekart/current/ekart-ecommerce-backend-0.0.1-SNAPSHOT.jar \
  --spring.profiles.active=prod &
```

### 7.2 容器回滚

```bash
# 回滚到上一个镜像标签
docker stop ekart-backend
docker rm ekart-backend
docker run -d --name ekart-backend \
  ... (同上环境变量) \
  ekart-backend:<PREV_TAG>
```

### 7.3 数据库回滚注意事项

**这是最关键的回滚风险点。** 该项目使用 `spring.jpa.hibernate.ddl-auto=update`，意味着：

- Hibernate 在启动时**自动增量修改表结构**（加列、建表），但**不会删除列或表**。
- **不可逆操作**：如果新版本添加了非空列（带默认值）或修改了列类型，回滚到旧版本后旧代码不受影响（Hibernate 不会删除新列）。但如果新版本**重命名**了列或表，则回滚后旧版本可能无法找到原始列。
- **建议**：
  1. 每次部署前备份数据库：
     ```bash
     pg_dump -h <DB_HOST> -U <DB_USER> -d ekartdb > ekartdb_backup_$(date +%Y%m%d%H%M%S).sql
     ```
  2. 如果涉及不兼容的 Schema 变更，先在 UAT 环境验证回滚可行性。
  3. 回滚数据库（仅在必要时）：
     ```bash
     psql -h <DB_HOST> -U <DB_USER> -d ekartdb < ekartdb_backup_<TIMESTAMP>.sql
     ```

### 7.4 回滚检查清单

| 步骤 | 操作 | 确认项 |
|------|------|--------|
| 1 | 备份当前数据库 | `pg_dump` 完成 |
| 2 | 停止当前应用 | 进程已终止 / 容器已停止 |
| 3 | 替换为旧版 JAR / 镜像 | 版本号正确 |
| 4 | 启动应用 | 日志中出现 `EkartEcommerceBackendApplication started` |
| 5 | 验证 API 可用 | `curl http://localhost:8000/api/v1/` 返回 200 |
| 6 | 验证管理员登录 | `/api/v1/auth/authenticate` 正常返回 token |
| 7 | 验证 Redis 缓存 | 无 `RedisConnectionFailureException` |
| 8 | 验证 Stripe Webhook | Stripe Dashboard 中无投递失败 |

---

## 附录 A：API 端点速查

| 方法 | 路径 | 认证 | 说明 |
|------|------|------|------|
| POST | `/api/v1/auth/authenticate` | 否 | 登录，获取 JWT |
| GET  | `/api/v1/auth/check-token` | 否 | 校验 JWT 有效性 |
| POST | `/api/v1/auth/generate-reset-token` | 否 | 申请密码重置 |
| POST | `/api/v1/auth/validate-reset-token` | 否 | 验证重置令牌 |
| POST | `/api/v1/auth/reset-password` | 否 | 重置密码 |
| POST | `/api/v1/customers` | 否 | 注册新用户 |
| POST | `/api/v1/payment/create-checkout-session` | 是 | 创建 Stripe 结账会话 |
| POST | `/api/v1/payment/webhook/stripe` | 否 | Stripe Webhook 回调 |
| GET  | 所有端点 | 否 | GET 请求均为公开访问 |
| -    | `/swagger-ui.html` | 否 | Swagger UI 文档 |

## 附录 B：Profile 配置文件对照

| 文件 | Profile | 数据源 |
|------|---------|--------|
| `application.properties` | 公共基础配置 | — |
| `application-dev.properties` | `dev` | H2 内存数据库 |
| `application-uat.properties` | `uat` | PostgreSQL |
| `application-prod.properties` | `prod` | PostgreSQL (Neon) |
| `application-docker.properties` | `docker` | PostgreSQL + Redis |

## 附录 C：项目目录结构

```
ekart-ecommerce-backend/
├── .github/workflows/maven-checks.yml    # CI 流水线
├── .mvn/wrapper/                          # Maven Wrapper
├── src/main/java/com/vedasole/ekartecommercebackend/
│   ├── EkartEcommerceBackendApplication.java   # 启动入口
│   ├── config/                            # ApplicationConfig, DevMailConfig, SwaggerConfig
│   ├── security/                          # WebSecurityConfig, JwtService, JWTAuthenticationFilter
│   ├── controller/                        # REST 控制器
│   ├── entity/                            # JPA 实体
│   ├── repository/                        # Spring Data JPA 仓库
│   ├── service/service_interface/         # 服务接口
│   ├── service/service_impl/              # 服务实现（含 StripeService）
│   ├── payload/                           # DTO
│   ├── exception/                         # 自定义异常
│   └── utility/                           # AppConstant, ApplicationInitializer
├── src/main/resources/
│   ├── application.properties             # 公共配置
│   ├── application-dev.properties         # 开发配置
│   ├── application-uat.properties         # UAT 配置
│   ├── application-prod.properties        # 生产配置
│   ├── application-docker.properties      # Docker 配置
│   ├── logback.xml                        # 日志配置
│   └── templates/                         # 邮件模板 (welcome, orderConfirmation, resetPassword)
├── entrypoint.sh                          # Docker 开发入口脚本
├── schema.sql                             # PostgreSQL DDL 参考
├── mvnw / mvnw.cmd                        # Maven Wrapper
└── pom.xml                                # Maven 构建配置
```

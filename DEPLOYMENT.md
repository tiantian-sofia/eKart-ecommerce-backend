# eKart 后端部署与运维手册

> **项目**: `ekart-ecommerce-backend` (com.vedasole:ekart-ecommerce-backend:0.0.1-SNAPSHOT)
> **框架**: Spring Boot 3.2.11 / Java 17
> **默认端口**: 8000
> **配置文件基线**: `src/main/resources/application.properties`

---

## 目录

1. [打包构建](#1-打包构建)
2. [容器化部署](#2-容器化部署)
3. [外部依赖接入](#3-外部依赖接入)
4. [生产环境配置与密钥管理](#4-生产环境配置与密钥管理)
5. [健康检查与启动验证](#5-健康检查与启动验证)
6. [日志与常见故障排查](#6-日志与常见故障排查)
7. [回滚要点](#7-回滚要点)

---

## 1. 打包构建

### 1.1 环境要求

| 依赖 | 最低版本 | 说明 |
|------|---------|------|
| JDK | 17 | `pom.xml` 中 `<java.version>17</java.version>` |
| Maven | 3.9.5 | 可使用仓库自带的 `./mvnw` (Maven Wrapper v3.2.0) |

验证环境：

```bash
java -version     # 确认输出包含 "17"
./mvnw --version  # 确认 Maven 版本 >= 3.9.5
```

### 1.2 构建命令

```bash
# 完整构建（跳过测试，生成可执行 JAR）
mvn clean install -DskipTests

# 使用 Maven Wrapper（推荐，无需本地安装 Maven）
./mvnw clean install -DskipTests
```

构建产物路径：

```
target/ekart-ecommerce-backend-0.0.1-SNAPSHOT.jar
```

> **说明**: `spring-boot-maven-plugin` 负责将项目打包为可执行的 fat JAR（已自动排除 Lombok）。

### 1.3 运行测试

```bash
# 测试需要 Redis 可用（localhost:6379），CI 中使用 redis:alpine 容器
mvn test
```

> CI 流水线 (`.github/workflows/maven-checks.yml`) 中 Redis 以 `redis:alpine` 服务容器运行，并通过 `redis-cli ping` 做就绪检查。

### 1.4 本地开发模式运行

```bash
./mvnw spring-boot:run
```

默认激活 `dev` profile（由 `application.properties` 中 `spring.profiles.active=dev` 决定），使用 H2 内存数据库 + Simple 缓存，无需外部依赖。

---

## 2. 容器化部署

### 2.1 仓库现状

仓库中包含：
- **`entrypoint.sh`** — 开发用途的入口脚本
- **`.dockerignore`** — 排除 `.env`、`.git`、IDE 文件、`Dockerfile*`、`docker-compose*` 等

仓库中 **不包含**：
- `Dockerfile`（已被 `.dockerignore` 排除）
- `docker-compose.yml` / `docker-compose.yaml`

因此需在部署环境中自行编写 Dockerfile。

### 2.2 推荐 Dockerfile（生产用）

以下 Dockerfile 基于仓库的实际构建方式，使用多阶段构建：

```dockerfile
# ── 阶段 1: 构建 ──
FROM eclipse-temurin:17-jdk AS builder
WORKDIR /build
COPY mvnw pom.xml ./
COPY .mvn .mvn
RUN chmod +x mvnw && ./mvnw dependency:go-offline -B
COPY src src
RUN ./mvnw clean install -DskipTests -B

# ── 阶段 2: 运行 ──
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=builder /build/target/ekart-ecommerce-backend-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8000
ENTRYPOINT ["java", "-jar", "app.jar"]
```

> **注意**: 生产运行镜像只需 JRE，不需要 Maven 或 JDK 完整工具链。

### 2.3 镜像构建与推送

```bash
# 构建镜像
docker build -t ekart-backend:latest .

# 标记并推送至镜像仓库（示例）
docker tag ekart-backend:latest registry.example.com/ekart-backend:<version>
docker push registry.example.com/ekart-backend:<version>
```

### 2.4 容器启动命令

```bash
docker run -d \
  --name ekart-backend \
  -p 8000:8000 \
  -e SPRING_PROFILES_ACTIVE=docker \
  -e dbUrl=jdbc:postgresql://postgres:5432/ekartdb \
  -e dbUsername=ekart \
  -e dbPassword=<DB_PASSWORD> \
  -e REDIS_HOST=redis \
  -e jsonSecretKey=<JWT_SECRET> \
  -e adminPassword=<ADMIN_PASSWORD> \
  -e STRIPE_API_KEY=<STRIPE_KEY> \
  -e STRIPE_ENDPOINT_SECRET=<STRIPE_WEBHOOK_SECRET> \
  -e emailUsername=<SMTP_USER> \
  -e emailPassword=<SMTP_PASSWORD> \
  -e logtailSourceToken=<LOGTAIL_TOKEN> \
  -e root.level=INFO \
  ekart-backend:<version>
```

### 2.5 使用 entrypoint.sh（仅开发环境）

仓库自带的 `entrypoint.sh` 面向开发场景：
- 在容器内启动 Redis (`redis-server --daemonize yes`)
- 以 `dev` profile 和 `mvn spring-boot:run` 方式运行
- 禁用了 Spring DevTools 自动重启 (`-Dspring.devtools.restart.enabled=false`)

**不建议在生产环境使用此脚本。**

### 2.6 Profile 选择

| Profile | 配置文件 | 数据源 | 缓存 | 适用场景 |
|---------|---------|--------|------|---------|
| `dev` | `application-dev.properties` | H2 内存库 (`jdbc:h2:mem:ekartdb`) | Simple (内存) | 本地开发 |
| `docker` | `application-docker.properties` | PostgreSQL (环境变量注入) | Redis | 容器化部署 |
| `uat` | `application-uat.properties` | PostgreSQL (环境变量注入) | Redis (继承 base) | 验收测试 |
| `prod` | `application-prod.properties` | Neon PostgreSQL (环境变量注入) | Redis (继承 base) | 生产环境 |

通过 `-e SPRING_PROFILES_ACTIVE=<profile>` 或 `-Dspring.profiles.active=<profile>` 切换。

---

## 3. 外部依赖接入

### 3.1 依赖总览

| 服务 | 用途 | 必需 Profile | 配置键 |
|------|------|-------------|--------|
| **PostgreSQL** (Neon) | 主数据库 | prod, docker, uat | `spring.datasource.url/username/password` |
| **Redis** | Spring Cache (TTL 15 min) | docker, prod, uat | `spring.data.redis.host/port` |
| **Gmail SMTP** | 邮件发送 (订单确认、密码重置) | 非 dev | `spring.mail.username/password` |
| **Stripe** | 支付处理 & Webhook | 全部 | `stripeApiKey`, `stripe.endpoint.secret` |
| **BetterStack (Logtail)** | 集中日志采集 | 全部 | `logtailSourceToken` (logback.xml) |

### 3.2 PostgreSQL 数据库

**生产环境**使用 Neon Serverless PostgreSQL，通过以下环境变量注入（`application-prod.properties`）：

```properties
spring.datasource.url=${neonEkartDBUrl}
spring.datasource.username=${neonEkartDBUsername}
spring.datasource.password=${neonEkartDBPassword}
spring.datasource.driverClassName=org.postgresql.Driver
spring.jpa.database-platform=org.hibernate.dialect.PostgreSQLDialect
spring.jpa.hibernate.ddl-auto=update
```

**docker 环境**使用独立的环境变量名（`application-docker.properties`）：

```properties
spring.datasource.url=${dbUrl}
spring.datasource.username=${dbUsername}
spring.datasource.password=${dbPassword}
```

> **注意**: 项目未使用 Flyway 或 Liquibase，DDL 管理依赖 Hibernate `ddl-auto=update`。`schema.sql` 仅作为参考 DDL，不会被 Spring Boot 自动执行（因为 `ddl-auto=update` 模式下 Hibernate 自行建表）。

### 3.3 Redis

```properties
# application.properties (base)
spring.data.redis.host=localhost
spring.data.redis.port=6379
spring.cache.type=redis
spring.cache.redis.time-to-live=15m

# application-docker.properties (override)
spring.data.redis.host=${REDIS_HOST:host.docker.internal}
spring.data.redis.port=6379
```

- `dev` profile 中 Redis 被禁用：`spring.cache.type=simple` + `spring.autoconfigure.exclude=...RedisAutoConfiguration`
- 生产 / docker / uat 环境必须有可用的 Redis 实例

### 3.4 Gmail SMTP

```properties
spring.mail.host=smtp.gmail.com
spring.mail.port=587
spring.mail.username=${emailUsername:dummy@gmail.com}
spring.mail.password=${emailPassword:dummy}
spring.mail.properties.mail.smtp.auth=true
spring.mail.properties.mail.smtp.starttls.enable=true
```

- `dev` profile 中 `DevMailConfig.java`（`@Profile("dev")`）提供 no-op `JavaMailSender`，邮件仅记录日志不实际发送
- 生产环境必须提供真实 Gmail 应用专用密码

### 3.5 Stripe 支付

```properties
stripeApiKey=${STRIPE_API_KEY:sk_test_dummy}
stripe.endpoint.secret=${STRIPE_ENDPOINT_SECRET:dummy}
```

- Webhook 回调路径：`POST /api/v1/payment/webhook/stripe`（在 `WebSecurityConfig` 中已配置为公开访问）
- 生产环境需使用 Stripe Live Key（`sk_live_...`）
- Stripe Dashboard 中 Webhook 端点需指向 `https://<your-domain>/api/v1/payment/webhook/stripe`

### 3.6 BetterStack (Logtail) 日志采集

配置于 `src/main/resources/logback.xml`：

```xml
<appender name="Logtail" class="com.logtail.logback.LogtailAppender">
    <appName>ekart_logs</appName>
    <sourceToken>${logtailSourceToken}</sourceToken>
    <ingestUrl>https://s996098.eu-fsn-3.betterstackdata.com</ingestUrl>
    <mdcFields>requestId,requestTime</mdcFields>
    <mdcTypes>string,int</mdcTypes>
</appender>
```

- 环境变量 `logtailSourceToken` 必须设置，否则 Logtail Appender 初始化可能失败
- Ingest URL 固定为 `https://s996098.eu-fsn-3.betterstackdata.com`

---

## 4. 生产环境配置与密钥管理

### 4.1 环境变量完整清单

以下为所有环境必须设置的生产级环境变量（**不得**使用代码中硬编码的默认值）：

| 环境变量 | 用途 | 来源配置文件 | 默认值（仅供开发） |
|---------|------|------------|-----------------|
| `neonEkartDBUrl` | Neon PostgreSQL JDBC URL | `application-prod.properties` | **无（必须设置）** |
| `neonEkartDBUsername` | Neon 数据库用户名 | `application-prod.properties` | **无（必须设置）** |
| `neonEkartDBPassword` | Neon 数据库密码 | `application-prod.properties` | **无（必须设置）** |
| `jsonSecretKey` | JWT 签名密钥 (HS256, Base64, ≥256-bit) | `application.properties` | `4u1ChBccGbf/9KZ9ph51V0lQpoKSUc1RmhZsHnQYdW0=` |
| `adminPassword` | 管理员账户初始密码 | `application.properties` | `Admin@123` |
| `STRIPE_API_KEY` | Stripe API 密钥 | `application.properties` | `sk_test_dummy` |
| `STRIPE_ENDPOINT_SECRET` | Stripe Webhook 签名密钥 | `application.properties` | `dummy` |
| `emailUsername` | Gmail SMTP 用户名 | `application.properties` | `dummy@gmail.com` |
| `emailPassword` | Gmail SMTP 应用专用密码 | `application.properties` | `dummy` |
| `logtailSourceToken` | BetterStack 日志 Source Token | `logback.xml` | **无（必须设置）** |
| `root.level` | 日志根级别 | `logback.xml` | **无（建议设置 INFO）** |
| `frontendDomainUrl` | 前端域名 (邮件链接/重定向) | Java 代码 `@Value` | `http://localhost:5173` |

Docker / UAT profile 额外变量：

| 环境变量 | 用途 |
|---------|------|
| `dbUrl` / `uatDBUrl` | PostgreSQL JDBC URL |
| `dbUsername` / `uatDBUsername` | PostgreSQL 用户名 |
| `dbPassword` / `uatDBPassword` | PostgreSQL 密码 |
| `REDIS_HOST` | Redis 主机名 (docker profile, 默认 `host.docker.internal`) |

### 4.2 密钥管理要点

1. **JWT 密钥 (`jsonSecretKey`)**
   - 必须使用 Base64 编码的 ≥256-bit (32 字节) 密钥
   - 生成方式：`openssl rand -base64 32`
   - 更换密钥会导致所有已签发 JWT 失效，用户需重新登录

2. **管理员密码 (`adminPassword`)**
   - `ApplicationInitializer`（`@Profile({"prod","uat"})`）在应用启动时自动创建管理员账户 `admin@ekart.com`
   - 如果该邮箱用户已存在则跳过创建
   - 生产环境务必设置强密码

3. **Stripe 密钥**
   - 生产使用 `sk_live_...` 前缀的 Live Key
   - Webhook Signing Secret 从 Stripe Dashboard 获取

4. **数据库凭证**
   - `prod` profile 的 `neonEkartDB*` 变量**没有**硬编码默认值，未设置将导致启动失败
   - 建议通过平台级密钥管理（如 Neon Dashboard、Kubernetes Secrets）注入

### 4.3 生产环境 docker run 示例

```bash
docker run -d \
  --name ekart-backend \
  --restart unless-stopped \
  -p 8000:8000 \
  -e SPRING_PROFILES_ACTIVE=prod \
  -e neonEkartDBUrl="jdbc:postgresql://<neon-host>/ekartdb?sslmode=require" \
  -e neonEkartDBUsername="<neon-user>" \
  -e neonEkartDBPassword="<neon-password>" \
  -e jsonSecretKey="$(openssl rand -base64 32)" \
  -e adminPassword="<strong-admin-password>" \
  -e STRIPE_API_KEY="sk_live_<live-key>" \
  -e STRIPE_ENDPOINT_SECRET="whsec_<webhook-secret>" \
  -e emailUsername="ekart-support@vedasole.cloud" \
  -e emailPassword="<gmail-app-password>" \
  -e logtailSourceToken="<betterstack-source-token>" \
  -e root.level=INFO \
  -e frontendDomainUrl="https://ekart-shopping.netlify.app" \
  ekart-backend:<version>
```

### 4.4 CORS 配置

`application.properties` 中的 `cors.allowed.origins` 已包含生产域名：

```properties
cors.allowed.origins=http://localhost:5173,https://ekart.vedasole.cloud,https://ekart-shopping.netlify.app,https://develop--ekart-shopping.netlify.app
```

如需修改，可通过环境变量覆盖该配置键。`WebSecurityConfig` 在启动时读取此值，若为空或 null 将抛出 `IllegalStateException`。

---

## 5. 健康检查与启动验证

### 5.1 当前状态

项目 **未引入** `spring-boot-starter-actuator`，因此不存在 `/actuator/health` 等标准健康检查端点。

### 5.2 启动验证（日志确认）

应用成功启动后，`EkartEcommerceBackendApplication.onApplicationReady()` 会输出以下日志：

```
EkartEcommerceBackendApplication started 🚀
Application name: ekart-ecommerce-backend, Port:8000
Cors allowed origins: <cors.allowed.origins 的值>
```

**验证步骤**：

```bash
# 1. 检查容器运行状态
docker ps --filter name=ekart-backend

# 2. 查看启动日志，确认包含上述标志性输出
docker logs ekart-backend 2>&1 | grep "EkartEcommerceBackendApplication started"

# 3. 等待 ApplicationInitializer 完成（prod/uat profile）
#    日志中可能出现管理员用户创建相关信息
```

### 5.3 接口级存活探测

可用以下方式做基本连通性检查：

```bash
# Swagger UI 页面（公开访问，无需认证）
curl -s -o /dev/null -w "%{http_code}" http://localhost:8000/swagger-ui/index.html
# 预期返回: 200

# API 根路径（公开访问）
curl -s -o /dev/null -w "%{http_code}" http://localhost:8000/api/v1/
# 预期返回: 200 或 302

# OpenAPI JSON 文档
curl -s -o /dev/null -w "%{http_code}" http://localhost:8000/v2/api-docs
# 预期返回: 200
```

> **注意**: 所有 GET 请求在 `WebSecurityConfig` 中配置为 `permitAll()`，因此无需 JWT 即可访问。

### 5.4 推荐：容器编排中的探针配置

如果部署于 Kubernetes 等平台，建议配置：

```yaml
# 存活探针 — 验证端口 8000 可达
livenessProbe:
  httpGet:
    path: /swagger-ui/index.html
    port: 8000
  initialDelaySeconds: 60
  periodSeconds: 30

# 就绪探针 — 验证 Spring 上下文加载完成
readinessProbe:
  httpGet:
    path: /swagger-ui/index.html
    port: 8000
  initialDelaySeconds: 30
  periodSeconds: 10
```

> `initialDelaySeconds` 需根据实际启动耗时调整。首次启动含 Hibernate DDL 更新和管理员用户创建，可能需要 30-60 秒。

---

## 6. 日志与常见故障排查

### 6.1 日志架构

```
应用 ──┬──► Console (stdout) ──► 容器日志 / docker logs
       └──► Logtail Appender ──► BetterStack (https://s996098.eu-fsn-3.betterstackdata.com)
```

- **Console 输出格式**: `%d{yyyy-MM-dd HH:mm:ss.SSS} %-4relative [%thread] %-5level %logger{35} - %msg`
- **BetterStack**: 通过 `com.logtail:logback-logtail:0.3.4` 发送，携带 MDC 字段 `requestId`(string) 和 `requestTime`(int)
- 日志级别由 `logback.xml` 的 `${root.level}` 环境变量控制

### 6.2 日志级别设置

| Profile | 默认级别 | 配置来源 |
|---------|---------|---------|
| dev | DEBUG | `application-dev.properties` → `logging.level.root=DEBUG` |
| uat | DEBUG | `application-uat.properties` → `logging.level.root=DEBUG` |
| docker | 由 `root.level` 环境变量决定 | `logback.xml` |
| prod | 由 `root.level` 环境变量决定 | `logback.xml` |

生产环境建议设置 `root.level=INFO` 或 `WARN`。

### 6.3 查看日志

```bash
# 实时查看容器日志
docker logs -f ekart-backend

# BetterStack Web UI
# 登录 BetterStack Dashboard → 搜索 appName=ekart_logs
# 可按 requestId 追踪单次请求链路
```

### 6.4 常见故障排查

#### 启动失败：数据库连接

```
症状: HikariPool-1 - Exception during pool initialization
      Connection to <host> refused
```

**排查**：
1. 确认 `neonEkartDBUrl`（prod）或 `dbUrl`（docker）环境变量已正确设置
2. 确认数据库主机可达：`docker exec ekart-backend nc -zv <db-host> 5432`
3. Neon 数据库需确认 SSL 连接：JDBC URL 应包含 `?sslmode=require`
4. 确认 `neonEkartDBUsername` / `neonEkartDBPassword` 正确

#### 启动失败：Redis 连接

```
症状: Unable to connect to Redis; nested exception is
      io.lettuce.core.RedisConnectionException
```

**排查**：
1. 确认 Redis 实例运行中：`redis-cli -h <REDIS_HOST> ping`
2. docker profile 中默认连接 `${REDIS_HOST:host.docker.internal}`
3. 如果 Redis 与 Spring Boot 在同一 Docker 网络中，`REDIS_HOST` 应设为 Redis 容器名

#### 启动失败：JWT 密钥

```
症状: The signing key's size is XXX bits which is not secure enough
      for the HS256 algorithm
```

**排查**：
1. `jsonSecretKey` 必须是 Base64 编码的 ≥256-bit (32 字节) 密钥
2. 重新生成：`openssl rand -base64 32`

#### 启动失败：CORS 配置

```
症状: IllegalStateException: CORS allowed origins property
      `cors.allowed.origins` must be configured when credentials are enabled
```

**排查**：
1. 确认 `cors.allowed.origins` 配置键不为空
2. 该值在 `application.properties` 中有默认值，但如被环境变量覆盖为空字符串则会触发此错误

#### 邮件发送失败

```
症状: MailConnectException: Couldn't connect to host, port: smtp.gmail.com, 587
```

**排查**：
1. 确认 `emailUsername` 和 `emailPassword` 环境变量为有效 Gmail 账户
2. Gmail 需使用**应用专用密码**（非账户密码）：Google 账户 → 安全性 → 两步验证 → 应用专用密码
3. 确认容器可访问外部网络（`smtp.gmail.com:587`）

#### Stripe Webhook 签名验证失败

```
症状: SignatureVerificationException: No signatures found matching the expected signature
```

**排查**：
1. 确认 `STRIPE_ENDPOINT_SECRET` 的值与 Stripe Dashboard 中 Webhook 端点的 Signing Secret 一致
2. Webhook URL 必须为 `https://<domain>/api/v1/payment/webhook/stripe`
3. 确认 Stripe Dashboard 中该端点已启用相关事件类型

#### Hibernate DDL 自动更新异常

```
症状: Schema-validation: missing column / wrong column type
```

**排查**：
1. `ddl-auto=update` 只会添加新列/表，**不会**删除或修改已有列
2. 如需修改已有表结构（如删除列、更改类型），需手动执行 SQL 或使用 `schema.sql` 作为参考
3. **切勿**在生产环境使用 `ddl-auto=create-drop`（这是 dev profile 的设置）

---

## 7. 回滚要点

### 7.1 回滚前检查

1. **记录当前版本**：
   ```bash
   docker inspect ekart-backend --format '{{.Config.Image}}'
   ```

2. **确认数据库兼容性**：
   - 由于使用 `ddl-auto=update`，新版本可能已添加新列/表
   - 回滚到旧版本前，确认旧代码不会因缺少新列而报错（Hibernate `update` 模式不会删除列，因此通常安全）
   - 如果新版本执行了手动 DDL（如 `ALTER TABLE`、`DROP COLUMN`），回滚前需恢复这些变更

3. **备份数据库**：
   ```bash
   # Neon 支持 Point-in-Time Recovery，确认 PITR 已开启
   # 或使用 pg_dump 做手动备份
   pg_dump -h <neon-host> -U <user> -d ekartdb -f backup_$(date +%Y%m%d%H%M).sql
   ```

### 7.2 回滚步骤

```bash
# 1. 停止当前容器
docker stop ekart-backend

# 2. 移除当前容器
docker rm ekart-backend

# 3. 使用旧版本镜像启动
docker run -d \
  --name ekart-backend \
  --restart unless-stopped \
  -p 8000:8000 \
  -e SPRING_PROFILES_ACTIVE=prod \
  -e neonEkartDBUrl="<db-url>" \
  -e neonEkartDBUsername="<db-user>" \
  -e neonEkartDBPassword="<db-password>" \
  -e jsonSecretKey="<jwt-secret>" \
  -e adminPassword="<admin-password>" \
  -e STRIPE_API_KEY="<stripe-key>" \
  -e STRIPE_ENDPOINT_SECRET="<stripe-webhook-secret>" \
  -e emailUsername="<smtp-user>" \
  -e emailPassword="<smtp-password>" \
  -e logtailSourceToken="<logtail-token>" \
  -e root.level=INFO \
  -e frontendDomainUrl="https://ekart-shopping.netlify.app" \
  ekart-backend:<previous-version>

# 4. 验证启动成功
docker logs -f ekart-backend
# 等待 "EkartEcommerceBackendApplication started" 日志输出
```

### 7.3 回滚注意事项

| 关注点 | 说明 |
|--------|------|
| **JWT 密钥** | 回滚前后必须使用相同的 `jsonSecretKey`，否则所有在线用户 Token 失效 |
| **数据库 Schema** | `ddl-auto=update` 是单向操作（只加不删）。回滚到旧代码时，数据库中可能残留新版本添加的列/表，通常不影响旧代码运行 |
| **Stripe Webhook** | 如果新版本修改了 Webhook 路径或事件处理逻辑，回滚后需在 Stripe Dashboard 确认端点配置与旧版本匹配 |
| **管理员账户** | `ApplicationInitializer` 仅在 `admin@ekart.com` 不存在时创建，回滚不会重复创建 |
| **缓存** | 回滚后 Redis 中可能残留新版本的缓存数据。建议回滚后清空缓存：`redis-cli FLUSHDB` |
| **CORS** | 确认回滚版本的 `cors.allowed.origins` 包含当前前端域名 |

---

## 附录 A：CI 流水线参考

`.github/workflows/maven-checks.yml` 流水线 "Maven Checks"：

| 步骤 | 命令/动作 |
|------|---------|
| 触发条件 | Push: `master`, `develop`, `EK*`; PR → `master`, `develop` |
| JDK | Temurin 17, Maven 缓存 |
| 服务 | `redis:alpine` on 6379 (health check: `redis-cli ping`) |
| 构建 | `mvn clean install -DskipTests` |
| 验证 | `mvn verify` |
| 等待 Redis | 轮询 `redis-cli -h localhost ping` (最多 10 次, 间隔 5s) |
| 测试 | `mvn test` |

CI 环境变量（均为 fake 值，仅用于通过配置加载）：

```yaml
jsonSecretKey: 'fakeSecretKey'
adminPassword: 'fakeAdminPassword'
stripeApiKey: 'fake-stripe-api-key'
stripeEndpointSecret: 'fake-stripe-endpoint-secret'
frontendDomainUrl: 'http://localhost:5173'
```

## 附录 B：API 公开端点

以下端点在 `WebSecurityConfig` 中配置为无需认证：

| 端点 | 说明 |
|------|------|
| `/api/v1/` | API 根路径 |
| `/api/v1/auth/*` | 认证相关 (登录/注册/密码重置) |
| `POST /api/v1/customers` | 客户注册 |
| `POST /api/v1/payment/webhook/*` | Stripe Webhook |
| `/swagger-ui/**` | Swagger UI |
| `/swagger-resources/**` | Swagger 资源 |
| `/v2/api-docs/**` | OpenAPI 文档 |
| `/h2-console/**` | H2 控制台 (仅 dev) |
| 所有 `GET` 请求 | 全局 GET 公开 |

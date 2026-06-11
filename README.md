# eKart E-Commerce Backend

[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=ved-asole_eKart-ecommerce-backend&metric=alert_status)](https://sonarcloud.io/summary/new_code?id=ved-asole_eKart-ecommerce-backend)
[![Maven Checks](https://github.com/ved-asole/eKart-ecommerce-backend/actions/workflows/maven-checks.yml/badge.svg)](https://github.com/ved-asole/eKart-ecommerce-backend/actions/workflows/maven-checks.yml)
[![Better Stack Badge](https://uptime.betterstack.com/status-badges/v1/monitor/1h7k6.svg)](https://coders-arena.betteruptime.com)
[![Lines of Code](https://sonarcloud.io/api/project_badges/measure?project=ved-asole_eKart-ecommerce-backend&metric=ncloc)](https://sonarcloud.io/summary/new_code?id=ved-asole_eKart-ecommerce-backend)

A RESTful backend API for the eKart e-commerce platform, built with Java and Spring Boot. It exposes endpoints for customer registration and authentication, product and category management, shopping cart operations, order processing, and Stripe-based payments.

**Live demo:** [ekart.vedasole.cloud](https://ekart.vedasole.cloud)

---

## Table of Contents

- [Screenshots](#screenshots)
- [ER Diagram](#er-diagram)
- [Tech Stack](#tech-stack)
- [Prerequisites](#prerequisites)
- [Environment Variables](#environment-variables)
- [Getting Started](#getting-started)
- [Running Tests](#running-tests)
- [API Endpoints](#api-endpoints)
- [Spring Profiles](#spring-profiles)
- [Project Structure](#project-structure)
- [CI/CD](#cicd)
- [License](#license)

---

## Screenshots

| Home | Products | Cart |
|------|----------|------|
| ![Home](eKart_shopping_home.png) | ![Products](eKart_shopping_all-products.png) | ![Cart](eKart_shopping_cart.png) |

| Orders | Admin Dashboard | Payment Gateway |
|--------|-----------------|-----------------|
| ![Orders](eKart_shopping_orders.png) | ![Admin](eKart_shopping_admin-panel.png) | ![Payment](eKart_shopping_payment-gateway.png) |

## ER Diagram

![eKart ER Diagram](ekartdb-backend-ER-diagram.png)

The database schema is documented in [`schema.sql`](schema.sql) and includes 9 tables: `_user`, `customer`, `address`, `category`, `product`, `cart`, `cart_item`, `"order"`, and `order_item`.

---

## Tech Stack

| Layer | Technology |
|-------|------------|
| Language | Java 17 |
| Framework | Spring Boot 3.2.11 |
| Web | Spring Web, Spring HATEOAS |
| Persistence | Spring Data JPA, Hibernate |
| Database | PostgreSQL (prod / uat / docker), H2 in-memory (dev / test) |
| Caching | Spring Cache with Redis (prod), simple in-memory cache (dev / test) |
| Security | Spring Security, JWT (jjwt 0.11.5) |
| Payments | Stripe SDK 25.13.0 |
| API Docs | springdoc-openapi 2.5.0 (Swagger UI) |
| Email | Spring Mail + Thymeleaf templates |
| Logging | Logback + Logtail (logback-logtail 0.3.4) |
| Mapping | ModelMapper 3.2.0, Lombok 1.18.38 |
| Build | Maven 3.9.5 (via Maven Wrapper) |

---

## Prerequisites

- **JDK 17** or later
- **Redis** (only required for `prod` / `uat` / `docker` profiles; the default `dev` profile uses in-memory caching and does **not** need Redis)
- **PostgreSQL** (only required for `prod` / `uat` / `docker` profiles; `dev` uses an embedded H2 database)

> Maven does **not** need to be installed separately. The project includes the Maven Wrapper (`mvnw` / `mvnw.cmd`).

---

## Environment Variables

The `dev` profile works out of the box with built-in defaults (H2 database, in-memory cache, no-op mail sender). For non-dev profiles, you must provide the variables listed below.

### Shared (all profiles)

| Variable | Used in property | Description | Default |
|----------|-----------------|-------------|---------|
| `jsonSecretKey` | `jwt.secret.key` | Base64-encoded key (>= 256 bits) for HS256 JWT signing | built-in dev key |
| `adminPassword` | `admin.password` | Password for the auto-created admin account | `Admin@123` |
| `STRIPE_API_KEY` | `stripeApiKey` | Stripe API secret key | `sk_test_dummy` |
| `STRIPE_ENDPOINT_SECRET` | `stripe.endpoint.secret` | Stripe webhook endpoint secret | `dummy` |
| `emailUsername` | `spring.mail.username` | SMTP username for outbound email | `dummy@gmail.com` |
| `emailPassword` | `spring.mail.password` | SMTP password for outbound email | `dummy` |

### `prod` profile

| Variable | Description |
|----------|-------------|
| `neonEkartDBUrl` | PostgreSQL JDBC URL |
| `neonEkartDBUsername` | Database username |
| `neonEkartDBPassword` | Database password |

### `uat` profile

| Variable | Description |
|----------|-------------|
| `uatDBUrl` | PostgreSQL JDBC URL |
| `uatDBUsername` | Database username |
| `uatDBPassword` | Database password |

### `docker` profile

| Variable | Description | Default |
|----------|-------------|---------|
| `dbUrl` | PostgreSQL JDBC URL | *(required)* |
| `dbUsername` | Database username | *(required)* |
| `dbPassword` | Database password | *(required)* |
| `REDIS_HOST` | Redis server hostname | `host.docker.internal` |

---

## Getting Started

### 1. Clone the repository

```bash
git clone https://github.com/ved-asole/eKart-ecommerce-backend.git
cd eKart-ecommerce-backend
```

### 2. Build the project

```bash
./mvnw clean install -DskipTests
```

On Windows, use `mvnw.cmd` instead of `./mvnw`.

### 3. Run the application

```bash
./mvnw spring-boot:run
```

This starts the server with the default `dev` profile, which:
- Uses an embedded **H2** in-memory database (auto-created, no setup required)
- Uses **simple in-memory cache** (no Redis needed)
- Mocks email sending (logged to console instead of dispatching real emails)

The application starts on **port 8000**. Verify it is running:

```bash
curl http://localhost:8000/api/v1/
```

### 4. Access Swagger UI

Once running, browse the interactive API documentation at:

```
http://localhost:8000/swagger-ui.html
```

Authentication-protected endpoints require a JWT Bearer token. Obtain one by calling `POST /api/v1/auth/authenticate`.

### Running with a specific profile

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=prod
```

Replace `prod` with the desired profile name (`uat`, `docker`). Ensure the corresponding environment variables are set before starting.

---

## Running Tests

```bash
./mvnw test
```

Tests run under the `test` profile, which uses:
- An embedded **H2** in-memory database (`jdbc:h2:mem:testdb`)
- **Simple in-memory cache** (Redis is not required)
- Pre-configured test credentials for JWT, Stripe, and admin accounts

The test suite includes:
- Unit tests for entities, DTOs, configs, and services
- Repository integration tests (via Spring Data JPA + H2)
- Controller tests (`CategoryControllerTest`)
- Integration tests (`CategoryControllerITTest`)
- Security configuration tests (`WebSecurityConfigTest`)

To build and run tests together:

```bash
./mvnw clean verify
```

---

## API Endpoints

All endpoints are prefixed with `/api/v1`. GET endpoints are publicly accessible; most POST/PUT/DELETE endpoints require JWT authentication. Customer registration (`POST /api/v1/customers`) and Stripe webhooks (`POST /api/v1/payment/webhook/*`) are also public.

### Authentication (`/api/v1/auth`)

| Method | Path | Description |
|--------|------|-------------|
| POST | `/auth/authenticate` | Log in with email/password; returns JWT |
| GET | `/auth/check-token` | Validate current JWT |
| POST | `/auth/generate-reset-token` | Request a password-reset email |
| POST | `/auth/validate-reset-token` | Check if a reset token is valid |
| POST | `/auth/reset-password` | Reset password with token |

### Categories (`/api/v1/categories`)

| Method | Path | Description |
|--------|------|-------------|
| POST | `/categories` | Create a category |
| PUT | `/categories/{categoryId}` | Update a category |
| DELETE | `/categories/{categoryId}` | Delete a category |
| GET | `/categories/{categoryId}` | Get a category |
| GET | `/categories` | List all categories |
| GET | `/categories/parent` | List parent categories |
| GET | `/categories/parent/page` | Paginated parent categories |
| GET | `/categories/page` | Paginated categories |
| GET | `/categories/count` | Category count |

### Products (`/api/v1/products`)

| Method | Path | Description |
|--------|------|-------------|
| POST | `/products` | Create a product |
| PUT | `/products/{productId}` | Update a product |
| DELETE | `/products/{productId}` | Delete a product |
| GET | `/products/{productId}` | Get a product |
| GET | `/products` | List all products |
| GET | `/products/search?searchKey=` | Search by name or description |
| GET | `/products/page` | Paginated products |
| GET | `/products/category/{categoryId}` | Products by category |
| GET | `/products/category/{categoryId}/page` | Paginated products by category |
| GET | `/products/count` | Product count |

### Customers (`/api/v1/customers`)

| Method | Path | Description |
|--------|------|-------------|
| POST | `/customers` | Register a new customer (public) |
| PUT | `/customers/{customerId}` | Update customer |
| DELETE | `/customers/{customerId}` | Delete customer (ADMIN only) |
| GET | `/customers/{customerId}` | Get customer |
| GET | `/customers` | List all customers |
| GET | `/customers/page` | Paginated customers |
| GET | `/customers/count` | Customer count |

### Orders (`/api/v1/orders`)

| Method | Path | Description |
|--------|------|-------------|
| POST | `/orders` | Create an order |
| PUT | `/orders/{orderId}` | Update an order |
| DELETE | `/orders/{orderId}` | Delete an order |
| GET | `/orders/{orderId}` | Get an order |
| GET | `/orders` | List all orders |
| GET | `/orders/page` | Paginated orders |
| GET | `/orders/customer/{customerId}` | Orders by customer |
| GET | `/orders/customer/{customerId}/page` | Paginated orders by customer |
| GET | `/orders/count` | Order count |
| GET | `/orders/income` | Total income |
| GET | `/orders/income-by-month` | Monthly income breakdown |

### Order Items (`/api/v1/order/{orderId}/items`)

| Method | Path | Description |
|--------|------|-------------|
| POST | `/order/{orderId}/items` | Add an order item |
| PUT | `/order/{orderId}/items` | Update an order item |
| DELETE | `/order/{orderId}/items/{orderItemId}` | Delete an order item |
| DELETE | `/order/{orderId}/items` | Delete all items in an order |
| GET | `/order/{orderId}/items/{orderItemId}` | Get an order item |
| GET | `/order/{orderId}/items` | List items in an order |

### Shopping Cart (`/api/v1/shopping-cart`)

| Method | Path | Description |
|--------|------|-------------|
| POST | `/shopping-cart/customer/{customerId}` | Create a cart |
| PUT | `/shopping-cart/{cartId}` | Add/update item in cart |
| DELETE | `/shopping-cart/customer/{customerId}` | Delete a cart |
| GET | `/shopping-cart/customer/{customerId}` | Get cart by customer |

### Cart Items (`/api/v1/shopping-cart/{cartId}/items`)

| Method | Path | Description |
|--------|------|-------------|
| POST | `/shopping-cart/{cartId}/items` | Add a cart item |
| PUT | `/shopping-cart/{cartId}/items` | Update a cart item |
| DELETE | `/shopping-cart/{cartId}/items/{cartItemId}` | Delete a cart item |
| DELETE | `/shopping-cart/{cartId}/items` | Delete all items in cart |
| GET | `/shopping-cart/{cartId}/items/{cartItemId}` | Get a cart item |
| GET | `/shopping-cart/{cartId}/items` | List items in cart |

### Payments (`/api/v1/payment`)

| Method | Path | Description |
|--------|------|-------------|
| POST | `/payment/create-checkout-session` | Create a Stripe checkout session |
| POST | `/payment/webhook/stripe` | Stripe webhook endpoint (public) |

---

## Spring Profiles

| Profile | Database | Cache | Email | Usage |
|---------|----------|-------|-------|-------|
| `dev` (default) | H2 in-memory | Simple (in-memory) | No-op (logged) | Local development |
| `test` | H2 in-memory | Simple (in-memory) | N/A | Automated tests |
| `prod` | PostgreSQL (Neon) | Redis | SMTP (Gmail) | Production |
| `uat` | PostgreSQL | Redis | SMTP (Gmail) | User acceptance testing |
| `docker` | PostgreSQL | Redis | SMTP (Gmail) | Containerized deployment |

The active profile is set in `application.properties` (`spring.profiles.active=dev`) and can be overridden at runtime via `-Dspring-boot.run.profiles=<profile>` or the `SPRING_PROFILES_ACTIVE` environment variable.

---

## Project Structure

```
eKart-ecommerce-backend/
├── .github/workflows/
│   └── maven-checks.yml              # CI pipeline
├── .mvn/wrapper/                      # Maven Wrapper JARs & config
├── src/
│   ├── main/
│   │   ├── java/com/vedasole/ekartecommercebackend/
│   │   │   ├── EkartEcommerceBackendApplication.java   # Application entry point
│   │   │   ├── config/                # ApplicationConfig, DevMailConfig, SwaggerConfig
│   │   │   ├── controller/            # REST controllers (9 classes)
│   │   │   ├── entity/                # JPA entities (10 classes)
│   │   │   ├── exception/             # APIException, GlobalExceptionHandler, ResourceNotFoundException
│   │   │   ├── payload/               # DTOs for request/response
│   │   │   ├── repository/            # Spring Data JPA repositories (9 interfaces)
│   │   │   ├── security/              # JWT filter, service, entry point, WebSecurityConfig
│   │   │   ├── service/
│   │   │   │   ├── service_impl/      # Service implementations (11 classes)
│   │   │   │   └── service_interface/ # Service interfaces (10 interfaces)
│   │   │   └── utility/               # AppConstant, ApplicationInitializer
│   │   └── resources/
│   │       ├── application.properties             # Shared configuration
│   │       ├── application-dev.properties          # Dev profile (H2, no Redis)
│   │       ├── application-prod.properties         # Production profile (PostgreSQL)
│   │       ├── application-uat.properties          # UAT profile (PostgreSQL)
│   │       ├── application-docker.properties       # Docker profile (PostgreSQL + Redis)
│   │       └── templates/                          # Thymeleaf email templates
│   │           ├── orderConfirmation.html
│   │           ├── resetPassword.html
│   │           └── welcome.html
│   └── test/
│       ├── java/com/vedasole/ekartecommercebackend/  # Test classes
│       └── resources/application.properties          # Test configuration
├── schema.sql                         # Database DDL (reference only, Hibernate manages schema)
├── ekartdb-ER.drawio                  # ER diagram source (draw.io)
├── entrypoint.sh                      # Docker entrypoint (starts Redis + app with dev profile)
├── mvnw / mvnw.cmd                   # Maven Wrapper scripts
├── pom.xml                            # Maven build descriptor
├── CODE_OF_CONDUCT.md
└── LICENSE                            # Apache License 2.0
```

---

## CI/CD

The project uses **GitHub Actions** (defined in `.github/workflows/maven-checks.yml`). The workflow runs on pushes to `master`, `develop`, and `EK*` branches, as well as pull requests targeting `master` or `develop`.

Steps executed:
1. Checkout code
2. Set up JDK 17 (Temurin) with Maven caching
3. Start a Redis service container
4. Build with `mvn clean install -DskipTests`
5. Verify with `mvn verify`
6. Run tests with `mvn test`

---

## License

This project is licensed under the [Apache License 2.0](LICENSE).

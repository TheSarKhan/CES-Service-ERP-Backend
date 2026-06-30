# CES Service Management System — Backend

Spring Boot 3.3.5 / Java 21 backend for the **CES Service Management System** — a
multi-branch technical-service platform for construction-equipment services.

This repository is the **foundation layer**: build configuration, security/JWT,
multi-branch tenancy, the common API envelope and error model, the auth and branch
modules, and infrastructure stubs (Redis, CES ERP Feign client, PDF generation).
Business modules (vehicles, work orders, costing, warehouse, RBAC, etc.) and the
Flyway migrations are added by other agents.

---

## Tech stack

| Concern        | Technology |
|----------------|------------|
| Language       | Java 21 (LTS) |
| Framework      | Spring Boot 3.3.5 |
| Security       | Spring Security 6, JWT (jjwt 0.12.6, HS256) |
| Persistence    | Spring Data JPA + PostgreSQL 16 |
| Migrations     | Flyway 10 (schema `ces_service`) |
| Cache / tokens | Spring Data Redis 7 |
| Mapping        | MapStruct 1.6 + Lombok |
| API docs       | SpringDoc OpenAPI 2.6 (Swagger UI) |
| ERP client     | Spring Cloud OpenFeign |
| PDF            | Thymeleaf + Flying Saucer (iText5) |
| Excel          | Apache POI |
| Tests          | JUnit 5, Spring Boot Test, Testcontainers |

---

## Requirements

- JDK 21
- PostgreSQL 16 (schema `ces_service`)
- Redis 7
- Docker (optional, for the containerised runtime)

The bundled Maven Wrapper (`./mvnw`) downloads Maven 3.9.9 on first use — no local
Maven install required.

---

## Configuration

All settings live in `src/main/resources/application*.yml` and are overridable via
environment variables. Key variables (see the root `.env.example`):

| Variable | Purpose |
|----------|---------|
| `SPRING_DATASOURCE_URL` / `_USERNAME` / `_PASSWORD` | PostgreSQL connection |
| `SPRING_DATA_REDIS_HOST` / `_PORT` / `_PASSWORD`    | Redis connection |
| `JWT_SECRET`                                        | Base64 256-bit signing secret |
| `JWT_EXPIRATION_MS`                                 | Access token TTL (default 1 h) |
| `JWT_REFRESH_EXPIRATION_MS`                         | Refresh token TTL (default 7 d) |
| `CES_ERP_BASE_URL` / `CES_ERP_API_KEY`             | CES ERP integration |
| `SPRING_PROFILES_ACTIVE`                            | `dev` or `prod` |

- **dev** profile: Swagger enabled, DEBUG logging.
- **prod** profile: Swagger disabled, reduced logging.

---

## Build & run

```bash
# Build (skip tests)
./mvnw -B package -DskipTests

# Run locally with the dev profile
SPRING_PROFILES_ACTIVE=dev ./mvnw spring-boot:run

# Run the packaged jar
java -jar target/ces-service.jar
```

### Docker

```bash
# Production image
docker build -t ces-service-backend --target prod .

# Development image (hot reload + JVM debug on :5005)
docker build -t ces-service-backend-dev --target dev .
```

The backend is orchestrated together with PostgreSQL, Redis, the frontend, and
Nginx via the root `docker-compose.yml` / `docker-compose.dev.yml`.

---

## API conventions

- Base path: `/api/v1`
- Every authenticated request carries `Authorization: Bearer <token>` and
  `X-Branch-Id: <branch-uuid>`.
- Success envelope: `{ "success": true, "data": ..., "meta": ..., "timestamp": ... }`
- Error envelope: `{ "success": false, "error": { "code", "message", "details" }, "timestamp": ... }`
- Pagination: `?page=1&size=20&sort=created_at&dir=desc` (page is 1-based, max size 100).
- Swagger UI (dev only): `http://localhost:8080/swagger-ui.html`
- Health: `http://localhost:8080/actuator/health`

---

## Package layout

```
com.ces.service
├── CesServiceApplication          # @SpringBootApplication entry point
├── config/                        # Security, Redis, JPA auditing, CORS, OpenAPI
├── common/
│   ├── entity/BaseEntity          # id, branch_id, audit columns, soft delete
│   ├── dto/                       # ApiResponse, PageResponse, PageMeta, ErrorResponse
│   ├── exception/                 # ErrorCode, BusinessException, GlobalExceptionHandler
│   ├── filter/                    # RateLimit → JwtAuth → BranchContext filters
│   └── security/                  # BranchContext, SecurityUtils, CesUserPrincipal
├── module/
│   ├── auth/                      # login / refresh / logout / switch-branch / reset
│   └── branch/                    # branch CRUD (multi-branch anchor)
└── infrastructure/
    ├── redis/                     # RedisTokenStore, CacheKeys
    ├── erp/                       # ErpApiClient (Feign) + DTOs + config
    └── pdf/                       # PdfGeneratorService (Thymeleaf + Flying Saucer)
```

> The user/role/permission persistence (RBAC) lives under `module.user` /
> `module.role` and is provided by a separate agent. `AuthService` depends on it
> through the `AuthUserGateway` port; a `@ConditionalOnMissingBean` placeholder
> keeps the context bootable until the real adapter is registered.

---

## Security model

- Stateless JWT; filter chain order: **CORS → RateLimit → JwtAuth → BranchContext**.
- Method-level authorization via `@PreAuthorize("hasAuthority('PERMISSION_CODE')")`.
- Token revocation (logout / password change) via a Redis `jti` blacklist.
- Brute-force protection on `/auth/login` (5 failed attempts → 15-minute block).
- Row-level multi-branch tenancy: queries filter by `branch_id`; the active branch
  is validated against the token and bound to `BranchContext`.

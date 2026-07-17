# AIChat

基于 Vue 3 和 Spring Boot 的 AI 对话与运营管理系统，提供流式 AI 聊天、用户余额与签到、JWT 认证，以及管理员用户管理和充值退款能力。

## 功能特性

- Access Token 与 HttpOnly Refresh Token 双令牌认证
- 基于角色的菜单与接口权限控制
- 基于 OpenAI 兼容 Responses API 的 SSE 流式对话
- 会话历史、搜索、自动标题与逻辑删除
- AI 对话扣费、失败退款、每日签到奖励与余额流水
- 管理员仪表盘、用户管理、充值、退款与敏感操作审计
- 自适应桌面端与移动端网页界面

## 架构

```text
浏览器
  |
  | :8080
  v
Nginx（Vue 单页应用 + /api 反向代理）
  |
  v
Spring Boot
  |          |
  v          v
MySQL      Redis
```

Docker 部署只对外开放 `8080` 端口。Spring Boot、MySQL 和 Redis 均处于 Docker 私有网络中。

## 技术栈

| 分层 | 技术 |
| --- | --- |
| 前端 | Vue 3、TypeScript、Vite、Pinia、Element Plus、ECharts、Axios |
| 后端 | Java 21、Spring Boot 4、Spring Security、MyBatis-Plus、Flyway、Spring AI |
| 数据存储 | MySQL 8、Redis 7 |
| 部署 | Docker Compose、Nginx |

## 目录结构

```text
.
├── backend/                 # Spring Boot API
├── frontend/                # Vue 前端应用
├── docker-compose.yml       # 生产环境服务编排
├── .env.example             # 生产环境变量模板
├── deploy.sh                # 服务器一键部署脚本
└── DEPLOY.md                # Docker 详细运维说明
```

## Docker 快速部署

需要先安装 Docker Engine 和 Docker Compose Plugin。

```sh
cp .env.example .env
vi .env
chmod +x deploy.sh
./deploy.sh
```

部署前必须在 `.env` 中填写所有 `replace_with_...` 占位值，尤其是 OpenAI 兼容接口地址、API Key、模型名、数据库密码、Redis 密码和高强度 JWT 密钥。通过外部 HTTPS 反向代理部署域名时，还应设置 `CORS_ALLOWED_ORIGINS=https://你的域名`，不要带结尾斜杠。

首次部署时，请设置 `ADMIN_BOOTSTRAP_ENABLED=true` 并填写 `INITIAL_ADMIN_*`。部署脚本会等待管理员初始化完成，自动把开关设为 `false` 并重建后端，因此后续启动无法再次初始化管理员。

部署完成后访问：`http://服务器IP:8080`。

完整的服务器准备、备份、升级、日志和排障流程见 [DEPLOY.md](DEPLOY.md)。

## 本地开发

前置条件：

- Java 21
- Node.js 22+
- pnpm 11+
- MySQL 8
- Redis 7

在 `backend/src/main/resources/application-local.properties` 或系统环境变量中配置本地密钥，然后分别启动后端和前端：

```sh
cd backend
./mvnw spring-boot:run

cd ../frontend
pnpm install
pnpm dev
```

Vite 开发服务器默认会把 `/api` 代理到 `http://127.0.0.1:8080`。

## 验证命令

```sh
cd backend && ./mvnw test
cd frontend && pnpm build
docker compose --env-file .env.example config
```

## 安全说明

- 不要提交 `.env`、API Key、数据库密码或 JWT 密钥。
- 仅开放 `8080` 给用户访问，不要公开 MySQL 或 Redis 端口。
- 除非明确需要永久删除数据，否则不要执行 `docker compose down -v`。
- 公网生产环境建议在外部反向代理或负载均衡器中配置 HTTPS。

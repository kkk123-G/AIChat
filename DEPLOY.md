# Docker 部署说明

本部署方案仅对外开放一个端口：`8080`。Nginx 提供 Vue 单页应用，并将 `/api` 反向代理到 Spring Boot 容器；MySQL 与 Redis 仅在 Docker 私有网络中运行，没有宿主机端口映射。

## 首次部署

1. 在 Linux 服务器上安装 Docker Engine 与 Docker Compose Plugin。
2. 将完整的 `AIChat` 目录上传到服务器，其中必须包含 `backend`、`frontend`、`docker-compose.yml`、`.env.example` 和 `deploy.sh`。
3. 在项目目录创建并填写生产环境变量文件：

   ```sh
   cp .env.example .env
   vi .env
   chmod +x deploy.sh
   ./deploy.sh
   ```

4. 在 `.env` 中替换全部 `replace_with_...` 占位值后，访问 `http://服务器IP:8080`。

首次部署时保持 `ADMIN_BOOTSTRAP_ENABLED=true`，并填写三个 `INITIAL_ADMIN_*` 变量。`deploy.sh` 会等待后端首次启动完成，自动将该开关改为 `false`，然后重建后端容器。因此，之后的部署和重启都不会再启用管理员初始化。

## 日常运维

```sh
# 更新项目文件后的重新部署
./deploy.sh

# 查看全部服务日志
docker compose logs -f

# 仅查看后端日志
docker compose logs -f backend

# 停止容器，但保留数据库、Redis 和应用日志
docker compose down

# 启动已有镜像和数据卷
docker compose up -d
```

除非确认要永久删除数据库与 Redis 数据，否则不要执行 `docker compose down -v`。

## 安全说明

- 妥善保存 `.env`，其中包含数据库、Redis、JWT、OpenAI 和首次管理员凭据。
- 使用高强度随机 `JWT_SECRET`，可通过 `openssl rand -base64 48` 生成。
- MySQL 与 Redis 不应映射宿主机端口，用户只需要访问 `8080`。
- 前端和 API 使用同源 `/api` 路径，不需要配置公开的后端地址或生产环境 CORS。

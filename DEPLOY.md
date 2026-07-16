# Docker Deployment

This deployment exposes only one host port: `8080`. Nginx serves the Vue SPA
and proxies `/api` to the Spring Boot container. MySQL and Redis are private
Docker-network services and have no host port mappings.

## First Deployment

1. Install Docker Engine and the Docker Compose plugin on the Linux server.
2. Copy the whole `AIChat` directory to the server, including `backend`,
   `frontend`, `docker-compose.yml`, `.env.example`, and `deploy.sh`.
3. In that directory, create the production environment file and set every
   `replace_with_...` value:

   ```sh
   cp .env.example .env
   vi .env
   chmod +x deploy.sh
   ./deploy.sh
   ```

4. Open `http://SERVER_IP:8080`.

For the first deployment only, leave `ADMIN_BOOTSTRAP_ENABLED=true` and fill
the three `INITIAL_ADMIN_*` values. `deploy.sh` changes that line to `false`
after the backend has completed its first startup, then recreates the backend.
Every later deployment and restart has administrator bootstrapping disabled.

## Daily Operations

```sh
# Upgrade after replacing the project files
./deploy.sh

# View all service logs
docker compose logs -f

# View only backend logs
docker compose logs -f backend

# Stop containers while preserving database, Redis, and application logs
docker compose down

# Start existing images and volumes
docker compose up -d
```

Never run `docker compose down -v` unless the database and Redis data should
be permanently removed.

## Security Notes

- Keep `.env` private. It contains database, Redis, JWT, OpenAI, and optional
  first-admin credentials.
- Use a long random `JWT_SECRET`; `openssl rand -base64 48` is suitable.
- MySQL and Redis have no host-facing ports. Only `8080` is public.
- The frontend calls the same-origin `/api` path, so no public backend origin
  or production CORS configuration is required.

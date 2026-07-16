#!/usr/bin/env sh
set -eu

if [ ! -f .env ]; then
  echo "Missing .env. Create it from .env.example and set all required secrets."
  exit 1
fi

bootstrap_enabled=false
if grep -qx 'ADMIN_BOOTSTRAP_ENABLED=true' .env; then
  bootstrap_enabled=true
fi

docker compose up -d --build

# Wait until ApplicationRunner has created the administrator before disabling
# bootstrap and recreating the backend with the false value.
if [ "$bootstrap_enabled" = true ]; then
  attempts=0
  until docker compose logs backend 2>&1 | grep -q 'Started BackendApplication'; do
    attempts=$((attempts + 1))
    if [ "$attempts" -ge 60 ]; then
      echo "Backend did not finish starting. Administrator bootstrap remains enabled in .env."
      exit 1
    fi
    sleep 2
  done

  sed -i 's/^ADMIN_BOOTSTRAP_ENABLED=true$/ADMIN_BOOTSTRAP_ENABLED=false/' .env
  docker compose up -d --force-recreate backend
  echo "Initial administrator bootstrap has been disabled for future starts."
fi

docker compose ps

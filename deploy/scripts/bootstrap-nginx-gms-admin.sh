#!/usr/bin/env bash
set -euo pipefail

APP_ROOT="${APP_ROOT:-/opt/gms-admin}"
NGINX_CONF_SOURCE="${NGINX_CONF_SOURCE:-deploy/nginx/gms-admin.conf}"

if ! command -v nginx >/dev/null 2>&1; then
  if command -v dnf >/dev/null 2>&1; then
    dnf install -y nginx
  elif command -v yum >/dev/null 2>&1; then
    yum install -y nginx
  elif command -v apt-get >/dev/null 2>&1; then
    apt-get update
    DEBIAN_FRONTEND=noninteractive apt-get install -y nginx
  else
    echo "No supported package manager found for installing nginx" >&2
    exit 1
  fi
fi

install -d "${APP_ROOT}/releases/initial"

if [ ! -f "${APP_ROOT}/releases/initial/index.html" ]; then
  cat > "${APP_ROOT}/releases/initial/index.html" <<'HTML'
<!doctype html>
<html lang="zh-CN">
  <head>
    <meta charset="UTF-8" />
    <title>GMS Admin</title>
  </head>
  <body>GMS Admin is waiting for the first GitHub Actions deployment.</body>
</html>
HTML
fi

ln -sfn "${APP_ROOT}/releases/initial" "${APP_ROOT}/current"
install -m 0644 "${NGINX_CONF_SOURCE}" /etc/nginx/conf.d/gms-admin.conf

nginx -t
systemctl enable --now nginx
systemctl reload nginx || systemctl restart nginx

if command -v firewall-cmd >/dev/null 2>&1 && firewall-cmd --state >/dev/null 2>&1; then
  firewall-cmd --permanent --add-port=7431/tcp
  firewall-cmd --reload
fi

if command -v ufw >/dev/null 2>&1 && ufw status | grep -q '^Status: active'; then
  ufw allow 7431/tcp
fi

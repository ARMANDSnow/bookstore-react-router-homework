#!/usr/bin/env bash
set -euo pipefail
project_root="$(cd "$(dirname "$0")/../.." && pwd)"
java_cmd="${JAVA_HOME:+$JAVA_HOME/bin/}java"
java_major="$($java_cmd -version 2>&1 | sed -n '1s/.*version "\([0-9]*\).*/\1/p')"
if [[ "$java_major" != 21 ]]; then
  echo '作业4验收请设置 JAVA_HOME 指向 JDK 21。' >&2
  exit 1
fi
cd "$project_root/backend"
# 可通过 MAVEN_OPTS 指定本机 Maven 缓存位置；不把依赖打进提交包。
exec mvn spring-boot:run -Dspring-boot.run.profiles=assignment4 \
  -Dspring-boot.run.arguments="--server.address=127.0.0.1 --server.port=${PORT:-8084} --bookstore.catalog.simulated-delay-ms=${CATALOG_DELAY_MS:-200}"

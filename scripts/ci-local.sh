#!/usr/bin/env bash
# Reproduz localmente as mesmas etapas do pipeline (.github/workflows/ci.yml).
# Requisitos: Java 21, Docker (opcional) e gitleaks (opcional).
#
#   ./scripts/ci-local.sh            # validacao + build/testes + artefato
#   ./scripts/ci-local.sh --docker   # ... + imagem e smoke test
set -euo pipefail
cd "$(dirname "$0")/.."

etapa() { printf '\n==== %s ====\n' "$*"; }

etapa "1. Validacao inicial"
for f in pom.xml mvnw .mvn/wrapper/maven-wrapper.properties Dockerfile src/main/resources/application.properties; do
  [ -f "$f" ] || { echo "ERRO: arquivo obrigatorio ausente: $f"; exit 1; }
done
[ -x mvnw ] || { echo "ERRO: mvnw sem permissao de execucao (chmod +x mvnw)"; exit 1; }
if command -v gitleaks >/dev/null 2>&1; then
  gitleaks git . --redact
else
  echo "AVISO: gitleaks nao instalado; varredura de segredos ignorada nesta execucao local."
fi

etapa "2. Build e testes"
./mvnw --batch-mode --no-transfer-progress clean verify
python3 scripts/resumo_testes.py target/surefire-reports || true

etapa "Artefato"
sha="$(git rev-parse --short=7 HEAD 2>/dev/null || echo local)"
nome="biblioteca-$sha"
mkdir -p dist
cp target/biblioteca.jar "dist/$nome.jar"
(cd dist && sha256sum "$nome.jar" > "$nome.jar.sha256" && cat "$nome.jar.sha256")

if [ "${1:-}" = "--docker" ]; then
  etapa "3. Imagem Docker e smoke test"
  docker build --build-arg JAR_FILE="dist/$nome.jar" -t "biblioteca:sha-$sha" .
  docker rm -f biblioteca-smoke >/dev/null 2>&1 || true
  docker run -d --name biblioteca-smoke -p 8080:8080 \
    -e JWT_SECRET="$(openssl rand -base64 48)" "biblioteca:sha-$sha" >/dev/null
  trap 'docker rm -f biblioteca-smoke >/dev/null 2>&1 || true' EXIT
  for i in $(seq 1 60); do
    curl -fsS -o /dev/null http://localhost:8080/login && break
    [ "$i" -eq 60 ] && { docker logs biblioteca-smoke; echo "ERRO: sem resposta em 60s"; exit 1; }
    sleep 1
  done
  echo "GET /login      -> $(curl -s -o /dev/null -w '%{http_code}' http://localhost:8080/login) (esperado 200)"
  echo "GET /api/livros -> $(curl -s -o /dev/null -w '%{http_code}' http://localhost:8080/api/livros) (esperado 401)"
fi

etapa "Concluido: dist/$nome.jar"

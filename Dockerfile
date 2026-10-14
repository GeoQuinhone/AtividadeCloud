# Imagem de execucao da Biblioteca.
#
# Este Dockerfile NAO compila o codigo: ele empacota o JAR que o pipeline ja
# construiu e testou ("construir uma vez, promover o mesmo artefato").
#
#   No pipeline : docker build --build-arg JAR_FILE=dist/biblioteca-<sha>.jar -t biblioteca:sha-<sha> .
#   Localmente  : ./mvnw clean verify && docker build -t biblioteca:local .
#
# Para executar e obrigatorio informar o segredo do JWT:
#   docker run -p 8080:8080 -e JWT_SECRET="$(openssl rand -base64 48)" biblioteca:local

FROM eclipse-temurin:21-jre-alpine

ARG JAR_FILE=target/biblioteca.jar

# Usuario sem privilegios de root dentro do container
RUN addgroup -S app && adduser -S app -G app
WORKDIR /app

COPY --chown=app:app ${JAR_FILE} /app/app.jar

USER app
EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]

FROM maven:3.10.0-eclipse-temurin-21-alpine AS build
WORKDIR /workspace
COPY pom.xml ./
COPY shared/common/ shared/common/
COPY services/ services/
RUN --mount=type=cache,target=/root/.m2,sharing=locked \
    mvn -B -ntp -pl services/config-server -am -DskipTests package \
    && cp services/config-server/target/config-server-*.jar /application.jar

FROM eclipse-temurin:21-jre-alpine AS runtime
RUN apk add --no-cache curl \
    && addgroup -S -g 10001 app \
    && adduser -S -D -H -u 10001 -G app app
WORKDIR /app
COPY --from=build --chown=app:app /application.jar application.jar
ENV SERVER_PORT=8888 \
    MANAGEMENT_ENDPOINT_HEALTH_PROBES_ENABLED=true \
    MANAGEMENT_ENDPOINTS_WEB_EXPOSURE_INCLUDE=health \
    JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=70.0"
USER app
EXPOSE 8888
HEALTHCHECK --interval=10s --timeout=5s --start-period=60s --retries=12 \
    CMD curl --fail --silent --show-error "http://127.0.0.1:${SERVER_PORT}/actuator/health/readiness" || exit 1
ENTRYPOINT ["java", "-jar", "/app/application.jar"]

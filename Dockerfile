FROM ghcr.io/graalvm/native-image-community:25 AS build
WORKDIR /workspace
COPY .mvn/ .mvn/
COPY mvnw pom.xml checkstyle.xml ./
RUN chmod +x mvnw && ./mvnw -B -q dependency:go-offline
COPY src/ src/
RUN ./mvnw -B -Pnative -DskipTests -Dcheckstyle.skip native:compile

FROM debian:12-slim
RUN useradd --system --uid 10001 --no-create-home app
WORKDIR /app
COPY --from=build /workspace/target/user-service /app/user-service
ENV PORT=8080
EXPOSE 8080
USER app
ENTRYPOINT ["/app/user-service"]

FROM ghcr.io/graalvm/native-image-community:25 AS build
WORKDIR /workspace
COPY .mvn/ .mvn/
COPY mvnw pom.xml checkstyle.xml ./
RUN chmod +x mvnw && ./mvnw -B -q dependency:go-offline
COPY src/ src/
RUN ./mvnw -B -Pnative -DskipTests -Dcheckstyle.skip native:compile

FROM gcr.io/distroless/base-debian12:nonroot
WORKDIR /app
COPY --from=build /workspace/target/user-service /app/user-service
ENV PORT=8080
EXPOSE 8080
USER nonroot
ENTRYPOINT ["/app/user-service"]

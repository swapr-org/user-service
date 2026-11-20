# user-service

The swapr service that owns user profiles, location, taxonomy and interests. It is also the reference service: every other swapr service starts as a copy of this repo, so it carries the build, lint, configuration, authentication and delivery pipeline that all services share.

Stack: Java 25, Spring Boot 4, Maven, Flyway, Checkstyle (Google rules, 120 columns), deployed to Render as a GraalVM native image.

## Running locally

Prerequisites: Docker and JDK 25.

```sh
./mvnw spring-boot:run
```

With no profile set, the `local` profile is active. Spring Boot's Docker Compose support starts the PostGIS database from `docker-compose.yml` and wires the connection in. Local auth points at the QA Supabase project, so sign in through the QA project to get a JWT. Any environment variable from the table below overrides the `local` defaults.

Run the linter and unit tests with:

```sh
./mvnw verify
```

The unit tests start a PostGIS container through Testcontainers, so Docker must be running.

## Environment variables

| Variable | Purpose | Default |
|---|---|---|
| `SPRING_PROFILES_ACTIVE` | `local`, `qa` or `prod`. Render sets it per service. | `local` |
| `PORT` | HTTP port. Render sets it. | `8080` |
| `DATABASE_URL` | JDBC URL, for example `jdbc:postgresql://<host>/<db>?sslmode=require`. Neon needs `sslmode=require`. | none (local uses Docker Compose) |
| `DATABASE_USERNAME` | Database user. | none |
| `DATABASE_PASSWORD` | Database password. Secret. | none |
| `SUPABASE_URL` | Supabase project URL, for example `https://<ref>.supabase.co`. The JWT issuer and JWKS address are built from it. | QA project in `local` |
| `INTERNAL_TOKEN` | Shared service-to-service secret for `/internal/**`. Secret. Startup fails when it is empty. | `local-internal-token` in `local` |
| `CORS_ALLOWED_ORIGINS` | Comma-separated origins allowed to call `/v1/**` from a browser. | `http://localhost:5173` in `local` |

YAML holds defaults only. Secrets are set in Render and never committed.

## Endpoints and security

- `/actuator/health` reports overall health including the `db` component. Use it to confirm a deployed service can reach Neon.
- `/actuator/health/liveness` ignores the database. Render's health check uses it, so a slow Neon wake-up does not fail deploys or restart the instance.
- `/v3/api-docs` serves the OpenAPI document without auth.
- `/v1/**` needs a Supabase JWT: issuer `<SUPABASE_URL>/auth/v1`, audience `authenticated`, signed by a key from the project's JWKS. The `sub` claim is the user's UUID and the principal name.
- `/internal/**` needs the `X-Internal-Token` header and rejects any request carrying an `Authorization` header.
- Every other path is denied. Authentication and access errors are RFC 9457 Problem Details.

## Pipeline

`.github/workflows/pipeline.yml` has four jobs:

1. `verify` runs `./mvnw verify` (Checkstyle and unit tests) on every push to any branch and on pull requests. Any Checkstyle warning or failing test fails the run.
2. `image` runs on pushes to `main` only. It builds the native image from the `Dockerfile` and pushes `ghcr.io/<org>/user-service:<commit-sha>`. There is no `latest` tag.
3. `deploy-qa` calls the Render deploy hook in the GitHub Environment `qa` with that exact image.
4. `deploy-prod` waits for approval on the GitHub Environment `prod`, then calls the prod deploy hook with the same image. It never rebuilds, so prod runs only what QA ran.

Deploy jobs share a concurrency group per environment, so two runs never deploy at once and older pending prod approvals are cancelled. After a deploy, check `/actuator/health` by hand and confirm `db` is `UP`.

Build the native image locally with `docker build -t user-service .`, or without Docker using `./mvnw -Pnative native:compile` on a GraalVM JDK 25.

## Copy this service

Follow these steps to start a new service, for example `listing-service`, from this repo.

1. **Copy the files** into the new repo, leaving out `target/`, `worktrees/` and `.git/`.
2. **Rename** every `user-service` reference:
   - `artifactId`, `name` and `description` in `pom.xml`;
   - the base package `app.swapr.user` and `UserServiceApplication` (for example `app.swapr.listing` and `ListingServiceApplication`), including the test classes;
   - `spring.application.name` in `application.yaml`;
   - the database name and user in `docker-compose.yml`;
   - the native executable path in the `Dockerfile` (`target/<artifactId>`);
   - `SERVICE_NAME` in `.github/workflows/pipeline.yml`, which names the image `ghcr.io/<org>/<service>`;
   - the title and description in this README.
3. **Create the Neon projects** `<service>-qa` and `<service>-prod`, in the shared region. A service without a database (such as `feed-service`) drops the JPA, Flyway, Postgres, Docker Compose and Testcontainers pieces instead.
4. **Create the Render services** `<service>-qa` and `<service>-prod`:
   - deploy from an existing image, `ghcr.io/<org>/<service>:<sha>`;
   - set the health check path to `/actuator/health/liveness`;
   - set the environment variables `SPRING_PROFILES_ACTIVE` (`qa` or `prod`), `DATABASE_URL`, `DATABASE_USERNAME`, `DATABASE_PASSWORD`, `SUPABASE_URL`, `INTERNAL_TOKEN` and `CORS_ALLOWED_ORIGINS`. Render sets `PORT`.
5. **Add the deploy hooks** to GitHub: copy each Render service's deploy hook URL into the secret `RENDER_DEPLOY_HOOK_URL` of the GitHub Environment `qa` or `prod`. The `prod` Environment must have required reviewers, which only works while the repo is public.
6. **Make the GHCR package public** after the first `image` job pushes it (GitHub org, Packages, the package, Package settings, Change visibility). Render pulls without credentials, so the first QA deploy may fail until this is done; re-run the `deploy-qa` job afterwards.
7. **Remove anything `user-service`-specific**: its migrations under `src/main/resources/db/migration`, its domain code and tests, and any README sections that describe its domain. Keep the security package, the health configuration and the context test.

# user-service

The swapr service that owns user profiles, location, taxonomy and interests. It is also the reference service: every other swapr service starts as a copy of this repo, so it carries the build, lint, configuration, authentication and delivery pipeline that all services share.

Stack: Java 25, Spring Boot 4, Maven, Flyway, Checkstyle (Google rules, 120 columns), deployed to Render as a GraalVM native image.

## Running locally

Prerequisites: Docker and JDK 25.

Once, copy `.env.example` to `.env` and fill in the QA Supabase project URL and any non-empty local internal token. `.env` is gitignored.

```sh
./mvnw spring-boot:run
```

With no profile set, the `local` profile is active. It reads `.env` from the repo root, and Spring Boot's Docker Compose support starts the PostGIS database from `docker-compose.yml` and wires the connection in. Local auth points at the QA Supabase project, so sign in through the QA project to get a JWT. Real environment variables override the values in `.env`.

Run the linter and unit tests with:

```sh
./mvnw verify
```

The unit tests start a PostGIS container through Testcontainers, so Docker must be running.

## Environment variables

| Variable | Purpose | Default |
|---|---|---|
| `SPRING_PROFILES_ACTIVE` | `local`, `qa` or `prod`. Render sets it per service. | `local` |
| `PORT` | HTTP port. Render sets it. Local runs listen on 8081. | `8081` |
| `DATABASE_URL` | JDBC URL, for example `jdbc:postgresql://<host>/<db>?sslmode=require`. Neon needs `sslmode=require`. | none (local uses Docker Compose) |
| `DATABASE_USERNAME` | Database user. | none |
| `DATABASE_PASSWORD` | Database password. Secret. | none |
| `SUPABASE_URL` | Supabase project URL, for example `https://<ref>.supabase.co`. The JWT issuer and JWKS address are built from it. | none (`.env` locally) |
| `INTERNAL_TOKEN` | Shared service-to-service secret for `/internal/**`. Secret. Startup fails when it is empty. | none (`.env` locally) |
| `CORS_ALLOWED_ORIGINS` | Comma-separated origins allowed to call `/v1/**` from a browser. | none (`.env` locally) |

YAML holds defaults only and reads every per-environment value from an environment variable, including for local runs. Locally those come from `.env`; in QA and prod they are set in Render. Secrets are never committed.

## Endpoints and security

- `/actuator/health` reports overall health including the `db` component. Use it to confirm a deployed service can reach Neon.
- `/actuator/health/liveness` ignores the database. Render's health check uses it, so a slow Neon wake-up does not fail deploys or restart the instance.
- `/v3/api-docs` serves the OpenAPI document without auth.
- `/v1/**` needs a Supabase JWT: issuer `<SUPABASE_URL>/auth/v1`, audience `authenticated`, signed by a key from the project's JWKS. The `sub` claim is the user's UUID and the principal name.
- `/internal/**` needs the `X-Internal-Token` header. A missing or wrong token gets 401. A request with the right token that also carries an `Authorization` header gets 403, so user JWTs never reach internal endpoints.
- Every other path is denied. Authentication and access errors are RFC 9457 Problem Details.

User-service endpoints (all errors are Problem Details; validation errors add an `errors` array of `{field, message}`):

- `GET /v1/users/me` returns the caller's own profile `{id, displayName, radiusKm, city}`, creating it on first use with radius 25 km and no name or city.
- `PUT /v1/users/me` with `{displayName, cityId, radiusKm}` replaces the caller's profile. The name is trimmed to 1–50 characters, `cityId` must be an existing city, and the radius is a whole number from 1 to 100.
- `GET /v1/users/{id}` returns a public profile `{id, displayName, city: {name, region, countryCode}}`, or 404 until the user has set a name and city.
- `GET /v1/cities?query=<text>` returns up to 10 cities `{id, name, region, countryCode}` whose name starts with the query, ignoring case and accents, largest first. The query must be at least 2 characters.
- `DELETE /internal/v1/test-data/users/{id}` exists only in the `qa` profile. It deletes everything user-service stores for the user and always returns 204. The integration suite calls it to clean up.

## City data

Locations are cities from the GeoNames gazetteer, loaded by the Flyway migration `V1__create_cities.sql`. It was generated once from `cities15000.txt` (cities with at least 15,000 people) and `admin1CodesASCII.txt` (region names), downloaded on 2026-11-23. Each city's ID is a UUID v5 of its GeoNames URI (`https://sws.geonames.org/<geonameid>/`, URL namespace), so IDs are the same in every environment. `folded_name` is the name lowercased with combining marks removed after Unicode NFD, so `zur` finds Zürich. A profile stores only a city, so its location is that city's centre point.

City data from GeoNames (geonames.org), licensed under CC BY 4.0.

## Pipeline

`.github/workflows/pipeline.yml` has five jobs:

1. `verify` runs `./mvnw verify` (Checkstyle and unit tests) on every push to any branch and on pull requests. Any Checkstyle warning or failing test fails the run.
2. `image` runs on pushes to `main` only. It builds the native image from the `Dockerfile` and pushes `ghcr.io/<org>/user-service:<commit-sha>`. There is no `latest` tag.
3. `deploy-qa` deploys that exact image to QA through the Render deploy hook in the GitHub Environment `qa`, and waits until the deploy is live.
4. `e2e` runs the whole integration test suite against QA by calling `integration-test-suite`'s reusable workflow. A failing suite skips `deploy-prod`, so no prod approval is offered.
5. `deploy-prod` waits for approval on the GitHub Environment `prod`, then deploys the same image to prod and waits until it is live. It never rebuilds, so prod runs only what passed in QA.

Both deploy jobs run `.github/scripts/render-deploy.sh`. It triggers the deploy hook, reads the deploy ID from the response, sleeps 5 minutes, then polls the Render API every 15 seconds until the deploy is `live`. A failed, cancelled or replaced deploy, an error response, or no live deploy 15 minutes after the trigger fails the job with an `::error::` annotation naming the environment, the deploy ID and the last status. Render 5xx responses are retried until the limit.

The workflow has one concurrency group per branch, so runs on `main` go one at a time from QA deploy through prod deploy, and a later merge can never replace the QA build while an earlier one is being tested. A run waiting for prod approval holds the group, so approve or reject it to let the next merge deploy. Only the newest queued run is kept. After a deploy, check `/actuator/health` by hand and confirm `db` is `UP`.

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
5. **Add the deploy settings** to GitHub, in the GitHub Environment `qa` or `prod`:
   - the secret `RENDER_DEPLOY_HOOK_URL`: the Render service's deploy hook URL;
   - the variable `RENDER_SERVICE_ID`: the Render service's `srv-…` ID.

   The org-level secret `RENDER_API_KEY` (a read-only Render key) already covers every service. The `prod` Environment must have required reviewers, which only works while the repo is public.
6. **Make the GHCR package public** after the first `image` job pushes it (GitHub org, Packages, the package, Package settings, Change visibility). Render pulls without credentials, so the first QA deploy may fail until this is done; re-run the `deploy-qa` job afterwards.
7. **Remove anything `user-service`-specific**: its migrations under `src/main/resources/db/migration`, its domain code and tests, the user-service-specific deletes in the test-data controller, and any README sections that describe its domain, including the city data section. Keep the security package, the health configuration and the context test.

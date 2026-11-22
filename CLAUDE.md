# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Status

`backend/` and `web/` are scaffolded (Tasks 1 and 3 of `tasks/todo.md`). Progress is tracked in `tasks/todo.md`.

Commands (run in `backend/`, Docker must be running for Testcontainers):

```
docker compose -f ../compose.yaml up -d postgres redis     # local Postgres 17 + Redis 7
./mvnw verify                                              # all tests + module boundary check + Spotless check
./mvnw test -Dtest=ModularityTest                          # module boundaries only, no Docker
./mvnw spotless:apply                                      # format
./mvnw -q -DskipTests package && java -jar target/plandi-0.0.1-SNAPSHOT.jar --spring.profiles.active=local
```

Web commands (run in `web/`; the dev server proxies `/api` to `localhost:8080`):

```
npm ci
npm run dev                                                # Vite dev server
npm run lint && npm test && npm run build                  # oxlint, Vitest, tsc + Vite build (PWA)
```

Staging (home machine, Cloudflare Tunnel → https://plandi-staging.evts.id.vn, setup in `docs/staging.md`):

```
./deploy.sh                                                # git pull → build → docker compose -f compose.prod.yml up -d
docker compose -f compose.prod.yml logs -f app cloudflared
```

The project path contains Vietnamese characters, which breaks two tools on Windows: Maven 3.10.x (the wrapper is pinned to 3.9.x) and `spring-boot:run` (run the jar instead).

Module boundaries are enforced: each module's `package-info.java` lists `allowedDependencies`, and `ModularityTest` fails on any other dependency or on use of another module's `internal` package.

The source of truth for scope and decisions is the research doc: https://claude.ai/artifact/GpXGUfRAYgNu3gKYXwHpNn (Claude Docs, in Vietnamese). Read it before making product or architecture decisions. The owner writes in Vietnamese, so reply in Vietnamese.

Specs live in `spec/`: `capability-map.md` (modules, dependency order), `SPEC.md` (shared stack, commands, style, testing, boundaries) and `SPEC-<module>.md` per module. Code follows the approved spec; change the spec first.

## Skills by phase

Skills are user-level (`~/.claude/skills`, plugins); invoke them at these points without being asked:

- Spec or a new module: `spec-driven-development`, then `planning-and-task-breakdown` (plan in `tasks/plan.md`, tasks in `tasks/todo.md`)
- Architecture decisions: `documentation-and-adrs` (ADRs in `docs/adr/`); module contracts: `api-and-interface-design`
- Writing code: `incremental-implementation` + `test-driven-development`; commits and branches: `git-workflow-and-versioning`
- Auth, consent, export/delete, encryption, invite links: `security-and-hardening`
- Before deploy: `ci-cd-and-automation`, `observability-and-instrumentation`, `shipping-and-launch`
- `web/` UI: design each screen in Figma first (Figma MCP; PLANDI folder in the owner's education team), show the owner, then build it with `frontend-ui-engineering`, `accessibility-engineering`

Where `superpowers:*` overlaps one of these (TDD, brainstorming), use the skill listed here.

## Commits

A task in `tasks/todo.md` has several parts. Commit each part as soon as it is done and verified. Don't wait until the whole task is finished. For each part:

1. Finish one working part (for example: the migration and entity, then one endpoint, then the web screen).
2. Run its tests, usually `./mvnw verify` in `backend/`. Add a manual check where tests can't reach. Never commit a part that fails or doesn't build.
3. Commit only that part. The message is in Vietnamese with a conventional prefix, e.g. `feat(account): đăng ký bằng email`.
4. Then start the next part.

Work for task N happens on a branch named `task-N-<slug>`. Local commits need no confirmation. Merging into `main` and pushing need the owner's go-ahead.

## What Plandi is

Plandi ("plan" + "đi") is a web app for one city, TP Trà Vinh. A user sets a monthly budget per sub-category, and the app suggests 2–3 weekend plans (venues, movies at CGV Vincom Trà Vinh) that fit the remaining budget. The app has two modes:

- **Cá nhân** (personal)
- **Cặp đôi** (couple; the partner joins via an expiring invite link)

There are two goals: real users, and a backend portfolio piece for an intern or fresher Java role.

## Planned stack and layout

- One repo containing `backend/` (Java 21, Spring Boot 4, PostgreSQL + Flyway, Redis, Spring Scheduler) and `web/` (React, responsive web/PWA).
- The backend is a modular monolith, not microservices. Planned modules: account, fund + invites, budget/expenses, catalog (venues, movies, admin), recommendation, privacy (consent, export, delete).
- Deploy with Docker Compose and GitHub Actions to a VPS with HTTPS.
- Tests use JUnit 5 and Testcontainers.
- No LLM in the MVP. Spring AI is deferred.

## Cross-cutting rules (these are why the design looks the way it does)

- **Fund model:** a personal fund is just a fund with one member. Each account has exactly one personal fund and joins at most one couple fund. Don't build two separate code paths for the two modes.
- **Couple privacy:** personal-fund spending is never visible to the partner, and couple-mode recommendations may only use couple-fund data. Every query filters by the fund the caller belongs to. Cross-fund access needs its own tests.
- **Money:** store as `BIGINT` VND, since VND has no minor unit.
- **External sources live behind adapters** (Google Places, TMDB). Recommendations must still work from our own catalog when an adapter is off.
- **Google Places policy:** only store `place_id`. Lat/lng may be cached for 30 days at most. Name, rating, hours, photos and reviews are fetched at display time and never stored or pre-indexed. Show "Google Maps" attribution wherever this data appears. `rating`, `userRatingCount`, `priceLevel` and opening hours are Enterprise SKU: 1,000 free calls a month, then $20 per 1,000. Use Redis to count calls.
- **Recommendation pipeline:**
  1. Filter our own catalog (budget via price tier, tags, food filters) down to about 10 candidates.
  2. Fetch Google rating, rating count and opening hours for those candidates in one call each, and drop the ones that are closed at the planned time. If Google is off or over quota, skip the hours filter and warn the user.
  3. Re-rank using the Bayesian average `v/(v+m)·R + m/(v+m)·C`, plus tag, preference and novelty scores.
  4. Combine 2–3 items so the total stays within budget.
  5. Return each plan with a human-readable reason.

  The ranking is a plain formula, not ML.
- **TMDB:** free only for non-commercial use, with attribution in About or Credits. Commercial use needs a written agreement, and TMDB data must not be used to train AI. A weekly scheduled job syncs movies. Cinema showtimes are entered by hand through the admin page, because no Vietnamese cinema chain has a public showtime API.
- **Data the app must not collect** (Decree 356/2025, Article 4, sensitive data):
  - no GPS location on the server; the user picks an area, or the phone picks the nearest area itself and sends only its code (ADR-0006)
  - no health data; food filters are optional tags with no reason asked
  - no reading of user messages
  - no scraping of TikTok, Facebook or cinema sites

  TikTok links are embedded via the official embed/oEmbed only. Treat spending logs and in-app behaviour as sensitive. That means per-purpose consent, real export and delete (including logs), encryption of sensitive columns, and logs that contain no amounts or user notes.

## Out of scope for the MVP

Holding or moving money, ticket booking, native apps or widgets, cities other than Trà Vinh, and the roommates/friends/family templates. A roommates fund reconciled through VietQR + SePay webhooks is planned for phase 2.

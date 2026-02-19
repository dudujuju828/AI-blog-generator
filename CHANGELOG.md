# Changelog

All notable changes to this project are documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/).

## [0.2.0] - 2026-02-19

### Added

- Iterative review and revise loop to improve blog post quality — after the initial draft, a critic prompt identifies weaknesses in structure, depth, clarity, and engagement, then the post is rewritten to address the feedback
- Optional `iterations` field on the `POST /api/generate` request body (defaults to 1 for backwards compatibility); each additional iteration adds one review/revise cycle
- `chatCompletion` helper in `LlmService` to reduce duplication across generate, review, and revise calls

## [0.1.0] - 2026-02-19

### Added

- Spring Boot 3.4.3 project (Java 24) with Maven wrapper for building and running the application
- `POST /api/generate` REST endpoint that accepts a `topic` string and returns the generated post's `slug`, `title`, `commitUrl`, and a status `message` using Java record DTOs
- OpenAI integration (`LlmService`) that sends a system-prompted chat completion request to generate a structured blog post (title, description, tags, and markdown content) and derives a URL-safe slug from the title
- GitHub integration (`GitHubService`) that formats the generated post as an MDX file with frontmatter (title, date, description, tags), Base64-encodes it, and commits it to the portfolio repository via the GitHub Contents API
- `BlogGeneratorService` orchestrator that ties the full pipeline together — LLM generation followed by GitHub commit — and is injected into the controller
- Global exception handler (`@RestControllerAdvice`) that returns structured JSON errors: 502 for external API failures (`RestClientException`) and 500 for internal runtime errors
- Project README documenting setup, configuration, and API usage
- `generate-post.cmd` Windows batch script that starts the Spring Boot server, waits for it to become healthy, sends a generate request via `curl`, and tears the server down afterward

### Fixed

- `RestClient` URI template encoding issue where path variables containing forward slashes (e.g. `src/content/blog/{slug}.mdx`) were percent-encoded to `%2F`, causing 404s from the GitHub API — resolved by switching to string concatenation for the request URI

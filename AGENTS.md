# Repository Guidelines

## Project Structure & Modules
- Feature modules (Spring Modulith): `transfer` (REST + Spring Batch job launcher), `metadata` (EXIF/hash extraction, catalog jobs, sidecars), `ai` (AI enrichment listeners + REST). Keep module boundaries strict; prefer events over direct cross-module calls.
- Config: `src/main/resources/application.yaml`; static assets under `resources/static`, templates under `resources/templates`; H2 metadata DB lives under `.h2/`.
- Tests mirror modules in `src/test/java`; fixtures under `src/test/resources`. Build output is in `target/`.

## Build, Test, and Development Commands
- `./mvnw clean verify` — full build with unit/integration tests.
- `./mvnw spring-boot:run` — start the API locally on port 8080 (override with `--server.port=9090`).
- `./mvnw -DskipTests package` — produce `target/filetrans-0.0.1-SNAPSHOT.jar` for deployment.
- `java -jar target/filetrans-0.0.1-SNAPSHOT.jar --server.port=9090` — run the packaged app.

## Coding Style & Naming
- Java 25, 4-space indent, no trailing whitespace; favor Lombok for boilerplate where already used.
- Package-by-feature; class names reflect roles (e.g., `*Controller`, `*Service`, `*JobConfig`, `*ItemReader`).
- Method and variable names stay imperative and descriptive; constants are upper snake case.
- Favor constructor injection (Lombok `@RequiredArgsConstructor`); log with `@Slf4j` and structured messages.

## Testing Guidelines
- Primary stack: JUnit 5 + Spring Boot Test + Spring Batch Test; Modulith tests live in `ModulithStructureTests`.
- Unit tests follow `*Test`; end-to-end and integration specs use `*E2ETest`/`*IntegrationTest`.
- Run `./mvnw test` before commits; prefer isolated unit coverage for parsing/filtering logic (e.g., `Extensions`, `FileItemReader`) and integration coverage for job wiring and REST endpoints.

## Commit & Pull Request Expectations
- Commit messages: present-tense, imperative summaries (`Add restart index guard`, `Refine EXIF extraction`). Include scope when helpful (`ai:`, `metadata:`).
- Pull requests should link issues/tickets, describe behavior changes, list test commands run, and include API samples for REST changes. Attach screenshots or logs when altering job output or API responses.
- Keep diffs small and modular; update or add tests alongside feature or bugfix changes.

## Architecture & Batch Notes
- Spring Batch 6: jobs are immutable; prefer constructor-based beans and JobParameters as records. Use `@EnableBatchProcessing` + store-specific config (JDBC) rather than deprecated modular mode (see Springbatchmigration.md).
- Transfer job: reader scans source tree with extension filtering and restart index; processor computes `targetBaseDir/YYYY/YYYY-MM-dd`; writer moves files (chunk size 50).
- Metadata pipeline: sidecar writer produces `<file>.metadata.json` and emits path events; catalog job aggregates sidecars.
- AI module: listens to metadata completion events and enriches sidecars via `ExternalAiClient`; defaults to a no-op client if none configured. Keep `ai` decoupled from `metadata` by using events, not direct service calls.

## Configuration & Safety
- Default config initializes Spring Batch and H2 schemas; override `spring.datasource.url` when running outside the repo root.
- File operations need read/write permissions on source/target roots; test on staging paths before pointing at real media libraries.

AI Analysis Module — Design Document
1. Purpose

The AI Analysis module processes JPG images after they have been moved by the Transfer module.
Its responsibilities are:

Receive notification when a file has been transferred.

If the file is a JPG, run an AI analysis step.

Emit an event containing the analysis results.

The Metadata module listens for this event and updates the sidecar metadata file.

At this stage the module uses dummy AI output (no real model). The structure, events, and dependencies are designed so a real AI client can be plugged in later with no architectural changes.

2. High-Level Workflow
   Step-by-step

A JPG file is moved

The Transfer module emits a FileTransferredEvent.

AI module receives the event

AI module filters non-JPG files.

Relevant JPG files are passed to its internal "AI Analysis Service".

AI module performs dummy AI analysis

Generates placeholder metadata such as a short description, tags, and a confidence score.

AI module emits an AiAnalysisCompletedEvent

Contains the target path and the AI-generated metadata values.

Metadata module receives the completed AI event

Locates or creates the appropriate sidecar JSON metadata file.

Inserts or updates the AI-related values.

Persists the file.

3. Module Boundaries
   AI module inputs

Event: "FileTransferredEvent" from Transfer module

Only uses the file path.

AI module outputs

Event: "AiAnalysisCompletedEvent"

Contains AI metadata required by Metadata module.

Internal-only components

AI Analysis service

Dummy AI client (to be replaced with Spring AI)

Event listener for incoming Transfer events

External modules interacting with AI

Transfer module: sends the initial file-moved event

Metadata module: consumes the AI-completed event

4. Responsibilities of the AI Module
   4.1. Event Listener

Subscribes to file transferred events.

Ensures input is relevant (JPG or JPEG extension).

Delegates analysis to internal service.

4.2. AI Analysis Service

Acts as the orchestrator of the module.

Calls an AI client (dummy or real).

Packages results into a finished event.

Publishes the finished event.

4.3. AI Client (Dummy Implementation)

Simulates AI output.

Returns placeholder structured metadata:

A short textual description.

A small list of tags.

A numeric confidence.

Later this component is replaced by a Spring AI / Ollama client that takes an image and returns genuine model output.

4.4. Outgoing Completed Event

Represents the result of the AI step.

Includes:

The image file path

The AI metadata object (description, tags, score)

This event is consumed by the metadata module.

5. Interaction With Other Modules
   5.1. Transfer → AI

Trigger: FileTransferredEvent

AI validates the file type.

AI runs analysis.

5.2. AI → Metadata

Trigger: AiAnalysisCompletedEvent

Metadata module:

Locates sidecar JSON next to the file.

Merges new AI fields into existing metadata.

Writes the updated file to disk.

5.3. No direct circular dependencies

AI module depends on Transfer module only through events.

Metadata module depends on AI module only for event types.

Both relationships use event-driven decoupling, following Modulith best practices.

6. Data Contracts

Data exchanged between modules should be stable and versioned.
Two contracts exist:

6.1. Input Contract: FileTransferredEvent

Contains:

Destination path of the moved file

Module assumption:

The file exists at the given path

Transfer module has ensured file operations already completed

6.2. Output Contract: AiAnalysisCompletedEvent

Contains:

Image path

AI metadata object with:

Short description (string)

List of tags

Confidence score (double)

These fields should be small and descriptive, not model-specific.

7. Failure Scenarios and Handling
   7.1. Non-image files

JPG-only processing. Other file types are ignored.

7.2. AI analysis failure (in future real model)

For dummy implementation:

Always succeeds with dummy data.

For real implementation:

Log the failure

Do not publish AiAnalysisCompletedEvent

Optionally publish a failure event (future extension)

7.3. Metadata module failures

AI module remains unaware of downstream failures.

Responsibility stays with the metadata module.

8. Extensibility
   Easily add

Spring AI Ollama client

Async execution with TaskExecutor

Batch processing of bulk directories

Embedding-based tagging

Object detection and bounding box extraction

Prompt-based description generation

Minimal required changes

Swap dummy AI client for a real one

Add configuration under application.yaml

Add a controller to trigger manual re-analysis

The rest of the architecture remains stable.

9. Modulith Considerations
   AI module boundaries

Define an explicit module for AI:

Exported types:

AI metadata structure

AI completed event type

Hidden types:

Event listeners

AI analysis service

AI client implementation

Dependencies

Allowed:

Transfer (for events)

Spring Core, Events

Disallowed:

Direct access to Metadata classes

Direct access to filesystem operations outside analysis

This ensures clean separability and clear ownership of each domain.

10. Testing Strategy
    10.1. AI Module Unit Tests

Dummy AI client always produces predictable dummy output.

AI analysis service:

For valid JPG paths → should publish completed event.

For non-JPG → should do nothing.

10.2. Integration Test (AI → Metadata)

Publish a simulated file transferred event.

Assert that:

Metadata module wrote a sidecar.

AI values were included.

10.3. Modulith Structure Test

Confirm:

AI module depends on Transfer

Metadata module depends on AI API

No accidental circular dependencies.

11. Summary

The AI module introduces an event-driven analysis step that cleanly integrates into your existing file-transfer and metadata pipeline. It keeps responsibilities well-separated:

Transfer moves files

AI interprets JPG images

Metadata persists information

The architecture is stable, testable, modular, and ready for real model integration later.

If you want, I can also produce:

✅ An architecture diagram (ASCII or SVG)
✅ A sequence diagram for the event flow
✅ A README for the ai module
✅ A Modulith module description file

Just tell me what you want next.
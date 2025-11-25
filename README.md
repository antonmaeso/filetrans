# FileTrans

Automated Spring Boot/Spring Batch service to move files from cameras or staging areas into an organized destination folder tree. Files are grouped by capture date (`yyyy/yyyy-MM-dd`) and copied using chunk-based batch jobs, so the service can be triggered through a simple REST API while benefiting from restartable, observable batch processing.

## Table of Contents
1. [Architecture](#architecture)
2. [Prerequisites](#prerequisites)
3. [Building and Running](#building-and-running)
4. [Configuration](#configuration)
5. [REST API](#rest-api)
6. [Batch Job Mechanics](#batch-job-mechanics)
7. [Filtering by Extension](#filtering-by-extension)
8. [Logging & Monitoring](#logging--monitoring)
9. [Development Tips](#development-tips)

---

## Architecture

```
+---------------------------+        +----------------------------+
| FileTransferController    |        |  FileTransferService       |
| POST /transfers           | -----> |  Builds JobParameters and  |
| GET  /transfers/{id}      |        |  launches Spring Batch Job |
+---------------------------+        +----------------------------+
                                                |
                                                v
         +-----------------------------------------------+
         | Spring Batch Job: photoImportJob              |
         |  • ItemStreamReader<FileDescriptor>           |
         |      - Scans source tree (recursive)          |
         |      - Applies extension filters              |
         |      - Persists iterator index in             |
         |        ExecutionContext for restartability    |
         |  • Processor -> determines target path        |
         |  • Writer -> moves file, creates directories  |
         +-----------------------------------------------+
```

- **Extensions**: `Extensions` record normalizes/holds allowed extensions per job.
- **Idempotency / Restartability**: Reader persists the next index in the `ExecutionContext`, so a crashed job restarts at the last unprocessed file.
- **Observability**: `@Slf4j` logging throughout controller, service, readers, listeners, and writers shows every lifecycle event.

## Prerequisites

- Java 25 (configured via `pom.xml`).
- Maven Wrapper (`./mvnw`) – no system Maven required.
- Access to the directories you wish to move from/to.

## Building and Running

```bash
# compile
./mvnw -DskipTests compile

# run (default port 8080)
./mvnw spring-boot:run

# or build a runnable jar
./mvnw -DskipTests package
java -jar target/filetrans-0.0.1-SNAPSHOT.jar

# change port if needed
java -jar target/filetrans-0.0.1-SNAPSHOT.jar --server.port=9090
```

The application uses an embedded H2 database stored under `.h2/` to manage Spring Batch metadata (job/step executions, restart checkpoints).

## Configuration

`src/main/resources/application.yaml`:

```yaml
spring:
  batch.jdbc.initialize-schema: always
  batch.job.enabled: false          # jobs start via REST, not automatically
  datasource:
    url: jdbc:h2:file:./.h2/filetrans-db;DB_CLOSE_DELAY=-1;MODE=PostgreSQL;AUTO_SERVER=TRUE
    driver-class-name: org.h2.Driver

logging.level.com.ant.filetrans: INFO
```

Change the datasource URL if you prefer a different storage location. Spring Batch schema initialization is enabled by default for local use.

## REST API

### Create a transfer

```
POST /transfers
Content-Type: application/json
```

```json
{
  "sourceDir": "/Volumes/CARD/DCIM/101",
  "targetBaseDir": "/Users/me/Pictures/Imported",
  "extensions": ["jpg", "*.NEF"],        // optional, case-insensitive
  "filePath": "/Volumes/CARD/DCIM/101/IMG_0001.JPG" // optional - results in single-file transfer
}
```

Rules:
- `targetBaseDir` is required.
- Provide either `sourceDir` (directory mode) or `filePath` (single file mode).
- `extensions` is optional; omit or send an empty list to transfer all files.

Response: `202 Accepted` with body:

```json
{
  "executionId": 6,
  "status": "STARTING",
  "startTime": "2025-11-24T17:07:03.102Z",
  "endTime": null
}
```

`Location` header points to `/transfers/{executionId}`.

### Check job status

```
GET /transfers/{executionId}
```

Returns `404` if the job execution is unknown, otherwise the same `TransferResponse` as above. Use the `status` field (`STARTING`, `COMPLETED`, `FAILED`, etc.) to determine progress.

## Batch Job Mechanics

- **Job name**: `photoImportJob`.
- **Step**: `FileTransferStep` with chunk size 50.
- **Reader**: `FileItemReader` implements `ItemStreamReader<FileDescriptor>`. It:
  - Recursively scans the source directory at `open()`.
  - Filters by allowed extensions.
  - Stores the next index in the `ExecutionContext` (`filetrans.reader.index`).
  - Resumes from that index if restarted.
- **Processor**: Determines the destination path under `targetBaseDir/YYYY/YYYY-MM-dd`.
- **Writer**: Creates target directories as needed and moves files with `Files.move(..., REPLACE_EXISTING)`.

### Restart behavior
Because the reader saves its iterator index, interrupting a job (crash or manual stop) won’t re-copy processed files. Spring Batch automatically reloads the `ExecutionContext` and the reader jumps to the saved index.

## Filtering by Extension

- Extensions are normalized to lowercase without dots or wildcards (`*.JPG` → `jpg`, `.nef` → `nef`).
- Provide multiple entries to whitelist several extensions; ordering and duplicates are removed.
- If you omit `extensions`, every file under the source directory is eligible.
- Filtering occurs in the reader; jobs that specify an extension but have no matching files finish immediately with `read=0`.

## Logging & Monitoring

Key log points (default level `INFO`):

- REST controller: incoming request, accepted job ID.
- Service: job launch and stop events.
- Job listener: start/finish with status and parameters.
- Step listener: each step’s read/write/skip counts.
- Reader: number of files discovered, restart index.
- Writer: each chunk’s size and every move performed.

Increase detail by setting `logging.level.com.ant.filetrans=DEBUG` to see per-file reads and additional diagnostics.

## Development Tips

- **Testing**: Current project has no unit tests; recommended starting points are `Extensions.parse()` and `FileItemReader` (verify extension filtering and restart index behavior).
- **IDE output**: IntelliJ users should “Rebuild Project” whenever the Maven output gets out-of-sync (e.g., stray compiled classes causing duplicate beans).
- **Port conflicts**: When running multiple services locally, specify `--server.port` to avoid “connector startFailed” errors.
- **Directory permissions**: Ensure the runtime user can read from `sourceDir` and write to `targetBaseDir`; failures are logged and the job may skip unreadable files.

---

Need help extending this module (e.g., additional metadata storage, alternative storage backends, or asynchronous completion callbacks)? Open an issue or extend the code following the patterns documented above. Happy transferring!

# Report Delivery Reliability

A Java 21 and Spring Boot application that groups report-job attempts by logical run, identifies runs that never succeeded, and highlights candidates for a safe retry. It includes a small browser dashboard and JSON API backed by the provided seed data.

## Run in VS Code

1. Install JDK 21 and Apache Maven.
2. Open this folder in VS Code and install the recommended Java and Spring Boot extensions.
3. Use **Run Report Reliability** in Run and Debug, or run from a terminal:

   ```powershell
   mvn spring-boot:run
   ```

4. Open [http://localhost:8080](http://localhost:8080).

The seed data is loaded from `starter_kit/data/report_jobs.json` and packaged onto the application classpath by Maven.

## API

| Endpoint | Description |
|---|---|
| `GET /api/summary` | Counts by outcome |
| `GET /api/report-runs` | All grouped logical runs and their attempts |
| `GET /api/report-runs/never-succeeded` | Runs with no successful attempt |
| `GET /api/report-runs/{runId}` | One run, or `404` when absent |

For example, `GET /api/report-runs/never-succeeded` returns `R-005` as exhausted and unresolved, and `R-010` as eligible for one retry.

Run the test suite with:

```powershell
mvn test
```

## Retry policy and assumptions

- `completed` is the only success status.
- `failed`, `timed_out`, and `error` are retryable failure statuses.
- Any successful attempt makes the logical run successful, even if earlier attempts failed.
- A run with no success and a latest known failure is recommended for retry when its latest attempt number is below 3.
- Unknown statuses are never retried automatically; they are flagged for manual review.
- The dataset contains no executor integration, idempotency key, or failure details. Therefore, this version **recommends** retries but does not trigger real report jobs. Production auto-retry must additionally verify idempotency and retry transient errors only.
- The attempt limit assumes attempt numbers are accurate and sequential; production systems should enforce this at the scheduler/queue boundary.

## Seed data result

The dataset has 12 logical runs: 10 succeeded at least once, 1 remains unresolved after three failed attempts (`R-005`), and 1 has a retry recommendation (`R-010`). A run that previously failed but later completed (`R-003`) is correctly treated as succeeded.

## Limitations and next steps

- The sample dataset is loaded once at startup; no live scheduler, persistence, authentication, or tenant-level access control is included.
- There is no diagnostic message in the source data, so the reason shown describes the latest observed status rather than asserting a root cause.
- Retry classification is status-based. Production policy should separate transient and permanent failures, apply exponential backoff and jitter, and emit alerts for exhausted runs.
- For larger history, replace startup in-memory grouping with a database query and incremental processing; add metrics and durable retry state to avoid duplicate execution.

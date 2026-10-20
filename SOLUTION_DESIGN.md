# Solution Design: Report Delivery Reliability

## Problem

Meridian Payments needs to detect scheduled report runs that never produced a successful delivery, rather than discovering missing statements through customer complaints. The supplied history is attempt-oriented, so the implementation first groups attempts by logical `run_id`, then determines the final outcome and whether a bounded retry is appropriate.

## Assumptions

| Topic | Assumption | Rationale |
|---|---|---|
| Logical run | `run_id` uniquely identifies one scheduled report execution across attempts. | Attempt records with the same run ID are retries of one run, not separate statements. |
| Success | `completed` means a report run succeeded; any completed attempt makes the run successful. | A transient first failure followed by completion should not be reported as a missing delivery. |
| Failures | `failed`, `timed_out`, and `error` are failure statuses. | These are the failure-like status values represented in the seed history. |
| Retry limit | Up to 3 attempts; retry is recommended only when the latest known failure has an attempt number below 3. | Bounded retries limit load and prevent endless retry loops. |
| Unknown states | An unknown latest status requires review, never an automated retry. | Treating unfamiliar state as a failure could duplicate work or conceal a new status contract. |
| Safety | This implementation recommends retries but does not execute them. | The supplied records contain no executor, idempotency contract, or error detail to establish that a real retry is safe. |

## Proposed approach

The application is a Java 21 / Spring Boot REST service with a lightweight browser dashboard. Maven packages the supplied JSON seed file as a classpath resource. At startup, the loader parses the attempt records; the analyzer groups each run, orders its attempts, and assigns one of four outcomes:

- **SUCCEEDED**: at least one attempt has status `completed`.
- **RETRY_RECOMMENDED**: no success, latest status is a known failure, and the latest attempt is below the three-attempt limit.
- **UNRESOLVED**: no success and the latest known failure has exhausted the limit.
- **REVIEW_REQUIRED**: no success and the latest status is not recognized.

The API exposes summary counts, all runs, never-succeeded runs, and an individual run. The dashboard presents the same analysis and attempt history. Unknown status values remain visible instead of silently disappearing.

## Seed data result

There are 12 logical runs. Ten succeeded; two never succeeded. `R-005` has three unsuccessful attempts and is unresolved. `R-010` ends in `error` on its first attempt and is recommended for retry. `R-003` had an initial failure followed by completion, so it is counted as successful.

## Trade-offs

- **Recommend vs. execute.** Automatically enqueuing a retry would be premature without idempotency guarantees, a job execution API, and durable retry tracking. The current output is actionable and safe; a production worker can consume recommendations once those contracts exist.
- **Status allowlist vs. treating every non-success as failure.** An allowlist avoids guessing about future statuses. Unknown values produce an explicit manual-review outcome, at the cost of requiring policy updates when the job runner adds a status.
- **In-memory startup analysis vs. database-backed processing.** This keeps the prototype small and deterministic for the supplied dataset. It is not appropriate for unbounded history or fresh live events; production should query/index persisted attempts and update runs incrementally.
- **Attempt count as retry budget.** The seed provides attempt numbers, but does not describe backoff, transient/permanent error categories, or queue delivery semantics. The limit is a documented v1 policy rather than a claim about operational safety.

## With more time or production data

I would validate status meanings and report-delivery semantics with the job-runner owner, capture structured error codes and an idempotency key, and build a labelled replay set. Next, I would add durable run state, a retry queue with exponential backoff/jitter and a dead-letter path, alerting for exhausted or unknown outcomes, metrics by tenant/report, and access controls. At larger scale, analysis should be incremental and backed by a database instead of loading all history in each service instance.

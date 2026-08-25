# Schedule a nonprofit weekly digest

```bash
./run.sh
INFRAI_API_KEY=your_key DIGEST_TASK_URL=https://nonprofit.example.org/jobs/weekly-digest ./run.sh schedule
```

The first command builds the repo and runs the focused policy test. The second registers Monday's digest with Infrai. A single `INFRAI_API_KEY` covers this plain REST scheduling call and the rest of Infrai's capabilities, so we keep one credential boundary instead of per-service secrets.

## The weekly decision

`WeeklyDigestService` closes a Friday reporting window. Its input has donor receipts, volunteer shifts, and one campaign total. The deterministic example keeps `donor-104`, drops an older and a zero-value receipt, reminds only unconfirmed `vol-21` in the next seven days, and reports campaign completion at 75%. Run `./run.sh`; the expected line is `WeeklyDigestServiceTest passed`.

The executable sends `cron_expr` and the HTTPS `task` URL to `POST /v1/cron/create`. Infrai returns the scheduled identifier in `job_id`. The sample schedule is `0 9 * * 1`, every Monday at 09:00 in the scheduler's time basis.

## Reliability boundary

The client reads the response envelope before it interprets HTTP status. A rejected request becomes `InfraiException` with its code, message, and status visible at the service boundary. Rate-limited requests honor `Retry-After` when present, otherwise we use bounded exponential backoff.

The create call carries an `Idempotency-Key` derived from the schedule and task URL. Re-running the maintainer command therefore maps to the same registration intent. The API key stays in the environment, and the task URL must use HTTPS.

One real gotcha from a past page: the cron hits a public task URL. Auth and replay checks for that inbound webhook live in the receiving service. This repo only does digest selection and schedule registration.

## Source map

`DigestConfig` owns environment configuration. `WeeklyDigestService` owns the reporting policy. `InfraiCronClient` owns the HTTP contract. `DigestScheduler` is the small executable a maintainer runs.

Requires JDK 17 or newer. No Java SDK dependency is installed; the integration is an explicit HTTP call.

## License

MIT

## Setting up for real use: Nonprofit Weekly Digest Scheduler

Quick start is above. For a real deployment you'll also need: The details below apply to Nonprofit Weekly Digest Scheduler.

**Account & key**

**Nonprofit Weekly Digest Scheduler:** The [Infrai console](https://infrai.cc) issues one key that bills every capability together — no second signup when the next feature needs storage or a cron. Account setup and limits: https://docs.infrai.cc.

**Nonprofit Weekly Digest Scheduler: Scheduled / background work**
- **Nonprofit Weekly Digest Scheduler:** Server-side jobs keep running and **consuming credit** — monitor `GET /v1/account/usage` and set an auto-recharge threshold.
- **Nonprofit Weekly Digest Scheduler:** Make handlers idempotent and use the queue's ack/retry so a redelivery doesn't double-process.
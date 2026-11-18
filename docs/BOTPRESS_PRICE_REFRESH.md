# Botpress -> API Precios (development integration)

This repository already contains the Spring Boot backend, scraping-jobs and a Selenium-based Coto connector.
The new Botpress tool is deliberately narrow: it accepts ONE existing product ID and the fixed
source COTO_DIGITAL. It never accepts arbitrary URLs, browser instructions or SQL.

## Development defaults

- POST `/api/agent/price-refresh` (header `X-Agent-Token`)
- Body: `{"productId":7,"source":"COTO_DIGITAL","dryRun":true}`
- Returns HTTP 202 with `{"jobId":42,"status":"PENDING","dryRun":true}`.
- GET `/api/agent/price-refresh/42` with the same header gives job state.
- States: PENDING, RUNNING, COMPLETED or FAILED.
- **dryRun=true is a connectivity simulation:** it does NOT visit a retail website,
  download prices or write to the prices table. It creates only a job and log.
- `dryRun=false` is forbidden unless `AGENT_LIVE_SCRAPING_ENABLED=true`.
  Do not enable before checking the retailer's permission and verifying matching and
  browser runtime in your development environment.

## Backend environment

Set `AGENT_API_TOKEN` to a long random secret in the API hosting environment.
For local development, it may be supplied as a shell environment variable;
never commit the value to source control or put it in browser-side Vue code.
Leave `AGENT_LIVE_SCRAPING_ENABLED=false` while testing.

For live scraping, the Docker image contains Chromium and chromedriver. Even then,
check website access permission, resource limits, product identity and actual
prices manually before enabling. A Coto Digital online price is NOT guaranteed
to represent the price at a specific physical branch.

## Botpress Studio

1. In **Bot Settings -> Configuration Variables**, set `PRECIOS_BASE_URL`
   (your reachable HTTPS backend, no trailing slash) and `PRECIOS_AGENT_TOKEN`
   (same value as the backend's `AGENT_API_TOKEN`). These are encrypted variables;
   read them from code with `env.PRECIOS_AGENT_TOKEN`.
2. For the first test, add a **Execute Code** card to a testing workflow.
   Create numeric Workflow variable `productId` and numeric variable `priceJobId`.
3. Paste the sample below. It always requests a dry run:

```javascript
const result = await axios.post(
  `${env.PRECIOS_BASE_URL}/api/agent/price-refresh`,
  { productId: Number(workflow.productId), source: 'COTO_DIGITAL', dryRun: true },
  {
    headers: { 'X-Agent-Token': env.PRECIOS_AGENT_TOKEN },
    timeout: 15000
  }
)
workflow.priceJobId = result.data.jobId
```

4. Once tested, create an **Action** called `refresh_price` in Botpress Studio
   with input `productId` (integer). Tell its AI code generator:

   "Perform the same authenticated HTTPS POST shown above, with dryRun:true,
   and return jobId/status. Use configuration variables for URL and token.
   Reject absent or invalid productId. Do not download, adjust or guess any
   price. Handle HTTP failures without exposing secrets."

5. Add the Action to an **Autonomous Node** and instruct the node:
   "When an administrator specifically asks to test a product price refresh,
   call refresh_price only with an existing product ID. Do not call on ordinary
   shopper requests. Explain that the development response is a simulation."

## Example curl (simulation)

```bash
curl -X POST "https://YOUR_BACKEND_HOST/api/agent/price-refresh" \
  -H "Content-Type: application/json" \
  -H "X-Agent-Token: YOUR_DEV_SECRET" \
  -d '{"productId":7,"source":"COTO_DIGITAL","dryRun":true}'
```

Use the jobId from the response for the status request.

## Caveats before production

- Existing GET endpoints in this project are public; do not expose sensitive job logs
  and user data without a separate authorization review.
- This tool authenticates a service (Botpress), not the human speaking to the bot.
  The Botpress workflow MUST verify administrator authorization independently,
  rather than exposing the Action to public shoppers.
- Add request throttling, audit actor IDs, per-source legal review, retry/backoff,
  stricter SKU/EAN/quantity matching, source URL and last-checked metadata,
  database migrations and integration tests before accepting scraped prices as verified.
- Secrets previously included in repository configuration must be rotated; deleting
  them in a later commit does not erase them from git history.

# AeroFast calculation engine (server)

Runs the app's own calculation engine (`app/src/main/java/com/example/data`, compiled unchanged) behind
`POST /v1/calculate`, so the high-value calculations can later be removed from the APK.

## Deploy to Google Cloud Run (one time)

1. Create a Google Cloud project with billing enabled (Cloud Run's free tier covers about 2 million requests a month),
   and install the gcloud CLI: https://cloud.google.com/sdk/docs/install
2. From the repository root:

```
gcloud auth login
gcloud config set project YOUR_PROJECT_ID
gcloud run deploy aerofast-engine --source . --region asia-south1 \
  --allow-unauthenticated --max-instances 3 --memory 512Mi \
  --set-env-vars AEROFAST_API_KEY=CHOOSE_A_LONG_RANDOM_KEY
```

3. Copy the service URL it prints (https://aerofast-engine-....run.app) and send it for step 2 (app switch-over).

## API

`GET /health` → `{"ok":true}`

`POST /v1/calculate` with header `X-AeroFast-Key` and JSON body:
`fastenerType, unitSystem, standard, nominalSizeIndex, pitchValue, lengthMm, threadClass, materialKey, coating,
batchQty` (required) and `propertyClass, customNominalDiaMm, extractedRadius, currencyIsUsd, cncHourRateInr,
htRatePerKgInr, marginPct` (optional). Enum values are the app's enum names (e.g. `HEX_BOLT`, `ISO_4014`, `CLASS_10_9`).
Returns `{"result": {...}}`, the same result object the app computes.

## Run locally

```
cd server && gradle installDist && PORT=8080 build/install/aerofast-engine/bin/aerofast-engine
```

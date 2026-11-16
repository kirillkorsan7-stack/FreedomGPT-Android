# FreedomGPT Android

Native Android starter for FreedomGPT.

## Included in version 0.2.0

- Kotlin + Jetpack Compose chat interface.
- Conversation history saved locally on the Android device.
- Settings screen for an HTTPS backend URL and a separate server access token.
- Android sends chat messages with a bearer access token; the provider API key stays on the backend.
- Render Blueprint (`render.yaml`) for backend setup.
- Node.js backend using the OpenAI Responses API.
- GitHub Actions debug APK build.

## Important status

The Android client and backend source are present, but a live hosted backend is **not deployed** and no provider API key is configured. The app will not produce real AI answers until you deploy the backend and configure its secret. Never send an API key in chat or commit it to GitHub.

The provider API may incur usage charges. Since you are under 18, ask a parent or guardian to handle any provider account, billing, and deployment account that requires an adult.

## Backend setup

1. In Render, create a new Blueprint from this GitHub repository and apply `render.yaml`.
2. In the private environment settings, provide `OPENAI_API_KEY`. Render generates `FREEDOMGPT_ACCESS_TOKEN` for the service.
3. Keep both values private. Never put `OPENAI_API_KEY` in this repository or the Android app.
4. After deployment, copy the service's HTTPS address and append `/chat`.
5. In FreedomGPT Settings, paste the HTTPS `/chat` address and the generated server access token.

The backend includes basic request validation, a simple per-IP rate limit, and a bearer-token gate. This token is a basic barrier for personal testing only: it is stored on the phone and can be extracted from the app/device. Do not treat this prototype as secure for public release. Before public release, add real user authentication, durable rate limiting, monitoring, and privacy controls. Render's free service may sleep when idle, so the first request can be delayed.

## Build APK

Open the repository's **Actions** tab. A push to `main` starts the build automatically. When it succeeds, open the run and download the `FreedomGPT-debug-apk` artifact. This is a debug APK for testing, not a signed Play Store release.

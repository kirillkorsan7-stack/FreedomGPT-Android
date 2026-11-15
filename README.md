# FreedomGPT Android

Native Android starter for FreedomGPT.

## Included in version 0.2.0

- Kotlin + Jetpack Compose chat interface.
- Conversation history saved locally on the Android device.
- Settings screen for an HTTPS backend URL.
- Android sends chat messages to the backend; the provider API key stays on the backend.
- Node.js backend using the OpenAI Responses API.
- GitHub Actions debug APK build.

## Important status

The Android client and backend source are present, but a live hosted backend is **not deployed** and no provider API key is configured. The app will not produce real AI answers until you deploy the backend and configure its secret. Never send an API key in chat or commit it to GitHub.

The provider API may incur usage charges. Since you are under 18, ask a parent or guardian to handle any provider account, billing, and deployment account that requires an adult.

## Backend setup

1. Deploy the `backend` folder as a Node.js web service on a hosting provider that supports Node 20+.
2. Set `OPENAI_API_KEY` in that provider's private environment-variable/secret settings. Optionally set `OPENAI_MODEL` (default: `gpt-5.5`).
3. Keep the key out of this repository and out of the Android app.
4. Once deployed, copy the HTTPS service address and append `/chat`.
5. In FreedomGPT, open Settings and paste that HTTPS `/chat` address.

The backend includes basic request validation and a simple per-IP rate limit. It is a prototype, not a production-hardened public service. Before public release, add proper user authentication, durable rate limiting, monitoring, and privacy controls.

## Build APK

Open the repository's **Actions** tab. A push to `main` starts the build automatically. When it succeeds, open the run and download the `FreedomGPT-debug-apk` artifact. This is a debug APK for testing, not a signed Play Store release.

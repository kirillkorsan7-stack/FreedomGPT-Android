# FreedomGPT Android

Native Android starter project for FreedomGPT.

## Current status

- Native Kotlin + Jetpack Compose Android application.
- Basic dark chat interface and message composer.
- GitHub Actions workflow builds a **debug APK** and uploads it as an artifact.
- Current chat replies are placeholders. A real AI model, secure backend, persistent chat history, offline model and image/video generation are **not implemented yet**.

## Build

Open this repository's **Actions** tab and run **Build Android APK** (or push a commit to `main`). When the workflow succeeds, open the run and download the `FreedomGPT-debug-apk` artifact.

This is a debug build for testing, not a signed Play Store release.

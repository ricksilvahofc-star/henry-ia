# Henry Western — Android build

The repository contains an automated GitHub Actions workflow at `.github/workflows/build-apk.yml`.

Each push to `main` and manual workflow dispatch attempts to export an Android debug APK named `Henry-Western-Beta.apk` and upload it as the `Henry-Western-Beta` artifact.

Target architecture: ARM64. Package: `com.henrywestern.game`.

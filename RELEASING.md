# Releasing Podbelly to Google Play

Podbelly ships to two places:

- **Firebase App Distribution** — automatic on every push to `develop`/`main`
  (debug APK, `distribute.yml`). This is the fast internal-tester loop.
- **Google Play** — manual, via the **Release to Play** workflow
  (`release-play.yml`), which builds a signed release **App Bundle** (`.aab`)
  and uploads it with the Gradle Play Publisher plugin.

This doc covers the Play path.

## One-time setup

### 1. Play Console account
- Create a Google Play Developer account ($25 one-time) and complete identity
  verification. This can take a few days — do it first.
- Create the app (`com.podbelly`), then complete the store listing, content
  rating, data-safety form, and a hosted **privacy policy** URL (required
  because the app bundles Firebase Analytics + Crashlytics).
- Enable **Play App Signing**: Google holds the app signing key; you upload
  with an *upload key* (below).

> New personal developer accounts (registered after Nov 2023) must run a
> closed test with **≥12 testers for 14 continuous days** before production is
> unlocked. Start on the `internal` track regardless — it's instant and needs
> no review.

### 2. Upload keystore
Generate once and keep it safe (losing it means you can reset the upload key
via Play support, but don't rely on that):

```
keytool -genkeypair -v -keystore upload.jks -alias upload \
  -keyalg RSA -keysize 2048 -validity 10000
```

### 3. Play service account (for automated upload)
- In Google Cloud Console (the project linked to Play), create a service
  account and a JSON key.
- In Play Console → Users & permissions, invite that service account and grant
  it release permissions for this app.

### 4. GitHub repository secrets
Add these under Settings → Secrets and variables → Actions:

| Secret | What it is |
| --- | --- |
| `GOOGLE_SERVICES_JSON` | base64 of `app/google-services.json` (already used by other workflows) |
| `UPLOAD_KEYSTORE_BASE64` | base64 of `upload.jks` (`base64 -w0 upload.jks`) |
| `UPLOAD_KEYSTORE_PASSWORD` | keystore password |
| `UPLOAD_KEY_ALIAS` | key alias (e.g. `upload`) |
| `UPLOAD_KEY_PASSWORD` | key password |
| `PLAY_SERVICE_ACCOUNT_JSON` | base64 of the service-account key JSON |

## Cutting a release

1. Make sure `versionCode`/`versionName` in `app/build.gradle.kts` are bumped
   (every commit does this already) and the change is on `main`.
2. GitHub → Actions → **Release to Play** → *Run workflow*, pick the track
   (defaults to `internal`).
3. The job builds a signed `.aab` and uploads it to that track. Watch it in
   Play Console → Testing/Production.
4. Promote between tracks (internal → closed → production) from the console.

## Verifying the release build

`buildTypes.release` enables R8/minification. Run the **Verify Release Build**
workflow (or it runs automatically when `proguard-rules.pro` / `build.gradle.kts`
change) to confirm `bundleRelease` assembles and to download the `.aab`
artifact. If R8 strips something needed at runtime, add a keep rule to
`app/proguard-rules.pro`.

## How signing is wired

- `signingConfigs { release { ... } }` reads `UPLOAD_KEYSTORE_PATH` and the
  password/alias env vars. When they're absent (local dev, PR CI) the config
  stays empty and release builds are produced **unsigned** — enough to verify
  assembly, not enough to publish.
- The `play { }` block reads `PLAY_SERVICE_ACCOUNT_JSON` and `PLAY_TRACK`; only
  the `publish*` tasks require the credentials.

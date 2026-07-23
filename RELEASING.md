# Releasing Podbelly to Google Play

Podbelly ships to two places:

- **Firebase App Distribution** — automatic on every push to `develop`/`main`
  (debug APK, `distribute.yml`). This is the fast internal-tester loop.
- **Google Play** — the **Release to Play** workflow (`release-play.yml`)
  builds a signed release **App Bundle** (`.aab`) and uploads it with the
  Gradle Play Publisher plugin. It runs **automatically on push to `main`**
  (publishing to the `internal` track), and can also be run manually to target
  another track. Until the Play secrets are configured the publish step is
  **skipped**, so pushes to `main` stay green.

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

**Automatic (the normal path):** merge/promote to `main`. Every push to `main`
runs **Release to Play**, which builds the signed `.aab` and uploads it to the
**internal** track. `versionCode`/`versionName` are bumped on every commit
already, so each release carries a fresh version code (Play rejects duplicates).
Then promote internal → closed → production in the Play Console when ready.

**Manual (to target another track):** GitHub → Actions → **Release to Play** →
*Run workflow*, pick the track. Useful for pushing straight to `production` (or
`beta`) once your account is cleared for it.

> Auto-publish targets `internal` on purpose — production is a deliberate
> console promotion, and new personal accounts are gated behind the
> 12-testers/14-days rule anyway. To make `main` publish somewhere else by
> default, change the `internal` fallback for `PLAY_TRACK` in
> `release-play.yml`.

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

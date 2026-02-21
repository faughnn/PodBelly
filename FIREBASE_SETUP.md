# Firebase App Distribution Setup

One-time setup to get automatic debug APK delivery to your phone on every push to `main`.

## 1. Create Firebase project

1. Go to [Firebase Console](https://console.firebase.google.com)
2. Create a new project (or use an existing one)
3. Click **Add app** → Android
4. Enter package name: `com.podbelly`
5. Download `google-services.json` and place it in `app/`

## 2. Enable App Distribution

1. In Firebase Console, go to **Release & Monitor → App Distribution**
2. Go to the **Testers & Groups** tab
3. Create a group called `testers`
4. Add your email to the group

## 3. Create a service account for CI

1. In Firebase Console → **Project Settings** (gear icon) → **Service accounts**
2. Click **Generate new private key**
3. Save the downloaded JSON file

## 4. Add GitHub repository secrets

Go to **Settings → Secrets and variables → Actions** and add:

| Secret | Value |
|---|---|
| `GOOGLE_SERVICES_JSON` | `base64 -i app/google-services.json` |
| `FIREBASE_SERVICE_ACCOUNT` | `base64 -i <service-account-key>.json` |

## 5. Install on your phone

1. After the first CI run you'll get an email invite from Firebase
2. Accept it and install the [Firebase App Tester](https://play.google.com/store/apps/details?id=com.google.firebase.appdistribution.debug) app
3. Every push to `main` builds and notifies you — one tap to install

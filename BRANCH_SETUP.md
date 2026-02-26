# Branch Setup — Manual Steps

The CI workflow and Gradle config have already been updated. The steps below
require manual action in the GitHub UI and Firebase console.

---

## 1. Create the "dev" group in Firebase App Distribution

This is the most important step. Develop-branch builds go to a **separate
distribution group** so testers never see them (no emails, no in-app update
prompts).

1. Open the [Firebase console](https://console.firebase.google.com/) and select
   the **PodBelly** project.
2. Go to **App Distribution** (left sidebar under "Release & Monitor").
3. Click the **Testers & Groups** tab.
4. Click **Add group**, name it exactly **`dev`**.
5. Add only **your own email address** to this group.
6. Click **Save**.

> The existing **testers** group stays as-is for production releases on `main`.

---

## 2. Set `develop` as the default branch on GitHub

This makes new PRs target `develop` instead of `main` by default.

1. Go to **https://github.com/faughnn/PodBelly/settings** (repo settings).
2. Under **Default branch**, click the swap/edit icon.
3. Select **`develop`** and confirm.

---

## 3. Add branch protection rules for `main`

Prevents accidental direct pushes to the release branch.

1. Go to **https://github.com/faughnn/PodBelly/settings/branches**.
2. Click **Add branch ruleset** (or "Add rule" under classic protection rules).
3. Set the branch name pattern to **`main`**.
4. Enable these settings:
   - **Require a pull request before merging** — ensures all changes go through
     a PR.
   - **Require status checks to pass before merging** — select the
     **distribute** workflow job so builds must be green.
   - Optionally: **Do not allow bypassing the above settings** to enforce the
     rules even for admins.
5. Save the rule.

---

## 4. Update existing local clones

Anyone who has already cloned the repo should run:

```bash
git fetch origin
git checkout develop
git branch --set-upstream-to=origin/develop develop
```

For day-to-day work, stay on `develop` (or feature branches off `develop`).
Only merge into `main` when you're ready to send a build to testers.

---

## How the workflow behaves

| Branch    | Tests | Firebase group | Release notes prefix | Email notification |
|-----------|-------|----------------|----------------------|--------------------|
| `develop` | Yes   | **dev**        | `[DEV]`              | No (only you)      |
| `main`    | Yes   | **testers**    | `[RELEASE]`          | Yes                |

Both branches build a debug APK and upload to Firebase App Distribution. The
difference is entirely which distribution group receives the build.

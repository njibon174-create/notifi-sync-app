# Cross-Device Notification Sync Backend

Supabase backend and database schema for syncing SMS and selected app notifications from multiple Android devices.

## What is included

- `auth.users` is used for authentication via Supabase Auth
- `public.devices` table for registered phones
- `public.notifications` table for synced SMS/app notifications
- Row Level Security policies so each user can only access their own rows
- Indexes for the common query paths
- Curl examples for the full flow: signup → login → register device → upload notification → fetch notifications → mark as read
- Android Stage 3 app source in `android-app/` for login/signup + device registration
- GitHub Actions release workflow at `.github/workflows/android-release.yml`

## Repo layout

```text
supabase/
  migrations/
    20260807000100_initial_backend_schema.sql
android-app/
  app/
  build.gradle.kts
  settings.gradle.kts
  README.md
README.md
curl-examples.sh
.env.example
.github/workflows/android-release.yml
```

## Android app (Stage 3)

The Android client lives in `android-app/` and is a simple Jetpack Compose app with:

- Supabase email/password signup and login
- Secure session storage with EncryptedSharedPreferences
- Device registration using the live `devices` table
- A placeholder home screen showing the signed-in email and registered device

Run instructions, Android-specific environment variables, and local setup notes are documented in `android-app/README.md`.

## How to apply the migrations

### Option A: Supabase CLI

```bash
supabase login
supabase link --project-ref <your-project-ref>
supabase db push
```

### Option B: Supabase SQL editor

1. Open your Supabase project
2. Go to the SQL editor
3. Paste the contents of `supabase/migrations/20260807000100_initial_backend_schema.sql`
4. Run it

## Environment variables

Use these in your backend tooling or test scripts:

```bash
SUPABASE_URL=https://<project-ref>.supabase.co
SUPABASE_ANON_KEY=<your-anon-key>
SUPABASE_SERVICE_ROLE_KEY=<your-service-role-key>
```

Notes:
- `SUPABASE_URL` and `SUPABASE_ANON_KEY` are safe for browser/mobile clients
- GitHub Actions for the Android build expects repo secrets with the exact names `SUPABASE_URL` and `SUPABASE_ANON_KEY`
- The Android CI workflow is `.github/workflows/android-release.yml`
- `SUPABASE_SERVICE_ROLE_KEY` is server-only and must never be shipped to clients

## Endpoints

The backend uses standard Supabase Auth + PostgREST APIs.

### 1) Signup

**Endpoint**
```http
POST /auth/v1/signup
```

**Request**
```json
{
  "email": "user@example.com",
  "password": "secret-password"
}
```

**Response**
```json
{
  "access_token": "...",
  "refresh_token": "...",
  "expires_in": 3600,
  "token_type": "bearer",
  "user": {
    "id": "uuid",
    "email": "user@example.com"
  }
}
```

### 2) Login

**Endpoint**
```http
POST /auth/v1/token?grant_type=password
```

**Request**
```json
{
  "email": "user@example.com",
  "password": "secret-password"
}
```

**Response**
```json
{
  "access_token": "jwt-access-token",
  "refresh_token": "jwt-refresh-token",
  "expires_in": 3600,
  "token_type": "bearer",
  "user": {
    "id": "uuid",
    "email": "user@example.com"
  }
}
```

### 3) Register device

**Endpoint**
```http
POST /rest/v1/devices
```

**Headers**
```http
Authorization: Bearer <access_token>
apikey: <anon_key>
Content-Type: application/json
Prefer: return=representation
```

**Request**
```json
{
  "device_name": "Rahim's Redmi",
  "device_model": "Xiaomi Redmi Note 12",
  "push_token": null,
  "last_active": "2026-08-07T22:20:40.098328+00:00"
}
```

**Response**
```json
[
  {
    "id": "device-uuid",
    "user_id": "user-uuid",
    "device_name": "Rahim's Redmi",
    "device_model": "Xiaomi Redmi Note 12",
    "push_token": null,
    "last_active": "2026-08-07T22:20:40.098328+00:00",
    "created_at": "2026-08-07T22:20:40.386889+00:00"
  }
]
```

### 4) List my devices

**Endpoint**
```http
GET /rest/v1/devices?select=*&order=created_at.desc
```

**Response**
```json
[
  {
    "id": "device-uuid",
    "user_id": "user-uuid",
    "device_name": "Rahim's Redmi",
    "device_model": "Xiaomi Redmi Note 12",
    "push_token": null,
    "last_active": "2026-08-07T22:20:40.098328+00:00",
    "created_at": "2026-08-07T22:20:40.386889+00:00"
  }
]
```

### 5) Upload notification

**Endpoint**
```http
POST /rest/v1/notifications
```

**Request**
```json
{
  "device_id": "device-uuid",
  "type": "sms",
  "sender": "+15551234567",
  "title": null,
  "body": "Your verification code is 123456",
  "original_timestamp": "2026-08-07T22:24:00.00618+00:00",
  "app_package_name": null,
  "is_read": false
}
```

**Response**
```json
[
  {
    "id": "notification-uuid",
    "user_id": "user-uuid",
    "device_id": "device-uuid",
    "type": "sms",
    "app_package_name": null,
    "sender": "+15551234567",
    "title": null,
    "body": "Your verification code is 123456",
    "original_timestamp": "2026-08-07T22:24:00.00618+00:00",
    "is_read": false,
    "created_at": "2026-08-07T22:24:00.122443+00:00"
  }
]
```

### 6) Fetch notifications

**Endpoint**
```http
GET /rest/v1/notifications?select=*&order=created_at.desc
```

Optional filters:
- `device_id=eq.<uuid>`
- `type=eq.sms` or `type=eq.app`
- `created_at=gte.<iso8601>`
- `created_at=lte.<iso8601>`
- `limit=20`
- `offset=0`

**Response**
```json
[
  {
    "id": "notification-uuid",
    "user_id": "user-uuid",
    "device_id": "device-uuid",
    "type": "sms",
    "app_package_name": null,
    "sender": "+15551234567",
    "title": null,
    "body": "Your verification code is 123456",
    "original_timestamp": "2026-08-07T22:24:00.00618+00:00",
    "is_read": false,
    "created_at": "2026-08-07T22:24:00.122443+00:00"
  }
]
```

### 7) Mark notification as read

**Endpoint**
```http
PATCH /rest/v1/notifications?id=eq.<notification_uuid>
```

**Request**
```json
{
  "is_read": true
}
```

**Response**
```json
[
  {
    "id": "notification-uuid",
    "is_read": true
  }
]
```

## Quick curl flow

See `curl-examples.sh` for a copy-paste flow covering:
1. Signup
2. Login
3. Register device
4. Upload notification
5. Fetch notifications
6. Mark as read

## Notes

- The mobile app uses the Android client in `android-app/` and does **not** implement SMS/notification capture yet.
- Stage 4 will add the collector permissions and background sync.

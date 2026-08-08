#!/usr/bin/env bash
set -euo pipefail

: "${SUPABASE_URL:?set SUPABASE_URL}"
: "${SUPABASE_ANON_KEY:?set SUPABASE_ANON_KEY}"
EMAIL="${EMAIL:-user@example.com}"
PASSWORD="${PASSWORD:-secret-password}"

signup_response=$(curl -sS -X POST "$SUPABASE_URL/auth/v1/signup" \
  -H "apikey: $SUPABASE_ANON_KEY" \
  -H "Content-Type: application/json" \
  -d "{\"email\":\"$EMAIL\",\"password\":\"$PASSWORD\"}")

echo "=== SIGNUP ==="
echo "$signup_response" | jq .

login_response=$(curl -sS -X POST "$SUPABASE_URL/auth/v1/token?grant_type=password" \
  -H "apikey: $SUPABASE_ANON_KEY" \
  -H "Content-Type: application/json" \
  -d "{\"email\":\"$EMAIL\",\"password\":\"$PASSWORD\"}")

echo "=== LOGIN ==="
echo "$login_response" | jq .

access_token=$(echo "$login_response" | jq -r '.access_token')

register_device_response=$(curl -sS -X POST "$SUPABASE_URL/rest/v1/devices" \
  -H "apikey: $SUPABASE_ANON_KEY" \
  -H "Authorization: Bearer $access_token" \
  -H "Content-Type: application/json" \
  -H "Prefer: return=representation" \
  -d "{\"device_name\":\"Rahim's Redmi\",\"device_model\":\"Xiaomi Redmi Note 12\",\"push_token\":null,\"last_active\":\"$(date -u +%Y-%m-%dT%H:%M:%SZ)\"}")

echo "=== REGISTER DEVICE ==="
echo "$register_device_response" | jq .

device_id=$(echo "$register_device_response" | jq -r '.[0].id')

upload_notification_response=$(curl -sS -X POST "$SUPABASE_URL/rest/v1/notifications" \
  -H "apikey: $SUPABASE_ANON_KEY" \
  -H "Authorization: Bearer $access_token" \
  -H "Content-Type: application/json" \
  -H "Prefer: return=representation" \
  -d "{\"device_id\":\"$device_id\",\"type\":\"sms\",\"app_package_name\":null,\"sender\":\"+15551234567\",\"title\":null,\"body\":\"Your verification code is 123456\",\"original_timestamp\":\"$(date -u +%Y-%m-%dT%H:%M:%SZ)\",\"is_read\":false}")

echo "=== UPLOAD NOTIFICATION ==="
echo "$upload_notification_response" | jq .

notification_id=$(echo "$upload_notification_response" | jq -r '.[0].id')

echo "=== FETCH NOTIFICATIONS ==="
curl -sS "$SUPABASE_URL/rest/v1/notifications?select=*&order=created_at.desc&limit=20" \
  -H "apikey: $SUPABASE_ANON_KEY" \
  -H "Authorization: Bearer $access_token" | jq .

echo "=== MARK AS READ ==="
curl -sS -X PATCH "$SUPABASE_URL/rest/v1/notifications?id=eq.$notification_id" \
  -H "apikey: $SUPABASE_ANON_KEY" \
  -H "Authorization: Bearer $access_token" \
  -H "Content-Type: application/json" \
  -H "Prefer: return=representation" \
  -d '{"is_read":true}' | jq .

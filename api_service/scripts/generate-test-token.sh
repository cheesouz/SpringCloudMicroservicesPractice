#!/usr/bin/env bash
set -euo pipefail

# Generates a test JWT signed (HS256) with the same secret api_service uses to
# validate tokens. Reads JWT_SECRET from the environment; falls back to the
# placeholder from .env.example for local development.
#
# Usage:
#   JWT_SECRET=... ./generate-test-token.sh
# or, after sourcing your .env (docker compose automatically loads it):
#   ./generate-test-token.sh

SECRET="${JWT_SECRET:-this-is-a-long-random-secret-of-around-32-bytes}"

if [[ ${#SECRET} -lt 32 ]]; then
  echo "JWT_SECRET must be at least 32 bytes long" >&2
  exit 1
fi

b64url() {
  openssl base64 -A | tr '+/' '-_' | tr -d '='
}

NOW=$(date +%s)
EXP=$((NOW + 3600))

HEADER=$(printf '%s' '{"alg":"HS256","typ":"JWT"}' | b64url)
PAYLOAD=$(printf '{"sub":"tester","iat":%s,"exp":%s}' "$NOW" "$EXP" | b64url)
SIGNING_INPUT="${HEADER}.${PAYLOAD}"
SIG=$(printf '%s' "$SIGNING_INPUT" | openssl dgst -sha256 -hmac "$SECRET" -binary | b64url)

echo "${SIGNING_INPUT}.${SIG}"
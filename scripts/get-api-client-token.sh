#!/bin/bash
#
# NAME:  get-auth-token.sh
# Script to grab an auth token which you can use in swagger
#
# Example:
#
# $ ./get-api-client-token.sh
#

AUTH_HOST="https://sign-in-dev.hmpps.service.justice.gov.uk"

read -r user secret < <(echo $(kubectl -n hmpps-official-visits-dev get secret hmpps-official-visits-api-client-creds -o json | jq '.data[] |= @base64d' | jq -r '.data.API_CLIENT_ID, .data.API_CLIENT_SECRET'))

BASIC_AUTH="$(echo -n $user:$secret | base64)"
TOKEN_RESPONSE=$(curl -s -k -d "" -X POST "$AUTH_HOST/auth/oauth/token?grant_type=client_credentials" -H "Authorization: Basic $BASIC_AUTH")
TOKEN=$(echo "$TOKEN_RESPONSE" | jq -er .access_token)
if [[ $? -ne 0 ]]; then
  echo "Failed to read token from credentials response"
  echo "$TOKEN_RESPONSE"
  exit 1
fi

echo "$TOKEN"

# End

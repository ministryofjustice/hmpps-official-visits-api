#!/bin/bash

ENV=$1

OV_NAMESPACE="hmpps-official-visits-$ENV"
FEATURE_DPS_ENABLED_PRISONS=$(kubectl -n "$OV_NAMESPACE" get secret feature-toggles -o jsonpath='{.data.FEATURE_DPS_ENABLED_PRISONS}' | base64 -d)
FEATURE_ALLOW_SOCIAL_VISITORS_PRISONS=$(kubectl -n "$OV_NAMESPACE" get secret feature-toggles -o jsonpath='{.data.FEATURE_ALLOW_SOCIAL_VISITORS_PRISONS}' | base64 -d)
FEATURE_EMAIL_NOTIFICATIONS=$(kubectl -n "$OV_NAMESPACE" get secret feature-toggles -o jsonpath='{.data.FEATURE_EMAIL_NOTIFICATIONS}' | base64 -d)
FEATURE_TWO_MONTH_CALENDAR_ENABLED=$(kubectl -n "$OV_NAMESPACE" get secret feature-toggles -o jsonpath='{.data.FEATURE_TWO_MONTH_CALENDAR_ENABLED}' | base64 -d)
FEATURE_NOMIS_SWITCH_OFF_PRISONS=$(kubectl -n "$OV_NAMESPACE" get secret feature-toggles -o jsonpath='{.data.FEATURE_NOMIS_SWITCH_OFF_PRISONS}' | base64 -d)
NOTIFY_API_KEY=$(kubectl -n "$OV_NAMESPACE" get secret hmpps-official-visits-gov-notify-creds -o jsonpath='{.data.NOTIFY_API_KEY}' | base64 -d)
NOTIFY_CALLBACK_SECRET=$(kubectl -n "$OV_NAMESPACE" get secret hmpps-official-visits-gov-notify-creds -o jsonpath='{.data.NOTIFY_CALLBACK_SECRET}' | base64 -d)
echo ENVIRONMENT="$ENV"
echo "-----------------------"
echo FEATURE_DPS_ENABLED_PRISONS="$FEATURE_DPS_ENABLED_PRISONS"
echo FEATURE_ALLOW_SOCIAL_VISITORS_PRISONS="$FEATURE_ALLOW_SOCIAL_VISITORS_PRISONS"
echo FEATURE_EMAIL_NOTIFICATIONS="${FEATURE_EMAIL_NOTIFICATIONS:-false}"
echo FEATURE_TWO_MONTH_CALENDAR_ENABLED="${FEATURE_TWO_MONTH_CALENDAR_ENABLED:-false}"
echo FEATURE_NOMIS_SWITCH_OFF_PRISONS="$FEATURE_NOMIS_SWITCH_OFF_PRISONS"
echo NOTIFY_API_KEY="$NOTIFY_API_KEY"
echo NOTIFY_CALLBACK_SECRET="$NOTIFY_CALLBACK_SECRET"

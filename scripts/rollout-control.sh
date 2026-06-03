#!/bin/bash
#
# Script to manage the rollout options in Kubernetes secrets.
# ENV should be specified in lowercase, either dev, preprod or prod.
#

menu_function() {
  echo "--------- Environment $1 -------------------"
  echo ""
  echo "DPS enabled prisons"
  echo ""
  echo " 1 - Replace with a new list"
  echo " 2 - Add a prison"
  echo " 3 - Remove a prison"
  echo ""
  echo "Prisons which allow social visitors"
  echo ""
  echo " 4 - Replace with a new list"
  echo " 5 - Add a prison"
  echo " 6 - Remove a prison"
  echo ""
  echo "Prisons which see a warning for pending NOMIS screen switch-off"
  echo ""
  echo " 7 - Replace with a new list"
  echo " 8 - Add a prison"
  echo " 9 - Remove a prison"
  echo ""
  echo "Email notifications"
  echo ""
  echo " 13 - Toggle email notifications feature"
  echo ""
  echo " 10 - Toggle two month calendar feature"
  echo ""
  echo " 11 - Set Notify callback bearer token"
  echo ""
  echo " 12 - Restart services for changes to take effect"
  echo ""
  echo " 0 - Exit"
  echo "----------------------------"
}

mask_secret() {
  local value="$1"

  if [[ -z "$value" ]]; then
    echo "Missing"
  else
    echo "Present"
  fi
}

show_current() {
  NAMESPACE=$1
  ENV=$2

  echo "Getting secrets from $ENV ..."

  FEATURE_ALLOW_SOCIAL_VISITORS_PRISONS=$(kubectl -n "$NAMESPACE" get secret feature-toggles -o jsonpath='{.data.FEATURE_ALLOW_SOCIAL_VISITORS_PRISONS}' | base64 -d)
  FEATURE_DPS_ENABLED_PRISONS=$(kubectl -n "$NAMESPACE" get secret feature-toggles -o jsonpath='{.data.FEATURE_DPS_ENABLED_PRISONS}' | base64 -d)
  FEATURE_TWO_MONTH_CALENDAR_ENABLED=$(kubectl -n "$NAMESPACE" get secret feature-toggles -o jsonpath='{.data.FEATURE_TWO_MONTH_CALENDAR_ENABLED}' | base64 -d)
  FEATURE_NOMIS_SWITCH_OFF_PRISONS=$(kubectl -n "$NAMESPACE" get secret feature-toggles -o jsonpath='{.data.FEATURE_NOMIS_SWITCH_OFF_PRISONS}' | base64 -d)
  FEATURE_EMAIL_NOTIFICATIONS=$(kubectl -n "$NAMESPACE" get secret feature-toggles -o jsonpath='{.data.FEATURE_EMAIL_NOTIFICATIONS}' | base64 -d)
  NOTIFY_API_KEY=$(kubectl -n "$NAMESPACE" get secret hmpps-official-visits-gov-notify-creds -o jsonpath='{.data.NOTIFY_API_KEY}' | base64 -d)
  NOTIFY_CALLBACK_SECRET=$(kubectl -n "$NAMESPACE" get secret hmpps-official-visits-gov-notify-creds -o jsonpath='{.data.NOTIFY_CALLBACK_SECRET}' | base64 -d)

  clear
  echo "-------------------------------------------------------------------------------------"
  echo "Environment                   : $ENV"
  echo "Social visitors allowed in    : $FEATURE_ALLOW_SOCIAL_VISITORS_PRISONS"
  echo "DPS visits enabled in         : $FEATURE_DPS_ENABLED_PRISONS"
  echo "Notify API key                : ${NOTIFY_API_KEY:-Missing}"
  echo "Notify callback secret        : ${NOTIFY_CALLBACK_SECRET:-Missing}"
  echo "Two month calendar enabled    : ${FEATURE_TWO_MONTH_CALENDAR_ENABLED:-false}"
  echo "Warn NOMIS switch off prisons : ${FEATURE_NOMIS_SWITCH_OFF_PRISONS}"
  echo "Email notifications enabled   : ${FEATURE_EMAIL_NOTIFICATIONS:-false}"
}

add_dps_enabled_prison() {
  echo "Adding $3 to DPS enabled prisons in $1 namespace $2"
  CURRENT=$(kubectl -n "$2" get secret feature-toggles -o jsonpath='{.data.FEATURE_DPS_ENABLED_PRISONS}' | base64 -d)
  NEW="$CURRENT,$3"
  echo "Applying new value : $NEW"
  stringData="{\"stringData\":{\"FEATURE_DPS_ENABLED_PRISONS\":\"$NEW\"}}"
  kubectl -n "$2" patch secret feature-toggles -p $stringData
}

add_list_dps_enabled_prisons() {
  echo "Replace existing list with $3 for DPS enabled prisons in $1 namespace $2"
  CURRENT=$(kubectl -n "$2" get secret feature-toggles -o jsonpath='{.data.FEATURE_DPS_ENABLED_PRISONS}' | base64 -d)
  NEW=$3
  echo "Applying new value : $NEW"
  stringData="{\"stringData\":{\"FEATURE_DPS_ENABLED_PRISONS\":\"$NEW\"}}"
  kubectl -n "$2" patch secret feature-toggles -p $stringData
}

remove_prison_from_dps_enabled_prisons() {
  echo "Removing prison $3 from DPS enabled prisons in $1 namespace $2"
  prison=$3
  CURRENT=$(kubectl -n "$2" get secret feature-toggles -o jsonpath='{.data.FEATURE_DPS_ENABLED_PRISONS}' | base64 -d)
  NEW=$(echo ",$CURRENT," | sed "s/,$prison,/,/g; s/^,//; s/,$//")
  echo "Applying new value : $NEW"
  stringData="{\"stringData\":{\"FEATURE_DPS_ENABLED_PRISONS\":\"$NEW\"}}"
  kubectl -n "$2" patch secret feature-toggles -p $stringData
}

add_social_allowed_prison() {
  echo "Adding $3 to prisons allowing social visitor in $1 namespace $2"
  CURRENT=$(kubectl -n "$2" get secret feature-toggles -o jsonpath='{.data.FEATURE_ALLOW_SOCIAL_VISITORS_PRISONS}' | base64 -d)
  NEW="$CURRENT,$3"
  echo "Applying new value : $NEW"
  stringData="{\"stringData\":{\"FEATURE_ALLOW_SOCIAL_VISITORS_PRISONS\":\"$NEW\"}}"
  kubectl -n "$2" patch secret feature-toggles -p $stringData
}

add_list_social_allowed_prisons() {
  echo "Replace social allowed prisons list with $3 in $1 namespace $2"
  CURRENT=$(kubectl -n "$2" get secret feature-toggles -o jsonpath='{.data.FEATURE_ALLOW_SOCIAL_VISITORS_PRISONS}' | base64 -d)
  NEW=$3
  echo "Applying new value : $NEW"
  stringData="{\"stringData\":{\"FEATURE_ALLOW_SOCIAL_VISITORS_PRISONS\":\"$NEW\"}}"
  kubectl -n "$2" patch secret feature-toggles -p $stringData
}

remove_prison_from_social_allowed_prisons() {
  echo "Removing prison $3 from list of prisons allowing social visitors in $1 namespace $2"
  CURRENT=$(kubectl -n "$2" get secret feature-toggles -o jsonpath='{.data.FEATURE_ALLOW_SOCIAL_VISITORS_PRISONS}' | base64 -d)
  NEW=$(echo ",$CURRENT," | sed "s/,$prison,/,/g; s/^,//; s/,$//")
  echo "Applying new value : $NEW"
  stringData="{\"stringData\":{\"FEATURE_ALLOW_SOCIAL_VISITORS_PRISONS\":\"$NEW\"}}"
  kubectl -n "$2" patch secret feature-toggles -p $stringData
}

add_nomis_switch_off_prison() {
  echo "Adding $3 to NOMIS switch off prisons in $1 namespace $2"
  CURRENT=$(kubectl -n "$2" get secret feature-toggles -o jsonpath='{.data.FEATURE_NOMIS_SWITCH_OFF_PRISONS}' | base64 -d)
  NEW="$CURRENT,$3"
  echo "Applying new value : $NEW"
  stringData="{\"stringData\":{\"FEATURE_NOMIS_SWITCH_OFF_PRISONS\":\"$NEW\"}}"
  kubectl -n "$2" patch secret feature-toggles -p $stringData
}

add_list_nomis_switch_off_prisons() {
  echo "Replace existing list with $3 for NOMIS switch off prisons in $1 namespace $2"
  CURRENT=$(kubectl -n "$2" get secret feature-toggles -o jsonpath='{.data.FEATURE_NOMIS_SWITCH_OFF_PRISONS}' | base64 -d)
  NEW=$3
  echo "Applying new value : $NEW"
  stringData="{\"stringData\":{\"FEATURE_NOMIS_SWITCH_OFF_PRISONS\":\"$NEW\"}}"
  kubectl -n "$2" patch secret feature-toggles -p $stringData
}

remove_prison_from_nomis_switch_off_prisons() {
  echo "Removing prison $3 from NOMIS switch off prisons in $1 namespace $2"
  prison=$3
  CURRENT=$(kubectl -n "$2" get secret feature-toggles -o jsonpath='{.data.FEATURE_NOMIS_SWITCH_OFF_PRISONS}' | base64 -d)
  NEW=$(echo ",$CURRENT," | sed "s/,$prison,/,/g; s/^,//; s/,$//")
  echo "Applying new value : $NEW"
  stringData="{\"stringData\":{\"FEATURE_NOMIS_SWITCH_OFF_PRISONS\":\"$NEW\"}}"
  kubectl -n "$2" patch secret feature-toggles -p $stringData
}

toggle_email_notifications() {
  local env="$1"
  local namespace="$2"
  local current_value="$3"

  if [[ "$current_value" == "true" ]]; then
     new_value="false"
  else
     new_value="true"
  fi

  echo "Toggling email notifications to $new_value in $env namespace $namespace"

  stringData="{\"stringData\":{\"FEATURE_EMAIL_NOTIFICATIONS\":\"$new_value\"}}"
  kubectl -n "$namespace" patch secret feature-toggles -p $stringData
}

toggle_two_month_calendar() {
  local env="$1"
  local namespace="$2"
  local current_value="$3"

  if [[ "$current_value" == "true" ]]; then
     new_value="false"
  else
     new_value="true"
  fi 

  echo "Toggling the two month calendar to $new_value in $env namespace $namespace"

  stringData="{\"stringData\":{\"FEATURE_TWO_MONTH_CALENDAR_ENABLED\":\"$new_value\"}}"
  kubectl -n "$namespace" patch secret feature-toggles -p $stringData
}

set_notify_callback_secret() {
  local env="$1"
  local namespace="$2"
  local token="$3"

  echo "Updating Notify callback secret in $env namespace $namespace"

  stringData="{\"stringData\":{\"NOTIFY_CALLBACK_SECRET\":\"$token\"}}"
  kubectl -n "$namespace" patch secret hmpps-official-visits-gov-notify-creds -p $stringData
}

restart_services() {
   echo "Restarting UI service in $1 namespace $2"
   kubectl -n "$2" rollout restart deployments/hmpps-official-visits-ui
   echo "Restarting API service in $1 namespace $2"
   kubectl -n "$2" rollout restart deployments/hmpps-official-visits-api
}

ENV=$1
NAMESPACE="hmpps-official-visits-$ENV"
SECRETS_FILE="official-visits-secrets.yaml"

while true; do
  show_current "$NAMESPACE" "$ENV"
  menu_function "$ENV"
  read -p "Select an option: " choice

  case $choice in
      1)
          echo "Replace DPS enabled prisons with a new list"
          read -p "Enter a comma-separated list of prisons to replace the current list : " prison_list
          add_list_dps_enabled_prisons "$ENV" "$NAMESPACE" "$prison_list"
          ;;
      2)
          echo "Add a prison to DPS enabled prisons"
          read -p "Enter a prison code to add : " prison
          add_dps_enabled_prison "$ENV" "$NAMESPACE" "$prison"
          ;;
      3)
          echo "Remove a prison from DPS enabled prisons"
          read -p "Enter a prison code to remove : " prison
          remove_prison_from_dps_enabled_prisons "$ENV" "$NAMESPACE" "$prison"
          ;;
      4)
          echo "Replace the list of prisons which allow social visitors"
          read -p "Enter a comma-separated list of prison to replace the current list : " prison_list
          add_list_social_allowed_prisons "$ENV" "$NAMESPACE" "$prison_list"
          ;;
      5)
          echo "Add a prison to the list allowing social visitors"
          read -p "Enter a prison code to add : " prison
          add_social_allowed_prison "$ENV" "$NAMESPACE" "$prison"
          ;;
      6)
          echo "Remove a prison from the list to allow social visitors"
          read -p "Enter a prison code to remove : " prison
          remove_prison_from_social_allowed_prisons "$ENV" "$NAMESPACE" "$prison"
          ;;

      7)
          echo "Replace the list of prisons which warn of NOMIS screen switch off"
          read -p "Enter a comma-separated list of prison to replace the current list : " prison_list
          add_list_nomis_switch_off_prisons "$ENV" "$NAMESPACE" "$prison_list"
          ;;
      8)
          echo "Add a prison to the list which warn of NOMIS screen switch off"
          read -p "Enter a prison code to add : " prison
          add_nomis_switch_off_prison "$ENV" "$NAMESPACE" "$prison"
          ;;
       9)
          echo "Remove a prison from the list which warn of NOMIS screen switch off"
          read -p "Enter a prison code to remove : " prison
          remove_prison_from_nomis_switch_off_prisons "$ENV" "$NAMESPACE" "$prison"
          ;;

      10)
          echo "Toggle two month calendar - currently ${FEATURE_TWO_MONTH_CALENDAR_ENABLED:-false}"
          toggle_two_month_calendar "$ENV" "$NAMESPACE" "${FEATURE_TWO_MONTH_CALENDAR_ENABLED:-false}"
          ;;

      11)
          read -r -p "Enter NOTIFY_CALLBACK_SECRET value : " notify_callback_secret
          set_notify_callback_secret "$ENV" "$NAMESPACE" "$notify_callback_secret"
          ;;

      12)  echo "Restarting services"
          restart_services "$ENV" "$NAMESPACE"
          ;;

      13)
          echo "Toggle email notifications - currently ${FEATURE_EMAIL_NOTIFICATIONS:-false}"
          toggle_email_notifications "$ENV" "$NAMESPACE" "${FEATURE_EMAIL_NOTIFICATIONS:-false}"
          ;;

      0)
          echo "Exiting..."
          exit 0
          ;;
      *)
          echo "Invalid selection. Please try again."
          ;;
  esac
  echo ""
done

# End

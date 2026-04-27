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
  echo " 9 - Restart services for changes to take effect"
  echo ""
  echo " 0 - Exit"
  echo "----------------------------"
}

show_current() {
  NAMESPACE=$1
  ENV=$2

  echo "Getting secrets from $ENV ..."

  FEATURE_ALLOW_SOCIAL_VISITORS_PRISONS=$(kubectl -n "$NAMESPACE" get secret feature-toggles -o jsonpath='{.data.FEATURE_ALLOW_SOCIAL_VISITORS_PRISONS}' | base64 -d)
  FEATURE_DPS_ENABLED_PRISONS=$(kubectl -n "$NAMESPACE" get secret feature-toggles -o jsonpath='{.data.FEATURE_DPS_ENABLED_PRISONS}' | base64 -d)

  clear
  echo "-------------------------------------------------------------------------------------"
  echo "Environment                 : $ENV"
  echo "Social visitors allowed in  : $FEATURE_ALLOW_SOCIAL_VISITORS_PRISONS"
  echo "DPS visits enabled in       : $FEATURE_DPS_ENABLED_PRISONS"
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

      9)  echo "Restarting services"
          restart_services "$ENV" "$NAMESPACE"
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
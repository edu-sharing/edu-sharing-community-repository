#!/bin/bash
set -e  # Exit script if any command fails

if [[ -n "$KUBERNETES_SERVICE_HOST" ]]; then
    echo "Running inside a Kubernetes pod"
    NAMESPACE=$(cat /var/run/secrets/kubernetes.io/serviceaccount/namespace)
    SERVICE_NAME="edusharing-repository-service-headless.$NAMESPACE.svc.cluster.local"
    REPO_COUNT=$(nslookup "$SERVICE_NAME" | grep "$SERVICE_NAME" | wc -l)
    if [ "$REPO_COUNT" -gt "1" ]; then
      echo "can not run update. more than one repository started"
      exit 1
    fi
else
    echo "Not running inside a Kubernetes pod"
fi


source ./bin/updates/scripts/helper/base.sh

VERSION=$(get_alfresco_version "$repository_database_user" "$repository_database_pass" "$repository_database_host" "$repository_database_port" "$repository_database_name")

SQL_FILE="./bin/updates/scripts/sql/postgresql-mnt24815.sql"

if [[ "$VERSION" == "23.4.0" ]]; then
  echo "alfresco version must be fixed:$VERSION"
  export PGPASSWORD="$DB_PASS"
  psql -U "$DB_USER" -d "$DB_NAME" -h "$DB_HOST"  -p "$DB_PORT" -f "$SQL_FILE"

  # Check if the command was successful
  if [ $? -eq 0 ]; then
      echo "SQL script executed successfully."
  else
      echo "Error executing SQL script." >&2
  fi

else
  echo "alfresco version must not be fixed:$VERSION"
fi

# success
exit 0

# error
#exit 1

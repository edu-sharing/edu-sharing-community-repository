#!/bin/bash
set -e  # Exit script if any command fails

u="1_alf23.4.0_crc"
source ./bin/updates/scripts/helper/base.sh

repository_database_host="${REPOSITORY_DATABASE_HOST:-repository-database}"
repository_database_name="${REPOSITORY_DATABASE_NAME:-repository}"
repository_database_pass="${REPOSITORY_DATABASE_PASS:-repository}"
repository_database_port="${REPOSITORY_DATABASE_PORT:-5432}"
repository_database_user="${REPOSITORY_DATABASE_USER:-repository}"

if [[ -n "$KUBERNETES_SERVICE_HOST" ]]; then
    log $u "Running inside a Kubernetes pod"
    NAMESPACE=$(cat /var/run/secrets/kubernetes.io/serviceaccount/namespace)
    SERVICE_NAME="edusharing-repository-service-headless.$NAMESPACE.svc.cluster.local"
    REPO_COUNT=$(nslookup "$SERVICE_NAME" | grep -A2 "$SERVICE_NAME" | grep "Address" | grep -v $(hostname -i) | wc -l)
    log $u "other repos active: $REPO_COUNT"
    # on startup dns would not recognize current repo so we check > 0 instead of one
    if [ "$REPO_COUNT" -gt "0" ]; then
      log $u "can not run update. more than one repository started"
      exit 1
    fi
else
    log $u "Not running inside a Kubernetes pod"
fi

if check_table_exists "$repository_database_user" "$repository_database_pass" "$repository_database_host" "$repository_database_port" "$repository_database_name" "alf_node_properties"; then
    log $u "alfresco database scheme exists. Proceed with the next steps."
else
    log $u "Initial alfresco install. skipping update"
    exit 0
fi


VERSION=$(get_alfresco_version "$repository_database_user" "$repository_database_pass" "$repository_database_host" "$repository_database_port" "$repository_database_name")

if [[ $VERSION =~ ^[0-9]+\.[0-9]+\.[0-9]+$ ]]; then
    log $u "Version is valid $VERSION"
else
    log $u "Invalid version format: $VERSION"
    exit 1
fi

SQL_FILE="./bin/updates/scripts/sql/postgresql-mnt24815.sql"

if [[ "$VERSION" == "23.4.0" ]]; then
  log $u "Version must be fixed:$VERSION"
  export PGPASSWORD="$repository_database_pass"
  psql -U "$repository_database_user" -d "$repository_database_name" -h "$repository_database_host"  -p "$repository_database_port" -f "$SQL_FILE"

  # Check if the command was successful
  if [ $? -eq 0 ]; then
      log $u "SQL script executed successfully."
  else
      log $u "Error executing SQL script."
      exit 1
  fi

else
  log $u "Version must not be fixed:$VERSION"
fi

# success
exit 0

# error
#exit 1

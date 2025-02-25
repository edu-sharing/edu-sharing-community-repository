#!/bin/bash
set -e  # Exit script if any command fails

# Function to build the SQL query for version selection
get_select_version_number() {
    local VERSION_PARAM="$1"  # Get function parameter
    local NODE_ID="$2"
    local SELECT_SYS_NS_ID="$3"

    # Constructing the query
    local rs="SELECT string_value FROM alf_node_properties
                                 WHERE node_id=$NODE_ID
                                 AND qname_id=(SELECT id FROM alf_qname
                                               WHERE local_name='$VERSION_PARAM'
                                               AND ns_id=($SELECT_SYS_NS_ID)"
    echo "$rs"
}

# Function to get DB credentials and version information
get_alfresco_version() {
    # Read the database credentials from the properties file
    local DB_USER=$1
    local DB_PASS=$2
    local DB_HOST=$3
    local DB_PORT=$4
    local DB_NAME=$5

    # SQL Queries for getting version
    local SELECT_SYS_NS_ID="select id from alf_namespace where uri='http://www.alfresco.org/model/system/1.0')"
    local SELECT_SYS_NODE_ID="select node_id from alf_node_properties where qname_id=(select id from alf_qname where local_name='name' and ns_id=($SELECT_SYS_NS_ID) and string_value='Main Repository'"

    # Set the database password for psql
    export PGPASSWORD="$DB_PASS"

    if ! psql -U "$DB_USER" -d "$DB_NAME" -h "$DB_HOST" -p "$DB_PORT" -c "SELECT 1" >/dev/null 2>&1; then
        echo "Error: Unable to connect to the database. Please check your credentials and connection details." >&2
        exit 1
    fi

    # Get the SYS_NODE_ID
    local SYS_NODE_ID=$(psql -U "$DB_USER" -d "$DB_NAME" -h "$DB_HOST" -p "$DB_PORT" -t -A -c "$SELECT_SYS_NODE_ID" 2>/dev/null)
    if [[ $? -ne 0 || -z "$SYS_NODE_ID" ]]; then
        echo "Error: Failed to retrieve SYS_NODE_ID." >&2
        exit 1
    fi


    # Get version parts (Major, Minor, Patch)
    local SQL_QUERY=$(get_select_version_number "versionMajor" "$SYS_NODE_ID" "$SELECT_SYS_NS_ID")
    local MAJOR_VERSION=$(psql -U "$DB_USER" -d "$DB_NAME" -h "$DB_HOST" -p "$DB_PORT" -t -A -c "$SQL_QUERY" 2>/dev/null)
    if [[ $? -ne 0 || -z "$MAJOR_VERSION" ]]; then
        echo "Error: Failed to retrieve $MAJOR_VERSION." >&2
        exit 1
    fi

    SQL_QUERY=$(get_select_version_number "versionMinor" "$SYS_NODE_ID" "$SELECT_SYS_NS_ID")
    local MINOR_VERSION=$(psql -U "$DB_USER" -d "$DB_NAME" -h "$DB_HOST" -p "$DB_PORT" -t -A -c "$SQL_QUERY" 2>/dev/null)
    if [[ $? -ne 0 || -z "$MINOR_VERSION" ]]; then
        echo "Error: Failed to retrieve $MINOR_VERSION." >&2
        exit 1
    fi

    SQL_QUERY=$(get_select_version_number "versionRevision" "$SYS_NODE_ID" "$SELECT_SYS_NS_ID")
    local PATCH_VERSION=$(psql -U "$DB_USER" -d "$DB_NAME" -h "$DB_HOST" -p "$DB_PORT" -t -A -c "$SQL_QUERY" 2>/dev/null)
    if [[ $? -ne 0 || -z "$PATCH_VERSION" ]]; then
        echo "Error: Failed to retrieve $PATCH_VERSION." >&2
        exit 1
    fi

    # Output the full version
    echo "$MAJOR_VERSION.$MINOR_VERSION.$PATCH_VERSION"
}

#!/bin/bash

# Function to check if a table exists in a PostgreSQL database
check_table_exists() {
    local DB_USER="$1"
    local DB_PASS="$2"
    local DB_HOST="$3"
    local DB_PORT="$4"
    local DB_NAME="$5"
    local TABLE_NAME="$6"

    # Set the database password for psql
    export PGPASSWORD="$DB_PASS"


    # Option 3: Using \d command
    psql -U "$DB_USER" -d "$DB_NAME" -h "$DB_HOST" -p "$DB_PORT" -c "\d $TABLE_NAME" 2>/dev/null
    if [ $? -eq 0 ]; then
        return 0
    fi

    return 1
}
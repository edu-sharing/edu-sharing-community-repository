#!/bin/bash
set -e  # Exit script if any command fails

SCRIPT_DIR="./bin/updates/scripts"  # Change this to your folder path

mkdir -p /opt/alfresco/alf_data/scripts_processed

# Loop through scripts sorted by name
for script in $(ls "$SCRIPT_DIR"/*.sh | sort); do
    script_name=$(basename "$script" .sh)  # Extract only the filename without extension
    done_file="/opt/alfresco/alf_data/scripts_processed/${script_name}.done"  # Corrected done file path

    if [ ! -f "$done_file" ]; then
        echo "Running: $script"
        if bash "$script"; then
                echo "Success executing $script"
                touch "$done_file"  # Mark as done only if script runs successfully
        else
                echo "Error executing $script" >&2  # Print error message to stderr
                exit 1
        fi

    else
        echo "Skipping: $script (already done)"
    fi
done
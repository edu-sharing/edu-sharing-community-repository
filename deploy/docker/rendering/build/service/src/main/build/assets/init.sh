#!/bin/bash
set -eux

########################################################################################################################

my_host_internal="${RENDERING_SERVICE_HOST_INTERNAL:-rendering-service}"
my_port_internal="${RENDERING_SERVICE_PORT_INTERNAL:-8080}"

my_base_internal="http://${my_host_internal}:${my_port_internal}/esrender"
my_meta_internal="${my_base_internal}/application/esmain/metadata.php"

repository_service_host="${REPOSITORY_SERVICE_HOST:-repository-service}"
repository_service_port="${REPOSITORY_SERVICE_PORT:-8080}"

repository_service_base="http://${repository_service_host}:${repository_service_port}/edu-sharing"

repository_service_admin_pass="${REPOSITORY_SERVICE_ADMIN_PASS:-admin}"

### Wait ###############################################################################################################

until wait-for-it "${my_host_internal}:${my_port_internal}" -t 3; do sleep 1; done

until [[ $( curl -sSf -w "%{http_code}\n" -o /dev/null "${my_base_internal}/admin/" ) -eq 200 ]]
do
	echo >&2 "Waiting for ${my_host_internal} ..."
	sleep 3
done

########################################################################################################################

my_appid=$( \
	curl -sS "${my_meta_internal}" | xmlstarlet sel -t -v '/properties/entry[@key="appid"]' - | xargs echo \
)

access_token=$( \
	curl -sS \
		-XPOST \
		-d "grant_type=password&client_id=eduApp&client_secret=secret&username=admin&password=${repository_service_admin_pass}" \
		"${repository_service_base}/oauth2/token" | jq -r '.access_token' \
)

has_my_appid=$( \
	curl -sS \
		-H "Authorization: Bearer ${access_token}" \
		-H "Accept: application/json" \
		"${repository_service_base}/rest/admin/v1/applications" | jq -r '.[] | select(.id == "'"${my_appid}"'") | .id' \
)

if [ -n "${has_my_appid}" ]
then
	curl -sS \
		-H "Authorization: Bearer ${access_token}" \
		-H "Accept: application/json" \
		-XDELETE \
		"${repository_service_base}/rest/admin/v1/applications/${my_appid}"
fi

curl -sS \
	-H "Authorization: Bearer ${access_token}" \
	-H "Accept: application/json" \
	-XPUT \
	"${repository_service_base}/rest/admin/v1/applications?url=$( jq -nr --arg v "${my_meta_internal}" '$v|@uri' )"

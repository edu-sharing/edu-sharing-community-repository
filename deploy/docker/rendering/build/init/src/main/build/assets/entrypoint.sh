#!/bin/bash
set -eux

########################################################################################################################

rendering_service_host="${RENDERING_SERVICE_HOST:-rendering-service}"
rendering_service_port="${RENDERING_SERVICE_PORT:-8080}"
rendering_service_prot="${RENDERING_SERVICE_PROT:-"http"}"

rendering_service_base="${rendering_service_prot}://${rendering_service_host}:${rendering_service_port}/esrender"
rendering_service_meta="${rendering_service_base}/application/esmain/metadata.php"

repository_service_host="${REPOSITORY_SERVICE_HOST:-repository-service}"
repository_service_port="${REPOSITORY_SERVICE_PORT:-8080}"
repository_service_prot="${REPOSITORY_SERVICE_PROT:-"http"}"

repository_service_root_name="admin"
repository_service_root_pass="${REPOSITORY_SERVICE_ROOT_PASS:-admin}"

repository_service_base="${repository_service_prot}://${repository_service_host}:${repository_service_port}/edu-sharing"

### Wait ###############################################################################################################

until wait-for-it "${repository_service_host}:${repository_service_port}" -t 3; do sleep 1; done

until [[ $( curl -sSf -w "%{http_code}\n" -o /dev/null "${repository_service_base}/rest/_about/status/SERVICE?timeoutSeconds=3" ) -eq 200 ]]
do
	echo >&2 "Waiting for repository-service  ..."
	sleep 3
done

until wait-for-it "${rendering_service_host}:${rendering_service_port}" -t 3; do sleep 1; done

until [[ $( curl -sSf -w "%{http_code}\n" -o /dev/null "${rendering_service_base}/admin/" ) -eq 200 ]]
do
	echo >&2 "Waiting for rendering-service  ..."
	sleep 3
done

########################################################################################################################

my_appid=$( \
	curl -sS "${rendering_service_meta}" | xmlstarlet sel -t -v '/properties/entry[@key="appid"]' - | xargs echo \
)

access_token=$( \
	curl -sS \
		-XPOST \
		-d "grant_type=password&client_id=eduApp&client_secret=secret&username=${repository_service_root_name}&password=${repository_service_root_pass}" \
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
	"${repository_service_base}/rest/admin/v1/applications?url=$( jq -nr --arg v "${rendering_service_meta}" '$v|@uri' )"

#!/bin/bash
[[ -n $DEBUG ]] && set -x
set -eu

########################################################################################################################

my_host_internal="${SERVICES_RENDERING_SERVICE_HOST_INTERNAL:-services-rendering-service}"
my_port_internal="${SERVICES_RENDERING_SERVICE_PORT_INTERNAL:-8080}"

my_base_internal="http://${my_host_internal}:${my_port_internal}/esrender"
my_meta_internal="${my_base_internal}/application/esmain/metadata.php"

repository_service_host="${REPOSITORY_SERVICE_HOST:-repository-service}"
repository_service_port="${REPOSITORY_SERVICE_PORT:-8080}"

repository_service_base="http://${repository_service_host}:${repository_service_port}/edu-sharing"

repository_service_admin_user="admin"
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

has_my_appid=$( \
	curl -sS \
		-H "Accept: application/json" \
		--user "${repository_service_admin_user}:${repository_service_admin_pass}" \
		"${repository_service_base}/rest/admin/v1/applications" | jq -r '.[] | select(.id == "'"${my_appid}"'") | .id' \
)

if [ -n "${has_my_appid}" ]
then
	curl -sS \
		-H "Accept: application/json" \
		--user "${repository_service_admin_user}:${repository_service_admin_pass}" \
		-XDELETE \
		"${repository_service_base}/rest/admin/v1/applications/${my_appid}"
fi

curl -sS \
  -H "Accept: application/json" \
  --user "${repository_service_admin_user}:${repository_service_admin_pass}" \
  -XPUT \
  "${repository_service_base}/rest/admin/v1/applications?url=$( jq -nr --arg v "${my_meta_internal}" '$v|@uri' )"


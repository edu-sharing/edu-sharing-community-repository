#!/bin/bash
set -eux

########################################################################################################################

my_bind="${RENDERING_SERVICE_BIND:-"0.0.0.0"}"

my_prot_external="${RENDERING_SERVICE_PROT_EXTERNAL:-http}"
my_host_external="${RENDERING_SERVICE_HOST_EXTERNAL:-rendering.127.0.0.1.nip.io}"
my_port_external="${RENDERING_SERVICE_PORT_EXTERNAL:-9100}"
my_path_external="${RENDERING_SERVICE_PATH_EXTERNAL:-/esrender}"
my_base_external="${my_prot_external}://${my_host_external}:${my_port_external}${my_path_external}"

my_host_internal="${RENDERING_SERVICE_HOST_INTERNAL:-rendering-service}"
my_port_internal="${RENDERING_SERVICE_PORT_INTERNAL:-8080}"
my_base_internal="http://${my_host_internal}:${my_port_internal}/esrender"

rendering_cache_host="${RENDERING_CACHE_HOST:-rendering-cache}"
rendering_cache_port="${RENDERING_CACHE_PORT:-6379}"
rendering_cache_prot="${RENDERING_CACHE_PROT:-tcp://}"
rendering_cache_opts="${RENDERING_CACHE_OPTS:-}"
rendering_cache_type="${RENDERING_CACHE_TYPE:-redis}"

rendering_database_driv="${RENDERING_DATABASE_DRIV:-"mysql"}"
rendering_database_host="${RENDERING_DATABASE_HOST:-rendering-database}"
rendering_database_name="${RENDERING_DATABASE_NAME:-rendering}"
rendering_database_pass="${RENDERING_DATABASE_PASS:-rendering}"
rendering_database_port="${RENDERING_DATABASE_PORT:-3306}"
rendering_database_user="${RENDERING_DATABASE_USER:-rendering}"

repository_service_host="${REPOSITORY_SERVICE_HOST:-repository-service}"
repository_service_port="${REPOSITORY_SERVICE_PORT:-8080}"

repository_service_base="http://${repository_service_host}:${repository_service_port}/edu-sharing"

### Wait ###############################################################################################################

until wait-for-it "${rendering_cache_host}:${rendering_cache_port}" -t 3; do sleep 1; done

[[ "${rendering_cache_type}" == "rediscluster" ]] && {
	until [[ $(redis-cli --cluster info "${rendering_cache_host}" "${rendering_cache_port}" | grep '[OK]' | cut -d ' ' -f5) -gt 1 ]]; do
		echo "."
		sleep 2
	done
}

until wait-for-it "${rendering_database_host}:${rendering_database_port}" -t 3; do sleep 1; done

[[ "${rendering_database_driv}" == "mysql" ]] && {
	until mysql -h"${rendering_database_host}" -P"${rendering_database_port}" \
		-u"${rendering_database_user}" -p"${rendering_database_pass}" \
		"${rendering_database_name}" <<<'SELECT 1' &>/dev/null; do
		echo >&2 "Waiting for ${rendering_database_host} ..."
		sleep 3
	done
}

[[ "${rendering_database_driv}" == "pgsql" ]] && {
	until PGPASSWORD="${rendering_database_pass}" \
		psql -h "${rendering_database_host}" -p "${rendering_database_port}" -U "${rendering_database_user}" -d "${rendering_database_name}" -c '\q'; do
		echo >&2 "Waiting for ${rendering_database_host} ..."
		sleep 3
	done
}

until wait-for-it "${repository_service_host}:${repository_service_port}" -t 3; do sleep 1; done

until [[ $(curl -sSf -w "%{http_code}\n" -o /dev/null "${repository_service_base}/rest/_about/status/SERVICE?timeoutSeconds=3") -eq 200 ]]; do
	echo >&2 "Waiting for ${repository_service_host} service ..."
	sleep 3
done

until [[ $(curl -sSf -w "%{http_code}\n" -o /dev/null "${repository_service_base}/rest/_about/status/SEARCH?timeoutSeconds=3") -eq 200 ]]; do
	echo >&2 "Waiting for ${repository_service_host} search ..."
	sleep 3
done

########################################################################################################################

sed -i 's|^Listen \([0-9]+\)|Listen '"${my_bind}"':\1|g' /etc/apache2/ports.conf

sed -i 's|^\(\s*\)[#]*ServerName.*|\1ServerName '"${my_host_external}"'|' /etc/apache2/sites-available/external.conf
sed -i 's|^\(\s*\)[#]*ServerName.*|\1ServerName '"${my_host_internal}"'|' /etc/apache2/sites-available/internal.conf

sed -i 's|^[;\s]*session\.save_handler.*|session.save_handler = '"${rendering_cache_type}"'|' "${PHP_INI_DIR}/php.ini"
echo "session.save_path = \"${rendering_cache_prot}${rendering_cache_host}:${rendering_cache_port}${rendering_cache_opts}\"" >>"${PHP_INI_DIR}/php.ini"

########################################################################################################################

if mkdir "${RS_CACHE}/config" >/dev/null 2>&1; then

	touch "${RS_CACHE}/config.lock"

	echo "install started."

	mkdir -p "${RS_CACHE}/data"
	chown www-data:www-data "${RS_CACHE}/data"

	cat >/tmp/config.ini <<-EOF
		[application]
		; url for client requests (accessible from the internet)
		application_url_client=${my_base_external}
		; url for requests from repository
		application_url_repository=${my_base_internal}
		; ip of the server
		application_host=
		; root directory of the rendering service application
		application_root=${RS_ROOT}
		; cache directory
		application_cache=${RS_CACHE}/data
		; save cache directory (optional)
		application_cache_save=
		; path to the ffmpeg binary
		application_ffmpeg=/usr/bin/ffmpeg
		
		[database]
		; driver (mysql or pgsql)
		db_driver=${rendering_database_driv}
		; db host
		db_host=${rendering_database_host}
		; db port
		db_port=${rendering_database_port}
		; db name
		db_name=${rendering_database_name}
		; db user
		db_user=${rendering_database_user}
		; db password
		db_password=${rendering_database_pass}
		
		[repository]
		; url of the repository to fetch properties and content from
		repository_url=${repository_service_base}
	EOF

	before="$(mktemp)"

	php admin/cli/install.php -c /tmp/config.ini
	rm -f /tmp/config.ini

	echo "config saving."

	find -L . -type d -exec mkdir -p "${RS_CACHE}/config/{}" \;
	find -L . -type f -newer "${before}" -exec cp {} "${RS_CACHE}/config/{}" \;
	find "${RS_CACHE}/config" -type d -empty -delete

	rm "${RS_CACHE}/config.lock"

	echo "config saved."

else

	echo "install skipped."

	sleep 2
	while [ -e "${RS_CACHE}/config.lock" ]; do
		echo .
		sleep 2
	done

	echo "config restoring."

	pushd "${RS_CACHE}/config"

	find . -type d -exec mkdir -p "${RS_ROOT}/{}" \;
	find . -type f -exec cp -f {} "${RS_ROOT}/{}" \;

	popd

	echo "config restored."
fi

########################################################################################################################

yes | php admin/cli/update.php

chown -R www-data:www-data "${RS_ROOT}"

########################################################################################################################

exec "$@"

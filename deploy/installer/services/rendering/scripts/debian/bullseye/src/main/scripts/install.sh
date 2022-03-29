#!/bin/bash
set -e
set -o pipefail

########################################################################################################################

source ./artifact.sh

########################################################################################################################

if [[ -z $WWW_ROOT ]] ; then
	echo ""
	echo "Env WWW_ROOT not defined! It must point to your website document root!"
	exit 1
fi


if [[ -z $RS_ROOT ]] ; then
	echo ""
	echo "Env RS_ROOT not defined! It must point inside \"$(basename "${RS_ROOT}")\" of your website document root!"
	exit 1
fi


if [[ -z $RS_CACHE ]] ; then
	echo ""
	echo "Env RS_CACHE not defined! It must point outside of your website document root!"
	exit 1
fi

# TODO check if apache is running!
# TODO check if postgresql is running!

########################################################################################################################

execution_folder="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

pushd "$execution_folder" &> /dev/null
if [[ -f ".env.base" ]] ; then
	echo "Load .env.base"
	source .env.base &> /dev/null
fi
popd

########################################################################################################################

usage() {
	echo "Options:"
	echo ""

	echo "-?"
	echo "--help"
	echo "  Display available options"
	echo ""

	echo "-f = environment file"
	echo "--file"
	echo "  Loads the configuration from the specified environment file"
	echo ""

	echo "--local"
	echo "  Use local maven cache for installation"
}

########################################################################################################################

use_local_maven_cache=0

while true; do
	flag="$1"
	shift || break

	case "$flag" in
			--help|'-?') usage && exit 0 ;;
			--file|-f) source "$1" && shift	;;
			--local) use_local_maven_cache=1 ;;
			*) {
				echo "error: unknown flag: $flag"
				usage
			}  >&2
			exit 1 ;;
	esac
done

########################################################################################################################

my_home_appid="${SERVICES_RENDERING_SERVICE_HOME_APPID:-esrender}"

my_prot_external="${SERVICES_RENDERING_SERVICE_PROT_EXTERNAL:-http}"
my_host_external="${SERVICES_RENDERING_SERVICE_HOST_EXTERNAL:-localhost}"
my_port_external="${SERVICES_RENDERING_SERVICE_PORT_EXTERNAL:-80}"
my_path_external="${SERVICES_RENDERING_SERVICE_PATH_EXTERNAL:-$(basename "${RS_ROOT}")}"

my_prot_internal="${SERVICES_RENDERING_SERVICE_PROT_INTERNAL:-http}"
my_host_internal="${SERVICES_RENDERING_SERVICE_HOST_INTERNAL:-127.0.0.1}"
my_port_internal="${SERVICES_RENDERING_SERVICE_PORT_INTERNAL:-80}"
my_path_internal="${SERVICES_RENDERING_SERVICE_PATH_INTERNAL:-$(basename "${RS_ROOT}")}"

my_internal_url="${my_prot_internal}://${my_host_internal}:${my_port_internal}/${my_path_internal}"
my_external_url="${my_prot_external}://${my_host_external}:${my_port_external}/${my_path_external}"

my_cache_cleaner_interval="${SERVICES_RENDERING_SERVICE_CACHE_CLEANER_INTERVAL:-"10 * * * *"}"

db_driver=pgsql
db_host="${SERVICES_RENDERING_DATABASE_HOST:-127.0.0.1}"
db_port="${SERVICES_RENDERING_DATABASE_PORT:-5432}"
db_name="${SERVICES_RENDERING_DATABASE_NAME:-rendering}"
db_user="${SERVICES_RENDERING_DATABASE_USER:-rendering}"
db_password="${SERVICES_RENDERING_DATABASE_PASS:-rendering}"

repository_service_host="${REPOSITORY_SERVICE_HOST:-}"
repository_service_port="${REPOSITORY_SERVICE_PORT:-80}"

if [[ -n $repository_service_host ]] ; then
	repository_service_base="http://${repository_service_host}:${repository_service_port}/edu-sharing"
fi

repository_user="admin"
repository_password="${REPOSITORY_SERVICE_ADMIN_PASS:-admin}"

rendering_proxy_host="${SERVICES_RENDERING_SERVICE_PROXY_HOST:-}"
rendering_proxy_port="${SERVICES_RENDERING_SERVICE_PROXY_PORT:-}"
rendering_proxy_user="${SERVICES_RENDERING_SERVICE_PROXY_USER:-}"
rendering_proxy_pass="${SERVICES_RENDERING_SERVICE_PROXY_PASS:-}"

########################################################################################################################

until wait-for-it "${db_host}:${db_port}" -t 3; do sleep 1; done

if [[ "${db_driver}" == "pgsql" ]] ; then
	until PGPASSWORD="${db_password}" psql -h "${db_host}" -p "${db_port}" -U "${db_user}" -d "${db_name}" -c '\q'; do
		echo >&2 "Waiting for database postgresql ${db_host}:${db_port} ..."
		sleep 3
	done
fi

if [[ -n $repository_service_base ]] ; then
	until wait-for-it "${repository_service_host}:${repository_service_port}" -t 3; do sleep 1; done

	until [[ $(curl -sSf -w "%{http_code}\n" -o /dev/null -H 'Accept: application/json' "${repository_service_base}/rest/_about/status/SERVICE?timeoutSeconds=3") -eq 200 ]]; do
		echo >&2 "Waiting for repository ${repository_service_host} service ..."
		sleep 3
	done

	until [[ $(curl -sSf -w "%{http_code}\n" -o /dev/null -H 'Accept: application/json' "${repository_service_base}/rest/_about/status/SEARCH?timeoutSeconds=3") -eq 200 ]]; do
		echo >&2 "Waiting for repository ${repository_service_host} search ..."
		sleep 3
	done
fi

########################################################################################################################

info() {
	echo ""
	echo "#########################################################################"
	echo ""
	echo "rendering-database:"
	echo ""
	echo "  Database:"
	echo ""
	echo "    Host:         ${db_host}"
	echo "    Port:         ${db_port}"
	echo "    Name:         ${db_name}"
	echo ""
	echo "  Credentials:"
	echo ""
	echo "     User:        ${db_user}"
	echo "     Password:    ${db_password}"
	echo ""
	echo "#########################################################################"
	echo ""
	echo "rendering-service:"
	echo ""
	echo "  Common:"
	echo ""
	echo "    AppId:        ${my_home_appid}"
	echo ""
	echo "  Public:"
	echo ""
	echo "    Protocol:     ${my_prot_external}"
	echo "    Host:         ${my_host_external}"
	echo "    Port:         ${my_port_external}"
	echo "    Path:         ${my_path_external}"
	echo ""
  echo "  Private:"
  echo ""
  echo "    Protocol:     ${my_prot_internal}"
  echo "    Host:         ${my_host_internal}"
  echo "    Port:         ${my_port_internal}"
  echo "    Path:         ${my_path_internal}"
  echo ""
  echo "  Services:"
  echo ""
  echo "    Public:       ${my_external_url}"
  echo "    Private:      ${my_internal_url}"
  echo ""

	if [[ -n $rendering_proxy_host ]] ; then
	echo "#########################################################################"
  echo ""
  echo "proxy:"
  echo ""
  echo "  Host:           ${rendering_proxy_host}"
  echo "  Port:           ${rendering_proxy_port}"
  echo "  User:           ${rendering_proxy_user}"
  echo "  Password:       ${rendering_proxy_pass}"
  echo ""
	fi

	if [[ -n $repository_service_base ]] ; then
  echo "#########################################################################"
  echo ""
  echo "repository-service:"
  echo ""
  echo "  URL:            ${repository_service_base}"
  echo "  User:           ${repository_user}"
  echo "  Password:       ${repository_password}"
  echo ""
  fi

  echo "#########################################################################"
  echo ""
  echo ""
}

########################################################################################################################

install_edu_sharing() {

	echo "- reset rendering server"
	rm -rf "${RS_ROOT}"

	echo "- unpack edu-sharing rendering-service distribution"
	tar xzf "${ARTIFACT_ID}-${ARTIFACT_VERSION}-bin.tar.gz" --exclude './vendor/lib/converter'
	rm "${ARTIFACT_ID}-${ARTIFACT_VERSION}-bin.tar.gz"

}

########################################################################################################################

pushd "${WWW_ROOT}" &> /dev/null

if [[ use_local_maven_cache -eq 1 ]] ; then
	echo "- WARNING: local maven cache is being used"
else
	echo "- download edu-sharing rendering-service distribution"

	mvn -q dependency:get \
			-Dartifact="org.edu_sharing:${ARTIFACT_ID}:${ARTIFACT_VERSION}:tar.gz:bin" \
			-DremoteRepositories=edusharing-remote::::https://artifacts.edu-sharing.com/repository/maven-remote/ \
			-Dtransitive=false
fi

echo "- copy edu-sharing rendering-service distribution"

mvn -q -llr dependency:copy \
		-Dartifact="org.edu_sharing:${ARTIFACT_ID}:${ARTIFACT_VERSION}:tar.gz:bin" \
		-DoutputDirectory=.

if [[ ! -d "${RS_CACHE}" ]] ; then

  mkdir -p "${RS_CACHE}"
  chown -R www-data:www-data "${RS_CACHE}"

	echo ""
	echo "Install ... "

	install_edu_sharing || exit 1

  cat >/tmp/config.ini <<-EOF
		[application]
		; url for client requests (accessible from the internet)
		application_url_client=${my_external_url}
		; url for requests from repository
		application_url_repository=${my_internal_url}
		; ip of the server
		application_host=
		; root directory of the rendering service application
		application_root=${RS_ROOT}
		; cache directory
		application_cache=${RS_CACHE}
		; save cache directory (optional)
		application_cache_save=
		; path to the ffmpeg binary
		application_ffmpeg=/usr/bin/ffmpeg

		[database]
		; driver (mysql or pgsql)
		db_driver=${db_driver}
		; db host
		db_host=${db_host}
		; db port
		db_port=${db_port}
		; db name
		db_name=${db_name}
		; db user
		db_user=${db_user}
		; db password
		db_password=${db_password}

		[repository]
		; url of the repository to fetch properties and content from
		repository_url=${repository_service_base}
	EOF

  cat /tmp/config.ini

	echo "- call ${RS_ROOT}/admin/cli/install.php"
  php "${RS_ROOT}"/admin/cli/install.php -c /tmp/config.ini || {
		echo "- rollback"
  	result=$?;
  	rm -rf "${RS_CACHE}" 2> /dev/null
  	rm -rf "${RS_ROOT}" 2> /dev/null
  	rm -f /tmp/config.ini 2> /dev/null
  	exit $result
  }

  rm -f /tmp/config.ini

	if [[ -n $repository_service_base ]] && [[ -n $repository_user ]] && [[ -n $repository_password ]] ; then

		# TODO: check Apache has to be running to provide own metadata

		until [[ $( curl -sSf -w "%{http_code}\n" -o /dev/null "${my_internal_url}/admin/" ) -eq 200 ]]
		do
			echo >&2 "Waiting for ${my_internal_url} ..."
			sleep 3
		done

		my_meta_internal="${my_internal_url}/application/esmain/metadata.php"
		my_appid=$( \
			curl -sS "${my_meta_internal}" | xmlstarlet sel -t -v '/properties/entry[@key="appid"]' - | xargs echo \
		)

		has_my_appid=$( \
			curl -sS \
				-H "Accept: application/json" \
				--user "${repository_user}:${repository_password}" \
				"${repository_service_base}/rest/admin/v1/applications" | jq -r '.[] | select(.id == "'"${my_appid}"'") | .id' \
		)

		if [[ -z "${has_my_appid}" ]] ; then
			echo "- register service with ${repository_service_base}"
			curl -sS \
				-H "Accept: application/json" \
				--user "${repository_user}:${repository_password}" \
				-XPUT \
				"${repository_service_base}/rest/admin/v1/applications?url=$( jq -nr --arg v "${my_meta_internal}" '$v|@uri' )"
		fi

	fi

else

	echo ""
	echo "Update ... "

	echo "- make a snapshot of the rendering service"
  snapshot_name="$execution_folder/snapshots/edu-sharing-SNAPSHOT-$(date "+%Y.%m.%d-%H.%M.%S").tar.gz"
	mkdir -p "$(dirname "$snapshot_name")"
	tar -czf "$snapshot_name" $(basename "${RS_ROOT}")

	install_edu_sharing || exit 1

  echo "- restore config"
  tar -zxf "$snapshot_name" $(basename "${RS_ROOT}")/conf
  tar -zxf "$snapshot_name" --wildcards "*config.php" -C $(basename "${RS_ROOT}")

	echo "- call ${RS_ROOT}/admin/cli/update.php"
	yes | php "${RS_ROOT}"/admin/cli/update.php || true

	echo "- prune snapshots (keep 3)"
	pushd "$execution_folder"/snapshots &> /dev/null
	ls -pt | grep -v / | grep "edu-sharing-SNAPSHOT" | tail -n +4 | xargs -I {} rm {}
	popd

fi

proxyConf="${RS_ROOT}/conf/proxy.conf.php"
if [[ -n $rendering_proxy_host ]] ; then
	echo "- update $proxyConf"
	cp -f "${RS_ROOT}/conf/proxy.conf.php.example" "${proxyConf}"
	sed -i -r "s|define\('HTTP_PROXY_HOST',.*);|define('HTTP_PROXY_HOST', '$rendering_proxy_host');|" "${proxyConf}"
	sed -i -r "s|define\('HTTP_PROXY_PORT',.*);|define('HTTP_PROXY_PORT', '$rendering_proxy_port');|" "${proxyConf}"
	sed -i -r "s|define\('HTTP_PROXY_USER',.*);|define('HTTP_PROXY_USER', '$rendering_proxy_user');|" "${proxyConf}"
	sed -i -r "s|define\('HTTP_PROXY_PASS',.*);|define('HTTP_PROXY_PASS', '$rendering_proxy_pass');|" "${proxyConf}"
else
	echo "- remove $proxyConf"
	rm -f "${proxyConf}"
fi

dbConf="${RS_ROOT}/conf/db.conf.php"
echo "- update ${dbConf}"
sed -i -r "s|\$dsn.*|\$dsn = \"${db_driver}:host=${db_host};port=${db_port};dbname=${db_name}\";|" "${dbConf}"
sed -i -r "s|\$dbuser.*|\$dbuser = \"${db_user}\";|" "${dbConf}"
sed -i -r "s|\$pwd.*|\pwd = \"${db_password}\";|" "${dbConf}"

systemConf="${RS_ROOT}/conf/system.conf.php"
echo "- update ${systemConf}"
sed -i -r "s|\$MC_URL.*|\$MC_URL = '${my_external_url}';|" "${systemConf}"
sed -i -r "s|\$MC_DOCROOT.*|\$MC_DOCROOT = '${RS_ROOT}';|" "${systemConf}"
sed -i -r "s|\$CC_RENDER_PATH.*|\$CC_RENDER_PATH = '${RS_CACHE}';|" "${systemConf}"

homeApp="${RS_ROOT}/conf/esmain/homeApplication.properties.xml"
echo "- update ${homeApp}"
xmlstarlet ed -L \
	-u '/properties/entry[@key="scheme"]' -v "${my_prot_internal}" \
	-u '/properties/entry[@key="host"]' -v "${my_host_internal}" \
	-u '/properties/entry[@key="port"]' -v "${my_port_internal}" \
	-u '/properties/entry[@key="appid"]' -v "${my_home_appid}" \
	"${homeApp}"

echo "- set cache cleaner cronjob"
croneJob=/tmp/mycron
crontab -l > "${croneJob}" 2> /dev/null || touch "${croneJob}"
sed -i -r 's|^[#]*.*Rendering-Service cache cleaner|'"${my_cache_cleaner_interval} ${RS_CACHE} www-data php /func/classes.new/Helper/cacheCleaner.php # Rendering-Service cache cleaner"'|' "${croneJob}"
grep -q '^[#]*.*Rendering-Service cache cleaner' "${croneJob}" || echo "${my_cache_cleaner_interval} ${RS_CACHE} www-data php /func/classes.new/Helper/cacheCleaner.php # Rendering-Service cache cleaner" >>"${croneJob}"
crontab "${croneJob}"
rm -f  "${croneJob}"

popd

echo "- set permission ${RS_ROOT}"
chown -R www-data:www-data "${RS_ROOT}"

info >> "$execution_folder/install_log-$(date "+%Y.%m.%d-%H.%M.%S").txt"
info

echo "- Please check logging configuration:"
echo "    ${RS_ROOT}/conf/de.metaventis.esrender.cachecleaner.properties"
echo "    ${RS_ROOT}/conf/de.metaventis.esrender.log4php.properties"

echo "- done."
exit

########################################################################################################################

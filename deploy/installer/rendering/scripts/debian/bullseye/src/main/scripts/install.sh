#!/bin/bash
set -e
set -o pipefail


# TODO check if apache is stopped otherwise exit!
#[[ "$(apache2 status )" != "tomcat not running" ]] && {
#	echo ""
#	echo "Please stop Tomcat before you can run the installation!"
#	exit
#}

# TODO check if db is running otherwise exit!
#[[ "$(./alfresco.sh status postgresql)" != "postgresql not running" ]] && {
#	echo ""
#	echo "Please stop Postgresql before you can run the installation!"
#	exit
#}


# load the default configuration
if [[ -f ".env.base" ]] ; then
	source .env.base
fi


usage() {
	echo "Options:"
	echo ""

	echo "-f = environment file"
	echo "--file"
	echo "Loads the configuration from the specified environment file"
}

while true; do
	flag="$1"
	shift || break

	case "$flag" in
			--help|'-?') usage && exit 0 ;;
			--file|-f) source "$1" && shift	;;
			*) {
				echo "error: unknown flag: $flag"
				usage
			}  >&2
			exit 1 ;;
	esac
done

my_home_appid="${RENDERING_SERVICE_HOME_APPID:-esrender}" # Kundenprojekt ?

my_prot_external="${RENDERING_SERVICE_PROT_EXTERNAL:-http}"
my_host_external="${RENDERING_SERVICE_HOST_EXTERNAL:-localhost}"
my_port_external="${RENDERING_SERVICE_PORT_EXTERNAL:-80}"
my_path_external="${RENDERING_SERVICE_PATH_EXTERNAL:-$(basename "${RS_ROOT}")}"

my_prot_internal="${RENDERING_SERVICE_PROT_INTERNAL:-http}"
my_host_internal="${RENDERING_SERVICE_HOST_INTERNAL:-127.0.0.1}"
my_port_internal="${RENDERING_SERVICE_PORT_INTERNAL:-80}"
my_path_internal="${RENDERING_SERVICE_PATH_INTERNAL:-$(basename "${RS_ROOT}")}"

my_internal_url="${my_prot_internal}://${my_host_internal}:${my_port_internal}/${my_path_internal}"
my_external_url="${my_prot_external}://${my_host_external}:${my_port_external}/${my_path_external}"

my_cache_cleaner_interval="${RENDERING_SERVICE_CACHE_CLEANER_INTERVAL:-"10 * * * *"}"

db_driver=pgsql
db_host="${RENDERING_DATABASE_HOST:-127.0.0.1}"
db_port="${RENDERING_DATABASE_PORT:-5432}"
db_name="${RENDERING_DATABASE_NAME:-rendering}"
db_user="${RENDERING_DATABASE_USER:-rendering}"
db_password="${RENDERING_DATABASE_PASS:-rendering}"

repository_service_host="${REPOSITORY_SERVICE_HOST:-localhost}"
repository_service_port="${REPOSITORY_SERVICE_PORT:-80}"
repository_service_base="http://${repository_service_host}:${repository_service_port}/edu-sharing"

repository_user="admin"
repository_password="${REPOSITORY_SERVICE_ADMIN_PASS:-admin}"

rendering_proxy_host="${RENDERING_PROXY_HOST:-}"
rendering_proxy_port="${RENDERING_PROXY_PORT:-}"
rendering_proxy_user="${RENDERING_PROXY_USER:-}"
rendering_proxy_pass="${RENDERING_PROXY_PASS:-}"

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



install_edu_sharing() {

	echo "- download edu-sharing rendering-service distribution"
	mvn -q dependency:get \
			-Dartifact=org.edu_sharing:edu_sharing-community-deploy-installer-rendering-distribution:${org.edu_sharing:edu_sharing-community-deploy-installer-rendering-distribution:tar.gz:bin.version}:tar.gz:bin \
			-DremoteRepositories=myreleases::::https://artifacts.edu-sharing.com/repository/community-releases/,mysnapshots::::https://artifacts.edu-sharing.com/repository/community-snapshots/ \
			-Dtransitive=false

	echo "- unpack edu-sharing rendering-service distribution"
	mvn -q dependency:copy \
			-Dartifact=org.edu_sharing:edu_sharing-community-deploy-installer-rendering-distribution:${org.edu_sharing:edu_sharing-community-deploy-installer-rendering-distribution:tar.gz:bin.version}:tar.gz:bin \
			-DoutputDirectory=.

	tar xzf edu_sharing-community-deploy-installer-rendering-distribution-${org.edu_sharing:edu_sharing-community-deploy-installer-rendering-distribution:tar.gz:bin.version}-bin.tar.gz --exclude './vendor/lib/converter'
	rm edu_sharing-community-deploy-installer-rendering-distribution-${org.edu_sharing:edu_sharing-community-deploy-installer-rendering-distribution:tar.gz:bin.version}-bin.tar.gz

}

########################################################################################################################

until wait-for-it "${db_host}:${db_port}" -t 3; do sleep 1; done

[[ "${db_driv}" == "pgsql" ]] && {
	until PGPASSWORD="${db_pass}" psql -h "${db_host}" -p "${db_port}" -U "${db_user}" -d "${db_name}" -c '\q'; do
		echo >&2 "Waiting for database postgresql ${db_host}:${db_port} ..."
		sleep 3
	done
}

until wait-for-it "${repository_service_host}:${repository_service_port}" -t 3; do sleep 1; done

until [[ $(curl -sSf -w "%{http_code}\n" -o /dev/null -H 'Accept: application/json' "${repository_service_base}/rest/_about/status/SERVICE?timeoutSeconds=3") -eq 200 ]]; do
	echo >&2 "Waiting for repository ${repository_service_host} service ..."
	sleep 3
done

until [[ $(curl -sSf -w "%{http_code}\n" -o /dev/null -H 'Accept: application/json' "${repository_service_base}/rest/_about/status/SEARCH?timeoutSeconds=3") -eq 200 ]]; do
	echo >&2 "Waiting for repository ${repository_service_host} search ..."
	sleep 3
done

########################################################################################################################

pushd $WWW_ROOT

if [[ ! -d "${RS_CACHE}" ]] ; then

	echo ""
	echo "Install ... "

	install_edu_sharing

  mkdir -p "${RS_CACHE}"
  chown -R www-data:www-data "${RS_CACHE}"

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

  php "${RS_ROOT}"/admin/cli/install.php -c /tmp/config.ini || {
  	result=$?;
  	rm -rf "${RS_CACHE}" 2> /dev/null
  	rm -rf "${RS_ROOT}" 2> /dev/null
  	rm -f /tmp/config.ini 2> /dev/null
  	exit $result
  }


  rm -f /tmp/config.ini
	mv "${RS_ROOT}"/install/ "${RS_ROOT}"/install.bak

	if [[ -n $repository_service_base ]] && [[ -n $repository_user ]] && [[ -n $repository_password ]] ; then
		echo "- register rendering service with the repository"

		until [[ $( curl -sSf -w "%{http_code}\n" -o /dev/null "${my_internal_url}/admin/" ) -eq 200 ]]
    do
    	echo >&2 "Waiting for ${my_internal_url} ..."
    	sleep 3
    done

		my_meta_internal="${my_internal_url}/application/esmain/metadata.php"
		my_appid=$( \
			curl -sS "${my_meta_internal}" | xmlstarlet sel -t -v '/properties/entry[@key="appid"]' - | xargs echo \
		)

		access_token=$( \
  	curl -sS \
  		-XPOST \
  		-d "grant_type=password&client_id=eduApp&client_secret=secret&username=${repository_user}&password=${repository_password}" \
  		"${repository_service_base}/oauth2/token" | jq -r '.access_token' \
  	)

  	if [ -n "${has_my_appid}" ] ; then
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
	fi

else

	echo ""
	echo "Update ... "

	echo "- make a snapshot of the rendering service"
  snapshot_name=rendering-SNAPSHOT-$(date "+%Y.%m.%d-%H.%M.%S")".tar.gz"
	tar -czf $snapshot_name $(basename "${RS_ROOT}")

	echo "- cleanup rendering server"
	rm -rf $RS_ROOT

	install_edu_sharing

  echo "- restore base rendering config"
  tar -zxf $snapshot_name $(basename "${RS_ROOT}")/conf
  tar -zxf $snapshot_name --wildcards "*config.php" -C $(basename "${RS_ROOT}")

	echo "- update rendering service"
	yes | php "${RS_ROOT}"/admin/cli/update.php || true
	mv "${RS_ROOT}"/install/ "${RS_ROOT}"/install.bak

	echo ""
	echo "- delete old rendering SNAPSHOTS (keep 3 backups)"
  ls -pt | grep -v / | grep "rendering-SNAPSHOT" | tail -n +4 | xargs -I {} rm {}

fi

echo "- update configuration"
if [[ -n $rendering_proxy_host ]] ; then
	proxyConf="${RS_ROOT}"/conf/proxy.conf.php
	cp -rf "${RS_ROOT}"/conf/proxy.conf.php.example "${proxyConf}"
	sed -i -r "s|define\('HTTP_PROXY_HOST',.*);|define('HTTP_PROXY_HOST', '$rendering_proxy_host');|" "${proxyConf}"
	sed -i -r "s|define\('HTTP_PROXY_PORT',.*);|define('HTTP_PROXY_PORT', $rendering_proxy_port);|" "${proxyConf}"
	sed -i -r "s|define\('HTTP_PROXY_USER',.*);|define('HTTP_PROXY_USER', '$rendering_proxy_user');|" "${proxyConf}"
	sed -i -r "s|define\('HTTP_PROXY_PASS',.*);|define('HTTP_PROXY_PASS', '$rendering_proxy_pass');|" "${proxyConf}"
fi

dbConf="${RS_ROOT}"/conf/db.conf.php
sed -i -r "s|\$dsn.*|\$dsn = \"${db_driver}:host=${db_host};port=${db_port};dbname=${db_name}\";|" "${dbConf}"
sed -i -r "s|\$dbuser.*|\$dbuser = \"$db_user\";|" "${dbConf}"
sed -i -r "s|\$pwd.*|\pwd = \"$db_password\";|" "${dbConf}"

systemConf="${RS_ROOT}"/conf/system.conf.php
sed -i -r "s|\$MC_URL.*|\$MC_URL = '$db_user';|" "${systemConf}"
sed -i -r "s|\$MC_DOCROOT.*|\$MC_DOCROOT = '$db_user';|" "${systemConf}"
sed -i -r "s|\$CC_RENDER_PATH.*|\$CC_RENDER_PATH = '$db_user';|" "${systemConf}"

xmlstarlet ed -L \
	-u '/properties/entry[@key="scheme"]' -v "${my_prot_internal}" \
	-u '/properties/entry[@key="host"]' -v "${my_host_internal}" \
	-u '/properties/entry[@key="port"]' -v "${my_port_internal}" \
	-u '/properties/entry[@key="appid"]' -v "${my_home_appid}" \
	"${RS_ROOT}"/conf/esmain/homeApplication.properties.xml

echo "- set cache cleaner CronJob"
croneJob=/tmp/mycron
crontab -l > "${croneJob}" 2> /dev/null || touch "${croneJob}"

sed -i -r 's|^[#]*.*Rendering-Service cache cleaner|'"${my_cache_cleaner_interval} ${RS_CACHE} www-data php /func/classes.new/Helper/cacheCleaner.php # Rendering-Service cache cleaner"'|' "${croneJob}"
grep -q '^[#]*.*Rendering-Service cache cleaner' "${croneJob}" || echo "${my_cache_cleaner_interval} ${RS_CACHE} www-data php /func/classes.new/Helper/cacheCleaner.php # Rendering-Service cache cleaner" >>"${croneJob}"

crontab "${croneJob}"
rm -f  "${croneJob}"

chown -R www-data:www-data "${RS_ROOT}"
popd

info >> "install_log-$(date "+%Y.%m.%d-%H.%M.%S").txt"
info
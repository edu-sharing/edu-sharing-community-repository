#!/bin/bash
set -e
set -o pipefail

pushd $WWW_ROOT

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



usage() {
	echo "Options:"
	echo ""

	echo "--interalURL"
	echo "internal application url"

	echo "--externalURL"
	echo "external application url"

	echo "-d dbname"
  echo "--dbname=dbname"
  echo "Specifies the name of the database to connect to."
	echo ""

	echo "-h hostname"
  echo "--host=hostname"
  echo "Specifies the host name of the machine on which the server is running."
	echo ""

  echo "-p port"
  echo "--port=port"
  echo " Specifies the TCP port on which the server is listening for connections."
	echo ""

  echo "-U username"
  echo "--username=username"
  echo " Connect to the database as the user username."
	echo ""

  echo "-W"
  echo "--password"
  echo "Connect to the database with the user password."
	echo ""

  echo "-R"
  echo "--repository"
  echo "url of the repository to fetch properties and content from"
}

internalURL=http://127.0.0.1:8080/esrender
externalURL=http://127.0.0.1:8080/esrender
db_host=127.0.0.1
db_port=5432
db_name=rendering
db_user=rendering
db_password=rendering
repository_service_base=

while true; do
	flag="$1"
	echo "flage: $1"
	shift || break

	case "$flag" in
			--help|'-?') usage && exit 0 ;;
			--host|-h) db_host="$1" && shift	;;
			--port|-p) db_port="$1" && shift	;;
			--dbname|-d) db_name="$1" && shift	;;
			--username|-U) db_user="$1" && shift	;;
			--password|-W) db_password="$1" && shift	;;
			--repository|-R) repository_service_base="$1" && shift ;;
			--internalURL) internalURL="$1" && shift ;;
			--externalURL) externalURL="$1" && shift ;;
			*) {
				echo "error: unknown flag: $flag"
				usage
			}  >&2
			exit 1 ;;
	esac
done




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

if [[ ! -d "${RS_CACHE}" ]] ; then

	echo ""
	echo "Install ... "

	install_edu_sharing

  mkdir -p "${RS_CACHE}"
  chown -R www-data:www-data "${RS_CACHE}"

  cat >/tmp/config.ini <<-EOF
		[application]
		; url for client requests (accessible from the internet)
		application_url_client=${externalURL}
		; url for requests from repository
		application_url_repository=${internalURL}
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
		db_driver=pgsql
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

  php esrender/admin/cli/install.php -c /tmp/config.ini || {
  	result=$?;
  	rm -rf "${RS_CACHE}" 2> /dev/null
  	rm -rf esrender 2> /dev/null
  	rm -f /tmp/config.ini 2> /dev/null
  	exit $result
  }


  rm -f /tmp/config.ini
	mv esrender/install/ esrender/install.bak

else

	echo ""
	echo "Update ... "

	echo "- make a snapshot of the rendering service"
  snapshot_name=rendering-SNAPSHOT-$(date "+%Y.%m.%d-%H.%M.%S")".tar.gz"
	tar -czf $snapshot_name esrender

	echo "- cleanup rendering server"
	rm -rf $RS_ROOT

	install_edu_sharing

  echo "- restore base rendering config"
  tar -zxf $snapshot_name esrender/conf
  tar -zxf $snapshot_name --wildcards "*config.php" -C esrender

	yes | php esrender/admin/cli/update.php

	echo "- delete old rendering SNAPSHOTS (keep 3 backups)"
  ls -pt | grep -v / | grep "rendering-SNAPSHOT" | tail -n +4 | xargs -I {} rm {}

fi

chown -R www-data:www-data "${RS_ROOT}"
popd
#!/bin/bash
set -e
set -o pipefail

[[ ! -f "${ALF_HOME}/alfresco.sh" ]] && {
	echo ""
	echo "Env ALF_HOME must point to the home directory of your Alfresco Platform!"
	exit
}

pushd "$ALF_HOME"

[[ ! -d tomcat/webapps/alfresco || ! -d tomcat/webapps/solr4 ]] && {
	echo ""
	echo "You must have started the Alfresco Platform at least once before you can run the installation."
	exit
}

[[ "$(./alfresco.sh status tomcat)" != "tomcat not running" ]] && {
	echo ""
	echo "Please stop Tomcat before you can run the installation!"
	exit
}

[[ "$(./alfresco.sh status postgresql)" != "postgresql not running" ]] && {
	echo ""
	echo "Please stop Postgresql before you can run the installation!"
	exit
}

# load the default configuration
if [[ -f ".env.base" ]] ; then
	source .env.base
fi

usage() {
	echo "Options:"
	echo ""

	echo"-?"
	echo"--help"
	echo "Display available options"
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

################################################################################################################

my_home_appid="${REPOSITORY_SERVICE_HOME_APPID:-local}" # Kundenprojekt ?

my_admin_pass="${REPOSITORY_SERVICE_ADMIN_PASS:-admin}"
my_admin_pass_md4="$(printf '%s' "$my_admin_pass" | iconv -t utf16le | openssl md4 | awk '{ print $2 }')"

my_prot_external="${REPOSITORY_SERVICE_PROT_EXTERNAL:-"http"}"
my_host_external="${REPOSITORY_SERVICE_HOST_EXTERNAL:-"localhost"}"
my_port_external="${REPOSITORY_SERVICE_PORT_EXTERNAL:-80}"
my_path_external="${REPOSITORY_SERVICE_PATH_EXTERNAL:-/"edu-sharing"}"
my_base_external="${my_prot_external}://${my_host_external}:${my_port_external}${my_path_external}"
my_auth_external="${my_base_external}/services/authentication"

my_host_internal="${REPOSITORY_SERVICE_HOST_INTERNAL:-"localhost"}"
my_port_internal="${REPOSITORY_SERVICE_PORT_INTERNAL:-8080}"

repository_database_driv="${REPOSITORY_DATABASE_DRIV:-"org.postgresql.Driver"}"
repository_database_host="${REPOSITORY_DATABASE_HOST:-"127.0.0.1"}"
repository_database_name="${REPOSITORY_DATABASE_NAME:-"repository"}"
repository_database_opts="${REPOSITORY_DATABASE_OPS:-}"
repository_database_pass="${REPOSITORY_DATABASE_PASS:-"repository"}"
repository_database_pool_max="${REPOSITORY_DATABASE_POOL_MAX:-80}"
repository_database_pool_sql="${REPOSITORY_DATABASE_POOL_SQL:-SELECT 1}"
repository_database_port="${REPOSITORY_DATABASE_PORT:-5432}"
repository_database_prot="${REPOSITORY_DATABASE_PROT:-"postgresql"}"
repository_database_user="${REPOSITORY_DATABASE_USER:-"repository"}"
repository_database_jdbc="jdbc:${repository_database_prot}://${repository_database_host}:${repository_database_port}/${repository_database_name}${repository_database_opts}"

repository_proxy_host="${REPOSITORY_PROXY_HOST:-}"
repository_proxy_nonproxyhosts="${REPOSITORY_PROXY_NONPROXYHOSTS:-}"
repository_proxy_proxyhost="${REPOSITORY_PROXY_PROXYHOST:-}"
repository_proxy_proxyport="${REPOSITORY_PROXY_PROXYPORT:-}"
repository_proxy_proxyuser="${REPOSITORY_PROXY_PROXYUSER:-}"
repository_proxy_proxypass="${REPOSITORY_PROXY_PROXYPASS:-}"

repository_search_solr4_host="${REPOSITORY_SEARCH_SOLR4_HOST:-"127.0.0.1"}"
repository_search_solr4_port="${REPOSITORY_SEARCH_SOLR4_PORT:-8080}"

repository_transform_host="${REPOSITORY_TRANSFORM_HOST:-"127.0.0.1"}"
repository_transform_port="${REPOSITORY_TRANSFORM_PORT:-8100}"

repository_contentstore="${REPOSITORY_SERVICE_CONTENTSTORE:-}"
repository_contentstore_deleted="${REPOSITORY_SERVICE_CONTENTSTORE_DELETED:-}"


info() {
	echo ""
	echo "#########################################################################"
	echo ""
	echo "repository-database:"
	echo ""
	echo "  Database:"
	echo ""
	echo "    Driver:            ${repository_database_driv}"
	echo "    Protocol:          ${repository_database_prot}"
	echo "    Host:              ${repository_database_host}"
	echo "    Port:              ${repository_database_port}"
	echo "    Name:              ${repository_database_name}"
	echo "    Optionals:         ${repository_database_opts}"
  echo "    Max Pool:          ${repository_database_pool_max}"
  echo "    Pool SQL:          ${repository_database_pool_sql}"
  echo ""
	echo "  Credentials:"
	echo ""
	echo "     User:             ${repository_database_user}"
	echo "     Password:         ${repository_database_pass}"
	echo ""
	echo "#########################################################################"
	echo ""
	echo "repository-service:"
	echo ""
	echo "  Common:"
	echo ""
	echo "    AppId:               ${my_home_appid}"
	echo "    Admin password:      ${my_admin_pass}"
	echo ""
	echo "  Public:"
	echo ""
	echo "    Protocol:            ${my_prot_external}"
	echo "    Host:                ${my_host_external}"
	echo "    Port:                ${my_port_external}"
	echo "    Path:                ${my_path_external}"
	echo ""
  echo "  Private:"
  echo ""
  echo "    Host:              ${my_host_internal}"
  echo "    Port:              ${my_port_internal}"
  echo ""
  echo "  Services:"
  echo ""
  echo "    Public:            ${my_base_external}"
  echo ""

	if [[ -n $rendering_proxy_host ]] ; then
	echo "#########################################################################"
  echo ""
  echo "proxy:"
  echo ""
  echo "  Host:                ${repository_proxy_host}"
  echo "  Non Proxy Host:      ${repository_proxy_nonproxyhosts}"
  echo "  Proxy Host:          ${repository_proxy_proxyhost}"
  echo "  Proxy Port:          ${repository_proxy_proxyport}"
  echo "  Proxy User:          ${repository_proxy_proxyuser}"
  echo "  Proxy Password:      ${repository_proxy_proxypass}"
  echo ""
	fi

  echo "#########################################################################"
  echo ""
  echo "solr4:"
  echo ""
  echo "  Host:                ${repository_search_solr4_host}"
  echo "  Port:                ${repository_search_solr4_port}"
  echo ""
 	echo "#########################################################################"
  echo ""
  echo "transformer:"
  echo ""
  echo "  Host:                ${repository_transform_host}"
  echo "  Port:                ${repository_transform_port}"
  echo ""
  echo "#########################################################################"
  echo ""

  if [[ -n $repository_contentstore || -n $repository_contentstore_deleted ]] ; then
  echo "#########################################################################"
  echo ""
  echo "persistent data:"
  echo ""
  echo "  contentstore:        ${repository_contentstore}"
  echo "  contentstore.delete: ${repository_contentstore_deleted}"
  echo ""
  echo "#########################################################################"
  echo ""
  fi

  echo ""
}


install_edu_sharing() {

	echo "- clean up outdated libraries"
	rm -f tomcat/lib/postgresql-*
	rm -f tomcat/webapps/alfresco/WEB-INF/lib/commons-lang3-*
	rm -f tomcat/webapps/alfresco/WEB-INF/lib/hazelcast-*
	rm -f tomcat/webapps/alfresco/WEB-INF/lib/jackson-*


	########################################################################################################################

	echo "- download edu-sharing repository distribution"
	mvn -q dependency:get \
		-Dartifact=org.edu_sharing:edu_sharing-community-deploy-installer-repository-distribution:${org.edu_sharing:edu_sharing-community-deploy-installer-repository-distribution:tar.gz:bin.version}:tar.gz:bin \
		-DremoteRepositories=myreleases::::https://artifacts.edu-sharing.com/repository/community-releases/,mysnapshots::::https://artifacts.edu-sharing.com/repository/community-snapshots/ \
		-Dtransitive=false

	echo "- unpack edu-sharing repository distribution"
	mvn -q dependency:copy \
		-Dartifact=org.edu_sharing:edu_sharing-community-deploy-installer-repository-distribution:${org.edu_sharing:edu_sharing-community-deploy-installer-repository-distribution:tar.gz:bin.version}:tar.gz:bin \
		-DoutputDirectory=.

	tar xzf edu_sharing-community-deploy-installer-repository-distribution-${org.edu_sharing:edu_sharing-community-deploy-installer-repository-distribution:tar.gz:bin.version}-bin.tar.gz
	rm edu_sharing-community-deploy-installer-repository-distribution-${org.edu_sharing:edu_sharing-community-deploy-installer-repository-distribution:tar.gz:bin.version}-bin.tar.gz

	echo "- install Alfresco Module Packages"
	if [[ -d amps/alfresco/0 ]]; then
	  java -jar bin/alfresco-mmt.jar install amps/alfresco/0 tomcat/webapps/alfresco -directory -nobackup -force 2> /dev/null
	fi

	if [[ -d amps/alfresco/1 ]]; then
	  java -jar bin/alfresco-mmt.jar install amps/alfresco/1 tomcat/webapps/alfresco -directory -nobackup -force 2> /dev/null
  fi

  if [[ -d amps/edu-sharing/1 ]]; then
    java -jar bin/alfresco-mmt.jar install amps/edu-sharing/1 tomcat/webapps/edu-sharing -directory -nobackup -force 2> /dev/null
  fi


	### Tomcat #############################################################################################################

	echo "- update tomcat env"
	sed -i -r 's|file\.encoding=.*\"|file.encoding=UTF-8 $CATALINA_OPTS \"|' tomcat/bin/setenv.sh
	grep -q 'file\.encoding' tomcat/bin/setenv.sh || echo 'CATALINA_OPTS="-Dfile.encoding=UTF-8 $CATALINA_OPTS "' >> tomcat/bin/setenv.sh

	sed -i -r 's|org\.xml\.sax\.parser=.*\"|org.xml.sax.parser=com.sun.org.apache.xerces.internal.parsers.SAXParser $CATALINA_OPTS \"|' tomcat/bin/setenv.sh
	grep -q 'org\.xml\.sax\.parser' tomcat/bin/setenv.sh || echo 'CATALINA_OPTS="-Dorg.xml.sax.parser=com.sun.org.apache.xerces.internal.parsers.SAXParser $CATALINA_OPTS "' >> tomcat/bin/setenv.sh

	sed -i -r 's|javax\.xml\.parsers\.DocumentBuilderFactory=.*\"|javax.xml.parsers.DocumentBuilderFactory=com.sun.org.apache.xerces.internal.jaxp.DocumentBuilderFactoryImpl $CATALINA_OPTS \"|' tomcat/bin/setenv.sh
	grep -q 'javax\.xml\.parsers\.DocumentBuilderFactory' tomcat/bin/setenv.sh || echo 'CATALINA_OPTS="-Djavax.xml.parsers.DocumentBuilderFactory=com.sun.org.apache.xerces.internal.jaxp.DocumentBuilderFactoryImpl $CATALINA_OPTS "' >> tomcat/bin/setenv.sh

	sed -i -r 's|javax\.xml\.parsers\.SAXParserFactory=.*\"|javax.xml.parsers.SAXParserFactory=com.sun.org.apache.xerces.internal.jaxp.SAXParserFactoryImpl $CATALINA_OPTS \"|' tomcat/bin/setenv.sh
	grep -q 'javax\.xml\.parsers\.SAXParserFactory' tomcat/bin/setenv.sh || echo 'CATALINA_OPTS="-Djavax.xml.parsers.SAXParserFactory=com.sun.org.apache.xerces.internal.jaxp.SAXParserFactoryImpl $CATALINA_OPTS "' >> tomcat/bin/setenv.sh

	### Alfresco platform ##################################################################################################

	alfGlobalProps="tomcat/shared/classes/alfresco-global.deployment.properties"
	solr4WorkspaceProps="solr4/workspace-SpacesStore/conf/solrcore.properties"
	solr4ArchiveProps="solr4/archive-SpacesStore/conf/solrcore.properties"

	touch $alfGlobalProps
	touch tomcat/shared/classes/config/000-default.conf
	touch tomcat/shared/classes/config/edu-sharing.deployment.conf

	echo "- update alfresco env"

	if [[ -n $repository_contentstore ]] ; then
  	echo "dir.contentstore=${repository_contentstore}" >> "${alfGlobalProps}"
  fi

  if [[ -n $repository_contentstore_deleted ]] ; then
	  echo "dir.contentstore.deleted=${repository_contentstore_deleted}" >> "${alfGlobalProps}"
	fi

  echo 'img.root=/usr' >> "${alfGlobalProps}"
  echo 'img.gslib=/usr/bin' >> "${alfGlobalProps}"
  echo 'exiftool.dyn=/usr/bin' >> "${alfGlobalProps}"
  echo 'exiftool.exe=${exiftool.dyn}/exiftool' >> "${alfGlobalProps}"
  echo 'ffmpeg.dyn=/usr/bin' >> "${alfGlobalProps}"
  echo 'ffmpeg.exe=${ffmpeg.dyn}/ffmpeg' >> "${alfGlobalProps}"
  echo 'img.dyn=/usr/bin' >> "${alfGlobalProps}"
  echo 'img.exe=${img.dyn}/convert' >> "${alfGlobalProps}"
	echo "alfresco_user_store.adminpassword=${my_admin_pass_md4}" >> "${alfGlobalProps}"
	echo "db.driver=${repository_database_driv}" >> "${alfGlobalProps}"
	echo "db.url=${repository_database_jdbc}" >> "${alfGlobalProps}"
	echo "db.username=${repository_database_user}" >> "${alfGlobalProps}"
	echo "db.password=${repository_database_pass}" >> "${alfGlobalProps}"
	echo "db.pool.max=${repository_database_pool_max}" >> "${alfGlobalProps}"
	echo "db.pool.validate.query=${repository_database_pool_sql}" >> "${alfGlobalProps}"
	echo "ooo.enabled=true" >> "${alfGlobalProps}"
	echo "ooo.exe=" >> "${alfGlobalProps}"
	echo "ooo.host=${repository_transform_host}" >> "${alfGlobalProps}"
	echo "ooo.port=${repository_transform_port}" >> "${alfGlobalProps}"
	echo "solr.host=${repository_search_solr4_host}" >> "${alfGlobalProps}"
  echo "solr.port=${repository_search_solr4_port}" >> "${alfGlobalProps}"
  echo "solr.secureComms=none" >> "${alfGlobalProps}"
  echo "alfresco.secureComms=none" >> "${solr4WorkspaceProps}"
  echo "alfresco.secureComms=none" >> "${solr4ArchiveProps}"

	### edu-sharing ########################################################################################################

	echo "- update edu-sharing env"
	xmlstarlet ed -L \
		-u '/properties/entry[@key="appid"]' -v "${my_home_appid}" \
		-u '/properties/entry[@key="authenticationwebservice"]' -v "${my_auth_external}" \
		-u '/properties/entry[@key="clientport"]' -v "${my_port_external}" \
		-u '/properties/entry[@key="clientprotocol"]' -v "${my_prot_external}" \
		-u '/properties/entry[@key="domain"]' -v "${my_host_external}" \
		-u '/properties/entry[@key="host"]' -v "${my_host_internal}" \
		-u '/properties/entry[@key="password"]' -v "${my_admin_pass}" \
		-u '/properties/entry[@key="port"]' -v "${my_port_internal}" \
		tomcat/shared/classes/homeApplication.properties.xml

	if [[ -n "${repository_proxy_host}" ]] ; then
		hocon -f tomcat/shared/classes/config/edu-sharing.deployment.conf \
			set "repository.proxy.host" "${repository_proxy_host}"
	fi

	if [[ -n "${repository_proxy_nonproxyhosts}" ]]  ; then
		export CATALINA_OPTS="-Dhttp.nonProxyHosts=${repository_proxy_nonproxyhosts} $CATALINA_OPTS"
		export CATALINA_OPTS="-Dhttps.nonProxyHosts=${repository_proxy_nonproxyhosts} $CATALINA_OPTS"
		hocon -f tomcat/shared/classes/config/edu-sharing.deployment.conf \
			set "repository.proxy.nonproxyhosts" "${repository_proxy_nonproxyhosts}"
	fi

	if [[ -n "${repository_proxy_proxyhost}" ]] ; then
		export CATALINA_OPTS="-Dhttp.proxyHost=${repository_proxy_proxyhost} $CATALINA_OPTS"
		export CATALINA_OPTS="-Dhttps.proxyHost=${repository_proxy_proxyhost} $CATALINA_OPTS"
		hocon -f tomcat/shared/classes/config/edu-sharing.deployment.conf \
			set "repository.proxy.proxyhost" "${repository_proxy_proxyhost}"
	fi

	if [[ -n "${repository_proxy_proxypass}" ]] ; then
		export CATALINA_OPTS="-Dhttp.proxyPass=${repository_proxy_proxypass} $CATALINA_OPTS"
		export CATALINA_OPTS="-Dhttps.proxyPass=${repository_proxy_proxypass} $CATALINA_OPTS"
		hocon -f tomcat/shared/classes/config/edu-sharing.deployment.conf \
			set "repository.proxy.proxypass" "${repository_proxy_proxypass}"
	fi

	if [[ -n "${repository_proxy_proxyport}" ]] ; then
		export CATALINA_OPTS="-Dhttp.proxyPort=${repository_proxy_proxyport} $CATALINA_OPTS"
		export CATALINA_OPTS="-Dhttps.proxyPort=${repository_proxy_proxyport} $CATALINA_OPTS"
		hocon -f tomcat/shared/classes/config/edu-sharing.deployment.conf \
			set "repository.proxy.proxyport" "${repository_proxy_proxyport}"
	fi

	if [[ -n "${repository_proxy_proxyuser}" ]] ; then
		export CATALINA_OPTS="-Dhttp.proxyUser=${repository_proxy_proxyuser} $CATALINA_OPTS"
		export CATALINA_OPTS="-Dhttps.proxyUser=${repository_proxy_proxyuser} $CATALINA_OPTS"
		hocon -f tomcat/shared/classes/config/edu-sharing.deployment.conf \
			set "repository.proxy.proxyuser" "${repository_proxy_proxyuser}"
	fi
}

########################################################################################################################

alfresco_base_snapshot="alfresco-base-SNAPSHOT-DO-NOT-DELETE-ME.tar.gz"
if [[ -f ../$alfresco_base_snapshot ]] ; then

	echo ""
	echo "Update ... "

	echo "- make a snapshot of edu-sharing platform"
  snapshot_name=edu-sharing-SNAPSHOT-$(date "+%Y.%m.%d-%H.%M.%S")".tar.gz"
	tar -czf ../$snapshot_name amps tomcat solr4

	echo "- cleanup amps and tomcat"
  rm -rf amps
  rm -rf tomcat
  rm -rf solr4

  echo "- restore amps and tomcat"
  tar -zxf ../$alfresco_base_snapshot

	install_edu_sharing

  echo "- restore persistent data of Alfresco platform"
  if [[ $(tar -tf  ../$snapshot_name | grep 'tomcat/shared/classes/config/persistent' | wc -l) -gt 0 ]]; then
    tar -zxf ../$snapshot_name tomcat/shared/classes/config/persistent -C tomcat/shared/classes/config/
  else
    echo "nothing to restore"
  fi  

  echo "- delete old edu-sharing SNAPSHOTS (keep 3 backups)"
  pushd ..
  ls -pt | grep -v / | grep "edu-sharing-SNAPSHOT" | tail -n +4 | xargs -I {} rm {}
	popd

else

	echo ""
	echo "Install ... "

	echo "- make a snapshot of Alfresco platform"
 	tar -czf ../$alfresco_base_snapshot amps tomcat solr4

  install_edu_sharing

fi

popd

info >> "install_log-$(date "+%Y.%m.%d-%H.%M.%S").txt"
info

echo "- done."
echo "- you may need to delete the solr4 index, model and content folders!"

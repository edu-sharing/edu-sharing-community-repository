#!/bin/bash
set -e
set -o pipefail

########################################################################################################################
#execution_folder=$(dirname -- "${BASH_SOURCE[0]}")
execution_folder="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
echo execution_folder

pushd "$execution_folder" &> /dev/null

# load the default configuration
if [[ -f ".env.base" ]] ; then
	echo "Load .env.base"
	source .env.base &> /dev/null
fi

# LOAD PLUGIN BASE ENVIRONMENT VARIABLES
if [[ -d "$execution_folder/plugins" ]] ; then
	for plugin in "$execution_folder"/plugins/plugin-*/.env.base; do
		 [[ -f "$plugin" ]] && {
		 		source "$plugin" || exit 1
		 }
	done
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

	echo "--baseimage"
	echo "  Define the path to the alfresco base image"
}

########################################################################################################################

use_local_maven_cache=0
alfresco_base_image="$execution_folder/alfresco-base-image-for-edu-sharing_DO-NOT-DELETE-ME.tar.gz"

while true; do
	flag="$1"
	shift || break

	case "$flag" in
			--help|'-?') usage && exit 0 ;;
			--file|-f) source "$1" && shift	;;
			--local) use_local_maven_cache=1 ;;
			--baseimage) alfresco_base_image="$1" && shift;;
			*) {
				echo "error: unknown flag: $flag"
				usage
			}  >&2
			exit 1 ;;
	esac
done

########################################################################################################################

my_home_appid="${REPOSITORY_SERVICE_HOME_APPID:-"local"}" # Kundenprojekt ?
my_home_auth="${REPOSITORY_SERVICE_HOME_AUTH:-}"
my_home_provider="${REPOSITORY_SERVICE_HOME_PROVIDER:-}"

my_admin_pass="${REPOSITORY_SERVICE_ADMIN_PASS:-"admin"}"
my_admin_pass_md4="$(printf '%s' "$my_admin_pass" | iconv -t utf16le | openssl md4 | awk '{ print $2 }')"

my_prot_external="${REPOSITORY_SERVICE_PROT_EXTERNAL:-"http"}"
my_host_external="${REPOSITORY_SERVICE_HOST_EXTERNAL:-"localhost"}"
my_port_external="${REPOSITORY_SERVICE_PORT_EXTERNAL:-80}"
my_path_external="${REPOSITORY_SERVICE_PATH_EXTERNAL:-"edu-sharing"}"
my_base_external="${my_prot_external}://${my_host_external}:${my_port_external}/${my_path_external}"
my_auth_external="${my_base_external}/services/authentication"

my_host_internal="${REPOSITORY_SERVICE_HOST_INTERNAL:-"localhost"}"
my_port_internal="${REPOSITORY_SERVICE_PORT_INTERNAL:-8080}"

repository_database_driv="${REPOSITORY_DATABASE_DRIV:-"org.postgresql.Driver"}"
repository_database_host="${REPOSITORY_DATABASE_HOST:-"127.0.0.1"}"
repository_database_name="${REPOSITORY_DATABASE_NAME:-"repository"}"
repository_database_opts="${REPOSITORY_DATABASE_OPS:-}"
repository_database_pass="${REPOSITORY_DATABASE_PASS:-"repository"}"
repository_database_pool_max="${REPOSITORY_DATABASE_POOL_MAX:-80}"
repository_database_pool_sql="${REPOSITORY_DATABASE_POOL_SQL:-"SELECT 1"}"
repository_database_port="${REPOSITORY_DATABASE_PORT:-5432}"
repository_database_prot="${REPOSITORY_DATABASE_PROT:-"postgresql"}"
repository_database_user="${REPOSITORY_DATABASE_USER:-"repository"}"
repository_database_jdbc="jdbc:${repository_database_prot}://${repository_database_host}:${repository_database_port}/${repository_database_name}${repository_database_opts}"

repository_httpclient_disablesni4hosts="${REPOSITORY_SERVICE_HTTP_CLIENT_DISABLE_SNI4HOSTS:-}"
repository_httpclient_proxy_host="${REPOSITORY_SERVICE_HTTP_CLIENT_PROXY_HOST:-}"
repository_httpclient_proxy_nonproxyhosts="${REPOSITORY_SERVICE_HTTP_CLIENT_PROXY_NONPROXYHOSTS:-}"
repository_httpclient_proxy_proxyhost="${REPOSITORY_SERVICE_HTTP_CLIENT_PROXY_PROXYHOST:-}"
repository_httpclient_proxy_proxyport="${REPOSITORY_SERVICE_HTTP_CLIENT_PROXY_PROXYPORT:-}"
repository_httpclient_proxy_proxyuser="${REPOSITORY_SERVICE_HTTP_CLIENT_PROXY_PROXYUSER:-}"
repository_httpclient_proxy_proxypass="${REPOSITORY_SERVICE_HTTP_CLIENT_PROXY_PROXYPASS:-}"

repository_search_solr4_host="${REPOSITORY_SEARCH_SOLR4_HOST:-"127.0.0.1"}"
repository_search_solr4_port="${REPOSITORY_SEARCH_SOLR4_PORT:-8080}"

repository_transform_host="${REPOSITORY_TRANSFORM_HOST:-"127.0.0.1"}"
repository_transform_port="${REPOSITORY_TRANSFORM_PORT:-8100}"

repository_contentstore="${REPOSITORY_SERVICE_CONTENTSTORE:-}"
repository_contentstore_deleted="${REPOSITORY_SERVICE_CONTENTSTORE_DELETED:-}"

########################################################################################################################

# LOAD PLUGIN CONFIG VARIABLES
if [[ -d "$execution_folder/plugins" ]] ; then
	for plugin in "$execution_folder"/plugins/plugin-*/load_config.sh; do
		 [[ -f "$plugin" ]] && {
		 		source "$plugin" || exit 1
		 }
	done
fi

########################################################################################################################

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
	echo "    AppId:             ${my_home_appid}"
	echo "    Authentication     ${my_home_auth}"
	echo "    Admin password:    ${my_admin_pass}"
	echo ""
	echo "  Public:"
	echo ""
	echo "    Protocol:          ${my_prot_external}"
	echo "    Host:              ${my_host_external}"
	echo "    Port:              ${my_port_external}"
	echo "    Path:              ${my_path_external}"
	echo ""
  echo "  Private:"
  echo ""
  echo "    Host:              ${my_host_internal}"
  echo "    Port:              ${my_port_internal}"
  echo ""
  echo "  Services:"
  echo ""
  echo "    Public:            ${my_base_external}"
  echo "    Home provider:     ${REPOSITORY_SERVICE_HOME_PROVIDER}"
  echo ""

	if [[ -n $rendering_proxy_host ]] ; then
	echo "#########################################################################"
  echo ""
  echo "httpclient:"
  echo ""
  echo "  Disable SNI4Hosts:     ${repository_httpclient_disablesni4hosts}"
  echo ""
  echo "  proxy:"
  echo ""
  echo "    Host:                ${repository_httpclient_proxy_host}"
  echo "    Non Proxy Host:      ${repository_httpclient_proxy_nonproxyhosts}"
  echo "    Proxy Host:          ${repository_httpclient_proxy_proxyhost}"
  echo "    Proxy Port:          ${repository_httpclient_proxy_proxyport}"
  echo "    Proxy User:          ${repository_httpclient_proxy_proxyuser}"
  echo "    Proxy Password:      ${repository_httpclient_proxy_proxypass}"
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


  if [[ -n $repository_contentstore || -n $repository_contentstore_deleted ]] ; then
  echo "#########################################################################"
  echo ""
  echo "persistent data:"
  echo ""
  echo "  contentstore:        ${repository_contentstore}"
  echo "  contentstore.delete: ${repository_contentstore_deleted}"
  echo ""
  fi

  ######################################################################################################################

  # PRINT PLUGIN CONFIG
  if [[ -d "$execution_folder/plugins" ]] ; then
  	for plugin in "$execution_folder"/plugins/plugin-*/print_config.sh; do
  		 [[ -f "$plugin" ]] && {
  		 		source "$plugin" || exit 1
  		 }
  	done
  fi

  echo "#########################################################################"
  echo ""
}

########################################################################################################################

install_edu_sharing() {
	pushd "$ALF_HOME" &> /dev/null

	echo "- clean up outdated libraries"
	rm -f tomcat/lib/postgresql-*
	rm -f tomcat/webapps/alfresco/WEB-INF/lib/commons-lang3-*
	rm -f tomcat/webapps/alfresco/WEB-INF/lib/hazelcast-*
	rm -f tomcat/webapps/alfresco/WEB-INF/lib/jackson-*

	######################################################################################################################

	echo "- unpack edu-sharing repository"
	tar xzf edu_sharing-community-deploy-installer-repository-distribution-${org.edu_sharing:edu_sharing-community-deploy-installer-repository-distribution:tar.gz:bin.version}-bin.tar.gz

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


	### Tomcat ###########################################################################################################

	echo "- update tomcat env"
	sed -i -r 's|file\.encoding=.*\"|file.encoding=UTF-8 $CATALINA_OPTS \"|' tomcat/bin/setenv.sh
	grep -q 'file\.encoding' tomcat/bin/setenv.sh || echo 'CATALINA_OPTS="-Dfile.encoding=UTF-8 $CATALINA_OPTS "' >> tomcat/bin/setenv.sh

	sed -i -r 's|org\.xml\.sax\.parser=.*\"|org.xml.sax.parser=com.sun.org.apache.xerces.internal.parsers.SAXParser $CATALINA_OPTS \"|' tomcat/bin/setenv.sh
	grep -q 'org\.xml\.sax\.parser' tomcat/bin/setenv.sh || echo 'CATALINA_OPTS="-Dorg.xml.sax.parser=com.sun.org.apache.xerces.internal.parsers.SAXParser $CATALINA_OPTS "' >> tomcat/bin/setenv.sh

	sed -i -r 's|javax\.xml\.parsers\.DocumentBuilderFactory=.*\"|javax.xml.parsers.DocumentBuilderFactory=com.sun.org.apache.xerces.internal.jaxp.DocumentBuilderFactoryImpl $CATALINA_OPTS \"|' tomcat/bin/setenv.sh
	grep -q 'javax\.xml\.parsers\.DocumentBuilderFactory' tomcat/bin/setenv.sh || echo 'CATALINA_OPTS="-Djavax.xml.parsers.DocumentBuilderFactory=com.sun.org.apache.xerces.internal.jaxp.DocumentBuilderFactoryImpl $CATALINA_OPTS "' >> tomcat/bin/setenv.sh

	sed -i -r 's|javax\.xml\.parsers\.SAXParserFactory=.*\"|javax.xml.parsers.SAXParserFactory=com.sun.org.apache.xerces.internal.jaxp.SAXParserFactoryImpl $CATALINA_OPTS \"|' tomcat/bin/setenv.sh
	grep -q 'javax\.xml\.parsers\.SAXParserFactory' tomcat/bin/setenv.sh || echo 'CATALINA_OPTS="-Djavax.xml.parsers.SAXParserFactory=com.sun.org.apache.xerces.internal.jaxp.SAXParserFactoryImpl $CATALINA_OPTS "' >> tomcat/bin/setenv.sh

	if [[ -n "${repository_httpclient_proxy_nonproxyhosts}" ]]  ; then
		sed -i -r "s|http\.nonProxyHosts=.*\"|http.nonProxyHosts=${repository_httpclient_proxy_nonproxyhosts} "'$CATALINA_OPTS'" \"|" tomcat/bin/setenv.sh
    grep -q 'http\.nonProxyHosts' tomcat/bin/setenv.sh || echo "CATALINA_OPTS=\"-Dhttp.nonProxyHosts=${repository_httpclient_proxy_nonproxyhosts} "'$CATALINA_OPTS'" \"" >> tomcat/bin/setenv.sh

		sed -i -r "s|https\.nonProxyHosts=.*\"|https.nonProxyHosts=${repository_httpclient_proxy_nonproxyhosts} "'$CATALINA_OPTS'" \"|" tomcat/bin/setenv.sh
		grep -q 'https\.nonProxyHosts' tomcat/bin/setenv.sh || echo "CATALINA_OPTS=\"-Dhttps.nonProxyHosts=${repository_httpclient_proxy_nonproxyhosts} "'$CATALINA_OPTS'" \"" >> tomcat/bin/setenv.sh
	fi

	if [[ -n "${repository_httpclient_proxy_proxyhost}" ]] ; then
		sed -i -r "s|http\.proxyHost=.*\"|http.proxyHost=${repository_httpclient_proxy_proxyhost} "'$CATALINA_OPTS'" \"|" tomcat/bin/setenv.sh
    grep -q 'http\.proxyHost' tomcat/bin/setenv.sh || echo "CATALINA_OPTS=\"-Dhttp.proxyHost=${repository_httpclient_proxy_proxyhost} "'$CATALINA_OPTS'" \"" >> tomcat/bin/setenv.sh

		sed -i -r "s|https\.proxyHost=.*\"|https.proxyHost=${repository_httpclient_proxy_proxyhost} "'$CATALINA_OPTS'" \"|" tomcat/bin/setenv.sh
		grep -q 'https\.proxyHost' tomcat/bin/setenv.sh || echo "CATALINA_OPTS=\"-Dhttps.proxyHost=${repository_httpclient_proxy_proxyhost} "'$CATALINA_OPTS'" \"" >> tomcat/bin/setenv.sh
	fi

	if [[ -n "${repository_httpclient_proxy_proxypass}" ]] ; then
		sed -i -r "s|http\.proxyPass=.*\"|http.proxyPass=${repository_httpclient_proxy_proxypass} "'$CATALINA_OPTS'" \"|" tomcat/bin/setenv.sh
    grep -q 'http\.proxyPass' tomcat/bin/setenv.sh || echo "CATALINA_OPTS=\"-Dhttp.proxyPass=${repository_httpclient_proxy_proxypass} "'$CATALINA_OPTS'" \"" >> tomcat/bin/setenv.sh

		sed -i -r "s|https\.proxyPass=.*\"|https.proxyPass=${repository_httpclient_proxy_proxypass}\"|" tomcat/bin/setenv.sh
		grep -q 'https\.proxyPass' tomcat/bin/setenv.sh || echo "CATALINA_OPTS=\"-Dhttps.proxyPass=${repository_httpclient_proxy_proxypass} "'$CATALINA_OPTS'" \"" >> tomcat/bin/setenv.sh
	fi

	if [[ -n "${repository_httpclient_proxy_proxyport}" ]] ; then
		sed -i -r "s|http\.proxyPort=.*\"|http.proxyPort=${repository_httpclient_proxy_proxyport} "'$CATALINA_OPTS'" \"|" tomcat/bin/setenv.sh
    grep -q 'http\.proxyPort' tomcat/bin/setenv.sh || echo "CATALINA_OPTS=\"-Dhttp.proxyPort=${repository_httpclient_proxy_proxyport} "'$CATALINA_OPTS'" \"" >> tomcat/bin/setenv.sh

		sed -i -r "s|https\.proxyPort=.*\"|https.proxyPort=${repository_httpclient_proxy_proxyport} "'$CATALINA_OPTS'" \"|" tomcat/bin/setenv.sh
		grep -q 'https\.proxyPort' tomcat/bin/setenv.sh || echo "CATALINA_OPTS=\"-Dhttps.proxyPort=${repository_httpclient_proxy_proxyport} "'$CATALINA_OPTS'" \"" >> tomcat/bin/setenv.sh
	fi

	if [[ -n "${repository_httpclient_proxy_proxyuser}" ]] ; then
		sed -i -r "s|http\.proxyUser=.*\"|http.proxyUser=${repository_httpclient_proxy_proxyuser} "'$CATALINA_OPTS'" \"|" tomcat/bin/setenv.sh
    grep -q 'http\.proxyUser' tomcat/bin/setenv.sh || echo "CATALINA_OPTS=\"-Dhttp.proxyUser=${repository_httpclient_proxy_proxyuser} "'$CATALINA_OPTS'" \"" >> tomcat/bin/setenv.sh

		sed -i -r "s|https\.proxyUser=.*\"|https.proxyUser=${repository_httpclient_proxy_proxyuser} "'$CATALINA_OPTS'" \"|" tomcat/bin/setenv.sh
		grep -q 'https\.proxyUser' tomcat/bin/setenv.sh || echo "CATALINA_OPTS=\"-Dhttps.proxyUser=${repository_httpclient_proxy_proxyuser} "'$CATALINA_OPTS'" \"" >> tomcat/bin/setenv.sh
	fi


	### Alfresco platform ################################################################################################

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

	### edu-sharing ######################################################################################################

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

	if [[ -n "${my_home_auth}" ]] ; then
		xmlstarlet ed -L \
			-d '/properties/entry[@key="allowed_authentication_types"]' \
			-s '/properties' -t elem -n "entry" -v "${my_home_auth}" \
			--var entry '$prev' \
			-i '$entry' -t attr -n "key" -v "allowed_authentication_types" \
			tomcat/shared/classes/homeApplication.properties.xml

		if [[ "${my_home_auth}" == "shibboleth" ]] ; then
			sed -i -r 's|<!--\s*SAML||g' tomcat/webapps/edu-sharing/WEB-INF/web.xml
			sed -i -r 's|SAML\s*-->||g'  tomcat/webapps/edu-sharing/WEB-INF/web.xml
			xmlstarlet ed -L \
				-s '/config/values' -t elem -n 'loginUrl' -v '' \
				-d '/config/values/loginUrl[position() != 1]' \
				-u '/config/values/loginUrl' -v '/edu-sharing/shibboleth' \
				-s '/config/values' -t elem -n 'logout' -v '' \
				-d '/config/values/logout[position() != 1]' \
				-s '/config/values/logout' -t elem -n 'url' -v '' \
				-d '/config/values/logout/url[position() != 1]' \
				-u '/config/values/logout/url' -v '/edu-sharing/saml/logout' \
				-s '/config/values/logout' -t elem -n 'destroySession' -v '' \
				-d '/config/values/logout/destroySession[position() != 1]' \
				-u '/config/values/logout/destroySession' -v 'false' \
				tomcat/shared/classes/config/client.config.xml
		fi
	fi


	if [[ -n "${my_home_provider}" ]] ; then
		xmlstarlet ed -L \
    		-d '/properties/entry[@key="remote_provider"]' \
    		-s '/properties' -t elem -n "entry" -v "${my_home_provider}" \
    		--var entry '$prev' \
    		-i '$entry' -t attr -n "key" -v "remote_provider" \
    		tomcat/shared/classes/homeApplication.properties.xml
	fi

	if [[ -n "${repository_httpclient_disablesni4hosts}" ]] ; then
		hocon -f tomcat/shared/classes/config/edu-sharing.deployment.conf \
			set "repository.httpclient.disableSNI4Hosts" "${repository_httpclient_disablesni4hosts}"
	fi

	if [[ -n "${repository_httpclient_proxy_host}" ]] ; then
		hocon -f tomcat/shared/classes/config/edu-sharing.deployment.conf \
			set "repository.httpclient.proxy.host" "${repository_httpclient_proxy_host}"
	fi

  if [[ -n "${repository_httpclient_proxy_nonproxyhosts}" ]]  ; then
  	hocon -f tomcat/shared/classes/config/edu-sharing.deployment.conf \
  		set "repository.httpclient.proxy.nonproxyhosts" "${repository_httpclient_proxy_nonproxyhosts}"
  fi

  if [[ -n "${repository_httpclient_proxy_proxyhost}" ]] ; then
  	hocon -f tomcat/shared/classes/config/edu-sharing.deployment.conf \
  		set "repository.httpclient.proxy.proxyhost" "${repository_httpclient_proxy_proxyhost}"
  fi

  if [[ -n "${repository_httpclient_proxy_proxypass}" ]] ; then
  	hocon -f tomcat/shared/classes/config/edu-sharing.deployment.conf \
  		set "repository.httpclient.proxy.proxypass" "${repository_httpclient_proxy_proxypass}"
  fi

  if [[ -n "${repository_httpclient_proxy_proxyport}" ]] ; then
  	hocon -f tomcat/shared/classes/config/edu-sharing.deployment.conf \
  		set "repository.httpclient.proxy.proxyport" "${repository_httpclient_proxy_proxyport}"
  fi

  if [[ -n "${repository_httpclient_proxy_proxyuser}" ]] ; then
  	hocon -f tomcat/shared/classes/config/edu-sharing.deployment.conf \
  		set "repository.httpclient.proxy.proxyuser" "${repository_httpclient_proxy_proxyuser}"
  fi

	popd

	######################################################################################################################

	# LOAD PLUGIN CONFIG
	if [[ -d "$execution_folder/plugins" ]] ; then
		for plugin in "$execution_folder"/plugins/plugin-*/setup_config.sh; do
  		 [[ -f "$plugin" ]] && {
  		 		source "$plugin" || exit 1
  		 }
  	done
  fi

}

########################################################################################################################

pushd "$ALF_HOME" &> /dev/null
if [[ ! -f alfresco.sh ]] ; then
	echo ""
	echo "Env ALF_HOME must point to the home directory of your Alfresco Platform!"
	exit 1
fi

if [[ ! -d tomcat/webapps/alfresco || ! -d tomcat/webapps/solr4 ]] ; then
	echo ""
	echo "You must have started the Alfresco Platform at least once before you can run the installation."
	exit 1
fi

if [[ "$(./alfresco.sh status tomcat)" != "tomcat not running" ]] ; then
	echo ""
	echo "Please stop Tomcat before you can run the installation!"
	exit 1
fi

if [[ "$(./alfresco.sh status postgresql)" != "postgresql not running" ]] ; then
	echo ""
	echo "Please stop Postgresql before you can run the installation!"
	exit 1
fi
popd

########################################################################################################################

if [[ use_local_maven_cache -eq 1 ]] ; then
	echo "- WARNING local maven cache is used"
else
	echo "- download edu-sharing repository distribution"
	mvn -q dependency:get \
		-Dartifact=org.edu_sharing:edu_sharing-community-deploy-installer-repository-distribution:${org.edu_sharing:edu_sharing-community-deploy-installer-repository-distribution:tar.gz:bin.version}:tar.gz:bin \
		-DremoteRepositories=edusharing-remote::::https://artifacts.edu-sharing.com/repository/maven-remote/ \
		-Dtransitive=false
fi


pushd "$ALF_HOME" &> /dev/null

echo "- unpack edu-sharing repository distribution"
mvn -q dependency:copy \
	-Dartifact=org.edu_sharing:edu_sharing-community-deploy-installer-repository-distribution:${org.edu_sharing:edu_sharing-community-deploy-installer-repository-distribution:tar.gz:bin.version}:tar.gz:bin \
	-DoutputDirectory=.


if [[ -f "$alfresco_base_image" ]] ; then

	echo ""
	echo "Update ... "

	echo "- make a snapshot of edu-sharing platform"
	snapshot_name="$execution_folder/snapshots/edu-sharing-SNAPSHOT-$(date "+%Y.%m.%d-%H.%M.%S").tar.gz"
	mkdir -p "$(dirname "$snapshot_name")"
	tar -czf "$snapshot_name" amps tomcat solr4

	echo "- cleanup amps and tomcat"
	rm -rf amps
	rm -rf tomcat
	rm -rf solr4

	echo "- restore amps and tomcat"
	tar -zxf "$alfresco_base_image"

	install_edu_sharing

	echo "- restore persistent data of Alfresco platform"
	if [[ $(tar -tf  "$snapshot_name" | grep 'tomcat/shared/classes/config/persistent' | wc -l) -gt 0 ]]; then
		tar -zxf "$snapshot_name" tomcat/shared/classes/config/persistent -C tomcat/shared/classes/config/
	else
		echo "nothing to restore"
	fi

	echo "- delete old edu-sharing SNAPSHOTS (keep 3 backups)"
	pushd .. &> /dev/null
	ls -pt | grep -v / | grep "edu-sharing-SNAPSHOT" | tail -n +4 | xargs -I {} rm {}
	popd

else

	echo ""
	echo "Install ... "

	echo "- make a snapshot of Alfresco platform"
	mkdir -p "$(dirname "$alfresco_base_image")"
	tar -czf "$alfresco_base_image" amps tomcat solr4

	install_edu_sharing

fi

echo "- you may need to delete the solr4 index, model and content folders!"

rm edu_sharing-community-deploy-installer-repository-distribution-${org.edu_sharing:edu_sharing-community-deploy-installer-repository-distribution:tar.gz:bin.version}-bin.tar.gz
popd

info >> "$execution_folder/install_log-$(date "+%Y.%m.%d-%H.%M.%S").txt"
info

echo "- done."
exit

########################################################################################################################

#!/bin/bash
set -e
set -o pipefail

########################################################################################################################

# load the default configuration
if [[ -f ".env.base" ]] ; then
	echo "Load .env.base"
	source .env.base
fi

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

	echo "--all"
  echo "  Setup all products"
	echo ""

  echo "--repository"
  echo "  Setup edu-sharing repository"
	echo ""

#  echo "--elastictracker"
#  echo "  Setup elastic tracker"
#  echo ""

}

########################################################################################################################

REPOSITORY=$((1<<0));
#ELASTIC_TRACKER=$((1<<1));

install_options=0
use_local_maven_cache=0

while true; do
	flag="$1"
	shift || break

	case "$flag" in
			--help|'-?') usage && exit 0 ;;
			--file|-f) source "$1" && shift	;;
			--local) use_local_maven_cache=1 ;;
			--all) install_options=$((REPOSITORY)) ;;
#			--all) install_options=$((REPOSITORY | ELASTIC_TRACKER)) ;;
			--repository) install_options=$((install_options | REPOSITORY)) ;;
#			--elastictracker) install_options=$((install_options | ELASTIC_TRACKER)) ;;
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

repository_httpclient_disablesni4hosts="${REPOSITORY_HTTPCLIENT_DISABLE_SNI4HOSTS:-}"
repository_httpclient_proxy_host="${REPOSITORY_HTTPCLIENT_PROXY_HOST:-}"
repository_httpclient_proxy_nonproxyhosts="${REPOSITORY_HTTPCLIENT_PROXY_NONPROXYHOSTS:-}"
repository_httpclient_proxy_proxyhost="${REPOSITORY_HTTPCLIENT_PROXY_PROXYHOST:-}"
repository_httpclient_proxy_proxyport="${REPOSITORY_HTTPCLIENT_PROXY_PROXYPORT:-}"
repository_httpclient_proxy_proxyuser="${REPOSITORY_HTTPCLIENT_PROXY_PROXYUSER:-}"
repository_httpclient_proxy_proxypass="${REPOSITORY_HTTPCLIENT_PROXY_PROXYPASS:-}"

repository_search_solr4_host="${REPOSITORY_SEARCH_SOLR4_HOST:-"127.0.0.1"}"
repository_search_solr4_port="${REPOSITORY_SEARCH_SOLR4_PORT:-8080}"

repository_transform_host="${REPOSITORY_TRANSFORM_HOST:-"127.0.0.1"}"
repository_transform_port="${REPOSITORY_TRANSFORM_PORT:-8100}"

repository_contentstore="${REPOSITORY_SERVICE_CONTENTSTORE:-}"
repository_contentstore_deleted="${REPOSITORY_SERVICE_CONTENTSTORE_DELETED:-}"

#repository_search_elastic_tracker_server_address="${REPOSITORY_SEARCH_ELASTIC_TRACKER_SERVER_HOST:-"127.0.0.1"}"
#repository_search_elastic_tracker_server_port="${REPOSITORY_SEARCH_ELASTIC_TRACKER_SERVER_PORT:-8081}"
#repository_search_elastic_tracker_management_server_address="${REPOSITORY_SEARCH_ELASTIC_TRACKER_MANAGEMENT_SERVER_HOST:-"127.0.0.1"}"
#repository_search_elastic_tracker_management_server_port="${REPOSITORY_SEARCH_ELASTIC_TRACKER_MANAGEMENT_SERVER_PORT:-8082}"

#repository_search_elastic_host="${REPOSITORY_SEARCH_ELASTIC_HOST:-"127.0.0.1"}"
#repository_search_elastic_port="${REPOSITORY_SEARCH_ELASTIC_PORT:-9200}"
#repository_search_elastic_base="http://${repository_search_elastic_host}:${repository_search_elastic_port}"
#repository_search_elastic_index_shards="${REPOSITORY_SEARCH_ELASTIC_INDEX_SHARDS:-1}"
#repository_search_elastic_index_replicas="${REPOSITORY_SEARCH_ELASTIC_INDEX_REPLICAS:-1}"

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
#  echo "#########################################################################"
#  echo ""
#  echo "elastic search:"
#  echo ""
#  echo "  Host:                ${repository_search_elastic_host}"
#  echo "  Port:                ${repository_search_elastic_port}"
#  echo ""
#  echo "  Index:"
#  echo ""
#  echo "    Shards:            ${repository_search_elastic_index_shards}"
#  echo "    Replicas:          ${repository_search_elastic_index_replicas}"
#  echo ""
#  echo "  tracker:"
#  echo ""
#  echo "    Host:              ${repository_search_elastic_tracker_server_address}"
#  echo "    Port:              ${repository_search_elastic_tracker_server_port}"
#  echo ""
#  echo "    Management:"
#  echo ""
#  echo "      Host:            ${repository_search_elastic_tracker_management_server_address}"
#  echo "      Port:            ${repository_search_elastic_tracker_management_server_port}"
#  echo ""
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

########################################################################################################################

install_edu_sharing() {
	pushd "$ALF_HOME"

	echo "- clean up outdated libraries"
	rm -f tomcat/lib/postgresql-*
	rm -f tomcat/webapps/alfresco/WEB-INF/lib/commons-lang3-*
	rm -f tomcat/webapps/alfresco/WEB-INF/lib/hazelcast-*
	rm -f tomcat/webapps/alfresco/WEB-INF/lib/jackson-*

	######################################################################################################################

	echo "- unpack edu-sharing repository"
	tar xzf edu_sharing-community-deploy-installer-repository-distribution-${org.edu_sharing:edu_sharing-community-deploy-installer-repository-distribution:tar.gz:bin.version}-bin.tar.gz # \
#		--exclude='./elastictracker'

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
		sed -i -r "s|http\.nonProxyHosts=.*\"|http.nonProxyHosts=${repository_httpclient_proxy_nonproxyhosts}\"|" tomcat/bin/setenv.sh
    grep -q 'http\.nonProxyHosts' tomcat/bin/setenv.sh || echo "CATALINA_OPTS=\"-Dhttp.nonProxyHosts ${repository_httpclient_proxy_nonproxyhosts}\"" >> tomcat/bin/setenv.sh

		sed -i -r "s|https\.nonProxyHosts=.*\"|https.nonProxyHosts=${repository_httpclient_proxy_nonproxyhosts}\"|" tomcat/bin/setenv.sh
		grep -q 'https\.nonProxyHosts' tomcat/bin/setenv.sh || echo "CATALINA_OPTS=\"-Dhttps.nonProxyHosts ${repository_httpclient_proxy_nonproxyhosts}\"" >> tomcat/bin/setenv.sh
	fi

	if [[ -n "${repository_httpclient_proxy_proxyhost}" ]] ; then
		sed -i -r "s|http\.proxyHost=.*\"|http.proxyHost=${repository_httpclient_proxy_proxyhost}\"|" tomcat/bin/setenv.sh
    grep -q 'http\.proxyHost' tomcat/bin/setenv.sh || echo "CATALINA_OPTS=\"-Dhttp.proxyHost ${repository_httpclient_proxy_proxyhost}\"" >> tomcat/bin/setenv.sh

		sed -i -r "s|https\.proxyHost=.*\"|https.proxyHost=${repository_httpclient_proxy_proxyhost}\"|" tomcat/bin/setenv.sh
		grep -q 'https\.proxyHost' tomcat/bin/setenv.sh || echo "CATALINA_OPTS=\"-Dhttps.proxyHost ${repository_httpclient_proxy_proxyhost}\"" >> tomcat/bin/setenv.sh
	fi

	if [[ -n "${repository_httpclient_proxy_proxypass}" ]] ; then
		sed -i -r "s|http\.proxyPass=.*\"|http.proxyPass=${repository_httpclient_proxy_proxypass}\"|" tomcat/bin/setenv.sh
    grep -q 'http\.proxyPass' tomcat/bin/setenv.sh || echo "CATALINA_OPTS=\"-Dhttp.proxyPass ${repository_httpclient_proxy_proxypass}\"" >> tomcat/bin/setenv.sh

		sed -i -r "s|https\.proxyPass=.*\"|https.proxyPass=${repository_httpclient_proxy_proxypass}\"|" tomcat/bin/setenv.sh
		grep -q 'https\.proxyPass' tomcat/bin/setenv.sh || echo "CATALINA_OPTS=\"-Dhttps.proxyPass ${repository_httpclient_proxy_proxypass}\"" >> tomcat/bin/setenv.sh
	fi

	if [[ -n "${repository_httpclient_proxy_proxyport}" ]] ; then
		sed -i -r "s|http\.proxyPort=.*\"|http.proxyPort=${repository_httpclient_proxy_proxyport}\"|" tomcat/bin/setenv.sh
    grep -q 'http\.proxyPort' tomcat/bin/setenv.sh || echo "CATALINA_OPTS=\"-Dhttp.proxyPort ${repository_httpclient_proxy_proxyport}\"" >> tomcat/bin/setenv.sh

		sed -i -r "s|https\.proxyPort=.*\"|https.proxyPort=${repository_httpclient_proxy_proxyport}\"|" tomcat/bin/setenv.sh
		grep -q 'https\.proxyPort' tomcat/bin/setenv.sh || echo "CATALINA_OPTS=\"-Dhttps.proxyPort ${repository_httpclient_proxy_proxyport}\"" >> tomcat/bin/setenv.sh
	fi

	if [[ -n "${repository_httpclient_proxy_proxyuser}" ]] ; then
		sed -i -r "s|http\.proxyUser=.*\"|http.proxyUser=${repository_httpclient_proxy_proxyuser}\"|" tomcat/bin/setenv.sh
    grep -q 'http\.proxyUser' tomcat/bin/setenv.sh || echo "CATALINA_OPTS=\"-Dhttp.proxyUser ${repository_httpclient_proxy_proxyuser}\"" >> tomcat/bin/setenv.sh

		sed -i -r "s|https\.proxyUser=.*\"|https.proxyUser=${repository_httpclient_proxy_proxyuser}\"|" tomcat/bin/setenv.sh
		grep -q 'https\.proxyUser' tomcat/bin/setenv.sh || echo "CATALINA_OPTS=\"-Dhttps.proxyUser ${repository_httpclient_proxy_proxyuser}\"" >> tomcat/bin/setenv.sh
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

#	hocon -f tomcat/shared/classes/config/edu-sharing.deployment.conf \
#		set "elasticsearch.servers" '["'"${repository_search_elastic_host}:${repository_search_elastic_port}"'"]'

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
}

########################################################################################################################

#can_install_elastic_tracker() {
#	pushd "$ALF_HOME"
#
#		#if [[ -f "/etc/systemd/system/elastictracker" && "$(systemctl status elastictracker)" == "elastictracker is running"  ]] ; then
#		if [[ -f "/etc/systemd/system/elastictracker" && $(systemctl is-active --quiet elastictracker) ]] ; then
#  		echo ""
#  		echo "You must stop the elastic tracker before you can run the installation."
#  		exit 1
#  	fi
#
#	popd
#}

########################################################################################################################

#install_elastic_tracker() {
#	pushd "$ALF_HOME"
#
#	echo "- remove elastictracker"
#	rm -rf elastictracker
#
#	### elastic tracker - fix security issues ############################################################################
#
#	echo "- create worker user"
#	id -u elastictracker &>/dev/null || adduser --home=$ALF_HOME/elastictracker --disabled-password --gecos "" --shell=/bin/bash elastictracker
#	chown -RL elastictracker:elastictracker /tmp
#
#	### elastic tracker - installation ###################################################################################
#
#	echo "- unpack edu-sharing elastic tracker"
#	tar -zxf edu_sharing-community-deploy-installer-repository-distribution-${org.edu_sharing:edu_sharing-community-deploy-installer-repository-distribution:tar.gz:bin.version}-bin.tar.gz \
#			elastictracker
#
#	chown -RL elastictracker:elastictracker ./elastictracker
#
#	###  elastic tracker #################################################################################################
#
#	echo "- update elastic tracker env"
#	elasticApplicationProps="elastictracker/application.properties"
#	touch "${elasticApplicationProps}"
#
#	sed -i -r 's|^[#]*\s*server\.address=.*|alfresco.address='"${repository_search_elastic_tracker_server_address}"'|' "${elasticApplicationProps}"
#	grep -q '^[#]*\s*server\.address=' "${elasticApplicationProps}" || echo "server.address=${repository_search_elastic_tracker_server_address}" >>"${elasticApplicationProps}"
#
#	sed -i -r 's|^[#]*\s*server\.port=.*|alfresco.host='"${repository_search_elastic_tracker_server_port}"'|' "${elasticApplicationProps}"
#	grep -q '^[#]*\s*server\.port=' "${elasticApplicationProps}" || echo "server.port=${repository_search_elastic_tracker_server_port}" >>"${elasticApplicationProps}"
#
#	sed -i -r 's|^[#]*\s*management\.server.\address=.*|alfresco.host='"${repository_search_elastic_tracker_management_server_address}"'|' "${elasticApplicationProps}"
#	grep -q '^[#]*\s*management\.server.\address=' "${elasticApplicationProps}" || echo "management.server.address=${repository_search_elastic_tracker_management_server_address}" >>"${elasticApplicationProps}"
#
#	sed -i -r 's|^[#]*\s*management\.server.\port=.*|alfresco.host='"${repository_search_elastic_tracker_management_server_port}"'|' "${elasticApplicationProps}"
#	grep -q '^[#]*\s*management\.server.\port=' "${elasticApplicationProps}" || echo "management.server.port=${repository_search_elastic_tracker_management_server_port}" >>"${elasticApplicationProps}"
#
#	sed -i -r 's|^[#]*\s*alfresco\.host=.*|alfresco.host='"${my_host_internal}"'|' "${elasticApplicationProps}"
#	grep -q '^[#]*\s*alfresco\.host=' "${elasticApplicationProps}" || echo "alfresco.host=${my_host_internal}" >>"${elasticApplicationProps}"
#
#	sed -i -r 's|^[#]*\s*alfresco\.port=.*|alfresco.port='"${my_port_internal}"'|' "${elasticApplicationProps}"
#	grep -q '^[#]*\s*alfresco\.port=' "${elasticApplicationProps}"|| echo "alfresco.port=${my_port_internal}" >>"${elasticApplicationProps}"
#
#	sed -i -r 's|^[#]*\s*alfresco\.password=.*|alfresco.password='"${my_admin_pass}"'|' "${elasticApplicationProps}"
#	grep -q '^[#]*\s*alfresco\.password=' "${elasticApplicationProps}" || echo "alfresco.password=${my_admin_pass}" >>"${elasticApplicationProps}"
#
#	sed -i -r 's|^[#]*\s*elastic\.host=.*|elastic.host='"${repository_search_elastic_host}"'|' "${elasticApplicationProps}"
#	grep -q '^[#]*\s*elastic\.host=' "${elasticApplicationProps}" || echo "elastic.host=${repository_search_elastic_host}" >>"${elasticApplicationProps}"
#
#	sed -i -r 's|^[#]*\s*elastic\.port=.*|elastic.port='"${repository_search_elastic_port}"'|' "${elasticApplicationProps}"
#	grep -q '^[#]*\s*elastic\.port=' "${elasticApplicationProps}" || echo "elastic.port=${repository_search_elastic_port}" >>"${elasticApplicationProps}"
#
#	sed -i -r 's|^[#]*\s*elastic\.index.\number_of_shards=.*|elastic.index.number_of_shards='"${repository_search_elastic_index_shards}"'|' "${elasticApplicationProps}"
# 	grep -q '^[#]*\s*elastic\.index.\number_of_shards=' "${elasticApplicationProps}" || echo "elastic.index.number_of_shards=${repository_search_elastic_index_shards}" >>"${elasticApplicationProps}"
#
#	sed -i -r 's|^[#]*\s*elastic.\index\.number_of_replicas=.*|elastic.index.number_of_replicas='"${repository_search_elastic_index_replicas}"'|' "${elasticApplicationProps}"
#	grep -q '^[#]*\s*elastic.\index\.number_of_replicas=' "${elasticApplicationProps}" || echo "elastic.index.number_of_replicas=${repository_search_elastic_index_replicas}" >>"${elasticApplicationProps}"
#
#	### elastic tracker - register systemd service #######################################################################
#
#	#ln -s "${ALF_HOME}/elastictracker/tracker.jar" /etc/init.d/elastictracker
#
#	pushd /etc/systemd/system
#
#	elastic_tracker_jar=edu_sharing-community-repository-backend-search-elastic-tracker-${org.edu_sharing:edu_sharing-community-repository-backend-search-elastic-tracker:jar.version}.jar
#
#	# TODO Logfile
#	if [[ ! -f elastictracker.service ]]; then
#		echo "- create systemd service"
#		touch elastictracker.service
#		{
#			echo "[Unit]"
#			echo "Description=edu-sharing elastic tracker"
#			echo "After=syslog.target network.target postgresql.service"
#			echo ""
#			echo "[Service]"
#			echo "WorkingDirectory=${ALF_HOME}/elastictracker"
#			echo "User=elastictracker"
#			echo "ExecStart=/usr/bin/java -jar ${ALF_HOME}/elastictracker/${elastic_tracker_jar}"
#			echo "SuccessExitStatus=143"
#			echo ""
#			echo "[Install]"
#			echo "WantedBy=multi-user.target"
#		 } >> elastictracker.service
#	else
#		echo "- update systemd service"
#
#		sed -i -r 's|^WorkingDirectory=.*|WorkingDirectory='"${ALF_HOME}/elastictracker"'|' elastictracker.service
#    grep -q '^WorkingDirectory=' elastictracker.service || echo "WorkingDirectory=${ALF_HOME}/elastictracker" >> elastictracker.service
#
#		sed -i -r 's|^ExecStart=.*|ExecStart='"${ALF_HOME}/elastictracker/${elastic_tracker_jar}"'|' elastictracker.service
#    grep -q '^ExecStart=' elastictracker.service || echo "ExecStart=/usr/bin/java -jar ${ALF_HOME}/elastictracker/${elastic_tracker_jar}" >> elastictracker.service
#	fi
#
#	popd
#	popd
#}

########################################################################################################################

can_install_repository(){
	pushd "$ALF_HOME"
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
}

########################################################################################################################

install_repository() {

	pushd "$ALF_HOME"

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

	echo "- you may need to delete the solr4 index, model and content folders!"
	popd
}

########################################################################################################################

if [[ $install_options == 0 ]]  ; then
	usage
	exit 1
fi


if [[ $((install_options & REPOSITORY)) != 0 ]]; then
	can_install_repository
fi

#if [[ $((install_options & ELASTIC_TRACKER)) != 0 ]]; then
#	can_install_elastic_tracker
#fi

########################################################################################################################

if [[ use_local_maven_cache -eq 1 ]] ; then
	echo "- WARNING local maven cache is used"
else
	echo "- download edu-sharing repository distribution"
	mvn -q dependency:get \
		-Dartifact=org.edu_sharing:edu_sharing-community-deploy-installer-repository-distribution:${org.edu_sharing:edu_sharing-community-deploy-installer-repository-distribution:tar.gz:bin.version}:tar.gz:bin \
		-DremoteRepositories=myreleases::::https://artifacts.edu-sharing.com/repository/community-releases/,mysnapshots::::https://artifacts.edu-sharing.com/repository/community-snapshots/ \
		-Dtransitive=false
fi

echo "- unpack edu-sharing repository distribution"
mvn -q dependency:copy \
	-Dartifact=org.edu_sharing:edu_sharing-community-deploy-installer-repository-distribution:${org.edu_sharing:edu_sharing-community-deploy-installer-repository-distribution:tar.gz:bin.version}:tar.gz:bin \
	-DoutputDirectory=.

if [[ $((install_options & REPOSITORY)) != 0 ]]; then
	install_repository
fi

#if [[ $((install_options & ELASTIC_TRACKER)) != 0 ]]; then
#	install_elastic_tracker
#fi

rm edu_sharing-community-deploy-installer-repository-distribution-${org.edu_sharing:edu_sharing-community-deploy-installer-repository-distribution:tar.gz:bin.version}-bin.tar.gz

info >> "install_log-$(date "+%Y.%m.%d-%H.%M.%S").txt"
info

echo "- done."
exit

########################################################################################################################

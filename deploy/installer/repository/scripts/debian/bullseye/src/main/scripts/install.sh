#!/bin/bash
set -e
set -o pipefail

########################################################################################################################

source bin/installer/artifact.sh

########################################################################################################################

if [[ -z $ALF_HOME ]] ; then
	echo ""
	echo "Env ALF_HOME is not defined! It must point to the home directory of your Alfresco Platform!"
	exit 1
fi

if [[ ! -f alfresco.sh ]] ; then
	echo ""
	echo "Missing $ALF_HOME/alfresco.sh."
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

########################################################################################################################

execution_folder="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

pushd "$execution_folder" &> /dev/null

# load the default configuration
if [[ -f ".env.base" ]] ; then
	echo "Load .env.base"
	source .env.base &> /dev/null
fi

if [[ -d plugins ]] ; then
	for plugin in plugins/plugin-*/.env.base; do
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
	echo ""
	echo "--backup"
	echo "  Define the path to the initial backup"
}

########################################################################################################################

use_local_maven_cache=0
backup="$execution_folder/backup-for-edu-sharing_DO-NOT-DELETE-ME.tar.gz"

while true; do
	flag="$1"
	shift || break

	case "$flag" in
			--help|'-?') usage && exit 0 ;;
			--file|-f) source "$1" && shift	;;
			--local) use_local_maven_cache=1 ;;
			--backup) backup="$1" && shift;;
			*) {
				echo "error: unknown flag: $flag"
				usage
			}  >&2
			exit 1 ;;
	esac
done

########################################################################################################################

my_home_appid="${REPOSITORY_SERVICE_HOME_APPID:-"local"}"
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

pushd "$execution_folder" &> /dev/null
if [[ -d plugins ]] ; then
	for plugin in plugins/plugin-*/load_config.sh; do
		 [[ -f "$plugin" ]] && {
		 		source "$plugin" || exit 1
		 }
	done
fi
popd

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

	pushd "$execution_folder" &> /dev/null
  if [[ -d plugins ]] ; then
  	for plugin in plugins/plugin-*/print_config.sh; do
  		 [[ -f "$plugin" ]] && {
  		 		source "$plugin" || exit 1
  		 }
  	done
  fi
  popd

  echo "#########################################################################"
  echo ""
  echo ""
}

########################################################################################################################

install_edu_sharing() {

	echo "- unpack edu-sharing repository distribution"

	mkdir -p dist
	tar -xzf "${ARTIFACT_ID}-${ARTIFACT_VERSION}-bin.tar.gz" -C dist
	rm "${ARTIFACT_ID}-${ARTIFACT_VERSION}-bin.tar.gz"

	######################################################################################################################

	echo "- cleanup"

	rm -rf amps/*
	rm -rf solr4/*
	rm -rf tomcat/*

	######################################################################################################################

	echo "- install tomcat"

  tar -xzf dist/artifacts/tomcat-${tomcat.version}.tar.gz -C tomcat --strip 1 --exclude apache-tomcat-${tomcat.version}/webapps

	######################################################################################################################

	echo "- restore based on $backup"

	tar -zxf "$backup"

	######################################################################################################################

	echo "- install edu-sharing"

	rm -f tomcat/webapps/alfresco/WEB-INF/lib/commons-lang3-*
	rm -f tomcat/webapps/alfresco/WEB-INF/lib/hazelcast-*
	rm -f tomcat/webapps/alfresco/WEB-INF/lib/jackson-*
	rm -f tomcat/webapps/alfresco/WEB-INF/lib/log4j-*
	rm -f tomcat/webapps/alfresco/WEB-INF/lib/slf4j-*

	cp -r dist/tomcat/* tomcat

	######################################################################################################################

	echo "- install amps"

	cp -r dist/amps/* amps

	if [[ -d amps/alfresco/0 ]]; then
	  java -jar bin/alfresco-mmt.jar install amps/alfresco/0 tomcat/webapps/alfresco -directory -nobackup -force 2> /dev/null
	fi

	if [[ -d amps/alfresco/1 ]]; then
	  java -jar bin/alfresco-mmt.jar install amps/alfresco/1 tomcat/webapps/alfresco -directory -nobackup -force 2> /dev/null
  fi

  if [[ -d amps/edu-sharing/1 ]]; then
    java -jar bin/alfresco-mmt.jar install amps/edu-sharing/1 tomcat/webapps/edu-sharing -directory -nobackup -force 2> /dev/null
  fi

	######################################################################################################################

	rm -rf dist
}

config_edu_sharing() {

	setEnvSh="tomcat/bin/setenv.sh"

	catProps="tomcat/conf/catalina.properties"
	catServe="tomcat/conf/server.xml"

	catCxAlf="tomcat/conf/Catalina/localhost/alfresco.xml"
	catCxShr="tomcat/conf/Catalina/localhost/share.xml"
	catCxSol="tomcat/conf/Catalina/localhost/solr4.xml"

	eduCConf="tomcat/shared/classes/config/defaults/client.config.xml"

	alfProps="tomcat/shared/classes/config/cluster/alfresco-global.properties"
	eduSConf="tomcat/shared/classes/config/cluster/edu-sharing.deployment.conf"
	eduProps="tomcat/shared/classes/config/cluster/applications/homeApplication.properties.xml"

	eduWebXm="tomcat/webapps/edu-sharing/WEB-INF/web.xml"

	solr4Arc="solr4/archive-SpacesStore/conf/solrcore.properties"
	solr4Wor="solr4/workspace-SpacesStore/conf/solrcore.properties"

	### Tomcat ###########################################################################################################

	echo "- update ${catProps}"

  mkdir -p tomcat/shared/classes
  mkdir -p tomcat/shared/lib
  sed -i -r \
		's|^[#]*\s*shared\.loader=.*|shared.loader=${catalina.base}/shared/classes,${catalina.base}/shared/lib/*.jar|' \
		${catProps}

	echo "- update ${catServe}"

  xmlstarlet ed -L \
    -d '/Server/Service[@name="Catalina"]/Connector[@port="8080"]' \
		-s '/Server/Service[@name="Catalina"]' -t elem -n "Connector" \
		--var http '$prev' \
		-i '$http' -t attr -n "protocol" -v "HTTP/1.1" \
		-i '$http' -t attr -n "address" -v "0.0.0.0" \
		-i '$http' -t attr -n "port" -v "8080" \
		-i '$http' -t attr -n "connectionTimeout" -v "20000" \
    -d '/Server/Service[@name="Catalina"]/Connector[@port="8009"]' \
		-s '/Server/Service[@name="Catalina"]' -t elem -n "Connector" \
		--var ajp '$prev' \
		-i '$ajp' -t attr -n "protocol" -v "AJP/1.3" \
		-i '$ajp' -t attr -n "address" -v "0.0.0.0" \
		-i '$ajp' -t attr -n "port" -v "8009" \
		-i '$ajp' -t attr -n "secretRequired" -v "false" \
		-d '/Server/Service[@name="Catalina"]/Connector[@port="8443"]' \
		${catServe}

	if [[ ! -f ${setEnvSh} ]] ; then
		echo "- create ${setEnvSh}"
		touch ${setEnvSh}
	else
		echo "- update ${setEnvSh}"
	fi

	sed -i -r 's|alfresco\.home=.*\"|alfresco.home=$ALF_HOME $CATALINA_OPTS \"|' ${setEnvSh}
	grep -q 'alfresco\.home' ${setEnvSh} || echo 'CATALINA_OPTS="-Dalfresco.home=$ALF_HOME $CATALINA_OPTS "' >> ${setEnvSh}

	sed -i -r 's|file\.encoding=.*\"|file.encoding=UTF-8 $CATALINA_OPTS \"|' ${setEnvSh}
	grep -q 'file\.encoding' ${setEnvSh} || echo 'CATALINA_OPTS="-Dfile.encoding=UTF-8 $CATALINA_OPTS "' >> ${setEnvSh}

	sed -i -r 's|user\.country=.*\"|user.country=DE $CATALINA_OPTS \"|' ${setEnvSh}
	grep -q 'user\.country' ${setEnvSh} || echo 'CATALINA_OPTS="-Duser.country=DE $CATALINA_OPTS "' >> ${setEnvSh}

	sed -i -r 's|user\.language=.*\"|user.language=de $CATALINA_OPTS \"|' ${setEnvSh}
	grep -q 'user\.language' ${setEnvSh} || echo 'CATALINA_OPTS="-Duser.language=de $CATALINA_OPTS "' >> ${setEnvSh}

	sed -i -r 's|org\.xml\.sax\.parser=.*\"|org.xml.sax.parser=com.sun.org.apache.xerces.internal.parsers.SAXParser $CATALINA_OPTS \"|' ${setEnvSh}
	grep -q 'org\.xml\.sax\.parser' ${setEnvSh} || echo 'CATALINA_OPTS="-Dorg.xml.sax.parser=com.sun.org.apache.xerces.internal.parsers.SAXParser $CATALINA_OPTS "' >> ${setEnvSh}

	sed -i -r 's|javax\.xml\.parsers\.DocumentBuilderFactory=.*\"|javax.xml.parsers.DocumentBuilderFactory=com.sun.org.apache.xerces.internal.jaxp.DocumentBuilderFactoryImpl $CATALINA_OPTS \"|' ${setEnvSh}
	grep -q 'javax\.xml\.parsers\.DocumentBuilderFactory' ${setEnvSh} || echo 'CATALINA_OPTS="-Djavax.xml.parsers.DocumentBuilderFactory=com.sun.org.apache.xerces.internal.jaxp.DocumentBuilderFactoryImpl $CATALINA_OPTS "' >> ${setEnvSh}

	sed -i -r 's|javax\.xml\.parsers\.SAXParserFactory=.*\"|javax.xml.parsers.SAXParserFactory=com.sun.org.apache.xerces.internal.jaxp.SAXParserFactoryImpl $CATALINA_OPTS \"|' ${setEnvSh}
	grep -q 'javax\.xml\.parsers\.SAXParserFactory' ${setEnvSh} || echo 'CATALINA_OPTS="-Djavax.xml.parsers.SAXParserFactory=com.sun.org.apache.xerces.internal.jaxp.SAXParserFactoryImpl $CATALINA_OPTS "' >> ${setEnvSh}

	if [[ -n "${repository_httpclient_proxy_nonproxyhosts}" ]]  ; then
		sed -i -r "s|http\.nonProxyHosts=.*\"|http.nonProxyHosts=${repository_httpclient_proxy_nonproxyhosts} "'$CATALINA_OPTS'" \"|" ${setEnvSh}
    grep -q 'http\.nonProxyHosts' ${setEnvSh} || echo "CATALINA_OPTS=\"-Dhttp.nonProxyHosts=${repository_httpclient_proxy_nonproxyhosts} "'$CATALINA_OPTS'" \"" >> ${setEnvSh}

		sed -i -r "s|https\.nonProxyHosts=.*\"|https.nonProxyHosts=${repository_httpclient_proxy_nonproxyhosts} "'$CATALINA_OPTS'" \"|" ${setEnvSh}
		grep -q 'https\.nonProxyHosts' ${setEnvSh} || echo "CATALINA_OPTS=\"-Dhttps.nonProxyHosts=${repository_httpclient_proxy_nonproxyhosts} "'$CATALINA_OPTS'" \"" >> ${setEnvSh}
	fi

	if [[ -n "${repository_httpclient_proxy_proxyhost}" ]] ; then
		sed -i -r "s|http\.proxyHost=.*\"|http.proxyHost=${repository_httpclient_proxy_proxyhost} "'$CATALINA_OPTS'" \"|" ${setEnvSh}
    grep -q 'http\.proxyHost' ${setEnvSh} || echo "CATALINA_OPTS=\"-Dhttp.proxyHost=${repository_httpclient_proxy_proxyhost} "'$CATALINA_OPTS'" \"" >> ${setEnvSh}

		sed -i -r "s|https\.proxyHost=.*\"|https.proxyHost=${repository_httpclient_proxy_proxyhost} "'$CATALINA_OPTS'" \"|" ${setEnvSh}
		grep -q 'https\.proxyHost' ${setEnvSh} || echo "CATALINA_OPTS=\"-Dhttps.proxyHost=${repository_httpclient_proxy_proxyhost} "'$CATALINA_OPTS'" \"" >> ${setEnvSh}
	fi

	if [[ -n "${repository_httpclient_proxy_proxypass}" ]] ; then
		sed -i -r "s|http\.proxyPass=.*\"|http.proxyPass=${repository_httpclient_proxy_proxypass} "'$CATALINA_OPTS'" \"|" ${setEnvSh}
    grep -q 'http\.proxyPass' ${setEnvSh} || echo "CATALINA_OPTS=\"-Dhttp.proxyPass=${repository_httpclient_proxy_proxypass} "'$CATALINA_OPTS'" \"" >> ${setEnvSh}

		sed -i -r "s|https\.proxyPass=.*\"|https.proxyPass=${repository_httpclient_proxy_proxypass}\"|" ${setEnvSh}
		grep -q 'https\.proxyPass' ${setEnvSh} || echo "CATALINA_OPTS=\"-Dhttps.proxyPass=${repository_httpclient_proxy_proxypass} "'$CATALINA_OPTS'" \"" >> ${setEnvSh}
	fi

	if [[ -n "${repository_httpclient_proxy_proxyport}" ]] ; then
		sed -i -r "s|http\.proxyPort=.*\"|http.proxyPort=${repository_httpclient_proxy_proxyport} "'$CATALINA_OPTS'" \"|" ${setEnvSh}
    grep -q 'http\.proxyPort' ${setEnvSh} || echo "CATALINA_OPTS=\"-Dhttp.proxyPort=${repository_httpclient_proxy_proxyport} "'$CATALINA_OPTS'" \"" >> ${setEnvSh}

		sed -i -r "s|https\.proxyPort=.*\"|https.proxyPort=${repository_httpclient_proxy_proxyport} "'$CATALINA_OPTS'" \"|" ${setEnvSh}
		grep -q 'https\.proxyPort' ${setEnvSh} || echo "CATALINA_OPTS=\"-Dhttps.proxyPort=${repository_httpclient_proxy_proxyport} "'$CATALINA_OPTS'" \"" >> ${setEnvSh}
	fi

	if [[ -n "${repository_httpclient_proxy_proxyuser}" ]] ; then
		sed -i -r "s|http\.proxyUser=.*\"|http.proxyUser=${repository_httpclient_proxy_proxyuser} "'$CATALINA_OPTS'" \"|" ${setEnvSh}
    grep -q 'http\.proxyUser' ${setEnvSh} || echo "CATALINA_OPTS=\"-Dhttp.proxyUser=${repository_httpclient_proxy_proxyuser} "'$CATALINA_OPTS'" \"" >> ${setEnvSh}

		sed -i -r "s|https\.proxyUser=.*\"|https.proxyUser=${repository_httpclient_proxy_proxyuser} "'$CATALINA_OPTS'" \"|" ${setEnvSh}
		grep -q 'https\.proxyUser' ${setEnvSh} || echo "CATALINA_OPTS=\"-Dhttps.proxyUser=${repository_httpclient_proxy_proxyuser} "'$CATALINA_OPTS'" \"" >> ${setEnvSh}
	fi

	### Alfresco platform ################################################################################################

	echo "- update ${catCxAlf}"

	xmlstarlet ed -L \
		-d '/Context/Loader' \
		-s '/Context' -t elem -n "Resources" -v "" \
		--var resources '$prev' \
		-i '$resources' -t attr -n "cacheMaxSize" -v "20480" \
		${catCxAlf}

	if [[ -f ${catCxShr} ]] ; then
		echo "- update ${catCxShr}"

		xmlstarlet ed -L \
			-d '/Context/Loader' \
			-s '/Context' -t elem -n "Resources" -v "" \
			--var resources '$prev' \
			-i '$resources' -t attr -n "cacheMaxSize" -v "20480" \
			${catCxShr}
	fi

	if [[ -f ${solr4Arc} ]] ; then
		echo "- update ${solr4Arc}"
		echo "alfresco.secureComms=none" >> "${solr4Arc}"
	fi

	if [[ -f ${solr4Wor} ]] ; then
		echo "- update ${solr4Wor}"
		echo "alfresco.secureComms=none" >> "${solr4Wor}"
	fi

	echo "- update ${alfProps}"

	if [[ -n $repository_contentstore ]] ; then
  	echo "dir.contentstore=${repository_contentstore}" >> "${alfProps}"
  fi

  if [[ -n $repository_contentstore_deleted ]] ; then
	  echo "dir.contentstore.deleted=${repository_contentstore_deleted}" >> "${alfProps}"
	fi

  echo 'img.root=/usr' >> "${alfProps}"
  echo 'img.gslib=/usr/bin' >> "${alfProps}"

  echo 'exiftool.dyn=/usr/bin' >> "${alfProps}"
  echo 'exiftool.exe=${exiftool.dyn}/exiftool' >> "${alfProps}"

#  echo 'ffmpeg.dyn=/usr/bin' >> "${alfProps}"
#  echo 'ffmpeg.exe=${ffmpeg.dyn}/ffmpeg' >> "${alfProps}"

  echo 'img.dyn=/usr/bin' >> "${alfProps}"
  echo 'img.exe=${img.dyn}/convert' >> "${alfProps}"

  echo "alfresco-pdf-renderer.root=$ALF_HOME/common/alfresco-pdf-renderer" >>"${alfProps}"
  echo 'alfresco-pdf-renderer.exe=${alfresco-pdf-renderer.root}/alfresco-pdf-renderer' >>"${alfProps}"

	echo "alfresco_user_store.adminpassword=${my_admin_pass_md4}" >> "${alfProps}"

	echo "db.driver=${repository_database_driv}" >> "${alfProps}"
	echo "db.url=${repository_database_jdbc}" >> "${alfProps}"
	echo "db.username=${repository_database_user}" >> "${alfProps}"
	echo "db.password=${repository_database_pass}" >> "${alfProps}"
	echo "db.pool.max=${repository_database_pool_max}" >> "${alfProps}"
	echo "db.pool.validate.query=${repository_database_pool_sql}" >> "${alfProps}"

	echo "ooo.enabled=true" >> "${alfProps}"
	echo "ooo.exe=" >> "${alfProps}"
	echo "ooo.host=${repository_transform_host}" >> "${alfProps}"
	echo "ooo.port=${repository_transform_port}" >> "${alfProps}"

	echo "solr.host=${repository_search_solr4_host}" >> "${alfProps}"
  echo "solr.port=${repository_search_solr4_port}" >> "${alfProps}"
  echo "solr.secureComms=none" >> "${alfProps}"

	### edu-sharing ######################################################################################################

	echo "- update ${eduProps}"

	my_origin="${my_prot_external}://${my_host_external}"
	[[ $my_port_external != "80" && $my_port_external != "443" ]] && {
		my_origin="${my_origin}:${my_port_external}"
	}

	xmlstarlet ed -L \
		-u '/properties/entry[@key="appid"]' -v "${my_home_appid}" \
		-u '/properties/entry[@key="authenticationwebservice"]' -v "${my_auth_external}" \
		-u '/properties/entry[@key="clientport"]' -v "${my_port_external}" \
		-u '/properties/entry[@key="clientprotocol"]' -v "${my_prot_external}" \
		-u '/properties/entry[@key="domain"]' -v "${my_host_external}" \
		-u '/properties/entry[@key="host"]' -v "${my_host_internal}" \
		-u '/properties/entry[@key="password"]' -v "${my_admin_pass}" \
		-u '/properties/entry[@key="port"]' -v "${my_port_internal}" \
		-u '/properties/entry[@key="allow_origin"]' -v "${my_origin},http://localhost:54361" \
		${eduProps}

	if [[ -n "${my_home_provider}" ]] ; then
		xmlstarlet ed -L \
				-d '/properties/entry[@key="remote_provider"]' \
				-s '/properties' -t elem -n "entry" -v "${my_home_provider}" \
				--var entry '$prev' \
				-i '$entry' -t attr -n "key" -v "remote_provider" \
				${eduProps}
	fi

	if [[ -n "${my_home_auth}" ]] ; then
		xmlstarlet ed -L \
			-d '/properties/entry[@key="allowed_authentication_types"]' \
			-s '/properties' -t elem -n "entry" -v "${my_home_auth}" \
			--var entry '$prev' \
			-i '$entry' -t attr -n "key" -v "allowed_authentication_types" \
			${eduProps}

		if [[ "${my_home_auth}" == "shibboleth" ]] ; then
			echo "- update ${eduWebXm}"
			sed -i -r 's|<!--\s*SAML||g' ${eduWebXm}
			sed -i -r 's|SAML\s*-->||g'  ${eduWebXm}

			echo "- update ${eduCConf}"
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
				${eduCConf}
		fi
	fi

	echo "- update ${eduSConf}"

	if [[ -n "${repository_httpclient_disablesni4hosts}" ]] ; then
		hocon -f ${eduSConf} \
			set "repository.httpclient.disableSNI4Hosts" '"'"${repository_httpclient_disablesni4hosts}"'"'
	fi

	if [[ -n "${repository_httpclient_proxy_host}" ]] ; then
		hocon -f ${eduSConf} \
			set "repository.httpclient.proxy.host" '"'"${repository_httpclient_proxy_host}"'"'
	fi

  if [[ -n "${repository_httpclient_proxy_nonproxyhosts}" ]]  ; then
  	hocon -f ${eduSConf} \
  		set "repository.httpclient.proxy.nonproxyhosts" '"'"${repository_httpclient_proxy_nonproxyhosts}"'"'
  fi

  if [[ -n "${repository_httpclient_proxy_proxyhost}" ]] ; then
  	hocon -f ${eduSConf} \
  		set "repository.httpclient.proxy.proxyhost" '"'"${repository_httpclient_proxy_proxyhost}"'"'
  fi

  if [[ -n "${repository_httpclient_proxy_proxypass}" ]] ; then
  	hocon -f ${eduSConf} \
  		set "repository.httpclient.proxy.proxypass" '"'"${repository_httpclient_proxy_proxypass}"'"'
  fi

  if [[ -n "${repository_httpclient_proxy_proxyport}" ]] ; then
  	hocon -f ${eduSConf} \
  		set "repository.httpclient.proxy.proxyport" '"'"${repository_httpclient_proxy_proxyport}"'"'
  fi

  if [[ -n "${repository_httpclient_proxy_proxyuser}" ]] ; then
  	hocon -f ${eduSConf} \
  		set "repository.httpclient.proxy.proxyuser" '"'"${repository_httpclient_proxy_proxyuser}"'"'
  fi

	######################################################################################################################

	pushd "$execution_folder" &> /dev/null
	if [[ -d plugins ]] ; then
		for plugin in plugins/plugin-*/setup_config.sh; do
			if [[ -f "$plugin" ]] ; then
				source "$plugin" || exit 1
		  fi
  	done
  fi
  popd

}

########################################################################################################################

pushd "$ALF_HOME" &> /dev/null

if [[ use_local_maven_cache -eq 1 ]] ; then
	echo "- WARNING: local maven cache is being used"
else
	echo "- download edu-sharing repository distribution"

	mvn -q dependency:get \
		-Dartifact="org.edu_sharing:${ARTIFACT_ID}:${ARTIFACT_VERSION}:tar.gz:bin" \
		-DremoteRepositories=edusharing-remote::::https://artifacts.edu-sharing.com/repository/maven-remote/ \
		-Dtransitive=false
fi

echo "- copy edu-sharing repository distribution"

mvn -q -llr dependency:copy \
	-Dartifact="org.edu_sharing:${ARTIFACT_ID}:${ARTIFACT_VERSION}:tar.gz:bin" \
	-DoutputDirectory=.

if [[ ! -f "$backup" ]] ; then

	echo ""
	echo "Install ... "

	if [[ -d $ALF_HOME/alf_data/solr4 && $(find $ALF_HOME/alf_data/solr4 -type f | wc -l) -gt 0 ]] ; then
		echo "ERROR: You have to clean $ALF_HOME/alf_data/solr4 before install due to content model changes!"
		exit 1;
	fi

	echo "- make an initial backup on $backup"
	mkdir -p "$(dirname "$backup")"
	tar -czf "$backup" amps solr4 tomcat/conf/Catalina tomcat/scripts tomcat/webapps

else

	echo ""
	echo "Update ... "

fi

echo "- make a snapshot"
snapshot_name="$execution_folder/snapshots/edu-sharing-SNAPSHOT-$(date "+%Y.%m.%d-%H.%M.%S").tar.gz"
mkdir -p "$(dirname "$snapshot_name")"
tar -czf "$snapshot_name" tomcat

install_edu_sharing || exit 1

restores=(
	'tomcat/bin/setenv.sh'
	'tomcat/conf/logging.properties'
	'tomcat/webapps/alfresco/WEB-INF/classes/log4j2.xml'
	'tomcat/webapps/share/WEB-INF/classes/log4j.properties'
	'tomcat/shared/classes/config/cluster'
	'tomcat/shared/classes/config/node'
)

for restore in "${restores[@]}"
do
	if [[ $(tar -tf  "$snapshot_name" | grep -c "${restore}") -gt 0 ]]; then
		echo "- restore ${restore} from snapshot"
		tar -zxf "$snapshot_name" "${restore}" -C $(dirname "${restore}")
	fi
done

echo "- prune snapshots (keep 3)"
pushd "$execution_folder"/snapshots &> /dev/null
ls -pt | grep -v / | grep "edu-sharing-SNAPSHOT" | tail -n +4 | xargs -I {} rm {}
popd

configs=(
	'defaults'
	'plugins'
	'cluster'
	'node'
)

for config in "${configs[@]}"; do
	if [[ ! -f tomcat/shared/classes/config/$config/version.json ]]; then
		mkdir -p tomcat/shared/classes/config/$config
		for jar in tomcat/shared/lib/$config/*.jar; do
			if [[ -f $jar ]] ; then
				echo "- unpack $jar into tomcat/shared/classes/config/$config"
				unzip -qq -o $jar -d tomcat/shared/classes/config/$config -x 'META-INF/*'
			fi
		done
		cp -f tomcat/webapps/edu-sharing/version.json tomcat/shared/classes/config/$config
	else
		cmp -s tomcat/webapps/edu-sharing/version.json tomcat/shared/classes/config/$config/version.json || {
			echo "- update tomcat/shared/classes/config/$config/version.json"
			mv tomcat/shared/classes/config/$config/version.json tomcat/shared/classes/config/$config/version.json.$(date +%d-%m-%Y_%H-%M-%S )
			cp tomcat/webapps/edu-sharing/version.json tomcat/shared/classes/config/$config/version.json

			if [[ $config == 'cluster' ]] ; then
				echo "- WARNING: You may need to clean $ALF_HOME/alf_data/solr4 due to content model changes!"
			fi
		}
	fi
done
rm -rf tomcat/shared/lib/*

config_edu_sharing || exit 1

popd

info >> "$execution_folder/install_log-$(date "+%Y.%m.%d-%H.%M.%S").txt"
info

echo "- Please check logging configuration:"
echo "    ${ALF_HOME}/solr4/log4j-solr.properties"
echo "    ${ALF_HOME}/tomcat/conf/logging.properties"
echo "    ${ALF_HOME}/tomcat/webapps/alfresco/WEB-INF/classes/log4j2.xml"

echo "- done."
exit

########################################################################################################################

#!/bin/bash
set -eux

########################################################################################################################

my_admin_pass="${REPOSITORY_SERVICE_ADMIN_PASS:-admin}"
my_admin_pass_md4="$(printf '%s' "$my_admin_pass" | iconv -t utf16le | openssl md4 | awk '{ print $2 }')"

my_guest_user="${REPOSITORY_SERVICE_GUEST_USER:-}"
my_guest_pass="${REPOSITORY_SERVICE_GUEST_PASS:-}"

my_bind="${REPOSITORY_SERVICE_BIND:-"0.0.0.0"}"

my_home_appid="${REPOSITORY_SERVICE_HOME_APPID:-local}"
my_home_auth="${REPOSITORY_SERVICE_HOME_AUTH:-}"
my_home_provider="${REPOSITORY_SERVICE_HOME_PROVIDER:-}"

my_prot_external="${REPOSITORY_SERVICE_PROT_EXTERNAL:-http}"
my_host_external="${REPOSITORY_SERVICE_HOST_EXTERNAL:-repository.127.0.0.1.nip.io}"
my_port_external="${REPOSITORY_SERVICE_PORT_EXTERNAL:-8100}"
my_path_external="${REPOSITORY_SERVICE_PATH_EXTERNAL:-/edu-sharing}"
my_base_external="${my_prot_external}://${my_host_external}:${my_port_external}${my_path_external}"
my_auth_external="${my_base_external}/services/authentication"
my_pool_external="${REPOSITORY_SERVICE_POOL_EXTERNAL:-200}"
my_wait_external="${REPOSITORY_SERVICE_WAIT_EXTERNAL:-2000}"

my_host_internal="${REPOSITORY_SERVICE_HOST_INTERNAL:-repository-service}"
my_port_internal="${REPOSITORY_SERVICE_PORT_INTERNAL:-8080}"
my_pool_internal="${REPOSITORY_SERVICE_POOL_INTERNAL:-200}"
my_wait_internal="${REPOSITORY_SERVICE_WAIT_INTERNAL:-20000}"

my_session_timeout="${REPOSITORY_SERVICE_SESSION_TIMEOUT:-60}"

repository_cache_host="${REPOSITORY_CACHE_HOST:-repository-cache}"
repository_cache_port="${REPOSITORY_CACHE_PORT:-6379}"

repository_database_driv="${REPOSITORY_DATABASE_DRIV:-"org.postgresql.Driver"}"
repository_database_host="${REPOSITORY_DATABASE_HOST:-repository-database}"
repository_database_name="${REPOSITORY_DATABASE_NAME:-repository}"
repository_database_opts="${REPOSITORY_DATABASE_OPTS:-}"
repository_database_pass="${REPOSITORY_DATABASE_PASS:-repository}"
repository_database_pool_max="${REPOSITORY_DATABASE_POOL_MAX:-80}"
repository_database_pool_sql="${REPOSITORY_DATABASE_POOL_SQL:-SELECT 1}"
repository_database_port="${REPOSITORY_DATABASE_PORT:-5432}"
repository_database_prot="${REPOSITORY_DATABASE_PROT:-"postgresql"}"
repository_database_user="${REPOSITORY_DATABASE_USER:-repository}"
repository_database_jdbc="jdbc:${repository_database_prot}://${repository_database_host}:${repository_database_port}/${repository_database_name}${repository_database_opts}"

repository_search_elastic_host="${REPOSITORY_SEARCH_ELASTIC_HOST:-repository-search-elastic}"
repository_search_elastic_port="${REPOSITORY_SEARCH_ELASTIC_PORT:-9200}"
repository_search_elastic_base="http://${repository_search_elastic_host}:${repository_search_elastic_port}"

repository_search_solr4_host="${REPOSITORY_SEARCH_SOLR4_HOST:-repository-search-solr4}"
repository_search_solr4_port="${REPOSITORY_SEARCH_SOLR4_PORT:-8080}"

repository_smtp_auth="${REPOSITORY_SMTP_AUTH:-}"
repository_smtp_from="${REPOSITORY_SMTP_FROM:-noreply@repository.127.0.0.1.nip.io}"
repository_smtp_host="${REPOSITORY_SMTP_HOST:-}"
repository_smtp_pass="${REPOSITORY_SMTP_PASS:-}"
repository_smtp_port="${REPOSITORY_SMTP_PORT:-25}"
repository_smtp_repl="${REPOSITORY_SMTP_REPL:-true}"
repository_smtp_user="${REPOSITORY_SMTP_USER:-}"

repository_transform_host="${REPOSITORY_TRANSFORM_HOST:-repository-transform}"
repository_transform_port="${REPOSITORY_TRANSFORM_PORT:-8100}"

### Wait ###############################################################################################################

until wait-for-it "${repository_cache_host}:${repository_cache_port}" -t 3; do sleep 1; done

grep -Fq 'clusterServersConfig' tomcat/conf/redisson.yaml && {
	until [[ $(redis-cli --cluster info "${repository_cache_host}" "${repository_cache_port}" | grep '[OK]' | cut -d ' ' -f5) -gt 1 ]]; do
		echo >&2 "Waiting for ${repository_cache_host} ..."
		sleep 3
	done
}

until wait-for-it "${repository_search_elastic_host}:${repository_search_elastic_port}" -t 3; do sleep 1; done

until [[ $(curl -sSf -w "%{http_code}\n" -o /dev/null "${repository_search_elastic_base}/_cluster/health?wait_for_status=yellow&timeout=3s") -eq 200 ]]; do
	echo >&2 "Waiting for ${repository_search_elastic_host} ..."
	sleep 3
done

until wait-for-it "${repository_database_host}:${repository_database_port}" -t 3; do sleep 1; done

until PGPASSWORD="${repository_database_pass}" \
	psql -h "${repository_database_host}" -p "${repository_database_port}" -U "${repository_database_user}" -d "${repository_database_name}" -c '\q'; do
	echo >&2 "Waiting for ${repository_database_host} ..."
	sleep 3
done

until wait-for-it "${repository_transform_host}:${repository_transform_port}" -t 3; do sleep 1; done

### Tomcat #############################################################################################################

export CATALINA_OUT="/dev/stdout"

export CATALINA_OPTS="-Dfile.encoding=UTF-8 $CATALINA_OPTS"

export CATALINA_OPTS="-Dorg.xml.sax.parser=com.sun.org.apache.xerces.internal.parsers.SAXParser $CATALINA_OPTS"
export CATALINA_OPTS="-Djavax.xml.parsers.DocumentBuilderFactory=com.sun.org.apache.xerces.internal.jaxp.DocumentBuilderFactoryImpl $CATALINA_OPTS"
export CATALINA_OPTS="-Djavax.xml.parsers.SAXParserFactory=com.sun.org.apache.xerces.internal.jaxp.SAXParserFactoryImpl $CATALINA_OPTS"

export CATALINA_OPTS="-Dcaches.backupCount=1 $CATALINA_OPTS"
export CATALINA_OPTS="-Dcaches.readBackupData=false $CATALINA_OPTS"

export CATALINA_OPTS="-Dhazelcast.shutdownhook.policy=GRACEFUL $CATALINA_OPTS"

xmlstarlet ed -L \
	-d '/Server/Service[@name="Catalina"]/Connector' \
	-s '/Server/Service[@name="Catalina"]' -t elem -n 'Connector' -v '' \
	--var internal '$prev' \
	-i '$internal' -t attr -n "address" -v "${my_bind}" \
	-i '$internal' -t attr -n "port" -v "8080" \
	-i '$internal' -t attr -n "scheme" -v "http" \
	-i '$internal' -t attr -n "proxyName" -v "${my_host_internal}" \
	-i '$internal' -t attr -n "proxyPort" -v "${my_port_internal}" \
	-i '$internal' -t attr -n "protocol" -v "org.apache.coyote.http11.Http11Protocol" \
	-i '$internal' -t attr -n "connectionTimeout" -v "${my_wait_internal}" \
	-i '$internal' -t attr -n "maxThreads" -v "${my_pool_internal}" \
	-s '/Server/Service[@name="Catalina"]' -t elem -n 'Connector' -v '' \
	--var external '$prev' \
	-i '$external' -t attr -n "address" -v "${my_bind}" \
	-i '$external' -t attr -n "port" -v "8081" \
	-i '$external' -t attr -n "scheme" -v "${my_prot_external}" \
	-i '$external' -t attr -n "proxyName" -v "${my_host_external}" \
	-i '$external' -t attr -n "proxyPort" -v "${my_port_external}" \
	-i '$external' -t attr -n "protocol" -v "org.apache.coyote.http11.Http11Protocol" \
	-i '$external' -t attr -n "connectionTimeout" -v "${my_wait_external}" \
	-i '$external' -t attr -n "maxThreads" -v "${my_pool_external}" \
	tomcat/conf/server.xml

xmlstarlet ed -L \
  -N x="http://java.sun.com/xml/ns/javaee" \
	-u '/x:web-app/x:session-config/x:session-timeout' -v "${my_session_timeout}" \
	tomcat/webapps/edu-sharing/WEB-INF/web.xml

### Tomcat shared ######################################################################################################

tar -x -v --skip-old-files -f tomcat/shared.tar

### Alfresco platform ##################################################################################################

global="tomcat/shared/classes/alfresco-global.properties"

sed -i -r 's|^[#]*\s*alfresco_user_store\.adminpassword=.*|alfresco_user_store.adminpassword='"${my_admin_pass_md4}"'|' "${global}"
grep -q '^[#]*\s*alfresco_user_store\.adminpassword=' "${global}" || echo "alfresco_user_store.adminpassword=${my_admin_pass_md4}" >>"${global}"

sed -i -r 's|^[#]*\s*db\.driver=.*|db.driver='"${repository_database_driv}"'|' "${global}"
grep -q '^[#]*\s*db\.driver=' "${global}" || echo "db.driver=${repository_database_driv}" >>"${global}"

sed -i -r 's|^[#]*\s*db\.url=.*|db.url='"${repository_database_jdbc}"'|' "${global}"
grep -q '^[#]*\s*db\.url=' "${global}" || echo "db.url=${repository_database_jdbc}" >>"${global}"

sed -i -r 's|^[#]*\s*db\.username=.*|db.username='"${repository_database_user}"'|' "${global}"
grep -q '^[#]*\s*db\.username=' "${global}" || echo "db.username=${repository_database_user}" >>"${global}"

sed -i -r 's|^[#]*\s*db\.password=.*|db.password='"${repository_database_pass}"'|' "${global}"
grep -q '^[#]*\s*db\.password=' "${global}" || echo "db.password=${repository_database_pass}" >>"${global}"

sed -i -r 's|^[#]*\s*db\.pool\.max=.*|db.pool.max='"${repository_database_pool_max}"'|' "${global}"
grep -q '^[#]*\s*db\.pool\.max=' "${global}" || echo "db.pool.max=${repository_database_pool_max}" >>"${global}"

sed -i -r 's|^[#]*\s*db\.pool\.validate\.query=.*|db.pool.validate.query='"${repository_database_pool_sql}"'|' "${global}"
grep -q '^[#]*\s*db\.pool\.validate\.query=' "${global}" || echo "db.pool.validate.query=${repository_database_pool_sql}" >>"${global}"

sed -i -r 's|^[#]*\s*alfresco\.protocol=.*|alfresco.protocol='"${my_prot_external}"'|' "${global}"
grep -q '^[#]*\s*alfresco\.protocol=' "${global}" || echo "alfresco.protocol=${my_prot_external}" >>"${global}"

sed -i -r 's|^[#]*\s*alfresco\.host=.*|alfresco.host='"${my_host_external}"'|' "${global}"
grep -q '^[#]*\s*alfresco\.host=' "${global}" || echo "alfresco.host=${my_host_external}" >>"${global}"

sed -i -r 's|^[#]*\s*alfresco\.port=.*|alfresco.port='"${my_port_external}"'|' "${global}"
grep -q '^[#]*\s*alfresco\.port=' "${global}" || echo "alfresco.port=${my_port_external}" >>"${global}"

sed -i -r 's|^[#]*\s*ooo\.enabled=.*|ooo.enabled=true|' "${global}"
grep -q '^[#]*\s*ooo\.enabled=' "${global}" || echo "ooo.enabled=true" >>"${global}"

sed -i -r 's|^[#]*\s*ooo\.host=.*|ooo.host='"${repository_transform_host}"'|' "${global}"
grep -q '^[#]*\s*ooo\.host=' "${global}" || echo "ooo.host=${repository_transform_host}" >>"${global}"

sed -i -r 's|^[#]*\s*ooo\.port=.*|ooo.port='"${repository_transform_port}"'|' "${global}"
grep -q '^[#]*\s*ooo\.port=' "${global}" || echo "ooo.port=${repository_transform_port}" >>"${global}"

sed -i -r 's|^[#]*\s*share\.protocol=.*|share.protocol='"${my_prot_external}"'|' "${global}"
grep -q '^[#]*\s*share\.protocol=' "${global}" || echo "share.protocol=${my_prot_external}" >>"${global}"

sed -i -r 's|^[#]*\s*share\.host=.*|share.host='"${my_host_external}"'|' "${global}"
grep -q '^[#]*\s*share\.host=' "${global}" || echo "share.host=${my_host_external}" >>"${global}"

sed -i -r 's|^[#]*\s*share\.port=.*|share.port='"${my_port_external}"'|' "${global}"
grep -q '^[#]*\s*share\.port=' "${global}" || echo "share.port=${my_port_external}" >>"${global}"

sed -i -r 's|^[#]*\s*solr\.host=.*|solr.host='"${repository_search_solr4_host}"'|' "${global}"
grep -q '^[#]*\s*solr\.host=' "${global}" || echo "solr.host=${repository_search_solr4_host}" >>"${global}"

sed -i -r 's|^[#]*\s*solr\.port=.*|solr.port='"${repository_search_solr4_port}"'|' "${global}"
grep -q '^[#]*\s*solr\.port=' "${global}" || echo "solr.port=${repository_search_solr4_port}" >>"${global}"

sed -i -r 's|^[#]*\s*solr\.secureComms=.*|solr.secureComms=none|' "${global}"
grep -q '^[#]*\s*solr\.secureComms=' "${global}" || echo "solr.secureComms=none" >>"${global}"

sed -i -r 's|^[#]*\s*index\.subsystem\.name=.*|index.subsystem.name=solr4|' "${global}"
grep -q '^[#]*\s*index\.subsystem\.name=' "${global}" || echo "index.subsystem.name=solr4" >>"${global}"

### edu-sharing ########################################################################################################

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

[[ -n "${my_guest_user}" ]] && {
	xmlstarlet ed -L \
		-d '/properties/entry[@key="guest_username"]' \
		-s '/properties' -t elem -n "entry" -v "${my_guest_user}" \
		--var entry '$prev' \
		-i '$entry' -t attr -n "key" -v "guest_username" \
		tomcat/shared/classes/homeApplication.properties.xml
}

[[ -n "${my_guest_pass}" ]] && {
	xmlstarlet ed -L \
		-d '/properties/entry[@key="guest_password"]' \
		-s '/properties' -t elem -n "entry" -v "${my_guest_pass}" \
		--var entry '$prev' \
		-i '$entry' -t attr -n "key" -v "guest_password" \
		tomcat/shared/classes/homeApplication.properties.xml
}

xmlstarlet ed -L \
	-u '//entry[@key="mail.addReplyTo"]' -v "${repository_smtp_repl}" \
	-u '//entry[@key="mail.authtype"]' -v "${repository_smtp_auth}" \
	-u '//entry[@key="mail.smtp.from"]' -v "${repository_smtp_from}" \
	-u '//entry[@key="mail.smtp.passwd"]' -v "${repository_smtp_pass}" \
	-u '//entry[@key="mail.smtp.port"]' -v "${repository_smtp_port}" \
	-u '//entry[@key="mail.smtp.server"]' -v "${repository_smtp_host}" \
	-u '//entry[@key="mail.smtp.username"]' -v "${repository_smtp_user}" \
	tomcat/shared/classes/ccmail.properties.xml

[[ -n "${my_home_auth}" ]] && {
	xmlstarlet ed -L \
		-d '/properties/entry[@key="allowed_authentication_types"]' \
		-s '/properties' -t elem -n "entry" -v "${my_home_auth}" \
		--var entry '$prev' \
		-i '$entry' -t attr -n "key" -v "allowed_authentication_types" \
		tomcat/shared/classes/homeApplication.properties.xml

	[[ "${my_home_auth}" == "shibboleth" ]] && {
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
	}
}

[[ -n "${my_home_provider}" ]] && {
	xmlstarlet ed -L \
		-d '/properties/entry[@key="remote_provider"]' \
		-s '/properties' -t elem -n "entry" -v "${my_home_provider}" \
		--var entry '$prev' \
		-i '$entry' -t attr -n "key" -v "remote_provider" \
		tomcat/shared/classes/homeApplication.properties.xml
}

hocon -f tomcat/shared/classes/config/edu-sharing.deployment.conf \
	set "elasticsearch.servers" '["'"${repository_search_elastic_host}:${repository_search_elastic_port}"'"]'

########################################################################################################################

exec "$@"

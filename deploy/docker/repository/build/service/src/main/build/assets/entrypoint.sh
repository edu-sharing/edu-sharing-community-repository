#!/bin/bash
set -eux

########################################################################################################################

my_bind="${MY_BIND:-"0.0.0.0"}"
my_home_appid="${MY_HOME_APPID:-local}"
my_host_extern="${MY_HOST_EXTERN:-repository.127.0.0.1.xip.io}"
my_host_intern="${MY_HOST_INTERN:-repository-service}"
my_port_extern="${MY_PORT_EXTERN:-8100}"
my_port_intern="${MY_PORT_INTERN:-8080}"
my_prot_extern="http"
my_prot_intern="http"

my_base_extern="${my_prot_extern}://${my_host_extern}:${my_port_extern}/edu-sharing"

my_root_pass="${MY_ROOT_PASS:-admin}"
my_root_pass_md4="$(printf '%s' "$my_root_pass" | iconv -t utf16le | openssl md4 | awk '{ print $2 }' )"

my_smtp_auth="${MY_SMTP_AUTH:-}"
my_smtp_from="${MY_SMTP_FROM:-pleasechange@nodomain.com}"
my_smtp_host="${MY_SMTP_HOST:-repository-mail}"
my_smtp_pass="${MY_SMTP_PASS:-}"
my_smtp_port="${MY_SMTP_PORT:-25}"
my_smtp_repl="${MY_SMTP_REPL:-true}"
my_smtp_user="${MY_SMTP_USER:-}"

repository_cache_host="${REPOSITORY_CACHE_HOST:-repository-cache}"
repository_cache_port="${REPOSITORY_CACHE_PORT:-6379}"

repository_database_driv="${REPOSITORY_DATABASE_DRIV:-"org.postgresql.Driver"}"
repository_database_host="${REPOSITORY_DATABASE_HOST:-repository-database}"
repository_database_name="${REPOSITORY_DATABASE_NAME:-repository}"
repository_database_pass="${REPOSITORY_DATABASE_PASS:-repository}"
repository_database_port="${REPOSITORY_DATABASE_PORT:-5432}"
repository_database_prot="${REPOSITORY_DATABASE_PROT:-"postgresql"}"
repository_database_user="${REPOSITORY_DATABASE_USER:-repository}"

repository_database_jdbc="jdbc:${repository_database_prot}://${repository_database_host}:${repository_database_port}/${repository_database_name}"

repository_search_elastic_prot="http"
repository_search_elastic_host="${REPOSITORY_SEARCH_ELASTIC_HOST:-repository-search-elastic}"
repository_search_elastic_port="${REPOSITORY_SEARCH_ELASTIC_PORT:-9200}"

repository_search_elastic_base="${repository_search_elastic_prot}://${repository_search_elastic_host}:${repository_search_elastic_port}"

repository_search_solr4_host="${REPOSITORY_SEARCH_SOLR4_HOST:-repository-search-solr4}"
repository_search_solr4_port="${REPOSITORY_SEARCH_SOLR4_PORT:-8080}"

repository_transform_host="${REPOSITORY_TRANSFORM_HOST:-repository-transform}"
repository_transform_port="${REPOSITORY_TRANSFORM_PORT:-8100}"

### Wait ###############################################################################################################

until wait-for-it "${repository_cache_host}:${repository_cache_port}" -t 3; do sleep 1; done

until wait-for-it "${repository_database_host}:${repository_database_port}" -t 3; do sleep 1; done

until PGPASSWORD="${repository_database_pass}" \
  psql -h "${repository_database_host}" -p "${repository_database_port}" -U "${repository_database_user}" -d "${repository_database_name}" -c '\q'
do
  >&2 echo "Waiting for repository-database  ..."
  sleep 3
done

until wait-for-it "${my_smtp_host}:${my_smtp_port}" -t 3; do sleep 1; done

until wait-for-it "${repository_search_elastic_host}:${repository_search_elastic_port}" -t 3; do sleep 1; done

until [[ $( curl -sSf -w "%{http_code}\n" -o /dev/null "${repository_search_elastic_base}/_cluster/health?wait_for_status=yellow&timeout=3s" ) -eq 200 ]]
do
  >&2 echo "Waiting for repository-search-elastic  ..."
  sleep 3
done

until wait-for-it "${repository_transform_host}:${repository_transform_port}" -t 3; do sleep 1; done

### Tomcat #############################################################################################################

export CATALINA_OUT="/dev/stdout"

export CATALINA_OPTS="-Dfile.encoding=UTF-8 $CATALINA_OPTS"

export CATALINA_OPTS="-Dorg.xml.sax.parser=com.sun.org.apache.xerces.internal.parsers.SAXParser $CATALINA_OPTS"
export CATALINA_OPTS="-Djavax.xml.parsers.DocumentBuilderFactory=com.sun.org.apache.xerces.internal.jaxp.DocumentBuilderFactoryImpl $CATALINA_OPTS"
export CATALINA_OPTS="-Djavax.xml.parsers.SAXParserFactory=com.sun.org.apache.xerces.internal.jaxp.SAXParserFactoryImpl $CATALINA_OPTS"

xmlstarlet ed -L \
  -d '/Server/Service[@name="Catalina"]/Connector' \
  -s '/Server/Service[@name="Catalina"]' -t elem -n 'Connector' -v '' \
  --var intern '$prev' \
  -i '$intern' -t attr -n "address"            -v "${my_bind}" \
  -i '$intern' -t attr -n "port"               -v "8080" \
  -i '$intern' -t attr -n "scheme"             -v "${my_prot_intern}" \
  -i '$intern' -t attr -n "proxyName"          -v "${my_host_intern}" \
  -i '$intern' -t attr -n "proxyPort"          -v "${my_port_intern}" \
  -i '$intern' -t attr -n "protocol"           -v "HTTP/1.1" \
  -i '$intern' -t attr -n "connectionTimeout"  -v "20000" \
  -s '/Server/Service[@name="Catalina"]' -t elem -n 'Connector' -v '' \
  --var extern '$prev' \
  -i '$extern' -t attr -n "address"             -v "${my_bind}" \
  -i '$extern' -t attr -n "port"                -v "8081" \
  -i '$extern' -t attr -n "scheme"              -v "${my_prot_extern}" \
  -i '$extern' -t attr -n "proxyName"           -v "${my_host_extern}" \
  -i '$extern' -t attr -n "proxyPort"           -v "${my_port_extern}" \
  -i '$extern' -t attr -n "protocol"            -v "HTTP/1.1" \
  -i '$extern' -t attr -n "connectionTimeout"   -v "20000" \
  tomcat/conf/server.xml

xmlstarlet ed -L \
  -d '/Context/Manager[@className="org.redisson.tomcat.RedissonSessionManager"]' \
	-s '/Context' -t elem -n "Manager" -v "" \
	--var redis '$prev' \
	-i '$redis' -t attr -n "className" 							-v "org.redisson.tomcat.RedissonSessionManager" \
	-i '$redis' -t attr -n "configPath" 						-v "tomcat/conf/redisson.yaml" \
	-i '$redis' -t attr -n "readMode"   						-v "REDIS" \
	-i '$redis' -t attr -n "updateMode" 						-v "DEFAULT" \
	-i '$redis' -t attr -n "broadcastSessionEvents" -v "false" \
  tomcat/conf/context.xml

### Tomcat shared ######################################################################################################

tar -x -v --skip-old-files -f tomcat/shared.tar

### Alfresco platform ##################################################################################################

global="tomcat/shared/classes/alfresco-global.properties"

sed -i -r 's|^[#]*\s*alfresco_user_store\.adminpassword=.*|alfresco_user_store.adminpassword='"${my_root_pass_md4}"'|' "${global}"
grep -q   '^[#]*\s*alfresco_user_store\.adminpassword=' "${global}" || echo "alfresco_user_store.adminpassword=${my_root_pass_md4}" >> "${global}"

sed -i -r 's|^[#]*\s*db\.driver=.*|db.driver='"${repository_database_driv}"'|' "${global}"
grep -q   '^[#]*\s*db\.driver=' "${global}" || echo "db.driver=${repository_database_driv}" >> "${global}"

sed -i -r 's|^[#]*\s*db\.url=.*|db.url='"${repository_database_jdbc}"'|' "${global}"
grep -q   '^[#]*\s*db\.url=' "${global}" || echo "db.url=${repository_database_jdbc}" >> "${global}"

sed -i -r 's|^[#]*\s*db\.username=.*|db.username='"${repository_database_user}"'|' "${global}"
grep -q   '^[#]*\s*db\.username=' "${global}" || echo "db.username=${repository_database_user}" >> "${global}"

sed -i -r 's|^[#]*\s*db\.password=.*|db.password='"${repository_database_pass}"'|' "${global}"
grep -q   '^[#]*\s*db\.password=' "${global}" || echo "db.password=${repository_database_pass}" >> "${global}"

sed -i -r 's|^[#]*\s*alfresco\.protocol=.*|alfresco.protocol='"${my_prot_extern}"'|' "${global}"
grep -q   '^[#]*\s*alfresco\.protocol=' "${global}" || echo "alfresco.protocol=${my_prot_extern}" >> "${global}"

sed -i -r 's|^[#]*\s*alfresco\.host=.*|alfresco.host='"${my_host_extern}"'|' "${global}"
grep -q   '^[#]*\s*alfresco\.host=' "${global}" || echo "alfresco.host=${my_host_extern}" >> "${global}"

sed -i -r 's|^[#]*\s*alfresco\.port=.*|alfresco.port='"${my_port_extern}"'|' "${global}"
grep -q   '^[#]*\s*alfresco\.port=' "${global}" || echo "alfresco.port=${my_port_extern}" >> "${global}"

sed -i -r 's|^[#]*\s*share\.protocol=.*|share.protocol='"${my_prot_extern}"'|' "${global}"
grep -q   '^[#]*\s*share\.protocol=' "${global}" || echo "share.protocol=${my_prot_extern}" >> "${global}"

sed -i -r 's|^[#]*\s*share\.host=.*|share.host='"${my_host_extern}"'|' "${global}"
grep -q   '^[#]*\s*share\.host=' "${global}" || echo "share.host=${my_host_extern}" >> "${global}"

sed -i -r 's|^[#]*\s*share\.port=.*|share.port='"${my_port_extern}"'|' "${global}"
grep -q   '^[#]*\s*share\.port=' "${global}" || echo "share.port=${my_port_extern}" >> "${global}"

sed -i -r 's|^[#]*\s*ooo\.enabled=.*|ooo.enabled=true|' "${global}"
grep -q   '^[#]*\s*ooo\.enabled=' "${global}" || echo "ooo.enabled=true" >> "${global}"

sed -i -r 's|^[#]*\s*ooo\.host=.*|ooo.host='"${repository_transform_host}"'|' "${global}"
grep -q   '^[#]*\s*ooo\.host=' "${global}" || echo "ooo.host=${repository_transform_host}" >> "${global}"

sed -i -r 's|^[#]*\s*ooo\.port=.*|ooo.port='"${repository_transform_port}"'|' "${global}"
grep -q   '^[#]*\s*ooo\.port=' "${global}" || echo "ooo.port=${repository_transform_port}" >> "${global}"

sed -i -r 's|^[#]*\s*solr\.host=.*|solr.host='"${repository_search_solr4_host}"'|' "${global}"
grep -q   '^[#]*\s*solr\.host=' "${global}" || echo "solr.host=${repository_search_solr4_host}" >> "${global}"

sed -i -r 's|^[#]*\s*solr\.port=.*|solr.port='"${repository_search_solr4_port}"'|' "${global}"
grep -q   '^[#]*\s*solr\.port=' "${global}" || echo "solr.port=${repository_search_solr4_port}" >> "${global}"

sed -i -r 's|^[#]*\s*solr\.secureComms=.*|solr.secureComms=none|' "${global}"
grep -q   '^[#]*\s*solr\.secureComms=' "${global}" || echo "solr.secureComms=none" >> "${global}"

sed -i -r 's|^[#]*\s*index\.subsystem\.name=.*|index.subsystem.name=solr4|' "${global}"
grep -q   '^[#]*\s*index\.subsystem\.name=' "${global}" || echo "index.subsystem.name=solr4" >> "${global}"

### edu-sharing ########################################################################################################

xmlstarlet ed -L \
  -u '//entry[@key="appid"]'                      -v "${my_home_appid}" \
  -u '//entry[@key="authenticationwebservice"]'   -v "${my_base_extern}/services/authentication" \
  -u '//entry[@key="clientport"]'                 -v "${my_port_extern}" \
  -u '//entry[@key="clientprotocol"]'             -v "${my_prot_extern}" \
  -u '//entry[@key="domain"]'                     -v "${my_host_extern}" \
  -u '//entry[@key="host"]'                       -v "${my_host_intern}" \
  -u '//entry[@key="password"]'                   -v "${my_root_pass}" \
  -u '//entry[@key="port"]'                       -v "${my_port_intern}" \
  tomcat/shared/classes/homeApplication.properties.xml

xmlstarlet ed -L \
	-u '//entry[@key="mail.addReplyTo"]'           	-v "${my_smtp_repl}" \
  -u '//entry[@key="mail.authtype"]'           		-v "${my_smtp_auth}" \
  -u '//entry[@key="mail.smtp.from"]'           	-v "${my_smtp_from}" \
  -u '//entry[@key="mail.smtp.passwd"]'           -v "${my_smtp_pass}" \
  -u '//entry[@key="mail.smtp.port"]'           	-v "${my_smtp_port}" \
  -u '//entry[@key="mail.smtp.server"]'           -v "${my_smtp_host}" \
  -u '//entry[@key="mail.smtp.username"]'         -v "${my_smtp_user}" \
  tomcat/shared/classes/ccmail.properties.xml

hocon -f tomcat/shared/classes/config/edu-sharing.deployment.conf \
	set "elasticsearch.servers" '["'"${repository_search_elastic_host}:${repository_search_elastic_port}"'"]'

########################################################################################################################

exec "$@"

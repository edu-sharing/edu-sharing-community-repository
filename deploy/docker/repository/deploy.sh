#!/bin/bash
set -e
set -o pipefail

case "$(uname)" in
MINGW*)
	COMPOSE_EXEC="winpty docker-compose"
	;;
*)
	COMPOSE_EXEC="docker-compose"
	;;
esac
export COMPOSE_EXEC

export COMPOSE_NAME="${COMPOSE_PROJECT_NAME:-compose}"

export CLI_CMD="$0"
export CLI_OPT1="$1"
export CLI_OPT2="$2"
export CLI_OPT3="$3"
export CLI_OPT4="$4"

if [ -z "${M2_HOME}" ]; then
	export MVN_EXEC="mvn"
else
	export MVN_EXEC="${M2_HOME}/bin/mvn"
fi

[[ -z "${MVN_EXEC_OPTS}" ]] && {
	export MVN_EXEC_OPTS="-q -ff"
}

ROOT_PATH="$(
	cd "$(dirname ".")"
	pwd -P
)"
export ROOT_PATH

pushd "$(cd "$(dirname "${BASH_SOURCE[0]}")" >/dev/null && pwd)" >/dev/null || exit

BUILD_PATH="$(
	cd "$(dirname ".")"
	pwd -P
)"
export BUILD_PATH

COMPOSE_DIR="compose/target/compose"

[[ ! -d "${COMPOSE_DIR}" || -z "${CLI_OPT1}" ]] && {
	echo "Building ..."
	pushd "compose" >/dev/null || exit
	$MVN_EXEC $MVN_EXEC_OPTS -Dmaven.test.skip=true package || exit
	popd >/dev/null || exit
}

[[ -f ".env" ]] && {
	cp -f ".env" "${COMPOSE_DIR}"
}

pushd "${COMPOSE_DIR}" >/dev/null || exit

info() {
	[[ -f ".env" ]] && source .env
	echo ""
	echo "#########################################################################"
	echo ""
	echo "repository-cache:"
	echo ""
	echo "  Services:"
	echo ""
	echo "    REDIS:          127.0.0.1:${REPOSITORY_CACHE_PORT_REDIS:-8001}"
	echo ""
	echo "#########################################################################"
	echo ""
	echo "repository-database:"
	echo ""
	echo "  Credentials:"
	echo ""
	echo "    Name:           ${REPOSITORY_DATABASE_USER:-repository}"
	echo "    Password:       ${REPOSITORY_DATABASE_PASS:-repository}"
	echo ""
	echo "  Database:         ${REPOSITORY_DATABASE_NAME:-repository}"
	echo ""
	echo "  Services:"
	echo ""
	echo "    SQL:            127.0.0.1:${REPOSITORY_DATABASE_PORT_SQL:-8000}"
	echo ""
	echo "#########################################################################"
	echo ""
	echo "repository-search-elastic:"
	echo ""
	echo "  JVM:"
	echo ""
	echo "    XMS:            ${REPOSITORY_SEARCH_ELASTIC_JAVA_XMS:-1g}"
	echo "    XMX:            ${REPOSITORY_SEARCH_ELASTIC_JAVA_XMX:-1g}"
	echo ""
	echo "  Services:"
	echo ""
	echo "    HTTP:           http://127.0.0.1:${REPOSITORY_SEARCH_ELASTIC_PORT_HTTP:-8300}/"
	echo "    JPDA:           127.0.0.1:${REPOSITORY_SEARCH_ELASTIC_PORT_JPDA:-8301}"
	echo ""
	echo "#########################################################################"
	echo ""
	echo "repository-search-solr4:"
	echo ""
	echo "  JVM:"
	echo ""
	echo "    XMS:            ${REPOSITORY_SEARCH_SOLR4_JAVA_XMS:-1g}"
	echo "    XMX:            ${REPOSITORY_SEARCH_SOLR4_JAVA_XMX:-1g}"
	echo ""
	echo "  Services:"
	echo ""
	echo "    HTTP:           http://127.0.0.1:${REPOSITORY_SEARCH_SOLR4_PORT_HTTP:-8200}/solr4/"
	echo "    JPDA:           127.0.0.1:${REPOSITORY_SEARCH_SOLR4_PORT_JPDA:-8201}"
	echo ""
	echo "#########################################################################"
	echo ""
	echo "repository-service:"
	echo ""
	echo "  Credentials:"
	echo ""
	echo "    Admin:"
	echo "      Name:         admin"
	echo "      Password:     ${REPOSITORY_SERVICE_ADMIN_PASS:-admin}"
	echo ""
	echo "    Guest:"
	echo "      Name:         ${REPOSITORY_SERVICE_GUEST_USER:-}"
	echo "      Password:     ${REPOSITORY_SERVICE_GUEST_PASS:-}"
	echo ""
	echo "  JVM:"
	echo ""
	echo "    XMS:            ${REPOSITORY_SERVICE_JAVA_XMS:-1g}"
	echo "    XMX:            ${REPOSITORY_SERVICE_JAVA_XMX:-1g}"
	echo ""
	echo "  SMTP:"
	echo ""
	echo "    From:           ${REPOSITORY_SMTP_FROM:-noreply@repository.127.0.0.1.xip.io}"
	echo "    Add-Reply-To:   ${REPOSITORY_SMTP_REPL:-false}"
	echo "    Host:           ${REPOSITORY_SMTP_HOST:-}"
	echo "    Port:           ${REPOSITORY_SMTP_PORT:-25}"
	echo "    Auth:           ${REPOSITORY_SMTP_AUTH:-}"
	echo "    Username:       ${REPOSITORY_SMTP_USER:-}"
	echo "    Password:       ${REPOSITORY_SMTP_USER:-}"
	echo ""
	echo "  Services:"
	echo ""
	echo "    HTTP:           http://${REPOSITORY_SERVICE_HOST:-repository.127.0.0.1.xip.io}:${REPOSITORY_SERVICE_PORT_HTTP:-8100}/edu-sharing/"
	echo "    JPDA:           127.0.0.1:${REPOSITORY_SERVICE_PORT_JPDA:-8101}"
	echo ""
	echo "#########################################################################"
	echo ""
	echo ""
}

init() {
	docker volume create "${COMPOSE_NAME}_repository-database-volume" || exit
	docker volume create "${COMPOSE_NAME}_repository-search-elastic-volume" || exit
	docker volume create "${COMPOSE_NAME}_repository-search-solr4-volume" || exit
	docker volume create "${COMPOSE_NAME}_repository-service-volume-data" || exit
	docker volume create "${COMPOSE_NAME}_repository-service-volume-shared" || exit
}

purge() {
	docker volume rm -f "${COMPOSE_NAME}_repository-database-volume" || exit
	docker volume rm -f "${COMPOSE_NAME}_repository-search-elastic-volume" || exit
	docker volume rm -f "${COMPOSE_NAME}_repository-search-solr4-volume" || exit
	docker volume rm -f "${COMPOSE_NAME}_repository-service-volume-data" || exit
	docker volume rm -f "${COMPOSE_NAME}_repository-service-volume-shared" || exit
}

logs() {
	$COMPOSE_EXEC \
		-f "repository.yml" \
		logs -f || exit
}

up() {
	$COMPOSE_EXEC \
		-f "repository.yml" \
		-f "repository-image-remote.yml" \
		pull || exit

	$COMPOSE_EXEC \
		-f "repository.yml" \
		-f "repository-image-remote.yml" \
		-f "repository-network-prd.yml" \
		up -d || exit
}

down() {
	$COMPOSE_EXEC \
		-f "repository.yml" \
		down || exit
}

it() {
	$COMPOSE_EXEC \
		-f "repository.yml" \
		-f "repository-network-dev.yml" \
		up -d || exit
}

plugins() {
	echo "Checking plugins ..."
	echo "- remote             [ ${PLUGIN_REMOTE_ENABLED:-false} ]"
}

build() {
	[[ -z "${CLI_OPT2}" ]] && {
		echo ""
		echo "Usage: ${CLI_CMD} ${CLI_OPT1} <repository-project>"
		exit
	}

	pushd "${ROOT_PATH}/${CLI_OPT2}" >/dev/null || exit
	COMMUNITY_PATH=$(pwd)
	export COMMUNITY_PATH
	popd >/dev/null || exit

	echo "Checking artifact-id ..."

	EXPECTED_ARTIFACTID="edu_sharing-community-repository"

	pushd "${COMMUNITY_PATH}" >/dev/null || exit
	PROJECT_ARTIFACTID=$($MVN_EXEC -q -ff -nsu -N help:evaluate -Dexpression=project.artifactId -DforceStdout)
	echo "- repository         [ ${PROJECT_ARTIFACTID} ]"
	popd >/dev/null || exit

	[[ "${EXPECTED_ARTIFACTID}" != "${PROJECT_ARTIFACTID}" ]] && {
		echo "Error: expected artifact-id [ ${EXPECTED_ARTIFACTID} ] is different."
		exit
	}

	echo "Checking version ..."

	pushd "${BUILD_PATH}" >/dev/null || exit
	EXPECTED_VERSION=$($MVN_EXEC -q -ff -nsu -N help:evaluate -Dexpression=community.repository.version -DforceStdout)
	popd >/dev/null || exit

	pushd "${COMMUNITY_PATH}" >/dev/null || exit
	PROJECT_VERSION=$($MVN_EXEC -q -ff -nsu -N help:evaluate -Dexpression=project.version -DforceStdout)
	echo "- repository         [ ${PROJECT_VERSION} ]"
	popd >/dev/null || exit

	[[ "${EXPECTED_VERSION}" != "${PROJECT_VERSION}" ]] && {
		echo "Error: expected version [ ${EXPECTED_VERSION} ] is different."
		exit
	}

	echo "Building ..."

	echo "- repository"
	pushd "${COMMUNITY_PATH}" >/dev/null || exit
	$MVN_EXEC $MVN_EXEC_OPTS -Dmaven.test.skip=true clean install || exit
	popd >/dev/null || exit

	echo "- docker"
	pushd "${BUILD_PATH}/../build" >/dev/null || exit
	$MVN_EXEC $MVN_EXEC_OPTS -Dmaven.test.skip=true clean install || exit
	popd >/dev/null || exit
	pushd "${BUILD_PATH}" >/dev/null || exit
	$MVN_EXEC $MVN_EXEC_OPTS -Dmaven.test.skip=true clean install || exit
	popd >/dev/null || exit
	
	echo "export COMMUNITY_PATH=${COMMUNITY_PATH}" > .ctx
}

debug() {
	[[ -f ".ctx" ]] && source .ctx
	echo "<repository-path>:   ${COMMUNITY_PATH}"
	[[ -z "${COMMUNITY_PATH}" ]] && {
		echo ""
		echo "Error: missing context, build first."
		exit
	}

	$COMPOSE_EXEC \
		-f "repository.yml" \
		-f "repository-network-dev.yml" \
		-f "repository-debug.yml" \
		up -d || exit
}

package-alfresco() {
	[[ -f ".ctx" ]] && source .ctx
	echo "<repository-path>:   ${COMMUNITY_PATH}"
	[[ -z "${COMMUNITY_PATH}" ]] && {
		echo ""
		echo "Error: missing context, build first."
		exit
	}

	SUBMODULE_PATH="Backend/alfresco"

	echo "Building ..."

	echo "- repository         [ ${SUBMODULE_PATH} ]"
	pushd "${COMMUNITY_PATH}/${SUBMODULE_PATH}" >/dev/null || exit
	$MVN_EXEC $MVN_EXEC_OPTS -nsu -Dmaven.test.skip=true package || exit
	popd >/dev/null || exit
}

package-config() {
	[[ -f ".ctx" ]] && source .ctx
	echo "<repository-path>:   ${COMMUNITY_PATH}"
	[[ -z "${COMMUNITY_PATH}" ]] && {
		echo ""
		echo "Error: missing context, build first."
		exit
	}
	
	SUBMODULE_PATH="config"

	echo "Building ..."

	echo "- repository         [ ${SUBMODULE_PATH} ]"
	pushd "${COMMUNITY_PATH}/${SUBMODULE_PATH}" >/dev/null || exit
	pushd "src/main/resources/org" >/dev/null || exit
	find . -type d -exec mkdir -p "../../../../target/classes/org/{}" \;
	popd >/dev/null || exit
	diff -qr "src/main/resources/org" "target/classes/org" |
		grep "Files src/main/resources/org" |
		awk '{print $2}' |
		sed "s!src/main/resources/org!\.!" |
		xargs -I {} cp "src/main/resources/org/{}" "target/classes/org/{}" || true
	diff -qr "src/main/resources/org" "target/classes/org" |
		grep "Only in src/main/resources/org" |
		awk '{print $3,$4 }' |
		sed "s!: !/!" |
		sed "s!src/main/resources/org!\.!" |
		xargs -I {} cp "src/main/resources/org/{}" "target/classes/org/{}" || true
	popd >/dev/null || exit
}

package-services() {
	[[ -f ".ctx" ]] && source .ctx
	echo "<repository-path>:   ${COMMUNITY_PATH}"
	[[ -z "${COMMUNITY_PATH}" ]] && {
		echo ""
		echo "Error: missing context, build first."
		exit
	}

	SUBMODULE_PATH="Backend/services"

	echo "Building ..."

	echo "- repository         [ ${SUBMODULE_PATH} ]"
	pushd "${COMMUNITY_PATH}/${SUBMODULE_PATH}" >/dev/null || exit
	$MVN_EXEC $MVN_EXEC_OPTS -nsu -Dmaven.test.skip=true package || exit
	popd >/dev/null || exit
}

reload-alfresco() {
	[[ -f ".ctx" ]] && source .ctx
	echo "<repository-path>:   ${COMMUNITY_PATH}"
	[[ -z "${COMMUNITY_PATH}" ]] && {
		echo ""
		echo "Error: missing context, build first."
		exit
	}

	echo "Restarting ..."
	echo "- repository         [ Backend/alfresco ]"

	$COMPOSE_EXEC \
		-f "repository.yml" \
		exec repository-service \
		java -jar bin/alfresco-mmt.jar install amps tomcat/webapps/alfresco -directory -nobackup -force || exit

	$COMPOSE_EXEC \
		-f "repository.yml" \
		exec repository-service \
		touch tomcat/webapps/alfresco/WEB-INF/web.xml || exit

	sleep 10

	echo "Waiting for ..."
	echo "- repository         [ Backend/alfresco ]"

	[[ -f ".env" ]] && source .env
	curl -sSf -o /dev/null "http://${REPOSITORY_SERVICE_HOST:-repository.127.0.0.1.xip.io}:${REPOSITORY_SERVICE_PORT_HTTP:-8100}/alfresco/" || exit

	echo "Done."
}

reload-services() {
	[[ -f ".ctx" ]] && source .ctx
	echo "<repository-path>:   ${COMMUNITY_PATH}"
	[[ -z "${COMMUNITY_PATH}" ]] && {
		echo ""
		echo "Error: missing context, build first."
		exit
	}

	echo "Restarting ..."
	echo "- repository         [ Backend/services ]"

	$COMPOSE_EXEC \
		-f "repository.yml" \
		exec repository-service \
		touch tomcat/webapps/edu-sharing/WEB-INF/web.xml || exit

	sleep 10

	echo "Waiting for ..."
	echo "- repository         [ Backend/services ]"

	[[ -f ".env" ]] && source .env
	curl -sSf -o /dev/null "http://${REPOSITORY_SERVICE_HOST:-repository.127.0.0.1.xip.io}:${REPOSITORY_SERVICE_PORT_HTTP:-8100}/edu-sharing/" || exit

	echo "Done."
}

case "${CLI_OPT1}" in
build)
	plugins && build
	;;
info)
	info
	;;
init)
	init
	;;
logs)
	logs
	;;
plugins)
	plugins
	;;
purge)
	purge
	;;
start)
	init && up && logs
	;;
test)
	init && it && logs
	;;
stop)
	down
	;;
debug)
	init && debug && logs
	;;
rebuild-alfresco)
	package-alfresco && reload-alfresco
	;;
rebuild-config)
	package-config && reload-services
	;;
rebuild-services)
	package-services && reload-services
	;;
reload-alfresco)
	reload-alfresco
	;;
reload-services)
	reload-services
	;;
*)
	echo ""
	echo "Usage: ${CLI_CMD} [option]"
	echo ""
	echo "Option:"
	echo "  - build <repository-project>"
	echo "  - debug"
	echo "  - info"
	echo "  - init"
	echo "  - logs"
	echo "  - rebuild-alfresco"
	echo "  - rebuild-config"
	echo "  - rebuild-services"
	echo "  - reload-alfresco"
	echo "  - reload-services"
	echo "  - plugins"
	echo "  - purge"
	echo "  - start"
	echo "  - stop"
	echo "  - test"
	echo ""
	;;
esac

popd >/dev/null || exit
popd >/dev/null || exit

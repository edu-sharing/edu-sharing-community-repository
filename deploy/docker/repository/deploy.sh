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

[[ -f ".env" ]] && {
	cp -f ".env" "${COMPOSE_DIR}"
}

[[ ! -d "${COMPOSE_DIR}" ]] && {
	echo "Initializing ..."
	pushd "compose" >/dev/null || exit
	$MVN_EXEC $MVN_EXEC_OPTS -Dmaven.test.skip=true package || exit
	popd >/dev/null || exit
}

pushd "${COMPOSE_DIR}" >/dev/null || exit

info() {
	[[ -f ".env" ]] && source .env
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
	echo "    From:           ${REPOSITORY_SMTP_FROM:-noreply@repository.127.0.0.1.nip.io}"
	echo "    Add-Reply-To:   ${REPOSITORY_SMTP_REPL:-false}"
	echo "    Host:           ${REPOSITORY_SMTP_HOST:-}"
	echo "    Port:           ${REPOSITORY_SMTP_PORT:-25}"
	echo "    Auth:           ${REPOSITORY_SMTP_AUTH:-}"
	echo "    Username:       ${REPOSITORY_SMTP_USER:-}"
	echo "    Password:       ${REPOSITORY_SMTP_USER:-}"
	echo ""
	echo "  Services:"
	echo ""
	echo "    HTTP:           http://${REPOSITORY_SERVICE_HOST:-repository.127.0.0.1.nip.io}:${REPOSITORY_SERVICE_PORT_HTTP:-8100}/edu-sharing/"
	echo "    JPDA:           127.0.0.1:${REPOSITORY_SERVICE_PORT_JPDA:-8101}"
	echo ""
	echo "#########################################################################"
	echo ""
	echo ""
}

note() {
	[[ -f ".env" ]] && source .env
	echo ""
	echo "#########################################################################"
	echo ""
	echo "  edu-sharing community repository:"
	echo ""
	echo "    http://${REPOSITORY_SERVICE_HOST:-repository.127.0.0.1.nip.io}:${REPOSITORY_SERVICE_PORT_HTTP:-8100}/edu-sharing/"
	echo ""
	echo "    username: admin"
	echo "    password: ${REPOSITORY_SERVICE_ADMIN_PASS:-admin}"
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

ps() {
	$COMPOSE_EXEC \
		-f "repository.yml" \
		ps || exit
}

lstart() {
	$COMPOSE_EXEC \
		-f "repository.yml" \
		-f "repository-network-prd.yml" \
		up -d || exit
}

ltest() {
	$COMPOSE_EXEC \
		-f "repository.yml" \
		-f "repository-network-dev.yml" \
		up -d || exit
}

ldebug() {
	[[ -z "${CLI_OPT2}" ]] && {
		echo ""
		echo "Usage: ${CLI_CMD} ${CLI_OPT1} <edu-sharing community repository>"
		exit
	}

	pushd "${ROOT_PATH}/${CLI_OPT2}" >/dev/null || exit
	COMMUNITY_PATH=$(pwd)
	export COMMUNITY_PATH
	popd >/dev/null || exit

	$COMPOSE_EXEC \
		-f "repository.yml" \
		-f "repository-network-dev.yml" \
		-f "repository-debug.yml" \
		up -d || exit
}

rstart() {
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

rtest() {
	$COMPOSE_EXEC \
		-f "repository.yml" \
		-f "repository-image-remote.yml" \
		pull || exit

	$COMPOSE_EXEC \
		-f "repository.yml" \
		-f "repository-image-remote.yml" \
		-f "repository-network-dev.yml" \
		up -d || exit
}

rdebug() {
	[[ -z "${CLI_OPT2}" ]] && {
		echo ""
		echo "Usage: ${CLI_CMD} ${CLI_OPT1} <edu-sharing community repository>"
		exit
	}

	case $CLI_OPT2 in
		/*) pushd "${CLI_OPT2}" >/dev/null || exit ;;
		*) pushd "${ROOT_PATH}/${CLI_OPT2}" >/dev/null || exit ;;
	esac
	COMMUNITY_PATH=$(pwd)
	export COMMUNITY_PATH
	popd >/dev/null || exit

	$COMPOSE_EXEC \
		-f "repository.yml" \
		-f "repository-image-remote.yml" \
		pull || exit

	$COMPOSE_EXEC \
		-f "repository.yml" \
		-f "repository-image-remote.yml" \
		-f "repository-network-dev.yml" \
		-f "repository-debug.yml" \
		up -d || exit
}

stop() {
	$COMPOSE_EXEC \
		-f "repository.yml" \
		stop || exit
}

remove() {
	$COMPOSE_EXEC \
		-f "repository.yml" \
		down || exit
}

build() {
	echo "Building ..."

	[[ -z "${MVN_EXEC_OPTS}" ]] && {
		export MVN_EXEC_OPTS="-ff"
	}

	pushd "${BUILD_PATH}/../build/elasticsearch" >/dev/null || exit
	$MVN_EXEC $MVN_EXEC_OPTS -Dmaven.test.skip=true clean install || exit
	popd >/dev/null || exit
	pushd "${BUILD_PATH}/../build/postgresql" >/dev/null || exit
	$MVN_EXEC $MVN_EXEC_OPTS -Dmaven.test.skip=true clean install || exit
	popd >/dev/null || exit
	pushd "${BUILD_PATH}" >/dev/null || exit
	$MVN_EXEC $MVN_EXEC_OPTS -Dmaven.test.skip=true clean install || exit
	popd >/dev/null || exit
}

reload() {
	echo "Reloading ..."

	$COMPOSE_EXEC \
		-f "repository.yml" \
		exec repository-service \
		touch tomcat/webapps/edu-sharing/WEB-INF/web.xml || exit

	echo "Done."
}

watch() {
	[[ -z "${CLI_OPT2}" ]] && {
		echo ""
		echo "Usage: ${CLI_CMD} ${CLI_OPT1} <edu-sharing community repository>"
		exit
	}

	case $CLI_OPT2 in
		/*) pushd "${CLI_OPT2}" >/dev/null || exit ;;
		*) pushd "${ROOT_PATH}/${CLI_OPT2}" >/dev/null || exit ;;
	esac
	COMMUNITY_PATH=$(pwd)
	export COMMUNITY_PATH
	popd >/dev/null || exit

	echo "Watching ..."
	fswatch -o "${COMMUNITY_PATH}/Backend/services/webapp/target/edu-sharing.war" | tee /dev/tty | xargs -n1 \
		$COMPOSE_EXEC \
			-f "repository.yml" \
			exec repository-service \
			touch tomcat/webapps/edu-sharing/WEB-INF/web.xml
}

case "${CLI_OPT1}" in
rstart)
	init && rstart && note
	;;
rtest)
	init && rtest && logs
	;;
rdebug)
	init && rdebug && logs
	;;
build)
	build
	;;
lstart)
	init && lstart && note
	;;
ltest)
	init && ltest && logs
	;;
ldebug)
	init && ldebug && logs
	;;
reload)
	reload
	;;
watch)
	watch
	;;
info)
	info
	;;
logs)
	logs
	;;
ps)
	ps
	;;
stop)
	stop
	;;
remove)
	remove
	;;
purge)
	purge
	;;
*)
	echo ""
	echo "Usage: ${CLI_CMD} [option]"
	echo ""
	echo "Option:"
	echo ""
	echo "  - rstart            startup containers from remote images"
	echo "  - rtest             startup containers from remote images with dev ports"
	echo "  - rdebug <path>     startup containers from remote images with dev ports and artifacts"
	echo ""
	echo "  - build             build local images"
	echo ""
	echo "  - lstart            startup containers from local images"
	echo "  - ltest             startup containers from local images with dev ports"
	echo "  - ldebug <path>     startup containers from local images with dev ports and artifacts"
	echo ""
	echo "  - reload            reload services"
	echo "  - watch <path>      reload services if webapp artifact changed"
	echo ""
	echo "  - info              show information"
	echo "  - logs              show logs"
	echo "  - ps                show containers"
	echo ""
	echo "  - stop              stop all containers"
	echo "  - remove            remove all containers"
	echo ""
	echo "  - purge             purge all data volumes"
	echo ""
	;;
esac

popd >/dev/null || exit
popd >/dev/null || exit

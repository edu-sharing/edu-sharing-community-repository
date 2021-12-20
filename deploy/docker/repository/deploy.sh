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

[[ -f ".env" ]] && source .env

info() {
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

compose_yml() {

	COMPOSE_BASE_FILE="$1"
	COMPOSE_DIRECTORY="$(dirname "$COMPOSE_BASE_FILE")"
	COMPOSE_FILE_NAME="$(basename "$COMPOSE_BASE_FILE" | cut -f 1 -d '.' )" # without extension


	COMPOSE_FILE="$COMPOSE_DIRECTORY/$COMPOSE_FILE_NAME.yml"
	COMPOSE_LIST=
	if [[ -f "$COMPOSE_FILE" ]]; then
		COMPOSE_LIST="$COMPOSE_LIST -f $COMPOSE_FILE"
  fi

	shift || {
		echo "$COMPOSE_LIST"
		exit
	}

	while true; do
		flag="$1"
		shift || break

		COMPOSE_FILE=""
		case "$flag" in
			-dev) COMPOSE_FILE="$COMPOSE_DIRECTORY/$COMPOSE_FILE_NAME-network-dev.yml" ;;
			-prd) COMPOSE_FILE="$COMPOSE_DIRECTORY/$COMPOSE_FILE_NAME-network-prd.yml" ;;
			-debug) COMPOSE_FILE="$COMPOSE_DIRECTORY/$COMPOSE_FILE_NAME-debug.yml" ;;
			-remote) COMPOSE_FILE="$COMPOSE_DIRECTORY/$COMPOSE_FILE_NAME-image-remote.yml" ;;
			*) {
					echo "error: unknown flag: $flag"
					echo ""
					echo "valid flags are:"
					echo "  -dev"
					echo "  -prd"
					echo "  -debug"
					echo "  -remote"
				} >&2
				exit 1 ;;
		esac

		if [[ -f "$COMPOSE_FILE" ]]; then
			COMPOSE_LIST="$COMPOSE_LIST -f $COMPOSE_FILE"
		fi
	done

	echo $COMPOSE_LIST
}

compose_all_plugins() {
	COMPOSE_LIST=
	for plugin in plugin*/ ; do
		[ ! -d $plugin ] && continue
		COMPOSE_PLUGIN="$(compose_yml "./$plugin$(basename $plugin).yml" "$@")"
		COMPOSE_LIST="$COMPOSE_LIST $COMPOSE_PLUGIN"
	done

	echo $COMPOSE_LIST
}

init() {
	docker volume create "${COMPOSE_NAME}_repository-mongo-volume" || exit
	docker volume create "${COMPOSE_NAME}_repository-database-volume" || exit
	docker volume create "${COMPOSE_NAME}_repository-search-elastic-volume" || exit
	docker volume create "${COMPOSE_NAME}_repository-search-solr4-volume" || exit
	docker volume create "${COMPOSE_NAME}_repository-service-volume-data" || exit
	docker volume create "${COMPOSE_NAME}_repository-service-volume-shared" || exit
}

purge() {
	docker volume rm "${COMPOSE_NAME}_repository-mongo-volume" || exit
	docker volume rm -f "${COMPOSE_NAME}_repository-database-volume" || exit
	docker volume rm -f "${COMPOSE_NAME}_repository-search-elastic-volume" || exit
	docker volume rm -f "${COMPOSE_NAME}_repository-search-solr4-volume" || exit
	docker volume rm -f "${COMPOSE_NAME}_repository-service-volume-data" || exit
	docker volume rm -f "${COMPOSE_NAME}_repository-service-volume-shared" || exit
}

logs() {
	COMPOSE_LIST="$(compose_yml repository.yml) $(compose_all_plugins)"

	$COMPOSE_EXEC \
		$COMPOSE_LIST \
		logs -f || exit
}

ps() {
	COMPOSE_LIST="$(compose_yml repository.yml) $(compose_all_plugins)"

	$COMPOSE_EXEC \
		$COMPOSE_LIST \
		ps || exit
}

lstart() {
	COMPOSE_LIST="$(compose_yml repository.yml -prd) $(compose_all_plugins -prd)"

	$COMPOSE_EXEC \
		$COMPOSE_LIST \
		up -d || exit
}

ltest() {
	COMPOSE_LIST="$(compose_yml repository.yml -dev) $(compose_all_plugins -dev)"
	echo "$COMPOSE_LIST"

	$COMPOSE_EXEC \
		$COMPOSE_LIST \
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

	COMPOSE_LIST="$(compose_yml repository.yml -dev -debug) $(compose_all_plugins -dev)"

	$COMPOSE_EXEC \
		$COMPOSE_LIST \
		up -d || exit
}

rstart() {

	COMPOSE_LIST="$(compose_yml repository.yml -remote -prd) $(compose_all_plugins -remote -prd)"

	$COMPOSE_EXEC \
		$COMPOSE_LIST \
		pull || exit

	$COMPOSE_EXEC \
		$COMPOSE_LIST \
		up -d || exit
}

rtest() {
	COMPOSE_LIST="$(compose_yml repository.yml -remote -dev) $(compose_all_plugins -remote -dev)"

	$COMPOSE_EXEC \
		$COMPOSE_LIST \
		pull || exit

	$COMPOSE_EXEC \
		$COMPOSE_LIST \
		up -d || exit
}
#TODO DEBUG PLUGIN
#TODO CHECK CLEAN BEFORE BUILD

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


	COMPOSE_LIST="$(compose_yml repository.yml -remote -dev -debug) $(compose_all_plugins -remote -dev)"

	$COMPOSE_EXEC \
		$COMPOSE_LIST \
		pull || exit

	$COMPOSE_EXEC \
		$COMPOSE_LIST \
		up -d || exit
}

stop() {
	COMPOSE_LIST="$(compose_yml repository.yml -remote -dev) $(compose_all_plugins -remote -dev)"

	$COMPOSE_EXEC \
		$COMPOSE_LIST \
		stop || exit
}

remove() {

	COMPOSE_LIST="$(compose_yml repository.yml -remote -dev) $(compose_all_plugins -remote -dev)"

	$COMPOSE_EXEC \
		$COMPOSE_LIST \
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

	COMPOSE_LIST="$(compose_yml repository.yml) $(compose_all_plugins)"

	$COMPOSE_EXEC \
		$COMPOSE_LIST \
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

	COMPOSE_LIST="$(compose_yml repository.yml) $(compose_all_plugins)"

	echo "Watching ..."
	fswatch -o "${COMMUNITY_PATH}/Backend/services/webapp/target/edu-sharing.war" | tee /dev/tty | xargs -n1 \
		$COMPOSE_EXEC \
		$COMPOSE_LIST \
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

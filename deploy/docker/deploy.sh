#!/bin/bash
set -e
set -o pipefail

ROOT_PATH="$(
	cd "$(dirname ".")"
	pwd -P
)"
export ROOT_PATH

SOURCE_PATH="$(cd "$(dirname "${BASH_SOURCE[0]}")" >/dev/null && pwd)"
pushd "${SOURCE_PATH}" >/dev/null || exit

COMPOSE_PROJECT=$(basename $(cd ../../.. && pwd))
COMPOSE_WORKTREE=""
COMPOSE_DIR="compose/target/compose"

pushd ".." >/dev/null || exit
[[ -f ".info" ]] && {
	set -a
	source .info
	set +a
}
[[ -f ".env" ]] && {
	set -a
	source .env
	set +a
}
popd >/dev/null || exit

[[ -f ".env" ]] && {
	cp -f ".env" "${COMPOSE_DIR}"
}

export COMPOSE_NAME="${COMPOSE_PROJECT_NAME:-$(echo "${COMPOSE_PROJECT}-${COMPOSE_WORKTREE}-docker-$(git rev-parse --abbrev-ref HEAD)" | sed 's|[\/\.]|-|g')}"

case "$(uname)" in
MINGW*)
	COMPOSE_EXEC="winpty docker-compose -p $COMPOSE_NAME"
	;;
*)
	COMPOSE_EXEC="docker-compose -p $COMPOSE_NAME"
	;;
esac

export COMPOSE_EXEC

export CLI_CMD="$0"
export CLI_OPT1="$1"
export CLI_OPT2="$2"

export MAVEN_CMD="mvn"

[[ -z "${MAVEN_CMD_OPTS}" ]] && {
	export MAVEN_CMD_OPTS="-q -ff"
}

[[ -z "${MAVEN_HOME}" ]] && {
	export MAVEN_HOME="$HOME/.m2"
}

[[ ! -d "${COMPOSE_DIR}" ]] && {
	echo "Initializing ..."
	pushd "repository/compose" >/dev/null || exit
	$MAVEN_CMD $MAVEN_CMD_OPTS -Dmaven.test.skip=true install || exit
	popd >/dev/null || exit
	pushd "services/rendering/compose" >/dev/null || exit
	$MAVEN_CMD $MAVEN_CMD_OPTS -Dmaven.test.skip=true install || exit
	popd >/dev/null || exit
	pushd "compose" >/dev/null || exit
	$MAVEN_CMD $MAVEN_CMD_OPTS -Dmaven.test.skip=true package || exit
	popd >/dev/null || exit
}

pushd "${COMPOSE_DIR}" >/dev/null || exit

[[ -f ".env" ]] && source .env

info() {
	echo ""
	echo "#########################################################################"
	echo ""
	echo "cache:"
	echo ""
	echo "  Port:             127.0.0.1:${CACHE_PORT:-7000}"
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
	echo "  Port:             127.0.0.1:${REPOSITORY_DATABASE_PORT:-8000}"
	echo ""
	echo "#########################################################################"
	echo ""
	echo "repository-mongo:"
	echo ""
	echo "  Credentials:"
	echo ""
	echo "    Name:           ${REPOSITORY_MONGO_USER:-repository}"
	echo "    Password:       ${REPOSITORY_MONGO_PASS:-repository}"
	echo ""
	echo "  Database:         ${REPOSITORY_MONGO_DATABASE:-edu-sharing}"
	echo ""
	echo "  Port:             127.0.0.1:${REPOSITORY_MONGO_PORT:-8500}"
	echo ""
	echo "#########################################################################"
	echo ""
	echo "repository-mongo-express:"
	echo ""
	echo "  Port:             http://127.0.0.1:${REPOSITORY_MONGO_EXPRESS_PORT:-8501}"
	echo ""
	echo "#########################################################################"
	echo ""
	echo "repository-search-elastic-index:"
	echo ""
	echo "  JVM:"
	echo ""
	echo "    XMS:            ${REPOSITORY_SEARCH_ELASTIC_INDEX_JAVA_XMS:-1g}"
	echo "    XMX:            ${REPOSITORY_SEARCH_ELASTIC_INDEX_JAVA_XMX:-1g}"
	echo ""
	echo "  Services:"
	echo ""
	echo "    HTTP:           http://127.0.0.1:${REPOSITORY_SEARCH_ELASTIC_INDEX_PORT_HTTP:-8300}/"
	echo "    JPDA:           127.0.0.1:${REPOSITORY_SEARCH_ELASTIC_INDEX_PORT_JPDA:-8301}"
	echo ""
	echo "#########################################################################"
	echo ""
	echo "repository-search-elastic-tracker:"
	echo ""
	echo "  JVM:"
	echo ""
	echo "    XMS:            ${REPOSITORY_SEARCH_ELASTIC_TRACKER_JAVA_XMS:-1g}"
	echo "    XMX:            ${REPOSITORY_SEARCH_ELASTIC_TRACKER_JAVA_XMX:-1g}"
	echo ""
	echo "  Services:"
	echo ""
	echo "    JPDA:           127.0.0.1:${REPOSITORY_SEARCH_ELASTIC_TRACKER_PORT_JPDA:-8401}"
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
	echo "services-rendering-database:"
	echo ""
	echo "  Credentials:"
	echo ""
	echo "    Name:           ${SERVICES_RENDERING_DATABASE_USER:-rendering}"
	echo "    Password:       ${SERVICES_RENDERING_DATABASE_PASS:-rendering}"
	echo ""
	echo "  Database:         ${SERVICES_RENDERING_DATABASE_NAME:-rendering}"
	echo ""
	echo "  Port:             127.0.0.1:${SERVICES_RENDERING_DATABASE_PORT:-9000}"
	echo ""
	echo "#########################################################################"
	echo ""
	echo "services-rendering-service:"
	echo ""
	echo "  Credentials:"
	echo ""
	echo "    Name:           ${SERVICES_RENDERING_DATABASE_USER:-rendering}"
	echo "    Password:       ${SERVICES_RENDERING_DATABASE_PASS:-rendering}"
	echo ""
	echo "  Services:"
	echo ""
	echo "    HTTP:           http://${SERVICES_RENDERING_SERVICE_HOST:-rendering.services.127.0.0.1.nip.io}:${SERVICES_RENDERING_SERVICE_PORT_HTTP:-9100}/esrender/admin/"
	echo ""
	echo "#########################################################################"
	echo ""
	echo ""
}

note() {
	echo ""
	echo "#########################################################################"
	echo ""
	echo "  edu-sharing repository:"
	echo ""
	echo "    http://${REPOSITORY_SERVICE_HOST:-repository.127.0.0.1.nip.io}:${REPOSITORY_SERVICE_PORT_HTTP:-8100}/edu-sharing/"
	echo ""
	echo "    username: admin"
	echo "    password: ${REPOSITORY_SERVICE_ADMIN_PASS:-admin}"
	echo ""
	echo "  edu-sharing services:"
	echo ""
	echo "    rendering:"
	echo ""
	echo "      http://${SERVICES_RENDERING_SERVICE_HOST:-rendering.services.127.0.0.1.nip.io}:${SERVICES_RENDERING_SERVICE_PORT_HTTP:-9100}/esrender/admin/"
	echo ""
	echo "      username: ${SERVICES_RENDERING_DATABASE_USER:-rendering}"
	echo "      password: ${SERVICES_RENDERING_DATABASE_PASS:-rendering}"
	echo ""
	echo "#########################################################################"
	echo ""
	echo "  All exposed ports are bound to network address: ${COMMON_BIND_HOST:-127.0.0.1}."
	echo ""
	echo "#########################################################################"
	echo ""
	echo "  If you need to customize the installation, then:"
	echo ""
	echo "    - make a copy of \".env.sample\" to \".env\""
	echo "    - comment out and update relevant entities inside \".env\""
	echo ""
	echo "#########################################################################"
	echo ""
	echo ""
}

compose() {

	COMPOSE_BASE_FILE="$1"
	COMPOSE_DIRECTORY="$(dirname "$COMPOSE_BASE_FILE")"
	COMPOSE_FILE_NAME="$(basename "$COMPOSE_BASE_FILE" | cut -f 1 -d '.')" # without extension

	COMPOSE_LIST=

	shift && {

		while true; do
			flag="$1"
			shift || break

			COMPOSE_FILE=""
			case "$flag" in
			-common) COMPOSE_FILE="$COMPOSE_DIRECTORY/$COMPOSE_FILE_NAME-common.yml" ;;
			-debug) COMPOSE_FILE="$COMPOSE_DIRECTORY/$COMPOSE_FILE_NAME-debug.yml" ;;
			-dev) COMPOSE_FILE="$COMPOSE_DIRECTORY/$COMPOSE_FILE_NAME-dev.yml" ;;
			-remote) COMPOSE_FILE="$COMPOSE_DIRECTORY/$COMPOSE_FILE_NAME-remote.yml" ;;
			*)
				{
					echo "error: unknown flag: $flag"
					echo ""
					echo "valid flags are:"
					echo "  -common"
					echo "  -debug"
					echo "  -dev"
					echo "  -remote"
				} >&2
				exit 1
				;;
			esac

			if [[ -f "$COMPOSE_FILE" ]]; then
				COMPOSE_LIST="$COMPOSE_LIST -f $COMPOSE_FILE"
			fi

		done

	}

	echo $COMPOSE_LIST
}

compose_plugins() {
	PLUGIN_DIR="$1"
	shift

	COMPOSE_LIST=
	for plugin in $PLUGIN_DIR/plugin*/; do
		[ ! -d $plugin ] && continue
		COMPOSE_PLUGIN="$(compose "./$plugin$(basename $plugin).yml" "$@")"
		COMPOSE_LIST="$COMPOSE_LIST $COMPOSE_PLUGIN"
	done

	echo $COMPOSE_LIST
}

logs() {
	COMPOSE_LIST="$COMPOSE_LIST $(compose edusharing.yml -common)"
	COMPOSE_LIST="$COMPOSE_LIST $(compose repository/repository.yml -common) $(compose_plugins repository -common)"
	COMPOSE_LIST="$COMPOSE_LIST $(compose services/rendering/rendering.yml -common)"
	COMPOSE_LIST="$COMPOSE_LIST $(compose project.yml -common)"

	echo "Use compose set: $COMPOSE_LIST"

	$COMPOSE_EXEC \
		$COMPOSE_LIST \
		logs -f || exit
}

ps() {
	COMPOSE_LIST="$COMPOSE_LIST $(compose edusharing.yml -common)"
	COMPOSE_LIST="$COMPOSE_LIST $(compose repository/repository.yml -common) $(compose_plugins repository -common)"
	COMPOSE_LIST="$COMPOSE_LIST $(compose services/rendering/rendering.yml -common)"
	COMPOSE_LIST="$COMPOSE_LIST $(compose project.yml -common)"

	echo "Use compose set: $COMPOSE_LIST"

	$COMPOSE_EXEC \
		-f $COMPOSE_LIST \
		ps || exit
}

rstart() {
	COMPOSE_LIST="$COMPOSE_LIST $(compose edusharing.yml -common -remote)"
	COMPOSE_LIST="$COMPOSE_LIST $(compose repository/repository.yml -common -remote) $(compose_plugins repository -common -remote)"
	COMPOSE_LIST="$COMPOSE_LIST $(compose services/rendering/rendering.yml -common -remote)"
	COMPOSE_LIST="$COMPOSE_LIST $(compose project.yml -common -remote)"

	echo "Use compose set: $COMPOSE_LIST"

	$COMPOSE_EXEC \
		$COMPOSE_LIST \
		pull || exit

	$COMPOSE_EXEC \
		$COMPOSE_LIST \
		up -d || exit
}

rdebug() {
	COMPOSE_LIST="$COMPOSE_LIST $(compose edusharing.yml -common -remote -debug)"
	COMPOSE_LIST="$COMPOSE_LIST $(compose repository/repository.yml -common -remote -debug) $(compose_plugins repository -common -remote -debug)"
	COMPOSE_LIST="$COMPOSE_LIST $(compose services/rendering/rendering.yml -common -remote -debug)"
	COMPOSE_LIST="$COMPOSE_LIST $(compose project.yml -common -remote -debug)"

	echo "Use compose set: $COMPOSE_LIST"

	$COMPOSE_EXEC \
		$COMPOSE_LIST \
		pull || exit

	$COMPOSE_EXEC \
		$COMPOSE_LIST \
		up -d || exit
}

rdev() {
	pushd "${SOURCE_PATH}/../../.." >/dev/null || exit
	GIT_ROOT=$(pwd)
	export GIT_ROOT
	popd >/dev/null || exit

	COMPOSE_LIST="$COMPOSE_LIST $(compose edusharing.yml -common -remote -debug -dev)"
	COMPOSE_LIST="$COMPOSE_LIST $(compose repository/repository.yml -common -remote -debug -dev) $(compose_plugins repository -common -remote -debug -dev)"
	COMPOSE_LIST="$COMPOSE_LIST $(compose services/rendering/rendering.yml -common -remote -debug -dev)"
	COMPOSE_LIST="$COMPOSE_LIST $(compose project.yml -common -remote -debug -dev)"

	echo "Use compose set: $COMPOSE_LIST"

	$COMPOSE_EXEC \
		$COMPOSE_LIST \
		pull || exit

	$COMPOSE_EXEC \
		$COMPOSE_LIST \
		up -d || exit
}

lstart() {
	COMPOSE_LIST="$COMPOSE_LIST $(compose edusharing.yml -common)"
	COMPOSE_LIST="$COMPOSE_LIST $(compose repository/repository.yml -common) $(compose_plugins repository -common)"
	COMPOSE_LIST="$COMPOSE_LIST $(compose services/rendering/rendering.yml -common)"
	COMPOSE_LIST="$COMPOSE_LIST $(compose project.yml -common)"

	echo "Use compose set: $COMPOSE_LIST"

	$COMPOSE_EXEC \
		$COMPOSE_LIST \
		up -d || exit
}

ldebug() {
	COMPOSE_LIST="$COMPOSE_LIST $(compose edusharing.yml -common -debug)"
	COMPOSE_LIST="$COMPOSE_LIST $(compose repository/repository.yml -common -debug) $(compose_plugins repository -common -debug)"
	COMPOSE_LIST="$COMPOSE_LIST $(compose services/rendering/rendering.yml -common -debug)"
	COMPOSE_LIST="$COMPOSE_LIST $(compose project.yml -common -debug)"

	echo "Use compose set: $COMPOSE_LIST"

	$COMPOSE_EXEC \
		$COMPOSE_LIST \
		up -d || exit
}

ldev() {
	pushd "${SOURCE_PATH}/../../.." >/dev/null || exit
	GIT_ROOT=$(pwd)
	export GIT_ROOT
	popd >/dev/null || exit

	COMPOSE_LIST="$COMPOSE_LIST $(compose edusharing.yml -common -debug -dev)"
	COMPOSE_LIST="$COMPOSE_LIST $(compose repository/repository.yml -common -debug -dev) $(compose_plugins repository -common -debug -dev)"
	COMPOSE_LIST="$COMPOSE_LIST $(compose services/rendering/rendering.yml -common -debug -dev)"
	COMPOSE_LIST="$COMPOSE_LIST $(compose project.yml -common -debug -dev)"

	echo "Use compose set: $COMPOSE_LIST"

	$COMPOSE_EXEC \
		$COMPOSE_LIST \
		up -d || exit
}

stop() {
	COMPOSE_LIST="$COMPOSE_LIST $(compose edusharing.yml -common -debug)"
	COMPOSE_LIST="$COMPOSE_LIST $(compose repository/repository.yml -common -debug) $(compose_plugins repository -common -debug)"
	COMPOSE_LIST="$COMPOSE_LIST $(compose services/rendering/rendering.yml -common -debug)"
	COMPOSE_LIST="$COMPOSE_LIST $(compose project.yml -common -debug)"

	echo "Use compose set: $COMPOSE_LIST"

	$COMPOSE_EXEC \
		$COMPOSE_LIST \
		stop || exit
}

remove() {
	read -p "Are you sure you want to continue? [y/N] " answer
	case ${answer:0:1} in
	y | Y)
		COMPOSE_LIST="$COMPOSE_LIST $(compose edusharing.yml -common -debug)"
		COMPOSE_LIST="$COMPOSE_LIST $(compose repository/repository.yml -common -debug) $(compose_plugins repository -common -debug)"
		COMPOSE_LIST="$COMPOSE_LIST $(compose services/rendering/rendering.yml -common -debug)"
		COMPOSE_LIST="$COMPOSE_LIST $(compose project.yml -common -debug)"

		echo "Use compose set: $COMPOSE_LIST"

		$COMPOSE_EXEC \
			$COMPOSE_LIST \
			down -v || exit
		;;
	*)
		echo Canceled.
		;;
	esac
}

reload() {
	[[ -z "${CLI_OPT2}" ]] && {
		CLI_OPT2="edu-sharing"
	}

	COMPOSE_LIST="$COMPOSE_LIST $(compose edusharing.yml -common)"
	COMPOSE_LIST="$COMPOSE_LIST $(compose repository/repository.yml -common) $(compose_plugins repository -common)"
	COMPOSE_LIST="$COMPOSE_LIST $(compose services/rendering/rendering.yml -common)"
	COMPOSE_LIST="$COMPOSE_LIST $(compose project.yml -common)"

	echo "Use compose set: $COMPOSE_LIST"

	echo "Reinstall"
	$COMPOSE_EXEC \
		$COMPOSE_LIST \
		exec repository-service \
		reinstall.sh || exit

	echo "Reloading $CLI_OPT2 ..."
	$COMPOSE_EXEC \
		$COMPOSE_LIST \
		exec repository-service \
		touch tomcat/webapps/$CLI_OPT2/WEB-INF/web.xml || exit

	echo "Done."
}

ci() {
	COMPOSE_LIST1="$(compose_plugins repository -remote)"
	COMPOSE_LIST2="$(compose_plugins services/rendering -remote)"

  [[ -n $COMPOSE_LIST1 || -n $COMPOSE_LIST2 ]] && {
		echo "Use compose set: $COMPOSE_LIST1 $COMPOSE_LIST2"

		$COMPOSE_EXEC \
			$COMPOSE_LIST1 $COMPOSE_LIST2 \
			pull || exit
	}

	COMPOSE_LIST="$COMPOSE_LIST $(compose edusharing.yml -common)"
	COMPOSE_LIST="$COMPOSE_LIST $(compose repository/repository.yml -common) $(compose_plugins repository -common -remote)"
	COMPOSE_LIST="$COMPOSE_LIST $(compose services/rendering/rendering.yml -common)"
	COMPOSE_LIST="$COMPOSE_LIST $(compose project.yml -common)"

	echo "Use compose set: $COMPOSE_LIST"

	$COMPOSE_EXEC \
		$COMPOSE_LIST \
		up -d || exit
}

case "${CLI_OPT1}" in
rstart)
	rstart && note
	;;
rdebug)
	rdebug && info
	;;
rdev)
	rdev && info
	;;
lstart)
	lstart && note
	;;
ldebug)
	ldebug && info
	;;
ldev)
	ldev && info
	;;
reload)
	reload
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
ci)
	ci
	;;
*)
	echo ""
	echo "Usage: ${CLI_CMD} [option]"
	echo ""
	echo "Option:"
	echo ""
	echo "  - rstart            startup containers from remote images"
	echo "  - rdebug            startup containers from remote images with dev ports"
	echo "  - rdev              startup containers from remote images with dev ports and artifacts"
	echo ""
	echo "  - lstart            startup containers from local images"
	echo "  - ldebug            startup containers from local images with dev ports"
	echo "  - ldev              startup containers from local images with dev ports and artifacts"
	echo ""
	echo "  - ci                startup containers inside ci-pipeline"
	echo ""
	echo "  - reload [service]  reload services [edu-sharing]"
	echo ""
	echo "  - info              show information"
	echo "  - logs              show logs"
	echo "  - ps                show containers"
	echo ""
	echo "  - stop              stop all containers"
	echo "  - remove            remove all containers and volumes"
	echo ""
	;;
esac

popd >/dev/null || exit
popd >/dev/null || exit

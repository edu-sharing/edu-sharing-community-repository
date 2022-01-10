#!/bin/bash
set -e
set -o pipefail

GIT_BRANCH="$(git rev-parse --abbrev-ref HEAD | sed 's/\//-/')"
export COMPOSE_NAME="${COMPOSE_PROJECT_NAME:-installer-$GIT_BRANCH}"

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
export CLI_OPT3="$3"
export CLI_OPT4="$4"

export MAVEN_CMD="mvn"

[[ -z "${MAVEN_CMD_OPTS}" ]] && {
	export MAVEN_CMD_OPTS="-q -ff"
}

[[ -z "${MAVEN_HOME}" ]] && {
	export MAVEN_HOME="$HOME/.m2"
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

COMPOSE_DIR="compose/debian/bullseye/target/compose"

[[ -f ".env" ]] && {
	cp -f ".env" "${COMPOSE_DIR}"
}


[[ ! -d "${COMPOSE_DIR}" ]] && {
	echo "Initializing ..."
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
	echo "  edu-sharing community repository:"
	echo ""
	echo "    http://${REPOSITORY_SERVICE_HOST:-repository.127.0.0.1.nip.io}:${REPOSITORY_SERVICE_PORT:-8100}/edu-sharing/"
	echo ""
	echo "    username: admin"
	echo "    password: ${REPOSITORY_SERVICE_ADMIN_PASS:-admin}"
	echo ""
	echo "  edu-sharing community services rendering:"
	echo ""
	echo "    http://${RENDERING_SERVICE_HOST:-rendering.127.0.0.1.nip.io}:${RENDERING_SERVICE_PORT:-9100}/esrender/admin/"
	echo ""
	echo "    username: ${RENDERING_DATABASE_USER:-rendering}"
	echo "    password: ${RENDERING_DATABASE_PASS:-rendering}"
	echo ""
	echo "#########################################################################"
	echo ""
}

compose_only() {

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
				-ci) COMPOSE_FILE="$COMPOSE_DIRECTORY/$COMPOSE_FILE_NAME-ci.yml" ;;
      	-local) COMPOSE_FILE="$COMPOSE_DIRECTORY/$COMPOSE_FILE_NAME-local.yml" ;;
      	-remote) COMPOSE_FILE="$COMPOSE_DIRECTORY/$COMPOSE_FILE_NAME-remote.yml" ;;
			*)
				{
					echo "error: unknown flag: $flag"
					echo ""
					echo "valid flags are:"
					echo "  -ci"
          echo "  -local"
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

compose_all() {

	COMPOSE_BASE_FILE="$1"
	COMPOSE_DIRECTORY="$(dirname "$COMPOSE_BASE_FILE")"
	COMPOSE_FILE_NAME="$(basename "$COMPOSE_BASE_FILE" | cut -f 1 -d '.')" # without extension

	COMPOSE_LIST=

	COMPOSE_FILE="$COMPOSE_DIRECTORY/$COMPOSE_FILE_NAME.yml"
	if [[ -f "$COMPOSE_FILE" ]]; then
		COMPOSE_LIST="$COMPOSE_LIST -f $COMPOSE_FILE"
	fi

	shift && {

		while true; do
			flag="$1"
			shift || break

			COMPOSE_LIST="$COMPOSE_LIST $(compose_only "$COMPOSE_BASE_FILE" "$flag")"
		done

	}

	echo $COMPOSE_LIST
}

compose_only_plugins() {
	COMPOSE_LIST=
	for plugin in plugin*/; do
		[ ! -d "$plugin" ] && continue
		COMPOSE_PLUGIN="$(compose_only "./$plugin$(basename $plugin).yml" "$@")"
		COMPOSE_LIST="$COMPOSE_LIST $COMPOSE_PLUGIN"
	done

	echo $COMPOSE_LIST
}

compose_all_plugins() {

	COMPOSE_LIST=
	for plugin in plugin*/; do
		[ ! -d "$plugin" ] && continue
		COMPOSE_PLUGIN="$(compose_all "./$plugin$(basename $plugin).yml" "$@")"
		COMPOSE_LIST="$COMPOSE_LIST $COMPOSE_PLUGIN"
	done

	echo $COMPOSE_LIST
}

logs() {
	COMPOSE_LIST="$(compose_all aio.yml) $(compose_all_plugins)"

	echo "Use compose set: $COMPOSE_LIST"

	$COMPOSE_EXEC \
		$COMPOSE_LIST \
		logs -f || exit
}

ps() {
	COMPOSE_LIST="$(compose_all aio.yml) $(compose_all_plugins)"

	echo "Use compose set: $COMPOSE_LIST"

	$COMPOSE_EXEC \
		$COMPOSE_LIST \
		ps || exit
}

init() {
	rm -f .env.repository .env.rendering .env.elastic .env.transform
	{
		echo "REPOSITORY_SERVICE_ADMIN_PASS=${REPOSITORY_SERVICE_ADMIN_PASS:-admin}"
		echo "REPOSITORY_SERVICE_HOST_EXTERNAL=${REPOSITORY_SERVICE_HOST:-repository.127.0.0.1.nip.io}"
		echo "REPOSITORY_SERVICE_HOST_INTERNAL=repository"
		echo "REPOSITORY_SERVICE_PORT_EXTERNAL=${REPOSITORY_SERVICE_PORT:-8100}"
		echo "REPOSITORY_SERVICE_PORT_INTERNAL=80"
		echo "REPOSITORY_SERVICE_HOME_APPID=${COMPOSE_PROJECT_NAME:-compose}"

		# plugin elastic (please check deploy/installer/repository/scripts/../load_config.sh inside plugin)
		echo "REPOSITORY_SEARCH_ELASTIC_HOST=elastic"
		echo "REPOSITORY_SEARCH_ELASTIC_PORT=9200"

		# plugin transform (please check deploy/installer/repository/scripts/../load_config.sh inside plugin)
		echo "REPOSITORY_TRANSFORM_SERVER_HOST=transform"
		echo "REPOSITORY_TRANSFORM_SERVER_PORT=8080"
	} >> .env.repository

	{
		echo "RENDERING_DATABASE_PASS=${RENDERING_DATABASE_PASS:-rendering}"
		echo "RENDERING_DATABASE_USER=${RENDERING_DATABASE_USER:-rendering}"
		echo "RENDERING_SERVICE_HOST_EXTERNAL=${RENDERING_SERVICE_HOST:-rendering.127.0.0.1.nip.io}"
		echo "RENDERING_SERVICE_HOST_INTERNAL=rendering"
		echo "RENDERING_SERVICE_PORT_EXTERNAL=${RENDERING_SERVICE_PORT:-9100}"
		echo "RENDERING_SERVICE_PORT_INTERNAL=80"
		echo "REPOSITORY_SERVICE_ADMIN_PASS=${REPOSITORY_SERVICE_ADMIN_PASS:-admin}"
		echo "REPOSITORY_SERVICE_HOST=repository"

		echo "REPOSITORY_SERVICE_PORT=80"
	} >> .env.rendering

	# plugin elastic (please check deploy/installer/tracker/scripts/../install.sh inside plugin)
	{
		echo "REPOSITORY_SERVICE_ADMIN_PASS=${REPOSITORY_SERVICE_ADMIN_PASS:-admin}"
    echo "REPOSITORY_SERVICE_HOST_INTERNAL=repository"
    echo "REPOSITORY_SERVICE_PORT_INTERNAL=80"
	} >> .env.elastic

	# plugin transform (please check deploy/installer/server/scripts/../install.sh inside plugin)
	{
		echo ""
	} >> .env.transform

}

rstart() {
	COMPOSE_LIST="$(compose_all aio.yml -remote) $(compose_all_plugins -remote)"

	echo "Use compose set: $COMPOSE_LIST"

	$COMPOSE_EXEC \
		$COMPOSE_LIST \
		pull || exit

	$COMPOSE_EXEC \
		$COMPOSE_LIST \
		up -d || exit
}

lstart() {
	COMPOSE_LIST="$(compose_all aio.yml -local) $(compose_all_plugins -local)"

	echo "Use compose set: $COMPOSE_LIST"

	$COMPOSE_EXEC \
		$COMPOSE_LIST \
		up -d || exit
}

stop() {
	COMPOSE_LIST="$(compose_all aio.yml) $(compose_all_plugins)"

	echo "Use compose set: $COMPOSE_LIST"

	$COMPOSE_EXEC \
		$COMPOSE_LIST \
		stop || exit
}

remove() {
		COMPOSE_LIST="$(compose_all aio.yml) $(compose_all_plugins)"

		echo "Use compose set: $COMPOSE_LIST"

		$COMPOSE_EXEC \
			$COMPOSE_LIST \
			down || exit
}

ci() {
	COMPOSE_LIST1="$(compose_only_plugins repository -remote)"
	COMPOSE_LIST2="$(compose_only_plugins rendering -remote)"

  [[ -n $COMPOSE_LIST1 || -n $COMPOSE_LIST2 ]] && {
		echo "Use compose set: $COMPOSE_LIST1 $COMPOSE_LIST2"

		$COMPOSE_EXEC \
			$COMPOSE_LIST1 $COMPOSE_LIST2 \
			pull || exit
	}

	COMPOSE_LIST="$(compose_all aio.yml -local -ci) $(compose_all_plugins -remote -ci)"

	echo "Use compose set: $COMPOSE_LIST"

	$COMPOSE_EXEC \
		$COMPOSE_LIST \
		up -d || exit
}

case "${CLI_OPT1}" in
rstart)
	init && rstart && info
	;;
lstart)
	init && lstart && info
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
	init && ci
	;;
*)
	echo ""
	echo "Usage: ${CLI_CMD} [option]"
	echo ""
	echo "Option:"
	echo ""
	echo "  - rstart            startup containers from remote images"
	echo "  - lstart            startup containers from local images"
	echo ""
	echo "  - ci                startup containers inside ci-pipeline"
	echo ""
	echo "  - info              show information"
	echo "  - logs              show logs"
	echo "  - ps                show containers"
	echo ""
	echo "  - stop              stop all containers"
	echo "  - remove            remove all containers"
	echo ""
	;;
esac

popd >/dev/null || exit
popd >/dev/null || exit
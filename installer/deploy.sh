#!/bin/bash
set -e
set -o pipefail

GIT_BRANCH="$(git rev-parse --abbrev-ref HEAD | sed 's|[\/\.]|-|g')"
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

PLATFORM="debian/bullseye"
COMPOSE_DIR="compose/$PLATFORM/target/compose"

[[ ! -d "${COMPOSE_DIR}" ]] && {
	echo "Initializing ..."
	pushd "rendering/compose/$PLATFORM" >/dev/null || exit
	$MAVEN_CMD $MAVEN_CMD_OPTS -Dmaven.test.skip=true install || exit
	popd >/dev/null || exit
	pushd "repository/compose/$PLATFORM" >/dev/null || exit
	$MAVEN_CMD $MAVEN_CMD_OPTS -Dmaven.test.skip=true install || exit
	popd >/dev/null || exit
	pushd "compose/$PLATFORM" >/dev/null || exit
	$MAVEN_CMD $MAVEN_CMD_OPTS -Dmaven.test.skip=true package || exit
	popd >/dev/null || exit
}

[[ -f ".env" ]] && {
	cp -f ".env" "${COMPOSE_DIR}"
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
				-ci) COMPOSE_FILE="$COMPOSE_DIRECTORY/$COMPOSE_FILE_NAME-ci.yml" ;;
      	-local) COMPOSE_FILE="$COMPOSE_DIRECTORY/$COMPOSE_FILE_NAME-local.yml" ;;
      	-remote) COMPOSE_FILE="$COMPOSE_DIRECTORY/$COMPOSE_FILE_NAME-remote.yml" ;;
			*)
				{
					echo "error: unknown flag: $flag"
					echo ""
					echo "valid flags are:"
					echo "  -common"
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
	COMPOSE_LIST="$COMPOSE_LIST $(compose rendering/rendering.yml -common) $(compose_plugins rendering -common)"

	echo "Use compose set: $COMPOSE_LIST"

	$COMPOSE_EXEC \
		$COMPOSE_LIST \
		logs -f || exit
}

ps() {
	COMPOSE_LIST="$COMPOSE_LIST $(compose edusharing.yml -common)"
	COMPOSE_LIST="$COMPOSE_LIST $(compose repository/repository.yml -common) $(compose_plugins repository -common)"
	COMPOSE_LIST="$COMPOSE_LIST $(compose rendering/rendering.yml -common) $(compose_plugins rendering -common)"

	echo "Use compose set: $COMPOSE_LIST"

	$COMPOSE_EXEC \
		-f $COMPOSE_LIST \
		ps || exit
}

init() {
	mkdir -p rendering
	mkdir -p repository/plugin-elastic
	mkdir -p repository/plugin-transform

	rm -f rendering/.env repository/.env repository/plugin-elastic/.env repository/plugin-transform/.env

	{
		echo "RENDERING_DATABASE_PASS=${RENDERING_DATABASE_PASS:-rendering}"
		echo "RENDERING_DATABASE_USER=${RENDERING_DATABASE_USER:-rendering}"

		echo "RENDERING_SERVICE_HOST_EXTERNAL=${RENDERING_SERVICE_HOST:-rendering.127.0.0.1.nip.io}"
		echo "RENDERING_SERVICE_PORT_EXTERNAL=${RENDERING_SERVICE_PORT:-9100}"

		echo "RENDERING_SERVICE_HOST_INTERNAL=rendering"
		echo "RENDERING_SERVICE_PORT_INTERNAL=80"

		echo "REPOSITORY_SERVICE_ADMIN_PASS=${REPOSITORY_SERVICE_ADMIN_PASS:-admin}"

		echo "REPOSITORY_SERVICE_HOST=repository"
		echo "REPOSITORY_SERVICE_PORT=80"
	} >> rendering/.env

	{
		echo "REPOSITORY_SERVICE_HOME_APPID=${COMPOSE_PROJECT_NAME:-compose}"

		echo "REPOSITORY_SERVICE_ADMIN_PASS=${REPOSITORY_SERVICE_ADMIN_PASS:-admin}"

		echo "REPOSITORY_SERVICE_HOST_EXTERNAL=${REPOSITORY_SERVICE_HOST:-repository.127.0.0.1.nip.io}"
		echo "REPOSITORY_SERVICE_PORT_EXTERNAL=${REPOSITORY_SERVICE_PORT:-8100}"

		echo "REPOSITORY_SERVICE_HOST_INTERNAL=repository"
		echo "REPOSITORY_SERVICE_PORT_INTERNAL=80"

		# plugin cluster (please check deploy/installer/repository/scripts/../load_config.sh inside plugin)
		echo "REPOSITORY_SERVICE_CLUSTER_NETWORK_TCPIP_MEMBERS=repository"

		# plugin elastic (please check deploy/installer/repository/scripts/../load_config.sh inside plugin)
		echo "REPOSITORY_SEARCH_ELASTIC_HOST=repository-elastic"

		# plugin transform (please check deploy/installer/repository/scripts/../load_config.sh inside plugin)
		echo "REPOSITORY_TRANSFORM_SERVER_HOST=repository-transform"
	} >> repository/.env

	# plugin elastic (please check deploy/installer/tracker/scripts/../install.sh inside plugin)
	{
		echo "REPOSITORY_SERVICE_ADMIN_PASS=${REPOSITORY_SERVICE_ADMIN_PASS:-admin}"

    echo "REPOSITORY_SERVICE_HOST=repository"
    echo "REPOSITORY_SERVICE_PORT=80"
	} >> repository/plugin-elastic/.env

	# plugin transform (please check deploy/installer/server/scripts/../install.sh inside plugin)
	{
		echo "REPOSITORY_TRANSFORM_SERVER_BIND=0.0.0.0"
		echo "REPOSITORY_TRANSFORM_MANAGEMENT_SERVER_BIND=0.0.0.0"
	} >> repository/plugin-transform/.env

}

rstart() {
	COMPOSE_LIST="$COMPOSE_LIST $(compose edusharing.yml -common -remote)"
	COMPOSE_LIST="$COMPOSE_LIST $(compose repository/repository.yml -common -remote) $(compose_plugins repository -common -remote)"
	COMPOSE_LIST="$COMPOSE_LIST $(compose rendering/rendering.yml -common -remote) $(compose_plugins rendering -common -remote)"

	echo "Use compose set: $COMPOSE_LIST"

	$COMPOSE_EXEC \
		$COMPOSE_LIST \
		pull || exit

	$COMPOSE_EXEC \
		$COMPOSE_LIST \
		up -d || exit
}

lstart() {
	COMPOSE_LIST="$COMPOSE_LIST $(compose edusharing.yml -common -local)"
	COMPOSE_LIST="$COMPOSE_LIST $(compose repository/repository.yml -common -local) $(compose_plugins repository -common -local)"
	COMPOSE_LIST="$COMPOSE_LIST $(compose rendering/rendering.yml -common -local) $(compose_plugins rendering -common -local)"

	echo "Use compose set: $COMPOSE_LIST"

	$COMPOSE_EXEC \
		$COMPOSE_LIST \
		up -d || exit
}

stop() {
	COMPOSE_LIST="$COMPOSE_LIST $(compose edusharing.yml -common)"
	COMPOSE_LIST="$COMPOSE_LIST $(compose repository/repository.yml -common) $(compose_plugins repository -common)"
	COMPOSE_LIST="$COMPOSE_LIST $(compose rendering/rendering.yml -common) $(compose_plugins rendering -common)"

	echo "Use compose set: $COMPOSE_LIST"

	$COMPOSE_EXEC \
		$COMPOSE_LIST \
		stop || exit
}

remove() {
	read -p "Are you sure you want to continue? [y/N] " answer
	case ${answer:0:1} in
	y | Y)
		COMPOSE_LIST="$COMPOSE_LIST $(compose edusharing.yml -common)"
		COMPOSE_LIST="$COMPOSE_LIST $(compose repository/repository.yml -common) $(compose_plugins repository -common)"
		COMPOSE_LIST="$COMPOSE_LIST $(compose rendering/rendering.yml -common) $(compose_plugins rendering -common)"

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
	echo "  - remove            remove all containers and volumes"
	echo ""
	;;
esac

popd >/dev/null || exit
popd >/dev/null || exit

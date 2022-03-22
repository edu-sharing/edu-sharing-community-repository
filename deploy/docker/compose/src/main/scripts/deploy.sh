#!/bin/bash
set -e
set -o pipefail

GIT_BRANCH="$(echo '${project.version}' | sed 's|[\/\.]|-|g')"
export COMPOSE_NAME="${COMPOSE_PROJECT_NAME:-edusharing-docker-$GIT_BRANCH}"

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

ROOT_PATH="$(
	cd "$(dirname ".")"
	pwd -P
)"
export ROOT_PATH

pushd "$(cd "$(dirname "${BASH_SOURCE[0]}")" >/dev/null && pwd)" >/dev/null || exit

COMPOSE_DIR="."

[[ -f ".env" ]] && {
	cp -f ".env" "${COMPOSE_DIR}"
}

pushd "${COMPOSE_DIR}" >/dev/null || exit

if [[ -f ".env" ]] ; then
	source .env
else
	REPOSITORY_SERVICE_HOST="127.0.0.1"
	SERVICES_RENDERING_SERVICE_HOST="127.0.0.1"
fi

info() {
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
			-remote) COMPOSE_FILE="$COMPOSE_DIRECTORY/$COMPOSE_FILE_NAME-remote.yml" ;;
			*)
				{
					echo "error: unknown flag: $flag"
					echo ""
					echo "valid flags are:"
					echo "  -common"
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

	echo "Use compose set: $COMPOSE_LIST"

	$COMPOSE_EXEC \
		$COMPOSE_LIST \
		logs -f || exit
}

ps() {
	COMPOSE_LIST="$COMPOSE_LIST $(compose edusharing.yml -common)"
	COMPOSE_LIST="$COMPOSE_LIST $(compose repository/repository.yml -common) $(compose_plugins repository -common)"
	COMPOSE_LIST="$COMPOSE_LIST $(compose services/rendering/rendering.yml -common)"

	echo "Use compose set: $COMPOSE_LIST"

	$COMPOSE_EXEC \
		-f $COMPOSE_LIST \
		ps || exit
}

rstart() {
	COMPOSE_LIST="$COMPOSE_LIST $(compose edusharing.yml -common -remote)"
	COMPOSE_LIST="$COMPOSE_LIST $(compose repository/repository.yml -common -remote) $(compose_plugins repository -common -remote)"
	COMPOSE_LIST="$COMPOSE_LIST $(compose services/rendering/rendering.yml -common -remote)"

	echo "Use compose set: $COMPOSE_LIST"

	$COMPOSE_EXEC \
		$COMPOSE_LIST \
		pull || exit

	$COMPOSE_EXEC \
		$COMPOSE_LIST \
		up -d || exit
}

stop() {
	COMPOSE_LIST="$COMPOSE_LIST $(compose edusharing.yml -common)"
	COMPOSE_LIST="$COMPOSE_LIST $(compose repository/repository.yml -common) $(compose_plugins repository -common)"
	COMPOSE_LIST="$COMPOSE_LIST $(compose services/rendering/rendering.yml -common)"

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
		COMPOSE_LIST="$COMPOSE_LIST $(compose services/rendering/rendering.yml -common)"

		echo "Use compose set: $COMPOSE_LIST"

		$COMPOSE_EXEC \
			$COMPOSE_LIST \
			down || exit
		;;
	*)
		echo Canceled.
		;;
	esac
}

purge() {
	read -p "Are you sure you want to continue? [y/N] " answer
	case ${answer:0:1} in
	y | Y)
		COMPOSE_LIST="$COMPOSE_LIST $(compose edusharing.yml -common)"
		COMPOSE_LIST="$COMPOSE_LIST $(compose repository/repository.yml -common) $(compose_plugins repository -common)"
		COMPOSE_LIST="$COMPOSE_LIST $(compose services/rendering/rendering.yml -common)"

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
start)
	rstart && info
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
	echo "  - start             startup containers"
	echo ""
	echo "  - info              show information"
	echo "  - logs              show logs"
	echo "  - ps                show containers"
	echo ""
	echo "  - stop              stop all containers"
	echo "  - remove            remove all containers"
	echo "  - purge             remove all containers and volumes"
	echo ""
	;;
esac

popd >/dev/null || exit
popd >/dev/null || exit

#!/bin/bash
set -e
set -o pipefail

GIT_BRANCH="$(echo '${project.version}' | sed 's|[\/\.]|-|g' | tr '[:upper:]' '[:lower:]')"
export COMPOSE_PROJECT_NAME="${COMPOSE_PROJECT_NAME:-edusharing-docker-$GIT_BRANCH}"

case "$(uname)" in
MINGW*)
	COMPOSE_EXEC="winpty docker compose"
	;;
*)
	COMPOSE_EXEC="docker compose"
	;;
esac

export COMPOSE_EXEC

export CLI_CMD="$0"
export CLI_OPT1="$1"

ROOT_PATH="$(
	cd "$(dirname ".")"
	pwd -P
)"
export ROOT_PATH

pushd "$(cd "$(dirname "${BASH_SOURCE[0]}")" >/dev/null && pwd)" >/dev/null || exit

COMPOSE_DIR="."

[[ -f ".env" ]] && [[ ! "${COMPOSE_DIR}/.env" -ef "./.env" ]] && {
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
	echo "    ${REPOSITORY_SERVICE_PROT:-http}://${REPOSITORY_SERVICE_HOST:-repository.127.0.0.1.nip.io}:${REPOSITORY_SERVICE_PORT:-8100}${REPOSITORY_SERVICE_PATH:-/edu-sharing}/"
	echo ""
	echo "    username: admin"
	echo "    password: ${REPOSITORY_SERVICE_ADMIN_PASS:-admin}"
	echo ""
	echo "  edu-sharing services:"
	echo ""
	echo "    rendering:"
	echo ""
	echo "      ${SERVICES_RENDERING_SERVICE_PROT:-http}://${SERVICES_RENDERING_SERVICE_HOST:-rendering.services.127.0.0.1.nip.io}:${SERVICES_RENDERING_SERVICE_PORT:-9100}${SERVICES_RENDERING_SERVICE_PATH:-/esrender}/admin/"
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

	COMPOSE_DIRECTORY="$1"
	COMPOSE_LIST=

	shift && {

    COMPOSE_FILE_GROUP="$1"

    shift && {

      while true; do
        flag="$1"
        shift || break

        COMPOSE_FILE_TYPE=""
        case "$flag" in
        -common) COMPOSE_FILE_TYPE="common" ;;
        -debug) COMPOSE_FILE_TYPE="debug" ;;
        -dev) COMPOSE_FILE_TYPE="dev" ;;
        -remote) COMPOSE_FILE_TYPE="remote" ;;
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

        while IFS='' read -r COMPOSE_FILE; do
          COMPOSE_LIST="$COMPOSE_LIST -f ${COMPOSE_FILE}"
        done < <(find "${COMPOSE_DIRECTORY}" -type f -name "${COMPOSE_FILE_GROUP}_*-${COMPOSE_FILE_TYPE}.yml" | sort -g)

      done
    }

	}

	echo $COMPOSE_LIST
}

logs() {
	COMPOSE_LIST="$COMPOSE_LIST $(compose . "*" -common)"

	echo "Use compose set: $COMPOSE_LIST"

	$COMPOSE_EXEC \
		$COMPOSE_LIST \
		logs -f $@ || exit
}

terminal() {
	COMPOSE_LIST="$COMPOSE_LIST $(compose . "*" -common)"

	echo "Use compose set: $COMPOSE_LIST"

	$COMPOSE_EXEC \
		$COMPOSE_LIST \
		exec -u root -it  $1 /bin/bash || exit
}

ps() {
	COMPOSE_LIST="$COMPOSE_LIST $(compose . "*" -common)"

	echo "Use compose set: $COMPOSE_LIST"

	$COMPOSE_EXEC \
		$COMPOSE_LIST \
		ps || exit
}

rstart() {
	COMPOSE_LIST="$COMPOSE_LIST $(compose . "*" -common -remote)"

	echo "Use compose set: $COMPOSE_LIST"

	$COMPOSE_EXEC \
		$COMPOSE_LIST \
		pull || exit

	$COMPOSE_EXEC \
		$COMPOSE_LIST \
		up -d $@ || exit
}

stop() {
	COMPOSE_LIST="$COMPOSE_LIST $(compose . "*" -common)"

	echo "Use compose set: $COMPOSE_LIST"

	$COMPOSE_EXEC \
		$COMPOSE_LIST \
		stop $@ || exit
}

remove() {
	read -p "Are you sure you want to continue? [y/N] " answer
	case ${answer:0:1} in
	y | Y)
		COMPOSE_LIST="$COMPOSE_LIST $(compose . "*" -common)"

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
		COMPOSE_LIST="$COMPOSE_LIST $(compose . "*" -common)"

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

shift

case "${CLI_OPT1}" in
start)
	rstart $@ && info
	;;
info)
	info
	;;
logs)
	logs $@
	;;
ps)
	ps
	;;
stop)
	stop $@
	;;
remove)
	remove
	;;
purge)
	purge
	;;
restart)
	stop $@ && rstart $@ && info
	;;
terminal)
  terminal $@
  ;;
*)
	echo ""
	echo "Usage: ${CLI_CMD} [option]"
	echo ""
	echo "Option:"
	echo ""
	echo "  - start [Service...]   startup containers"
	echo "  - restart [Service...] stops and starts containers"
	echo ""
	echo "  - info                 show information"
	echo "  - logs [Service...]    show logs"
	echo "  - ps                   show containers"
	echo ""
	echo "  - stop [Service...]    stop all containers"
	echo "  - remove               remove all containers"
	echo "  - purge                remove all containers and volumes"
	echo ""
	echo "  - terminal [service]   open container bash as root"
	echo ""
	;;
esac

popd >/dev/null || exit
popd >/dev/null || exit

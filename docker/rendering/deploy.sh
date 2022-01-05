#!/bin/bash
set -e
set -o pipefail

GIT_BRANCH="$(git rev-parse --abbrev-ref HEAD | sed 's/\//-/')"
export COMPOSE_NAME="${COMPOSE_PROJECT_NAME:-edu-sharing-$GIT_BRANCH}"

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
	echo "rendering-database:"
	echo ""
	echo "  Credentials:"
	echo ""
	echo "    Name:           ${RENDERING_DATABASE_USER:-rendering}"
	echo "    Password:       ${RENDERING_DATABASE_PASS:-rendering}"
	echo ""
	echo "  Database:         ${RENDERING_DATABASE_NAME:-rendering}"
	echo ""
	echo "  Port:             127.0.0.1:${RENDERING_DATABASE_PORT:-9000}"
	echo ""
	echo "#########################################################################"
	echo ""
	echo "rendering-service:"
	echo ""
	echo "  Credentials:"
	echo ""
	echo "    Name:           ${RENDERING_DATABASE_USER:-rendering}"
	echo "    Password:       ${RENDERING_DATABASE_PASS:-rendering}"
	echo ""
	echo "  Services:"
	echo ""
	echo "    HTTP:           http://${RENDERING_SERVICE_HOST:-rendering.127.0.0.1.nip.io}:${RENDERING_SERVICE_PORT_HTTP:-9100}/esrender/admin/"
	echo ""
	echo "#########################################################################"
	echo ""
	echo ""
}

note() {
	echo ""
	echo "#########################################################################"
	echo ""
	echo "  edu-sharing community services rendering:"
	echo ""
	echo "    http://${RENDERING_SERVICE_HOST:-rendering.127.0.0.1.nip.io}:${RENDERING_SERVICE_PORT_HTTP:-9100}/esrender/admin/"
	echo ""
	echo "    username: ${RENDERING_DATABASE_USER:-rendering}"
	echo "    password: ${RENDERING_DATABASE_PASS:-rendering}"
	echo ""
	echo "#########################################################################"
	echo ""
	echo ""
}

compose_yml() {

	COMPOSE_BASE_FILE="$1"
	COMPOSE_DIRECTORY="$(dirname "$COMPOSE_BASE_FILE")"
	COMPOSE_FILE_NAME="$(basename "$COMPOSE_BASE_FILE" | cut -f 1 -d '.')" # without extension

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
		-test) COMPOSE_FILE="$COMPOSE_DIRECTORY/$COMPOSE_FILE_NAME-test.yml" ;;
		-debug) COMPOSE_FILE="$COMPOSE_DIRECTORY/$COMPOSE_FILE_NAME-debug.yml" ;;
		-remote) COMPOSE_FILE="$COMPOSE_DIRECTORY/$COMPOSE_FILE_NAME-remote.yml" ;;
		*)
			{
				echo "error: unknown flag: $flag"
				echo ""
				echo "valid flags are:"
				echo "  -test"
				echo "  -debug"
				echo "  -remote"
			} >&2
			exit 1
			;;
		esac

		if [[ -f "$COMPOSE_FILE" ]]; then
			COMPOSE_LIST="$COMPOSE_LIST -f $COMPOSE_FILE"
		fi
	done

	echo $COMPOSE_LIST
}

# shellcheck disable=SC2120
compose_all_plugins() {
	COMPOSE_LIST=
	for plugin in plugin*/; do
		[ ! -d $plugin ] && continue
		COMPOSE_PLUGIN="$(compose_yml "./$plugin$(basename $plugin).yml" "$@")"
		COMPOSE_LIST="$COMPOSE_LIST $COMPOSE_PLUGIN"
	done

	echo $COMPOSE_LIST
}

logs() {

	COMPOSE_LIST="$(compose_yml rendering.yml) $(compose_all_plugins)"

	echo "Use compose set: $COMPOSE_LIST"

	$COMPOSE_EXEC \
		$COMPOSE_LIST \
		logs -f || exit
}

ps() {
	COMPOSE_LIST="$(compose_yml rendering.yml) $(compose_all_plugins)"

	echo "Use compose set: $COMPOSE_LIST"

	$COMPOSE_EXEC \
		$COMPOSE_LIST \
		ps || exit
}

rstart() {
	COMPOSE_LIST="$(compose_yml rendering.yml -remote) $(compose_all_plugins -remote)"

	echo "Use compose set: $COMPOSE_LIST"

	$COMPOSE_EXEC \
		$COMPOSE_LIST \
		pull || exit

	$COMPOSE_EXEC \
		$COMPOSE_LIST \
		up -d || exit
}

rtest() {
	COMPOSE_LIST="$(compose_yml rendering.yml -remote -test) $(compose_all_plugins -remote -test)"

	echo "Use compose set: $COMPOSE_LIST"

	$COMPOSE_EXEC \
		$COMPOSE_LIST \
		pull || exit

	$COMPOSE_EXEC \
		$COMPOSE_LIST \
		up -d || exit
}

rdebug() {
	[[ -z "${CLI_OPT2}" ]] && {
		echo ""
		echo "Usage: ${CLI_CMD} ${CLI_OPT1} <edu-sharing community services rendering>"
		exit
	}

	case $CLI_OPT2 in
	/*) pushd "${CLI_OPT2}" >/dev/null || exit ;;
	*) pushd "${ROOT_PATH}/${CLI_OPT2}" >/dev/null || exit ;;
	esac

	COMMUNITY_PATH=$(pwd)
	export COMMUNITY_PATH
	popd >/dev/null || exit

	COMPOSE_LIST="$(compose_yml rendering.yml -remote -test -debug) $(compose_all_plugins -remote -test)"

	echo "Use compose set: $COMPOSE_LIST"

	$COMPOSE_EXEC \
		$COMPOSE_LIST \
		pull || exit

	$COMPOSE_EXEC \
		$COMPOSE_LIST \
		up -d || exit
}

lstart() {
	COMPOSE_LIST="$(compose_yml rendering.yml) $(compose_all_plugins)"

	echo "Use compose set: $COMPOSE_LIST"

	$COMPOSE_EXEC \
		$COMPOSE_LIST \
		up -d || exit
}

ltest() {
	COMPOSE_LIST="$(compose_yml rendering.yml -test) $(compose_all_plugins -test)"

	echo "Use compose set: $COMPOSE_LIST"

	$COMPOSE_EXEC \
		$COMPOSE_LIST \
		up -d || exit
}

ldebug() {
	[[ -z "${CLI_OPT2}" ]] && {
		echo ""
		echo "Usage: ${CLI_CMD} ${CLI_OPT1} <edu-sharing community services rendering>"
		exit
	}

	case $CLI_OPT2 in
	/*) pushd "${CLI_OPT2}" >/dev/null || exit ;;
	*) pushd "${ROOT_PATH}/${CLI_OPT2}" >/dev/null || exit ;;
	esac

	COMMUNITY_PATH=$(pwd)
	export COMMUNITY_PATH
	popd >/dev/null || exit

	COMPOSE_LIST="$(compose_yml rendering.yml -test -debug) $(compose_all_plugins -test)"

	echo "Use compose set: $COMPOSE_LIST"

	$COMPOSE_EXEC \
		$COMPOSE_LIST \
		up -d || exit
}

stop() {
	COMPOSE_LIST="$(compose_yml rendering.yml) $(compose_all_plugins)"

	echo "Use compose set: $COMPOSE_LIST"

	$COMPOSE_EXEC \
		$COMPOSE_LIST \
		stop || exit
}

remove() {
	read -p "Are you sure you want to continue? [y/N] " answer
	case ${answer:0:1} in
	y | Y)
		COMPOSE_LIST="$(compose_yml rendering.yml) $(compose_all_plugins)"

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
	rstart && note
	;;
rtest)
	rtest && logs
	;;
rdebug)
	rdebug && logs
	;;
lstart)
	lstart && note
	;;
ltest)
	ltest && logs
	;;
ldebug)
	ldebug && logs
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
	echo "  - lstart            startup containers from local images"
	echo "  - ltest             startup containers from local images with dev ports"
	echo "  - ldebug <path>     startup containers from local images with dev ports and artifacts"
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

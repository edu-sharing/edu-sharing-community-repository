#!/bin/bash
set -e
set -o pipefail

GIT_BRANCH="$(git rev-parse --abbrev-ref HEAD | sed 's/\//-/')"
export COMPOSE_NAME="${COMPOSE_PROJECT_NAME:-docker-$GIT_BRANCH}"

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

ROOT_PATH="$(
	cd "$(dirname ".")"
	pwd -P
)"
export ROOT_PATH

pushd "$(cd "$(dirname "${BASH_SOURCE[0]}")" >/dev/null && pwd)" >/dev/null || exit

COMPOSE_DIR="compose/target/compose"

[[ ! -d "${COMPOSE_DIR}" ]] && {
	echo "Initializing ..."
	pushd "rendering/compose" >/dev/null || exit
	$MAVEN_CMD $MAVEN_CMD_OPTS -Dmaven.test.skip=true install || exit
	popd >/dev/null || exit
	pushd "repository/compose" >/dev/null || exit
	$MAVEN_CMD $MAVEN_CMD_OPTS -Dmaven.test.skip=true install || exit
	popd >/dev/null || exit
	pushd "compose" >/dev/null || exit
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
	echo "    http://${REPOSITORY_SERVICE_HOST:-repository.127.0.0.1.nip.io}:${REPOSITORY_SERVICE_PORT_HTTP:-8100}/edu-sharing/"
	echo ""
	echo "    username: admin"
	echo "    password: ${REPOSITORY_SERVICE_ADMIN_PASS:-admin}"
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
	PLUGIN_DIR="$1"
	shift

	COMPOSE_LIST=
	for plugin in $PLUGIN_DIR/plugin*/; do
		[ ! -d $plugin ] && continue
		COMPOSE_PLUGIN="$(compose_only "./$plugin$(basename $plugin).yml" "$@")"
		COMPOSE_LIST="$COMPOSE_LIST $COMPOSE_PLUGIN"
	done

	echo $COMPOSE_LIST
}

compose_all_plugins() {
	PLUGIN_DIR="$1"
	shift

	COMPOSE_LIST=
	for plugin in $PLUGIN_DIR/plugin*/; do
		[ ! -d $plugin ] && continue
		COMPOSE_PLUGIN="$(compose_all "./$plugin$(basename $plugin).yml" "$@")"
		COMPOSE_LIST="$COMPOSE_LIST $COMPOSE_PLUGIN"
	done

	echo $COMPOSE_LIST
}

logs() {
	COMPOSE_LIST="$COMPOSE_LIST $(compose_all repository/repository.yml) $(compose_all_plugins repository)"
	COMPOSE_LIST="$COMPOSE_LIST $(compose_all rendering/rendering.yml) $(compose_all_plugins rendering)"

	echo "Use compose set: $COMPOSE_LIST"

	$COMPOSE_EXEC \
		$COMPOSE_LIST \
		logs -f || exit
}

ps() {
	COMPOSE_LIST="$COMPOSE_LIST $(compose_all repository/repository.yml) $(compose_all_plugins repository)"
	COMPOSE_LIST="$COMPOSE_LIST $(compose_all rendering/rendering.yml) $(compose_all_plugins rendering)"

	echo "Use compose set: $COMPOSE_LIST"

	$COMPOSE_EXEC \
		-f $COMPOSE_LIST \
		ps || exit
}

rstart() {
	COMPOSE_LIST="$COMPOSE_LIST $(compose_all repository/repository.yml -remote) $(compose_all_plugins repository -remote)"
	COMPOSE_LIST="$COMPOSE_LIST $(compose_all rendering/rendering.yml -remote) $(compose_all_plugins rendering -remote)"

	echo "Use compose set: $COMPOSE_LIST"

	$COMPOSE_EXEC \
		$COMPOSE_LIST \
		pull || exit

	$COMPOSE_EXEC \
		$COMPOSE_LIST \
		up -d || exit
}

rtest() {
	COMPOSE_LIST="$COMPOSE_LIST $(compose_all repository/repository.yml -remote -test) $(compose_all_plugins repository -remote -test)"
	COMPOSE_LIST="$COMPOSE_LIST $(compose_all rendering/rendering.yml -remote -test) $(compose_all_plugins rendering -remote -test)"

	echo "Use compose set: $COMPOSE_LIST"

	$COMPOSE_EXEC \
		$COMPOSE_LIST \
		pull || exit

	$COMPOSE_EXEC \
		$COMPOSE_LIST \
		up -d || exit
}

rdebug() {
	[[ -z $CLI_OPT2 ]] && {
		CLI_OPT2="../.."
	}

	COMPOSE_LIST="$COMPOSE_LIST $(compose_all repository/repository.yml -remote -test -debug) $(compose_all_plugins repository -remote -test -debug)"
	COMPOSE_LIST="$COMPOSE_LIST $(compose_all rendering/rendering.yml -remote -test -debug) $(compose_all_plugins rendering -remote -test -debug)"

	case $CLI_OPT2 in
	/*) pushd "${CLI_OPT2}" >/dev/null || exit ;;
	*) pushd "${ROOT_PATH}/${CLI_OPT2}" >/dev/null || exit ;;
	esac

	COMMUNITY_PATH=$(pwd)

	export COMMUNITY_PATH
	popd >/dev/null || exit

	echo "Use compose set: $COMPOSE_LIST"

	$COMPOSE_EXEC \
		$COMPOSE_LIST \
		pull || exit

	$COMPOSE_EXEC \
		$COMPOSE_LIST \
		up -d || exit
}

lstart() {
	COMPOSE_LIST="$COMPOSE_LIST $(compose_all repository/repository.yml) $(compose_all_plugins repository)"
	COMPOSE_LIST="$COMPOSE_LIST $(compose_all rendering/rendering.yml) $(compose_all_plugins rendering)"

	echo "Use compose set: $COMPOSE_LIST"

	$COMPOSE_EXEC \
		$COMPOSE_LIST \
		up -d || exit
}

ltest() {
	COMPOSE_LIST="$COMPOSE_LIST $(compose_all repository/repository.yml -test) $(compose_all_plugins repository -test)"
	COMPOSE_LIST="$COMPOSE_LIST $(compose_all rendering/rendering.yml -test) $(compose_all_plugins rendering -test)"

	echo "Use compose set: $COMPOSE_LIST"

	$COMPOSE_EXEC \
		$COMPOSE_LIST \
		up -d || exit
}

ldebug() {
	[[ -z "${CLI_OPT2}" ]] && {
		CLI_OPT2="../.."
	}

	case $CLI_OPT2 in
	/*) pushd "${CLI_OPT2}" >/dev/null || exit ;;
	*) pushd "${ROOT_PATH}/${CLI_OPT2}" >/dev/null || exit ;;
	esac

	COMMUNITY_PATH=$(pwd)
	export COMMUNITY_PATH
	popd >/dev/null || exit

	COMPOSE_LIST="$COMPOSE_LIST $(compose_all repository/repository.yml -test -debug) $(compose_all_plugins repository -test -debug)"
	COMPOSE_LIST="$COMPOSE_LIST $(compose_all rendering/rendering.yml -test -debug) $(compose_all_plugins rendering -test -debug)"

	echo "Use compose set: $COMPOSE_LIST"

	$COMPOSE_EXEC \
		$COMPOSE_LIST \
		up -d || exit
}

stop() {
	COMPOSE_LIST="$COMPOSE_LIST $(compose_all repository/repository.yml) $(compose_all_plugins repository)"
	COMPOSE_LIST="$COMPOSE_LIST $(compose_all rendering/rendering.yml) $(compose_all_plugins rendering)"

	echo "Use compose set: $COMPOSE_LIST"

	$COMPOSE_EXEC \
		$COMPOSE_LIST \
		stop || exit
}

remove() {
	read -p "Are you sure you want to continue? [y/N] " answer
	case ${answer:0:1} in
	y | Y)
		COMPOSE_LIST="$COMPOSE_LIST $(compose_all repository/repository.yml) $(compose_all_plugins repository)"
		COMPOSE_LIST="$COMPOSE_LIST $(compose_all rendering/rendering.yml) $(compose_all_plugins rendering)"

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

ci() {

	COMPOSE_LIST1="$(compose_only_plugins repository -remote)"
	COMPOSE_LIST2="$(compose_only_plugins rendering -remote)"

  [[ -n $COMPOSE_LIST1 || -n $COMPOSE_LIST2 ]] && {
		echo "Use compose set: $COMPOSE_LIST1 $COMPOSE_LIST2"

		$COMPOSE_EXEC \
			$COMPOSE_LIST1 $COMPOSE_LIST2 \
			pull || exit
	}

	COMPOSE_LIST="$COMPOSE_LIST $(compose_all repository/repository.yml) $(compose_all_plugins repository -remote)"
	COMPOSE_LIST="$COMPOSE_LIST $(compose_all rendering/rendering.yml) $(compose_all_plugins rendering -remote)"

	echo "Use compose set: $COMPOSE_LIST"

	$COMPOSE_EXEC \
		$COMPOSE_LIST \
		up -d || exit

}

case "${CLI_OPT1}" in
rstart)
	rstart && info
	;;
rtest)
	rtest && logs
	;;
rdebug)
	rdebug && logs
	;;
lstart)
	lstart && info
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
	echo "  - rtest             startup containers from remote images with dev ports"
	echo "  - rdebug [<path>]   startup containers from remote images with dev ports and artifacts [../..]"
	echo ""
	echo "  - lstart            startup containers from local images"
	echo "  - ltest             startup containers from local images with dev ports"
	echo "  - ldebug [<path>]   startup containers from local images with dev ports and artifacts [../..]"
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
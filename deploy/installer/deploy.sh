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

COMPOSE_DIR="compose/debian/bullseye/target/compose"

[[ -f ".env" ]] && {
	cp -f ".env" "${COMPOSE_DIR}"
}

export COMPOSE_CI=""
if [ -n "${CI}" ]; then
	export REPOSITORY_SERVICE_HOST="docker"
	export RENDERING_SERVICE_HOST="docker"
	export COMPOSE_CI=" -f aio-ci.yml ${COMPOSE_CI}"
fi

if [ -n "${MAVEN_HOME}" ]; then
	export COMPOSE_CI=" -f aio-volume.yml ${COMPOSE_CI}"
fi

[[ ! -d "${COMPOSE_DIR}" ]] && {
	echo "Initializing ..."
	pushd "compose" >/dev/null || exit
	$MVN_EXEC $MVN_EXEC_OPTS -Dmaven.test.skip=true package || exit
	popd >/dev/null || exit
}


pushd "${COMPOSE_DIR}" >/dev/null || exit

note() {
	[[ -f ".env" ]] && source .env
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
	echo "#########################################################################"	echo ""
	echo ""
}

logs() {
	$COMPOSE_EXEC \
		-f "aio.yml" \
		logs -f || exit
}

ps() {
	$COMPOSE_EXEC \
		-f "aio.yml" \
		ps || exit
}

rstart() {
	$COMPOSE_EXEC \
		-f "aio.yml" \
		-f "aio-image-remote.yml" \
		pull || exit

	$COMPOSE_EXEC \
		-f "aio.yml" \
		-f "aio-image-remote.yml" \
		$COMPOSE_CI \
		up -d || exit
}

lstart() {
	$COMPOSE_EXEC \
		-f "aio.yml" \
		$COMPOSE_CI \
		up -d || exit
}

stop() {
	$COMPOSE_EXEC \
		-f "aio.yml" \
		stop || exit
}

remove() {
	$COMPOSE_EXEC \
		-f "aio.yml" \
		down || exit
}

case "${CLI_OPT1}" in
rstart)
	rstart && note
	;;
lstart)
	lstart && note
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
	echo "  - lstart            startup containers from local images"
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

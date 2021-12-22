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
	echo "rendering-database:"
	echo ""
	echo "  Credentials:"
	echo ""
	echo "    Name:           ${RENDERING_DATABASE_USER:-rendering}"
	echo "    Password:       ${RENDERING_DATABASE_PASS:-rendering}"
	echo ""
	echo "  Database:         ${RENDERING_DATABASE_NAME:-rendering}"
	echo ""
	echo "  Services:"
	echo ""
	echo "    SQL:            127.0.0.1:${RENDERING_DATABASE_PORT_SQL:-9000}"
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

logs() {
	$COMPOSE_EXEC \
		-f "rendering.yml" \
		logs -f || exit
}

ps() {
	$COMPOSE_EXEC \
		-f "rendering.yml" \
		ps || exit
}

rstart() {
	$COMPOSE_EXEC \
		-f "rendering.yml" \
		-f "rendering-image-remote.yml" \
		pull || exit

	$COMPOSE_EXEC \
		-f "rendering.yml" \
		-f "rendering-image-remote.yml" \
		-f "rendering-network-prd.yml" \
		up -d || exit
}

rtest() {
	$COMPOSE_EXEC \
		-f "rendering.yml" \
		-f "rendering-image-remote.yml" \
		pull || exit

	$COMPOSE_EXEC \
		-f "rendering.yml" \
		-f "rendering-image-remote.yml" \
		-f "rendering-network-dev.yml" \
		up -d || exit
}

rdebug() {
	[[ -z "${CLI_OPT2}" ]] && {
		echo ""
		echo "Usage: ${CLI_CMD} ${CLI_OPT1} <edu-sharing community services rendering>"
		exit
	}

	pushd "${ROOT_PATH}/${CLI_OPT2}" >/dev/null || exit
	COMMUNITY_PATH=$(pwd)
	export COMMUNITY_PATH
	popd >/dev/null || exit

	$COMPOSE_EXEC \
		-f "rendering.yml" \
		-f "rendering-image-remote.yml" \
		pull || exit

	$COMPOSE_EXEC \
		-f "rendering.yml" \
		-f "rendering-image-remote.yml" \
		-f "rendering-network-dev.yml" \
		-f "rendering-debug.yml" \
		up -d || exit
}

lstart() {
	$COMPOSE_EXEC \
		-f "rendering.yml" \
		-f "rendering-network-prd.yml" \
		up -d || exit
}

ltest() {
	$COMPOSE_EXEC \
		-f "rendering.yml" \
		-f "rendering-network-dev.yml" \
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

	$COMPOSE_EXEC \
		-f "rendering.yml" \
		-f "rendering-network-dev.yml" \
		-f "rendering-debug.yml" \
		up -d || exit
}

stop() {
	$COMPOSE_EXEC \
		-f "rendering.yml" \
		stop || exit
}

remove() {
	$COMPOSE_EXEC \
		-f "rendering.yml" \
		down || exit
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

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

info() {
	[[ -f ".env" ]] && source .env
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

init() {
	docker volume create "${COMPOSE_NAME}_rendering-database-volume" || exit
	docker volume create "${COMPOSE_NAME}_rendering-service-volume" || exit
}

purge() {
	docker volume rm -f "${COMPOSE_NAME}_rendering-database-volume" || exit
	docker volume rm -f "${COMPOSE_NAME}_rendering-service-volume" || exit
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

up() {
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

it() {
	$COMPOSE_EXEC \
		-f "rendering.yml" \
		-f "rendering-network-dev.yml" \
		up -d || exit
}

debug() {
	[[ -z "${CLI_OPT2}" ]] && {
		echo ""
		echo "Usage: ${CLI_CMD} ${CLI_OPT1} <path>"
		exit
	}

	pushd "${ROOT_PATH}/${CLI_OPT2}" >/dev/null || exit
	COMMUNITY_PATH=$(pwd)
	export COMMUNITY_PATH
	popd >/dev/null || exit

	$COMPOSE_EXEC \
		-f "rendering.yml" \
		-f "rendering-network-dev.yml" \
		-f "rendering-debug.yml" \
		up -d || exit
}

down() {
	$COMPOSE_EXEC \
		-f "rendering.yml" \
		down || exit
}

build() {
	echo "Building ..."

	pushd "${BUILD_PATH}/../build/postgresql" >/dev/null || exit
	$MVN_EXEC $MVN_EXEC_OPTS -Dmaven.test.skip=true clean install || exit
	popd >/dev/null || exit
	pushd "${BUILD_PATH}" >/dev/null || exit
	$MVN_EXEC $MVN_EXEC_OPTS -Dmaven.test.skip=true clean install || exit
	popd >/dev/null || exit
}

case "${CLI_OPT1}" in
build)
	build
	;;
info)
	info
	;;
purge)
	purge
	;;
start)
	init && up && logs
	;;
test)
	init && it && logs
	;;
debug)
	init && debug && logs
	;;
logs)
	logs
	;;
ps)
	ps
	;;
stop)
	down
	;;
*)
	echo ""
	echo "Usage: ${CLI_CMD} [option]"
	echo ""
	echo "Option:"
	echo ""
	echo "  - start:              startup with remote images"
	echo "  - stop:               shutdown"
	echo ""
	echo "  - build:              build local images"
	echo "  - test:               startup with local images"
	echo "  - debug <path>:       startup with local images and mounted artifacts"
	echo ""
	echo "  - info:               show all information"
	echo "  - logs:               show all logs"
	echo "  - ps:                 show all running containers"
	echo ""
	echo "  - purge:              purge all data volumes"
	echo ""
	;;
esac

popd >/dev/null || exit
popd >/dev/null || exit

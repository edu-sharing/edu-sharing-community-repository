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

[[ ! -d "${COMPOSE_DIR}" || -z "${CLI_OPT1}" ]] && {
	echo "Building ..."
	pushd "compose" >/dev/null || exit
	$MVN_EXEC $MVN_EXEC_OPTS -Dmaven.test.skip=true package || exit
	popd >/dev/null || exit
}

[[ -f ".env" ]] && {
	cp -f ".env" "${COMPOSE_DIR}"
}

pushd "${COMPOSE_DIR}" >/dev/null || exit

info() {
	[[ -f ".env" ]] && source .env
	echo ""
	echo "#########################################################################"
	echo ""
	echo "rendering-cache:"
	echo ""
	echo "  Services:"
	echo ""
	echo "    REDIS:          127.0.0.1:${RENDERING_CACHE_PORT_REDIS:-9001}"
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
	echo "    HTTP:           http://${RENDERING_SERVICE_HOST:-rendering.127.0.0.1.xip.io}:${RENDERING_SERVICE_PORT_HTTP:-9100}/esrender/admin/"
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

down() {
	$COMPOSE_EXEC \
		-f "rendering.yml" \
		down || exit
}

it() {
	$COMPOSE_EXEC \
		-f "rendering.yml" \
		-f "rendering-network-dev.yml" \
		up -d || exit
}

build() {
	[[ -z "${CLI_OPT2}" ]] && {
		echo ""
		echo "Usage: ${CLI_CMD} ${CLI_OPT1} <rendering-project>"
		exit
	}

	pushd "${ROOT_PATH}/${CLI_OPT2}" >/dev/null || exit
	COMMUNITY_PATH=$(pwd)
	export COMMUNITY_PATH
	popd >/dev/null || exit

	echo "Checking artifactId ..."

	EXPECTED_ARTIFACTID="edu_sharing-community-rendering"

	pushd "${COMMUNITY_PATH}" >/dev/null || exit
	PROJECT_ARTIFACTID=$($MVN_EXEC -q -ff -nsu -N help:evaluate -Dexpression=project.artifactId -DforceStdout)
	echo "- rendering          [ ${PROJECT_ARTIFACTID} ]"
	popd >/dev/null || exit

	[[ "${EXPECTED_ARTIFACTID}" != "${PROJECT_ARTIFACTID}" ]] && {
		echo "Error: expected artifactId [ ${EXPECTED_ARTIFACTID} ] is different."
		exit
	}

	echo "Checking version ..."

	pushd "${BUILD_PATH}" >/dev/null || exit
	EXPECTED_VERSION=$($MVN_EXEC -q -ff -nsu -N help:evaluate -Dexpression=community.rendering.version -DforceStdout)
	popd >/dev/null || exit

	pushd "${COMMUNITY_PATH}" >/dev/null || exit
	PROJECT_VERSION=$($MVN_EXEC -q -ff -nsu -N help:evaluate -Dexpression=project.version -DforceStdout)
	echo "- rendering          [ ${PROJECT_VERSION} ]"
	popd >/dev/null || exit

	[[ "${EXPECTED_VERSION}" != "${PROJECT_VERSION}" ]] && {
		echo "Error: expected version [ ${EXPECTED_VERSION} ] is different."
		exit
	}

	echo "Building ..."

	echo "- rendering"
	pushd "${COMMUNITY_PATH}" >/dev/null || exit
	$MVN_EXEC $MVN_EXEC_OPTS -Dmaven.test.skip=true clean install || exit
	popd >/dev/null || exit

	echo "- docker"
	pushd "${BUILD_PATH}/../build" >/dev/null || exit
	$MVN_EXEC $MVN_EXEC_OPTS -Dmaven.test.skip=true clean install || exit
	popd >/dev/null || exit
	pushd "${BUILD_PATH}" >/dev/null || exit
	$MVN_EXEC $MVN_EXEC_OPTS -Dmaven.test.skip=true clean install || exit
	popd >/dev/null || exit

	echo "export COMMUNITY_PATH=${COMMUNITY_PATH}" > .ctx
}

debug() {
	[[ -f ".ctx" ]] && source .ctx
	echo "<rendering-path>:   ${COMMUNITY_PATH}"
	[[ -z "${COMMUNITY_PATH}" ]] && {
		echo ""
		echo "Error: missing context, build first."
		exit
	}

	$COMPOSE_EXEC \
		-f "rendering.yml" \
		-f "rendering-network-dev.yml" \
		-f "rendering-debug.yml" \
		up -d || exit
}

package() {
	[[ -f ".ctx" ]] && source .ctx
	echo "<rendering-path>:   ${COMMUNITY_PATH}"
	[[ -z "${COMMUNITY_PATH}" ]] && {
		echo ""
		echo "Error: missing context, build first."
		exit
	}

	echo "Building ..."

	echo "- rendering"
	pushd "${COMMUNITY_PATH}" >/dev/null || exit
	$MVN_EXEC $MVN_EXEC_OPTS -nsu -Dmaven.test.skip=true package || exit
	popd >/dev/null || exit
}

case "${CLI_OPT1}" in
build)
	build
	;;
info)
	info
	;;
init)
	init
	;;
logs)
	logs
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
stop)
	down
	;;
debug)
	init && debug && logs
	;;
rebuild)
	package
	;;
*)
	echo ""
	echo "Usage: ${CLI_CMD} [option]"
	echo ""
	echo "Option:"
	echo "  - build <rendering-project>"
	echo "  - debug"
	echo "  - info"
	echo "  - init"
	echo "  - logs"
	echo "  - purge"
	echo "  - rebuild"
	echo "  - start"
	echo "  - stop"
	echo "  - test"
	echo ""
	;;
esac

popd >/dev/null || exit
popd >/dev/null || exit

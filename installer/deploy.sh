#!/bin/bash
set -e
set -o pipefail

GIT_BRANCH="$(git rev-parse --abbrev-ref HEAD | sed 's/\//-/')"
export COMPOSE_NAME="${COMPOSE_PROJECT_NAME:-installer-$GIT_BRANCH}"

case "$(uname)" in
MINGW*)
	COMPOSE_EXEC="winpty docker-compose"
	;;
*)
	COMPOSE_EXEC="docker-compose"
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

init() {
	{
		echo "REPOSITORY_SERVICE_ADMIN_PASS=${REPOSITORY_SERVICE_ADMIN_PASS:-admin}"
		echo "REPOSITORY_SERVICE_HOST_EXTERNAL=${REPOSITORY_SERVICE_HOST:-repository.127.0.0.1.nip.io}"
		echo "REPOSITORY_SERVICE_HOST_INTERNAL=repository"
		echo "REPOSITORY_SERVICE_PORT_EXTERNAL=${REPOSITORY_SERVICE_PORT:-8100}"
		echo "REPOSITORY_SERVICE_PORT_INTERNAL=80"
		echo "REPOSITORY_SERVICE_HOME_APPID=${COMPOSE_PROJECT_NAME:-compose}"
	} >>.env.repository
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
	} >>.env.rendering
}

rstart() {
	$COMPOSE_EXEC \
		-f "aio.yml" \
		-f "aio-remote.yml" \
		pull || exit

	$COMPOSE_EXEC \
		-f "aio.yml" \
		-f "aio-remote.yml" \
		$COMPOSE_CI \
		up -d || exit
}

lstart() {
	[[ -z "${MAVEN_HOME}" ]] && {
		export MAVEN_HOME="$HOME/.m2"
	}

	$COMPOSE_EXEC \
		-f "aio.yml" \
		-f "aio-local.yml" \
		$COMPOSE_CI \
		up -d || exit
}

stop() {
	$COMPOSE_EXEC \
		-f "aio.yml" \
		stop || exit
}

remove() {
	read -p "Are you sure you want to continue? [y/N] " answer
	case ${answer:0:1} in
	y | Y)
		$COMPOSE_EXEC \
			-f "aio.yml" \
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
	init && lstart
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

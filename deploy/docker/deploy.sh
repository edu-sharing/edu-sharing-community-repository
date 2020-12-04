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

COMPOSE_DIR="compose/target/compose"

[[ ! -d "${COMPOSE_DIR}" || -z "${CLI_OPT1}" ]] && {
	echo "Building ..."
	pushd "rendering/compose" >/dev/null || exit
	$MVN_EXEC $MVN_EXEC_OPTS -Dmaven.test.skip=true install || exit
	popd >/dev/null || exit
	pushd "repository/compose" >/dev/null || exit
	$MVN_EXEC $MVN_EXEC_OPTS -Dmaven.test.skip=true install || exit
	popd >/dev/null || exit
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
	echo "#########################################################################"
	echo ""
	echo "  edu-sharing is starting. This might take a few minutes."
	echo ""
	echo "  When ready:"
	echo ""
	echo "  1. Open:   http://${REPOSITORY_SERVICE_HOST:-repository.127.0.0.1.xip.io}:${REPOSITORY_SERVICE_PORT_HTTP:-8100}/edu-sharing/"
	echo ""
	echo "  2. Login:  username:  admin"
	echo "             password:  ${REPOSITORY_SERVICE_ADMIN_PASS:-admin}"
	echo ""
	echo "#########################################################################"
	echo "#########################################################################"
	echo ""
}

init() {
	docker volume create "${COMPOSE_NAME}_rendering-database-volume" || exit
	docker volume create "${COMPOSE_NAME}_rendering-service-volume" || exit
	docker volume create "${COMPOSE_NAME}_repository-database-volume" || exit
	docker volume create "${COMPOSE_NAME}_repository-search-elastic-volume" || exit
	docker volume create "${COMPOSE_NAME}_repository-search-solr4-volume" || exit
	docker volume create "${COMPOSE_NAME}_repository-service-volume-data" || exit
	docker volume create "${COMPOSE_NAME}_repository-service-volume-shared" || exit
}

logs() {
	docker-compose \
		-f "rendering.yml" \
		-f "repository.yml" \
		logs -f || exit
}

purge() {
	docker volume rm -f "${COMPOSE_NAME}_rendering-database-volume" || exit
	docker volume rm -f "${COMPOSE_NAME}_rendering-service-volume" || exit
	docker volume rm -f "${COMPOSE_NAME}_repository-database-volume" || exit
	docker volume rm -f "${COMPOSE_NAME}_repository-search-elastic-volume" || exit
	docker volume rm -f "${COMPOSE_NAME}_repository-search-solr4-volume" || exit
	docker volume rm -f "${COMPOSE_NAME}_repository-service-volume-data" || exit
	docker volume rm -f "${COMPOSE_NAME}_repository-service-volume-shared" || exit
}

up() {
	docker-compose \
		-f "rendering.yml" \
		-f "rendering-image-remote.yml" \
		-f "repository.yml" \
		-f "repository-image-remote.yml" \
		pull || exit

	docker-compose \
		-f "rendering.yml" \
		-f "rendering-image-remote.yml" \
		-f "rendering-network-prd.yml" \
		-f "repository.yml" \
		-f "repository-image-remote.yml" \
		-f "repository-network-prd.yml" \
		up -d || exit
}

down() {
	docker-compose \
		-f "rendering.yml" \
		-f "rendering-image-remote.yml" \
		-f "rendering-network-prd.yml" \
		-f "repository.yml" \
		-f "repository-image-remote.yml" \
		-f "repository-network-prd.yml" \
		down || exit
}

backup() {
	[[ -z "${CLI_OPT2}" ]] && {
		echo ""
		echo "Usage: ${CLI_CMD} ${CLI_OPT1} <backup-directory>"
		exit
	}

	pushd "${ROOT_PATH}/${CLI_OPT2}" >/dev/null || exit
	BACKUP_PATH=$(pwd)
	popd >/dev/null || exit

	docker run --rm \
		-v "${BACKUP_PATH}":/destination \
		--mount "source=${COMPOSE_NAME}_rendering-database-volume,target=/data/rendering-database-volume" \
		--mount "source=${COMPOSE_NAME}_rendering-service-volume,target=/data/rendering-service-volume" \
		--mount "source=${COMPOSE_NAME}_repository-database-volume,target=/data/repository-database-volume" \
		--mount "source=${COMPOSE_NAME}_repository-search-elastic-volume,target=/data/repository-search-elastic-volume" \
		--mount "source=${COMPOSE_NAME}_repository-search-solr4-volume,target=/data/repository-search-solr4-volume" \
		--mount "source=${COMPOSE_NAME}_repository-service-volume-data,target=/data/repository-service-volume-data" \
		--mount "source=${COMPOSE_NAME}_repository-service-volume-shared,target=/data/repository-service-volume-shared" \
		debian tar cvf "/destination/${COMPOSE_NAME}.tar" /data || exit
}

restore() {
	[[ -z "${CLI_OPT2}" ]] && {
		echo ""
		echo "Usage: ${CLI_CMD} ${CLI_OPT1} <backup-directory>"
		exit
	}

	pushd "${ROOT_PATH}/${CLI_OPT2}" >/dev/null || exit
	BACKUP_PATH=$(pwd)
	popd >/dev/null || exit

	docker run --rm \
		-v "${BACKUP_PATH}":/source \
		--mount "source=${COMPOSE_NAME}_rendering-database-volume,target=/data/rendering-database-volume" \
		--mount "source=${COMPOSE_NAME}_rendering-service-volume,target=/data/rendering-service-volume" \
		--mount "source=${COMPOSE_NAME}_repository-database-volume,target=/data/repository-database-volume" \
		--mount "source=${COMPOSE_NAME}_repository-search-elastic-volume,target=/data/repository-search-elastic-volume" \
		--mount "source=${COMPOSE_NAME}_repository-search-solr4-volume,target=/data/repository-search-solr4-volume" \
		--mount "source=${COMPOSE_NAME}_repository-service-volume-data,target=/data/repository-service-volume-data" \
		--mount "source=${COMPOSE_NAME}_repository-service-volume-shared,target=/data/repository-service-volume-shared" \
		debian tar xvf "/source/${COMPOSE_NAME}.tar" -C / || exit
}

case "${CLI_OPT1}" in
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
	init && up && info
	;;
stop)
	down
	;;
backup)
	backup
	;;
restore)
	init && restore
	;;
*)
	echo ""
	echo "Usage: ${CLI_CMD} [option]"
	echo ""
	echo "Option:"
	echo "  - backup           <backup-directory>"
	echo "  - info"
	echo "  - init"
	echo "  - logs"
	echo "  - purge"
	echo "  - restore          <backup-directory>"
	echo "  - start"
	echo "  - stop"
	echo ""
	;;
esac

popd >/dev/null || exit
popd >/dev/null || exit

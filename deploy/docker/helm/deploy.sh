#!/bin/bash
set -e
set -o pipefail

COMMAND=${1:?"Please specify a helm command"}
RELEASE=${2:-""}
CHART=${3:-""}
VERSION=${4:-""}
USERNAME=${5:-""}
PASSWORD=${6:-""}

CONTEXT="$(kubectl config current-context)"
NAMESPACE="$(kubectl config view --minify --output 'jsonpath={..namespace}')"

root=${EDU_ROOT:-$HOME/.edusharing}

mkdir -p "${root}/helm/config"
touch "${root}/helm/config/${RELEASE}.yaml"

mkdir -p "${root}/helm/context/${CONTEXT}/${NAMESPACE}"
touch "${root}/helm/context/${CONTEXT}/${RELEASE}.yaml"
touch "${root}/helm/context/${CONTEXT}/${NAMESPACE}/${RELEASE}.yaml"

OPTIONS=()

OPTIONS+=("--values")
OPTIONS+=("${root}/helm/config/${RELEASE}.yaml")
OPTIONS+=("--values")
OPTIONS+=("${root}/helm/context/${CONTEXT}/${RELEASE}.yaml")
OPTIONS+=("--values")
OPTIONS+=("${root}/helm/context/${CONTEXT}/${NAMESPACE}/${RELEASE}.yaml")

[[ -n $USERNAME && -n $PASSWORD ]] && {
	OPTIONS+=("--set")
	OPTIONS+=("global.image.pullSecrets[0].name=registry")
	OPTIONS+=("--set")
	OPTIONS+=("image.pullSecrets[0].name=registry")
	OPTIONS+=("--set")
	OPTIONS+=("image.pullSecrets[0].server=docker.edu-sharing.com")
	OPTIONS+=("--set")
	OPTIONS+=("image.pullSecrets[0].username=${USERNAME}")
	OPTIONS+=("--set")
	OPTIONS+=("image.pullSecrets[0].password=${PASSWORD}")
}

[[ -n $DEBUG ]] && {
	OPTIONS+=(--dry-run)
	OPTIONS+=(--debug)
}

SOURCE_PATH="$(cd "$(dirname "${BASH_SOURCE[0]}")" >/dev/null && pwd)"
pushd "${SOURCE_PATH}" >/dev/null || exit

file="bundle/target/helm/repo/${CHART}-${VERSION}.tgz"

if [[ -f $file ]]; then

	set -x
	helm "${COMMAND}" "${RELEASE}" "${file}" "${OPTIONS[@]}"
	set +x

else

	CREDENTIALS=()
	[[ -n $USERNAME ]] && {
		CREDENTIALS+=("--username")
		CREDENTIALS+=("${USERNAME}")
	}
	[[ -n $PASSWORD ]] && {
		CREDENTIALS+=("--password")
		CREDENTIALS+=("${PASSWORD}")
	}

	set -x
	helm "${COMMAND}" "${RELEASE}" \
		"${CHART}" --version "${VERSION}" \
		--repo "https://artifacts.edu-sharing.com/repository/helm/" \
		"${CREDENTIALS[@]}" \
		"${OPTIONS[@]}"
	set +x

fi

popd >/dev/null || exit
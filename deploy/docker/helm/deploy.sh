#!/bin/bash
set -e
set -o pipefail

RELEASE=${1}
CHART=${2}
VERSION=${3:-""}
USERNAME=${4:-""}
PASSWORD=${5:-""}

CONTEXT="$(kubectl config current-context)"
NAMESPACE="$(kubectl config view --minify --output 'jsonpath={..namespace}')"

SOURCE_PATH="$(cd "$(dirname "${BASH_SOURCE[0]}")" >/dev/null && pwd)"

pushd "${SOURCE_PATH}" >/dev/null || exit

pushd "../../../.." >/dev/null || exit
mkdir -p "helm" && cd "helm"
values="$(
	cd "$(dirname ".")"
	pwd -P
)"

mkdir -p "${values}/${CONTEXT}/${NAMESPACE}"

[[ -f "${values}/${RELEASE}.yaml" ]] || sops "${values}/${RELEASE}.yaml"
[[ -f "${values}/${CONTEXT}/${RELEASE}.yaml" ]] || sops "${values}/${CONTEXT}/${RELEASE}.yaml"
[[ -f "${values}/${CONTEXT}/${NAMESPACE}/${RELEASE}.yaml" ]] || sops "${values}/${CONTEXT}/${NAMESPACE}/${RELEASE}.yaml"

popd >/dev/null || exit

OPTIONS=()

OPTIONS+=("--timeout")
OPTIONS+=("30m")

OPTIONS+=("--values")
OPTIONS+=("${values}/${RELEASE}.yaml")
OPTIONS+=("--values")
OPTIONS+=("${values}/${CONTEXT}/${RELEASE}.yaml")
OPTIONS+=("--values")
OPTIONS+=("${values}/${CONTEXT}/${NAMESPACE}/${RELEASE}.yaml")

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

[[ -n $HELM_DEBUG ]] && {
	OPTIONS+=(--dry-run)
	OPTIONS+=(--debug)
}

file="bundle/target/helm/repo/${CHART}-${VERSION}.tgz"

if [[ -f $file ]]; then

	set -x
	helm secrets upgrade --install "${RELEASE}" \
	  "${file}" \
    "${OPTIONS[@]}"
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
	helm secrets upgrade --install "${RELEASE}" \
		"${CHART}" --version "${VERSION}" \
		--repo "https://artifacts.edu-sharing.com/repository/helm/" \
		"${CREDENTIALS[@]}" \
		"${OPTIONS[@]}"
	set +x

fi

popd >/dev/null || exit
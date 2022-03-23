#!/bin/bash
set -e
set -o pipefail

COMMAND=${1:?"Please specify a helm command"}
RELEASE=${2:-""}
VERSION=${3:-""}
USERNAME=${4:-""}
PASSWORD=${5:-""}

OPTIONS=()
#OPTIONS+=(--dry-run)
#OPTIONS+=(--debug)

CONTEXT="$(kubectl config current-context)"
NAMESPACE="$(kubectl config view --minify --output 'jsonpath={..namespace}')"

root=${EDU_ROOT:-$HOME/.edusharing}

mkdir -p "${root}/helm/config"
touch "${root}/helm/config/${RELEASE}.yaml"

mkdir -p "${root}/helm/context/${CONTEXT}/${NAMESPACE}"
touch "${root}/helm/context/${CONTEXT}/${RELEASE}.yaml"
touch "${root}/helm/context/${CONTEXT}/${NAMESPACE}/${RELEASE}.yaml"

OPTIONS+=("--values")
OPTIONS+=("${root}/helm/config/${RELEASE}.yaml")
OPTIONS+=("--values")
OPTIONS+=("${root}/helm/context/${CONTEXT}/${RELEASE}.yaml")
OPTIONS+=("--values")
OPTIONS+=("${root}/helm/context/${CONTEXT}/${NAMESPACE}/${RELEASE}.yaml")

[[ -n $USERNAME && -n $PASSWORD ]] && {
	OPTIONS+=("--username")
	OPTIONS+=("${USERNAME}")
	OPTIONS+=("--password")
	OPTIONS+=("${PASSWORD}")
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

file="bundle/target/helm/repo/${RELEASE}-${VERSION:-9999.99.99-dev}.tgz"

if [[ -f $file ]]; then

	helm "${COMMAND}" "${RELEASE}" "${file}" "${OPTIONS[@]}"

else

	helm "${COMMAND}" "${RELEASE}" \
		"${RELEASE}" --version "${VERSION:->0.0.0-0}" \
		--repo "https://artifacts.edu-sharing.com/repository/helm/" \
		"${OPTIONS[@]}"

fi

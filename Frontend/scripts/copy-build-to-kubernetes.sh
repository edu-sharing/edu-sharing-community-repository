#!/bin/sh
# Copies a local `npm run build` into a running kubernetes installation.
#
#   npm run copy-build-to-kubernetes --kubeconfig=<file> --namespace=<ns>
#   npm run copy-build-to-kubernetes-web-components --kubeconfig=<file> --namespace=<ns>
#
# The app goes to every frontend server pod. Their /app/dist is only writable with
# global.debug=true (the chart then mounts it on a volume, seeded from the image); DEV
# mode re-reads index.html and revalidates the cached files, so no restart is needed.
# A pod restart drops the copy again.
# assets/i18n and assets/images also go to tomcat, which still reads them server side
# (mails, I18nAngular) - only with the full build, not with web-components.
set -eu

subpath="${1:-}"
namespace="${npm_config_namespace:-}"
[ -n "$namespace" ] || { echo "pass --namespace=<namespace> (and --kubeconfig=<file> unless the default one)" >&2; exit 1; }
dist="./dist${subpath:+/$subpath}"
frontend_server="edusharing-repository-frontend-server"
repository_service="edusharing-repository-service-0"

k() {
    kubectl ${npm_config_kubeconfig:+--kubeconfig="$npm_config_kubeconfig"} \
        --namespace="$namespace" "$@"
}

[ -d "$dist" ] || { echo "$dist does not exist - run npm run build first" >&2; exit 1; }

pods=$(k get pods -o name | sed -n "s|^pod/\($frontend_server-.*\)|\1|p")
[ -n "$pods" ] || { echo "no $frontend_server pods in namespace $namespace" >&2; exit 1; }

for pod in $pods; do
    echo "$dist -> $pod:/app/dist/${subpath:+$subpath/}"
    if ! k cp "$dist/." "$pod:/app/dist/${subpath:+$subpath/}"; then
        echo "copy to $pod failed - /app/dist is only writable with global.debug=true" >&2
        exit 1
    fi
done

if [ -z "$subpath" ]; then
    # tomcat unpacks the webapp into a temp dir with a random suffix
    assets=$(k exec "$repository_service" -- sh -c 'ls -d "$CATALINA_HOME"/temp/*0-edu-sharing*/assets | head -1')
    for dir in i18n images; do
        echo "./dist/assets/$dir -> $repository_service:$assets/$dir"
        k cp "./dist/assets/$dir/." "$repository_service:$assets/$dir/"
    done
fi

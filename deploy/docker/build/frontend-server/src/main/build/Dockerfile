########################################################################################################################
# stage 1 - typescript build of the node server
#
# The angular app is not built here: dist/ is the frontend war of the same maven build
# (unpacked by pom.xml), built once by the frontend module.
########################################################################################################################

# alpine like the runtime, so the production node_modules copied over below are
# installed for musl, not glibc
FROM ${docker.from.node.alpine} AS server-build

WORKDIR /build

COPY frontend-server/ ./

# inputs of the generated src/shared/ (scripts/sync-shared.mjs, scripts/generate-api-types.mjs),
# placed at the same location relative to the server as in the repository
# (keep in sync with SHARED_FILES there)
COPY frontend/projects/edu-sharing-ui/src/lib/util/VCard.ts \
     frontend/projects/edu-sharing-ui/src/lib/util/license-url.ts \
     /Frontend/projects/edu-sharing-ui/src/lib/util/
COPY rest-api/openapi.json /Backend/services/rest/api/src/main/resources/openapi.json

RUN set -eux \
    && npm ci --no-audit --no-fund \
    && npm run build \
    && npm test \
    && npm ci --omit=dev --no-audit --no-fund

########################################################################################################################
# stage 2 - runtime
########################################################################################################################

FROM ${docker.from.node.alpine}

# pick up os security fixes published after the base image tag. npm and corepack
# ship with the base image, are never used at runtime (CMD is plain node) and bring
# their own dependency tree - the bulk of the scanner findings outside the os
RUN set -eux \
    && apk upgrade --no-cache \
    && rm -rf /usr/local/lib/node_modules/npm /usr/local/lib/node_modules/corepack \
    && rm -f /usr/local/bin/npm /usr/local/bin/npx /usr/local/bin/corepack

# the V8 heap follows the container memory limit (cgroup): without it node takes about half of
# the limit but at least ~259 MB - more than a 256Mi container. 40 % leaves the rest to code,
# socket buffers and streams (~150-180 MB under load); the app itself uses well below 100 MB
# heap. Override with NODE_OPTIONS (helm: config.extraEnv).
ENV NODE_ENV=production \
    NODE_OPTIONS=--max-old-space-size-percentage=40 \
    HOST=0.0.0.0 \
    PORT=8080 \
    CONTEXT_PATH=/edu-sharing \
    DIST_DIR=/app/dist \
    MANAGEMENT_PORT=9090

WORKDIR /app

COPY --from=server-build --chown=node:node /build/node_modules ./node_modules
COPY --from=server-build --chown=node:node /build/build ./build
COPY --from=server-build --chown=node:node /build/package.json ./package.json
COPY --chown=node:node dist/ ./dist

USER node

# 8080 app, 9090 management server (/metrics, /health/liveness, /health/readiness) - see Frontend-Server/src/lib/management-server.ts
EXPOSE 8080 9090

# liveness on the management port never touches the backend, so a cold repository
# does not mark this container unhealthy
HEALTHCHECK --interval=15s --timeout=3s --start-period=10s --retries=3 \
    CMD node -e "fetch('http://127.0.0.1:'+process.env.MANAGEMENT_PORT+'/health/liveness').then(r=>process.exit(r.ok?0:1)).catch(()=>process.exit(1))"

CMD ["node", "build/index.js"]

########################################################################################################################

LABEL git.branch=${git.branch} \
git.closest.tag.name=${git.closest.tag.fixed} \
git.commit.id=${git.commit.id} \
git.dirty=${git.dirty} \
mvn.project.artifactId=${project.artifactId} \
mvn.project.groupId=${project.groupId} \
mvn.project.version=${project.version}

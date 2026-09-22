########################################################################################################################
# stage 1 - angular browser build (no SSR)
########################################################################################################################

FROM ${docker.from.node} AS frontend-build

WORKDIR /build

COPY frontend/ ./

# Frontend/projects/edu-sharing-api generates its client from the Backend source
# tree, which does not exist here - the spec travels as a maven artifact instead.
# Generating it up front makes the `update-if-needed` check in `prebuild` a no-op,
# so no build step ever reaches for ../../../Backend or for the network.
COPY rest-api/openapi.json projects/edu-sharing-api/build/openapi.json

RUN set -eux \
    && npm ci --no-audit --no-fund \
    && npm --prefix projects/edu-sharing-api run generate-api \
    && npm run build

########################################################################################################################
# stage 2 - typescript build of the node server
########################################################################################################################

FROM ${docker.from.node} AS server-build

WORKDIR /build

COPY frontend-server/ ./

RUN set -eux \
    && npm ci --no-audit --no-fund \
    && npm run build \
    && npm test \
    && npm ci --omit=dev --no-audit --no-fund

########################################################################################################################
# stage 3 - runtime
########################################################################################################################

FROM ${docker.from.node.slim}

ENV NODE_ENV=production \
    HOST=0.0.0.0 \
    PORT=8080 \
    CONTEXT_PATH=/edu-sharing \
    DIST_DIR=/app/dist

WORKDIR /app

COPY --from=server-build --chown=node:node /build/node_modules ./node_modules
COPY --from=server-build --chown=node:node /build/build ./build
COPY --from=server-build --chown=node:node /build/package.json ./package.json
COPY --from=frontend-build --chown=node:node /build/dist ./dist

USER node

EXPOSE 8080

# the health route never touches the backend, so a cold repository does not keep
# this container out of the load balancer
HEALTHCHECK --interval=15s --timeout=3s --start-period=10s --retries=3 \
    CMD node -e "fetch('http://127.0.0.1:'+process.env.PORT+process.env.CONTEXT_PATH+'/_health').then(r=>process.exit(r.ok?0:1)).catch(()=>process.exit(1))"

CMD ["node", "build/index.js"]

########################################################################################################################

LABEL git.branch=${git.branch} \
git.closest.tag.name=${git.closest.tag.fixed} \
git.commit.id=${git.commit.id} \
git.dirty=${git.dirty} \
mvn.project.artifactId=${project.artifactId} \
mvn.project.groupId=${project.groupId} \
mvn.project.version=${project.version}

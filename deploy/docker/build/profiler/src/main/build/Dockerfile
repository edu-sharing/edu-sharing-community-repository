FROM ${docker.registry}/mirror/docker.io/openjdk:11-jdk-bullseye

########################################################################################################################

RUN set -eux \
    && apt-get update -y \
    && apt-get install -y --no-install-recommends \
        jq \
		php \
		wait-for-it \
        xmlstarlet \
    && apt-get clean \
    && rm -r /var/lib/apt/lists/*

WORKDIR /opt/profiler

RUN set -eux \
    && wget -qO- https://github.com/Granulate/async-profiler/releases/download/v2.0g1/async-profiler-2.0-linux-x64.tar.gz | tar xvz -C /opt/profiler \
    && wget -qO- https://github.com/Granulate/phpspy/archive/refs/tags/v0.6.0-g3.tar.gz | tar xvz -C /opt/profiler \
    && wget -q https://github.com/Granulate/phpspy/releases/download/v0.6.0-g3/phpspy -O /opt/profiler/phpspy-0.6.0-g3/phpspy \
    && mkdir -p /opt/profiler/report \
    && chmod -R a+rwx /opt/profiler

COPY assets bin/

ENTRYPOINT ["sh", "-c", "sleep infinity"]

########################################################################################################################

LABEL git.branch=${git.branch}
LABEL git.closest.tag.name=${git.closest.tag.fixed}
LABEL git.commit.id=${git.commit.id}
LABEL git.dirty=${git.dirty}
LABEL mvn.project.artifactId=${project.artifactId}
LABEL mvn.project.groupId=${project.groupId}
LABEL mvn.project.version=${project.version}

FROM docker.io/debian:10

########################################################################################################################

RUN set -eux \
    && echo 'deb http://deb.debian.org/debian buster-backports main' >> /etc/apt/sources.list \
	&& apt-get update -y \
	&& apt-get install -y --no-install-recommends -t buster-backports \
	    curl \
        jq \
        wait-for-it \
        xmlstarlet \
    && apt-get clean \
    && rm -r /var/lib/apt/lists/*

########################################################################################################################

COPY assets/entrypoint.sh /usr/local/bin/

RUN set -eux \
    && chmod +x /usr/local/bin/entrypoint.sh

########################################################################################################################

ENTRYPOINT ["entrypoint.sh"]

########################################################################################################################

LABEL community.version=${community.rendering.version}
LABEL git.branch=${git.branch}
LABEL git.closest.tag.name=${git.closest.tag.name}
LABEL git.commit.id=${git.commit.id}
LABEL git.dirty=${git.dirty}
LABEL mvn.project.artifactId=${project.artifactId}
LABEL mvn.project.groupId=${project.groupId}
LABEL mvn.project.version=${project.version}

FROM ${docker.from.openjdk.11}

########################################################################################################################

RUN set -eux \
    && apt-get update -y \
    && apt-get install -y --no-install-recommends \
		jq \
		wait-for-it \
        xmlstarlet \
    && apt-get clean \
    && rm -r /var/lib/apt/lists/*

########################################################################################################################

ENV ALF_HOME /opt/alfresco
ENV PATH $ALF_HOME/bin:$PATH

WORKDIR $ALF_HOME

COPY assets/entrypoint.sh bin/

RUN set -eux \
	&& find bin -type f -name '*.sh' -exec chmod +x {} \;

RUN set -eux \
    && adduser --uid=1000 --home=$ALF_HOME --disabled-password --gecos "" --shell=/bin/bash worker \
    && chown -RL worker:worker . \
    && chown -RL worker:worker /tmp

RUN set -eux \
    && apt-get update -y \
    && apt-get install -y --no-install-recommends \
        ffmpeg \
	&& apt-get clean \
    && rm -r /var/lib/apt/lists/*

USER worker

### Tracker ############################################################################################################

COPY --chown=worker:worker artifacts/edu_sharing-community-repository-backend-transform-${org.edu_sharing:edu_sharing-community-repository-backend-transform:jar.version}.jar ./

########################################################################################################################

EXPOSE 8080 8081

ENTRYPOINT ["entrypoint.sh"]

########################################################################################################################

LABEL git.branch=${git.branch}
LABEL git.closest.tag.name=${git.closest.tag.fixed}
LABEL git.commit.id=${git.commit.id}
LABEL git.dirty=${git.dirty}
LABEL mvn.project.artifactId=${project.artifactId}
LABEL mvn.project.groupId=${project.groupId}
LABEL mvn.project.version=${project.version}

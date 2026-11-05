FROM ${docker.from.openjdk.17.liberica.debian}

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
	&& find bin -type f -name '*.sh' -exec chmod +x {} \; \
    && adduser --uid=1000 --home=$ALF_HOME --disabled-password --gecos "" --shell=/bin/bash worker \
    && chown -RL worker:worker . \
    && chown -RL worker:worker /tmp \
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

LABEL git.branch=${git.branch} \
git.closest.tag.name=${git.closest.tag.fixed} \
git.commit.id=${git.commit.id} \
git.dirty=${git.dirty} \
mvn.project.artifactId=${project.artifactId} \
mvn.project.groupId=${project.groupId} \
mvn.project.version=${project.version}

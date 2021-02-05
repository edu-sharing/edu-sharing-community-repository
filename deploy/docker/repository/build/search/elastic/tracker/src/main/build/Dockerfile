FROM docker.io/openjdk:8-jdk-buster

########################################################################################################################

RUN set -eux \
    && echo 'deb http://deb.debian.org/debian stretch-backports main' >> /etc/apt/sources.list \
    && apt-get update -y \
    && apt-get install -y --no-install-recommends -t stretch-backports \
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
    && chmod +x bin/entrypoint.sh

RUN set -eux \
    && adduser --uid=1000 --home=$ALF_HOME --disabled-password --gecos "" --shell=/bin/bash worker \
    && chown -RL worker:worker . \
    && chown -RL worker:worker /tmp

USER worker

### Tracker ############################################################################################################

COPY --chown=worker:worker artifacts/*.jar tracker.jar

########################################################################################################################

EXPOSE 8080

ENTRYPOINT ["entrypoint.sh"]

########################################################################################################################

LABEL git.branch=${git.branch}
LABEL git.closest.tag.name=${git.closest.tag.name}
LABEL git.commit.id=${git.commit.id}
LABEL git.dirty=${git.dirty}
LABEL mvn.project.artifactId=${project.artifactId}
LABEL mvn.project.groupId=${project.groupId}
LABEL mvn.project.version=${project.version}

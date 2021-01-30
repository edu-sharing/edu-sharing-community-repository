FROM docker.io/tomcat:${repository.tomcat.version}-jdk8

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

RUN set -eux \
    && ln -sf /usr/local/tomcat tomcat

COPY assets/entrypoint.sh bin/

RUN set -eux \
    && chmod +x bin/entrypoint.sh

RUN set -eux \
    && adduser --uid=1000 --home=$ALF_HOME --disabled-password --gecos "" --shell=/bin/bash worker \
    && chown -RL worker:worker . \
    && chown -RL worker:worker /tmp

USER worker

### Tomcat #############################################################################################################

COPY --chown=worker:worker assets/tomcat/logging.properties tomcat/conf/logging.properties

### Alfresco Solr4 #####################################################################################################

COPY --chown=worker:worker artifacts/alfresco-solr4-distribution-${repository.alfresco.platform.version}.zip /tmp/alfresco-solr4-distribution.zip

RUN set -eux \
	&& mkdir -p /tmp/install/alfresco-solr4 \
	&& unzip /tmp/alfresco-solr4-distribution.zip -d /tmp/install/alfresco-solr4 \
	&& rm -f /tmp/alfresco-solr4-distribution.zip \
	&& mkdir -p alf_data \
	&& mkdir -p solr4 \
    && mv /tmp/install/alfresco-solr4/alfrescoModels solr4 \
    && mv /tmp/install/alfresco-solr4/archive-SpacesStore solr4 \
    && mv /tmp/install/alfresco-solr4/conf solr4 \
    && mv /tmp/install/alfresco-solr4/templates solr4 \
    && mv /tmp/install/alfresco-solr4/workspace-SpacesStore solr4 \
    && mv /tmp/install/alfresco-solr4/solr.xml solr4 \
    && mkdir -p tomcat/conf/Catalina/localhost \
    && mv /tmp/install/alfresco-solr4/context.xml tomcat/conf/Catalina/localhost/solr4.xml \
    && mkdir -p tomcat/webapps/solr4 \
	&& unzip /tmp/install/alfresco-solr4/solr4.war -d tomcat/webapps/solr4 \
    && rm -rf /tmp/install/alfresco-solr4 \
    && sed -i \
        's|@@ALFRESCO_SOLR4_DIR@@|'"$ALF_HOME"'/solr4|' \
        tomcat/conf/Catalina/localhost/solr4.xml \
    && sed -i \
        's|@@ALFRESCO_SOLR4_MODEL_DIR@@|'"$ALF_HOME"'/alf_data/solr4/model|' \
        tomcat/conf/Catalina/localhost/solr4.xml \
    && sed -i \
        's|@@ALFRESCO_SOLR4_CONTENT_DIR@@|'"$ALF_HOME"'/alf_data/solr4/content|' \
        tomcat/conf/Catalina/localhost/solr4.xml \
    && sed -i \
        's|@@ALFRESCO_SOLR4_DATA_DIR@@|'"$ALF_HOME"'/alf_data/solr4/index|' \
        solr4/archive-SpacesStore/conf/solrcore.properties \
    && sed -i \
        's|@@ALFRESCO_SOLR4_DATA_DIR@@|'"$ALF_HOME"'/alf_data/solr4/index|' \
        solr4/workspace-SpacesStore/conf/solrcore.properties

COPY --chown=worker:worker assets/solr4 solr4/log4j-solr.properties

########################################################################################################################

EXPOSE 8080

ENTRYPOINT ["entrypoint.sh"]
CMD ["catalina.sh", "run"]

########################################################################################################################

LABEL git.branch=${git.branch}
LABEL git.closest.tag.name=${git.closest.tag.name}
LABEL git.commit.id=${git.commit.id}
LABEL git.dirty=${git.dirty}
LABEL mvn.project.artifactId=${project.artifactId}
LABEL mvn.project.groupId=${project.groupId}
LABEL mvn.project.version=${project.version}

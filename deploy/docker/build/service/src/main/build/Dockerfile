FROM ${docker.edu_sharing.community.common.alfresco.repository}

########################################################################################################################

USER root

RUN set -eux \
    && ln -sf /opt/bitnami/tomcat tomcat

RUN set -eux \
    && mkdir -p tomcat/shared/classes/config/defaults \
    && mkdir -p tomcat/shared/classes/config/plugins/plugin-cluster \
    && mkdir -p tomcat/shared/classes/config/plugins/plugin-elastic \
    && mkdir -p tomcat/shared/classes/config/plugins/plugin-mongo \
    && mkdir -p tomcat/shared/classes/config/plugins/plugin-remote \
    && mkdir -p tomcat/shared/classes/config/plugins/plugin-transform \
    && mkdir -p tomcat/shared/classes/config/cluster/applications \
    && mkdir -p tomcat/shared/classes/config/node \
    && chown -RL worker:worker . \
    && chown -RL worker:worker /tmp

#USER worker
RUN set -eux \
    && rm \
        tomcat/webapps/alfresco/WEB-INF/classes/logging.properties \
        tomcat/webapps/alfresco/WEB-INF/classes/log4j.properties \
        tomcat/webapps/alfresco/WEB-INF/lib/commons-lang3-* \
        tomcat/webapps/alfresco/WEB-INF/lib/jackson-* \
        tomcat/webapps/alfresco/WEB-INF/lib/log4j-* \
        tomcat/webapps/alfresco/WEB-INF/lib/slf4j-* \
        tomcat/webapps/alfresco/WEB-INF/lib/jsr305-* \
        tomcat/webapps/alfresco/WEB-INF/lib/woodstox-core-* \
        tomcat/webapps/alfresco/WEB-INF/lib/owasp-java-html-sanitizer-* \
        tomcat/webapps/alfresco/WEB-INF/lib/reload4j-1.2.18.3*

COPY --chown=worker:worker artifacts/edu_sharing-community-repository-backend-tomcat-${org.edu_sharing:edu_sharing-community-repository-backend-tomcat:jar.version}.jar tomcat/lib/
COPY --chown=worker:worker webapp/*.war tomcat/webapps/edu-sharing.war

RUN set -eux \
    && mkdir -p tomcat/webapps/edu-sharing \
	&& unzip tomcat/webapps/edu-sharing.war -d tomcat/webapps/edu-sharing \
	&& rm -f tomcat/webapps/edu-sharing.war \
	&& cp tomcat/webapps/edu-sharing/META-INF/context.xml tomcat/conf/Catalina/localhost/edu-sharing.xml

COPY --chown=worker:worker amps amps/
COPY --chown=worker:worker config tomcat/shared/assets/

RUN set -eux \
    && chown -RL worker:worker alf_data \
    && chown -RL worker:worker tomcat/conf \
    && chown -RL worker:worker tomcat/shared \
    && chown -RL worker:worker tomcat/webapps \
    && chown -RL worker:worker tomcat/work \
    && chown -RL worker:worker tomcat/temp

########################################################################################################################

COPY --chown=worker:worker assets/entrypoint.sh assets/reinstall.sh assets/libcheck.sh bin/

RUN set -eux \
	&& find bin -type f -name '*.sh' -exec chmod +x {} \;

########################################################################################################################

RUN set -eux \
    && mkdir profiler \
    && wget -qO- https://github.com/Granulate/async-profiler/releases/download/v2.0g1/async-profiler-2.0-linux-x64.tar.gz | tar xvz -C profiler \
    && chown -RL worker:worker profiler

COPY --chown=worker:worker assets/profiler profiler/

########################################################################################################################

USER worker

STOPSIGNAL SIGWINCH

EXPOSE 8009 8080 8081

ENTRYPOINT ["entrypoint.sh"]
CMD ["catalina.sh", "run"]

VOLUME [ "/opt/alfresco/alf_data", "/opt/bitnami/tomcat/shared/classes/config/cluster", "/opt/bitnami/tomcat/shared/classes/config/node" ]

########################################################################################################################

LABEL git.branch=${git.branch}
LABEL git.closest.tag.name=${git.closest.tag.fixed}
LABEL git.commit.id=${git.commit.id}
LABEL git.dirty=${git.dirty}
LABEL mvn.project.artifactId=${project.artifactId}
LABEL mvn.project.groupId=${project.groupId}
LABEL mvn.project.version=${project.version}

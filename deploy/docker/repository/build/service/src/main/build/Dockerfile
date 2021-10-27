
FROM ${docker.from.tomcat}


RUN set -eux \
    && echo 'deb http://deb.debian.org/debian stretch-backports main' >> /etc/apt/sources.list \
    && apt-get update -y \
    && apt-get install -y --no-install-recommends \
	    ffmpeg \
	    redis-tools \
    && apt-get install -y --no-install-recommends -t stretch-backports \
        exiftool \
	    ghostscript \
	    gsfonts \
	    imagemagick \
	    jq \
        postgresql-client \
        ruby-hocon \
        wait-for-it \
        xmlstarlet \
    && apt-get clean \
    && rm -r /var/lib/apt/lists/*


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


RUN set -eux \
    && mkdir -p tomcat/shared/classes/alfresco \
    && mkdir -p tomcat/shared/lib \
    && mkdir -p tomcat/logs \
    && touch tomcat/logs/catalina.out

COPY --chown=worker:worker artifacts/jmx_prometheus_javaagent-${repository.prometheus.jmx.version}.jar tomcat/agent/jmx_prometheus_javaagent.jar
COPY --chown=worker:worker assets/agent/jmx-exporter-config.yaml tomcat/agent/

COPY --chown=worker:worker artifacts/postgresql-${repository.postgresql.client.version}.jar tomcat/lib/
COPY --chown=worker:worker artifacts/redisson-all-${repository.redis.client.version}.jar tomcat/lib/
COPY --chown=worker:worker artifacts/redisson-tomcat-${repository.tomcat.major}-${repository.redis.client.version}.jar tomcat/lib/

COPY --chown=worker:worker assets/tomcat/logging.properties tomcat/conf/logging.properties
COPY --chown=worker:worker assets/tomcat/redisson.yaml tomcat/conf/redisson.yaml

RUN set -eux \
    && sed -i -r \
        's|^[#]*\s*shared\.loader=.*|shared.loader=${catalina.base}/shared/classes,${catalina.base}/shared/lib/*.jar|' \
        tomcat/conf/catalina.properties \
    && xmlstarlet ed -L \
        -s '/Context' -t elem -n "Resources" -v "" \
        --var resources '$prev' \
        -i '$resources' -t attr -n "cacheMaxSize" -v "20480" \
        tomcat/conf/context.xml \
    && xmlstarlet ed -L \
        -i '/Server/Service[@name="Catalina"]/Engine[@name="Catalina"]/Host[@name="localhost"]' -t attr -n 'hostConfigClass' -v 'org.edu_sharing.catalina.startup.OrderedHostConfig' \
        -d '/Server/Service[@name="Catalina"]/Engine[@name="Catalina"]/Host[@name="localhost"]/Valve[@className="org.apache.catalina.valves.AccessLogValve"]/@directory' \
        -d '/Server/Service[@name="Catalina"]/Engine[@name="Catalina"]/Host[@name="localhost"]/Valve[@className="org.apache.catalina.valves.AccessLogValve"]/@prefix' \
        -d '/Server/Service[@name="Catalina"]/Engine[@name="Catalina"]/Host[@name="localhost"]/Valve[@className="org.apache.catalina.valves.AccessLogValve"]/@suffix' \
        -u '/Server/Service[@name="Catalina"]/Engine[@name="Catalina"]/Host[@name="localhost"]/Valve[@className="org.apache.catalina.valves.AccessLogValve"]/@className' -v 'org.edu_sharing.catalina.valves.StdoutAccessLogValve' \
        tomcat/conf/server.xml


COPY --chown=worker:worker artifacts/alfresco-platform-community-distributionzip-${repository.alfresco.platform.version}.zip \
    /tmp/alfresco-platform-community-distributionzip.zip

RUN set -eux \
    && mkdir -p /tmp/install \
	&& unzip /tmp/alfresco-platform-community-distributionzip.zip -d /tmp/install \
	&& rm -f /tmp/alfresco-platform-community-distributionzip.zip \
	&& mv /tmp/install/alfresco-platform-community-distributionzip* /tmp/install/alfresco-platform \
    && mv /tmp/install/alfresco-platform/alf_data . \
    && mv /tmp/install/alfresco-platform/licenses . \
    && mv /tmp/install/alfresco-platform/README.txt . \
    && cp -rf /tmp/install/alfresco-platform/bin/* bin \
    && rm -f /tmp/install/alfresco-platform/web-server/lib/postgresql* \
    && cp -rf /tmp/install/alfresco-platform/web-server/* tomcat/ \
    && xmlstarlet ed -L -d '/Context/Loader' tomcat/conf/Catalina/localhost/alfresco.xml \
    && rm -rf /tmp/install/alfresco-platform \
    && mkdir -p tomcat/webapps/alfresco \
    && unzip tomcat/webapps/alfresco.war -d tomcat/webapps/alfresco -x "WEB-INF/lib/hazelcast*" \
    && rm -f tomcat/webapps/alfresco.war \
    && mv tomcat/shared/classes/alfresco-global.properties.sample tomcat/shared/classes/alfresco-global.properties \
    && sed -i 's/$/\n/' tomcat/shared/classes/alfresco-global.properties \
    && sed -i -r \
        's|^[#]*\s*dir\.root=.*|dir.root='"$ALF_HOME/alf_data"'|' \
        tomcat/shared/classes/alfresco-global.properties \
    && mv alf_data/keystore . \
    && sed -i -r \
        's|^[#]*\s*dir\.keystore=.*|dir.keystore='"$ALF_HOME/keystore"'|' \
        tomcat/shared/classes/alfresco-global.properties \
    && sed -i -r \
        's|^[#]*\s*img\.root=.*|img.root=/usr|' \
        tomcat/shared/classes/alfresco-global.properties \
    && sed -i -r \
        's|^[#]*\s*img\.gslib=.*|img.gslib=/usr/bin|' \
        tomcat/shared/classes/alfresco-global.properties \
    && echo 'exiftool.dyn=/usr/bin' >> \
        tomcat/shared/classes/alfresco-global.properties \
    && echo 'exiftool.exe=${exiftool.dyn}/exiftool' >> \
        tomcat/shared/classes/alfresco-global.properties \
    && echo 'ffmpeg.dyn=/usr/bin' >> \
        tomcat/shared/classes/alfresco-global.properties \
    && echo 'ffmpeg.exe=${ffmpeg.dyn}/ffmpeg' >> \
        tomcat/shared/classes/alfresco-global.properties \
    && echo 'img.dyn=/usr/bin' >> \
        tomcat/shared/classes/alfresco-global.properties \
    && echo 'img.exe=${img.dyn}/convert' >> \
        tomcat/shared/classes/alfresco-global.properties \
    && echo 'cifs.enabled=false' >> \
        tomcat/shared/classes/alfresco-global.properties \
    && echo 'ftp.enabled=false' >> \
        tomcat/shared/classes/alfresco-global.properties

COPY --chown=worker:worker assets/alfresco/log4j.properties tomcat/webapps/alfresco/WEB-INF/classes/log4j.properties
COPY --chown=worker:worker assets/alfresco/web-scripts-config-custom.xml tomcat/shared/classes/alfresco/extension/web-scripts-config-custom.xml


COPY --chown=worker:worker artifacts/alfresco-content-services-share-distribution-${repository.alfresco.share.version}.zip \
    /tmp/alfresco-content-services-share-distribution.zip

RUN set -eux \
    && mkdir -p /tmp/install \
	&& unzip /tmp/alfresco-content-services-share-distribution.zip -d /tmp/install \
	&& rm -f /tmp/alfresco-content-services-share-distribution.zip \
	&& mv /tmp/install/alfresco-content-services-share-distribution* /tmp/install/alfresco-share \
	&& java -jar bin/alfresco-mmt.jar install /tmp/install/alfresco-share/amps/alfresco-share-services.amp \
        tomcat/webapps/alfresco -nobackup -force \
    && mv /tmp/install/alfresco-share/web-extension* tomcat/shared/classes/alfresco/web-extension \
    && cp -rf /tmp/install/alfresco-share/web-server/* tomcat/ \
    && xmlstarlet ed -L -d '/Context/Loader' tomcat/conf/Catalina/localhost/share.xml \
    && rm -rf /tmp/install/alfresco-share \
    && mkdir -p tomcat/webapps/share \
    && unzip tomcat/webapps/share.war -d tomcat/webapps/share \
    && rm -f tomcat/webapps/share.war

COPY --chown=worker:worker assets/share/log4j.properties tomcat/webapps/share/WEB-INF/classes/log4j.properties
COPY --chown=worker:worker assets/share/share-config-custom.xml tomcat/shared/classes/alfresco/web-extension/share-config-custom.xml


COPY --chown=worker:worker artifacts/alfresco-pdf-renderer-${repository.alfresco.pdf-renderer.version}-linux.tgz /tmp/alfresco-pdf-renderer.tgz

RUN set -eux \
	&& mkdir -p /tmp/install/alfresco-pdf-renderer \
    && tar -xvzf /tmp/alfresco-pdf-renderer.tgz -C /tmp/install/alfresco-pdf-renderer \
    && rm -f /tmp/alfresco-pdf-renderer.tgz \
    && mkdir -p common \
    && mv /tmp/install/alfresco-pdf-renderer common/alfresco-pdf-renderer \
    && echo 'alfresco-pdf-renderer.root='"$ALF_HOME/common/alfresco-pdf-renderer" >> \
        tomcat/shared/classes/alfresco-global.properties \
    && echo 'alfresco-pdf-renderer.exe=${alfresco-pdf-renderer.root}/alfresco-pdf-renderer' >> \
        tomcat/shared/classes/alfresco-global.properties


COPY --chown=worker:worker artifacts/edu_sharing-community-repository-backend-tomcat-${org.edu_sharing:edu_sharing-community-repository-backend-tomcat:jar.version}.jar tomcat/lib/


COPY --chown=worker:worker artifacts/edu_sharing-community-repository-webapp-${org.edu_sharing:edu_sharing-community-repository-webapp:war.version}.war tomcat/webapps/

RUN set -eux \
    && mkdir -p tomcat/webapps/edu-sharing \
	&& unzip tomcat/webapps/edu_sharing-community-repository-webapp-${org.edu_sharing:edu_sharing-community-repository-webapp:war.version}.war -d tomcat/webapps/edu-sharing \
	&& rm -f tomcat/webapps/edu_sharing-community-repository-webapp-${org.edu_sharing:edu_sharing-community-repository-webapp:war.version}.war \
	&& cp tomcat/webapps/edu-sharing/META-INF/context.xml tomcat/conf/Catalina/localhost/edu-sharing.xml

COPY --chown=worker:worker assets/edu-sharing/log4j2.xml tomcat/webapps/edu-sharing/WEB-INF/log4j2.xml

RUN set -eux \
    && xmlstarlet ed -L \
        -d '/Context/Manager[@className="org.redisson.tomcat.RedissonSessionManager"]' \
        -s '/Context' -t elem -n "Manager" -v "" \
        --var redis '$prev' \
        -i '$redis' -t attr -n "className" -v "org.redisson.tomcat.RedissonSessionManager" \
        -i '$redis' -t attr -n "configPath" -v "tomcat/conf/redisson.yaml" \
        -i '$redis' -t attr -n "readMode" -v "MEMORY" \
        -i '$redis' -t attr -n "updateMode" -v "AFTER_REQUEST" \
        -i '$redis' -t attr -n "broadcastSessionEvents" -v "true" \
        -i '$redis' -t attr -n "broadcastSessionUpdates" -v "true" \
        tomcat/conf/Catalina/localhost/edu-sharing.xml


COPY --chown=worker:worker amps amps/

RUN set -eux \
    && if [ -d amps/alfresco/1 ] && [ -n $(ls -A amps/alfresco/1 | wc -l) ]; then java -jar bin/alfresco-mmt.jar install amps/alfresco/1 tomcat/webapps/alfresco -directory -nobackup -force; fi \
    && if [ -d amps/alfresco/2 ] && [ -n $(ls -A amps/alfresco/2 | wc -l) ]; then java -jar bin/alfresco-mmt.jar install amps/alfresco/2 tomcat/webapps/alfresco -directory -nobackup -force; fi \
    && if [ -d amps/edu-sharing/1 ] && [ -n $(ls -A amps/edu-sharing/1 | wc -l) ]; then java -jar bin/alfresco-mmt.jar install amps/edu-sharing/1 tomcat/webapps/edu-sharing -directory -nobackup -force; fi


COPY --chown=worker:worker artifacts/edu_sharing-community-repository-config-${org.edu_sharing:edu_sharing-community-repository-config:jar.version}.jar tomcat/shared/lib/

RUN set -eux \
	&& unzip tomcat/shared/lib/edu_sharing-community-repository-config-${org.edu_sharing:edu_sharing-community-repository-config:jar.version}.jar \
	    -d tomcat/shared/classes -x META-INF/* \
	&& rm -f tomcat/shared/lib/edu_sharing-community-repository-config-${org.edu_sharing:edu_sharing-community-repository-config:jar.version}.jar \
	&& touch tomcat/shared/classes/config/edu-sharing.conf \
	&& touch tomcat/shared/classes/config/edu-sharing.deployment.conf \
	&& touch tomcat/shared/classes/config/edu-sharing.server.conf \
    && tar cvf tomcat/shared.tar tomcat/shared


EXPOSE 8080 8081

ENTRYPOINT ["entrypoint.sh"]
CMD ["catalina.sh", "run"]


LABEL git.branch=${git.branch}
LABEL git.closest.tag.name=${git.closest.tag.fixed}
LABEL git.commit.id=${git.commit.id}
LABEL git.dirty=${git.dirty}
LABEL mvn.project.artifactId=${project.artifactId}
LABEL mvn.project.groupId=${project.groupId}
LABEL mvn.project.version=${project.version}

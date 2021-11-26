FROM docker.io/${os.distribution}:${os.codename}

ENV ALF_HOME /opt/alfresco
WORKDIR $ALF_HOME

RUN set -eux \
    && ln -sf /usr/share/zoneinfo/Europe/Berlin /etc/localtime

RUN set -eux \
    && apt-get update -y \
    && apt-get install -y --no-install-recommends \
        locales \
    && sed -i 's/^\(\\s+\)/# \1/' /etc/locale.gen \
    && sed -i 's/^# *\(de_DE.UTF-8\)/\1/' /etc/locale.gen \
    && locale-gen \
    && update-locale LANG=de_DE.utf8 \
    && apt-get clean \
    && rm -r /var/lib/apt/lists/*

RUN set -eux \
    && apt-get update -y \
    && apt-get install -y --no-install-recommends \
        apt-transport-https \
        ca-certificates \
        gnupg \
        lsb-release \
        procps \
        unzip \
        wget \
        xmlstarlet \
    && apt-get clean \
    && rm -r /var/lib/apt/lists/*

RUN set -eux \
    && apt-get update -y \
    && apt-get install -y --no-install-recommends \
        exiftool \
        ffmpeg \
        ghostscript \
        gsfonts \
        imagemagick \
        libreoffice \
    && apt-get clean \
    && rm -r /var/lib/apt/lists/*

RUN set -eux \
    && wget -q -O - https://www.postgresql.org/media/keys/ACCC4CF8.asc | apt-key add - \
    && echo "deb http://apt.postgresql.org/pub/repos/apt `lsb_release -cs`-pgdg main" | tee  /etc/apt/sources.list.d/pgdg.list \
    && apt-get update -y \
    && apt-get install -y --no-install-recommends \
        postgresql-13 \
    && pg_dropcluster 13 main \
    && apt-get clean \
    && rm -r /var/lib/apt/lists/*

RUN set -eux \
    && wget -q https://adoptopenjdk.jfrog.io/adoptopenjdk/api/gpg/key/public \
    && gpg --no-default-keyring --keyring ./adoptopenjdk-keyring.gpg --import public \
    && gpg --no-default-keyring --keyring ./adoptopenjdk-keyring.gpg --export --output adoptopenjdk-archive-keyring.gpg \
    && mv adoptopenjdk-archive-keyring.gpg /usr/share/keyrings \
    && rm adoptopenjdk-keyring.gpg public \
    && echo "deb [signed-by=/usr/share/keyrings/adoptopenjdk-archive-keyring.gpg] https://adoptopenjdk.jfrog.io/adoptopenjdk/deb `lsb_release -cs` main" | tee /etc/apt/sources.list.d/adoptopenjdk.list \
    && apt-get update -y \
    && apt-get install -y --no-install-recommends \
        adoptopenjdk-8-hotspot \
    && update-java-alternatives -s adoptopenjdk-8-hotspot-amd64 \
    && apt-get clean \
    && rm -r /var/lib/apt/lists/*

RUN set -eux \
    && wget -q https://dlcdn.apache.org/tomcat/tomcat-8/v8.5.73/bin/apache-tomcat-8.5.73.tar.gz \
    && mkdir -p tomcat \
    && tar -xzvf apache-tomcat-8.5.73.tar.gz -C tomcat --strip 1 \
    && rm apache-tomcat-8.5.73.tar.gz

RUN set -eux \
    && wget -q https://download.alfresco.com/release/community/201707-build-00028/alfresco-community-installer-201707-linux-x64.bin \
    && chmod +x alfresco-community-installer-201707-linux-x64.bin \
    && mkdir -p tomcat/webapps \
    && ./alfresco-community-installer-201707-linux-x64.bin \
      --mode unattended \
      --enable-components alfrescosolr4 \
      --disable-components javaalfresco,postgres,libreofficecomponent,alfrescosolr,aosmodule,alfrescowcmqs,alfrescogoogledocs \
      --prefix $ALF_HOME \
      --jdbc_url jdbc:postgresql://127.0.0.1/alfresco \
      --postgres_port 5432 \
      --jdbc_username alfresco \
      --jdbc_password alfresco \
      --alfresco_admin_password admin \
      --baseunixservice_install_as_service 0 \
      --tomcat_installation_type existing \
    && rm alfresco-community-installer-201707-linux-x64.bin \
    && mkdir -p tomcat/webapps/alfresco \
    && unzip tomcat/webapps/alfresco.war -d tomcat/webapps/alfresco \
    && rm tomcat/webapps/alfresco.war \
    && mkdir -p tomcat/webapps/share \
    && unzip tomcat/webapps/share.war -d tomcat/webapps/share \
    && rm tomcat/webapps/share.war \
    && mkdir -p tomcat/webapps/solr4 \
    && unzip tomcat/webapps/solr4.war -d tomcat/webapps/solr4 \
    && rm tomcat/webapps/solr4.war

COPY assets ./

RUN set -eux \
    && sed -i -r \
        's|^[#]*\s*shared\.loader=.*|shared.loader=${catalina.base}/shared/classes,${catalina.base}/shared/lib/*.jar|' \
        tomcat/conf/catalina.properties \
    && xmlstarlet ed -L \
        -u '/Server/Service[@name="Catalina"]/Connector[@port="8080"]/@protocol' -v 'org.apache.coyote.http11.Http11NioProtocol' \
        -d '/Server/Service[@name="Catalina"]/Connector[@port="8009"]' \
        -d '/Server/Service[@name="Catalina"]/Connector[@port="8443"]' \
        tomcat/conf/server.xml \
    && xmlstarlet ed -L \
        -d '/Context/Loader' \
        -s '/Context' -t elem -n "Resources" -v "" \
        --var resources '$prev' \
        -i '$resources' -t attr -n "cacheMaxSize" -v "20480" \
        tomcat/conf/Catalina/localhost/alfresco.xml \
    && xmlstarlet ed -L \
        -d '/Context/Loader' \
        -s '/Context' -t elem -n "Resources" -v "" \
        --var resources '$prev' \
        -i '$resources' -t attr -n "cacheMaxSize" -v "20480" \
        tomcat/conf/Catalina/localhost/share.xml \
    && echo 'solr.port=8080' >> tomcat/shared/classes/alfresco-global.properties \
    && echo 'solr.secureComms=none' >> tomcat/shared/classes/alfresco-global.properties \
    && sed -i -r \
        's|^[#]*\s*alfresco\.secureComms=.*|alfresco.secureComms=none|' \
        solr4/workspace-SpacesStore/conf/solrcore.properties \
    && sed -i -r \
        's|^[#]*\s*alfresco\.secureComms=.*|alfresco.secureComms=none|' \
        solr4/archive-SpacesStore/conf/solrcore.properties

RUN set -eux \
    && mkdir -p alf_data/postgresql postgresql \
    && pg_createcluster \
      --datadir=$ALF_HOME/alf_data/postgresql \
      --logfile=$ALF_HOME/postgresql/postgresql.log \
      --encoding UTF8 \
      --port=5432 \
      --start \
      --start-conf=manual \
      13 alfresco \
    && su -c 'psql -f postgresql/conf/init_db.sql' postgres \
    && pg_ctlcluster 13 alfresco stop \
    && rm tomcat/lib/postgresql-* \
    && wget -q https://jdbc.postgresql.org/download/postgresql-42.3.1.jar \
    && mv postgresql-42.3.1.jar tomcat/lib \
    && sed -i -r \
        "s|^[#]*\s*listen_addresses\s*=\s*.*|listen_addresses = '\*'                 # what IP address(es) to listen on;|" \
        /etc/postgresql/13/alfresco/postgresql.conf \
    && sed -i -r \
        's|host    all             all             127.0.0.1/32            md5|host    all             all             0.0.0.0/0            md5|' \
        /etc/postgresql/13/alfresco/pg_hba.conf \
    && sed -i -r \
        's|host    all             all             ::1/128                 md5|host    all             all             ::0/0                 md5|' \
        /etc/postgresql/13/alfresco/pg_hba.conf

RUN set -eux \
    && rm -rf common/lib \
    && sed -i -r \
        's|^[#]*\s*img\.root=.*|img.root=/usr|' \
        tomcat/shared/classes/alfresco-global.properties \
    && sed -i -r \
        's|^[#]*\s*img\.root=.*|img.root=/usr|' \
        tomcat/shared/classes/alfresco-global.properties \
    && sed -i -r \
        's|^[#]*\s*ooo\.exe=.*|ooo.host=127.0.0.1|' \
        tomcat/shared/classes/alfresco-global.properties

RUN set -eux \
    && apt-get update -y \
    && apt-get install -y --no-install-recommends \
	    maven=3.6.3-* \
    && apt-get clean \
    && rm -r /var/lib/apt/lists/*

RUN set -eux \
    && apt-get update -y \
    && apt-get install -y --no-install-recommends \
        curl \
        dnsutils \
        jq \
        lsof \
        nano \
        net-tools \
        vim \
    && apt-get clean \
    && rm -r /var/lib/apt/lists/*

COPY install.sh .

#Test Install and Update
RUN set -eux \
    && ./install.sh \
    && ./install.sh
    #&& rm -rf alf_data/solr4/index alf_data/solr4/model alf_data/solr4/content


ENTRYPOINT ["./entrypoint.sh"]

EXPOSE 8080

VOLUME alf_data

LABEL git.branch=${git.branch}
LABEL git.closest.tag.name=${git.closest.tag.fixed}
LABEL git.commit.id=${git.commit.id}
LABEL git.dirty=${git.dirty}
LABEL mvn.project.artifactId=${project.artifactId}
LABEL mvn.project.groupId=${project.groupId}
LABEL mvn.project.version=${project.version}

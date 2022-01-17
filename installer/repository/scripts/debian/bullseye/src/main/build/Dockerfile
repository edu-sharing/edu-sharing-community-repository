########################################################################################################################
#                                                                                                                      #
#         !!!         THIS DOCKER IS FOR TESTING ONLY PURPOSES AND NOT FOR PRODUCTIVE DEPLOYMENT         !!!           #
#                                                                                                                      #
########################################################################################################################

FROM docker.io/${os.distribution}:${os.codename}

ENV ALF_HOME /opt/alfresco
ENV PATH $ALF_HOME/bin:$PATH

WORKDIR $ALF_HOME

RUN set -eux \
    && ln -sf /usr/share/zoneinfo/Europe/Berlin /etc/localtime

RUN set -eux \
    && adduser --uid=1000 --home=$ALF_HOME --disabled-password --gecos "" --shell=/bin/bash worker \
    && chown -RL worker:worker .


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
        ruby-hocon \
        xmlstarlet \
        systemctl \
        wait-for-it \
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
        postgresql-${postgresql.version.major} \
    && pg_dropcluster ${postgresql.version.major} main \
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
    && wget -q https://dlcdn.apache.org/tomcat/tomcat-${tomcat.version.major}/v${tomcat.version}/bin/apache-tomcat-${tomcat.version}.tar.gz \
    && mkdir -p tomcat \
    && tar -xzvf apache-tomcat-${tomcat.version}.tar.gz -C tomcat --strip 1 \
    && rm apache-tomcat-${tomcat.version}.tar.gz

RUN set -eux \
    && wget -q https://download.alfresco.com/release/community/201707-build-00028/alfresco-community-installer-201707-linux-x64.bin \
    && chmod +x alfresco-community-installer-201707-linux-x64.bin \
    && mkdir -p tomcat/webapps \
    && ./alfresco-community-installer-201707-linux-x64.bin \
      --mode unattended \
      --enable-components alfrescosolr4 \
      --disable-components javaalfresco,postgres,libreofficecomponent,alfrescosolr,aosmodule,alfrescowcmqs,alfrescogoogledocs \
      --prefix $ALF_HOME \
      --jdbc_url jdbc:postgresql://127.0.0.1/repository \
      --postgres_port 5432 \
      --jdbc_username repository \
      --jdbc_password repository \
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

RUN set -eux \
    && apt-get update -y \
    && apt-get install -y --no-install-recommends \
      apache2 \
    && a2enmod proxy \
    && a2enmod proxy_ajp \
    && a2enmod proxy_http \
    && apt-get clean \
    && rm -r /var/lib/apt/lists/*

COPY assets/apache2/ports.conf /etc/apache2/ports.conf
COPY assets/apache2/sites/* /etc/apache2/sites-available/

COPY assets/postgresql/ ./postgresql/
COPY assets/libreoffice/ ./libreoffice/
COPY assets/tomcat/ ./tomcat/

RUN set -eux \
    && sed -i -r \
        's|^[#]*\s*shared\.loader=.*|shared.loader=${catalina.base}/shared/classes,${catalina.base}/shared/lib/*.jar|' \
        tomcat/conf/catalina.properties \
    && xmlstarlet ed -L \
        -i '/Server/Service[@name="Catalina"]/Connector[@port="8009"]' -t attr -n 'secretRequired' -v 'false' \
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
        tomcat/conf/Catalina/localhost/share.xml

RUN set -eux \
    && mkdir -p alf_data/postgresql postgresql \
    && pg_createcluster \
      --datadir=$ALF_HOME/alf_data/postgresql \
      --logfile=$ALF_HOME/postgresql/postgresql.log \
      --encoding UTF8 \
      --port=5432 \
      --start \
      --start-conf=manual \
      ${postgresql.version.major} repository \
    && su -c 'psql -f postgresql/conf/init_db.sql' postgres \
    && pg_ctlcluster ${postgresql.version.major} repository stop \
    && rm tomcat/lib/postgresql-* \
    && wget -q https://jdbc.postgresql.org/download/postgresql-42.3.1.jar \
    && mv postgresql-42.3.1.jar tomcat/lib \
    && sed -i -r \
        "s|^[#]*\s*listen_addresses\s*=\s*.*|listen_addresses = '\*'                 # what IP address(es) to listen on;|" \
        /etc/postgresql/${postgresql.version.major}/repository/postgresql.conf \
    && sed -i -r \
        's|host    all             all             127.0.0.1/32            md5|host    all             all             0.0.0.0/0            md5|' \
        /etc/postgresql/${postgresql.version.major}/repository/pg_hba.conf \
    && sed -i -r \
        's|host    all             all             ::1/128                 md5|host    all             all             ::0/0                 md5|' \
        /etc/postgresql/${postgresql.version.major}/repository/pg_hba.conf

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

########################################################################################################################

RUN set -eux \
    && wget -qO - https://artifacts.elastic.co/GPG-KEY-elasticsearch | apt-key add - \
    && echo "deb https://artifacts.elastic.co/packages/7.x/apt stable main" | tee /etc/apt/sources.list.d/elastic-7.x.list \
    && apt-get update -y \
    && apt-get install -y --no-install-recommends \
        elasticsearch \
    && mkdir /var/run/elasticsearch \
    && chown elasticsearch:elasticsearch -R /var/run/elasticsearch \
    && chown elasticsearch:elasticsearch -R /usr/share/elasticsearch \
    && chown elasticsearch:elasticsearch -R /var/log/elasticsearch \
    && chown elasticsearch:elasticsearch -R /var/lib/elasticsearch \
    && chown elasticsearch:elasticsearch -R /etc/default/elasticsearch \
    && chown elasticsearch:elasticsearch -R /etc/elasticsearch \
    && apt-get clean \
    && rm -r /var/lib/apt/lists/*

########################################################################################################################

COPY install.sh .env.base bin/installer/
COPY assets/entrypoint.sh bin/
COPY assets/settings.xml /root/.m2/

########################################################################################################################

COPY plugins/docker/ bin/plugins/
COPY plugins/installer bin/installer/plugins

########################################################################################################################

# THANK YOU BILL ;)
RUN set -eux \
    && find . -type f -name "*.sh" -exec chmod +x {} \;

ENTRYPOINT ["bin/entrypoint.sh"]

EXPOSE 80 8080

#VOLUME alf_data

LABEL git.branch=${git.branch}
LABEL git.closest.tag.name=${git.closest.tag.fixed}
LABEL git.commit.id=${git.commit.id}
LABEL git.dirty=${git.dirty}
LABEL mvn.project.artifactId=${project.artifactId}
LABEL mvn.project.groupId=${project.groupId}
LABEL mvn.project.version=${project.version}

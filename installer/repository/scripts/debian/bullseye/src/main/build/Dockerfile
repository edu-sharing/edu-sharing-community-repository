########################################################################################################################
#                                                                                                                      #
#         !!!         THIS DOCKER IS FOR TESTING ONLY PURPOSES AND NOT FOR PRODUCTIVE DEPLOYMENT         !!!           #
#                                                                                                                      #
########################################################################################################################

FROM docker.io/${os.distribution}:${os.codename}

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
        apt-utils \
        ca-certificates \
        cron \
        curl \
        dirmngr \
        dnsutils \
        gnupg \
        jq \
        lsb-release \
        lsof \
        nano \
        net-tools \
        postgresql-client \
        procps \
        ruby-hocon \
        systemctl \
        unzip \
        vim \
        wait-for-it \
        wget \
        xmlstarlet \
    && apt-get clean \
    && rm -r /var/lib/apt/lists/*

########################################################################################################################

RUN set -eux \
    && wget -q https://adoptopenjdk.jfrog.io/adoptopenjdk/api/gpg/key/public \
    && gpg --no-default-keyring --keyring ./adoptopenjdk-keyring.gpg --import public \
    && gpg --no-default-keyring --keyring ./adoptopenjdk-keyring.gpg --export --output adoptopenjdk-archive-keyring.gpg \
    && mv adoptopenjdk-archive-keyring.gpg /usr/share/keyrings \
    && rm adoptopenjdk-keyring.gpg public \
    && echo "deb [signed-by=/usr/share/keyrings/adoptopenjdk-archive-keyring.gpg] https://artifacts.edu-sharing.com/repository/adoptopenjdk `lsb_release -cs` main" | tee /etc/apt/sources.list.d/adoptopenjdk.list \
    && apt-get update -y \
    && apt-get install -y --no-install-recommends \
        adoptopenjdk-8-hotspot \
    && update-java-alternatives -s adoptopenjdk-8-hotspot-amd64 \
    && apt-get clean \
    && rm -r /var/lib/apt/lists/*

RUN set -eux \
    && apt-get update -y \
    && apt-get install -y --no-install-recommends \
	    maven=3.6.3-* \
    && apt-get clean \
    && rm -r /var/lib/apt/lists/*

########################################################################################################################

RUN set -eux \
    && apt-get update -y \
    && apt-get install -y --no-install-recommends \
        exiftool \
        ffmpeg \
        ghostscript \
        gsfonts \
        imagemagick \
    && apt-get clean \
    && rm -r /var/lib/apt/lists/*

########################################################################################################################

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

########################################################################################################################

ENV ALF_HOME /opt/alfresco
ENV PATH $ALF_HOME/bin:$PATH

WORKDIR $ALF_HOME

########################################################################################################################

RUN set -eux \
    && wget -q https://download.alfresco.com/release/community/201707-build-00028/alfresco-community-installer-201707-linux-x64.bin \
    && chmod +x alfresco-community-installer-201707-linux-x64.bin \
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

COPY assets/share tomcat/webapps/share/WEB-INF/classes/
COPY assets/solr4 solr4/
COPY assets/tomcat tomcat/

########################################################################################################################

RUN set -eux \
    && apt-get update -y \
    && apt-get install -y --no-install-recommends \
        libreoffice \
    && apt-get clean \
    && rm -r /var/lib/apt/lists/*

COPY assets/libreoffice libreoffice/

########################################################################################################################

RUN set -eux \
    && wget -q -O - https://www.postgresql.org/media/keys/ACCC4CF8.asc | apt-key add - \
    && echo "deb https://artifacts.edu-sharing.com/repository/postgresql `lsb_release -cs`-pgdg main" | tee  /etc/apt/sources.list.d/postgresql.list \
    && apt-get update -y \
    && apt-get install -y --no-install-recommends \
        postgresql-${postgresql.version.major}=${postgresql.version.major}.${postgresql.version.minor}* \
    && pg_dropcluster ${postgresql.version.major} main \
    && apt-get clean \
    && rm -r /var/lib/apt/lists/*

COPY assets/postgresql postgresql/

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
    && sed -i -r \
        "s|^[#]*\s*listen_addresses.*|listen_addresses = '\*'|" \
        /etc/postgresql/${postgresql.version.major}/repository/postgresql.conf \
    && sed -i -r 's|127.0.0.1/32|0.0.0.0/0|' \
        /etc/postgresql/${postgresql.version.major}/repository/pg_hba.conf \
    && sed -i -r 's|::1/128|::0/0|' \
        /etc/postgresql/${postgresql.version.major}/repository/pg_hba.conf

########################################################################################################################

COPY scripts/install.sh scripts/.env.base bin/installer/
COPY scripts/plugins/installer bin/installer/plugins/

COPY assets/entrypoint.sh bin/
COPY scripts/plugins/docker bin/plugins/

COPY assets/settings.xml /root/.m2/

RUN set -eux \
	&& find bin -type f -name '*.sh' -exec chmod +x {} \;

########################################################################################################################

ENTRYPOINT ["bin/entrypoint.sh"]

EXPOSE 80 8080

########################################################################################################################

LABEL git.branch=${git.branch}
LABEL git.closest.tag.name=${git.closest.tag.fixed}
LABEL git.commit.id=${git.commit.id}
LABEL git.dirty=${git.dirty}
LABEL mvn.project.artifactId=${project.artifactId}
LABEL mvn.project.groupId=${project.groupId}
LABEL mvn.project.version=${project.version}

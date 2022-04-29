########################################################################################################################
#                                                                                                                      #
#         !!!         THIS DOCKER IS FOR TESTING ONLY PURPOSES AND NOT FOR PRODUCTIVE DEPLOYMENT         !!!           #
#                                                                                                                      #
########################################################################################################################

FROM ${docker.registry}/mirror/docker.io/${os.distribution}:${os.codename}

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
    && a2enmod headers \
    && a2enmod rewrite \
    && apt-get clean \
    && rm -r /var/lib/apt/lists/*

COPY assets/apache2/ports.conf /etc/apache2/ports.conf
COPY assets/apache2/sites/* /etc/apache2/sites-available/

########################################################################################################################

ENV PHP_VERSION ${php.version.major}.${php.version.minor}

RUN set -eux \
    && curl -sL https://packages.sury.org/php/apt.gpg -o /etc/apt/trusted.gpg.d/php.gpg \
    && echo "deb https://artifacts.edu-sharing.com/repository/php $(lsb_release -sc) main" > /etc/apt/sources.list.d/php.list \
    && apt-get update -y \
    && apt-get install -y --no-install-recommends \
      php${PHP_VERSION}=${PHP_VERSION}* \
      php${PHP_VERSION}-common=${PHP_VERSION}* \
      php${PHP_VERSION}-curl=${PHP_VERSION}* \
      php${PHP_VERSION}-dom \
      php${PHP_VERSION}-exif \
      php${PHP_VERSION}-gd=${PHP_VERSION}* \
      php${PHP_VERSION}-mbstring=${PHP_VERSION}* \
      php${PHP_VERSION}-pdo \
      php${PHP_VERSION}-pdo-pgsql \
      php${PHP_VERSION}-pgsql=${PHP_VERSION}* \
      php${PHP_VERSION}-soap=${PHP_VERSION}* \
      php${PHP_VERSION}-xml=${PHP_VERSION}* \
      php${PHP_VERSION}-zip=${PHP_VERSION}* \
    && apt-get clean \
    && rm -r /var/lib/apt/lists/*

########################################################################################################################

ENV EDU_HOME /opt/edu-sharing
ENV PATH $EDU_HOME/bin:$PATH

WORKDIR $EDU_HOME

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

COPY assets/postgresql/ postgresql/

RUN set -eux \
    && mkdir -p postgresql/data postgresql/logs \
    && pg_createcluster \
      --datadir=$EDU_HOME/postgresql/data \
      --logfile=$EDU_HOME/postgresql/logs/postgresql.log \
      --encoding UTF8 \
      --port=5432 \
      --start \
      --start-conf=manual \
      ${postgresql.version.major} rendering \
    && su -c 'psql -f postgresql/conf/init_db.sql' postgres \
    && pg_ctlcluster ${postgresql.version.major} rendering stop \
    && sed -i -r \
        "s|^[#]*\s*listen_addresses.*|listen_addresses = '\*'|" \
        /etc/postgresql/${postgresql.version.major}/rendering/postgresql.conf \
    && sed -i -r 's|127.0.0.1/32|0.0.0.0/0|' \
        /etc/postgresql/${postgresql.version.major}/rendering/pg_hba.conf \
    && sed -i -r 's|::1/128|::0/0|' \
        /etc/postgresql/${postgresql.version.major}/rendering/pg_hba.conf

########################################################################################################################

COPY artifact.sh install.sh .env.base ./

COPY assets/entrypoint.sh assets/apache2-foreground.sh ./

COPY assets/settings.xml /root/.m2/

RUN set -eux \
	&& find . -type f -name '*.sh' -exec chmod +x {} \;

########################################################################################################################

ENV WWW_ROOT /var/www/html/
ENV RS_ROOT /var/www/html/esrender
ENV RS_CACHE /var/cache/esrender

ENTRYPOINT ["./entrypoint.sh"]

EXPOSE 8080

# https://httpd.apache.org/docs/2.4/stopping.html#gracefulstop
STOPSIGNAL SIGWINCH

########################################################################################################################

LABEL git.branch=${git.branch}
LABEL git.closest.tag.name=${git.closest.tag.fixed}
LABEL git.commit.id=${git.commit.id}
LABEL git.dirty=${git.dirty}
LABEL mvn.project.artifactId=${project.artifactId}
LABEL mvn.project.groupId=${project.groupId}
LABEL mvn.project.version=${project.version}

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
        apt-utils \
        apt-transport-https \
        ca-certificates \
        gnupg \
        lsb-release \
        procps \
        unzip \
        wget \
        xmlstarlet \
        curl \
        dirmngr \
        postgresql-client \
        jq \
        wait-for-it \
    && apt-get clean \
    && rm -r /var/lib/apt/lists/*

########################################################################################################################

ENV ALF_HOME /opt/alfresco
WORKDIR $ALF_HOME

COPY assets/postgresql/ postgresql/

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
    && mkdir -p alf_data/postgresql postgresql \
    && pg_createcluster \
      --datadir=$ALF_HOME/alf_data/postgresql \
      --logfile=$ALF_HOME/postgresql/postgresql.log \
      --encoding UTF8 \
      --port=5432 \
      --start \
      --start-conf=manual \
      ${postgresql.version.major} rendering \
    && su -c 'psql -f postgresql/conf/init_db.sql' postgres \
    && sed -i -r \
        "s|^[#]*\s*listen_addresses\s*=\s*.*|listen_addresses = '\*'                 # what IP address(es) to listen on;|" \
        /etc/postgresql/${postgresql.version.major}/rendering/postgresql.conf \
    && sed -i -r \
        's|host    all             all             127.0.0.1/32            md5|host    all             all             0.0.0.0/0            md5|' \
        /etc/postgresql/${postgresql.version.major}/rendering/pg_hba.conf \
    && sed -i -r \
        's|host    all             all             ::1/128                 md5|host    all             all             ::0/0                 md5|' \
        /etc/postgresql/${postgresql.version.major}/rendering/pg_hba.conf

########################################################################################################################

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
    && apt-get update -y \
    && apt-get install -y --no-install-recommends \
	    maven=3.6.3-* \
    && apt-get clean \
    && rm -r /var/lib/apt/lists/*

########################################################################################################################

ENV WWW_ROOT /var/www/html/
ENV RS_ROOT /var/www/html/esrender
ENV RS_CACHE /var/cache/esrender
ENV PHP_VERSION ${php.version.major}.${php.version.minor}

WORKDIR $WWW_ROOT

RUN set -eux \
    && curl -sL https://packages.sury.org/php/apt.gpg -o /etc/apt/trusted.gpg.d/php.gpg \
    && echo "deb https://packages.sury.org/php/ $(lsb_release -sc) main" > /etc/apt/sources.list.d/php.list \
    && apt-get update -y \
    && apt-get install -y --no-install-recommends \
      jq \
      imagemagick \
      ghostscript \
      php${PHP_VERSION} \
      php${PHP_VERSION}-dom \
      php${PHP_VERSION}-soap \
      php${PHP_VERSION}-common \
      php${PHP_VERSION}-gd \
      php${PHP_VERSION}-mbstring \
      php${PHP_VERSION}-zip \
      php${PHP_VERSION}-curl \
      php${PHP_VERSION}-exif \
      php${PHP_VERSION}-pgsql \
	  php-xdebug \
      apache2 \
      ffmpeg \
      cron \
    && a2enmod rewrite \
    && a2enmod headers \
    && apt-get clean \
    && rm -r /var/lib/apt/lists/*

#      php${PHP_VERSION}-session \
#      php${PHP_VERSION}-openssl \
#      php${PHP_VERSION}-zlib \

#        php${PHP_VERSION}-common -> instead of:
#        php${PHP_VERSION}-sockets \
#        php${PHP_VERSION}-fileinfo \
#        php${PHP_VERSION}-iconv \
#
#        extension not available!!!
#        php${PHP_VERSION}-wddx \
#      ncdu \
#      tzdata \

WORKDIR /opt/rendering

COPY install.sh ./
COPY .env.base ./
COPY assets/.env ./

COPY assets/apache2/ports.conf /etc/apache2/ports.conf
COPY assets/apache2/sites/* /etc/apache2/sites-available/

COPY assets/rendering/de.metaventis.esrender.log4php.properties $RS_ROOT/conf/
COPY assets/rendering/de.metaventis.esrender.log4php.cachecleaner.properties $RS_ROOT/install/_tmpl/conf/

COPY assets/entrypoint.sh ./
COPY assets/apache2-foreground ./

########################################################################################################################

# https://httpd.apache.org/docs/2.4/stopping.html#gracefulstop
STOPSIGNAL SIGWINCH

EXPOSE 8080

ENTRYPOINT ["./entrypoint.sh"]

########################################################################################################################

LABEL git.branch=${git.branch}
LABEL git.closest.tag.name=${git.closest.tag.fixed}
LABEL git.commit.id=${git.commit.id}
LABEL git.dirty=${git.dirty}
LABEL mvn.project.artifactId=${project.artifactId}
LABEL mvn.project.groupId=${project.groupId}
LABEL mvn.project.version=${project.version}

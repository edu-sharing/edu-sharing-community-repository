FROM ${docker.from.apache.server}

########################################################################################################################

RUN set -eux \
	&& apt-get update -y \
	&& apt-get install -y --no-install-recommends \
        ca-certificates \
        curl \
        exiftool \
        ffmpeg \
        jq \
        libfreetype6-dev \
        libjpeg62-turbo-dev \
        libpng-dev \
        libpq-dev \
        libxml2-dev \
        libzip-dev \
        mariadb-client \
        postgresql-client \
        redis-tools \
        wait-for-it \
        xmlstarlet \
        zip \
    && docker-php-ext-install -j$(nproc) soap \
    && docker-php-ext-configure gd --with-freetype=/usr/include/ --with-jpeg=/usr/include/ \
    && docker-php-ext-install -j$(nproc) gd \
    && docker-php-ext-configure exif \
    && docker-php-ext-install -j$(nproc) exif \
    && docker-php-ext-configure zip \
    && docker-php-ext-install -j$(nproc) zip \
    && docker-php-ext-install -j$(nproc) sockets \
    && docker-php-ext-install -j$(nproc) mysqli \
    && docker-php-ext-install -j$(nproc) pdo pdo_mysql pdo_pgsql \
    && docker-php-ext-enable opcache \
    && a2enmod rewrite \
    && a2enmod headers \
    && ln -s $PHP_INI_DIR/php.ini-production $PHP_INI_DIR/php.ini \
    && apt-get clean \
    && rm -r /var/lib/apt/lists/*

RUN set -eux \
    && curl -L -o /tmp/phpredis.tar.gz https://github.com/phpredis/phpredis/archive/${phpredis.version}.tar.gz \
    && mkdir -p /usr/src/php/ext/redis \
    && tar -C /usr/src/php/ext/redis -xzf /tmp/phpredis.tar.gz --strip 1 \
    && rm -r /tmp/phpredis.tar.gz \
    && docker-php-ext-install redis

########################################################################################################################

ENV RS_ROOT /var/www/html/esrender
ENV RS_CACHE /var/cache/esrender

WORKDIR $RS_ROOT

########################################################################################################################

COPY artifacts/edu_sharing-community-services-rendering-${org.edu_sharing:edu_sharing-community-services-rendering:tar.gz:bin.version}-bin.tar.gz /tmp/edu_sharing-community-services-rendering.tar.gz

RUN set -eux \
	&& tar -xzvf /tmp/edu_sharing-community-services-rendering.tar.gz --exclude './vendor/lib/converter' \
	&& rm -rf /tmp/edu_sharing-community-services-rendering.tar.gz

COPY assets/apache2/ports.conf /etc/apache2/ports.conf
COPY assets/apache2/sites/* /etc/apache2/sites-available/

RUN set -eux \
	&& a2dissite 000-default \
	&& a2ensite external \
	&& a2ensite internal

COPY assets/rendering/de.metaventis.esrender.log4php.properties conf/
COPY assets/rendering/de.metaventis.esrender.log4php.cachecleaner.properties conf/

########################################################################################################################

COPY assets/entrypoint.sh /usr/local/bin/
COPY assets/init.sh /usr/local/bin/

RUN set -eux \
	&& find /usr/local/bin -type f -name '*.sh' -exec chmod +x {} \;

########################################################################################################################

RUN set -eux \
    && addgroup --gid 1000 appuser \
    && adduser --uid 1000 --gid 1000 --disabled-password appuser \
    && adduser www-data appuser

RUN set -eux \
    && mkdir -p $RS_CACHE/config \
    && mkdir -p $RS_CACHE/data \
	&& chown -R appuser:appuser /etc/apache2 $RS_ROOT $RS_CACHE $PHP_INI_DIR

USER appuser

########################################################################################################################

EXPOSE 8080 8081

ENTRYPOINT ["entrypoint.sh"]
CMD ["apache2-foreground"]

VOLUME [ "/var/cache/esrender/config", "/var/cache/esrender/data" ]

########################################################################################################################

LABEL git.branch=${git.branch}
LABEL git.closest.tag.name=${git.closest.tag.fixed}
LABEL git.commit.id=${git.commit.id}
LABEL git.dirty=${git.dirty}
LABEL mvn.project.artifactId=${project.artifactId}
LABEL mvn.project.groupId=${project.groupId}
LABEL mvn.project.version=${project.version}

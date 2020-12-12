FROM docker.io/php:${rendering.php.version}-apache

########################################################################################################################

RUN set -eux \
    && echo 'deb http://deb.debian.org/debian buster-backports main' >> /etc/apt/sources.list \
	&& apt-get update -y \
	&& apt-get install -y --no-install-recommends \
        redis-tools \
	&& apt-get install -y --no-install-recommends -t buster-backports \
        ca-certificates \
        curl \
        exiftool \
        ffmpeg \
        jq \
        libfreetype6-dev \
        libjpeg62-turbo-dev \
        libpng-dev \
        libxml2-dev \
        libzip-dev \
        mariadb-client \
        wait-for-it \
        xmlstarlet \
    && docker-php-ext-configure gd --with-freetype-dir=/usr/include/ --with-jpeg-dir=/usr/include/ \
    && docker-php-ext-install -j$(nproc) gd \
    && docker-php-ext-configure exif \
    && docker-php-ext-install -j$(nproc) exif \
    && docker-php-ext-install -j$(nproc) soap \
    && docker-php-ext-install -j$(nproc) wddx \
    && docker-php-ext-configure zip --with-libzip \
    && docker-php-ext-install -j$(nproc) zip \
    && docker-php-ext-install -j$(nproc) sockets \
    && docker-php-ext-install -j$(nproc) mysqli \
    && docker-php-ext-install -j$(nproc) pdo pdo_mysql \
    && docker-php-ext-enable opcache \
    && a2enmod rewrite \
    && a2enmod headers \
    && ln -s $PHP_INI_DIR/php.ini-production $PHP_INI_DIR/php.ini \
    && apt-get clean \
    && rm -r /var/lib/apt/lists/*

RUN set -eux \
    && curl -L -o /tmp/phpredis.tar.gz https://github.com/phpredis/phpredis/archive/${rendering.redis.client.version}.tar.gz \
    && mkdir -p /usr/src/php/ext/redis \
    && tar -C /usr/src/php/ext/redis -xzf /tmp/phpredis.tar.gz --strip 1 \
    && rm -r /tmp/phpredis.tar.gz \
    && docker-php-ext-install redis

########################################################################################################################

ENV RS_ROOT /var/www/html/esrender
ENV RS_CACHE /var/cache/esrender

WORKDIR $RS_ROOT

########################################################################################################################

COPY artifacts/edu_sharing-community-rendering-${community.rendering.version}-bin.tar.gz /tmp/edu_sharing-community-rendering.tar.gz

RUN set -eux \
	&& mkdir -p $RS_ROOT $RS_CACHE \
	&& tar -xzvf /tmp/edu_sharing-community-rendering.tar.gz --exclude './vendor/lib/converter' \
	&& rm -rf /tmp/edu_sharing-community-rendering.tar.gz

COPY assets/apache2/ports.conf /etc/apache2/ports.conf
COPY assets/apache2/sites/* /etc/apache2/sites-available/

RUN set -eux \
	&& a2dissite 000-default \
	&& a2ensite external \
	&& a2ensite internal

COPY assets/rendering/de.metaventis.esrender.log4php.properties conf/
COPY assets/rendering/de.metaventis.esrender.log4php.cachecleaner.properties install/_tmpl/conf/

RUN set -eux \
    && mkdir -p $RS_CACHE \
	&& chown -R www-data:www-data $RS_ROOT $RS_CACHE

########################################################################################################################

COPY assets/entrypoint.sh /usr/local/bin/
COPY assets/init.sh /usr/local/bin/

RUN set -eux \
    && chmod +x /usr/local/bin/entrypoint.sh \
    && chmod +x /usr/local/bin/init.sh

########################################################################################################################

EXPOSE 8080 8081

ENTRYPOINT ["entrypoint.sh"]
CMD ["apache2-foreground"]

########################################################################################################################

LABEL git.branch=${git.branch}
LABEL git.closest.tag.name=${git.closest.tag.name}
LABEL git.commit.id=${git.commit.id}
LABEL git.dirty=${git.dirty}
LABEL mvn.project.artifactId=${project.artifactId}
LABEL mvn.project.groupId=${project.groupId}
LABEL mvn.project.version=${project.version}

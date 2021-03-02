FROM ${docker.from.openjdk}

########################################################################################################################

RUN set -eux \
    && echo 'deb http://deb.debian.org/debian buster-backports main' >> /etc/apt/sources.list \
	&& apt-get update -y \
	&& apt-get install -y --no-install-recommends -t buster-backports \
		fonts-dejavu \
		fonts-liberation \
		hyphen-* \
		jq \
		libreoffice-core \
		libreoffice-writer \
		libreoffice-calc \
		libreoffice-draw \
		libreoffice-impress \
		libreoffice-base \
		unoconv \
		ure \
		wait-for-it \
        xmlstarlet \
	&& apt-get clean \
	&& rm -r /var/lib/apt/lists/*

########################################################################################################################

ENV ALF_HOME /opt/alfresco
ENV PATH $ALF_HOME/bin:$PATH

WORKDIR $ALF_HOME

### Libreoffice Headless ###############################################################################################

RUN set -eux \
    && sed -i -r 's|^[#]*\s*CrashDumpEnable=.*|CrashDumpEnable=false|' /etc/libreoffice/sofficerc \
    && sed -i -r 's|^[#]*\s*HideEula=.*|HideEula=1|' /etc/libreoffice/sofficerc \
    && sed -i -r 's|^[#]*\s*Logo=.*|Logo=0|' /etc/libreoffice/sofficerc

########################################################################################################################

COPY assets/entrypoint*.sh bin/

RUN set -eux \
    && chmod +x bin/entrypoint*.sh

########################################################################################################################

RUN set -eux \
	&& adduser --uid=1000 --home=$ALF_HOME --disabled-password --gecos "" --shell=/bin/bash worker \
    && chown -RL worker:worker .

USER worker

########################################################################################################################

EXPOSE 8100

ENTRYPOINT ["entrypoint.sh"]

########################################################################################################################

LABEL git.branch=${git.branch}
LABEL git.closest.tag.name=${git.closest.tag.name}
LABEL git.commit.id=${git.commit.id}
LABEL git.dirty=${git.dirty}
LABEL mvn.project.artifactId=${project.artifactId}
LABEL mvn.project.groupId=${project.groupId}
LABEL mvn.project.version=${project.version}

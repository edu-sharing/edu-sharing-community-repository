#!/bin/bash
set -eu

########################################################################################################################

my_bind="${REPOSITORY_TRANSFORM_BIND:-"0"}"

########################################################################################################################

exec libreoffice \
  --nologo --norestore --invisible --headless \
  "--accept=socket,host=${my_bind},port=8100,tcpNoDelay=1;urp;"

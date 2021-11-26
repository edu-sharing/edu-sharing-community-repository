#!/bin/sh
set -eux

echo $PWD
pg_ctlcluster 13 rendering start
#apache2ctl start

# first arg is `-f` or `--some-option`
if [ "${1#-}" != "$1" ]; then
	set -- /usr/local/bin/apache2-foreground "$@"
fi

exec "$@"
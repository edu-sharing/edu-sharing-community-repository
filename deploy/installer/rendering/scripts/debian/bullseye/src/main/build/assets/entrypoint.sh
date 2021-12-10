#!/bin/sh
set -eux

pg_ctlcluster 13 rendering start

apache2ctl start

./install.sh -f .env $@
./install.sh -f .env $@

apache2ctl stop

exec ./apache2-foreground

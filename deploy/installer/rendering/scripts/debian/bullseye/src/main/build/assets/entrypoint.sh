#!/bin/sh
set -eux

pg_ctlcluster 13 rendering start


apache2ctl start

./install.sh -f .env "$@"
./install.sh -f .env "$@"

apache2ctl stop

while wait-for-it "localhost:80" -t 3; do sleep 1; done

exec ./apache2-foreground

#!/bin/sh
SERVICE='soffice'
OOPATH='/srv/openoffice/'

if ps ax | grep -v grep | grep $SERVICE > /dev/null
then
    echo `date` "$SERVICE service running, everything is fine" > /dev/null
else
    echo `date` "$SERVICE is not running, try to restart" >> "$OOPATH"check_ooruns.log
    "$OOPATH"scripts/ctl.sh start
fi
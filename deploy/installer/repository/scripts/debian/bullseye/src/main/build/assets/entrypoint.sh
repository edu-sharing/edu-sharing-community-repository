#!/bin/bash

./postgresql/scripts/ctl.sh start
./libreoffice/scripts/ctl.sh start
#./tomcat/scripts/ctl.sh daemon
./tomcat/scripts/ctl.sh start
apache2ctl start

tail -f /opt/alfresco/tomcat/logs/catalina.out
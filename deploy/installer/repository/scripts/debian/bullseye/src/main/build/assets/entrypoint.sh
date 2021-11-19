#!/bin/bash
    
./postgresql/scripts/ctl.sh start
./libreoffice/scripts/ctl.sh start
./tomcat/scripts/ctl.sh daemon

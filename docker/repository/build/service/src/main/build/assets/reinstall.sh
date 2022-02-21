#!/bin/bash
set -eux

########################################################################################################################

if [[ -d amps/alfresco/0 ]]  ; then
  java -jar bin/alfresco-mmt.jar install amps/alfresco/0 tomcat/webapps/alfresco -directory -nobackup -force
fi

if [[ -d amps/alfresco/1 ]] ; then
	java -jar bin/alfresco-mmt.jar install amps/alfresco/1 tomcat/webapps/alfresco -directory -nobackup -force
fi

if [[ -d amps/edu-sharing/1 ]] ; then
	java -jar bin/alfresco-mmt.jar install amps/edu-sharing/1 tomcat/webapps/edu-sharing -directory -nobackup -force
fi

libcheck.sh
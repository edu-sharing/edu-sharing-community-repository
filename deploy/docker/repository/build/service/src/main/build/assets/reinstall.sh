#!/bin/bash
set -eux

########################################################################################################################

if [[ -d amps/alfresco/1 ]] ; then
	java -jar bin/alfresco-mmt.jar install amps/alfresco/1 tomcat/webapps/alfresco -directory -nobackup -force
fi

if [[ -d amps/alfresco/2 ]] ; then
	java -jar bin/alfresco-mmt.jar install amps/alfresco/2 tomcat/webapps/alfresco -directory -nobackup -force
fi

if [[ -d amps/alfresco/3 ]]  ; then
  java -jar bin/alfresco-mmt.jar install amps/alfresco/3 tomcat/webapps/alfresco -directory -nobackup -force
fi

if [[ -d amps/edu-sharing/1 ]] ; then
	java -jar bin/alfresco-mmt.jar install amps/edu-sharing/1 tomcat/webapps/edu-sharing -directory -nobackup -force
fi

libcheck.sh
#!/bin/bash
[[ -n $DEBUG ]] && set -x
set -eu

########################################################################################################################

if [[ -d amps/alfresco/1 ]] ; then
  echo "install primary alfresco plugins"
  ls -1 amps/alfresco/1
	java -jar bin/alfresco-mmt.jar install amps/alfresco/1 tomcat/webapps/alfresco -directory -nobackup -force
fi

if [[ -d amps/alfresco/2 ]] ; then
  echo "install secondary alfresco plugins:"
  ls -1 amps/alfresco/2
	java -jar bin/alfresco-mmt.jar install amps/alfresco/2 tomcat/webapps/alfresco -directory -nobackup -force
fi

if [[ -d amps/alfresco/3 ]]  ; then
  echo "install tertiary alfresco plugins:"
  ls -1 amps/alfresco/3
  java -jar bin/alfresco-mmt.jar install amps/alfresco/3 tomcat/webapps/alfresco -directory -nobackup -force
fi

if [[ -d amps/edu-sharing/1 ]] ; then
  echo "install primary edu-sharing plugins:"
  ls -1 amps/edu-sharing/1
	java -jar bin/alfresco-mmt.jar install amps/edu-sharing/1 tomcat/webapps/edu-sharing -directory -nobackup -force
fi

libcheck.sh
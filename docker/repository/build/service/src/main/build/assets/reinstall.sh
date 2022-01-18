#!/bin/bash
set -eux

########################################################################################################################

[[ -d amps/alfresco/0 ]] && {
	java -jar bin/alfresco-mmt.jar install amps/alfresco/0 tomcat/webapps/alfresco -directory -nobackup -force
}

[[ -d amps/alfresco/1 ]] && {
	java -jar bin/alfresco-mmt.jar install amps/alfresco/1 tomcat/webapps/alfresco -directory -nobackup -force
}

[[ -d amps/edu-sharing/1 ]] && {
	java -jar bin/alfresco-mmt.jar install amps/edu-sharing/1 tomcat/webapps/edu-sharing -directory -nobackup -force
}
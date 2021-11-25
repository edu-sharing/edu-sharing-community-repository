#!/bin/bash
set -e
set -o pipefail

[[ ! -f "${ALF_HOME}/alfresco.sh" ]] && {
	echo ""
	echo "Env ALF_HOME must point to the home directory of your Alfresco Platform!"
	exit
}

pushd "$ALF_HOME"

[[ ! -d tomcat/webapps/alfresco || ! -d tomcat/webapps/solr4 ]] && {
	echo ""
	echo "You must have started the Alfresco Platform at least once before you can run the installation."
	exit
}

[[ "$(./alfresco.sh status tomcat)" != "tomcat not running" ]] && {
	echo ""
	echo "Please stop Tomcat before you can run the installation!"
	exit
}

[[ "$(./alfresco.sh status postgresql)" != "postgresql not running" ]] && {
	echo ""
	echo "Please stop Postgresql before you can run the installation!"
	exit
}

install_edu_sharing() {

	echo "- clean up outdated libraries"
	rm -f tomcat/lib/postgresql-*
	rm -f tomcat/webapps/alfresco/WEB-INF/lib/commons-lang3-*
	rm -f tomcat/webapps/alfresco/WEB-INF/lib/hazelcast-*
	rm -f tomcat/webapps/alfresco/WEB-INF/lib/jackson-*

	echo "- update tomcat env"
	sed -i -r 's|file\.encoding=.*\"|file.encoding=UTF-8 $CATALINA_OPTS \"|' tomcat/bin/setenv.sh
	grep -q 'file\.encoding' tomcat/bin/setenv.sh || echo 'CATALINA_OPTS="-Dfile.encoding=UTF-8 $CATALINA_OPTS "' >> tomcat/bin/setenv.sh
	sed -i -r 's|org\.xml\.sax\.parser=.*\"|org.xml.sax.parser=com.sun.org.apache.xerces.internal.parsers.SAXParser $CATALINA_OPTS \"|' tomcat/bin/setenv.sh
	grep -q 'org\.xml\.sax\.parser' tomcat/bin/setenv.sh || echo 'CATALINA_OPTS="-Dorg.xml.sax.parser=com.sun.org.apache.xerces.internal.parsers.SAXParser $CATALINA_OPTS "' >> tomcat/bin/setenv.sh
	sed -i -r 's|javax\.xml\.parsers\.DocumentBuilderFactory=.*\"|javax.xml.parsers.DocumentBuilderFactory=com.sun.org.apache.xerces.internal.jaxp.DocumentBuilderFactoryImpl $CATALINA_OPTS \"|' tomcat/bin/setenv.sh
	grep -q 'javax\.xml\.parsers\.DocumentBuilderFactory' tomcat/bin/setenv.sh || echo 'CATALINA_OPTS="-Djavax.xml.parsers.DocumentBuilderFactory=com.sun.org.apache.xerces.internal.jaxp.DocumentBuilderFactoryImpl $CATALINA_OPTS "' >> tomcat/bin/setenv.sh
	sed -i -r 's|javax\.xml\.parsers\.SAXParserFactory=.*\"|javax.xml.parsers.SAXParserFactory=com.sun.org.apache.xerces.internal.jaxp.SAXParserFactoryImpl $CATALINA_OPTS \"|' tomcat/bin/setenv.sh
	grep -q 'javax\.xml\.parsers\.SAXParserFactory' tomcat/bin/setenv.sh || echo 'CATALINA_OPTS="-Djavax.xml.parsers.SAXParserFactory=com.sun.org.apache.xerces.internal.jaxp.SAXParserFactoryImpl $CATALINA_OPTS "' >> tomcat/bin/setenv.sh

	echo "- download edu-sharing distribution"
	mvn -q dependency:get \
		-Dartifact=org.edu_sharing:edu_sharing-community-deploy-installer-repository-distribution:${org.edu_sharing:edu_sharing-community-deploy-installer-repository-distribution:tar.gz:bin.version}:tar.gz:bin \
		-DremoteRepositories=myreleases::::https://artifacts.edu-sharing.com/repository/community-releases/,mysnapshots::::https://artifacts.edu-sharing.com/repository/community-snapshots/ \
		-Dtransitive=false

	echo "- unpack edu-sharing distribution"
	mvn -q dependency:copy \
		-Dartifact=org.edu_sharing:edu_sharing-community-deploy-installer-repository-distribution:${org.edu_sharing:edu_sharing-community-deploy-installer-repository-distribution:tar.gz:bin.version}:tar.gz:bin \
		-DoutputDirectory=.

	tar xzf edu_sharing-community-deploy-installer-repository-distribution-${org.edu_sharing:edu_sharing-community-deploy-installer-repository-distribution:tar.gz:bin.version}-bin.tar.gz
	rm edu_sharing-community-deploy-installer-repository-distribution-${org.edu_sharing:edu_sharing-community-deploy-installer-repository-distribution:tar.gz:bin.version}-bin.tar.gz

	echo "- install Alfresco Module Packages"
	if [[ -d amps/alfresco/0 ]]; then
	  java -jar bin/alfresco-mmt.jar install amps/alfresco/0    tomcat/webapps/alfresco    -directory -nobackup -force
	fi

	if [[ -d amps/alfresco/1 ]]; then
	  java -jar bin/alfresco-mmt.jar install amps/alfresco/1    tomcat/webapps/alfresco    -directory -nobackup -force
  fi

  if [[ -d amps/edu-sharing/1 ]]; then
    java -jar bin/alfresco-mmt.jar install amps/edu-sharing/1 tomcat/webapps/edu-sharing -directory -nobackup -force
  fi

}

alfresco_base_snapshot="alfresco-base-SNAPSHOT-DO-NOT-DELETE-ME.tar.gz"
if [[ -f ../$alfresco_base_snapshot ]] ; then

	echo ""
	echo "Update ... "

	echo "- make a snapshot of edu-sharing platform"
  snapshot_name=edu-sharing-SNAPSHOT-$(date "+%Y.%m.%d-%H.%M.%S")".tar.gz"
	tar -czf ../$snapshot_name amps tomcat

	echo "- cleanup amps and tomcat"
  rm -rf amps
  rm -rf tomcat

  echo "- restore amps and tomcat"
  tar -zxf ../$alfresco_base_snapshot

	install_edu_sharing

  echo "- restore persistent data of Alfresco platform"
  if [[ $(tar -tf  ../$snapshot_name | grep 'tomcat/shared/classes/config/persistent' | wc -l) -gt 0 ]]; then
    tar -zxf ../$snapshot_name tomcat/shared/classes/config/persistent -C tomcat/shared/classes/config/
  else
    echo "nothing to restore"
  fi  

  echo "- delete old edu-sharing SNAPSHOTS (keep 3 backups)"
  pushd ..
  ls -pt ../ | grep -v / | grep "edu-sharing-SNAPSHOT" | tail -n +4 | xargs -I {} rm {}
	popd

else

	echo ""
	echo "Install ... "

	echo "- make a snapshot of Alfresco platform"
 	tar -czf ../$alfresco_base_snapshot amps tomcat

  install_edu_sharing

fi

popd

echo "- done."
echo "- you may need to delete the solr4 index, model and content folders!"
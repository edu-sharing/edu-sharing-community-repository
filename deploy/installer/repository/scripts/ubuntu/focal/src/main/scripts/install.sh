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

[[ -f snapshot.tar.gz || -d .git ]] && {
	echo ""
	echo "The installation was already been made!"
	exit
}

echo "- make a snapshot of Alfresco Platform"
tar -czf snapshot.tar.gz alf_data solr4 tomcat

echo "- clean up outdated libraries"
rm -f tomcat/lib/postgresql-*
rm -f tomcat/webapps/alfresco/WEB-INF/lib/commons-lang3-*
rm -f tomcat/webapps/alfresco/WEB-INF/lib/hazelcast-*
rm -f tomcat/webapps/alfresco/WEB-INF/lib/jackson-*

echo "- clean up solr4 index"
rm -rf alf_data/solr4

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
mvn dependency:get \
	-Dartifact=org.edu_sharing:edu_sharing-community-deploy-installer-repository-distribution:maven-develop-SNAPSHOT:tar.gz:bin \
	-DremoteRepositories=myreleases::::https://artifacts.edu-sharing.com/repository/community-releases/,mysnapshots::::https://artifacts.edu-sharing.com/repository/community-snapshots/ \
	-Dtransitive=false

echo "- unpack edu-sharing distribution"
mvn -q dependency:unpack \
	-Dartifact=org.edu_sharing:edu_sharing-community-deploy-installer-repository-distribution:${org.edu_sharing:edu_sharing-community-deploy-installer-repository-distribution:tar.gz:bin.version}:tar.gz:bin \
	-DoutputDirectory=.

echo "- install Alfresco Module Packages"
[[ -d amps/alfresco/0 ]]    && java -jar bin/alfresco-mmt.jar install amps/alfresco/0    tomcat/webapps/alfresco    -directory -force
[[ -d amps/alfresco/1 ]]    && java -jar bin/alfresco-mmt.jar install amps/alfresco/1    tomcat/webapps/alfresco    -directory -force
[[ -d amps/edu-sharing/1 ]] && java -jar bin/alfresco-mmt.jar install amps/edu-sharing/1 tomcat/webapps/edu-sharing -directory -force

echo "- initialize git-repo for custom configuration"
git init
git add tomcat/shared/*
git config --local user.email "you@example.com"
git config --local user.name  "Your name"
git commit -m "After install."
git branch -m master original
git checkout -b custom

popd

echo "- done."

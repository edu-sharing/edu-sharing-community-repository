#!/bin/bash
set -e
set -o pipefail

 [[ -z "${CLI_OPT2}" ]] && {
    echo ""
    echo "Usage: ${CLI_CMD} ${CLI_OPT1} <alfresco directory>"
    exit
  }

  echo "Install ..."
  pushd "${BUILD_PATH}" >/dev/null || exit

  # set the environment variable ALF_HOME
  echo "set the environment variable ALF_HOME"
  export ALF_HOME="${CLI_OPT2}"

  [[ ! -f $ALF_HOME/alfresco.sh ]] && {
    echo ""
    echo "No alfresco found under: ${ALF_HOME}"
    exit
  }

  [[ ! -d $ALF_HOME/tomcat/webapps/alfresco ]] && {
    echo ""
    echo "Alfresco needs to be initialized! Please start alfresco and visit the website. Wait until it is started..."
    exit
  }

  # install maven
  echo "install maven"
  apt install maven -y >/dev/null || exit

  # stop the server
  echo "stop the server"
  pushd $ALF_HOME >/dev/null || exit
  ./alfresco.sh stop >/dev/null || exit
  popd >/dev/null || exit

  # clean up outdated libraries
  echo "clean up outdated libraries"
  rm -f $ALF_HOME/tomcat/lib/postgresql-* >/dev/null || exit
  rm -f $ALF_HOME/tomcat/webapps/alfresco/WEB-INF/lib/commons-lang3-* >/dev/null || exit
  rm -f $ALF_HOME/tomcat/webapps/alfresco/WEB-INF/lib/hazelcast-* >/dev/null || exit
  rm -f $ALF_HOME/tomcat/webapps/alfresco/WEB-INF/lib/jackson-* >/dev/null || exit

  # clean up solr index
  echo "clean up solr index"
  rm -rf $ALF_HOME/alf_data/solr4 >/dev/null || exit

  [[ ! -f $ALF_HOME/snapshot.tar.gz ]] && {
    # adding following lines into $ALF_HOME/tomcat/bin/setenv.sh
    echo "adding following lines into $ALF_HOME/tomcat/bin/setenv.sh"
    echo "CATALINA_OPTS='-Dfile.encoding=UTF-8 $CATALINA_OPTS'" >>$ALF_HOME/tomcat/bin/setenv.sh
    echo "CATALINA_OPTS='-Dorg.xml.sax.parser=com.sun.org.apache.xerces.internal.parsers.SAXParser $CATALINA_OPTS'" >>$ALF_HOME/tomcat/bin/setenv.sh
    echo "CATALINA_OPTS='-Djavax.xml.parsers.DocumentBuilderFactory=com.sun.org.apache.xerces.internal.jaxp.DocumentBuilderFactoryImpl $CATALINA_OPTS'" >>$ALF_HOME/tomcat/bin/setenv.sh
    echo "CATALINA_OPTS='-Djavax.xml.parsers.SAXParserFactory=com.sun.org.apache.xerces.internal.jaxp.SAXParserFactoryImpl $CATALINA_OPTS'" >>$ALF_HOME/tomcat/bin/setenv.sh
    echo "export CATALINA_OPTS" >>$ALF_HOME/tomcat/bin/setenv.sh

    # make an initial snapshot of the tomcat subdirectory
    echo "make an initial snapshot of the tomcat subdirectory"
    pushd $ALF_HOME >/dev/null || exit
    tar -czvf snapshot.tar.gz tomcat >/dev/null || exit
    popd >/dev/null || exit
  }

  # unpack the installer artifact into the home directory of your Alfresco installation
  echo "unpack the installer artifact into the home directory of your Alfresco installation"
  pushd $ALF_HOME >/dev/null || exit
  mvn dependency:copy -Dartifact=org.edu_sharing:edu_sharing-community-deploy-installer-repository-distribution:${project.version}:tar.gz:bin #>/dev/null || exit
  #PROJECT_VERSION=$(mvn -q help:evaluate -Dexpression=project.version -DforceStdout)
  tar -xzvf ${BUILD_PATH}/target/edu_sharing-community-deploy-installer-repository-distribution-${project.version}-bin.tar.gz -C $ALF_HOME >/dev/null || exit
  popd >/dev/null || exit

  # deploy the Alfresco Module Packages (AMP)
  echo "deploy the Alfresco Module Packages (AMP)"
  mkdir -p $ALF_HOME/amps/alfresco/0
  mkdir -p $ALF_HOME/amps/alfresco/1
  mkdir -p $ALF_HOME/amps/edu-sharing/1
  $ALF_HOME/java/bin/java -jar $ALF_HOME/bin/alfresco-mmt.jar install $ALF_HOME/amps/alfresco/0 $ALF_HOME/tomcat/webapps/alfresco -directory -force >/dev/null || exit
  $ALF_HOME/java/bin/java -jar $ALF_HOME/bin/alfresco-mmt.jar install $ALF_HOME/amps/alfresco/1 $ALF_HOME/tomcat/webapps/alfresco -directory -force >/dev/null || exit
  $ALF_HOME/java/bin/java -jar $ALF_HOME/bin/alfresco-mmt.jar install $ALF_HOME/amps/edu-sharing/1 $ALF_HOME/tomcat/webapps/edu-sharing -directory -force >/dev/null || exit

  # initialize version control for your configuration
  echo "initialize version control for your configuration"
  pushd $ALF_HOME
  rm -rf .git/
  git init >/dev/null
  git add tomcat/shared/* >/dev/null || exit
  git commit -m "After install." >/dev/null || exit
  git branch -m master original >/dev/null || exit
  git checkout -b custom >/dev/null || exit
  popd >/dev/null || exit

  # start the server
  echo "start the server"
  pushd $ALF_HOME
  ./alfresco.sh start
  popd
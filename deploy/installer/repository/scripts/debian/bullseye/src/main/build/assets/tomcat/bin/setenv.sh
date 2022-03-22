#!/bin/sh

JAVA_HOME=$(update-java-alternatives -l adoptopenjdk-8-hotspot-amd64 | awk '{print $3}')
export JAVA_HOME

CATALINA_HOME=$ALF_HOME/tomcat
export CATALINA_HOME

CATALINA_OPTS="-server $CATALINA_OPTS "
CATALINA_OPTS="-Xms512M $CATALINA_OPTS "
CATALINA_OPTS="-Xmx4096M $CATALINA_OPTS "
CATALINA_OPTS="-XX:ReservedCodeCacheSize=128m $CATALINA_OPTS "
CATALINA_OPTS="-XX:+DisableExplicitGC $CATALINA_OPTS "
CATALINA_OPTS="-XX:+UseConcMarkSweepGC $CATALINA_OPTS "
CATALINA_OPTS="-XX:+UseParNewGC $CATALINA_OPTS "
CATALINA_OPTS="-Djava.awt.headless=true $CATALINA_OPTS "
export CATALINA_OPTS

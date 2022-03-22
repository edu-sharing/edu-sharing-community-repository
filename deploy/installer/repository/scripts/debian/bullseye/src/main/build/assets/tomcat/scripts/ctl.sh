#!/bin/bash

mkdir -p $ALF_HOME/tomcat/temp
CATALINA_PID=$ALF_HOME/tomcat/temp/catalina.pid
export CATALINA_PID

if [ "x$1" = "xstart" ]; then
  "$ALF_HOME"/tomcat/bin/startup.sh
elif [ "x$1" = "xdaemon" ]; then
  "$ALF_HOME"/tomcat/bin/catalina.sh run
elif [ "x$1" = "xstop" ]; then
  "$ALF_HOME"/tomcat/bin/shutdown.sh 300 -force
elif [ "x$1" = "xstatus" ]; then
	if [ -f $CATALINA_PID ] && kill -0 $(cat $CATALINA_PID) 2>/dev/null ; then
  	echo "tomcat already running"
  else
  	rm $CATALINA_PID 2>/dev/null
  	echo "tomcat not running"
	fi
fi

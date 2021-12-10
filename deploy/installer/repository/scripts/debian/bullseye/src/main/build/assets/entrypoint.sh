#!/bin/bash

# this script is docker specific

trap stop SIGTERM SIGINT SIGQUIT SIGHUP ERR

declare context=$(basename "${PWD}")
declare warnLogPrefix="WARNING  [docker-entrypoint] <$context>"
declare errorLogPrefix="ERROR  [docker-entrypoint] <$context>"
declare debugEnabled="${ENABLE_DEBUG:-false}"
declare -i waitBeforeJavaThreadDump="${WAIT_BEFORE_THREAD_DUMP:-120}"
declare triggerThreadDumpBeforeStop="${TRIGGER_THREAD_DUMP_BEFORE_STOP:-false}"

mkdir -p $ALF_HOME/tomcat/temp
mkdir -p $ALF_HOME/tomcat/logs
CATALINA_OUT=$ALF_HOME/tomcat/logs/catalina.out
CATALINA_PID=$ALF_HOME/tomcat/temp/catalina.pid
export CATALINA_OUT
export CATALINA_PID
export CATALINA_PID

start() {
	./install.sh --all -f .env $@
	./install.sh --all -f .env $@

	./postgresql/scripts/ctl.sh start
	./libreoffice/scripts/ctl.sh start
	systemctl start elasticsearch
	#./tomcat/scripts/ctl.sh daemon

	local catalinaPid

	# hacky way to "initialize" the output file
	touch "$CATALINA_OUT"
	tail --pid $$ -n 0 -F "${CATALINA_OUT}" &
	echo "$(date +'%F %T,%3N') ${warnLogPrefix} Starting tomcat" >>"$CATALINA_OUT"
	touch "${CATALINA_PID}"
	"$ALF_HOME"/tomcat/bin/catalina.sh run >>"${CATALINA_OUT}" 2>&1 &
	catalinaPid=$!
	echo $"$catalinaPid" >"${CATALINA_PID}"

	apache2ctl start

	until wait-for-it "localhost:9200" -t 3; do sleep 1; done

	until [[ $(curl -sSf -w "%{http_code}\n" -o /dev/null "http://localhost:9200/_cluster/health?wait_for_status=yellow&timeout=3s") -eq 200 ]]; do
		echo >&2 "Waiting for elasticsearch ..."
		sleep 3
	done

	until wait-for-it "localhost:8080" -t 3; do sleep 1; done

	until [[ $(curl -sSf -w "%{http_code}\n" -o /dev/null -H 'Accept: application/json' "http://localhost:8080/edu-sharing/rest/_about/status/SERVICE?timeoutSeconds=3") -eq 200 ]]; do
		echo >&2 "Waiting for edu-sharing ..."
		sleep 3
	done

	until [[ $(curl -sSf -w "%{http_code}\n" -o /dev/null -H 'Accept: application/json' "http://localhost:8080/edu-sharing/rest/_about/status/SEARCH?timeoutSeconds=3") -eq 200 ]]; do
		echo >&2 "Waiting for edu-sharing ..."
		sleep 3
	done

	systemctl start elastictracker

	echo "CATALINA_PID: $CATALINA_PID"
	wait "$catalinaPid"

	#https://stackoverflow.com/questions/60676374/unable-to-trap-signals-in-docker-entrypoint-script
}

stop() {

	echo "$(date +'%F %T,%3N') ${warnLogPrefix} Stop apache2"
	apache2ctl stop

	echo ""
	echo "#############################################"
	echo ""
	echo "CATALINA_PID: $CATALINA_PID"
	echo ""
	echo "#############################################"
	echo ""
	echo "$(date +'%F %T,%3N') ${warnLogPrefix} Start stopping tomcat" >>"$CATALINA_OUT"
	local catalinaPid
	wait $(cat "${CATALINA_PID}")
	catalinaPid=$(cat "${CATALINA_PID}")

	if "$triggerThreadDumpBeforeStop"; then
		echo "$(date +'%F %T,%3N') ${warnLogPrefix} Triggering pre-stop thread dump" >>"$CATALINA_OUT"
		kill -3 "$catalinaPid"
	fi

	echo "$(date +'%F %T,%3N') ${warnLogPrefix} Initiating tomcat stop" >>"$CATALINA_OUT"
	"$ALF_HOME"/tomcat/bin/catalina.sh stop "${waitBeforeJavaThreadDump}" >>"$CATALINA_OUT"

	wait "$catalinaPid"
	echo "$(date +'%F %T,%3N') ${warnLogPrefix} Completed tomcat stop" >>"$CATALINA_OUT"

	# give a sec for the tail to catch up with all the logs
	sleep 1

	echo "$(date +'%F %T,%3N') ${warnLogPrefix} Stop libreoffice"
	./libreoffice/scripts/ctl.sh stop

	echo "$(date +'%F %T,%3N') ${warnLogPrefix} Stop postgresql"
	./postgresql/scripts/ctl.sh stop

	exit
}

start

#./postgresql/scripts/ctl.sh start
#./libreoffice/scripts/ctl.sh start
##./tomcat/scripts/ctl.sh daemon
#./tomcat/scripts/ctl.sh start
#apache2ctl start
#
#tail -f /opt/alfresco/tomcat/logs/catalina.out

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
export CATALINA_OUT
CATALINA_PID=$ALF_HOME/tomcat/temp/catalina.pid
export CATALINA_PID


start() {

	echo "current path: $PWD"
	bin/installer/install.sh -f .env "$@" #|| exit 1
	bin/installer/install.sh -f .env "$@" #|| exit 1

	######################################################################################################################

	./postgresql/scripts/ctl.sh start
	./libreoffice/scripts/ctl.sh start


	######################################################################################################################

	# PLUGIN AFTER
	for plugin in bin/plugins/plugin-*/entrypoint.sh; do
		 [[ -f $plugin ]] && {
				source $plugin || exit 1
		 }
	done

	######################################################################################################################

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

start "$@"
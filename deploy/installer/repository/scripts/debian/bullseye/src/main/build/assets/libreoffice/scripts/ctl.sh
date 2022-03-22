#!/bin/bash
    
mkdir -p $ALF_HOME/libreoffice/temp
LIBREOFFICE_PID=$ALF_HOME/libreoffice/temp/libreoffice.pid
export LIBREOFFICE_PID

if [ "x$1" = "xstart" ]; then
  until [ $(ps ax | grep soffice | wc -l) -gt 1 ] ; do
    /usr/lib/libreoffice/program/soffice.bin --nofirststartwizard --nologo --headless --accept=socket,host=127.0.0.1,port=8100\;urp\;StarOffice.ServiceManager &
    sleep 3
  done
  echo $(ps ax | grep soffice | grep -v grep | awk '{print $1}') > $LIBREOFFICE_PID
elif [ "x$1" = "xstop" ]; then
  if [ -f $LIBREOFFICE_PID ] && kill -0 $(cat $LIBREOFFICE_PID) 2>/dev/null ; then
  	kill -15 $(cat $LIBREOFFICE_PID)
		until [ $(ps ax | grep soffice | wc -l) -eq 1 ] ; do
			sleep 3
		done
  	rm $LIBREOFFICE_PID
	fi
elif [ "x$1" = "xstatus" ]; then
  if [ -f $LIBREOFFICE_PID ] && kill -0 $(cat $LIBREOFFICE_PID) 2>/dev/null ; then
		echo "libreoffice already running"
	else
		echo "libreoffice not running"
		rm $LIBREOFFICE_PID 2>/dev/null
	fi
fi

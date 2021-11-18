#!/bin/bash
    
if [ "x$1" = "xstart" ]; then
  pg_ctlcluster 13 alfresco start
elif [ "x$1" = "xstop" ]; then
  pg_ctlcluster 13 alfresco stop
elif [ "x$1" = "xstatus" ]; then
  if [[ $(pg_ctlcluster 13 alfresco status) == *"is running" ]]; then
  	echo "postgresql already running"
  else
  	echo "postgresql not running"
  fi
fi

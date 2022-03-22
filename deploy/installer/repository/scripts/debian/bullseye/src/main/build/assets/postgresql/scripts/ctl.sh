#!/bin/bash
    
if [ "x$1" = "xstart" ]; then
  pg_ctlcluster 13 repository start
elif [ "x$1" = "xstop" ]; then
  pg_ctlcluster 13 repository stop
elif [ "x$1" = "xstatus" ]; then
  if [[ $(pg_ctlcluster 13 repository status | wc -l ) -gt 1 ]]; then
  	echo "postgresql already running"
  else
  	echo "postgresql not running"
  fi
fi

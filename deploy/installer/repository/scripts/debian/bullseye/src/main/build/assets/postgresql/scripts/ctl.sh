#!/bin/bash
    
if [ "x$1" = "xstart" ]; then
  pg_ctlcluster 13 alfresco start
elif [ "x$1" = "xstop" ]; then
  pg_ctlcluster 13 alfresco stop
elif [ "x$1" = "xstatus" ]; then
  pg_ctlcluster 13 alfresco status
fi

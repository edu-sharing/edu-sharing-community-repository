#!/bin/bash
set -eu

duration="${1:-60}"
pid=`ps -ef | grep java | grep alfresco | grep SAXParserFactory | awk '{print $2}'`

/opt/profiler/async-profiler-2.0-linux-x64/profiler.sh -e itimer -f jfr -f /opt/profiler/report/repository-service.jfr -d "${duration}" "${pid}"
java -cp /opt/profiler/async-profiler-2.0-linux-x64/build/converter.jar jfr2flame /opt/profiler/report/repository-service.jfr /opt/profiler/report/repository-service.html

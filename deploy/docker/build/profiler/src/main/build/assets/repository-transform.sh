#!/bin/bash
set -eu

duration="${1:-60}"
pid=`ps -ef | grep java | grep edu_sharing-enterprise-repository-plugin-transform-jodconverter-server | awk '{print $2}'`

/opt/profiler/async-profiler-2.0-linux-x64/profiler.sh -e itimer -f jfr -f /opt/profiler/report/repository-transform.jfr -d "${duration}" "${pid}"
java -cp /opt/profiler/async-profiler-2.0-linux-x64/build/converter.jar jfr2flame /opt/profiler/report/repository-transform.jfr /opt/profiler/report/repository-transform.html

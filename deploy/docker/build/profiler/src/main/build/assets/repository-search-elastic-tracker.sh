#!/bin/bash
set -eu

duration="${1:-60}"
pid=`ps -ef | grep java | grep edu_sharing-community-repository-plugin-elastic-tracker | awk '{print $2}'`

/opt/profiler/async-profiler-2.0-linux-x64/profiler.sh -e itimer -f jfr -f /opt/profiler/report/repository-search-elastic-tracker.jfr -d "${duration}" "${pid}"
java -cp /opt/profiler/async-profiler-2.0-linux-x64/build/converter.jar jfr2flame /opt/profiler/report/repository-search-elastic-tracker.jfr /opt/profiler/report/repository-search-elastic-tracker.html

#!/bin/bash
set -eu

duration="${1:-60}"
pid=`ps -ef | grep apache2 | grep -v grep | awk '{print $2}' | sort | tail -n 1`

/opt/profiler/phpspy-0.6.0-g3/phpspy -i $(( 1000*${duration} )) -p "${pid}" > /opt/profiler/report/services-rendering.trace
/opt/profiler/phpspy-0.6.0-g3/stackcollapse-phpspy.pl </opt/profiler/report/services-rendering.trace | /opt/profiler/phpspy-0.6.0-g3/vendor/flamegraph.pl >/opt/profiler/report/services-rendering.html

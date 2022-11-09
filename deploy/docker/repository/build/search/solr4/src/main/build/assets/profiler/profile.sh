#!/bin/bash
set -eu

SOURCE_PATH="$(cd "$(dirname "${BASH_SOURCE[0]}")" >/dev/null && pwd)"
pushd "${SOURCE_PATH}" >/dev/null || exit

./async-profiler-2.0-linux-x64/profiler.sh -e itimer -f jfr -f report.jfr -d "${1:-10}" 1
java -cp ./async-profiler-2.0-linux-x64/build/converter.jar jfr2flame report.jfr report.html

popd >/dev/null || exit

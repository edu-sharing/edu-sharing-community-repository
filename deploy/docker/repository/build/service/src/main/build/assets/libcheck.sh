#!/bin/bash
set -eu

########################################################################################################################

if [[ ! -d tomcat/webapps/alfresco/WEB-INF/lib ]] ; then
  exit 0
fi


pushd "tomcat/webapps/alfresco/WEB-INF/lib" >/dev/null || exit 1

duplicates=
for jlib in * ; do
  if [[ -f "../../../edu-sharing/WEB-INF/lib/$jlib" ]] ; then
    duplicates="$duplicates $jlib"
  fi
done

if [[ -n $duplicates ]] ; then
  echo "ERROR duplicated libraries found:"
  for dupl in $duplicates ; do
    echo "$dupl"
  done
  exit 1
fi

popd >/dev/null
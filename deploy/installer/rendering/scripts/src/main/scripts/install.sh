#!/bin/bash
set -e
set -o pipefail

[[ ! -d "${APACHE_HOME}" ]] && {
  	echo ""
  	echo "Env APACHE_HOME must point to the home directory of your Apache HTTP Server!"
  	exit
}

echo "- download edu-sharing distribution"
mvn -q dependency:get \
	-Dartifact=org.edu_sharing:edu_sharing-community-deploy-installer-rendering-distribution:${org.edu_sharing:edu_sharing-community-deploy-installer-rendering-distribution:tar.gz:bin.version}:tar.gz:bin \
	-DremoteRepositories=myreleases::::https://artifacts.edu-sharing.com/rendering/community-releases/,mysnapshots::::https://artifacts.edu-sharing.com/rendering/community-snapshots/ \
	-Dtransitive=false

echo "- unpack edu-sharing distribution"
mvn -q dependency:copy \
	-Dartifact=org.edu_sharing:edu_sharing-community-deploy-installer-rendering-distribution:${org.edu_sharing:edu_sharing-community-deploy-installer-rendering-distribution:tar.gz:bin.version}:tar.gz:bin \
	-DoutputDirectory=.

tar xzf edu_sharing-community-deploy-installer-rendering-distribution-${org.edu_sharing:edu_sharing-community-deploy-installer-rendering-distribution:tar.gz:bin.version}-bin.tar.gz
rm edu_sharing-community-deploy-installer-rendering-distribution-${org.edu_sharing:edu_sharing-community-deploy-installer-rendering-distribution:tar.gz:bin.version}-bin.tar.gz
#!/bin/bash
set -e
set -o pipefail

mvn dependency:copy -Dartifact=org.edu_sharing:edu_sharing-community-deploy-installer-repository-distribution:${project.version}:tar.gz:bin

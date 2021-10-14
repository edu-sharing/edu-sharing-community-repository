#!/usr/bin/env bash

set -e

# Swagger Codegen version to download and use.
SWAGGER_CODEGEN_VERSION=3.0.27

SWAGGER_CODEGEN=swagger-codegen-cli-$SWAGGER_CODEGEN_VERSION.jar
SWAGGER_CODEGEN_DOWNLOAD_URL=https://repo1.maven.org/maven2/io/swagger/codegen/v3/swagger-codegen-cli/$SWAGGER_CODEGEN_VERSION/$SWAGGER_CODEGEN

mkdir -p vendor
mkdir -p tmp

if [[ ! -f vendor/$SWAGGER_CODEGEN ]]; then
    wget -P vendor $SWAGGER_CODEGEN_DOWNLOAD_URL
fi

java -jar vendor/$SWAGGER_CODEGEN generate -l openapi-yaml -i swagger.json -o tmp


# Replace all these lines:
# ```
#           content:
#             '*/*':
# ```
# with this:
# ```
#           content:
#             'application/json':
# ```
sed -zE "s|(\s*content:\n\s*)'\*/\*'|\1'application/json'|g" tmp/openapi.yaml > openapi.yaml
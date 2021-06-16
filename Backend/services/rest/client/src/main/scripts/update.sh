#!/bin/bash
echo 'Please enter the address of the edu-sharing server to fetch from (e.g. http://192.168.16.1): '
read server
java -jar ./swagger-codegen-cli-2.4.10.jar generate -i "$server/edu-sharing/rest/swagger.json" -l java -o ../../../
@echo off
SET ALF_LIB_PATH=C:/myprogs/alfresco-labs-tomcat-3Stable/tomcat/webapps/alfresco/WEB-INF/lib
SET PROJECT_BIN_PATH=C:/Users/rudi/workspace/CCSearch/bin
SET AXIS_LIB_PATH=C:/myprogs/axis-1_4/lib

set /P WSDL_PATH=path to wsdl:
echo using wsdl:%WSDL_PATH%
rem set /P PACKAGE_NAME=package name:
rem echo using %PACKAGE_NAME%
rem set /P NAME=name:
rem echo using name: %NAME%
 
cd src-webservices

set CLASSPATH=%AXIS_LIB_PATH%/*;%ALF_LIB_PATH%/*;%PROJECT_BIN_PATH%/.
echo Classpath: %CLASSPATH%

java -classpath %CLASSPATH% org.apache.axis.wsdl.WSDL2Java -w -o . -d Session -s -S true %WSDL_PATH%
cd ..


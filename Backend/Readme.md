# edu-sharing Backend
This is the backend part of edu-sharing.
It uses Alfresco 5.0.d as a base.

Environment configuration
-------------------------
Install java jdk on your system
We recommend using Eclipse for project usage
Create a build.<os-username>.properties on the same level as build.properties and set the following values:
webserver.home=path/to/alfresco/tomcat

Deploy & Release
----------------
Use the Ant-Tasks provided in "build.xml" and run "deploy" and "release" to create a release.
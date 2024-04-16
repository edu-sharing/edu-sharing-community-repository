# edu-sharing Backend
This is the backend part of edu-sharing.
It uses Alfresco 5.2.f as a base (Community 201707).

Link to download page:
https://hub.alfresco.com/t5/alfresco-content-services-hub/alfresco-community-edition-201707-ga-file-list/ba-p/290487

Environment configuration
-------------------------
Install java jdk on your system

We recommend using IntelliJ IDEA or Eclipse for project usage.

Create a build.[your-os-username].properties on the same level as build.properties and set the following values:

webserver.home=path/to/alfresco/tomcat

Deploy & Release
----------------
Use the Ant-Tasks provided in "build.xml" and run `deploy` (to build all files and automatically transfer them to your local alfresco tomcat) and `release` (to create a release zip file).

You may also use the task `reload`. This will cause a touch-event on the web.xml inside the edu-sharing webapp and will let tomcat reload the webapp while it's running.

FAQ
---
`java.lang.OutOfMemoryError: Java heap space` occurs on Deploy

This may happens when the Ant heap is set to low.
In IntelliJ, go to Ant properties and increase the Heap size to 512MB.
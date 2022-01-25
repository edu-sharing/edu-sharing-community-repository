 
# edu-sharing community - deploy installer rendering

Prerequisites
-------------
- Debian:Bullseye
- Postgresql 13
- Java SE Development Kit 1.8
- Apache Maven 3.8.4+
- PHP 7.4 with dom, soap, common, gd, mbstring, zip, curl, exif, pgsql, x-debug
- xmlstarlet
- wait-for-it
- jq
- imagemagick
- ghostscript
- ffmpeg
- cron

Build
-----

1. Build installer artifact with maven.
   After that you can find the installer artifact inside the `target` subdirectory.


Installation or Update
----------------------
- During the update, the installation program saves the persistent settings and then rolls back the edu-sharing installation.
  Each update is treated as a new installation. At the end, the saved persistent settings are restored.
- The installer will automatically register the rendering-service with edu-sharing if you specify the repository server.
  For this it is necessary that the edu-sharing repository is already in operation.

0. Install following requirements:
   - [Apache HTTP Server](https://httpd.apache.org) and activate following modules:
     - mod_headers
     - mod_rewrite
     - [mod_php](https://www.php.net/manual/en/install.unix.apache2.php) with following extensions:
       - curl 
       - dom 
       - exif
       - fileinfo 
       - gd 
       - iconv 
       - mbstring 
       - openssl 
       - pdo
       - pdo_pgsql
       - session 
       - soap 
       - sockets 
       - zip
       - zlib
   - [PostgreSQL Server](https://www.postgresql.org)
   
1. Edit the configuration of your website by calling:
   ```
   cat > /etc/apache2/sites-available/000-default.conf << EOL  
   <VirtualHost *:80>
        DocumentRoot /var/www/html
        <Directory "/var/www/html">
            Options indexes FollowSymLinks MultiViews
            AllowOverride All
            Require all granted
        </Directory>
   </VirtualHost>
   EOL
   ```

2. Set the environment variables `WWW_ROOT`, `RS_ROOT`, `RS_CACHE` with the home directory of your Alfresco installation, for example:
   ```
   export WWW_ROOT=/var/www/html/
   export RS_ROOT=/var/www/html/esrender
   export RS_CACHE=/var/cache/esrender
   ```

4. Download the specific edu-sharing version from Maven repository
   ```
   BRANCH=maven-release-6.1
   mvn -q dependency:get \
       -Dartifact="org.edu_sharing:edu_sharing-community-deploy-installer-services-rendering-scripts-debian-bullseye:${BRANCH}:tar.gz:bin" \
       -DremoteRepositories=edusharing-remote::::https://artifacts.edu-sharing.com/repository/maven-remote/ \
       -Dtransitive=false
      
   mvn -q dependency:copy \
       -Dartifact="org.edu_sharing:edu_sharing-community-deploy-installer-services-rendering-scripts-debian-bullseye:${BRANCH}:tar.gz:bin" \
       -DoutputDirectory=.
   ```
   
5. Unzip the installer
   ```
   tar xzf "./edu_sharing-community-deploy-installer-services-rendering-scripts-debian-bullseye-${BRANCH}-bin.tar.gz"
   rm "./edu_sharing-community-deploy-installer-services-rendering-scripts-debian-bullseye-${BRANCH}-bin.tar.gz"
   ```

5. Run the installer

   To install or update rendering service:
   ```
   "./install.sh" 
   ```
   Next to the installer you will find an .env.base file which contains the default configuration of edu-sharing.
   Since this file is supplied with the installer and will be overwritten by the next update, the file should not be modified!
   Instead, create a separate .env file and define only the relevant settings you want to adjust.
   In addition, a log file with the used configuration will be created next to the installer.
   ```
   "./install.sh" -f .env
   ```
   To use a local build of edu-sharing.
   You will need to run the [distribution](distribution) until install in maven, before you can use it by the installer locally.
   ```
   "./install.sh" --local
   ```

6. Start the webserver and run the installation wizard under `http://<webserver>/esrender/admin/`.
   
---
If you need more information, please consult our [edu-sharing community sdk](https://scm.edu-sharing.com/edu-sharing-community/edu-sharing-community-sdk) project.

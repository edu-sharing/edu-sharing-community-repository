
# edu-sharing community - deploy installer rendering

Prerequisites
-------------

- Apache Maven 3.6.3+
- Git SCM

Build
-----

1. Build installer artifact by calling:

   ```
   ./deploy.sh build
   ```    

   After that you can find the installer artifact inside the `target` subdirectory.

Installation
------------

1. Install following requirements:

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
       - wddx
       - zip
       - zlib
   - [PostgreSQL Server](https://www.postgresql.org)
   
2. Edit the configuration of your website by calling:

   ```
   cat > /etc/apache2/sites-available/000-default.conf << EOL  
   <VirtualHost *:80>
        DocumentRoot /var/www/html
        <Directory "/var/www/html">
            Options -Indexes +FollowSymLinks
            AllowOverride All
            Require all granted
        </Directory>
   </VirtualHost>
   EOL
   ```

3. Create a directory for caching files and grant read/write permissions to the webserver by calling:

   ```
   mkdir -p /var/cache/esrender
   chown www-data:www-data /var/cache/esrender
   ```

4. Unpack the installer artifact (see [Build](#build)) into the document root of your website 
   and grant read/write permissions to the webserver by calling:

   ```
   tar -xzvf target/edu_sharing-community-deploy-installer-rendering-<version>-bin.tar.gz -C /var/www/html
   chown -R www-data:www-data /var/www/html/esrender
   ```

5. Start the webserver and run the installation wizard under `http://<webserver>/esrender/admin/`.
   
Update
------

1. Stop the webserver.

2. Save your custom configuration by calling:

   ```
   pushd /var/www
   git add html/esrender/conf/*
   git add html/esrender/**/config.php
   git commit -m "Before update."
   popd
   ```

3. Cleanup your document root by calling:

   ```
   rm -rf /var/www/html/esrender
   ```

4. Unpack the installer artifact (see [Build](#build)) into the document root of your website
   and grant read/write permissions to the webserver by calling:

   ```
   tar -xzvf target/edu_sharing-community-deploy-installer-rendering-<version>-bin.tar.gz -C /var/www/html
   chown -R www-data:www-data /var/www/html/esrender
   ```

5. Restore your custom configuration by calling:

   ```
   pushd /var/www
   git reset --hard
   popd
   ```

6. Start the webserver and run the update wizard under `http://yourdomain.de/esrender/admin/`.

7. Save your custom configuration by calling:

   ```
   pushd /var/www
   git add html/esrender/conf/*
   git add html/esrender/**/config.php
   git commit -m "After update."
   popd
   ```

---
If you need more information, please consult our [edu-sharing community sdk](https://scm.edu-sharing.com/edu-sharing-community/edu-sharing-community-sdk) project.

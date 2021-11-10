
# edu-sharing community - deploy docker

Prerequisites
-------------

- Docker Engine 19.03.0+
- Docker Compose 1.27.4+ 
- Apache Maven 3.6.3+
- Java SE Development Kit 1.8+
- Git

Download
--------

1. Download the docker artifact:

   * [releases](https://artifacts.edu-sharing.com/#browse/browse:community-releases:org%2Fedu_sharing%2Fedu_sharing-community-deploy-docker-deploy)
   * [snapshots](https://artifacts.edu-sharing.com/#browse/browse:community-snapshots:org%2Fedu_sharing%2Fedu_sharing-community-deploy-docker-deploy)

Installation
------------

1. Unpack the [downloaded](#download) docker artifact.
 
2. Start up an instance by calling:

   ```
   ./deploy.sh start
   ```

3. Request all necessary information by calling:

   ```
   ./deploy.sh info
   ```

   and print out all running containers by calling:

   ```
   ./deploy.sh ps
   ```

   and stream out the log messages by calling:

   ```
   ./deploy.sh logs
   ```

4. Shut down the instance by calling:

   ```
   ./deploy.sh stop
   ```
  
5. Save all data volumes on disk by calling:

   ```
   ./deploy.sh backup <path>
   ```
   
   > Later on you can restore all data volumes from disk by calling:
   > ```
   > ./deploy.sh restore <path>
   > ```
   
6. Clean up all data volumes by calling:

   ```
   ./deploy.sh purge
   ```

---
If you need more information, please consult our [edu-sharing community sdk](https://scm.edu-sharing.com/edu-sharing-community/edu-sharing-community-sdk) project.

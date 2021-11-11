
# edu-sharing community - deploy docker

Prerequisites
-------------

- Docker Engine 19.03.0+
- Docker Compose 1.27.4+ 
- Apache Maven 3.6.3+
- Git SCM

Installation
------------

1. Start up an instance by calling:

   ```
   ./deploy.sh start
   ```

2. Stream out the log messages by calling:

   ```
   ./deploy.sh logs
   ```

3. Shut down the instance by calling:

   ```
   ./deploy.sh stop
   ```

4. Clean up all data volumes by calling:

   ```
   ./deploy.sh purge
   ```

Backup
------

1. Shut down the instance by calling:

   ```
   ./deploy.sh stop
   ```
  
2. Save all data volumes on disk by calling:

   ```
   ./deploy.sh backup <path>
   ```

3. Clean up all data volumes by calling:

   ```
   ./deploy.sh purge
   ```

Restore
-------

1. Create all data volumes by calling:

   ```
   ./deploy.sh init
   ```

2. Restore all data volumes from disk by calling:

   ```
   ./deploy.sh restore <path>
   ```

3. Start up the instance by calling:

   ```
   ./deploy.sh start
   ```

---
If you need more information, please consult our [edu-sharing community sdk](https://scm.edu-sharing.com/edu-sharing-community/edu-sharing-community-sdk) project.

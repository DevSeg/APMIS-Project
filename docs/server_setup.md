# Installing an APMIS Server

**Note: This guide explains how to set up a APMIS server on Linux and Windows systems, the latter only being intended for usage on development systems. Please also note that certain parts of the setup script will not be executed on Windows.**

## Content
- [Installing an APMIS Server](#installing-an-apmis-server)
  - [Content](#content)
  - [Related](#related)
  - [Prerequisites](#prerequisites)
    - [Java 11](#java-11)
      - [Linux](#linux)
      - [Windows](#windows)
    - [Postgres Database](#postgres-database)
  - [APMIS Server](#apmis-server)
    - [Install on Linux](#install-on-linux)
    - [Install on Windows](#install-on-windows)
    - [Post-Installation Configuration](#post-installation-configuration)
  - [Web Server Setup](#web-server-setup)
    - [Apache Web Server](#apache-web-server)
    - [Firewall](#firewall)
    - [Postfix Mail Server](#postfix-mail-server)
      - [Install postfix and mailutils](#install-postfix-and-mailutils)
      - [Configure your system](#configure-your-system)
    - [Testing the Server Setup](#testing-the-server-setup)
- [Installing an APMIS Server for Development](#installing-an-apmis-server-for-development)
  - [Content](#content)
  - [Related](#related)
  - [Prerequisites](#prerequisites)
    - [Java 11](#java-11)
      - [Linux](#linux)
      - [Windows](#windows)
    - [Postgres Database](#postgres-database)
  - [APMIS Server](#apmis-server)
- [Updating an APMIS Server]
- [R Software Environment](#r-software-environment)
- [APMIS to APMIS Certificate Setup](#APMIS-to-apmis-certificate-setup)
- [Troubleshooting](#troubleshooting)
  - [Problem: Login fails](#problem-login-fails)
  - [Problem: Server is out of memory](#problem-server-is-out-of-memory)
	
	
## Related

* [Creating an App for a Demo Server](DEMO_APP.md)

## Prerequisites

### Java 11

Download and install the Java 11 **JDK** (not JRE) for your operating system. We suggest using the [Zulu OpenJDK](https://www.azul.com/downloads/zulu/).

#### [Linux](https://docs.azul.com/zulu/zuludocs/#ZuluUserGuide/PrepareZuluPlatform/AttachAPTRepositoryUbuntuOrDebianSys.htm)

```bash
sudo apt-key adv --keyserver hkp://keyserver.ubuntu.com:80 --recv-keys 0xB1998361219BD9C9
sudo apt-add-repository 'deb https://repos.azul.com/zulu/deb/ stable main'
sudo apt-get update
sudo apt-get install zulu11
```

#### Windows

For testing and development environments we suggest to download and run the installer of the Java 11 **JDK** for 32 or 64 bit client systems (depending on your system).
You can check your Java version from the shell/command line using: ``java -version``.

### Postgres Database

* Install PostgreSQL (currently 9.5, 9.6 or 10) on your system (manuals for all OS can be found here: <https://www.postgresql.org/download>)
* Set **max_connections = 288** and **max_prepared_transactions = 256** (at least, sum of all connection pools) in ``postgresql.conf`` (e.g. ``/etc/postgresql/10.0/main/postgresql.conf``; ``C:/Program Files/PostgreSQL/10.0/data``) - make sure the property is uncommented and restart the service to apply the changes.
* Install the "temporal tables" extension for Postgres (<https://github.com/arkhipov/temporal_tables>)
  * **Windows**: Download the latest version for your Postgres version: <https://github.com/arkhipov/temporal_tables/releases/latest>, then copy the DLL from the project into the PostgreSQL's lib directory and the .sql and .control files into the directory share\extension.
  * **Linux** (see <https://github.com/arkhipov/temporal_tables#installation)>

```bash
sudo apt-get install libpq-dev
sudo apt-get install postgresql-server-dev-all
sudo apt install pgxnclient
#Check for GCC:
gcc --version # and install if missing
sudo pgxn install temporal_tables
# The packages can be removed afterward
```


## APMIS Server

Get the latest APMIS build by downloading the ZIP archive from the latest release on GitHub: <https://github.com/xlg8/APMIS-Project/releases/latest>

### Install on Linux

Unzip the archive, copy/upload its contents to **/root/deploy/sormas/$(date +%F)** and make the setup script executable (as root user).

```bash
sudo su
mkdir /root/deploy/sormas
cd /root/deploy/sormas
APMIS_VERSION=1.y.z
wget https://github.com/xlg8/APMIS-Project/releases/download/v${APMIS_VERSION}/sormas_${APMIS_VERSION}.zip
unzip sormas_${APMIS_VERSION}.zip
mv deploy/ $(date +%F)
rm sormas_${APMIS_VERSION}.zip
chmod +x $(date +%F)/server-setup.sh
```

### Install on Windows

* Download & install Git for Windows. This will provide a bash emulation that you can use to run the setup script: <https://gitforwindows.org/>
* Unzip the ZIP archive (e.g. into you download directory)
* Open Git Bash and navigate to the setup sub-directory

### Post-Installation Configuration

* Optional: Open ``server-setup.sh`` in a text editor to customize the install paths, database access and ports for the server. The default ports are 6080 (HTTP), 6081 (HTTPS) and 6048 (admin). **Important:** Do not change the name of the database user. The pre-defined name is used in the statements executed in the database.
* Set up the database and a Payara domain for APMIS by executing the setup script: ``sudo -s ./server-setup.sh`` Press enter whenever asked for it
* **IMPORTANT**: Make sure the script executed successfully. If anything goes wrong you need to fix the problem (or ask for help), then delete the created domain directory and re-execute the script.
* **IMPORTANT**: Adjust the APMIS configuration for your country in /opt/domains/sormas/sormas.properties
* Adjust the logging configuration in ``/opt/domains/sormas/config/logback.xml`` based on your needs (e.g. configure and activate email appender)
* Linux: [Update the APMIS domain](SERVER_UPDATE.md)

## Web Server Setup

### Apache Web Server
**Note: This is not necessary for development systems.** When you are using APMIS in a production environment you should use a http server like Apache 2 instead of putting the Payara server in the first line.
Here are some things that you should do to configure the Apache server as a proxy:

Activate all needed modules:

```bash
a2enmod ssl
a2enmod rewrite
a2enmod proxy
a2enmod proxy_http
a2enmod headers
```

Create a new site `/etc/apache2/sites-available/your.apmis.server.url.conf` (e.g. apmis.org.conf).

Force SSL secured connections: redirect from http to https:

```xml
<VirtualHost *:80>
        ServerName your.apmis.server.url
        RewriteEngine On
        RewriteCond %{HTTPS} !=on
        RewriteRule ^/(.*) https://your.apmis.server.url/$1 [R,L]
</VirtualHost>
<IfModule mod_ssl.c>
<VirtualHost *:443>
        ServerName your.apmis.server.url
        ...
</VirtualHost>
</IfModule>
```
Configure logging:

```text
ErrorLog /var/log/apache2/error.log
LogLevel warn
LogFormat "%h %l %u %t \"%r\" %>s %b _%D_ \"%{User}i\"  \"%{Connection}i\"  \"%{Referer}i\" \"%{User-agent}i\"" combined_ext
CustomLog /var/log/apache2/access.log combined_ext
```

SSL key config:

```text
SSLEngine on
SSLCertificateFile    /etc/ssl/certs/your.apmis.server.url.crt
SSLCertificateKeyFile /etc/ssl/private/your.apmis.server.url.key
SSLCertificateChainFile /etc/ssl/certs/your.apmis.server.url.ca-bundle

# disable weak ciphers and old TLS/SSL
SSLProtocol all -SSLv3 -TLSv1 -TLSv1.1
SSLCipherSuite ECDHE-ECDSA-AES128-GCM-SHA256:ECDHE-RSA-AES128-GCM-SHA256:ECDHE-ECDSA-AES256-GCM-SHA384:ECDHE-RSA-AES256-GCM-SHA384:ECDHE-ECDSA-CHACHA20-POLY1305:ECDHE-RSA-CHACHA20-POLY1305:DHE$
SSLHonorCipherOrder off
```

Add a proxy pass to the local port:

```text
ProxyRequests Off
ProxyPass /sormas-ui http://localhost:6080/sormas-ui
ProxyPassReverse /sormas-ui http://localhost:6080/sormas-ui
ProxyPass /sormas-rest http://localhost:6080/sormas-rest
ProxyPassReverse /sormas-rest http://localhost:6080/sormas-rest
```

Configure security settings:

```text
Header always set X-Content-Type-Options "nosniff"
Header always set X-Xss-Protection "1; mode=block"
# Disable Caching
Header always set Cache-Control "no-cache, no-store, must-revalidate, private"
Header always set Pragma "no-cache"

Header always set Content-Security-Policy \
        "default-src 'none'; \
        object-src 'self'; \
        script-src 'self' 'unsafe-inline' 'unsafe-eval'; \
        connect-src https://fonts.googleapis.com https://fonts.gstatic.com 'self'; \
        img-src *; \
        style-src 'self' https://fonts.googleapis.com 'unsafe-inline'; \
        font-src https://fonts.gstatic.com 'self'; \
        frame-src 'self'; \
        worker-src 'self'; \
        manifest-src 'self'; \
        frame-ancestors 'self'

# The Content-Type header was either missing or empty.
# Ensure each page is setting the specific and appropriate content-type value for the content being delivered.
AddType application/vnd.ms-fontobject    .eot
AddType application/x-font-opentype      .otf
AddType image/svg+xml                    .svg
AddType application/x-font-ttf           .ttf
AddType application/font-woff            .woff
```

Activate output compression (very important!):

```xml
<IfModule mod_deflate.c>
        AddOutputFilterByType DEFLATE text/plain text/html text/xml
        AddOutputFilterByType DEFLATE text/css text/javascript
        AddOutputFilterByType DEFLATE application/json
        AddOutputFilterByType DEFLATE application/xml application/xhtml+xml
        AddOutputFilterByType DEFLATE application/javascript application/x-javascript
        DeflateCompressionLevel 1
</IfModule>
```

Provide the android apk:

```java
Options -Indexes
AliasMatch "/downloads/sormas-(.*)" "/var/www/sormas/downloads/sormas-$1"
```

For the Apache 2 security configuration we suggest the following settings (``/etc/apache2/conf-available/security.conf``):

```conf
ServerTokens Prod
ServerSignature Off
TraceEnable Off

Header always set Strict-Transport-Security "max-age=15768000; includeSubDomains; preload"
Header unset X-Frame-Options
Header always set X-Frame-Options SAMEORIGIN
Header unset Referrer-Policy
Header always set Referrer-Policy "same-origin"
Header edit Set-Cookie "(?i)^((?:(?!;\s?HttpOnly).)+)$" "$1;HttpOnly"
Header edit Set-Cookie "(?i)^((?:(?!;\s?Secure).)+)$" "$1;Secure"

Header unset X-Powered-By
Header unset Server
```

* In case you need to update the site config while the server is running, use the following command to publish the changes without the need for a reload:

```bash
apache2ctl graceful
```

### Firewall

* The server should only publish the ports that are needed. For APMIS this is port 80 (HTTP) and 443 (HTTPS). In addition you will need the SSH port to access the server for admin purposes.
* We suggest to use UFW (Uncomplicated Firewall) which provides a simple interface to iptables:

```bash
sudo apt-get install ufw
sudo ufw default deny incoming
sudo ufw default allow outgoing
sudo ufw allow ssh
sudo ufw allow http
sudo ufw allow https
sudo ufw enable
```

### Postfix Mail Server

#### Install postfix and mailutils

```bash
apt install aptitude
aptitude install postfix
-> choose "satelite system"
apt install mailutils
```

#### Configure your system

```bash
nano /etc/aliases
-> add "root: enter-your@support-email-here.com"
nano /opt/domains/sormas/config/logback.xml
-> make sure "EMAIL_ERROR" appender is active and sends out to your email address
```

### Testing the Server Setup

Use SSL Labs to test your server security config: <https://www.ssllabs.com/ssltest>


# Installing an APMIS Server for Development

**Note: This guide explains how to configure a APMIS server on Linux and Windows systems for development. 

## Content
* [Prerequisites](#prerequisites)
  * [Java 11](#java-11)
  * [ant](#ant)
  * [Postgres Database](#postgres-database)
* [APMIS Server](#apmis-server)

## Related
* [Installing an APMIS Server](SERVER_SETUP.md)

## Prerequisites

### Java 11
See [Installing Java](SERVER_SETUP.md#java-11)

APMIS just recently moved to Java 11. We still need to support Java 8 for a transition period. Therefore, please just
use Java 8 language features for now.

### Ant

Download and install Ant, it can be done from [Ant site](https://ant.apache.org/bindownload.cgi) or with packages from your Linux distribution.

### Postgres Database

See [Installing Postgresql](SERVER_SETUP.md#postgres-database)


## APMIS Server

Install you own Payara server (see [Installing an APMIS Server](SERVER_SETUP.md#sormas-server)) or run ``bash ./server-setup-dev-docker.sh``

This script will download Payara (if needed) and install APMIS in the Payara server.

You can edit this script to change paths and ports.

Other steps :
* **IMPORTANT**: Adjust the APMIS configuration for your country in /opt/domains/sormas/sormas.properties
* Adjust the logging configuration in ``${HOME}/opt/domains/sormas/config/logback.xml`` based on your needs (e.g. configure and activate email appender)
* Build and deploy applications (ear and war) with you IDE.

## Keycloak

See [Keycloak](SERVER_SETUP.md#keycloak-server) for how to install Docker locally.

If you are doing active development on Keycloak (themes, authentication mechanisms, translations, etc.) it's recommended to install the standalone variant.

## VAADIN Debug Mode

To enable [VAADIN Debug Mode](https://vaadin.com/docs/v8/framework/advanced/advanced-debug.html), go to ``sormas-ui/src/main/webapp/WEB-INF/web.xml`` and set ``productionMode`` to ``false``.
Make sure not to commit your changes to these files, for example by using .gitignore. To access the debug Window, got to <url>/sormas-ui/?debug. You may need to log in as admin once first.

## Other components

See [Installing an APMIS Server](#installing-an-apmis-server)


# Updating an APMIS Server

APMIS releases starting from 1.21.0 contain a script that automatically updates and deploys the server. If you are using an older version and therefore need to do a manual server update, please download the 1.21.0 release files and use the commands specified in the server-update.sh script.

## Preparations
Note: You can skip this step if you've just set up your APMIS server and have already downloaded the latest release.

* Get the latest release files (deploy.zip) from <https://github.com/xlg8/APMIS-Project/releases/latest>
* Unzip the archive and copy/upload its contents to **/root/deploy/sormas/$(date +%F)**
    ```bash
    cd /root/deploy/sormas
    APMIS_VERSION=1.y.z
    wget https://github.com/xlg8/APMIS-Project/releases/download/v${APMIS_VERSION}/sormas_${APMIS_VERSION}.zip
    unzip sormas_${APMIS_VERSION}.zip
    mv deploy/ $(date +%F)
    rm sormas_${APMIS_VERSION}.zip
    ```
## Automatic Server Update
* Navigate to the  folder containing the unzipped deploy files:
  ``cd /root/deploy/sormas/$(date +%F)``
* Make the update script executable:
  ``chmod +x server-update.sh``
* Optional: Open server-update.sh in a text editor to customize the values for e.g. the domain path or the database name. You only need to do this if you used custom values while setting up the server.
* Execute the update script and follow the instructions:
  ``./server-update.sh``
* If anything goes wrong, open the latest update log file (by default located in the "update-logs" folder in the domain directory) and check it for errors.

## Restoring the Database
If anything goes wrong during the automatic database update process when deploying the server, you can use the following command to restore the data:

``pg_restore --clean -U postgres -Fc -d sormas_db sormas_db_....dump``

## Default Logins
These are the default users for most user roles, intended to be used on development or demo systems. In all cases except the admin user, the username and password are identical. Make sure to deactivate them or change the passwords on productive systems.

### Admin
**Username:** admin
**Password:** sadmin

### Web users
**Surveillance Supervisor:** SurvSup
**Case Supervisor:** CaseSup
**Contact Supervisor:** ContSup
**Point of Entry Supervisor:** PoeSup
**Laboratory Officer:** LabOff
**Event Officer:** EveOff
**National User:** NatUser
**National Clinician:** NatClin

### Mobile app users
**Surveillance Officer:** SurvOff
**Hospital Informant:** HospInf
**Point of Entry Informant:** PoeInf


## R Software Environment

In order to enable disease network diagrams in the contact dashboard, R and several extension packages are required.
Then the Rscript executable has to be configured in the ``sormas.properties`` file.
This can be conveniently accomplished by executing the R setup script from the APMIS ZIP archive (see [APMIS Server](#apmis-server)):

* If the APMIS installation has been customized, ``r-setup.sh`` the install paths may have to be adjusted accordingly with a text editor.
* Execute R setup script and follow its instructions.

```bash
chmod +x r-setup.sh
./r-setup.sh
```

## APMIS to APMIS Certificate Setup

To be able to communicate with other APMIS instances, there are some additional steps which need to be taken, in order to set
up the certificate and the truststore. Please see the [related guide](https://github.com/xlg8/APMIS-Project/wiki/Creating-a-SORMAS2SORMAS-Certificate) for detailed instructions regarding SORMAS to SORMAS setup.
<br/>

## Troubleshooting

### Problem: Login fails

Check that the users table does have a corresponding entry. If not, the database initialization that is done when deploying sormas-ear.ear probably had an error.

### Problem: Server is out of memory

Old servers were set up with a memory size of less than 2048MB. You can change this using the following commands:

```bash
/opt/payara-172/glassfish/bin/asadmin --port 6048 delete-jvm-options -Xmx512m
/opt/payara-172/glassfish/bin/asadmin --port 6048 delete-jvm-options -Xmx1024m
/opt/payara-172/glassfish/bin/asadmin --port 6048 create-jvm-options -Xmx2048m
```

Alternative: You can edit the settings directly in the domain.xml in the config directory of the APMIS domain. Just search for ``Xmx`` - there should be two entries that need to be changed.



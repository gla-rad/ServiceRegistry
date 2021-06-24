# Maritime Connectivity Platform Service Registry
This is the implementation of the MCP Service Registry. It is under the Apache 
2.0 License.

The Maritime Connectivity Platform was formerly known as the Maritime Cloud and 
therefore there might still be references to that in this project.

## General
The MCP Service Registry is built using the Java Sprinboot frameworks and
required a PostgreSQL database with a PostGIS extension. More information 
on how to download and install PostGIS can he found 
[here](https://postgis.net/source/).

## Database Configuration
The service uses hibernate to initialise the database structure. The database
connection parameters such as the URL and username/password should be provided
in the *application.properties* file found in the resources' folder. Here
is an example:

```yaml
spring.datasource.url=jdbc:postgresql://localhost:5432/mcp_service_registry
spring.datasource.username=username
spring.datasource.password=password
```

To create a local database for development/testing you should install a 
postgreSQL server and the postGIS extension. On an ubuntu system this 
can be done easily as follows:

```bash
sudo apt install postgresql postgresql-contrib
sudo apt install postgis
sudo -i -u postgres
psql
```

The last two commands will allow you to connect to the newly installed server. 
Then create enable the postGIS extension and create a database and a
user for the service to connect to:

```bash
postgres=# CREATE DATABASE mcp_service_registry;
postgres=# \c mcp_service_registry;
postgres=# CREATE EXTENSION postgis;
postgres=# CREATE USER sysadmin WITH PASSWORD 'sysadmin';
postgres=# GRANT ALL PRIVILEGES ON DATABASE mcp_service_registry to sysadmin;
```

If you, like me don't remember your PostgreSQL command 
[here](https://gist.github.com/Kartones/dd3ff5ec5ea238d4c546) is a quick
cheatsheet.

## Contributing
Pull requests are welcome. For major changes, please open an issue first to
discuss what you would like to change.

Please make sure to update tests as appropriate.

## License
Distributed under the Apache License. See [LICENSE](./LICENSE) for more
information.

## Contact
Nikolaos Vastardis - Nikolaos.Vastardis@gla-rad.org

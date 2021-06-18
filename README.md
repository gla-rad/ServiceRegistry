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

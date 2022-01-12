# Maritime Connectivity Platform Service Registry
This is the implementation of the MCP Service Registry (MSR). It is under the Apache 
2.0 License.

The Maritime Connectivity Platform was formerly known as the Maritime Cloud and 
therefore there might still be references to that in this project.

This implementation is a reboot version of 
[Service registry from Efficiensea2](https://github.com/maritimeconnectivity/mc-serviceregistry) 
motivated by newly introduced requirements of MSR.

## Background
We are maintaining 
[Wiki pages](https://github.com/maritimeconnectivity/ServiceRegistry/wiki) for 
explaining backgrounds and issues.

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

## Keycloak Configuration
The current version of the MSR is using the [Keycloak](https://www.keycloak.org/)
for access management (version 15.0.1). The Spring Keycloak adapter is used to 
link the service to the authentication server. Therefore, before running the 
service you will need to create a security realm in Keycloak and setup a client
service. To get things going faster, the required client configuration can be
found in the [service-registry.json](src/main/resources/service-registry.json)
file. Note that the client's access type is *confidential* so you might need
to regererate the client's secret. Once the service is ready in the Keycloak
service, you will need to connect the service to it using the relevant section
of the *application.properties* configuration file.

    # Keycloak Configuration
    keycloak.auth-server-url=http://<keycloak-server>:8090/auth
    keycloak.realm=MCP
    keycloak.resource=service-registry
    keycloak.credentials.secret=<client-secret>
    keycloak.ssl-required=none
    keycloak.principal-attribute=preferred_username
    keycloak.autodetect-bearer-only=true
    keycloak.use-resource-role-mappings=true
    keycloak.token-minimum-time-to-live=30

Please note that for greater access management granularity, the resource policy
enforcer has been turned on. This means that every API resource of the service
should be registered with keycloak and a role, a policy and a permission should
be defined for it. For example, let's have a look at the endpoint that returns
the available instances: */api/instances*.

For this endpoint we have a resource registered in keycloak as follows:

    {
      "name": "api_instances",
      "type": "urn:service-registry:resources:instances",
      "ownerManagedAccess": false,
      "displayName": "API Instances Endpoint",
      "attributes": {},
      "_id": <some-id>>,
      "uris": [
        "/api/instances"
      ],
      "scopes": [
        {
          "name": "GET"
        },
        {
          "name": "POST"
        }
      ]
    }

Notice that the same resource is used by both the GET and the POST HTTP REST 
requests to allow users to also create new instances. In our example, for the
GET request, we have also defined a role called *get_api_instances*, a
policy with the same name for simplicity (*get_api_instances*) that checks
whether the user has that role, and finally a scope permission that grants
access to the endpoint if the user has the *get_api_instances* role.

    {
      "id": <some-id>,
      "name": "Allow Get Instances",
      "description": "Allows users to retrieve the instances",
      "type": "scope",
      "logic": "POSITIVE",
      "decisionStrategy": "UNANIMOUS",
      "config": {
        "resources": "[\"api_instances\"]",
        "scopes": "[\"GET\"]",
        "applyPolicies": "[\"get_api_instances\"]"
      }
    },

In order to enable the resource-level access management you will need to turn
on the *Authorization Enabled* setting in the main setting of the service 
registry client. To keep things simple, the authorization configuration can be
found [here](src/main/resources/service-registry-authorization.json).
You can simply import the file in the *Authorization* tab of the client.

The last trick that binds everything together is again found in the 
*application.properties* file:

    # Start enforcing the policies defined in keycloak
    keycloak.policy-enforcer-config.enforcement-mode=PERMISSIVE
    keycloak.policy-enforcer-config.http-method-as-scope=true
    keycloak.policy-enforcer-config.lazy-load-paths=true

The *keycloak.policy-enforcer-config.http-method-as-scope* property tells the
Keycloak adapter to use the REST request method as the scope for evaluating the
resource request. Hence, when a user performs a GET on the */api/instances* 
endpoint, the *get_api_instances* will be activated and allow access to the
resource only if the user has the *get_api_instances* role. Awesome!!!

### Roles
Because keycloak is not great in importing the client roles required for this
operation, a list of all currently used endpoint roles is provided below:

#### Endpoint Roles
| Role Name | Role Description |
| ----------| ---------------- |
| delete_api_doc | Users are allowed API requests to delete an existing doc |
| delete_api_instance | Users are allowed API requests to delete an existing instance |
| delete_api_xml | Users are allowed API requests to delete an existing xml |
| get_api_doc | Users are allowed API requests to retrieve an existing doc |
| get_api_docs | Users are allowed API requests to retrieve the docs |
| get_api_docs_dt | Users are allowed API requests to retrieve the docs in a datatables format |
| get_api_instance | Users are allowed API requests to retrieve a single instance |
| get_api_instances | Users are allowed API requests to retrieve the instances |
| get_api_instances_dt | Users are allowed API requests to retrieve the instances in a datatables format |
| get_api_search_instances | Users are allowed API requests to search for instances |
| get_api_xml | Users are allowed API requests to retrieve an existing xml |
| get_api_xmls | Users are allowed API requests to retrieve the xmls |
| get_api_xmls_dt | Users are allowed API requests to retrieve the xmls in a datatables format |
| post_api_docs | Users are allowed API requests to create new docs |
| post_api_instances | Users are allowed API requests to create new instances |
| post_api_xmls | Users are allowed API requests to create new xmls |
| put_api_doc | Users are allowed API requests to update an existing doc |
| put_api_instance | Users are allowed API requests to update an existing instance |
| put_api_instance_ledger_status | Users are allowed API requests to update the ledger status of existing instances |
| put_api_instance_status | Users are allowed API requests to update the status of existing instances |
| put_api_xml | Users are allowed API requests to update an existing xml |

#### Composite Roles
| Role Name | Role Description |
| ----------| ---------------- |
| user | Users are allowed to login and view the local service registry |
| service_admin | Users are allows to create and upload new services and change their local status |
| ledger_admin | Users are allowed to control instances and upload them to the global MSR ledger |
| admin | MSR site administrator role |

## Contributing
Pull requests are welcome. For major changes, please open an issue first to
discuss what you would like to change.

Please make sure to update tests as appropriate.

## License
Distributed under the Apache License. See [LICENSE](./LICENSE) for more
information.

## Maintainer
Nikolaos Vastardis - Nikolaos.Vastardis@gla-rad.org

## Acknowledgement

The development is a part of the project titled “Development of Open Platform Technologies for Smart Maritime Safety and Industries” funded by the Korea Research Institute of Ships and Ocean Engineering (PES4070).

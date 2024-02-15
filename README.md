# Maritime Connectivity Platform Service Registry
This is the implementation of the MCP Service Registry (MSR). It is under the 
Apache 2.0 License.

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
The MCP Service Registry is built using the Java Springboot framework and
required a PostgreSQL database with a PostGIS extension. More information 
on how to download and install PostGIS can he found 
[here](https://postgis.net/source/).

## Database Configuration
The service uses hibernate to initialise the database structure. The database
connection parameters such as the URL and username/password should be provided
in the *application.yaml* file found in the resources' folder. Here
is an example:

```yaml
spring:
    datasource:
        url: 'jdbc:postgresql://localhost:5432/mcp_service_registry'
        username: sysadmin
        password: sysadmin
```

To create a local database for development/testing you should install a
PostgreSQL server and the postGIS extension. On an Ubuntu system this 
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

```
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
The current version of the MSR is using [Keycloak](https://www.keycloak.org/)
for access management (version 21.1.2+). The Spring OIDC client is used to 
link the service to the authentication server. Therefore, before running the 
service you will need to create a security realm in Keycloak and setup a client
service. To get things going faster, the required client configuration can be
found in the [service-registry.json](src/main/resources/service-registry.json)
file. Note that the client's access type is *confidential*, so you might need
to regenerate the client's secret. Once the service is ready in the Keycloak
service, you will need to connect the service to it using the relevant section
of the *application.yaml* configuration file.

Note that an additional spring security entry is required for the feign client
configuration. This is required if you need to link the MSR with an MCP MIR
identity registry so that the service can also retrieve certificates for the
registered instances.

```yaml
# Keycloak Configuration
spring:
    security:
        oauth2:
            client:
                registration:
                    keycloak:
                        client-id: mcp-service-registry
                        client-secret: xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
                        client-name: Keycloak
                        provider: keycloak
                        authorization-grant-type: authorization_code
                        scope: web-origins,openid
                        redirect-uri: "{baseUrl}/login/oauth2/code/{registrationId}"
                    feign:
                        client-id: mcp-service-registry
                        client-secret: xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
                        client-name: Feign
                        provider: feign
                        authorization-grant-type: client_credentials
                        scope: web-origins,openid
                provider:
                    keycloak:
                        issuer-uri: 'http://localhost:8090/auth/realms/realm'
                        user-name-attribute: preferred_username
                    feign:
                        token-uri: 'http://localhost:8090/auth/realms/realm/protocol/openid-connect/token'
            resource-server:
                jwt:
                    issuer-uri: 'http://localhost:8090/auth/realms/realm'
```

## Docker Container
A version of the MCP Service Registry is also available via
[DockerHub](https://hub.docker.com/repository/docker/glarad/mc-service-registry/general).
This can be run as it is, or through a Docker-Compose script. The container
assumes a *docker* yaml file is provided and runs on that profile. By default,
all configurations should be provided under a *conf* directory linked to the
root of the container. For example, to run the container you can use the
following command:

```bash
sudo docker run -t -i --rm -p 8444:8444 -v /path/to/config-directory/on/machine:/conf <image-id>
```

For more information please have a look at the MSR docker
[overview.md](docker/overview.md) file.

An example docker-profile YAML configuration can be found below:

```yaml
server:
    port: '8444'
    servlet:
        context-path: ${service.variable.contextPath}

# Springboot Configuration
spring:
    application:
        name: mcp-service-registry
    jpa:
        properties:
            hibernate:
                search:
                    backend:
                        lucene_version: LATEST
                        directory:
                            root: ./lucene/
                        analysis:
                            configurer: >-
                                class:net.maritimeconnectivity.serviceregistry.config.MSRLuceneAnalysisConfigurer
                    schema_management:
                        strategy: create-or-update
        generate-ddl: true
        hibernate:
            ddl-auto: update
            show-sql: true
    datasource:
        url: jdbc:${service.variable.datasource.server.type}://${service.variable.datasource.server.host}:${service.variable.datasource.server.port}/${service.variable.datasource.database.name}
        username: ${service.variable.datasource.database.username}
        password: ${service.variable.datasource.database.password}
    flyway:
        enabled: false
        url: jdbc:${service.variable.datasource.server.type}://${service.variable.datasource.server.host}:${service.variable.datasource.server.port}/${service.variable.datasource.database.name}
        schemas: mcp_service_registry
        user: ${service.variable.datasource.database.username}
        password: ${service.variable.datasource.database.password}

    # Keycloak Configuration
    security:
        oauth2:
            client:
                registration:
                    keycloak:
                        client-id: ${service.variable.keycloak.client.id}
                        client-secret: ${service.variable.keycloak.client.secret}
                        client-name: Keycloak
                        provider: keycloak
                        authorization-grant-type: authorization_code
                        scope: web-origins,openid
                        redirect-uri: "{baseUrl}/login/oauth2/code/{registrationId}"
                    feign:
                        client-id: ${service.variable.keycloak.client.id}
                        client-secret: ${service.variable.keycloak.client.secret}
                        client-name: Feign
                        provider: feign
                        authorization-grant-type: client_credentials
                        scope: web-origins,openid
                provider:
                    keycloak:
                        issuer-uri: ${service.variable.keycloak.server.url}/auth/realms/${service.variable.keycloak.server.realm}
                        user-name-attribute: preferred_username
                    feign:
                        token-uri: ${service.variable.keycloak.server.url}/auth/realms/${service.variable.keycloak.server.realm}/protocol/openid-connect/token
            resource-server:
                jwt:
                    issuer-uri: ${service.variable.keycloak.server.url}/auth/realms/${service.variable.keycloak.server.realm}

# Management Endpoints
management:
    endpoint:
        health:
            show-details: when_authorized
            probes:
                enabled: true

# Springdoc Configuration
springdoc:
    swagger-ui:
        path: /swagger-ui.html
        display-query-params: true
        url: /v3/api-docs
    packagesToScan: net.maritimeconnectivity.serviceregistry.controllers

# Springdoc Swagger Configuration
swagger:
    title: Maritime Connectivity Platform Service Registry API
    description: 'Maritime Connectivity Platform Service Registry, developed by the MCC MSR WG'
    version: ${spring.application.version}
    termsOfServiceUrl: 'null'
    contactName: MCP Consortium
    contactUrl: 'https://mcp.discourse.group/'
    contactEmail: Nikolaos.Vastardis@gla-rad.org
    license: Apache-2.0
    licenseUrl: 'http://www.apache.org/licenses/LICENSE-2.0'

# MCP Service Registry Configuration
net:
    maritimeconnectivity:
        serviceregistry:
            allowedContentTypes: >-
                application/pdf,application/vnd.openxmlformats-officedocument.wordprocessingml.document,application/vnd.oasis.opendocument.text
            mir:
                server-url: ${service.variable.mir.server.url}
            ledger:
                server-url: ws://${service.variable.ledger.server.host}:${service.variable.ledger.server.port}
                credentials: ${service.variable.ledger.server.credentials}
                contract-address: ${service.variable.ledger.server.address}

# Local Service Configuration
info:
    msr:
        name: ${service.variable.info.name}
        mrn: ${service.variable.info.mrn}'
        url: ${service.variable.info.url}
        operatorName: ${service.variable.info.operator.name}
        operatorMrn: ${service.variable.info.operator.mrn}
        operatorContact: ${service.variable.info.operator.contact}
        operatorUrl: ${service.variable.info.operator.url}
        copyright: 'Copyright © 2024 Maritime Connectivity Platform Consortium'
        projectLocation: 'https://github.com/maritimeconnectivity/ServiceRegistry'
        profile: ${spring.profiles.active:test}

eureka:
    client:
        enabled: false

# SECOM OpenAPI Config
swagger:
    secomOpenApiConfig: 'openapi.json'
```

In the last line of the configuration you can also see how you can provide a
SECOM OpenAPI configuration file. This will define how SpringDoc will also 
generate the OpenAPI specification for the SECOM interfaces which are provided
by the [SECOMLib](https://github.com/gla-rad/SECOMLib) library and are defined
using [JAX-RS](https://www.baeldung.com/jax-rs-spec-and-implementations).

## MIR Integration
Another important point concerns the MCP MIR integration. This functionality
is one of the latest features and allows the MSR to include the service instance
certificates in the SECOM *search service* responses. These are retrieved 
through internal calls to the MIR service, under OAuth2 authentication. In 
terms of the implementation of this functionality, it is achieved simply by
using a Spring Feign client, which supports all types of MIR entity queries
(such as services, devices, vessels, users and roles).

To allow these calls into the MIR, we first need to configure our Keycloak
server in order to enable a service account for the MSR. This account 
should then be updated with the required attributes like:

* **org** - containing the organisation ID (MRN) of the service, preferably the
same as the MIR provider organisation
* **permissions** - preferably this should be set to SITE_ADMIN to allow the
MSR to perform all operations
* **mrn** - the MRN of the service registry

Finally, we need to make sure the *mcp-client-template* scope is allocated by
default to the Keycloak client entry, so that all the attribute mappers are
picked up correctly.

On runtime, we need to provide the respective *application.yaml* property:

**net.maritimeconnectivity.serviceregistry.mir.server-url** 

with the api endpoint of the MIR and that should be it! Note that the service
instances registered in the MIR and the MSR should match, i.e. they should have
the same organisation and instance ID MRNs.

## Keycloak Policy Enforcer Configuration - Deprecated

Please note that for greater access management granularity, in Springboot 2 the
resource policy enforcer could be used. **In the latest upgrade to Springboot 3
however this functionality has been removed and cannot be used for now**. This
section is left here only as a reference for future development.

In the Keycloak policy enforcer operation every API resource of the service
should be registered with keycloak and a role, a policy and a permission
should be defined for it. For example, let's have a look at the endpoint that
returns the available instances: */api/instances*.

For this endpoint we have a resource registered in keycloak as follows:

```json
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
```

Notice that the same resource is used by both the GET and the POST HTTP REST
requests to allow users to also create new instances. In our example, for the
GET request, we have also defined a role called *get_api_instances*, a
policy with the same name for simplicity (*get_api_instances*) that checks
whether the user has that role, and finally a scope permission that grants
access to the endpoint if the user has the *get_api_instances* role.

```json
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
```

In order to enable the resource-level access management you will need to turn
on the *Authorization Enabled* setting in the main setting of the service
registry client. To keep things simple, the authorization configuration can be
found [here](src/main/resources/service-registry-authorization.json).
You can simply import the file in the *Authorization* tab of the client.

The last trick that binds everything together is again found in the
*application.yaml* file:

```yaml
# Start enforcing the policies defined in keycloak
keycloak:
    policy-enforcer-config:
        enforcement-mode: PERMISSIVE
        http-method-as-scope: true
        lazy-load-paths: true
```

The *keycloak.policy-enforcer-config.http-method-as-scope* property tells the
Keycloak adapter to use the REST request method as the scope for evaluating the
resource request. Hence, when a user performs a GET on the */api/instances*
endpoint, the *get_api_instances* will be activated and allow access to the
resource only if the user has the *get_api_instances* role. Awesome!!! Or at
least it used to be :).

### Roles
Because keycloak is not great in importing the client roles required for this
operation, a list of all currently used endpoint roles is provided below:

#### Endpoint Roles
| Role Name                      | Role Description                                                                 |
|--------------------------------|----------------------------------------------------------------------------------|
| delete_api_doc                 | Users are allowed API requests to delete an existing doc                         |
| delete_api_instance            | Users are allowed API requests to delete an existing instance                    |
| delete_api_xml                 | Users are allowed API requests to delete an existing xml                         |
| get_api_doc                    | Users are allowed API requests to retrieve an existing doc                       |
| get_api_docs                   | Users are allowed API requests to retrieve the docs                              |
| get_api_docs_dt                | Users are allowed API requests to retrieve the docs in a datatables format       |
| get_api_instance               | Users are allowed API requests to retrieve a single instance                     |
| get_api_instances              | Users are allowed API requests to retrieve the instances                         |
| get_api_instances_dt           | Users are allowed API requests to retrieve the instances in a datatables format  |
| get_api_search_instances       | Users are allowed API requests to search for instances                           |
| post_api_search_service        | Users are allowed API requests to search for instances using SECOM               |
| get_api_xml                    | Users are allowed API requests to retrieve an existing xml                       |
| get_api_xmls                   | Users are allowed API requests to retrieve the xmls                              |
| get_api_xmls_dt                | Users are allowed API requests to retrieve the xmls in a datatables format       |
| post_api_docs                  | Users are allowed API requests to create new docs                                |
| post_api_instances             | Users are allowed API requests to create new instances                           |
| post_api_xmls                  | Users are allowed API requests to create new xmls                                |
| put_api_doc                    | Users are allowed API requests to update an existing doc                         |
| put_api_instance               | Users are allowed API requests to update an existing instance                    |
| put_api_instance_ledger_status | Users are allowed API requests to update the ledger status of existing instances |
| put_api_instance_status        | Users are allowed API requests to update the status of existing instances        |
| put_api_xml                    | Users are allowed API requests to update an existing xml                         |

#### Composite Roles
| Role Name     | Role Description                                                                 |
|---------------|----------------------------------------------------------------------------------|
| user          | Users are allowed to login and view the local service registry                   |
| service_admin | Users are allows to create and upload new services and change their local status |
| ledger_admin  | Users are allowed to control instances and upload them to the global MSR ledger  |
| admin         | MSR site administrator role                                                      |

## Contributing
Pull requests are welcome. For major changes, please open an issue first to
discuss what you would like to change.

Please make sure to update tests as appropriate.

## License
Distributed under the Apache License 2.0. See [LICENSE](./LICENSE) for more
information.

## Maintainer
Nikolaos Vastardis - Nikolaos.Vastardis@gla-rad.org

## Acknowledgement
The development is a part of the project titled “Development of Open Platform
Technologies for Smart Maritime Safety and Industries” funded by the Korea
Research Institute of Ships and Ocean Engineering (PES4070).

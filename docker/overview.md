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
[here](https://postgis.net/documentation/getting_started/).

## How to use this image
In order to use this image you should run the container providing the necessary
configuration through the available environment parameters. There are briefly
shown in the following table:

| Variable               | Description                                            | Default Value                  |
|------------------------|--------------------------------------------------------|--------------------------------|
| DATABASE_SERVER_TYPE   | The type of the database the service should connect to | postgresql                     |
| DATABASE_SERVER_URL    | The URL location of the database server                | http://localhost:8080          |
| DATABASE_USERNAME      | The username to be used for the database connection    | admin                          |
| DATABASE_PASSWORD      | The password to be used for the database connection    | admin                          |
| KEYCLOAK_SERVER_HOST   | The host name of the keycloak server                   | 5672                           |
| KEYCLOAK_SERVER_PORT   | The port of the keycloak server runs on                | 8090                           |
| KEYCLOAK_CLIENT_REALM  | The OIDC realm to be used for authentication           | mcp                            |
| KEYCLOAK_CLIENT_ID     | The OIDC client ID to be used for authentication       | mcp-service-registry           |
| KEYCLOAK_CLIENT_SECRET | The OIDC client secret to be used for authentication   | N/A                            |
| MCP_MIR_URL            | The URL of the MCP MIR API to retrieve certificates    | http://localhost:8443/oidc/api |
| MCP_LEDGER_HOST        | The name of the database to connect to                 | localhost                      |
| MCP_LEDGER_PORT        | The username for the database connection               | 8546                           |
| MCP_LEDGER_ADDRESS     | The password for the database connection               | N/A                            |
| MCP_LEDGER_CREDENTIALS | The server hostname of the eureka server               | N/A                            |

The parameters will be picked up and used to populate the default
**bootstrap.yaml** of the service that look as follows:

    server:
    port: 8444
    
    spring:
    application:
    name: mcp-service-registry
    version: 0.0.4
    
    # Pick up the service environment variables
    service:
        variable:
            datasource:
                server:
                    type: ${DATABASE_SERVER_TYPE:postgresql}
                    host: ${DATABASE_SERVER_HOST:localhost}
                    port: ${DATABASE_SERVER_PORT:5432}
                database:
                    name: ${DATABASE_NAME:mcp_service_registry}
                    username: ${DATABASE_USERNAME:admin}
                    password: ${DATABASE_PASSWORD:admin}
            keycloak:
                server:
                    url: ${KEYCLOAK_SERVER_URL:http://localhost:8080}
                    realm: ${KEYCLOAK_CLIENT_REALM:mcp}
                client:
                    id: ${KEYCLOAK_CLIENT_ID:mcp_service_registry}
                    secret: ${KEYCLOAK_CLIENT_SECRET:secret}
            mir:
                server:
                    url: ${MCP_MIR_URL:http://localhost:8443/oidc/api}
            ledger:
                server:
                    host: ${MCP_LEDGER_HOST:localhost}
                    port: ${MCP_LEDGER_PORT:8546}
                    address: ${MCP_LEDGER_ADDRESS:0x0000000000000000000000000000000000000000}
                    credentials: ${MCP_LEDGER_CREDENTIALS:abcdef1234567890abcdef1234567890abcdef1234567890abcdef1234567890}
                info:
                    name: ${SERVICE_INFO_NAME:Test MSR Instance}
                    mrn: ${SERVICE_INFO_MRN:urn:mrn:mcp:msr:test:testMSR1}
                    url: ${SERVICE_INFO_URL:http://localhost:8444}
                operator:
                    name: ${SERVICE_INFO_OPERATOR:Maritime Connectivity Platform}
                    contact: ${SERVICE_INFO_OPERATOR_CONTACT:secretariat@maritimeconnectivity.net}
                    mrn: ${SERVICE_INFO_OPERATOR_MRN:urn:mrn:mcp:org:test:testMSROperator1}
                    url: ${SERVICE_INFO_OPERATOR_URL:https://maritimeconnectivity.net/}

As you can see, the service is called **mcp-service-registry** and uses the
**8444** port when running.

To run the image, along with the aforementioned environment variables, you can
use the following command. Please make sure to replace the appropriate variables
with the correct values for your setup.

    docker run -t -i --rm \
        -p 8444:8444 \
        -e DATABASE_SERVER_TYPE='postgresql' \
        -e DATABASE_SERVER_HOST='localhost' \
        -e DATABASE_SERVER_PORT='5432' \
        -e DATABASE_USERNAME='admin' \
        -e DATABASE_PASSWORD='admin' \
        -e KEYCLOAK_SERVER_URL='http://localhost:8080' \
        -e KEYCLOAK_CLIENT_REALM='mcp' \
        -e KEYCLOAK_CLIENT_ID='mcp-service-registry' \
        -e KEYCLOAK_CLIENT_SECRET='secret' \
        -e MCP_MIR_URL='http://localhost:8443/oidc/api' \
        -e MCP_LEDGER_HOST='localhost' \
        -e MCP_LEDGER_PORT='8546' \
        -e MCP_LEDGER_ADDRESS='0x0000000000000000000000000000000000000000' \
        -e MCP_LEDGER_CREDENTIALS='abcdef1234567890abcdef1234567890abcdef123567890abcdef1234567890' \
        <image-id>

## Operation
The MSR does not provide actual maritime information but a specification of
various services, the information that they carry, and the technical means to
obtain it. An MSR instance contains service specifications according to a
Service Specification Standard (which is identical to IALA
[Guideline 1128](https://www.iala-aism.org/product/g1128/)) and provisioned
service instances implemented according to these service specifications.

The functionality of the MSR is twofold: service discovery and service
management. It enables service providers to register their services in the MCP
and allows an end-user to discover those services. Services and service
instances can be searched via different criteria such as keywords,
organizations, locations, or combinations, and more. The management of a service
encapsulates the functions to publish a service specification and register /
publish a service instance.

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

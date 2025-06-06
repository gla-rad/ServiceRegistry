# Build like this:
#     docker build -t <version> -f Dockerfile ..
# e.g.
#     docker build -t maritimeconnectivity/mc-service-registry:latest -f Dockerfile ..
#
# Run like this:
#     sudo docker run -t -i --rm -p 8444:8444 -v /path/to/config-directory/on/machine:/conf <image-id>
#
# You can also push to docker-hub like:
#     docker push glarad/mc-service-registry:tagname
#
# A customized conf file (application.yaml) must be available in the folder mounted to /conf.
# When using in non-local environment it is recommended to generate new trust and keystores and place them in
# the conf-folder and point to them in application.yaml.
#

# Use the official OpenJDK 21 image as the base image
FROM eclipse-temurin:21-jre-alpine

LABEL org.opencontainers.image.source="https://github.com/maritimeconnectivity/ServiceRegistry"

# Set the working directory in the container
WORKDIR /app

# Copy the Spring Boot JAR file into the container
COPY target/mcp-serviceregistry-core-0.0.8.jar /app/

# Create a modules path just in case
ENV LOADER_PATH /modules
RUN mkdir $LOADER_PATH

# Define a variable for the configuration directory
ENV CONFIG_DIR=/conf

EXPOSE 8444

CMD ["java", "-jar", "mcp-serviceregistry-core-0.0.8.jar", "--spring.config.location=optional:classpath:/,optional:file:${CONFIG_DIR}/"]

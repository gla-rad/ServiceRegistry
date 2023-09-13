# Build like this:
# docker build -t <version> -f Dockerfile ..
#
# Run like this:
# sudo docker run -t -i --rm -p 8444:8444 -v /path/to/config-directory/on/machine:/conf <image-id>
# 
# A customized conf file (application.yaml) must be available in the folder mounted to /conf.
# When using in non-local environment it is recommended to generate new trust and keystores and place them in
# the conf-folder and point to them in application.yaml.
FROM eclipse-temurin:17-jre

LABEL org.opencontainers.image.source="https://github.com/maritimeconnectivity/ServiceRegistry"

ENV LOADER_PATH /modules

RUN mkdir /conf

RUN mkdir $LOADER_PATH

ADD target/mcp-serviceregistry-core-0.0.4.jar .

EXPOSE 8444

CMD ["java", "-Dspring.profiles.active=docker", "-jar", "mcp-serviceregistry-core-0.0.4.jar", "--spring.config.location=classpath:/application.yaml,/conf/"]

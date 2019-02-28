FROM websphere-liberty:microProfile1
# Install opentracing usr feature
COPY server.xml /config/server.xml

# Don't fail on rc 22 feature already installed
RUN installUtility install --acceptLicense defaultServer || if [ $? -ne 22 ]; then exit $?; fi

COPY jvm.options /config/jvm.options

COPY target/acmeair-flightservice-java-2.0.0-SNAPSHOT.war /config/apps/


USER root
ENV DEBIAN_FRONTEND=noninteractive
RUN apt-get update && apt-get install -y tzdata && ln -fs /usr/share/zoneinfo/America/Chicago /etc/localtime && dpkg-reconfigure --frontend noninteractive tzdata
USER 1001

RUN server start && server stop


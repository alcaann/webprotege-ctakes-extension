FROM maven:3.6.0-jdk-11-slim AS build

# Remove all sources.list.d files, add only the necessary archive repos, and disable valid-until check
RUN rm -rf /etc/apt/sources.list.d/* \
    && echo 'deb http://archive.debian.org/debian stretch main contrib non-free\ndeb http://archive.debian.org/debian-security stretch/updates main contrib non-free' > /etc/apt/sources.list \
    && echo 'Acquire::Check-Valid-Until "false";' > /etc/apt/apt.conf.d/99no-check-valid-until \
    && apt-get update \
    && apt-get install -y git mongodb

COPY . /webprotege

WORKDIR /webprotege

RUN mkdir -p /data/db \
    && mongod --fork --syslog \
    && mvn clean package

FROM tomcat:8-jre11-slim

RUN rm -rf /usr/local/tomcat/webapps/* \
    && mkdir -p /srv/webprotege \
    && mkdir -p /usr/local/tomcat/webapps/ROOT

WORKDIR /usr/local/tomcat/webapps/ROOT

COPY --from=build /webprotege/webprotege-cli/target/webprotege-cli-4.0.2.jar /webprotege-cli.jar
COPY --from=build /webprotege/webprotege-server/target/webprotege-server-4.0.2.war ./webprotege.war
RUN unzip webprotege.war \
    && rm webprotege.war

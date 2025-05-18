#!/bin/bash

# Navigate to the webprotege directory
cd /webprotege

echo "Building WebProtege..."
# Build the Maven project
mvn clean install -DskipTests

echo "Deploying to Tomcat..."
# Copy the built artifacts to Tomcat webapps
rm -rf /usr/share/tomcat9/webapps/ROOT/*
mkdir -p /usr/share/tomcat9/webapps/ROOT
cp webprotege-server/target/webprotege-server-4.0.2.war /usr/share/tomcat9/webapps/ROOT/webprotege.war
cd /usr/share/tomcat9/webapps/ROOT
unzip webprotege.war
rm webprotege.war

# Copy the CLI jar
cp /webprotege/webprotege-cli/target/webprotege-cli-4.0.2.jar /webprotege-cli.jar

echo "Starting Tomcat..."
# Start Tomcat
service tomcat9 start

echo "WebProtege is now running!"
echo "Access it at http://localhost:5000"
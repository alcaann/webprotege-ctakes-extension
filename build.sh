#!/bin/bash

# Navigate to the webprotege directory
cd /webprotege

# Check if the build was successful
if [ ! -f webprotege-server/target/webprotege-server-4.0.2.war ]; then
    echo "Build failed: WAR file not created"
    exit 1
fi

# Set default values for configuration
MONGO_HOST=${WEBPROTEGE_MONGODB_HOST:-wpmongo}
DATA_DIR=${WEBPROTEGE_DATA_DIR:-/srv/webprotege}

# Create data directory
mkdir -p $DATA_DIR
chmod 777 $DATA_DIR

echo "Deploying to Tomcat with MongoDB host: $MONGO_HOST and data directory: $DATA_DIR..."

# Clean and prepare deployment directory
rm -rf /usr/share/tomcat9/webapps/ROOT/* 2>/dev/null
mkdir -p /usr/share/tomcat9/webapps/ROOT

# Extract the WAR file to ROOT directory
cp webprotege-server/target/webprotege-server-4.0.2.war /usr/share/tomcat9/webapps/ROOT/webprotege.war
cd /usr/share/tomcat9/webapps/ROOT
unzip -o webprotege.war
rm webprotege.war

# Copy the CLI jar
cp /webprotege/webprotege-cli/target/webprotege-cli-4.0.2.jar /webprotege-cli.jar 2>/dev/null

# Configure Tomcat
echo "Configuring Tomcat..."
mkdir -p /usr/share/tomcat9/conf

# Copy server.xml and web.xml from our prepared configs
cp /webprotege/conf/server.xml /usr/share/tomcat9/conf/server.xml
cp /webprotege/tomcat8_default_conf/web.xml /usr/share/tomcat9/conf/web.xml

# Ensure the properties file in the deployed webapp is updated
echo "Configuring WebProtege properties..."
mkdir -p /usr/share/tomcat9/webapps/ROOT/WEB-INF/classes


# Check MongoDB connectivity
echo "Checking MongoDB connectivity..."
if ! command -v nc &>/dev/null; then
    apt-get update -qq && apt-get install -y netcat > /dev/null 2>&1
fi

if nc -z -w 5 $MONGO_HOST 27017; then
    echo "MongoDB is reachable at $MONGO_HOST:27017"
else
    echo "WARNING: Cannot connect to MongoDB at $MONGO_HOST:27017"
    echo "Make sure MongoDB is running and network connectivity is configured correctly."
fi

echo "Starting Tomcat..."
/usr/share/tomcat9/bin/catalina.sh run

echo "WebProtege is now running!"
echo "Access it at http://localhost:8080 or http://localhost:5000"
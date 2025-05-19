#!/bin/bash
#
# WebProtege Installation Script
# This script builds, deploys, and configures WebProtege with Tomcat and MongoDB
#

# Exit on error
set -e

# Configuration (can be overridden with environment variables)
# Default MongoDB host is 'wpmongo' for Docker environment
MONGO_HOST=${WEBPROTEGE_MONGODB_HOST:-wpmongo}
DATA_DIR=${WEBPROTEGE_DATA_DIR:-/srv/webprotege}
TOMCAT_HOME=${TOMCAT_HOME:-/usr/share/tomcat9}

# Check prerequisites
command -v java >/dev/null 2>&1 || { echo "Java is required but not installed. Aborting."; exit 1; }

echo "=== WebProtege Installation ==="
echo "MongoDB Host: $MONGO_HOST"
echo "Data Directory: $DATA_DIR"
echo "Tomcat Home: $TOMCAT_HOME"

# Navigate to the webprotege directory
cd /webprotege

# Step 0: Build WebProtege
echo "=== Building WebProtege ==="
#mvn package -Dmaven.test.skip=true -DskipTests=true -Dgwt.draftCompile=true -Dgwt.localWorkers=8
mvn -T 1C install -P dev -Dmaven.test.skip=true -DskipTests=true -pl webprotege-client -am


# Step 1: Check if the build was successful
if [ ! -f webprotege-server/target/webprotege-server-4.0.2.war ]; then
    echo "Build artifacts not found. Build WebProtege first with 'mvn clean package'."
    exit 1
fi

# Step 2: Create and prepare data directory
echo "=== Preparing Data Directory ==="
mkdir -p $DATA_DIR
chmod 777 $DATA_DIR
echo "Data directory created at $DATA_DIR"

# Step 3: Deploy to Tomcat
echo "=== Deploying to Tomcat ==="
# Clean and prepare deployment directory
rm -rf $TOMCAT_HOME/webapps/ROOT/* 2>/dev/null || true
mkdir -p $TOMCAT_HOME/webapps/ROOT

# Extract the WAR file to ROOT directory
cp webprotege-server/target/webprotege-server-4.0.2.war $TOMCAT_HOME/webapps/ROOT/webprotege.war
cd $TOMCAT_HOME/webapps/ROOT
unzip -q -o webprotege.war
rm webprotege.war
echo "WebProtege deployed to Tomcat ROOT context"

# Also deploy the CLI tool
cp /webprotege/webprotege-cli/target/webprotege-cli-4.0.2.jar /webprotege-cli.jar 2>/dev/null || true
echo "WebProtege CLI deployed to /webprotege-cli.jar"

# Step 4: Configure Tomcat
echo "=== Configuring Tomcat ==="
mkdir -p $TOMCAT_HOME/conf

# Copy server.xml without AJP connector
cp /webprotege/conf/server.xml $TOMCAT_HOME/conf/server.xml

# Copy web.xml 
cp /webprotege/tomcat8_default_conf/web.xml $TOMCAT_HOME/conf/web.xml

echo "Tomcat configuration files installed"


# Step 6: Check MongoDB connectivity
echo "=== Checking MongoDB Connectivity ==="
if ! command -v nc &>/dev/null; then
    echo "Installing netcat for connectivity checks..."
    apt-get update -qq && apt-get install -y netcat > /dev/null 2>&1
fi

echo "Testing connection to MongoDB at $MONGO_HOST:27017..."
if nc -z -w 5 $MONGO_HOST 27017; then
    echo "SUCCESS: MongoDB is reachable at $MONGO_HOST:27017"
else
    echo "WARNING: Cannot connect to MongoDB at $MONGO_HOST:27017"
    echo "Make sure MongoDB is running and network connectivity is properly configured."
    echo "If using Docker, ensure containers are on the same network."
    echo "Continuing anyway, but WebProtege may not start correctly..."
fi

# Step 7: Start Tomcat
echo "=== Starting Tomcat ==="
if [ -f $TOMCAT_HOME/bin/catalina.sh ]; then
    echo "Starting Tomcat with catalina.sh"
    $TOMCAT_HOME/bin/catalina.sh run
else
    echo "ERROR: Tomcat catalina.sh not found at $TOMCAT_HOME/bin/catalina.sh"
    echo "Please check your Tomcat installation"
    exit 1
fi

# This section won't execute until Tomcat is stopped
echo "WebProtege has been installed and started!"
echo "Access it at http://localhost:8080"

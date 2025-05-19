#!/bin/bash

# MongoDB configuration script for WebProtege
# This script updates the deployed WebProtege configuration to use the MongoDB container

# Get the MongoDB host from environment variable or default to "wpmongo"
MONGO_HOST=${WEBPROTEGE_MONGODB_HOST:-wpmongo}
PROPERTIES_FILE="/usr/share/tomcat9/webapps/ROOT/WEB-INF/classes/webprotege.properties"

# Check if the properties file exists
if [ ! -f "$PROPERTIES_FILE" ]; then
    echo "ERROR: WebProtege properties file not found at $PROPERTIES_FILE"
    echo "Make sure WebProtege is properly deployed before running this script"
    exit 1
fi

# Update the MongoDB host in the deployed properties file
echo "Updating MongoDB host to $MONGO_HOST in $PROPERTIES_FILE"
sed -i "s/mongodb.host=.*/mongodb.host=$MONGO_HOST/" "$PROPERTIES_FILE"

# Verify the change
CURRENT_HOST=$(grep "mongodb.host" "$PROPERTIES_FILE" | grep -v "#" | cut -d'=' -f2)
echo "MongoDB host is now set to: $CURRENT_HOST"

# Test connectivity to MongoDB
echo "Testing connection to MongoDB at $MONGO_HOST:27017..."
if nc -z -w 5 "$MONGO_HOST" 27017; then
    echo "SUCCESS: MongoDB is reachable at $MONGO_HOST:27017"
else
    echo "WARNING: Cannot connect to MongoDB at $MONGO_HOST:27017"
    echo "Make sure MongoDB is running and network connectivity is properly set up"
    echo "If MongoDB is in another container, make sure the container name resolution works"
fi

echo "MongoDB configuration complete."

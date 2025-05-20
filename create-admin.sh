#!/bin/bash

# Create admin account script for WebProtege
# This script runs the webprotege-cli JAR to create an admin account without rebuilding the project

# Check if the CLI JAR exists
if [ ! -f /webprotege/webprotege-cli/target/webprotege-cli-4.0.2.jar ]; then
  echo "The WebProtege CLI JAR does not exist. Please build it first."
  exit 1
fi

# Run the CLI JAR with create-admin-account command
echo "Starting WebProtege admin account creation..."
java -jar /webprotege/webprotege-cli/target/webprotege-cli-4.0.2.jar create-admin-account

echo "Admin account creation process completed."

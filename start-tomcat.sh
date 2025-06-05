#!/bin/bash
#
# Tomcat Startup Script
# This script starts the Tomcat server for WebProtege
#

# Exit on error
set -e

# Configuration (can be overridden with environment variables)
TOMCAT_HOME=${TOMCAT_HOME:-/usr/share/tomcat9}

echo "=== Starting Tomcat ==="
echo "Tomcat Home: $TOMCAT_HOME"

if [ -f $TOMCAT_HOME/bin/catalina.sh ]; then
    echo "Starting Tomcat with catalina.sh"
    $TOMCAT_HOME/bin/catalina.sh run
else
    echo "ERROR: Tomcat catalina.sh not found at $TOMCAT_HOME/bin/catalina.sh"
    echo "Please check your Tomcat installation"
    exit 1
fi

# This section won't execute until Tomcat is stopped
echo "Tomcat has stopped."

#!/bin/bash

echo "====================================================="
echo "WebProtégé Development Container Startup"
echo "====================================================="

# Stage 1: Run installation (without starting Tomcat)
echo "Stage 1: Running WebProtégé installation..."
cd /webprotege
./install.sh

# Stage 2: Run admin account setup
echo "Stage 2: Setting up admin account and application settings..."
./admin-account-setup.sh

# Stage 3: Start Tomcat server
echo "Stage 3: Starting Tomcat server..."
exec ./start-tomcat.sh
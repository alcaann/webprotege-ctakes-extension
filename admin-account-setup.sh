#!/bin/bash
# Admin account setup script for WebProtégé

# Exit on error
set -e

CLI_JAR="/webprotege-cli.jar"
BUILT_CLI_JAR="/webprotege/webprotege-cli/target/webprotege-cli-4.0.2.jar"

# Check if the CLI JAR exists in the deployment location
if [ -f "$CLI_JAR" ]; then
    echo "Using deployed WebProtégé CLI JAR"
    JAR_PATH="$CLI_JAR"
# Check if the CLI JAR exists in the build directory
elif [ -f "$BUILT_CLI_JAR" ]; then
    echo "Using WebProtégé CLI JAR from build directory"
    JAR_PATH="$BUILT_CLI_JAR"
else
    echo "WebProtégé CLI JAR not found. Building project..."
    cd /webprotege
    mvn -T 1C install -P dev -Dmaven.test.skip=true -DskipTests=true -pl webprotege-cli -am
    
    if [ -f "$BUILT_CLI_JAR" ]; then
        echo "Build successful"
        JAR_PATH="$BUILT_CLI_JAR"
    else
        echo "Failed to build WebProtégé CLI. Please check your project setup."
        exit 1
    fi
fi

echo "=== Creating WebProtégé Admin Account ==="
echo "You will be prompted to enter the admin account details."
echo

# Run the CLI JAR with create-admin-account command
java -jar "$JAR_PATH" create-admin-account

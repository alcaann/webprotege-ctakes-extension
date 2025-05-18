#!/bin/bash

echo "====================================================="
echo "Development environment is ready!"
echo "====================================================="
echo "To build and run WebProtege, run:"
echo "./build.sh"
echo ""
echo "This container is ready for VS Code Dev Containers"
echo "You can now attach VS Code to this container and"
echo "develop your WebProtege application."
echo "====================================================="Start MongoDB for development
mongod --fork --syslog

echo "====================================================="
echo "Development environment is ready!"
echo "====================================================="
echo "To build and run WebProtege, run:"
echo "./build.sh"
echo ""
echo "This container is ready for VS Code Dev Containers"
echo "You can now attach VS Code to this container and"
echo "develop your WebProtege application."
echo "====================================================="

# Keep the container running
tail -f /dev/null
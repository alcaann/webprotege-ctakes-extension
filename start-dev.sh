// filepath: c:\d.TFG\webprotege-ctakes-extension\start-dev.sh
#!/bin/bash

echo "====================================================="
echo "WebProtégé Development Container is ready!"
echo "Project source is at /webprotege (mounted from host)."
echo "MongoDB is running in a separate container (wpmongo)."
echo "====================================================="
echo "For GWT Super Dev Mode:"
echo "1. Open a terminal and run: cd /webprotege && mvn gwt:codeserver -Dgwt.persistentunitcache=true -Dgwt.persistentunitcachedir=/webprotege/.gwt_cache -Dgwt.draftCompile=true -Dgwt.localWorkers=4"
echo "2. Open another terminal and run: cd /webprotege && mvn -Denv=dev tomcat7:run"
echo "   (Ensure pom.xml is configured for tomcat7-maven-plugin or similar for dev)"
echo "Alternatively, use VS Code tasks if configured."
echo "====================================================="
echo "To build and deploy a WAR to the container's Tomcat 9 (for non-GWT dev testing):"
echo "Run: /webprotege/build.sh"
echo "====================================================="

# Keep the container running
tail -f /dev/null
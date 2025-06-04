FROM maven:3.9.6-eclipse-temurin-11

# Install development tools
RUN apt-get update \
    && apt-get install -y git tomcat9 unzip curl \
    && rm -rf /var/lib/apt/lists/*

# Set up Tomcat directories
RUN rm -rf /usr/share/tomcat9/webapps/* \
    && mkdir -p /srv/webprotege \
    && mkdir -p /usr/share/tomcat9/webapps/ROOT

# Set up development workspace
WORKDIR /webprotege

# Copy POM files first to leverage Docker cache for dependencies
COPY pom.xml /webprotege/pom.xml
COPY webprotege-shared-core/pom.xml /webprotege/webprotege-shared-core/pom.xml
COPY webprotege-shared/pom.xml /webprotege/webprotege-shared/pom.xml
COPY webprotege-server-core/pom.xml /webprotege/webprotege-server-core/pom.xml
COPY webprotege-client/pom.xml /webprotege/webprotege-client/pom.xml
COPY webprotege-cli/pom.xml /webprotege/webprotege-cli/pom.xml
COPY webprotege-server/pom.xml /webprotege/webprotege-server/pom.xml

# Stage 0: Install parent POM (required by all modules)
RUN mvn install -N \
    -Dmaven.test.skip=true -DskipTests=true -Dmaven.javadoc.skip=true -B

# Download external dependencies for all modules (this helps with caching)
# Note: Internal module dependencies will be resolved as we build each module
RUN mvn dependency:go-offline \
    -Dmaven.test.skip=true -DskipTests=true -B || true

# Stage 1: Build webprotege-shared-core (no internal dependencies)
COPY webprotege-shared-core/src /webprotege/webprotege-shared-core/src
RUN mvn -pl webprotege-shared-core \
    install source:jar \
    -Dmaven.test.skip=true -DskipTests=true -Dmaven.javadoc.skip=true -B

# Stage 2: Build webprotege-shared (depends on shared-core)
COPY webprotege-shared/src /webprotege/webprotege-shared/src
RUN mvn -pl webprotege-shared \
    install source:jar \
    -Dmaven.test.skip=true -DskipTests=true -Dmaven.javadoc.skip=true -B

# Stage 3: Build webprotege-server-core (depends on shared)
COPY webprotege-server-core/src /webprotege/webprotege-server-core/src
RUN mvn -pl webprotege-server-core \
    install \
    -Dmaven.test.skip=true -DskipTests=true -Dmaven.javadoc.skip=true -B

# Stage 4: Build webprotege-client (depends on shared modules, can build in parallel with server)
COPY webprotege-client/src /webprotege/webprotege-client/src
# Download client-specific dependencies (especially GWT-related ones)
RUN mvn -pl webprotege-client dependency:go-offline \
    -Dmaven.test.skip=true -DskipTests=true -B || true
RUN mvn -pl webprotege-client \
    install \
    -Dmaven.test.skip=true -DskipTests=true -Dmaven.javadoc.skip=true -Dgwt.compiler.skip=true -B

# Stage 5: Build webprotege-server (depends on server-core)
COPY webprotege-server/src /webprotege/webprotege-server/src
# Download server-specific dependencies 
RUN mvn -pl webprotege-server dependency:go-offline \
    -Dmaven.test.skip=true -DskipTests=true -B || true
RUN mvn -pl webprotege-server \
    install \
    -Dmaven.test.skip=true -DskipTests=true -Dmaven.javadoc.skip=true -B

# Stage 6: Build webprotege-cli (depends on server-core, can build in parallel with server)
COPY webprotege-cli/src /webprotege/webprotege-cli/src
RUN mvn -pl webprotege-cli \
    install \
    -Dmaven.test.skip=true -DskipTests=true -Dmaven.javadoc.skip=true -B


# Copy start script (using regular COPY instead of --from)
COPY start-dev.sh /webprotege/

# Make the script executable
RUN chmod +x /webprotege/start-dev.sh

# Copy built-in-prefixes.csv to the expected location to avoid classpath extraction issues
RUN cp /webprotege/webprotege-server-core/src/main/resources/built-in-prefixes.csv /srv/webprotege/

# If there's a war file to be deployed, use a regular CP command
# Uncomment if webprotege-server produces a WAR file to deploy
# RUN cp /webprotege/webprotege-server/target/webprotege-server-*.war /usr/share/tomcat9/webapps/ROOT.war

# Entry point to keep the container running
CMD ["/webprotege/start-dev.sh"]
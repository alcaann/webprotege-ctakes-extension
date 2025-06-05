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

# Copy ALL POM files first (this layer will be cached unless POMs change)
COPY pom.xml /webprotege/pom.xml
COPY webprotege-shared-core/pom.xml /webprotege/webprotege-shared-core/pom.xml
COPY webprotege-shared/pom.xml /webprotege/webprotege-shared/pom.xml
COPY webprotege-server-core/pom.xml /webprotege/webprotege-server-core/pom.xml
COPY webprotege-client/pom.xml /webprotege/webprotege-client/pom.xml
COPY webprotege-cli/pom.xml /webprotege/webprotege-cli/pom.xml
COPY webprotege-server/pom.xml /webprotege/webprotege-server/pom.xml

# Install parent POM
RUN mvn install -N \
    -Dmaven.test.skip=true -DskipTests=true -Dmaven.javadoc.skip=true -B

# Download ALL external dependencies for ALL modules in one go
# This creates a single cached layer with all external dependencies
RUN mvn org.apache.maven.plugins:maven-dependency-plugin:3.5.0:go-offline \
    -Dmaven.test.skip=true -DskipTests=true -B

# Now copy source files and build each module using offline mode (-o flag)
# Each of these steps will only rebuild if the corresponding source changes

# Stage 1: Build webprotege-shared-core (no internal dependencies)
COPY webprotege-shared-core/src /webprotege/webprotege-shared-core/src
RUN mvn -pl webprotege-shared-core \
    install source:jar -o \
    -Dmaven.test.skip=true -DskipTests=true -Dmaven.javadoc.skip=true -B

# Stage 2: Build webprotege-shared (depends on shared-core)
COPY webprotege-shared/src /webprotege/webprotege-shared/src
RUN mvn -pl webprotege-shared \
    install source:jar -o \
    -Dmaven.test.skip=true -DskipTests=true -Dmaven.javadoc.skip=true -B

# Stage 3: Build webprotege-server-core (depends on shared)
COPY webprotege-server-core/src /webprotege/webprotege-server-core/src
RUN mvn -pl webprotege-server-core \
    install -o \
    -Dmaven.test.skip=true -DskipTests=true -Dmaven.javadoc.skip=true -B

# Stage 4: Build webprotege-client (depends on shared modules)
COPY webprotege-client/src /webprotege/webprotege-client/src
RUN mvn -pl webprotege-client \
    install \
    -Dmaven.test.skip=true -DskipTests=true -Dmaven.javadoc.skip=true -Dgwt.compiler.skip=true -B

# Stage 5: Build webprotege-server (depends on server-core)
COPY webprotege-server/src /webprotege/webprotege-server/src
RUN mvn -pl webprotege-server \
    install -o \
    -Dmaven.test.skip=true -DskipTests=true -Dmaven.javadoc.skip=true -B

# Stage 6: Build webprotege-cli (depends on server-core)
COPY webprotege-cli/src /webprotege/webprotege-cli/src
RUN mvn -pl webprotege-cli \
    install -o \
    -Dmaven.test.skip=true -DskipTests=true -Dmaven.javadoc.skip=true -B

# Copy start scripts
COPY start-dev.sh /webprotege/
COPY install.sh /webprotege/
COPY admin-account-setup.sh /webprotege/
COPY start-tomcat.sh /webprotege/

# Make the scripts executable
RUN chmod +x /webprotege/start-dev.sh
RUN chmod +x /webprotege/install.sh
RUN chmod +x /webprotege/admin-account-setup.sh
RUN chmod +x /webprotege/start-tomcat.sh

# Copy built-in-prefixes.csv to avoid classpath extraction issues
RUN cp /webprotege/webprotege-server-core/src/main/resources/built-in-prefixes.csv /srv/webprotege/

# Copy configuration files needed for installation
COPY conf/ /webprotege/conf/
COPY tomcat8_default_conf/ /webprotege/tomcat8_default_conf/

# Entry point to keep the container running
CMD ["/webprotege/start-dev.sh"]
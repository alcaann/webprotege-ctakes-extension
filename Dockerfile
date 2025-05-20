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

# Copy source code to container
COPY . /webprotege

# Create a build script and a start script for development
COPY build.sh /webprotege/build.sh
COPY start-dev.sh /webprotege/start-dev.sh

# Make the scripts executable
RUN chmod +x /webprotege/build.sh \
    && chmod +x /webprotege/start-dev.sh

# Entry point to keep the container running
CMD ["/webprotege/start-dev.sh"]

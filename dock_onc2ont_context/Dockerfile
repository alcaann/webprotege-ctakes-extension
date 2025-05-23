# Use an OpenJDK 17 base image (Temurin is a good distribution of OpenJDK)
FROM eclipse-temurin:17-jdk-focal

# Set DEBIAN_FRONTEND to noninteractive to avoid prompts during apt-get install
ENV DEBIAN_FRONTEND=noninteractive

# --- Install a specific newer version of Maven ---
ARG MAVEN_VERSION=3.9.6 # Or any version >= 3.8.1
ARG MAVEN_HOME=/opt/maven
ENV MAVEN_HOME=${MAVEN_HOME}
ENV PATH=${MAVEN_HOME}/bin:${PATH}

RUN apt-get update && apt-get install -y --no-install-recommends \
    wget \
    ca-certificates \
    && rm -rf /var/lib/apt/lists/* && \
    mkdir -p /opt && \
    wget https://archive.apache.org/dist/maven/maven-3/${MAVEN_VERSION}/binaries/apache-maven-${MAVEN_VERSION}-bin.tar.gz -O /tmp/maven.tar.gz && \
    tar -xzf /tmp/maven.tar.gz -C /opt && \
    ln -s /opt/apache-maven-${MAVEN_VERSION} ${MAVEN_HOME} && \
    rm /tmp/maven.tar.gz && \
    echo "Maven version $(mvn --version) installed."

# Install other necessary tools:
# git, subversion (keeping just in case),
# unzip (for dictionary), nano (for manual edits),
# and X11/font libraries.
RUN apt-get update && apt-get install -y --no-install-recommends \
    git \
    subversion \
    unzip \
    nano \
    libxext6 \
    libxrender1 \
    libxtst6 \
    libxi6 \
    fontconfig \
    libx11-6 \
    libxau6 \
    libxdmcp6 \
    libxcb1 \
    && rm -rf /var/lib/apt/lists/*

# Install Python and Flask
RUN apt-get update && apt-get install -y --no-install-recommends \
    python3 \
    python3-pip \
    && pip3 install Flask gunicorn \
    && rm -rf /var/lib/apt/lists/*

# --- cTAKES Source Build Section REMOVED ---

# --- Set up your application ---
WORKDIR /workspaces/dock_onc2ont

COPY pom.xml ./
COPY src ./src/
COPY input_data ./input_data/
COPY setup_ctakes_dictionary.sh ./
RUN chmod +x ./setup_ctakes_dictionary.sh

# Copy the Flask API file
COPY api.py ./

RUN ./setup_ctakes_dictionary.sh
RUN mvn clean package

# Expose the port Flask will run on
EXPOSE 5000

# Set the UMLS_APIKEY environment variable at runtime when starting the container
# Example: docker run -e UMLS_APIKEY="your-key" -p 5000:5000 your_image_name
ENV UMLS_APIKEY="d8b52583-9206-4422-b892-6551647e6b28"

# Run the Flask app using Gunicorn
CMD ["gunicorn", "--bind", "0.0.0.0:5000", "api:app"]
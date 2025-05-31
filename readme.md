WebProtégé CTakes Extension
============================

This is a specialized fork of WebProtégé that integrates clinical text processing capabilities through the onc2ont service. This branch (ctakes-extension) adds clinical text analysis functionality to the standard WebProtégé ontology development environment.

What is WebProtégé?
-------------------

WebProtégé is a free, open source collaborative ontology development environment.

It provides the following features:
- Support for editing OWL 2 ontologies
- A default simple editing interface, which provides access to commonly used OWL constructs
- Full change tracking and revision history
- Collaboration tools such as, sharing and permissions, threaded notes and discussions, watches and email notifications
- Customizable user interface
- Support for editing OBO ontologies
- Multiple file formats for upload and download of ontologies (supported formats: RDF/XML, Turtle, OWL/XML, OBO, and others)

WebProtégé runs as a Web application. End users access it through their Web browsers.

CTakes Extension Features
-------------------------

This extension integrates clinical text processing capabilities into WebProtégé, enabling the conversion of unstructured clinical notes into structured OWL2 ontology entities.

## Clinical Text Converter Perspective

A specialized workspace designed for clinical text analysis that includes:

- **Individual Lists**: Browse and manage ontology individuals
- **Individual Editor**: Edit individual properties and relationships  
- **Onc2Ont Portlet**: Process clinical text and generate ontology entities

```
┌─────────────────────────────────────────────────────────────┐
│                Clinical Text Converter                     │
├─────────────────┬─────────────────┬─────────────────────────┤
│  Individual     │  Individual     │     Onc2Ont            │
│     Lists       │    Editor       │     Portlet             │
│                 │                 │                         │
│  • Browse       │  • Properties   │  • Clinical Text Input  │
│  • Filter       │  • Relations    │  • Processing Status    │
│  • Select       │  • Annotations  │  • Generated Entities   │
│                 │                 │  • Statistics           │
└─────────────────┴─────────────────┴─────────────────────────┘
```

## Onc2Ont Portlet Integration

**Core Functionality:**
- **Portlet ID**: `portlets.Onc2Ont`
- **Title**: "Clinical Notes Converter"
- Processes clinical text through external onc2ont service
- Converts unstructured clinical notes into structured OWL2 ontology entities
- Real-time processing status updates and detailed statistics
- Automatic integration of generated axioms and annotations into the active ontology

**Processing Workflow:**
```
Clinical Text Input → Onc2Ont Service → OWL2 Entities → WebProtégé Integration
      ↓                    ↓                ↓                    ↓
   [Text Area]        [HTTP API]      [Axioms/Annotations]  [Change Events]
```

## Architecture Overview

### System Integration

```
┌─────────────────────────────────────────────────────────────────┐
│                    WebProtégé CTakes Extension                  │
├─────────────────────────┬───────────────────────────────────────┤
│      Client Side        │           Server Side                 │
│      (GWT/Web)         │           (Java/Spring)               │
├─────────────────────────┼───────────────────────────────────────┤
│                         │                                       │
│  Onc2OntPortletPresenter│  ProcessClinicalNotesActionHandler    │
│           ↓             │              ↓                        │
│  Onc2OntView/ViewImpl   │  ProcessClinicalNotesAction/Result    │
│           ↓             │              ↓                        │
│  WebProtégé UI Framework│  Change Management System             │
│                         │              ↓                        │
│                         │  External Onc2Ont Service (HTTP API)  │
└─────────────────────────┴───────────────────────────────────────┘
```

### Key Components

**Client-Side (GWT):**
- `Onc2OntPortletPresenter`: Handles UI logic, user interactions, and service communication
- `Onc2OntView` / `Onc2OntViewImpl`: User interface components for text input, status display, and results
- Integrated into WebProtégé's portlet system and perspective framework

**Server-Side (Java/Spring):**
- `ProcessClinicalNotesActionHandler`: Processes client requests and coordinates clinical text processing
- `ProcessClinicalNotesAction` / `ProcessClinicalNotesResult`: Action/Result pattern for RPC communication
- Integration with WebProtégé's change management and revision system
- HTTP client for external onc2ont service communication

**Configuration & Deployment:**
- `Clinical Text Converter.json`: Perspective layout configuration
- `perspective.list.json`: Registers the perspective in WebProtégé's default perspectives
- `PerspectiveDataCopier.java`: Ensures perspective files are properly deployed
- Automatic portlet registration through WebProtégé's module system

### File Structure

```
webprotege-ctakes-extension/
├── webprotege-client/src/main/java/edu/stanford/bmir/protege/web/client/onc2ont/
│   ├── Onc2OntPortletPresenter.java       # Client-side portlet logic
│   ├── Onc2OntView.java                   # UI interface definition
│   └── Onc2OntViewImpl.java               # UI implementation
├── webprotege-server-core/src/main/java/edu/stanford/bmir/protege/web/server/onc2ont/
│   ├── ProcessClinicalNotesAction.java    # RPC action definition
│   ├── ProcessClinicalNotesActionHandler.java  # Server-side processing
│   └── ProcessClinicalNotesResult.java    # RPC result definition
└── webprotege-server-core/src/main/resources/default-perspective-data/
    ├── Clinical Text Converter.json       # Perspective layout
    └── perspective.list.json              # Perspective registration
```

Usage
-----

### Using the Clinical Text Converter

1. **Access the Perspective**: Navigate to the "Clinical Text Converter" perspective from the perspective selector
2. **Input Clinical Text**: Use the Onc2Ont portlet to enter clinical notes or text
3. **Process Text**: Click the process button to send text to the onc2ont service
4. **Review Results**: Monitor processing status and review generated ontology entities
5. **Integration**: Generated axioms and annotations are automatically integrated into your ontology

### Prerequisites for CTakes Extension

- **Onc2Ont Service**: External onc2ont service must be running and accessible
- **Network Access**: Server must have HTTP access to the onc2ont service endpoint
- **Ontology Project**: An active WebProtégé project with appropriate permissions

The original WebProtégé instance is available at:

https://webprotege.stanford.edu

If you have downloaded the webprotege war file from GitHub, and would like to deploy it on your own server,
please follow the instructions at:

https://github.com/protegeproject/webprotege/wiki/WebProtégé-4.0.0-beta-x-Installation

Building
--------

To build WebProtégé CTakes Extension from source:

1) Clone the github repository
   ```
   git clone https://github.com/protegeproject/webprotege.git
   cd webprotege
   git checkout ctakes-extension
   ```
2) Open a terminal in the directory where you cloned the repository
3) Use maven to package WebProtégé
   ```
   mvn clean package
   ```
4) The WebProtege .war file will be built into the webprotege-server directory

**Note**: The CTakes extension includes additional dependencies for clinical text processing. Ensure the onc2ont service is properly configured and accessible for full functionality.

Running from Maven
------------------

To run WebProtégé in SuperDev Mode using maven

1) Start the GWT code server in one terminal window
    ```
    mvn gwt:codeserver
    ```
2) In a different terminal window start the tomcat server
    ```
    mvn -Denv=dev tomcat7:run
    ```
3) Browse to WebProtégé in a Web browser by navigating to [http://localhost:8080](http://localhost:8080)

Running from Docker
-------------------

To run WebProtégé using the Docker container

1) Create a new file called "docker-compose.yml" and copy-and-paste the following text:
   ```yml
   version: '3'

   services:
     wpmongo:
       image: mongo:4.1-bionic
     webprotege:
       image: protegeproject/webprotege
       restart: always
       environment:
         - webprotege.mongodb.host=wpmongo
       ports:
         - 5000:8080
       depends_on:
         - wpmongo
   ```
2) Enter this following command in the Terminal to start the docker container.
   ```bash
   $ docker-compose up
   ```
3) Browse to WebProtégé in a Web browser by navigating to [http://localhost:5000](http://localhost:5000)

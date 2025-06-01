# onc2ont: Clinical Text to OWL/TTL Processing Pipeline

## Overview

onc2ont is a comprehensive biomedical text processing system that transforms unstructured clinical narratives into structured semantic data using the Web Ontology Language (OWL) in Turtle (TTL) format. The system combines Apache cTAKES (Clinical Text Analysis and Knowledge Extraction System) for natural language processing with custom semantic conversion components to bridge clinical text and formal ontologies.

**Key Features:**
- Complete cTAKES NLP pipeline for clinical text analysis
- Advanced relation extraction for medical concepts
- Comprehensive assertion detection (negation, uncertainty, history, etc.)
- RESTful API for web service integration
- Docker containerization for easy deployment
- Protégé-compatible OWL/TTL output

## Architecture & Components

### Core Processing Pipeline

The system processes clinical text through a sophisticated multi-stage pipeline:

1. **Document Input Processing**
   - Custom file tree reader (`MyExtendingFileTreeReader`) with metadata tracking
   - Support for multiple document formats and encoding handling
   - Source document provenance preservation

2. **Advanced NLP Analysis Chain**
   - **Text Sectionization**: Clinical document structure recognition using regex patterns
   - **Segmentation & Tokenization**: Penn Treebank tokenizer with context-dependent processing
   - **Sentence Detection**: Boundary detection optimized for clinical text
   - **Part-of-Speech Tagging**: Medical domain-adapted POS tagging
   - **Syntactic Chunking**: Phrase-level grouping for improved concept recognition
   - **Dependency Parsing**: Grammatical relationship extraction using ClearNLP
   - **UMLS Dictionary Lookup**: Medical concept identification with UMLS integration

3. **Medical Assertion & Relation Processing**
   - **Assertion Analysis**: Multi-dimensional medical concept characterization:
     - Polarity detection (positive/negative)
     - Uncertainty assessment
     - Historical context identification
     - Conditional statement recognition
     - Generic vs. specific mentions
     - Subject assignment (patient vs. family member)
   - **Relation Extraction**: Semantic relationship identification:
     - Location-of relations (anatomical associations)
     - Degree-of relations (severity/intensity)
     - General modifier relationships

4. **Semantic Knowledge Conversion**
   - OWL ontology model creation with formal class hierarchy
   - Individual instance generation with unique URI schemes
   - Property assertion mapping with typed literals
   - Relation triple generation for semantic connections

### Implementation Components

#### `PlaintextToTtl` - Pipeline Orchestrator
- **Main Class**: Entry point for the complete processing pipeline
- **Component Integration**: Coordinates all cTAKES analysis engines in proper sequence
- **Resource Management**: Handles UMLS API key configuration and model loading
- **Error Handling**: Comprehensive exception management and logging
- **Pipeline Configuration**: 
  - Complete clinical NLP pipeline including all assertion engines
  - Configurable OpenNLP models for sentence detection and POS tagging
  - Integrated relation extraction components
- **Debugging Support**: Built-in view verification and annotation inspection

#### `MyExtendingFileTreeReader` - Enhanced File Processing
- **Inheritance**: Extends cTAKES `AbstractFileTreeReader` with custom capabilities
- **Metadata Views**: Creates specialized views for document source tracking
- **File Handling**: Robust encoding support and error recovery
- **Document Provenance**: Maintains filename and path information throughout processing

#### `MyOwlCasConsumer` - Semantic Conversion Engine
- **OWL Model Creation**: Generates formal ontology models using Apache Jena
- **Class Hierarchy Management**: 
  - `IdentifiedAnnotation` (root class)
  - `DiseaseDisorderMention`, `SignSymptomMention`, `AnatomicalSiteMention`
  - `MedicationMention`, `ProcedureMention`
- **Property Definitions**:
  - **Data Properties**: Text spans, offsets, polarity, CUI codes, assertion attributes
  - **Object Properties**: Document relationships, spatial relations, modifier connections
- **Individual Generation**: Unique URI creation with safe identifier schemes
- **Relation Processing**: Maps cTAKES relations to OWL object properties
- **TTL Serialization**: Protégé-compatible output with proper namespace management

#### `Flask REST API` - Web Service Interface
- **Endpoint**: `/process` - accepts clinical text via POST requests
- **Processing**: Orchestrates Java pipeline execution from Python
- **File Management**: Temporary file handling for input/output processing
- **Response Handling**: Returns TTL content with appropriate MIME types
- **Error Management**: Comprehensive logging and HTTP status code handling

#### `Docker Deployment` - Containerized Solution
- **Base Image**: Eclipse Temurin OpenJDK 17 for optimal Java performance
- **Build System**: Maven 3.9.6 for dependency management and compilation
- **Python Integration**: Flask and Gunicorn for production web service deployment
- **Environment Configuration**: UMLS API key management and runtime configuration
- **Port Exposure**: Standard HTTP port (5000) for API access

## Ontology Model & Output Format

### Semantic Structure

The generated ontology follows a formal OWL2 specification with consistent URI schemes:

- **Base URI**: `http://onc2ont.com/ontology/`
- **Instance Namespace**: `http://onc2ont.com/ontology/instance/`
- **Schema Namespace**: `http://onc2ont.com/ontology/schema#`

### Class Hierarchy
```
owl:NamedIndividual
├── SourceDocument
└── IdentifiedAnnotation
    ├── DiseaseDisorderMention
    ├── SignSymptomMention
    ├── AnatomicalSiteMention
    ├── MedicationMention
    └── ProcedureMention
```

### Property Schema

#### Data Properties (Literal Values)
- **`hasCoveredText`** (xsd:string): Original text span from clinical document
- **`hasBeginOffset`** (xsd:integer): Character position start index
- **`hasEndOffset`** (xsd:integer): Character position end index
- **`hasCUI`** (xsd:string): UMLS Concept Unique Identifier
- **`isNegated`** (xsd:boolean): Polarity assertion (true = negated/absent)
- **`isUncertain`** (xsd:boolean): Uncertainty level assessment
- **`isHistorical`** (xsd:boolean): Historical context flag
- **`isConditional`** (xsd:boolean): Conditional statement indicator
- **`isGeneric`** (xsd:boolean): Generic vs. specific mention classification
- **`hasSubject`** (xsd:string): Subject attribution (patient, family member, etc.)

#### Object Properties (Entity Relationships)
- **`occursInDocument`**: Links annotations to source documents
- **`locationOf`**: Spatial/anatomical relationship between entities
- **`degreeOf`**: Severity or intensity relationship
- **`hasModifier`**: General modifier and attribute connections

### Example TTL Output
```turtle
@base   <http://onc2ont.com/ontology/> .
@prefix inst: <http://onc2ont.com/ontology/instance/> .
@prefix onc:  <http://onc2ont.com/ontology/schema#> .

inst:ddm_severe_pain_15
    rdf:type onc:DiseaseDisorderMention ;
    rdfs:label "Disease/Disorder: severe pain"@en ;
    onc:hasCoveredText "severe pain" ;
    onc:hasBeginOffset "25"^^xsd:int ;
    onc:hasEndOffset "37"^^xsd:int ;
    onc:hasCUI "76948002" ;
    onc:isNegated "false"^^xsd:boolean ;
    onc:isUncertain "false"^^xsd:boolean ;
    onc:occursInDocument inst:doc_clinical_note_001 .
```

## Usage & Deployment

### Local Development

#### Prerequisites
- **Java Development Kit**: OpenJDK 17 or higher
- **Apache Maven**: Version 3.8.1 or higher
- **UMLS Terminology Services Account**: For optimal dictionary lookup performance

#### Build & Run
```bash
# Clone repository
git clone [repository-url]
cd dock_onc2ont

# Compile and package
mvn clean package

# Set UMLS API key (required)
export UMLS_APIKEY="your-umls-api-key-here"

# Run pipeline directly
java -Dctakes.umls_apikey=$UMLS_APIKEY \
     -cp target/onc2ont-pipeline-1.0-SNAPSHOT.jar:target/dependency/* \
     com.onc2ont.PlaintextToTtl

# Input files: place .txt files in input_data/
# Output files: generated .ttl files in output_data/
```

### Docker Deployment

#### Quick Start
```bash
# Build Docker image
docker build -t onc2ont .

# Run container with API service
docker run -e UMLS_APIKEY="your-key" -p 5000:5000 onc2ont

# Access API endpoint
curl -X POST -H "Content-Type: text/plain; charset=utf-8" \
     --data-binary "@clinical_note.txt" \
     http://localhost:5000/process > output.ttl
```

#### Production Deployment
The containerized service uses Gunicorn WSGI server for production-grade performance:
- **Host**: 0.0.0.0 (accepts external connections)
- **Port**: 5000 (configurable via Docker port mapping)
- **Scalability**: Can be deployed behind load balancers for high availability

### API Integration

#### REST Endpoint: `/process`
- **Method**: POST
- **Content-Type**: `text/plain; charset=utf-8`
- **Request Body**: Raw clinical text
- **Response**: OWL/TTL formatted ontology (MIME type: `text/turtle`)

#### Python Client Example
```python
import requests

url = "http://localhost:5000/process"
clinical_text = "Patient presents with chest pain and shortness of breath."

response = requests.post(
    url,
    data=clinical_text.encode('utf-8'),
    headers={'Content-Type': 'text/plain; charset=utf-8'}
)

if response.status_code == 200:
    ttl_content = response.text
    print("Generated TTL:", ttl_content)
```

### Protégé Integration

The generated TTL files are fully compatible with Protégé ontology editor:

1. **Import**: Use Protégé's "Open from URI" or local file import
2. **Visualization**: Explore class hierarchies and individual relationships
3. **Querying**: Use SPARQL queries for complex information extraction
4. **Reasoning**: Apply OWL reasoners for inference and validation

## Technical Specifications

### Dependencies & Frameworks
- **Apache cTAKES**: Version 6.0.0 - Clinical NLP processing
- **Apache UIMA**: Version 3.6.0 - Text analysis framework  
- **Apache Jena**: Version 4.10.0 - Semantic web and OWL processing
- **Flask**: Python web framework for REST API
- **Maven**: Build automation and dependency management
- **Docker**: Containerization with Eclipse Temurin OpenJDK 17

### Processing Capabilities
- **Text Formats**: Plain text (.txt), UTF-8 encoding support
- **Medical Vocabularies**: UMLS integration with CUI mapping
- **Language Support**: English clinical text (extensible to other languages)
- **Scalability**: Concurrent processing support via API endpoints
- **Output Quality**: Protégé-validated OWL2 DL compliance

### Performance Characteristics
- **Throughput**: Processes typical clinical notes (500-2000 words) in 10-30 seconds
- **Memory Usage**: 2-4 GB RAM recommended for optimal performance
- **Disk Space**: Minimal footprint beyond cTAKES model requirements (~1.5 GB)
- **Accuracy**: Leverages validated cTAKES models with medical domain optimization

## Project Status & Roadmap

### Current Implementation ✅
- Complete cTAKES NLP pipeline with all major components
- Advanced assertion detection (negation, uncertainty, history, conditional, generic)
- Comprehensive relation extraction (location-of, degree-of, modifiers)
- Full OWL ontology generation with proper class hierarchies
- RESTful API with Docker containerization
- Protégé-compatible TTL output format

### Recent Updates
- **Enhanced Pipeline**: Updated `PlaintextToTtl` with complete assertion analysis
- **Improved Consumer**: Advanced `MyOwlCasConsumer` with comprehensive property mapping
- **API Integration**: Production-ready Flask service with Gunicorn deployment
- **Docker Optimization**: Streamlined container build with Maven integration

### Future Enhancements 🚧
- **Extended Relations**: Additional semantic relationship types (causes, treats, etc.)
- **Multi-format Support**: JSON-LD, RDF/XML output options
- **Batch Processing**: High-throughput document processing capabilities
- **Validation Tools**: Automated ontology consistency checking
- **Performance Optimization**: Parallel processing and caching mechanisms

## Contributing & Support

### Development Environment
- IDE recommendations: IntelliJ IDEA or Eclipse with Maven support
- Testing: Use provided `test_api.py` for API validation
- Debugging: Enable UIMA logging for detailed pipeline inspection

### Troubleshooting
- **UMLS Access**: Ensure valid API key registration at [UTS](https://uts.nlm.nih.gov/)
- **Memory Issues**: Increase JVM heap size with `-Xmx4g` parameter
- **Model Loading**: Verify cTAKES model files in `src/main/resources/models/`

For detailed API documentation, see `api.README.md`.

## Quick Setup Guide

### Docker Desktop Setup

To get started quickly with Docker Desktop:

1. **Clone the repository**
   ```bash
   git clone [repository-url]
   cd dock_onc2ont
   ```

2. **Build the Docker image**
   ```bash
   docker build -t onc2ont:latest .
   ```
   This command will:
   - Download the required base image (Eclipse Temurin OpenJDK 17)
   - Install Maven and build dependencies
   - Compile the Java application using Maven
   - Set up the Flask API environment
   - Create a ready-to-run container image

3. **Run the container**
   ```bash
   docker run -e UMLS_APIKEY="your-umls-api-key" -p 5000:5000 onc2ont:latest
   ```

4. **Access the API**
   - The service will be available at `http://localhost:5000`
   - Use the `/process` endpoint to convert clinical text to TTL format

The built image will appear in Docker Desktop's Images section as `onc2ont:latest` and can be managed through the Docker Desktop interface.

---

**License**: Apache License 2.0  
**Contact**: [Project repository] for issues and contributions

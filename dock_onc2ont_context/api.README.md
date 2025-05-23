# ONC2ONT API Documentation

## Overview

The ONC2ONT API provides a RESTful interface to the clinical text processing pipeline, allowing users to transform plain text clinical notes into structured OWL2 Turtle format. The API exposes a simple endpoint that accepts clinical text and returns the corresponding ontology representation.

## Setup

### Prerequisites

- Docker installed on your system
- UMLS API Key (required for cTAKES dictionary lookup)

### Quick Start

1. **Clone the repository**

```powershell
git clone [repository-url]
cd dock_onc2ont
```

2. **Build the Docker image**

```powershell
docker build -t onc2ont .
```

3. **Run the Docker container**

```powershell
docker run -e UMLS_APIKEY="your-umls-api-key" -p 5000:5000 onc2ont
```

Replace `your-umls-api-key` with your actual UMLS API key. If you don't have a UMLS API key, you can obtain one by registering at the [UMLS Terminology Services](https://uts.nlm.nih.gov/).

### Environment Variables

- `UMLS_APIKEY`: Required for cTAKES to access UMLS dictionaries

## API Usage

### Endpoint: `/process`

**Method**: POST

**Content-Type**: `text/plain; charset=utf-8`

**Request Body**: Plain text clinical note

**Response**: OWL2 Turtle (.ttl) formatted ontology

### Example Usage

#### Using cURL

```powershell
curl -X POST -H "Content-Type: text/plain; charset=utf-8" --data-binary "@path/to/clinical_note.txt" http://localhost:5000/process > output.ttl
```

#### Using Python

```python
import requests

# API endpoint
url = "http://localhost:5000/process"

# Read clinical note from file
with open('clinical_note.txt', 'r', encoding='utf-8') as f:
    clinical_text = f.read()

# Send POST request
response = requests.post(
    url,
    data=clinical_text.encode('utf-8'),
    headers={'Content-Type': 'text/plain; charset=utf-8'}
)

# Check response status
if response.status_code == 200:
    # Save TTL output
    with open('output.ttl', 'w', encoding='utf-8') as f:
        f.write(response.text)
    print("Conversion successful!")
else:
    print(f"Error: {response.status_code}")
    print(response.text)
```

## Input and Output Format Examples

### Example Input

```
Setting: Outpatient.
Specialty: General Surgery. 
Note detail level (1-5): 3.

CC/HPI: Mrs. X is a 66 yo caucasian woman w/ a known hist of umbilical hernia who presents to the 
clinic for f/u of a suspected umbilical hernia s/p panni.

PMHx: Umbilical hernia 17 years ago, thyroid cancer, gall bladder disease, HTN.

A/P: 
Umbilical hernia: Mrs. X is a 66 yo woman in no acute distress who is now s/p panni w/ a known 
history of umbilical hernia 17 years ago with s/s c/w with an umbilical hernia but negative US.
```

### Example Output

```ttl
@base   <http://onc2ont.com/ontology/> .
@prefix inst: <http://onc2ont.com/ontology/instance/> .
@prefix onc:  <http://onc2ont.com/ontology/schema#> .
@prefix owl:  <http://www.w3.org/2002/07/owl#> .
@prefix rdf:  <http://www.w3.org/1999/02/22-rdf-syntax-ns#> .
@prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#> .
@prefix xsd:  <http://www.w3.org/2001/XMLSchema#> .

onc:hasCoveredText  rdf:type  owl:DatatypeProperty;
        rdfs:domain  onc:DiseaseDisorderMention;
        rdfs:label   "has covered text"@en;
        rdfs:range   xsd:string .

inst:ddm_bcfce68b-86d7-4747-b13b-235c9beded34
        rdf:type              onc:DiseaseDisorderMention;
        rdfs:label            "Disease/Disorder: umbilical hernia"@en;
        onc:hasBeginOffset    "120"^^xsd:int;
        onc:hasCUI            "396347007";
        onc:hasCoveredText    "umbilical hernia";
        onc:hasEndOffset      "136"^^xsd:int;
        onc:hasPolarity       "0"^^xsd:int;
        onc:occursInDocument  inst:doc_note1.txt .

onc:DiseaseDisorderMention
        rdf:type         owl:Class;
        rdfs:label       "Disease or Disorder Mention"@en;
        rdfs:subClassOf  owl:NamedIndividual .

# ... additional triples ...
```

## Testing

A test script is provided to verify the API functionality:

```powershell
python test_api.py
```

The test script sends a sample clinical note to the API and displays the response.

## Error Handling

The API returns standard HTTP status codes:

- `200 OK`: The request was successful, and the response contains the TTL data.
- `400 Bad Request`: The request was invalid (e.g., empty input text).
- `500 Internal Server Error`: An error occurred while processing the request.

Error responses include a descriptive message in the response body.

## Technical Details

- The API is implemented using Flask.
- The server runs on port 5000 by default.
- The Docker container uses Gunicorn as the WSGI server.
- The API processes text through the cTAKES pipeline and converts annotations to OWL2/TTL format.

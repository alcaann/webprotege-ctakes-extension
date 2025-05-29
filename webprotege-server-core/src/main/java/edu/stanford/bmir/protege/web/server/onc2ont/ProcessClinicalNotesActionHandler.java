package edu.stanford.bmir.protege.web.server.onc2ont;

import edu.stanford.bmir.protege.web.server.access.AccessManager;
import edu.stanford.bmir.protege.web.server.change.AddAxiomChange;
import edu.stanford.bmir.protege.web.server.change.AddOntologyAnnotationChange;
import edu.stanford.bmir.protege.web.server.change.FixedChangeListGenerator;
import edu.stanford.bmir.protege.web.server.change.OntologyChangeList;
import edu.stanford.bmir.protege.web.server.dispatch.AbstractProjectActionHandler;
import edu.stanford.bmir.protege.web.server.dispatch.ExecutionContext;
import edu.stanford.bmir.protege.web.server.owlapi.WebProtegeOWLManager;
import edu.stanford.bmir.protege.web.server.project.DefaultOntologyIdManager;
import edu.stanford.bmir.protege.web.server.project.chg.ChangeManager;
import edu.stanford.bmir.protege.web.shared.access.BuiltInAction;
import edu.stanford.bmir.protege.web.shared.onc2ont.ProcessClinicalNotesAction;
import edu.stanford.bmir.protege.web.shared.onc2ont.ProcessClinicalNotesResult;
import edu.stanford.bmir.protege.web.shared.project.ProjectId;
import org.semanticweb.owlapi.formats.RioTurtleDocumentFormat;
import org.semanticweb.owlapi.io.OWLOntologyDocumentSource;
import org.semanticweb.owlapi.io.StreamDocumentSource;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.model.parameters.Imports;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.inject.Inject;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Set;

/**
 * Action handler for processing clinical notes through the onc2ont service.
 */
public class ProcessClinicalNotesActionHandler extends AbstractProjectActionHandler<ProcessClinicalNotesAction, ProcessClinicalNotesResult> {

    private static final Logger logger = LoggerFactory.getLogger(ProcessClinicalNotesActionHandler.class);

    @Nonnull
    private final ProjectId projectId;

    @Nonnull
    private final ChangeManager changeManager;

    @Nonnull
    private final DefaultOntologyIdManager defaultOntologyIdManager;

    @Inject
    public ProcessClinicalNotesActionHandler(@Nonnull AccessManager accessManager,
                                          @Nonnull ProjectId projectId,
                                          @Nonnull ChangeManager changeManager,
                                          @Nonnull DefaultOntologyIdManager defaultOntologyIdManager) {
        super(accessManager);
        this.projectId = projectId;
        this.changeManager = changeManager;
        this.defaultOntologyIdManager = defaultOntologyIdManager;
    }

    @Nonnull
    @Override
    public Class<ProcessClinicalNotesAction> getActionClass() {
        return ProcessClinicalNotesAction.class;
    }

    @Nonnull
    @Override
    public ProcessClinicalNotesResult execute(@Nonnull ProcessClinicalNotesAction action, @Nonnull ExecutionContext executionContext) {
        try {
            logger.info("[Onc2Ont] Processing clinical notes for project: {}, user: {}", 
                       projectId.getId(), executionContext.getUserId().getUserName());
            
            // Call the onc2ont service API
            logger.info("[Onc2Ont] Sending {} characters of clinical notes to onc2ont service", 
                       action.getClinicalNotes().length());
            String ttlContent = sendToOnc2Ont(action.getClinicalNotes());
            logger.info("[Onc2Ont] Received {} bytes of Turtle content from service", 
                       ttlContent.getBytes(StandardCharsets.UTF_8).length);
            
            // Parse the Turtle content using WebProtege OWL API
            logger.info("[Onc2Ont] Parsing Turtle content using local OWL API parser");
            var parsingResult = parseTurtleContent(ttlContent);
            
            if (parsingResult.axioms.isEmpty()) {
                logger.error("[Onc2Ont] No axioms found in Turtle content from service");
                return ProcessClinicalNotesResult.error("No axioms found in service response");
            }
            
            logger.info("[Onc2Ont] Successfully parsed {} axioms and {} ontology annotations from Turtle content", 
                       parsingResult.axioms.size(), parsingResult.annotations.size());
            
            // Get the default ontology ID for adding axioms
            var ontologyId = defaultOntologyIdManager.getDefaultOntologyId();
            logger.info("[Onc2Ont] Adding axioms to ontology: {}", ontologyId);
            
            // Build the change list using the existing WebProtégé infrastructure
            var builder = OntologyChangeList.<String>builder();
            
            // Add axioms
            parsingResult.axioms.forEach(axiom -> 
                builder.add(AddAxiomChange.of(ontologyId, axiom))
            );
            
            // Add ontology annotations if any
            parsingResult.annotations.forEach(annotation -> 
                builder.add(AddOntologyAnnotationChange.of(ontologyId, annotation))
            );
            
            var changeList = builder.build("Added ontology elements from clinical notes processing");
            var changeListGenerator = new FixedChangeListGenerator<>(changeList.getChanges(),
                                                                     "",
                                                                     "Added ontology elements from clinical notes processing");
            
            // Apply the changes through the change manager
            logger.info("[Onc2Ont] Applying {} total changes ({} axioms + {} ontology annotations) through change manager", 
                       changeList.getChanges().size(), 
                       parsingResult.axioms.size(), 
                       parsingResult.annotations.size());
            var result = changeManager.applyChanges(executionContext.getUserId(), changeListGenerator);
            
            logger.info("[Onc2Ont] Successfully applied {} changes", result.getChangeList().size());
            
            return ProcessClinicalNotesResult.success();
            
        } catch (IOException e) {
            logger.error("[Onc2Ont] Failed to communicate with onc2ont service", e);
            return ProcessClinicalNotesResult.error("Failed to communicate with onc2ont service: " + e.getMessage());
        } catch (OWLOntologyCreationException e) {
            logger.error("[Onc2Ont] Failed to parse Turtle content from service", e);
            return ProcessClinicalNotesResult.error("Failed to parse axioms from service response: " + e.getMessage());
        } catch (Exception e) {
            logger.error("[Onc2Ont] Unexpected error during clinical notes processing", e);
            return ProcessClinicalNotesResult.error("An error occurred: " + e.getMessage());
        }
    }

    /**
     * Simple container for parsing results
     */
    private static class TurtleParsingResult {
        final Set<OWLAxiom> axioms;
        final Set<OWLAnnotation> annotations;
        
        TurtleParsingResult(Set<OWLAxiom> axioms, Set<OWLAnnotation> annotations) {
            this.axioms = axioms;
            this.annotations = annotations;
        }
    }

    /**
     * Parses turtle content and returns axioms and ontology annotations using Rio Turtle format
     */
    private TurtleParsingResult parseTurtleContent(String turtleContent) throws OWLOntologyCreationException {
        logger.info("[Onc2Ont] Parsing {} characters of Turtle content", turtleContent.length());
        
        OWLOntologyManager manager = WebProtegeOWLManager.createOWLOntologyManager();
        IRI tempDocumentIri = IRI.create(String.format("http://webprotege.stanford.edu/projects/%s/ontologies/temp", 
                                                       projectId.getId()));
        
        RioTurtleDocumentFormat rioTurtleFormat = new RioTurtleDocumentFormat();
        InputStream inputStream = new ByteArrayInputStream(turtleContent.getBytes(StandardCharsets.UTF_8));
        
        OWLOntologyDocumentSource source = new StreamDocumentSource(inputStream,
                                                                    tempDocumentIri,
                                                                    rioTurtleFormat,
                                                                    "text/turtle");
        
        OWLOntology ontology = manager.loadOntologyFromOntologyDocument(source);
        Set<OWLAxiom> axioms = ontology.getAxioms(Imports.INCLUDED);
        Set<OWLAnnotation> annotations = ontology.getAnnotations();
        
        // Log the ontology annotations for debugging
        if (!annotations.isEmpty()) {
            logger.info("[Onc2Ont] Found ontology annotations:");
            annotations.forEach(annotation -> 
                logger.info("[Onc2Ont]   - {}: {}", 
                           annotation.getProperty().getIRI(), 
                           annotation.getValue())
            );
        } else {
            logger.info("[Onc2Ont] No ontology annotations found in Turtle content");
        }
        
        return new TurtleParsingResult(axioms, annotations);
    }

    private String sendToOnc2Ont(String clinicalNotes) throws IOException {
        URI uri = URI.create("http://onc2ont:5000/process");
        logger.info("[Onc2Ont] Connecting to onc2ont service at {}", uri);
        HttpURLConnection connection = (HttpURLConnection) uri.toURL().openConnection();
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "text/plain; charset=utf-8");
        connection.setDoOutput(true);
        
        // Send the request
        logger.info("[Onc2Ont] Sending clinical notes to service");
        try (OutputStream os = connection.getOutputStream()) {
            byte[] input = clinicalNotes.getBytes(StandardCharsets.UTF_8);
            os.write(input, 0, input.length);
        }
        
        // Check response code
        int responseCode = connection.getResponseCode();
        logger.info("[Onc2Ont] Received response code: {}", responseCode);
        if (responseCode != 200) {
            logger.error("[Onc2Ont] Service returned non-200 response code: {}", responseCode);
            throw new IOException("Failed to process clinical notes. Response code: " + responseCode);
        }
        
        // Read the response
        logger.info("[Onc2Ont] Reading service response");
        try (BufferedReader br = new BufferedReader(new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
            StringBuilder response = new StringBuilder();
            String responseLine;
            while ((responseLine = br.readLine()) != null) {
                response.append(responseLine).append("\n");
            }
            String responseContent = response.toString();
            logger.info("[Onc2Ont] Received response of {} bytes", responseContent.length());
            if (responseContent.length() < 500) {
                logger.info("[Onc2Ont] Response content (truncated): {}", 
                    responseContent.length() > 100 ? responseContent.substring(0, 100) + "..." : responseContent);
            } else {
                logger.info("[Onc2Ont] Response too large to log. First 100 chars: {}", responseContent.substring(0, 100));
            }
            return responseContent;
        }
    }

    @Override
    @Nullable
    protected BuiltInAction getRequiredExecutableBuiltInAction() {
        return BuiltInAction.EDIT_ONTOLOGY;
    }
}
package edu.stanford.bmir.protege.web.server.onc2ont;

import edu.stanford.bmir.protege.web.server.access.AccessManager;
import edu.stanford.bmir.protege.web.server.change.ChangeApplicationResult;
import edu.stanford.bmir.protege.web.server.change.ChangeListGenerator;
import edu.stanford.bmir.protege.web.server.change.FixedChangeListGenerator;
import edu.stanford.bmir.protege.web.server.change.HasApplyChanges;
import edu.stanford.bmir.protege.web.server.change.OntologyChange;
import edu.stanford.bmir.protege.web.server.change.OwlOntologyChangeTranslator;
import edu.stanford.bmir.protege.web.server.change.OwlOntologyChangeTranslatorVisitor;
import edu.stanford.bmir.protege.web.server.dispatch.AbstractProjectActionHandler;
import edu.stanford.bmir.protege.web.server.dispatch.ExecutionContext;
import edu.stanford.bmir.protege.web.server.owlapi.WebProtegeOWLManager;
import edu.stanford.bmir.protege.web.server.project.chg.RootOntologyProvider;
import edu.stanford.bmir.protege.web.shared.access.BuiltInAction;
import edu.stanford.bmir.protege.web.shared.onc2ont.ProcessClinicalNotesAction;
import edu.stanford.bmir.protege.web.shared.onc2ont.ProcessClinicalNotesResult;
import edu.stanford.bmir.protege.web.shared.project.ProjectId;
import edu.stanford.bmir.protege.web.shared.user.UserId;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.io.StreamDocumentSource;
import org.semanticweb.owlapi.model.AddAxiom;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.inject.Inject;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

/**
 * Action handler for processing clinical notes through the onc2ont service.
 */
public class ProcessClinicalNotesActionHandler extends AbstractProjectActionHandler<ProcessClinicalNotesAction, ProcessClinicalNotesResult> {

    private static final Logger logger = LoggerFactory.getLogger(ProcessClinicalNotesActionHandler.class);

    @Nonnull
    private final OWLDataFactory dataFactory;

    @Nonnull
    private final ProjectId projectId;

    @Nonnull
    private final RootOntologyProvider rootOntologyProvider;

    @Nonnull
    private final OWLOntologyManager ontologyManager;
    
    @Nonnull
    private final HasApplyChanges changeManager;
    
    @Nonnull
    private final OwlOntologyChangeTranslator changeTranslator;

    @Inject
    public ProcessClinicalNotesActionHandler(@Nonnull AccessManager accessManager,
                                          @Nonnull OWLDataFactory dataFactory,
                                          @Nonnull ProjectId projectId,
                                          @Nonnull RootOntologyProvider rootOntologyProvider,
                                          @Nonnull OWLOntologyManager ontologyManager,
                                          @Nonnull HasApplyChanges changeManager,
                                          @Nonnull OwlOntologyChangeTranslator changeTranslator) {
        super(accessManager);
        this.dataFactory = dataFactory;
        this.projectId = projectId;
        this.rootOntologyProvider = rootOntologyProvider;
        this.ontologyManager = ontologyManager;
        this.changeManager = changeManager;
        this.changeTranslator = changeTranslator;
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
            UserId userId = executionContext.getUserId();
            logger.info("[Onc2Ont] Processing clinical notes for project: {}, user: {}", projectId.getId(), userId.getUserName());
            
            // Get the root ontology
            OWLOntology rootOntology = rootOntologyProvider.get();
            logger.info("[Onc2Ont] Retrieved root ontology: {}", rootOntology.getOntologyID());
            logger.info("[Onc2Ont] Root ontology has {} axioms before processing", rootOntology.getAxioms().size());
            
            // Debug OntologyManager implementation
            logger.info("[Onc2Ont] OntologyManager implementation: {}", ontologyManager.getClass().getName());
            if (ontologyManager instanceof org.semanticweb.owlapi.model.OWLOntologyManager) {
                logger.info("[Onc2Ont] OntologyManager is standard OWLAPI OntologyManager");
            }
            
            // Call the onc2ont service API
            logger.info("[Onc2Ont] Sending {} characters of clinical notes to onc2ont service", action.getClinicalNotes().length());
            String ttlContent = sendToOnc2Ont(action.getClinicalNotes());
            logger.info("[Onc2Ont] Received {} bytes of Turtle content from service", ttlContent.getBytes(StandardCharsets.UTF_8).length);
            
            // Parse the Turtle content into a temporary OWL ontology using a standalone manager
            // instead of the project's ontology manager which restricts ontology creation
            InputStream inputStream = new ByteArrayInputStream(ttlContent.getBytes(StandardCharsets.UTF_8));
            StreamDocumentSource documentSource = new StreamDocumentSource(inputStream);
            logger.info("[Onc2Ont] Loading ontology from Turtle content using standalone manager");
            
            // Create a standalone manager that isn't restricted
            OWLOntologyManager tempManager = WebProtegeOWLManager.createOWLOntologyManager();
            OWLOntology importedOntology = tempManager.loadOntologyFromOntologyDocument(documentSource, new OWLOntologyLoaderConfiguration());
            logger.info("[Onc2Ont] Loaded ontology with {} axioms", importedOntology.getAxioms().size());
            
            
            // Debug the axioms being added
            if (importedOntology.getAxioms().size() > 0) {
                logger.info("[Onc2Ont] Example axioms from imported ontology:");
                importedOntology.getAxioms().stream().limit(5).forEach(axiom -> 
                    logger.info("[Onc2Ont] Axiom: {}", axiom));
            } else {
                logger.warn("[Onc2Ont] No axioms found in imported ontology!");
            }
            
            // Merge the imported ontology with the root ontology using ChangeManager
            logger.info("[Onc2Ont] Merging axioms into root ontology using changeManager");
            
            // Create a list of changes (AddAxiom operations)
            List<OWLOntologyChange> owlChanges = new ArrayList<>();
            int axiomsCount = 0;
            
            for (OWLAxiom axiom : importedOntology.getAxioms()) {
                owlChanges.add(new AddAxiom(rootOntology, axiom));
                axiomsCount++;
            }
            
            // Convert OWLOntologyChange objects to WebProtege's OntologyChange objects
            List<OntologyChange> changes = new ArrayList<>();
            for (OWLOntologyChange owlChange : owlChanges) {
                changes.add(changeTranslator.toOntologyChange(owlChange));
            }
            
            // Generate a fixed change list with our changes
            ChangeListGenerator<Set<OWLEntity>> changeListGenerator = 
                new FixedChangeListGenerator<>(changes, importedOntology.getSignature(), "Added ontology elements from clinical notes processing");
            
            // Apply the changes through the change manager
            logger.info("[Onc2Ont] Applying {} change operations through change manager", changes.size());
            logger.info("[Onc2Ont] Original OWL changes count: {}", owlChanges.size());
            logger.info("[Onc2Ont] Converted WebProtege changes count: {}", changes.size());
            
            ChangeApplicationResult<Set<OWLEntity>> result = 
                changeManager.applyChanges(executionContext.getUserId(), changeListGenerator);
            
            logger.info("[Onc2Ont] Changes successfully applied: {}", result.getChangeList().size());
            logger.info("[Onc2Ont] Root ontology now has {} axioms after processing", rootOntology.getAxioms().size());
            
            // Log the entities affected by these changes
            logger.info("[Onc2Ont] Number of entities affected: {}", result.getSubject().size());
            if (!result.getSubject().isEmpty()) {
                logger.info("[Onc2Ont] Example affected entities:");
                result.getSubject().stream().limit(5).forEach(entity -> 
                    logger.info("[Onc2Ont] Entity: {}", entity));
            }
            
            return ProcessClinicalNotesResult.success();
        } catch (OWLOntologyCreationException e) {
            logger.error("[Onc2Ont] Failed to parse the generated ontology", e);
            return ProcessClinicalNotesResult.error("Failed to parse the generated ontology: " + e.getMessage());
        } catch (IOException e) {
            logger.error("[Onc2Ont] Failed to communicate with onc2ont service", e);
            return ProcessClinicalNotesResult.error("Failed to communicate with onc2ont service: " + e.getMessage());
        } catch (Exception e) {
            logger.error("[Onc2Ont] Unexpected error during clinical notes processing", e);
            return ProcessClinicalNotesResult.error("An error occurred: " + e.getMessage());
        }
    }

    private String sendToOnc2Ont(String clinicalNotes) throws IOException {
        URL url = new URL("http://onc2ont:5000/process");
        logger.info("[Onc2Ont] Connecting to onc2ont service at {}", url);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
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

package edu.stanford.bmir.protege.web.client.onc2ont;

import com.google.gwt.core.client.GWT;
import edu.stanford.bmir.protege.web.client.dispatch.DispatchServiceManager;
import edu.stanford.bmir.protege.web.client.lang.DisplayNameRenderer;
import edu.stanford.bmir.protege.web.client.portlet.AbstractWebProtegePortletPresenter;
import edu.stanford.bmir.protege.web.client.portlet.PortletUi;
import edu.stanford.bmir.protege.web.shared.event.WebProtegeEventBus;
import edu.stanford.bmir.protege.web.shared.onc2ont.ProcessClinicalNotesAction;
import edu.stanford.bmir.protege.web.shared.onc2ont.ProcessClinicalNotesResult;
import edu.stanford.bmir.protege.web.shared.project.ProjectId;
import edu.stanford.bmir.protege.web.client.selection.SelectionModel;
import edu.stanford.webprotege.shared.annotations.Portlet;

import javax.annotation.Nonnull;
import javax.inject.Inject;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A portlet that provides integration with the onc2ont service, allowing users to
 * convert clinical notes to OWL2 ontologies.
 */
@Portlet(id = "portlets.Onc2Ont", 
        title = "Clinical Notes Converter", 
        tooltip = "Converts clinical notes to OWL2 ontology using onc2ont service")
public class Onc2OntPortletPresenter extends AbstractWebProtegePortletPresenter {

    @Nonnull
    private final Onc2OntView view;

    @Nonnull
    private final DispatchServiceManager dispatchServiceManager;

    @Inject
    public Onc2OntPortletPresenter(@Nonnull SelectionModel selectionModel,
                                  @Nonnull ProjectId projectId,
                                  @Nonnull DisplayNameRenderer displayNameRenderer,
                                  @Nonnull Onc2OntView view,
                                  @Nonnull DispatchServiceManager dispatchServiceManager) {
        super(selectionModel, projectId, displayNameRenderer);
        this.view = checkNotNull(view);
        this.dispatchServiceManager = checkNotNull(dispatchServiceManager);
    }

    @Override
    public void startPortlet(PortletUi portletUi, WebProtegeEventBus eventBus) {
        portletUi.setWidget(view);
        view.setSubmitHandler(this::handleSubmitClinicalNotes);
    }

    private void handleSubmitClinicalNotes(String clinicalNotes) {
        // Add some client-side console logging
        GWT.log("[Onc2Ont] Submitting " + clinicalNotes.length() + " characters of clinical notes");
        
        // Set initial progress
        view.setProcessingStatus(Onc2OntView.ProcessingStatus.PROCESSING);
        view.setStatusMessage("Connecting to onc2ont service...");
        
        long startTime = System.currentTimeMillis();
        dispatchServiceManager.execute(new ProcessClinicalNotesAction(getProjectId(), clinicalNotes),
                result -> {
                    long processingTime = System.currentTimeMillis() - startTime;
                    if (result.isSuccess()) {
                        GWT.log("[Onc2Ont] Notes processed successfully in " + processingTime + "ms");
                        
                        // Update to completed status
                        view.setProcessingStatus(Onc2OntView.ProcessingStatus.COMPLETED);
                        
                        // Show detailed success message
                        double processingTimeSec = result.getProcessingTimeMs() / 1000.0;
                        String successMessage = "Clinical notes successfully processed! Added " + 
                            result.getTotalEntitiesAdded() + " entities (" + 
                            result.getAxiomsAdded() + " axioms + " + 
                            result.getAnnotationsAdded() + " annotations) " +
                            "to the ontology in " + (Math.round(processingTimeSec * 100.0) / 100.0) + 
                            " seconds. You may need to refresh to see the new entities.";
                        view.setStatusMessage(successMessage);
                        
                        // Show detailed statistics
                        view.showProcessingStats(
                            result.getAxiomsAdded(),
                            result.getAnnotationsAdded(),
                            result.getProcessingTimeMs(),
                            result.getInputCharacters(),
                            result.getResponseBytes()
                        );
                    } else {
                        GWT.log("[Onc2Ont] Error processing notes: " + result.getErrorMessage());
                        
                        // Update to error status
                        view.setProcessingStatus(Onc2OntView.ProcessingStatus.ERROR);
                        
                        String errorMessage = "Error: " + result.getErrorMessage() + 
                            " (Processing attempted for " + (Math.round((result.getProcessingTimeMs() / 1000.0) * 100.0) / 100.0) + 
                            " seconds with " + result.getInputCharacters() + " characters of input)";
                        view.setStatusMessage(errorMessage);
                    }
                });
    }
}

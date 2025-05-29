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
        view.setStatusMessage("Processing clinical notes... (Sending to onc2ont service)");
        
        long startTime = System.currentTimeMillis();
        dispatchServiceManager.execute(new ProcessClinicalNotesAction(getProjectId(), clinicalNotes),
                result -> {
                    long processingTime = System.currentTimeMillis() - startTime;
                    if (result.isSuccess()) {
                        GWT.log("[Onc2Ont] Notes processed successfully in " + processingTime + "ms");
                        view.setStatusMessage("Clinical notes were successfully processed and added to the ontology. " +
                                              "Processing time: " + (processingTime / 1000.0) + " seconds. " + 
                                              "You may need to refresh or navigate to see the new entities.");
                    } else {
                        GWT.log("[Onc2Ont] Error processing notes: " + result.getErrorMessage());
                        view.setStatusMessage("Error: " + result.getErrorMessage() + 
                                             " (Processing attempted for " + (processingTime / 1000.0) + " seconds)");
                    }
                });
    }
}

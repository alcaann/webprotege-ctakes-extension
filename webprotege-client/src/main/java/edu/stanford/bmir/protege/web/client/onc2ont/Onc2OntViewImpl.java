package edu.stanford.bmir.protege.web.client.onc2ont;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.TextArea;
import com.google.gwt.user.client.ui.Button;

import javax.annotation.Nonnull;
import javax.inject.Inject;

/**
 * Implementation of the {@link Onc2OntView}.
 */
public class Onc2OntViewImpl extends Composite implements Onc2OntView {

    interface Onc2OntViewImplUiBinder extends UiBinder<HTMLPanel, Onc2OntViewImpl> {
    }

    private static Onc2OntViewImplUiBinder ourUiBinder = GWT.create(Onc2OntViewImplUiBinder.class);

    @UiField
    TextArea notesArea;

    @UiField
    Button submitButton;

    @UiField
    Label statusLabel;
    
    @UiField
    Label progressLabel;
    
    @UiField
    HTMLPanel statsPanel;

    private SubmitHandler submitHandler;

    @Inject
    public Onc2OntViewImpl() {
        initWidget(ourUiBinder.createAndBindUi(this));
        notesArea.getElement().setAttribute("placeholder", "Enter clinical notes here...");
        setProcessingStatus(ProcessingStatus.IDLE);
        statsPanel.setVisible(false);
    }

    @Override
    public void setSubmitHandler(@Nonnull SubmitHandler handler) {
        this.submitHandler = handler;
    }

    @Override
    public void setStatusMessage(String message) {
        statusLabel.setText(message);
    }
    
    @Override
    public void setProcessingStatus(ProcessingStatus status) {
        progressLabel.setText(status.getDisplayText());
        progressLabel.removeStyleName("progress-error");
        progressLabel.removeStyleName("progress-success");
        
        switch (status) {
            case ERROR:
                progressLabel.addStyleName("progress-error");
                setSubmitEnabled(true);
                break;
            case COMPLETED:
                progressLabel.addStyleName("progress-success");
                setSubmitEnabled(true);
                break;
            case IDLE:
                setSubmitEnabled(true);
                statsPanel.setVisible(false);
                break;
            default:
                setSubmitEnabled(false);
                break;
        }
    }
    
    @Override
    public void showProcessingStats(int axiomsAdded, int annotationsAdded, 
                                  long processingTimeMs, int inputCharacters, int responseBytes) {
        statsPanel.clear();
        statsPanel.setVisible(true);
        
        double processingTimeSec = processingTimeMs / 1000.0;
        double responseKB = responseBytes / 1024.0;
        int totalEntities = axiomsAdded + annotationsAdded;
        
        StringBuilder statsHtml = new StringBuilder();
        statsHtml.append("<div class='stats-container'>");
        statsHtml.append("<h4>Processing Results:</h4>");
        statsHtml.append("<div class='stats-grid'>");
        
        // Input stats
        statsHtml.append("<div class='stat-item'>");
        statsHtml.append("<span class='stat-label'>Input:</span>");
        statsHtml.append("<span class='stat-value'>").append(inputCharacters).append(" characters</span>");
        statsHtml.append("</div>");
        
        // Processing time
        statsHtml.append("<div class='stat-item'>");
        statsHtml.append("<span class='stat-label'>Processing Time:</span>");
        statsHtml.append("<span class='stat-value'>").append(Math.round(processingTimeSec * 100.0) / 100.0).append(" seconds</span>");
        statsHtml.append("</div>");
        
        // Response size
        statsHtml.append("<div class='stat-item'>");
        statsHtml.append("<span class='stat-label'>Response Size:</span>");
        statsHtml.append("<span class='stat-value'>").append(Math.round(responseKB * 10.0) / 10.0).append(" KB</span>");
        statsHtml.append("</div>");
        
        // Total entities
        statsHtml.append("<div class='stat-item highlight'>");
        statsHtml.append("<span class='stat-label'>Total Entities Added:</span>");
        statsHtml.append("<span class='stat-value'>").append(totalEntities).append("</span>");
        statsHtml.append("</div>");
        
        // Axioms
        if (axiomsAdded > 0) {
            statsHtml.append("<div class='stat-item'>");
            statsHtml.append("<span class='stat-label'>Axioms:</span>");
            statsHtml.append("<span class='stat-value'>").append(axiomsAdded).append("</span>");
            statsHtml.append("</div>");
        }
        
        // Annotations
        if (annotationsAdded > 0) {
            statsHtml.append("<div class='stat-item'>");
            statsHtml.append("<span class='stat-label'>Annotations:</span>");
            statsHtml.append("<span class='stat-value'>").append(annotationsAdded).append("</span>");
            statsHtml.append("</div>");
        }
        
        // Processing rate
        double entitiesPerSec = totalEntities / processingTimeSec;
        statsHtml.append("<div class='stat-item'>");
        statsHtml.append("<span class='stat-label'>Processing Rate:</span>");
        statsHtml.append("<span class='stat-value'>").append(Math.round(entitiesPerSec * 10.0) / 10.0).append(" entities/sec</span>");
        statsHtml.append("</div>");
        
        statsHtml.append("</div></div>");
        
        statsPanel.getElement().setInnerHTML(statsHtml.toString());
    }
    
    @Override
    public void setSubmitEnabled(boolean enabled) {
        submitButton.setEnabled(enabled);
    }

    @UiHandler("submitButton")
    void handleSubmitButtonClicked(ClickEvent event) {
        if (submitHandler != null) {
            String notes = notesArea.getText().trim();
            if (!notes.isEmpty()) {
                GWT.log("[Onc2OntView] Submit button clicked with " + notes.length() + " chars of text");
                setProcessingStatus(ProcessingStatus.SENDING);
                submitHandler.handleSubmit(notes);
            } else {
                GWT.log("[Onc2OntView] Submit attempted with empty text field");
                setStatusMessage("Please enter clinical notes to convert.");
            }
        } else {
            GWT.log("[Onc2OntView] Submit button clicked but no handler is registered");
        }
    }
}

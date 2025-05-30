package edu.stanford.bmir.protege.web.client.onc2ont;

import com.google.gwt.user.client.ui.IsWidget;

/**
 * The view interface for the Clinical Notes Converter portlet.
 */
public interface Onc2OntView extends IsWidget {

    /**
     * Sets the handler for the submit button click events.
     * @param handler The handler.
     */
    void setSubmitHandler(SubmitHandler handler);

    /**
     * Sets a status message.
     * @param message The message to be displayed to the user.
     */
    void setStatusMessage(String message);
    
    /**
     * Sets the current processing status.
     * @param status The processing status.
     */
    void setProcessingStatus(ProcessingStatus status);
    
    /**
     * Shows processing statistics after completion.
     * @param axiomsAdded Number of axioms added.
     * @param annotationsAdded Number of annotations added.
     * @param processingTimeMs Processing time in milliseconds.
     * @param inputCharacters Number of input characters.
     * @param responseBytes Size of response in bytes.
     */
    void showProcessingStats(int axiomsAdded, int annotationsAdded, 
                           long processingTimeMs, int inputCharacters, int responseBytes);
    
    /**
     * Enables or disables the submit button.
     * @param enabled Whether the button should be enabled.
     */
    void setSubmitEnabled(boolean enabled);

    /**
     * Processing status enumeration.
     */
    enum ProcessingStatus {
        IDLE("Ready"),
        SENDING("Sending clinical notes to onc2ont service..."),
        PROCESSING("Processing clinical notes..."),
        PARSING("Parsing response from service..."),
        ADDING_TO_ONTOLOGY("Adding entities to ontology..."),
        COMPLETED("Processing completed"),
        ERROR("An error occurred");
        
        private final String displayText;
        
        ProcessingStatus(String displayText) {
            this.displayText = displayText;
        }
        
        public String getDisplayText() {
            return displayText;
        }
    }

    /**
     * Interface for handlers that respond to submit button clicks.
     */
    interface SubmitHandler {
        void handleSubmit(String clinicalNotes);
    }
}

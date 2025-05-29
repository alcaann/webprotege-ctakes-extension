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
     * Interface for handlers that respond to submit button clicks.
     */
    interface SubmitHandler {
        void handleSubmit(String clinicalNotes);
    }
}

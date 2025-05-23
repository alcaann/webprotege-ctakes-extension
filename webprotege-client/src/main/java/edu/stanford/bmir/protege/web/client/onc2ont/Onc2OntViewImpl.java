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

    private SubmitHandler submitHandler;

    @Inject
    public Onc2OntViewImpl() {
        initWidget(ourUiBinder.createAndBindUi(this));
        notesArea.getElement().setAttribute("placeholder", "Enter clinical notes here...");
    }

    @Override
    public void setSubmitHandler(@Nonnull SubmitHandler handler) {
        this.submitHandler = handler;
    }

    @Override
    public void setStatusMessage(String message) {
        statusLabel.setText(message);
    }

    @UiHandler("submitButton")
    void handleSubmitButtonClicked(ClickEvent event) {
        if (submitHandler != null) {
            String notes = notesArea.getText().trim();
            if (!notes.isEmpty()) {
                GWT.log("[Onc2OntView] Submit button clicked with " + notes.length() + " chars of text");
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

package edu.stanford.bmir.protege.web.shared.onc2ont;

import edu.stanford.bmir.protege.web.shared.annotations.GwtSerializationConstructor;
import edu.stanford.bmir.protege.web.shared.dispatch.Result;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Result of processing clinical notes through the onc2ont service.
 */
public class ProcessClinicalNotesResult implements Result {

    private boolean success;
    
    private String errorMessage;

    public ProcessClinicalNotesResult(boolean success, @Nullable String errorMessage) {
        this.success = success;
        this.errorMessage = errorMessage;
    }
    
    public static ProcessClinicalNotesResult success() {
        return new ProcessClinicalNotesResult(true, null);
    }
    
    public static ProcessClinicalNotesResult error(@Nonnull String errorMessage) {
        return new ProcessClinicalNotesResult(false, checkNotNull(errorMessage));
    }

    @GwtSerializationConstructor
    private ProcessClinicalNotesResult() {
        // For GWT serialization
    }

    public boolean isSuccess() {
        return success;
    }

    @Nullable
    public String getErrorMessage() {
        return errorMessage;
    }
}

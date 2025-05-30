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
    
    private int axiomsAdded;
    
    private int annotationsAdded;
    
    private long processingTimeMs;
    
    private int inputCharacters;
    
    private int responseBytes;

    public ProcessClinicalNotesResult(boolean success, 
                                    @Nullable String errorMessage,
                                    int axiomsAdded,
                                    int annotationsAdded, 
                                    long processingTimeMs,
                                    int inputCharacters,
                                    int responseBytes) {
        this.success = success;
        this.errorMessage = errorMessage;
        this.axiomsAdded = axiomsAdded;
        this.annotationsAdded = annotationsAdded;
        this.processingTimeMs = processingTimeMs;
        this.inputCharacters = inputCharacters;
        this.responseBytes = responseBytes;
    }
    
    public static ProcessClinicalNotesResult success(int axiomsAdded, 
                                                   int annotationsAdded,
                                                   long processingTimeMs,
                                                   int inputCharacters,
                                                   int responseBytes) {
        return new ProcessClinicalNotesResult(true, null, axiomsAdded, annotationsAdded, 
                                            processingTimeMs, inputCharacters, responseBytes);
    }
    
    public static ProcessClinicalNotesResult error(@Nonnull String errorMessage,
                                                 long processingTimeMs,
                                                 int inputCharacters) {
        return new ProcessClinicalNotesResult(false, checkNotNull(errorMessage), 
                                            0, 0, processingTimeMs, inputCharacters, 0);
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
    
    public int getAxiomsAdded() {
        return axiomsAdded;
    }
    
    public int getAnnotationsAdded() {
        return annotationsAdded;
    }
    
    public long getProcessingTimeMs() {
        return processingTimeMs;
    }
    
    public int getInputCharacters() {
        return inputCharacters;
    }
    
    public int getResponseBytes() {
        return responseBytes;
    }
    
    public int getTotalEntitiesAdded() {
        return axiomsAdded + annotationsAdded;
    }
}

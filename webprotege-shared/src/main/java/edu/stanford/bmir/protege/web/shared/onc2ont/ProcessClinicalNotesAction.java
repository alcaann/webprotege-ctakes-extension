package edu.stanford.bmir.protege.web.shared.onc2ont;

import edu.stanford.bmir.protege.web.shared.annotations.GwtSerializationConstructor;
import edu.stanford.bmir.protege.web.shared.dispatch.ProjectAction;
import edu.stanford.bmir.protege.web.shared.project.ProjectId;

import javax.annotation.Nonnull;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Action for processing clinical notes through the onc2ont service.
 */
public class ProcessClinicalNotesAction implements ProjectAction<ProcessClinicalNotesResult> {

    private ProjectId projectId;
    
    private String clinicalNotes;

    public ProcessClinicalNotesAction(@Nonnull ProjectId projectId, @Nonnull String clinicalNotes) {
        this.projectId = checkNotNull(projectId);
        this.clinicalNotes = checkNotNull(clinicalNotes);
    }

    @GwtSerializationConstructor
    private ProcessClinicalNotesAction() {
        // For GWT serialization
    }

    @Nonnull
    @Override
    public ProjectId getProjectId() {
        return projectId;
    }

    @Nonnull
    public String getClinicalNotes() {
        return clinicalNotes;
    }
}

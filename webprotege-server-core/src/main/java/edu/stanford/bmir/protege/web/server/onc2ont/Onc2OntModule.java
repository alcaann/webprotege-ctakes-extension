package edu.stanford.bmir.protege.web.server.onc2ont;

import dagger.Module;
import dagger.Provides;
import edu.stanford.bmir.protege.web.server.project.chg.RootOntologyProvider;
import edu.stanford.bmir.protege.web.shared.inject.ProjectSingleton;
import org.semanticweb.owlapi.model.OWLOntology;

import javax.annotation.Nonnull;

/**
 * Module to provide bindings for the onc2ont functionality.
 */
@Module
public class Onc2OntModule {
    
    // We don't need this provider as the RootOntologyProvider is already available
    // The ProcessClinicalNotesActionHandler expects RootOntologyProvider, not OWLOntology
}

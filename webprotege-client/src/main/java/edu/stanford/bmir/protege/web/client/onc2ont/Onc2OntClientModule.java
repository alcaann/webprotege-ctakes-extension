package edu.stanford.bmir.protege.web.client.onc2ont;

import dagger.Module;
import dagger.Provides;
import edu.stanford.webprotege.shared.annotations.PortletModule;

/**
 * Module for the Onc2Ont portlet.
 */
@Module
@PortletModule
public class Onc2OntClientModule {

    @Provides
    Onc2OntView providesOnc2OntView(Onc2OntViewImpl impl) {
        return impl;
    }
}

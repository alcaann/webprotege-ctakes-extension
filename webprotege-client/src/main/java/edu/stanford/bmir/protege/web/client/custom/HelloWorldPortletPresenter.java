package edu.stanford.bmir.protege.web.client.custom;

import com.google.gwt.user.client.ui.HTML;
import edu.stanford.bmir.protege.web.client.lang.DisplayNameRenderer;
import edu.stanford.bmir.protege.web.client.portlet.AbstractWebProtegePortletPresenter;
import edu.stanford.bmir.protege.web.client.portlet.PortletUi;
import edu.stanford.bmir.protege.web.client.selection.SelectionModel;
import edu.stanford.bmir.protege.web.shared.event.WebProtegeEventBus;
import edu.stanford.bmir.protege.web.shared.project.ProjectId;
import edu.stanford.webprotege.shared.annotations.Portlet;

import javax.annotation.Nonnull;
import javax.inject.Inject;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A simple hello world portlet for WebProtege
 */
@Portlet(
        id = "portlets.HelloWorld",
        title = "Hello World",
        tooltip = "Displays a simple hello world message"
)
public class HelloWorldPortletPresenter extends AbstractWebProtegePortletPresenter {

    @Inject
    public HelloWorldPortletPresenter(@Nonnull SelectionModel selectionModel,
                                      @Nonnull ProjectId projectId,
                                      @Nonnull DisplayNameRenderer displayNameRenderer) {
        super(selectionModel, projectId, displayNameRenderer);
    }

    @Override
    public void startPortlet(PortletUi portletUi, WebProtegeEventBus eventBus) {
        // Create a simple HTML widget with a hello world message
        HTML widget = new HTML("<div style='padding: 20px; text-align: center;'>" +
                                "<h3>Hello WebProtege!</h3>" +
                                "<p>This is a custom portlet example.</p>" +
                                "</div>");
        
        // Set the portlet content to our widget
        portletUi.setWidget(widget);
    }
}

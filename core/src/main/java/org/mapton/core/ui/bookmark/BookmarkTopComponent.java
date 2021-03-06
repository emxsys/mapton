/*
 * Copyright 2019 Patrik Karlström.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.mapton.core.ui.bookmark;

import javafx.scene.Scene;
import org.mapton.api.MOptions;
import org.mapton.api.MTopComponent;
import org.mapton.core.ui.bookmark.BookmarkView;
import org.netbeans.api.settings.ConvertAsProperties;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.util.NbBundle.Messages;
import org.openide.windows.TopComponent;
import se.trixon.almond.util.Dict;

/**
 * Top component which displays something.
 */
@ConvertAsProperties(
        dtd = "-//org.mapton.core.bookmark//Bookmark//EN",
        autostore = false
)
@TopComponent.Description(
        preferredID = "BookmarkTopComponent",
        persistenceType = TopComponent.PERSISTENCE_ALWAYS
)
@TopComponent.Registration(mode = "topLeft", openAtStartup = true)
@ActionID(category = "Window", id = "org.mapton.core.bookmark.BookmarkTopComponent")
@ActionReference(path = "Menu/Window" /*, position = 333 */)
@TopComponent.OpenActionRegistration(
        displayName = "#CTL_BookmarkAction",
        preferredID = "BookmarkTopComponent"
)
@Messages({
    "CTL_BookmarkAction=Bookmark"
})
public final class BookmarkTopComponent extends MTopComponent {

    public BookmarkTopComponent() {
        putClientProperty(PROP_MAXIMIZATION_DISABLED, Boolean.TRUE);
        putClientProperty(PROP_SLIDING_DISABLED, Boolean.TRUE);
        putClientProperty(PROP_UNDOCKING_DISABLED, Boolean.TRUE);
        putClientProperty(PROP_KEEP_PREFERRED_SIZE_WHEN_SLIDED_IN, Boolean.TRUE);

        setName(Dict.BOOKMARKS.toString());
        setPopOverHolder(true);
    }

    @Override
    protected void componentClosed() {
        MOptions.getInstance().setBookmarkVisible(false);
        super.componentClosed();
    }

    @Override
    protected void componentOpened() {
        MOptions.getInstance().setBookmarkVisible(true);
        super.componentOpened();
    }

    @Override
    protected void initFX() {
        setScene(createScene());
    }

    private Scene createScene() {
        return new Scene(new BookmarkView());
    }

    void readProperties(java.util.Properties p) {
        String version = p.getProperty("version");
    }

    void writeProperties(java.util.Properties p) {
        // better to version settings since initial version as advocated at
        // http://wiki.apidesign.org/wiki/PropertyFiles
        p.setProperty("version", "1.0");
    }
}

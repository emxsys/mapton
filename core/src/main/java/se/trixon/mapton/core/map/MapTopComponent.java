/*
 * Copyright 2018 Patrik Karlström.
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
package se.trixon.mapton.core.map;

import com.lynden.gmapsfx.GoogleMapView;
import com.lynden.gmapsfx.javascript.event.GMapMouseEvent;
import com.lynden.gmapsfx.javascript.event.MapStateEventType;
import com.lynden.gmapsfx.javascript.event.UIEventType;
import com.lynden.gmapsfx.javascript.object.GoogleMap;
import com.lynden.gmapsfx.javascript.object.LatLong;
import com.lynden.gmapsfx.javascript.object.MapOptions;
import com.lynden.gmapsfx.javascript.object.MapTypeIdEnum;
import java.util.Arrays;
import java.util.Collection;
import java.util.Locale;
import java.util.prefs.PreferenceChangeEvent;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.scene.Scene;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Slider;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.BorderPane;
import javafx.scene.web.WebView;
import org.controlsfx.control.action.Action;
import org.controlsfx.control.action.ActionUtils;
import org.netbeans.api.settings.ConvertAsProperties;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.util.NbBundle.Messages;
import org.openide.windows.TopComponent;
import se.trixon.almond.util.Dict;
import se.trixon.almond.util.SystemHelper;
import se.trixon.mapton.core.AppStatusPanel;
import se.trixon.mapton.core.api.DictMT;
import se.trixon.mapton.core.api.MapStyleProvider;
import se.trixon.mapton.core.api.Mapton;
import se.trixon.mapton.core.api.MaptonOptions;
import se.trixon.mapton.core.api.MaptonTopComponent;
import se.trixon.mapton.core.bookmark.BookmarkManager;

/**
 * Top component which displays something.
 */
@ConvertAsProperties(
        dtd = "-//se.trixon.mapton.core.map//Map//EN",
        autostore = false
)
@TopComponent.Description(
        preferredID = "MapTopComponent",
        //iconBase="SET/PATH/TO/ICON/HERE",
        persistenceType = TopComponent.PERSISTENCE_ALWAYS
)
@TopComponent.Registration(mode = "editor", openAtStartup = true)
@ActionID(category = "Window", id = "se.trixon.mapton.core.map.MapTopComponent")
@ActionReferences({
    @ActionReference(path = "Menu/Window" /*, position = 333 */)
    ,
    @ActionReference(path = "Shortcuts", name = "D-M")
})
@TopComponent.OpenActionRegistration(
        displayName = "#CTL_MapAction",
        preferredID = "MapTopComponent"
)
@Messages({
    "CTL_MapAction=Map",
    "CTL_MapTopComponent=Map Window",
    "HINT_MapTopComponent=This is a Map window"
})
public final class MapTopComponent extends MaptonTopComponent {

    private GoogleMap mMap;
    private final MapController mMapController = MapController.getInstance();
    private MapOptions mMapOptions;
    private GoogleMapView mMapView;
    private final Mapton mMapton = Mapton.getInstance();
    private final MaptonOptions mOptions = MaptonOptions.getInstance();
    private BorderPane mRoot;
    private Slider mZoomSlider;

    public MapTopComponent() {
        super();
        setName(Dict.MAP.toString());

        putClientProperty(TopComponent.PROP_CLOSING_DISABLED, Boolean.TRUE);
        putClientProperty(TopComponent.PROP_DRAGGING_DISABLED, Boolean.TRUE);
        putClientProperty(TopComponent.PROP_MAXIMIZATION_DISABLED, Boolean.TRUE);
        putClientProperty(TopComponent.PROP_UNDOCKING_DISABLED, Boolean.TRUE);

        mMapton.setMapTopComponent(this);
    }

    @Override
    public GoogleMap getMap() {
        return mMap;
    }

    public MapOptions getMapOptions() {
        return mMapOptions;
    }

    public GoogleMapView getMapView() {
        return mMapView;
    }

    @Override
    protected void initFX() {
        setScene(createScene());
        initContextMenu();
    }

    private Scene createScene() {
        mMapView = new GoogleMapView(Locale.getDefault().getLanguage(), null);
        mRoot = new BorderPane(mMapView);

        mMapView.addMapInitializedListener(() -> {
            mMapOptions = new MapOptions()
                    .center(mOptions.getMapCenter())
                    .zoom(mOptions.getMapZoom())
                    .mapType(MapTypeIdEnum.ROADMAP)
                    .rotateControl(true)
                    .streetViewControl(false)
                    .mapTypeControl(false)
                    .fullscreenControl(false)
                    .scaleControl(true)
                    .styleString(MapStyleProvider.getStyle(mOptions.getMapStyle()))
                    .zoomControl(false);

            mZoomSlider = new Slider(0, 22, 1);
            mMapView.getChildren().add(mZoomSlider);
            Mapton.getAppToolBar().setDisable(false);

            initMap();
            Platform.runLater(() -> {
                mMap.setZoom(mOptions.getMapZoom());
                mMap.setCenter(mOptions.getMapCenter());
            });

            initListeners();
        });

        return new Scene(mRoot);
    }

    private void initContextMenu() {
        Action copyLocationAction = new Action(DictMT.COPY_LOCATION.toString(), (ActionEvent event) -> {
            SystemHelper.copyToClipboard(String.format(Locale.ENGLISH, "geo:%.6f,%.6f;crs=wgs84", mMapController.getLatitude(), mMapController.getLongitude()));
        });

        Action setHomeAction = new Action(DictMT.SET_HOME.toString(), (ActionEvent t) -> {
            mOptions.setMapHome(mMap.getCenter());
            mOptions.setMapHomeZoom(mMap.getZoom());
        });

        Collection<? extends Action> actions = Arrays.asList(
                copyLocationAction,
                ActionUtils.ACTION_SEPARATOR,
                BookmarkManager.getInstance().getAddBookmarkAction(),
                setHomeAction
        );

        ContextMenu contextMenu = ActionUtils.createContextMenu(actions);

        WebView webView = mMapView.getWebview();
        webView.setContextMenuEnabled(false);
        webView.setOnMousePressed(e -> {
            if (e.getButton() == MouseButton.SECONDARY) {
                contextMenu.show(webView, e.getScreenX(), e.getScreenY());
            } else {
                contextMenu.hide();
            }
        });
    }

    private void initListeners() {
        mOptions.getPreferences().addPreferenceChangeListener((PreferenceChangeEvent evt) -> {
            Platform.runLater(() -> {
                switch (evt.getKey()) {
                    case MaptonOptions.KEY_MAP_STYLE:
                        initMap();
                        break;

                    case MaptonOptions.KEY_MAP_TYPE:
                        mMap.setMapType(mOptions.getMapType());
                        break;

                    default:
                }
            });
        });
    }

    private void initMap() {
        mMapOptions.styleString(MapStyleProvider.getStyle(mOptions.getMapStyle()));
        if (mMap != null) {
            mMapOptions
                    .center(mMap.getCenter())
                    .zoom(mMap.getZoom());
        }

        mMap = mMapView.createMap(mMapOptions);
        mMap.zoomProperty().bindBidirectional(mZoomSlider.valueProperty());

        mMap.addStateEventHandler(MapStateEventType.zoom_changed, () -> {
            mMapController.setZoom(mMap.getZoom());
        });

        mMap.addMouseEventHandler(UIEventType.mousemove, (GMapMouseEvent event) -> {
            LatLong latLong = event.getLatLong();
            mMapController.setLatLong(latLong);
            AppStatusPanel.getInstance().getView().updateLatLong();
        });
    }

    void readProperties(java.util.Properties p) {
        String version = p.getProperty("version");
        Platform.runLater(() -> {
        });
    }

    void writeProperties(java.util.Properties p) {
        // better to version settings since initial version as advocated at
        // http://wiki.apidesign.org/wiki/PropertyFiles
        p.setProperty("version", "1.0");
        Platform.runLater(() -> {
            try {
                mOptions.setMapCenter(mMap.getCenter());
                mOptions.setMapZoom(mMap.getZoom());
            } catch (Exception e) {
            }
        });
    }
}

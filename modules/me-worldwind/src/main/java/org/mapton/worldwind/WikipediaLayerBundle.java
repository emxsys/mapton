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
package org.mapton.worldwind;

import gov.nasa.worldwind.WorldWind;
import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.layers.RenderableLayer;
import gov.nasa.worldwind.render.Offset;
import gov.nasa.worldwind.render.PointPlacemark;
import gov.nasa.worldwind.render.PointPlacemarkAttributes;
import javafx.collections.ListChangeListener;
import org.mapton.api.MOptions;
import org.mapton.api.MWikipediaArticle;
import org.mapton.api.MWikipediaArticleManager;
import org.mapton.worldwind.api.LayerBundle;
import org.mapton.worldwind.api.LayerBundleManager;
import org.openide.util.lookup.ServiceProvider;

/**
 *
 * @author Patrik Karlström
 */
@ServiceProvider(service = LayerBundle.class)
public class WikipediaLayerBundle extends LayerBundle {

    private final RenderableLayer mLayer = new RenderableLayer();
    private final MOptions mOptions = MOptions.getInstance();
    private final MWikipediaArticleManager mWikipediaArticleManager = MWikipediaArticleManager.getInstance();

    public WikipediaLayerBundle() {
        mLayer.setName("Wikipedia");
        setName("Wikipedia");

        init();
        initListeners();
    }

    @Override
    public void populate() throws Exception {
        getLayers().add(mLayer);
    }

    private void init() {
        mLayer.setPickEnabled(true);
    }

    private void initListeners() {
        mWikipediaArticleManager.getItems().addListener((ListChangeListener.Change<? extends MWikipediaArticle> c) -> {
            refresh();
        });
    }

    private void refresh() {
        mLayer.removeAllRenderables();

        for (MWikipediaArticle bookmark : mWikipediaArticleManager.getItems()) {
            PointPlacemark placemark = new PointPlacemark(Position.fromDegrees(bookmark.getLatLon().getLatitude(), bookmark.getLatLon().getLongitude()));
            placemark.setLabelText(bookmark.getTitle());
            placemark.setAltitudeMode(WorldWind.CLAMP_TO_GROUND);
            placemark.setEnableLabelPicking(true);

            PointPlacemarkAttributes attrs = new PointPlacemarkAttributes(placemark.getDefaultAttributes());
            attrs.setImageAddress("org/mapton/wikipedia/Wikipedia-logo.png");
            attrs.setLabelScale(1.0);
            attrs.setScale(0.15);
            attrs.setImageOffset(Offset.CENTER);
            placemark.setAttributes(attrs);

            PointPlacemarkAttributes attrsH = new PointPlacemarkAttributes(attrs);
            attrsH.setScale(attrsH.getScale() * 1.5);
            placemark.setHighlightAttributes(attrsH);

            mLayer.addRenderable(placemark);
        }

        LayerBundleManager.getInstance().redraw();
    }
}

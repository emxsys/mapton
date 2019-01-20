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
package org.mapton.core.ui;

import java.util.prefs.PreferenceChangeEvent;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.text.Font;
import org.controlsfx.control.StatusBar;
import org.mapton.api.MCooTrans;
import org.mapton.api.MEngine;
import org.mapton.api.MOptions;
import org.mapton.api.Mapton;
import org.openide.util.Lookup;
import org.openide.util.LookupEvent;
import se.trixon.almond.util.Dict;
import se.trixon.almond.util.GlobalStateChangeEvent;

/**
 *
 * @author Patrik Karlström
 */
public class AppStatusView extends StatusBar {

    private final ComboBox<MCooTrans> mComboBox = new ComboBox();
    private MCooTrans mCooTrans;
    private final Label mLabel = new Label();
    private final MOptions mOptions = MOptions.getInstance();

    public static AppStatusView getInstance() {
        return Holder.INSTANCE;
    }

    private AppStatusView() {
        super.setText("");
        getRightItems().addAll(mLabel, mComboBox);

        mLabel.prefHeightProperty().bind(heightProperty());
        mLabel.setPadding(new Insets(0, 8, 0, 8));
        mLabel.setFont(Font.font("monospaced"));

        Lookup.getDefault().lookupResult(MCooTrans.class).addLookupListener((LookupEvent ev) -> {
            updateProviders();
        });

        mComboBox.setOnAction((ActionEvent event) -> {
            mCooTrans = mComboBox.getSelectionModel().getSelectedItem();
            mOptions.setMapCooTrans(mCooTrans.getName());
            updateMousePositionData();
        });

        mOptions.getPreferences().addPreferenceChangeListener((PreferenceChangeEvent evt) -> {
            if (evt.getKey().equals(MOptions.KEY_MAP_ENGINE)) {
                setMessage("");
            }
        });

        Mapton.getGlobalState().addListener((GlobalStateChangeEvent evt) -> {
            Platform.runLater(() -> {
                switch (evt.getKey()) {
                    case MEngine.KEY_STATUS_COORDINATE:
                        updateMousePositionData();
                        break;

                    case MEngine.KEY_STATUS_PROGRESS:
                        setProgress(evt.getValue());
                        break;
                }
            });
        }, MEngine.KEY_STATUS_COORDINATE, MEngine.KEY_STATUS_PROGRESS);
        updateProviders();
    }

    public void setMessage(String message) {
        Platform.runLater(() -> {
            mLabel.setText(message);
        });
    }

    public void updateMousePositionData() {
        MEngine engine = Mapton.getEngine();

        if (engine != null) {
            if (engine.getLatitude() != null) {
                double latitude = engine.getLatitude();
                double longitude = engine.getLongitude();

                if (latitude != 0 && longitude != 0) {
                    String altitude = "";
                    if (engine.getAltitude() != null) {
                        double metersAltitude = engine.getAltitude();
                        if (Math.abs(metersAltitude) >= 1000) {
                            altitude = String.format("%s %,7d km, ", Dict.ALTITUDE.toString(), (int) Math.round(metersAltitude / 1e3));
                        } else {
                            altitude = String.format("%s %,7d m, ", Dict.ALTITUDE.toString(), (int) Math.round(metersAltitude));
                        }
                    }

                    String elevation = "";
                    if (engine.getElevation() != null) {
                        elevation = String.format("%s %,6d %s, ", Dict.ELEVATION.toString(), (int) engine.getElevation().doubleValue(), Dict.METERS.toString().toLowerCase());
                    }

                    String cooString = mCooTrans.getString(latitude, longitude);
                    String lat = String.format("%9.6f°%s", Math.abs(latitude), latitude < 0 ? "S" : "N");
                    String lon = String.format("%10.6f°%s", Math.abs(longitude), longitude < 0 ? "W" : "E");
                    String latLon = String.format("%s  %s WGS 84 DD, %s", lat, lon, cooString);

                    setMessage(altitude + elevation + latLon);
                }
            } else {
                setMessage("");
                setProgress(0);
            }
        }
    }

    void setMode(boolean mapMode) {
        Platform.runLater(() -> {
            mLabel.setVisible(mapMode);
            mComboBox.setVisible(mapMode);
            setProgress(0);
        });
    }

    private void updateProviders() {
        Platform.runLater(() -> {
            mComboBox.getItems().setAll(MCooTrans.getCooTrans());
            mComboBox.setItems(mComboBox.getItems().sorted());

            for (MCooTrans cooTrans : mComboBox.getItems()) {
                Mapton.logLoading("Coordinate Transformation", cooTrans.getName());
            }

            if (!mComboBox.getItems().isEmpty()) {
                MCooTrans cooTrans = MCooTrans.getCooTrans(mOptions.getMapCooTransName());

                if (cooTrans == null) {
                    cooTrans = mComboBox.getItems().get(0);
                }

                mComboBox.getSelectionModel().select(cooTrans);
                mCooTrans = mComboBox.getSelectionModel().getSelectedItem();
                updateMousePositionData();
            }
        });
    }

    private static class Holder {

        private static final AppStatusView INSTANCE = new AppStatusView();
    }
}

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
package se.trixon.mapton.worldwind;

import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLEventListener;
import gov.nasa.worldwind.WorldWindow;
import gov.nasa.worldwind.event.PositionEvent;
import gov.nasa.worldwind.geom.Angle;
import gov.nasa.worldwind.geom.Box;
import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.geom.Sector;
import gov.nasa.worldwind.geom.Vec4;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javafx.scene.Node;
import org.openide.util.lookup.ServiceProvider;
import se.trixon.almond.nbp.NbLog;
import se.trixon.mapton.api.MEngine;
import se.trixon.mapton.api.MLatLon;
import se.trixon.mapton.api.MLatLonBox;

/**
 *
 * @author Patrik Karlström
 */
@ServiceProvider(service = MEngine.class)
public class WorldWindMapEngine extends MEngine {

    public static final String LOG_TAG = "WorldWind";
    private static final double MAX_ALTITUDE = 2.0E7f;
    private boolean mInitialized;
    private WorldWindowPanel mMap;
    private StyleView mStyleView;

    public WorldWindMapEngine() {
        mStyleView = new StyleView();
    }

    @Override
    public void fitToBounds(MLatLonBox latLonBox) {
        fitToBounds(toSector(latLonBox));
    }

    @Override
    public MLatLon getCenter() {
        Vec4 centerPoint = mMap.getView().getCenterPoint();
        Position centerPosition = mMap.getView().getGlobe().computePositionFromPoint(centerPoint);

        return toLatLon(centerPosition);
    }

    @Override
    public String getName() {
        return "WorldWind (NASA)";
    }

    @Override
    public Node getStyleView() {
        return mStyleView;
    }

    @Override
    public Object getUI() {
        if (mMap == null) {
            init();
            initListeners();
        }

        return mMap;
    }

    @Override
    public double getZoom() {
        return toGlobalZoom();
    }

    @Override
    public void onWhatsHere(String s) {
    }

    @Override
    public void panTo(MLatLon latLon, double zoom) {
        if (mInitialized) {
            mMap.getView().goTo(toPosition(latLon), toLocalZoom(zoom));
        }
    }

    @Override
    public void panTo(MLatLon latLon) {
        panTo(latLon, toGlobalZoom());
    }

    private void fitToBounds(Sector sector) {
        WorldWindow wwd = mMap.getWwd();

        if (sector == null) {
            throw new IllegalArgumentException();
        }

        Box extent = Sector.computeBoundingBox(wwd.getModel().getGlobe(),
                wwd.getSceneController().getVerticalExaggeration(), sector);

        Angle fieldOfView = wwd.getView().getFieldOfView();
        double zoom = extent.getRadius() / fieldOfView.cosHalfAngle() / fieldOfView.tanHalfAngle();

        // Configure OrbitView to look at the center of the sector from our estimated distance. This causes OrbitView to
        // animate to the specified position over several seconds. To affect this change immediately use the following:
        // ((OrbitView) wwd.getView()).setCenterPosition(new Position(sector.getCentroid(), 0d));
        // ((OrbitView) wwd.getView()).setZoom(zoom);
        wwd.getView().goTo(new Position(sector.getCentroid(), 0d), zoom);
    }

    private void init() {
        mMap = new WorldWindowPanel();

        NbLog.v(LOG_TAG, "Loaded and ready");
    }

    private void initListeners() {
        mMap.addMouseListener(new MouseAdapter() {

            @Override
            public void mousePressed(MouseEvent e) {
                log(String.format("GlobalZoom = %f", toGlobalZoom()));
                maybeShowPopup(e);
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                maybeShowPopup(e);
            }

            private void maybeShowPopup(MouseEvent e) {
                if (e.isPopupTrigger() && e.isShiftDown()) {
                    displayContextMenu(e.getLocationOnScreen());
                }
            }
        });

        mMap.addPositionListener((PositionEvent pe) -> {
            Position position = pe.getPosition();
            if (position != null) {
                setLatLonMouse(toLatLon(position));
            } else {
//                setLatLonMouse(null);
            }
        });

        mMap.addGLEventListener(new GLEventListener() {
            private boolean runOnce = true;

            @Override
            public void display(GLAutoDrawable drawable) {
                mInitialized = true;
                if (runOnce) {
                    initialized();
                    runOnce = false;
                }
            }

            @Override
            public void dispose(GLAutoDrawable drawable) {
            }

            @Override
            public void init(GLAutoDrawable drawable) {
            }

            @Override
            public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height) {
            }
        });
    }

    private double toGlobalZoom() {
        double revAltitude = MAX_ALTITUDE - mMap.getView().getEyePosition().getAltitude();

        return Math.max(revAltitude / MAX_ALTITUDE, 0);
    }

    private MLatLon toLatLon(Position p) {
        return new MLatLon(
                p.getLatitude().getDegrees(),
                p.getLongitude().getDegrees()
        );
    }

    private double toLocalZoom(double globalZoom) {
        return MAX_ALTITUDE - globalZoom * MAX_ALTITUDE;
    }

    private Position toPosition(MLatLon latLon) {
        Angle lat = Angle.fromDegreesLatitude(latLon.getLatitude());
        Angle lon = Angle.fromDegreesLatitude(latLon.getLongitude());

        return new Position(lat, lon, 0);
    }

    private Sector toSector(MLatLonBox latLonBox) {
        return new Sector(
                Angle.fromDegreesLatitude(latLonBox.getSouthWest().getLatitude()),
                Angle.fromDegreesLatitude(latLonBox.getNorthEast().getLatitude()),
                Angle.fromDegreesLatitude(latLonBox.getSouthWest().getLongitude()),
                Angle.fromDegreesLatitude(latLonBox.getNorthEast().getLongitude())
        );
    }
}

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

import org.openide.util.NbPreferences;
import se.trixon.almond.util.Dict;
import se.trixon.almond.util.OptionsBase;

/**
 *
 * @author Patrik Karlström
 */
public class ModuleOptions extends OptionsBase {

    public static final String KEY_DISPLAY_ATMOSPHERE = "display_atmosphere";
    public static final String KEY_DISPLAY_COMPASS = "display_compass";
    public static final String KEY_DISPLAY_CONTROLS = "display_controls";
    public static final String KEY_DISPLAY_PLACE_NAMES = "display_place_names";
    public static final String KEY_DISPLAY_SCALE_BAR = "display_scale_bar";
    public static final String KEY_DISPLAY_STARS = "display_stars";
    public static final String KEY_DISPLAY_WORLD_MAP = "display_world_map";
    public static final String KEY_MAP_ELEVATION = "map_elevation";
    public static final String KEY_MAP_GLOBE = "map_mode";
    public static final String KEY_MAP_OPACITY = "map_opacity";
    public static final String KEY_MAP_PROJECTION = "map_projection";
    public static final String KEY_MAP_STYLE = "map_style";
    public static final String KEY_RULER_ANNOTATION = "ruler_annotation";
    public static final String KEY_RULER_CONTROL_POINTS = "ruler_control_points";
    public static final String KEY_RULER_FOLLOW_TERRAIN = "ruler_follow_terrain";
    public static final String KEY_RULER_FREE_HAND = "ruler_free_hand";
    public static final String KEY_RULER_RUBBER_BAND = "ruler_rubber_band";
    public static final String KEY_VIEW_HEADING = "view_heading";
    public static final String KEY_VIEW_PITCH = "view_pitch";
    public static final String KEY_VIEW_ROLL = "view_roll";

    static final boolean DEFAULT_DISPLAY_ATMOSPHERE = true;
    static final boolean DEFAULT_DISPLAY_COMPASS = true;
    static final boolean DEFAULT_DISPLAY_CONTROLS = true;
    static final boolean DEFAULT_DISPLAY_PLACE_NAMES = true;
    static final boolean DEFAULT_DISPLAY_SCALE_BAR = true;
    static final boolean DEFAULT_DISPLAY_STARS = true;
    static final boolean DEFAULT_DISPLAY_WORLD_MAP = false;
    static final boolean DEFAULT_MAP_ELEVATION = true;
    static final boolean DEFAULT_MAP_GLOBE = true;
    static final double DEFAULT_MAP_OPACITY = 1.0;
    static final int DEFAULT_MAP_PROJECTION = 1;
    static final String DEFAULT_MAP_STYLE = Dict.MAP_TYPE_ROADMAP.toString();

    public static ModuleOptions getInstance() {
        return Holder.INSTANCE;
    }

    private ModuleOptions() {
        mPreferences = NbPreferences.forModule(ModuleOptions.class);
    }

    private static class Holder {

        private static final ModuleOptions INSTANCE = new ModuleOptions();
    }
}

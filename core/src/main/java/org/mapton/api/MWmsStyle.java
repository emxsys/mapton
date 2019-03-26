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
package org.mapton.api;

import com.google.gson.annotations.SerializedName;
import java.util.ArrayList;

/**
 *
 * @author Patrik Karlström
 */
public class MWmsStyle {

    @SerializedName("description")
    private String mDescription;
    @SerializedName("layers")
    private ArrayList<String> mLayers;
    @SerializedName("name")
    private String mName;
    @SerializedName("supplier")
    private String mSupplier;

    public String getDescription() {
        return mDescription;
    }

    public ArrayList<String> getLayers() {
        return mLayers;
    }

    public String getName() {
        return mName;
    }

    public String getSupplier() {
        return mSupplier;
    }

    public void setDescription(String description) {
        mDescription = description;
    }

    public void setLayers(ArrayList<String> layers) {
        mLayers = layers;
    }

    public void setName(String name) {
        mName = name;
    }

    public void setSupplier(String supplier) {
        mSupplier = supplier;
    }
}

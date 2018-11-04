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
package org.mapton.core.ui.context.open;

import java.util.Locale;
import org.openide.util.lookup.ServiceProvider;
import se.trixon.almond.util.MathHelper;
import org.mapton.api.MContextMenuItem;

/**
 *
 * @author Patrik Karlström
 */
@ServiceProvider(service = MContextMenuItem.class)
public class Hitta extends MContextMenuItem {

    @Override
    public String getName() {
        return "Hitta.se";
    }

    @Override
    public ContextType getType() {
        return ContextType.OPEN;
    }

    @Override
    public String getUrl() {
        return String.format(Locale.ENGLISH, "https://www.hitta.se/kartan!~%f,%f,%dz/",
                getLatitude(),
                getLongitude(),
                MathHelper.round(5 + 15 * getZoom())
        );
    }
}

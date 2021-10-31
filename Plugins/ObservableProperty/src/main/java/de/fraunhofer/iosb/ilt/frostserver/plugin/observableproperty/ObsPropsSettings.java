/*
 * Copyright (C) 2021 Fraunhofer Institut IOSB, Fraunhoferstr. 1, D 76131
 * Karlsruhe, Germany.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see http://www.gnu.org/licenses/.
 */
package de.fraunhofer.iosb.ilt.frostserver.plugin.observableproperty;

import de.fraunhofer.iosb.ilt.frostserver.plugin.coremodel.CoreModelSettings;
import de.fraunhofer.iosb.ilt.frostserver.settings.ConfigDefaults;
import de.fraunhofer.iosb.ilt.frostserver.settings.CoreSettings;
import de.fraunhofer.iosb.ilt.frostserver.settings.Settings;
import de.fraunhofer.iosb.ilt.frostserver.settings.annotation.DefaultValue;
import de.fraunhofer.iosb.ilt.frostserver.settings.annotation.DefaultValueBoolean;

/**
 *
 * @author hylke
 */
public class ObsPropsSettings implements ConfigDefaults {

    @DefaultValueBoolean(false)
    public static final String TAG_ENABLE_OBSERVABLEPROPERTIES_MODEL = "observableproperty.enable";
    @DefaultValue("")
    public static final String TAG_ID_TYPE_CONSTRAINT = "observableproperty.idType.constraint";
    @DefaultValue("")
    public static final String TAG_ID_TYPE_CONTEXTOBJECT = "observableproperty.idType.contextObject";
    @DefaultValue("")
    public static final String TAG_ID_TYPE_MATRIX = "observableproperty.idType.matrix";
    @DefaultValue("")
    public static final String TAG_ID_TYPE_OOI = "observableproperty.idType.objectOfInterest";
    @DefaultValue("")
    public static final String TAG_ID_TYPE_PROPERTY = "observableproperty.idType.property";

    public final String idTypeDefault;
    public final String idTypeConstraint;
    public final String idTypeContextObject;
    public final String idTypeMatrix;
    public final String idTypeObjectOfInterest;
    public final String idTypeProperty;

    public ObsPropsSettings(CoreSettings settings) {
        Settings pluginSettings = settings.getPluginSettings();
        idTypeDefault = pluginSettings.get(CoreModelSettings.TAG_ID_TYPE_DEFAULT, CoreModelSettings.class).toUpperCase();
        idTypeConstraint = pluginSettings.get(TAG_ID_TYPE_CONSTRAINT, idTypeDefault).toUpperCase();
        idTypeContextObject = pluginSettings.get(TAG_ID_TYPE_CONTEXTOBJECT, idTypeDefault).toUpperCase();
        idTypeMatrix = pluginSettings.get(TAG_ID_TYPE_MATRIX, idTypeDefault).toUpperCase();
        idTypeObjectOfInterest = pluginSettings.get(TAG_ID_TYPE_OOI, idTypeDefault).toUpperCase();
        idTypeProperty = pluginSettings.get(TAG_ID_TYPE_PROPERTY, idTypeDefault).toUpperCase();
    }

    public String getTypeFor(CoreSettings settings, String entityTypeName) {
        Settings pluginSettings = settings.getPluginSettings();
        return pluginSettings.get("observableproperty.idType." + entityTypeName, idTypeDefault).toUpperCase();
    }
}

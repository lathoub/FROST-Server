/*
 * Copyright (C) 2020 Fraunhofer Institut IOSB, Fraunhoferstr. 1, D 76131
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
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package de.fraunhofer.iosb.ilt.frostserver.plugin.observableproperty;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.fraunhofer.iosb.ilt.frostserver.model.ModelRegistry;
import de.fraunhofer.iosb.ilt.frostserver.model.loader.DefEntityProperty;
import de.fraunhofer.iosb.ilt.frostserver.model.loader.DefEntityType;
import de.fraunhofer.iosb.ilt.frostserver.model.loader.DefModel;
import de.fraunhofer.iosb.ilt.frostserver.persistence.PersistenceManager;
import de.fraunhofer.iosb.ilt.frostserver.persistence.PersistenceManagerFactory;
import de.fraunhofer.iosb.ilt.frostserver.persistence.pgjooq.PostgresPersistenceManager;
import de.fraunhofer.iosb.ilt.frostserver.plugin.coremodel.CoreModelSettings;
import de.fraunhofer.iosb.ilt.frostserver.plugin.coremodel.PluginCoreModel;
import de.fraunhofer.iosb.ilt.frostserver.service.PluginModel;
import de.fraunhofer.iosb.ilt.frostserver.service.PluginRootDocument;
import de.fraunhofer.iosb.ilt.frostserver.service.Service;
import de.fraunhofer.iosb.ilt.frostserver.service.ServiceRequest;
import de.fraunhofer.iosb.ilt.frostserver.settings.CoreSettings;
import de.fraunhofer.iosb.ilt.frostserver.settings.Settings;
import de.fraunhofer.iosb.ilt.frostserver.util.LiquibaseUser;
import de.fraunhofer.iosb.ilt.frostserver.util.exception.UpgradeFailedException;
import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author scf
 */
public class PluginObservableProperty implements PluginRootDocument, PluginModel, LiquibaseUser {

    private static final Logger LOGGER = LoggerFactory.getLogger(PluginObservableProperty.class.getName());

    private static final String MODEL_NAME = "ObservableProperty";
    private static final String MODEL_PATH = MODEL_NAME.toLowerCase();
    private static final String[] MODEL_FILES = new String[]{
        "op_constraint.json",
        "op_contextobject.json",
        "op_matrix.json",
        "op_ooi.json",
        "op_property.json"
    };
    private static final String LIQUIBASE_CHANGELOG_FILENAME = "liquibase/" + MODEL_PATH + "/tables.xml";

    private static final List<String> REQUIREMENTS = Arrays.asList(
            "https://padlet.com/barbaramagagna/sogprgszse1bgd24");

    private CoreSettings settings;
    private boolean enabled;
    private boolean fullyInitialised;
    private String idTypeDefault;
    private List<DefModel> modelDefinitions = new ArrayList<>();
    private Map<String, DefEntityProperty> primaryKeys;

    public PluginObservableProperty() {
        LOGGER.info("Creating new {} Plugin.", MODEL_NAME);
    }

    @Override
    public void init(CoreSettings settings) {
        this.settings = settings;
        Settings pluginSettings = settings.getPluginSettings();
        enabled = pluginSettings.getBoolean(MODEL_PATH + ".enable", false);
        if (enabled) {
            idTypeDefault = pluginSettings.get(CoreModelSettings.TAG_ID_TYPE_DEFAULT, CoreModelSettings.class).toUpperCase();
            settings.getPluginManager().registerPlugin(this);

            primaryKeys = new HashMap<>();
            for (String fileName : MODEL_FILES) {
                loadModelFile(fileName);
            }
            for (Map.Entry<String, DefEntityProperty> entry : primaryKeys.entrySet()) {
                String typeName = entry.getKey();
                primaryKeys.get(typeName).setType(getTypeFor(settings, typeName));
            }
        }
    }

    private void loadModelFile(String fileName) {
        final String fullPath = MODEL_PATH + "/" + fileName;
        LOGGER.info("Loading model definition from {}", fullPath);
        InputStream stream = getClass().getClassLoader().getResourceAsStream(fullPath);
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            DefModel modelDefinition = objectMapper.readValue(stream, DefModel.class);
            modelDefinition.init();
            modelDefinitions.add(modelDefinition);
            for (DefEntityType type : modelDefinition.getEntityTypes().values()) {
                primaryKeys.put(type.getName(), type.getEntityProperties().get("@iot.id"));
            }
        } catch (IOException ex) {
            LOGGER.error("Failed to load model definition", ex);
        }
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public boolean isFullyInitialised() {
        return fullyInitialised;
    }

    @Override
    public void modifyServiceDocument(ServiceRequest request, Map<String, Object> result) {
        Map<String, Object> serverSettings = (Map<String, Object>) result.get(Service.KEY_SERVER_SETTINGS);
        if (serverSettings == null) {
            // Nothing to add to.
            return;
        }
        Set<String> extensionList = (Set<String>) serverSettings.get(Service.KEY_CONFORMANCE_LIST);
        extensionList.addAll(REQUIREMENTS);
    }

    @Override
    public void registerEntityTypes() {
        ModelRegistry modelRegistry = settings.getModelRegistry();
        for (DefModel modelDefinition : modelDefinitions) {
            modelDefinition.registerEntityTypes(modelRegistry);
        }
    }

    @Override
    public boolean linkEntityTypes(PersistenceManager pm) {
        LOGGER.info("Initialising {} Model Types...", MODEL_NAME);
        ModelRegistry modelRegistry = settings.getModelRegistry();
        for (DefModel modelDefinition : modelDefinitions) {
            modelDefinition.linkEntityTypes(modelRegistry);
            pm.addModelMapping(modelDefinition);
        }

        // Done, release the model definition.
        modelDefinitions = null;
        fullyInitialised = true;
        return true;
    }

    public String getTypeFor(CoreSettings settings, String entityTypeName) {
        Settings pluginSettings = settings.getPluginSettings();
        return pluginSettings.get(MODEL_PATH + ".idType." + entityTypeName, idTypeDefault).toUpperCase();
    }

    public Map<String, Object> createLiqibaseParams(PostgresPersistenceManager ppm, Map<String, Object> target) {
        if (target == null) {
            target = new LinkedHashMap<>();
        }
        PluginCoreModel pCoreModel = settings.getPluginManager().getPlugin(PluginCoreModel.class);
        pCoreModel.createLiqibaseParams(ppm, target);
        for (Map.Entry<String, DefEntityProperty> entry : primaryKeys.entrySet()) {
            String typeName = entry.getKey();
            ppm.generateLiquibaseVariables(target, typeName, getTypeFor(settings, typeName));
        }
        return target;
    }

    @Override
    public String checkForUpgrades() {
        try (PersistenceManager pm = PersistenceManagerFactory.getInstance(settings).create()) {
            if (pm instanceof PostgresPersistenceManager) {
                PostgresPersistenceManager ppm = (PostgresPersistenceManager) pm;
                return ppm.checkForUpgrades(LIQUIBASE_CHANGELOG_FILENAME, createLiqibaseParams(ppm, null));
            }
            return "Unknown persistence manager class";
        }
    }

    @Override
    public boolean doUpgrades(Writer out) throws UpgradeFailedException, IOException {
        try (PersistenceManager pm = PersistenceManagerFactory.getInstance(settings).create()) {
            if (pm instanceof PostgresPersistenceManager) {
                PostgresPersistenceManager ppm = (PostgresPersistenceManager) pm;
                return ppm.doUpgrades(LIQUIBASE_CHANGELOG_FILENAME, createLiqibaseParams(ppm, null), out);
            }
            out.append("Unknown persistence manager class");
            return false;
        }
    }

}

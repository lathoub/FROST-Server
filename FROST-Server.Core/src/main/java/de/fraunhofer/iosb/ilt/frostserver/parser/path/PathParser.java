/*
 * Copyright (C) 2016 Fraunhofer Institut IOSB, Fraunhoferstr. 1, D 76131
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
package de.fraunhofer.iosb.ilt.frostserver.parser.path;

import de.fraunhofer.iosb.ilt.frostserver.model.EntityType;
import de.fraunhofer.iosb.ilt.frostserver.path.PathElementArrayIndex;
import de.fraunhofer.iosb.ilt.frostserver.path.PathElementCustomProperty;
import de.fraunhofer.iosb.ilt.frostserver.path.PathElementEntity;
import de.fraunhofer.iosb.ilt.frostserver.path.PathElementEntitySet;
import de.fraunhofer.iosb.ilt.frostserver.path.PathElementProperty;
import de.fraunhofer.iosb.ilt.frostserver.path.ResourcePath;
import de.fraunhofer.iosb.ilt.frostserver.path.Version;
import de.fraunhofer.iosb.ilt.frostserver.persistence.IdManager;
import de.fraunhofer.iosb.ilt.frostserver.persistence.IdManagerLong;
import de.fraunhofer.iosb.ilt.frostserver.property.EntityPropertyMain;
import de.fraunhofer.iosb.ilt.frostserver.util.StringHelper;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.Charset;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PathParser implements ParserVisitor {

    /**
     * The logger for this class.
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(PathParser.class);

    private final IdManager idmanager;

    /**
     * Parse the given path with an IdManagerlong and UTF-8 encoding.
     *
     * @param serviceRootUrl The root URL of the service.
     * @param version The version of the service.
     * @param path The path to parse.
     * @return The parsed ResourcePath.
     */
    public static ResourcePath parsePath(String serviceRootUrl, Version version, String path) {
        return parsePath(new IdManagerLong(), serviceRootUrl, version, path, StringHelper.UTF8);
    }

    /**
     * Parse the given path, assuming UTF-8 encoding.
     *
     * @param idmanager The IdManager to use
     * @param serviceRootUrl The root URL of the service.
     * @param version The version of the service.
     * @param path The path to parse.
     * @return The parsed ResourcePath.
     */
    public static ResourcePath parsePath(IdManager idmanager, String serviceRootUrl, Version version, String path) {
        return parsePath(idmanager, serviceRootUrl, version, path, StringHelper.UTF8);
    }

    /**
     * Parse the given path.
     *
     * @param idmanager The IdManager to use.
     * @param serviceRootUrl The root URL of the service.
     * @param version The version of the service.
     * @param path The path to parse.
     * @param encoding The character encoding to use when parsing.
     * @return The parsed ResourcePath.
     */
    public static ResourcePath parsePath(IdManager idmanager, String serviceRootUrl, Version version, String path, Charset encoding) {
        ResourcePath resourcePath = new ResourcePath();
        resourcePath.setServiceRootUrl(serviceRootUrl);
        resourcePath.setVersion(version);
        if (path == null) {
            resourcePath.setPath("");
            return resourcePath;
        }
        resourcePath.setPath(path);
        LOGGER.debug("Parsing: {}", path);
        InputStream is = new ByteArrayInputStream(path.getBytes(encoding));
        Parser t = new Parser(is, StringHelper.UTF8.name());
        try {
            ASTStart start = t.Start();
            PathParser v = new PathParser(idmanager);
            start.jjtAccept(v, resourcePath);
        } catch (ParseException | TokenMgrError ex) {
            LOGGER.error("Failed to parse '{}' because: {}", StringHelper.cleanForLogging(path), ex.getMessage());
            LOGGER.trace("Exception: ", ex);
            throw new IllegalStateException("Path is not valid.");
        }
        return resourcePath;
    }

    public PathParser(IdManager idmanager) {
        this.idmanager = idmanager;
    }

    public ResourcePath defltAction(SimpleNode node, ResourcePath data) {
        if (node.value == null) {
            LOGGER.debug("{}", node);
        } else {
            LOGGER.debug("{} : ({}){}", node, node.value.getClass().getSimpleName(), node.value);
        }
        node.childrenAccept(this, data);
        return data;
    }

    private void addAsEntitiy(ResourcePath rp, SimpleNode node, EntityType type) {
        PathElementEntity epa = new PathElementEntity();
        epa.setEntityType(type);
        if (node.value != null) {
            epa.setId(idmanager.parseId(node.value.toString()));
            rp.setIdentifiedElement(epa);
        }
        epa.setParent(rp.getLastElement());
        rp.addPathElement(epa, true, false);
    }

    private void addAsEntitiySet(ResourcePath rp, EntityType type) {
        PathElementEntitySet espa = new PathElementEntitySet();
        espa.setEntityType(type);
        espa.setParent(rp.getLastElement());
        rp.addPathElement(espa, true, false);
    }

    private void addAsEntitiyProperty(ResourcePath rp, EntityPropertyMain type) {
        PathElementProperty ppe = new PathElementProperty();
        ppe.setProperty(type);
        ppe.setParent(rp.getLastElement());
        rp.addPathElement(ppe);
    }

    private void addAsCustomProperty(ResourcePath rp, SimpleNode node) {
        PathElementCustomProperty cppa = new PathElementCustomProperty();
        cppa.setName(node.value.toString());
        cppa.setParent(rp.getLastElement());
        rp.addPathElement(cppa);
    }

    private void addAsArrayIndex(ResourcePath rp, SimpleNode node) {
        PathElementArrayIndex cpai = new PathElementArrayIndex();
        String image = node.value.toString();
        if (!image.startsWith("[") && image.endsWith("]")) {
            throw new IllegalArgumentException("Received node is not an array index: " + image);
        }
        String numberString = image.substring(1, image.length() - 1);
        try {
            int index = Integer.parseInt(numberString);
            cpai.setIndex(index);
            cpai.setParent(rp.getLastElement());
            rp.addPathElement(cpai);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Array indices must be integer values. Failed to parse: " + image);
        }
    }

    @Override
    public ResourcePath visit(SimpleNode node, ResourcePath data) {
        LOGGER.error("{}: acceptor not implemented in subclass?", node);
        node.childrenAccept(this, data);
        return data;
    }

    @Override
    public ResourcePath visit(ASTStart node, ResourcePath data) {
        node.childrenAccept(this, data);
        return data;
    }

    @Override
    public ResourcePath visit(ASTIdentifiedPath node, ResourcePath data) {
        return defltAction(node, data);
    }

    @Override
    public ResourcePath visit(ASTeActuator node, ResourcePath data) {
        addAsEntitiy(data, node, EntityType.ACTUATOR);
        return defltAction(node, data);
    }

    @Override
    public ResourcePath visit(ASTcActuators node, ResourcePath data) {
        addAsEntitiySet(data, EntityType.ACTUATOR);
        return defltAction(node, data);
    }

    @Override
    public ResourcePath visit(ASTeDatastream node, ResourcePath data) {
        addAsEntitiy(data, node, EntityType.DATASTREAM);
        return defltAction(node, data);
    }

    @Override
    public ResourcePath visit(ASTcDatastreams node, ResourcePath data) {
        addAsEntitiySet(data, EntityType.DATASTREAM);
        return defltAction(node, data);
    }

    @Override
    public ResourcePath visit(ASTeMultiDatastream node, ResourcePath data) {
        addAsEntitiy(data, node, EntityType.MULTIDATASTREAM);
        return defltAction(node, data);
    }

    @Override
    public ResourcePath visit(ASTcMultiDatastreams node, ResourcePath data) {
        addAsEntitiySet(data, EntityType.MULTIDATASTREAM);
        return defltAction(node, data);
    }

    @Override
    public ResourcePath visit(ASTeFeatureOfInterest node, ResourcePath data) {
        addAsEntitiy(data, node, EntityType.FEATUREOFINTEREST);
        return defltAction(node, data);
    }

    @Override
    public ResourcePath visit(ASTcFeaturesOfInterest node, ResourcePath data) {
        addAsEntitiySet(data, EntityType.FEATUREOFINTEREST);
        return defltAction(node, data);
    }

    @Override
    public ResourcePath visit(ASTeHistLocation node, ResourcePath data) {
        addAsEntitiy(data, node, EntityType.HISTORICALLOCATION);
        return defltAction(node, data);
    }

    @Override
    public ResourcePath visit(ASTcHistLocations node, ResourcePath data) {
        addAsEntitiySet(data, EntityType.HISTORICALLOCATION);
        return defltAction(node, data);
    }

    @Override
    public ResourcePath visit(ASTeLocation node, ResourcePath data) {
        addAsEntitiy(data, node, EntityType.LOCATION);
        return defltAction(node, data);
    }

    @Override
    public ResourcePath visit(ASTcLocations node, ResourcePath data) {
        addAsEntitiySet(data, EntityType.LOCATION);
        return defltAction(node, data);
    }

    @Override
    public ResourcePath visit(ASTeSensor node, ResourcePath data) {
        addAsEntitiy(data, node, EntityType.SENSOR);
        return defltAction(node, data);
    }

    @Override
    public ResourcePath visit(ASTcSensors node, ResourcePath data) {
        addAsEntitiySet(data, EntityType.SENSOR);
        return defltAction(node, data);
    }

    @Override
    public ResourcePath visit(ASTeTask node, ResourcePath data) {
        addAsEntitiy(data, node, EntityType.TASK);
        return defltAction(node, data);
    }

    @Override
    public ResourcePath visit(ASTcTasks node, ResourcePath data) {
        addAsEntitiySet(data, EntityType.TASK);
        return defltAction(node, data);
    }

    @Override
    public ResourcePath visit(ASTeTaskingCapability node, ResourcePath data) {
        addAsEntitiy(data, node, EntityType.TASKINGCAPABILITY);
        return defltAction(node, data);
    }

    @Override
    public ResourcePath visit(ASTcTaskingCapabilities node, ResourcePath data) {
        addAsEntitiySet(data, EntityType.TASKINGCAPABILITY);
        return defltAction(node, data);
    }

    @Override
    public ResourcePath visit(ASTeThing node, ResourcePath data) {
        addAsEntitiy(data, node, EntityType.THING);
        return defltAction(node, data);
    }

    @Override
    public ResourcePath visit(ASTcThings node, ResourcePath data) {
        addAsEntitiySet(data, EntityType.THING);
        return defltAction(node, data);
    }

    @Override
    public ResourcePath visit(ASTeObservation node, ResourcePath data) {
        addAsEntitiy(data, node, EntityType.OBSERVATION);
        return defltAction(node, data);
    }

    @Override
    public ResourcePath visit(ASTcObservations node, ResourcePath data) {
        addAsEntitiySet(data, EntityType.OBSERVATION);
        return defltAction(node, data);
    }

    @Override
    public ResourcePath visit(ASTeObservedProp node, ResourcePath data) {
        addAsEntitiy(data, node, EntityType.OBSERVEDPROPERTY);
        return defltAction(node, data);
    }

    @Override
    public ResourcePath visit(ASTcObservedProps node, ResourcePath data) {
        addAsEntitiySet(data, EntityType.OBSERVEDPROPERTY);
        return defltAction(node, data);
    }

    @Override
    public ResourcePath visit(ASTpCreationTime node, ResourcePath data) {
        addAsEntitiyProperty(data, EntityPropertyMain.CREATIONTIME);
        return defltAction(node, data);
    }

    @Override
    public ResourcePath visit(ASTpId node, ResourcePath data) {
        addAsEntitiyProperty(data, EntityPropertyMain.ID);
        return defltAction(node, data);
    }

    @Override
    public ResourcePath visit(ASTpSelfLink node, ResourcePath data) {
        addAsEntitiyProperty(data, EntityPropertyMain.SELFLINK);
        return defltAction(node, data);
    }

    @Override
    public ResourcePath visit(ASTpDescription node, ResourcePath data) {
        addAsEntitiyProperty(data, EntityPropertyMain.DESCRIPTION);
        return defltAction(node, data);
    }

    @Override
    public ResourcePath visit(ASTpDefinition node, ResourcePath data) {
        addAsEntitiyProperty(data, EntityPropertyMain.DEFINITION);
        return defltAction(node, data);
    }

    @Override
    public ResourcePath visit(ASTpEncodingType node, ResourcePath data) {
        addAsEntitiyProperty(data, EntityPropertyMain.ENCODINGTYPE);
        return defltAction(node, data);
    }

    @Override
    public ResourcePath visit(ASTpFeature node, ResourcePath data) {
        addAsEntitiyProperty(data, EntityPropertyMain.FEATURE);
        return defltAction(node, data);
    }

    @Override
    public ResourcePath visit(ASTpLocation node, ResourcePath data) {
        addAsEntitiyProperty(data, EntityPropertyMain.LOCATION);
        return defltAction(node, data);
    }

    @Override
    public ResourcePath visit(ASTpMetadata node, ResourcePath data) {
        addAsEntitiyProperty(data, EntityPropertyMain.METADATA);
        return defltAction(node, data);
    }

    @Override
    public ResourcePath visit(ASTpName node, ResourcePath data) {
        addAsEntitiyProperty(data, EntityPropertyMain.NAME);
        return defltAction(node, data);
    }

    @Override
    public ResourcePath visit(ASTpObservationType node, ResourcePath data) {
        addAsEntitiyProperty(data, EntityPropertyMain.OBSERVATIONTYPE);
        return defltAction(node, data);
    }

    @Override
    public ResourcePath visit(ASTpMultiObservationDataTypes node, ResourcePath data) {
        addAsEntitiyProperty(data, EntityPropertyMain.MULTIOBSERVATIONDATATYPES);
        return defltAction(node, data);
    }

    @Override
    public ResourcePath visit(ASTpPhenomenonTime node, ResourcePath data) {
        addAsEntitiyProperty(data, EntityPropertyMain.PHENOMENONTIME);
        return defltAction(node, data);
    }

    @Override
    public ResourcePath visit(ASTpProperties node, ResourcePath data) {
        addAsEntitiyProperty(data, EntityPropertyMain.PROPERTIES);
        return defltAction(node, data);
    }

    @Override
    public ResourcePath visit(ASTpResult node, ResourcePath data) {
        addAsEntitiyProperty(data, EntityPropertyMain.RESULT);
        return defltAction(node, data);
    }

    @Override
    public ResourcePath visit(ASTpResultTime node, ResourcePath data) {
        addAsEntitiyProperty(data, EntityPropertyMain.RESULTTIME);
        return defltAction(node, data);
    }

    @Override
    public ResourcePath visit(ASTpTaskingParameters node, ResourcePath data) {
        addAsEntitiyProperty(data, EntityPropertyMain.TASKINGPARAMETERS);
        return defltAction(node, data);
    }

    @Override
    public ResourcePath visit(ASTpTime node, ResourcePath data) {
        addAsEntitiyProperty(data, EntityPropertyMain.TIME);
        return defltAction(node, data);
    }

    @Override
    public ResourcePath visit(ASTpUnitOfMeasurement node, ResourcePath data) {
        addAsEntitiyProperty(data, EntityPropertyMain.UNITOFMEASUREMENT);
        return defltAction(node, data);
    }

    @Override
    public ResourcePath visit(ASTpUnitOfMeasurements node, ResourcePath data) {
        addAsEntitiyProperty(data, EntityPropertyMain.UNITOFMEASUREMENTS);
        return defltAction(node, data);
    }

    @Override
    public ResourcePath visit(ASTcpRef node, ResourcePath data) {
        data.setRef(true);
        return defltAction(node, data);
    }

    @Override
    public ResourcePath visit(ASTppValue node, ResourcePath data) {
        data.setValue(true);
        return defltAction(node, data);
    }

    @Override
    public ResourcePath visit(ASTppSubProperty node, ResourcePath data) {
        addAsCustomProperty(data, node);
        return defltAction(node, data);
    }

    @Override
    public ResourcePath visit(ASTppArrayIndex node, ResourcePath data) {
        addAsArrayIndex(data, node);
        return defltAction(node, data);
    }

    @Override
    public ResourcePath visit(ASTLong node, ResourcePath data) {
        return defltAction(node, data);
    }

    @Override
    public ResourcePath visit(ASTString node, ResourcePath data) {
        return defltAction(node, data);
    }

    @Override
    public ResourcePath visit(ASTpObservedArea node, ResourcePath data) {
        addAsEntitiyProperty(data, EntityPropertyMain.OBSERVEDAREA);
        return defltAction(node, data);
    }

    @Override
    public ResourcePath visit(ASTpParameters node, ResourcePath data) {
        addAsEntitiyProperty(data, EntityPropertyMain.PARAMETERS);
        return defltAction(node, data);
    }

    @Override
    public ResourcePath visit(ASTpResultQuality node, ResourcePath data) {
        addAsEntitiyProperty(data, EntityPropertyMain.RESULTQUALITY);
        return defltAction(node, data);
    }

    @Override
    public ResourcePath visit(ASTpValidTime node, ResourcePath data) {
        addAsEntitiyProperty(data, EntityPropertyMain.VALIDTIME);
        return defltAction(node, data);
    }

}

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
package de.fraunhofer.iosb.ilt.frostserver.persistence.orakeljooq.utils.fieldmapper;

import de.fraunhofer.iosb.ilt.frostserver.model.loader.DefNavigationProperty;
import de.fraunhofer.iosb.ilt.frostserver.persistence.IdManager;
import de.fraunhofer.iosb.ilt.frostserver.persistence.orakeljooq.OrakelPersistenceManager;
import de.fraunhofer.iosb.ilt.frostserver.persistence.orakeljooq.relations.RelationOneToMany;
import de.fraunhofer.iosb.ilt.frostserver.persistence.orakeljooq.tables.StaMainTable;
import de.fraunhofer.iosb.ilt.frostserver.persistence.orakeljooq.utils.PropertyFieldRegistry;
import de.fraunhofer.iosb.ilt.frostserver.property.NavigationPropertyMain;
import org.jooq.Table;
import org.jooq.TableField;

/**
 * The field mapper for Id fields.
 *
 * @author hylke
 */
public class FieldMapperOneToMany extends FieldMapperAbstract {

    /**
     * The name of the field in "my" table.
     */
    private String field;
    /**
     * The name of the other table we link to.
     */
    private String otherTableSchema;
    /**
     * The name of the other table we link to.
     */
    private String otherTable;
    /**
     * The field in the other table that is the key in the relation.
     */
    private String otherField;

    private int fieldIdx;
    private int fieldIdxOther;

    private DefNavigationProperty parent;

    @Override
    public void setParent(DefNavigationProperty parent) {
        this.parent = parent;
    }

    @Override
    public void registerField(OrakelPersistenceManager ppm, StaMainTable staTable) {
        fieldIdx = getOrRegisterField(ppm, field, staTable);
    }

    @Override
    public <J extends Comparable<J>, T extends StaMainTable<J, T>> void registerMapping(OrakelPersistenceManager ppm, T staTable) {
        final StaMainTable staTableOther = (StaMainTable) ppm.getTableCollection().getTableForName(otherTable);
        final Table dbTableOther = ppm.getDbTable(getOtherTableSchema(), otherTable);
        final NavigationPropertyMain navProp = parent.getNavigationProperty();

        PropertyFieldRegistry<J, T> pfReg = staTable.getPropertyFieldRegistry();
        IdManager idManager = ppm.getIdManager();
        pfReg.addEntry(navProp, t -> t.field(fieldIdx), idManager);

        fieldIdxOther = getOrRegisterField(ppm, otherField, staTableOther);
        staTable.registerRelation(new RelationOneToMany(navProp, staTable, staTableOther)
                .setSourceFieldAccessor(t -> (TableField) t.field(fieldIdx))
                .setTargetFieldAccessor(t -> (TableField) t.field(fieldIdxOther))
        );

        final DefNavigationProperty.Inverse inverse = parent.getInverse();
        if (inverse != null) {
            final NavigationPropertyMain navPropInverse = parent.getNavigationPropertyInverse();
            final PropertyFieldRegistry<J, T> pfRegOther = staTableOther.getPropertyFieldRegistry();
            pfRegOther.addEntry(navPropInverse, t -> t.field(fieldIdxOther), idManager);
            staTableOther.registerRelation(new RelationOneToMany(navPropInverse, staTableOther, staTable)
                    .setSourceFieldAccessor(t -> (TableField) t.field(fieldIdxOther))
                    .setTargetFieldAccessor(t -> (TableField) t.field(fieldIdx))
            );
        }
    }

    /**
     * The name of the field in "my" table.
     *
     * @return the field
     */
    public String getField() {
        return field;
    }

    /**
     * The name of the field in "my" table.
     *
     * @param field the field to set.
     * @return this.
     */
    public FieldMapperOneToMany setField(String field) {
        this.field = field;
        return this;
    }

    /**
     * The name of the other table we link to.
     *
     * @return the otherTableSchema
     */
    public String getOtherTableSchema() {
        return otherTableSchema;
    }

    /**
     * The name of the other table we link to.
     *
     * @param otherTableSchema the otherTableSchema to set.
     * @return this.
     */
    public FieldMapperOneToMany setOtherTableSchema(String otherTableSchema) {
        this.otherTableSchema = otherTableSchema;
        return this;
    }

    /**
     * The name of the other table we link to.
     *
     * @return the otherTable
     */
    public String getOtherTable() {
        return otherTable;
    }

    /**
     * The name of the other table we link to.
     *
     * @param otherTable the otherTable to set.
     * @return this.
     */
    public FieldMapperOneToMany setOtherTable(String otherTable) {
        this.otherTable = otherTable;
        return this;
    }

    /**
     * The field in the other table that is the key in the relation.
     *
     * @return the otherField
     */
    public String getOtherField() {
        return otherField;
    }

    /**
     * The field in the other table that is the key in the relation.
     *
     * @param otherField the otherField to set.
     * @return this.
     */
    public FieldMapperOneToMany setOtherField(String otherField) {
        this.otherField = otherField;
        return this;
    }

}

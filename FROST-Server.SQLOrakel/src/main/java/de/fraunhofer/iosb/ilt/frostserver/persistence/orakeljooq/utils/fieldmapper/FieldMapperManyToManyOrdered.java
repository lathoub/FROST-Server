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
import de.fraunhofer.iosb.ilt.frostserver.persistence.orakeljooq.relations.RelationManyToManyOrdered;
import de.fraunhofer.iosb.ilt.frostserver.persistence.orakeljooq.tables.StaLinkTable;
import de.fraunhofer.iosb.ilt.frostserver.persistence.orakeljooq.tables.StaMainTable;
import de.fraunhofer.iosb.ilt.frostserver.persistence.orakeljooq.utils.PropertyFieldRegistry;
import de.fraunhofer.iosb.ilt.frostserver.property.NavigationPropertyMain;
import org.jooq.TableField;

/**
 * The field mapper for Id fields.
 *
 * @author hylke
 */
public class FieldMapperManyToManyOrdered extends FieldMapperAbstract {

    /**
     * The name of the field in "my" table.
     */
    private String field;
    /**
     * Name of the schema of the link table.
     */
    private String linkTableSchema;
    /**
     * Name of the link table.
     */
    private String linkTable;
    /**
     * Name of "my" field in the link table.
     */
    private String linkOurField;
    /**
     * Name of the other table's field in the link table.
     */
    private String linkOtherField;
    /**
     * Name of the sorting field in the link table.
     */
    private String linkRankField;
    /**
     * The name of the schema of the other table we link to.
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
    /**
     * Flag indicating duplicates should be removed when following the relation
     * over a navigationLink.
     */
    private boolean distinct = false;
    /**
     * Flag indicating duplicates should be removed when following the inverse
     * relation over a navigationLink.
     */
    private boolean distinctInverse = false;

    private int fieldIdx;
    private int fieldIdxLinkOur;
    private int fieldIdxLinkOther;
    private int fieldIdxLinkRank;
    private int fieldIdxOther;

    private DefNavigationProperty parent;

    @Override
    public void setParent(DefNavigationProperty parent) {
        this.parent = parent;
    }

    @Override
    public void registerField(OrakelPersistenceManager opm, StaMainTable staTable) {
        fieldIdx = getOrRegisterField(opm, field, staTable);
    }

    @Override
    public <J extends Comparable<J>, T extends StaMainTable<J, T>> void registerMapping(OrakelPersistenceManager opm, T staTable) {
        final StaMainTable staTableOther = (StaMainTable) opm.getTableCollection().getTableForName(otherTable);
        fieldIdxOther = getOrRegisterField(opm, otherField, staTableOther);

        final StaLinkTable staTableLink = opm.getOrCreateLinkTable(getLinkTableSchema(), linkTable);
        fieldIdxLinkOur = getOrRegisterField(opm, linkOurField, staTableLink);
        fieldIdxLinkOther = getOrRegisterField(opm, linkOtherField, staTableLink);
        fieldIdxLinkRank = getOrRegisterField(opm, linkRankField, staTableLink);

        final NavigationPropertyMain navProp = parent.getNavigationProperty();
        final PropertyFieldRegistry<J, T> pfReg = staTable.getPropertyFieldRegistry();
        final IdManager idManager = opm.getIdManager();
        pfReg.addEntry(navProp, t -> t.field(fieldIdx), idManager);

        staTable.registerRelation(new RelationManyToManyOrdered(navProp, staTable, staTableLink, staTableOther)
                .setAlwaysDistinct(distinct)
                .setOrderFieldAcc(t -> (TableField) t.field(fieldIdxLinkRank))
                .setSourceFieldAcc(t -> (TableField) t.field(fieldIdx))
                .setSourceLinkFieldAcc(t -> (TableField) t.field(fieldIdxLinkOur))
                .setTargetLinkFieldAcc(t -> (TableField) t.field(fieldIdxLinkOther))
                .setTargetFieldAcc(t -> (TableField) t.field(fieldIdxOther))
        );

        final DefNavigationProperty.Inverse inverse = parent.getInverse();
        if (inverse != null) {
            final NavigationPropertyMain navPropInverse = parent.getNavigationPropertyInverse();
            final PropertyFieldRegistry<?, ?> pfRegOther = staTableOther.getPropertyFieldRegistry();
            pfRegOther.addEntry(navPropInverse, t -> t.field(fieldIdxOther), idManager);
            staTableOther.registerRelation(new RelationManyToManyOrdered(navPropInverse, staTableOther, staTableLink, staTable)
                    .setAlwaysDistinct(isDistinctInverse())
                    .setOrderFieldAcc(t -> (TableField) t.field(fieldIdxLinkRank))
                    .setSourceFieldAcc(t -> (TableField) t.field(fieldIdxOther))
                    .setSourceLinkFieldAcc(t -> (TableField) t.field(fieldIdxLinkOther))
                    .setTargetLinkFieldAcc(t -> (TableField) t.field(fieldIdxLinkOur))
                    .setTargetFieldAcc(t -> (TableField) t.field(fieldIdx))
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
    public FieldMapperManyToManyOrdered setField(String field) {
        this.field = field;
        return this;
    }

    /**
     * Name of the link table.
     *
     * @return the linkTableSchema
     */
    public String getLinkTableSchema() {
        return linkTableSchema;
    }

    /**
     * Name of the link table.
     *
     * @param linkTableSchema the linkTableSchema to set.
     * @return this.
     */
    public FieldMapperManyToManyOrdered setLinkTableSchema(String linkTableSchema) {
        this.linkTableSchema = linkTableSchema;
        return this;
    }

    /**
     * Name of the link table.
     *
     * @return the linkTable
     */
    public String getLinkTable() {
        return linkTable;
    }

    /**
     * Name of the link table.
     *
     * @param linkTable the linkTable to set.
     * @return this.
     */
    public FieldMapperManyToManyOrdered setLinkTable(String linkTable) {
        this.linkTable = linkTable;
        return this;
    }

    /**
     * Name of "my" field in the link table.
     *
     * @return the linkOurField
     */
    public String getLinkOurField() {
        return linkOurField;
    }

    /**
     * Name of "my" field in the link table.
     *
     * @param linkOurField the linkOurField to set.
     * @return this.
     */
    public FieldMapperManyToManyOrdered setLinkOurField(String linkOurField) {
        this.linkOurField = linkOurField;
        return this;
    }

    /**
     * Name of the other table's field in the link table.
     *
     * @return the linkOtherField
     */
    public String getLinkOtherField() {
        return linkOtherField;
    }

    /**
     * Name of the other table's field in the link table.
     *
     * @param linkOtherField the linkOtherField to set.
     * @return this.
     */
    public FieldMapperManyToManyOrdered setLinkOtherField(String linkOtherField) {
        this.linkOtherField = linkOtherField;
        return this;
    }

    /**
     * Name of the sorting field in the link table.
     *
     * @return the linkOrderField
     */
    public String getLinkRankField() {
        return linkRankField;
    }

    /**
     * Name of the sorting field in the link table.
     *
     * @param linkRankField the linkOrderField to set.
     * @return this.
     */
    public FieldMapperManyToManyOrdered setLinkRankField(String linkRankField) {
        this.linkRankField = linkRankField;
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
    public FieldMapperManyToManyOrdered setOtherTableSchema(String otherTableSchema) {
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
    public FieldMapperManyToManyOrdered setOtherTable(String otherTable) {
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
    public FieldMapperManyToManyOrdered setOtherField(String otherField) {
        this.otherField = otherField;
        return this;
    }

    /**
     * Flag indicating duplicates should be removed when following the relation
     * over a navigationLink.
     *
     * @return the distinct
     */
    public boolean isDistinct() {
        return distinct;
    }

    /**
     * Flag indicating duplicates should be removed when following the relation
     * over a navigationLink.
     *
     * @param distinct the distinct to set.
     * @return this.
     */
    public FieldMapperManyToManyOrdered setDistinct(boolean distinct) {
        this.distinct = distinct;
        return this;
    }

    /**
     * Flag indicating duplicates should be removed when following the inverse
     * relation over a navigationLink.
     *
     * @return the distinctInverse
     */
    public boolean isDistinctInverse() {
        return distinctInverse;
    }

    /**
     * Flag indicating duplicates should be removed when following the inverse
     * relation over a navigationLink.
     *
     * @param distinctInverse the distinctInverse to set.
     * @return this.
     */
    public FieldMapperManyToManyOrdered setDistinctInverse(boolean distinctInverse) {
        this.distinctInverse = distinctInverse;
        return this;
    }

}

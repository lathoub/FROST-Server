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
package de.fraunhofer.iosb.ilt.frostserver.persistence.orakeljooq.relations;

import de.fraunhofer.iosb.ilt.frostserver.model.EntityType;
import de.fraunhofer.iosb.ilt.frostserver.persistence.orakeljooq.OrakelPersistenceManager;
import de.fraunhofer.iosb.ilt.frostserver.persistence.orakeljooq.QueryBuilder;
import de.fraunhofer.iosb.ilt.frostserver.persistence.orakeljooq.tables.StaMainTable;
import de.fraunhofer.iosb.ilt.frostserver.persistence.orakeljooq.utils.QueryState;
import de.fraunhofer.iosb.ilt.frostserver.persistence.orakeljooq.utils.TableRef;
import de.fraunhofer.iosb.ilt.frostserver.property.NavigationPropertyMain;
import org.jooq.Field;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A relation from a source table to a target table.
 *
 * @author hylke
 * @param <J> The ID type
 * @param <S> The source table
 * @param <T> The target table
 */
public class RelationOneToMany<J extends Comparable, S extends StaMainTable<J, S>, T extends StaMainTable<J, T>> implements Relation<J, S> {

    private static final Logger LOGGER = LoggerFactory.getLogger(RelationOneToMany.class.getName());
    /**
     * The target entity type of the relation.
     */
    private final EntityType targetType;
    /**
     * The name of the relation. For official relations, this is the (singular)
     * entity type name.
     */
    private final String name;

    /**
     * The field on the source side that defines the relation.
     */
    private FieldAccessor<J, S> sourceFieldAccessor;

    /**
     * The table that is the target side of the relation.
     */
    private final T target;

    /**
     * The field on the target side that defines the relation.
     */
    private FieldAccessor<J, T> targetFieldAccessor;

    /**
     * Flag indicating if following this relation means the query needs to be
     * DISTINCT.
     */
    private final boolean distinctRequired;

    public RelationOneToMany(NavigationPropertyMain navProp, S source, T target) {
        this(navProp.getName(), source, target, navProp.getEntityType(), navProp.isEntitySet());
    }

    public RelationOneToMany(String name, S source, T target, EntityType targetType, boolean distinctRequired) {
        if (source == null) {
            // Source is only used for finding the generics...
            LOGGER.error("NULL source");
        }
        this.target = target;
        this.targetType = targetType;
        this.name = name;
        this.distinctRequired = distinctRequired;
    }

    public RelationOneToMany<J, S, T> setSourceFieldAccessor(FieldAccessor<J, S> sourceFieldAccessor) {
        this.sourceFieldAccessor = sourceFieldAccessor;
        return this;
    }

    public RelationOneToMany<J, S, T> setTargetFieldAccessor(FieldAccessor<J, T> targetFieldAccessor) {
        this.targetFieldAccessor = targetFieldAccessor;
        return this;
    }

    @Override
    public TableRef<J> join(S joinSource, QueryState<J, ?> queryState, TableRef<J> sourceRef) {
        Field<J> sourceField = sourceFieldAccessor.getField(joinSource);
        T targetAliased = (T) target.as(queryState.getNextAlias());
        Field<J> targetField = targetFieldAccessor.getField(targetAliased);
        queryState.setSqlFrom(queryState.getSqlFrom().innerJoin(targetAliased).on(targetField.eq(sourceField)));
        if (distinctRequired) {
            queryState.setDistinctRequired(distinctRequired);
        }
        return QueryBuilder.createJoinedRef(sourceRef, targetType, targetAliased);
    }

    /**
     * Re-links the one-to-many relation to a different entity. Updates the
     * targetField to sourceId on TargetTable where TargetTable.getId =
     * targetId.
     *
     * @param pm The PersistenceManager for queries.
     * @param sourceId The source id of the link.
     * @param targetId The target id of the link.
     */
    @Override
    public void link(OrakelPersistenceManager<J> pm, J sourceId, J targetId) {
        if (!distinctRequired) {
            throw new IllegalStateException("Trying to update a one-to-many relation from the wrong side.");
        }
        int count = pm.getDslContext().update(target)
                .set(targetFieldAccessor.getField(target), sourceId)
                .where(target.getId().eq(targetId))
                .execute();
        if (count != 1) {
            LOGGER.error("Executing query did not result in an update!");
        }
    }

    /**
     * The name of the relation. For official relations, this is the (singular)
     * entity type name.
     *
     * @return the name
     */
    @Override
    public String getName() {
        return name;
    }

}

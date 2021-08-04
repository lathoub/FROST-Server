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

import de.fraunhofer.iosb.ilt.frostserver.persistence.orakeljooq.OrakelPersistenceManager;
import de.fraunhofer.iosb.ilt.frostserver.persistence.orakeljooq.QueryBuilder;
import de.fraunhofer.iosb.ilt.frostserver.persistence.orakeljooq.tables.StaMainTable;
import de.fraunhofer.iosb.ilt.frostserver.persistence.orakeljooq.tables.StaTable;
import de.fraunhofer.iosb.ilt.frostserver.persistence.orakeljooq.utils.QueryState;
import de.fraunhofer.iosb.ilt.frostserver.persistence.orakeljooq.utils.TableRef;
import de.fraunhofer.iosb.ilt.frostserver.property.NavigationPropertyMain;
import de.fraunhofer.iosb.ilt.frostserver.query.OrderBy;
import org.jooq.DSLContext;
import org.jooq.Field;

/**
 * A relation from a source table to a target table using a link table ordered
 * by a long value.
 *
 * @author hylke
 * @param <J> The type of the identifiers used in the database.
 * @param <S> The source table.
 * @param <L> The link table linking source and target entities.
 * @param <T> The target table.
 */
public class RelationManyToManyOrdered<J extends Comparable, S extends StaMainTable<J, S>, L extends StaTable<J, L>, T extends StaMainTable<J, T>> extends RelationManyToMany<J, S, L, T> {

    /**
     * The field used for the ordering.
     */
    private FieldAccessor<Integer, L> orderFieldAcc;
    private boolean alwaysDistinct = false;

    public RelationManyToManyOrdered(NavigationPropertyMain navProp, S source, L linkTable, T target) {
        super(navProp, source, linkTable, target);
    }

    public RelationManyToManyOrdered<J, S, L, T> setOrderFieldAcc(FieldAccessor<Integer, L> orderFieldAcc) {
        this.orderFieldAcc = orderFieldAcc;
        return this;
    }

    public RelationManyToManyOrdered<J, S, L, T> setAlwaysDistinct(boolean alwaysDistinct) {
        this.alwaysDistinct = alwaysDistinct;
        return this;
    }

    @Override
    public TableRef<J> join(S source, QueryState<J, ?> queryState, TableRef<J> sourceRef) {
        T targetAliased = (T) getTarget().as(queryState.getNextAlias());
        L linkTableAliased = (L) getLinkTable().as(queryState.getNextAlias());
        Field<J> sourceField = getSourceFieldAcc().getField(source);
        Field<J> sourceLinkField = getSourceLinkFieldAcc().getField(linkTableAliased);
        Field<J> targetLinkField = getTargetLinkFieldAcc().getField(linkTableAliased);
        Field<J> targetField = getTargetFieldAcc().getField(targetAliased);
        queryState.setSqlFrom(queryState.getSqlFrom().innerJoin(linkTableAliased).on(sourceLinkField.eq(sourceField)));
        queryState.setSqlFrom(queryState.getSqlFrom().innerJoin(targetAliased).on(targetField.eq(targetLinkField)));
        if (alwaysDistinct || queryState.isFilter()) {
            queryState.setDistinctRequired(true);
        } else {
            Field<Integer> orderField = orderFieldAcc.getField(linkTableAliased);
            queryState.getSqlSortFields().add(orderField, OrderBy.OrderType.ASCENDING);
        }
        return QueryBuilder.createJoinedRef(sourceRef, getTargetType(), targetAliased);
    }

    @Override
    public void link(OrakelPersistenceManager<J> pm, J sourceId, J targetId) {
        final DSLContext dslContext = pm.getDslContext();
        final L linkTable = getLinkTable();
        final Field<J> sourceLinkField = getSourceLinkFieldAcc().getField(linkTable);
        final Field<J> targetLinkField = getTargetLinkFieldAcc().getField(linkTable);
        final Field<Integer> orderField = orderFieldAcc.getField(linkTable);
        dslContext.insertInto(linkTable)
                .set(sourceLinkField, sourceId)
                .set(targetLinkField, targetId)
                .set(
                        orderField,
                        dslContext.selectCount()
                                .from(linkTable)
                                .where(sourceLinkField.equal(sourceId))
                )
                .execute();
    }

}

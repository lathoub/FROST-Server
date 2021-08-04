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
import de.fraunhofer.iosb.ilt.frostserver.persistence.orakeljooq.tables.StaMainTable;
import de.fraunhofer.iosb.ilt.frostserver.persistence.orakeljooq.utils.QueryState;
import de.fraunhofer.iosb.ilt.frostserver.persistence.orakeljooq.utils.TableRef;

/**
 * The interface for table-to-table relations.
 *
 * @author hylke
 * @param <J> the type of the ID.
 * @param <S> the type of the table linked from.
 */
public interface Relation<J extends Comparable, S extends StaMainTable<J, S>> {

    /**
     * The name of the relation. For official relations, this is the (singular)
     * entity type name.
     *
     * @return the name
     */
    public String getName();

    public TableRef<J> join(S source, QueryState<J, ?> queryState, TableRef<J> sourceRef);

    /**
     * Create a link between the given source and target ids. This is not
     * necessarily supported for all implementations, in all directions. For
     * some types of relations this means creating new entries in a link table,
     * for other implementations it may mean existing relations are changed.
     *
     * @param pm The persistence manager to use for accessing the database.
     * @param sourceId The id of the entry in the source table.
     * @param targetId The id of the entry in the target table.
     */
    public void link(OrakelPersistenceManager<J> pm, J sourceId, J targetId);
}

/*
 * Copyright (C) 2020 Fraunhofer Institut IOSB, Fraunhoferstr. 1, D 76131
 * Karlsruhe, Germany.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library. If not, see <http://www.gnu.org/licenses/>.
 */
package de.fraunhofer.iosb.ilt.frostserver.persistence.orakeljooq.tables;

import org.jooq.Binding;
import org.jooq.DataType;
import org.jooq.Name;
import org.jooq.Record;
import org.jooq.Table;
import org.jooq.impl.DSL;

/**
 *
 * @author hylke
 * @param <J> The type of the ID fields.
 * @param <T> The exact type of the implementing class.
 */
public interface StaTable<J extends Comparable, T extends StaTable<J, T>> extends Table<Record> {

    @Override
    public StaTable<J, T> as(String name);

    public T getThis();

    public default int registerField(String name, DataType type) {
        return registerField(DSL.name(name), type, null);
    }

    public default int registerField(String name, DataType type, Binding binding) {
        return registerField(DSL.name(name), type, binding);
    }

    public default int registerField(Name name, DataType type) {
        return registerField(name, type, null);
    }

    public int registerField(Name name, DataType type, Binding binding);
}

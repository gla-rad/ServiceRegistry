/*
 * Copyright (c) 2021 Maritime Connectivity Platform Consortium
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.maritimeconnectivity.serviceregistry.models.dto.datatables;

/**
 * The type Order.
 *
 * The Datatables Order Class definition.
 *
 * @author Nikolaos Vastardis (email: Nikolaos.Vastardis@gla-rad.org)
 */
public class DtOrder {

    // Class Variables
    private Integer column;
    private DtDirection dir;

    /**
     * Instantiates a new Order.
     */
    public DtOrder() {

    }

    /**
     * Gets column.
     *
     * @return the column
     */
    public Integer getColumn() {
        return column;
    }

    /**
     * Sets column.
     *
     * @param column the column
     */
    public void setColumn(Integer column) {
        this.column = column;
    }

    /**
     * Gets dir.
     *
     * @return the dir
     */
    public DtDirection getDir() {
        return dir;
    }

    /**
     * Sets dir.
     *
     * @param dir the dir
     */
    public void setDir(DtDirection dir) {
        this.dir = dir;
    }
}


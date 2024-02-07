/*
 * Copyright (c) 2024 Maritime Connectivity Platform Consortium
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.maritimeconnectivity.serviceregistry.models.dto.datatables;

/**
 * The type DtColumn.
 *
 * The Datatables Column Class definition.
 *
 * @author Nikolaos Vastardis (email: Nikolaos.Vastardis@gla-rad.org)
 */
public class DtColumn {

    // Class Variables
    private String data;
    private String name;
    private Boolean searchable;
    private Boolean orderable;
    private DtSearch search;

    /**
     * Instantiates a new Column.
     */
    public DtColumn() {

    }

    /**
     * Instantiates a new Column.
     *
     * @param data the data
     */
    public DtColumn(String data) {
        this.data = data;
    }

    /**
     * Gets data.
     *
     * @return the data
     */
    public String getData() {
        return data;
    }

    /**
     * Sets data.
     *
     * @param data the data
     */
    public void setData(String data) {
        this.data = data;
    }

    /**
     * Gets name.
     *
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * Sets name.
     *
     * @param name the name
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Gets searchable.
     *
     * @return the searchable
     */
    public Boolean getSearchable() {
        return searchable;
    }

    /**
     * Sets searchable.
     *
     * @param searchable the searchable
     */
    public void setSearchable(Boolean searchable) {
        this.searchable = searchable;
    }

    /**
     * Gets orderable.
     *
     * @return the orderable
     */
    public Boolean getOrderable() {
        return orderable;
    }

    /**
     * Sets orderable.
     *
     * @param orderable the orderable
     */
    public void setOrderable(Boolean orderable) {
        this.orderable = orderable;
    }

    /**
     * Gets search.
     *
     * @return the search
     */
    public DtSearch getSearch() {
        return search;
    }

    /**
     * Sets search.
     *
     * @param search the search
     */
    public void setSearch(DtSearch search) {
        this.search = search;
    }

}

/*
 * Copyright (c) 2025 Maritime Connectivity Platform Consortium
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

import org.apache.lucene.search.SortField;
import org.apache.lucene.search.SortedNumericSortField;
import org.apache.lucene.search.SortedSetSortField;
import org.springframework.data.domain.PageRequest;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * The type DtPagingRequest.
 *
 * The Datatables Paging Request Class definition.
 *
 * @author Nikolaos Vastardis (email: Nikolaos.Vastardis@gla-rad.org)
 */
public class DtPagingRequest {

    // Class Variables
    private int start;
    private int length;
    private int draw;
    private List<DtOrder> order;
    private List<DtColumn> columns;
    private DtSearch search;

    /**
     * Instantiates a new Paging request.
     */
    public DtPagingRequest() {

    }

    /**
     * Gets start.
     *
     * @return the start
     */
    public int getStart() {
        return start;
    }

    /**
     * Sets start.
     *
     * @param start the start
     */
    public void setStart(int start) {
        this.start = start;
    }

    /**
     * Gets length.
     *
     * @return the length
     */
    public int getLength() {
        return length;
    }

    /**
     * Sets length.
     *
     * @param length the length
     */
    public void setLength(int length) {
        this.length = length;
    }

    /**
     * Gets draw.
     *
     * @return the draw
     */
    public int getDraw() {
        return draw;
    }

    /**
     * Sets draw.
     *
     * @param draw the draw
     */
    public void setDraw(int draw) {
        this.draw = draw;
    }

    /**
     * Gets order.
     *
     * @return the order
     */
    public List<DtOrder> getOrder() {
        return order;
    }

    /**
     * Sets order.
     *
     * @param order the order
     */
    public void setOrder(List<DtOrder> order) {
        this.order = order;
    }

    /**
     * Gets columns.
     *
     * @return the columns
     */
    public List<DtColumn> getColumns() {
        return columns;
    }

    /**
     * Sets columns.
     *
     * @param columns the columns
     */
    public void setColumns(List<DtColumn> columns) {
        this.columns = columns;
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
     * @param dtSearch the search
     */
    public void setSearch(DtSearch dtSearch) {
        this.search = dtSearch;
    }

    /**
     * Constructs a Springboot Data Sort object based on the information of the
     * datatables page request.
     *
     * @return the Springboot sort definition
     */
    public org.springframework.data.domain.Sort getSpringbootSort() {
        // Create the Springboot sorting and direction
        org.springframework.data.domain.Sort sort = org.springframework.data.domain.Sort.unsorted();
        for(DtOrder dtOrder : this.getOrder()) {
            String columnName = this.getColumns().get(dtOrder.getColumn()).getData();
            sort = sort.and(dtOrder.getDir() == DtDirection.asc ?
                    org.springframework.data.domain.Sort.by(columnName).ascending() :
                    org.springframework.data.domain.Sort.by(columnName).descending());
        }
        return sort;
    }

    /**
     * Constructs a Lucence Data Sort object based on the information of the
     * datatables page request.
     *
     * @param diffSortFields The fields that have been sorted under a different index
     * @return the Springboot sort definition
     */
    public org.apache.lucene.search.Sort getLucenceSort(List<String> diffSortFields) {
        // Create the Lucene sorting and direction
        List<SortField> sortFields = this.getOrder().stream()
                .map(dtOrder -> {
                    String field = this.getColumns().get(dtOrder.getColumn()).getData();
                    field = Optional.ofNullable(diffSortFields).orElseGet(() -> Collections.emptyList()).contains(field) ? field + "_sort" : field;
                    if(field.equals("id_sort") || field.equals("publishedAt_sort") || field.equals("lastUpdatedAt_sort")) {
                        return new SortedNumericSortField(field, SortField.Type.LONG, dtOrder.getDir() == DtDirection.desc);
                    } else {
                        return new SortedSetSortField(field, dtOrder.getDir() == DtDirection.desc);
                    }
                })
                .collect(Collectors.toList());
        return new org.apache.lucene.search.Sort(sortFields.toArray(new SortField[]{}));
    }

    /**
     * Constructs a Springboot JPA page request of the of the datatables page
     * request.
     *
     * @return the Springboot JPA page request
     */
    public PageRequest toPageRequest() {
        // Create the springboot pagination request
        return PageRequest.of(this.getStart()/this.getLength(), this.getLength(), this.getSpringbootSort());
    }

}

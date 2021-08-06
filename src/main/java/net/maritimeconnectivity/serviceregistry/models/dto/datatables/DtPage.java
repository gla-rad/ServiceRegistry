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

import org.springframework.data.domain.Page;

import java.util.List;
import java.util.stream.Collectors;

/**
 * The type DtPage.
 *
 * @param <T> the type parameter
 */
public class DtPage<T> {

    private List<T> data;
    private int recordsFiltered;
    private int recordsTotal;
    private int draw;

    /**
     * Instantiates a new Dt page.
     */
    public DtPage() {

    }

    /**
     * Instantiates a new Dt page.
     *
     * @param data the data
     */
    public DtPage(List<T> data) {
        this.data = data;
    }

    /**
     * Instantiates a new Dt page.
     *
     * @param page the Springboot page
     * @param dtPagingRequest the datatables paging request
     */
    public DtPage(Page<T> page, DtPagingRequest dtPagingRequest) {
        this(page.getContent().stream().collect(Collectors.toList()));
        this.setRecordsFiltered((int)  page.getTotalElements());
        this.setRecordsTotal((int) page.getTotalElements());
        this.setDraw(dtPagingRequest.getDraw());
    }

    /**
     * Gets data.
     *
     * @return the data
     */
    public List<T> getData() {
        return data;
    }

    /**
     * Sets data.
     *
     * @param data the data
     */
    public void setData(List<T> data) {
        this.data = data;
    }

    /**
     * Gets records filtered.
     *
     * @return the records filtered
     */
    public int getRecordsFiltered() {
        return recordsFiltered;
    }

    /**
     * Sets records filtered.
     *
     * @param recordsFiltered the records filtered
     */
    public void setRecordsFiltered(int recordsFiltered) {
        this.recordsFiltered = recordsFiltered;
    }

    /**
     * Gets records total.
     *
     * @return the records total
     */
    public int getRecordsTotal() {
        return recordsTotal;
    }

    /**
     * Sets records total.
     *
     * @param recordsTotal the records total
     */
    public void setRecordsTotal(int recordsTotal) {
        this.recordsTotal = recordsTotal;
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
}

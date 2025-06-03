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

package net.maritimeconnectivity.serviceregistry.models.dto.secom;

import java.util.List;

/**
 * The ResponseSearchObjectWithCert Class.
 * <p/>
 * This class mirrors the standard SECOM ResponseSearchObject object but
 * actually uses the internal SearchObjectResultWithCert objects into
 * the search service results list. This is not actually required in the
 * application runtime, but it's useful for the tests.
 *
 * @author Nikolaos Vastardis (email: Nikolaos.Vastardis@gla-rad.org)
 */
public class ResponseSearchObjectWithCert {

    // Class Variables
    List<SearchObjectResultWithCert> searchServiceResult;

    /**
     * Gets search service result.
     *
     * @return the search service result
     */
    public List<SearchObjectResultWithCert> getSearchServiceResult() {
        return searchServiceResult;
    }

    /**
     * Sets search service result.
     *
     * @param searchServiceResult the search service result
     */
    public void setSearchServiceResult(List<SearchObjectResultWithCert> searchServiceResult) {
        this.searchServiceResult = searchServiceResult;
    }

}

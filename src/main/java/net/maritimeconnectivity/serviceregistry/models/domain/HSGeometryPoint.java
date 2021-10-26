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

package net.maritimeconnectivity.serviceregistry.models.domain;

import org.hibernate.search.engine.spatial.GeoPoint;

import javax.persistence.Basic;
import javax.persistence.Embeddable;

@Embeddable
public class HSGeometryPoint implements GeoPoint {

    @Basic
    private Double latitude;

    @Basic
    private Double longitude;

    protected HSGeometryPoint() {
        // For Hibernate ORM
    }

    public HSGeometryPoint(double latitude, double longitude) {
        this.latitude = latitude;
        this.longitude = longitude;
    }

    @Override
    public double latitude() {
        return latitude;
    }

    @Override
    public double longitude() {
        return longitude;
    }
}

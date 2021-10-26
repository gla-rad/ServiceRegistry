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
import org.hibernate.search.engine.spatial.GeoPolygon;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;

import javax.persistence.Embeddable;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Embeddable
public abstract class HSGeometry extends Geometry implements GeoPolygon {

    public HSGeometry(GeometryFactory factory) {
        super(factory);
    }

    @Override
    public List<GeoPoint> points() {
        return Stream.of(this.getCoordinates())
                .map(point -> new GeoPoint() {
                    @Override
                    public double latitude() {
                        return point.getX();
                    }

                    @Override
                    public double longitude() {
                        return point.getX();
                    }
                })
                .collect(Collectors.toList());
    }
}

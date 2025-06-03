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

package net.maritimeconnectivity.serviceregistry.utils;

import org.apache.lucene.spatial.SpatialStrategy;
import org.apache.lucene.spatial.prefix.RecursivePrefixTreeStrategy;
import org.apache.lucene.spatial.prefix.tree.GeohashPrefixTree;
import org.apache.lucene.spatial.prefix.tree.SpatialPrefixTree;
import org.hibernate.search.backend.lucene.LuceneExtension;
import org.hibernate.search.mapper.pojo.bridge.ValueBridge;
import org.hibernate.search.mapper.pojo.bridge.binding.ValueBindingContext;
import org.hibernate.search.mapper.pojo.bridge.mapping.programmatic.ValueBinder;
import org.hibernate.search.mapper.pojo.bridge.runtime.ValueBridgeFromIndexedValueContext;
import org.hibernate.search.mapper.pojo.bridge.runtime.ValueBridgeToIndexedValueContext;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.spatial4j.context.jts.JtsSpatialContext;
import org.locationtech.spatial4j.shape.jts.JtsGeometry;

import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;

/**
 * The Hibernate Search Geometry Value Binder.
 *
 * This value binder class is used in order for Hibernate Search to generate
 * indexable fields from the geometry variables of each instance and then
 * be able to perform search queries on them using their Lucene indexes.
 *
 * @author Nikolaos Vastardis (email: Nikolaos.Vastardis@gla-rad.org)
 */
public class GeometryBinder implements ValueBinder {

    /**
     * <p>
     * The main binding operation where the geometry value bridge is used and
     * the geometry objects are indexed based on a recursive strategy depending
     * on the search level.
     * </p><p>
     * This is based on a previous implementation for the same problem on an
     * older hibernate search release, but had to be developed for hibernate
     * search 6. See the following links for more information
     * </p>
     * <ul>
     *     <li>
     *         <a>https://stackoverflow.com/questions/39440184/hibernate-search-query-all-the-entities-intersecting-point</a>
     *     </li>
     *     <li>
     *         <a>https://docs.jboss.org/hibernate/stable/search/reference/en-US/html_single</a>
     *     </li>
     * </ul>
     *
     * @param context    The value binding context.
     */
    @Override
    public void bind(ValueBindingContext<?> context) {
        context.bridge(
                Geometry.class,
                new GeometryValueBridge(),
                context.typeFactory()
                        .extension(LuceneExtension.get())
                        .asNative(
                                Geometry.class,
                                (absoluteFieldPath, value, collector) -> {
                                    JtsSpatialContext spatialContext = JtsSpatialContext.GEO;
                                    SpatialPrefixTree grid = new GeohashPrefixTree(spatialContext, 22);
                                    // Preparing the tree strategy field
                                    SpatialStrategy treeStrategy = new RecursivePrefixTreeStrategy(grid, "geometry");
                                    Optional.of(value)
                                            .map(v -> new JtsGeometry(v, spatialContext, false, true))
                                            .map(treeStrategy::createIndexableFields)
                                            .map(Arrays::asList)
                                            .orElse(Collections.emptyList())
                                            .stream()
                                            .forEach(collector::accept);
                                }
                        )
        );
    }

    /**
     * The private Geometry Value Bride that does pretty much nothing, just
     * returns the geometry value fields as they are.
     */
    private static class GeometryValueBridge implements ValueBridge<Geometry, Geometry> {
        @Override
        public Geometry toIndexedValue(Geometry value, ValueBridgeToIndexedValueContext context) {
            return value;
        }

        @Override
        public Geometry fromIndexedValue(Geometry value, ValueBridgeFromIndexedValueContext context) {
            return value;
        }
    }

}

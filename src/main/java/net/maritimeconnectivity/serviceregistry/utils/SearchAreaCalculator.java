package net.maritimeconnectivity.serviceregistry.utils;

import jakarta.persistence.EntityManager;
import lombok.extern.slf4j.Slf4j;
import net.maritimeconnectivity.serviceregistry.models.domain.SearchArea;
import org.apache.lucene.spatial.prefix.RecursivePrefixTreeStrategy;
import org.apache.lucene.spatial.prefix.tree.GeohashPrefixTree;
import org.apache.lucene.spatial.prefix.tree.SpatialPrefixTree;
import org.apache.lucene.spatial.query.SpatialArgs;
import org.apache.lucene.spatial.query.SpatialOperation;
import org.hibernate.search.backend.lucene.LuceneExtension;
import org.hibernate.search.mapper.orm.Search;
import org.hibernate.search.mapper.orm.scope.SearchScope;
import org.hibernate.search.mapper.orm.session.SearchSession;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.spatial4j.context.jts.JtsSpatialContext;
import org.locationtech.spatial4j.shape.jts.JtsGeometry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Component
@Slf4j
public class SearchAreaCalculator {

    @Autowired
    EntityManager entityManager;

    private final String G1191_SEARCHAREA_PREFIX = "urn:mrn:mcp:msr:search:searcharea:";

    public ArrayList<String> areaToSubjectMapper(List<SearchArea> areas) {
        ArrayList<String> subjects = new ArrayList<>();

        //Map results to subjects
        areas.forEach(searchArea -> {
            String subject = G1191_SEARCHAREA_PREFIX + searchArea.getName().toLowerCase();
            subjects.add(subject);
        });

        return subjects;

    }


    /**
     * This method calculates the subject based on the geometry provided in the search parameters.
     *
     * @param geometry The geometry string in WKT format from which to calculate the subject.
     *                 example = "POLYGON ((0.65 51.42, 0.65 52.26, 2.68 52.26, 2.68 51.42, 0.65 51.42))")
     * @return A string representing the subject derived from the geometry.
     */
    public List<SearchArea> findIntersectingSearchAreas(Geometry geometry) {
        log.debug("--Calculator searching for intersecting search areas for provided geometry");

        //Create Luscene query
        JtsSpatialContext ctx = JtsSpatialContext.GEO;
        int maxLevels = 12; //results in sub-meter precision for geohash
        SpatialPrefixTree grid = new GeohashPrefixTree(ctx, maxLevels);
        RecursivePrefixTreeStrategy strategy = new RecursivePrefixTreeStrategy(grid, "geometry");

        // Create the Lucene GeoSpatial Query
        var geoQuery = Optional.ofNullable(geometry)
                .map(g -> new SpatialArgs(SpatialOperation.Intersects, new JtsGeometry(g, ctx, false, true)))
                .map(strategy::makeQuery)
                .orElse(null);


        //Run the query - should find intersections in order to return areas of interest (only the areas!)
        SearchSession searchSession = Search.session( entityManager );
        SearchScope<SearchArea> scope = searchSession.scope( SearchArea.class );

        var lazyResults = searchSession.search( scope )
                .where(f -> f.bool()
                        .must(q2 -> Optional.ofNullable(geoQuery)
                                .map(q2.extension(LuceneExtension.get())::fromLuceneQuery)
                                .orElseGet(q2::matchAll)
                        )
                )
                .toQuery();

        List<SearchArea> hits = lazyResults.fetchHits(100); // Limit to 100 results for safety
        return hits;

    }
}

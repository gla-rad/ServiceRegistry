/**
 * Leaflet Draw Control Configurations
 */
var drawControlFull = undefined;
var drawControlEditOnly =  undefined;

/**
 * Initialises the map and sets the default view and zoom to the UK.
 *
 * @param  {string}         container       The map container to initialise the map for
 */
function initMap(container) {
    // Initialise the map before we need it
    var map = L.map(container).setView([54.910, -3.432], 5);
    L.tileLayer('http://{s}.tile.osm.org/{z}/{x}/{y}.png', {
        attribution: '&copy; <a href="http://osm.org/copyright">OpenStreetMap</a> contributors'
    }).addTo(map);
    return map;
}

/**
 * Initialises the and sets the default view and zoom to the UK like above. This
 * function however will also initialise a feature group for items to be drawn
 * on the map itself.
 *
 * @param  {string}         container       The map container to initialise the map for
 * @param  {FeatureGroup}   drawnItems      The feature items drawn on the map
 */
function initMapWithDrawnItems(container, drawnItems, allowDraw) {
    // Initialise the map
    var map = initMap(container);

    // FeatureGroup is to store editable layers
    map.addLayer(drawnItems);

    // Initialise the drawing control
    if(allowDraw) {
        initDrawControlFull(drawnItems);
        initDrawControlEditOnly(drawnItems);
    }

    // Handle the leaflet draw create events
    map.on('draw:created', function(e) {
        var type = e.layerType;
        var layer = e.layer;

        // Do whatever else you need to. (save to db, add to map etc)
        drawnItems.addLayer(layer);
    });

    // Returns the initialised map
    return map;
}

/**
 * Reset the map back to its original view.
 *
 * @param  {string}         container       The map container to initialise the map for
 */
function resetMapView(map) {
    instanceEditCoverageMap.setView([54.910, -3.432], 5);
}

/**
 * Initialises the full drawing control in the leaflet draw layer.
 *
 * @param  {FeatureGroup}   drawnItems      The feature items drawn on the map
 */
function initDrawControlFull(drawnItems) {
    drawControlFull = new L.Control.Draw({
        draw: {
            marker: false,
            polyline: true,
            polygon: true,
            rectangle: true,
            circle: false,
            circlemarker: false,
        },
        edit: {
            featureGroup: drawnItems
        }
    });
}

/**
 * Initialises the edit only drawing control in the leaflet draw layer.
 *
 * @param  {FeatureGroup}   drawnItems      The feature items drawn on the map
 */
function initDrawControlEditOnly(drawnItems) {
    drawControlEditOnly = new L.Control.Draw({
        edit: {
            featureGroup: drawnItems,
            remove: true
        },
        draw: false
    });
}

/**
 * This function will load the instance geometry onto the drawnItems variable
 * so that it is shown in the map's layers.
 *
 * @param  {GeoJSON}        geometry        The geometry to be loaded onto the map
 * @param  {Map}            map             The map to load the geometry into
 * @param  {FeatureGroup}   drawnItems      The feature items drawn on the map
 */
function loadGeometryOnMap(geometry, map, drawnItems) {
    // Recreate the drawn items feature group
    drawnItems.clearLayers();
    if(geometry) {
        var geomLayer = L.geoJson(geometry);
        addNonGroupLayers(geomLayer, drawnItems);
        map.setView(geomLayer.getBounds().getCenter(), 5);
        setTimeout(function() {
            map.fitBounds(geomLayer.getBounds());
        }, 700);
    }
}

/**
 * Would benefit from https://github.com/Leaflet/Leaflet/issues/4461
 */
function addNonGroupLayers(sourceLayer, targetGroup) {
    if (sourceLayer instanceof L.LayerGroup) {
        sourceLayer.eachLayer(function(layer) {
            addNonGroupLayers(layer, targetGroup);
        });
    } else {
        targetGroup.addLayer(sourceLayer);
    }
}

/**
 * A helper function that picks up all the drawn items and translates then into
 * a GeoJSON format and combines them into a geometry collection.
 *
 * @param  {FeatureGroup}   drawnItems      The feature items drawn on the map
 */
function getGeometryCollectionFromMap(drawnItems) {
    // Initialise a geometry collection
    var geometry = {
        type: "GeometryCollection",
        geometries: []
    };
    drawnItems.toGeoJSON().features.forEach(feature => {
        geometry.geometries.push(feature.geometry);
    });
    // And return
    return geometry;
}

/**
 * A helper function that picks up a single geometry from the drawn items and
 * translates then into a GeoJSON format.
 *
 * @param  {FeatureGroup}   drawnItems      The feature items drawn on the map
 */
function getSingleGeometryFromMap(drawnItems) {
    // Initialise a single geometry object
    var geometry = undefined;
    drawnItems.toGeoJSON().features.forEach(feature => {
        geometry = feature.geometry;
    });
    // And return
    return geometry;
}
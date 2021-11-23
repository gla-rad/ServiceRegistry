/**
 * Global variables
 */
var searchMap = undefined;
var instancesTable = undefined;
var drawControl = undefined;
var drawControlFull = undefined;
var drawControlEditOnly = undefined;
var instanceItems = undefined;
var geoSpatialSearchMode = "geoJson";

/**
 * The Instances Search Table Column Definitions
 * @type {Array}
 */
var columnDefs = [{
    data: "id",
    title: "ID",
    type: "hidden",
    visible: false,
    searchable: false
}, {
    data: "name",
    title: "Name",
    readonly : true,
    hoverMsg: "Name of service",
    placeholder: "Name of the service"
}, {
    data: "version",
    title: "Version",
    readonly : true,
    hoverMsg: "Version of service",
    placeholder: "Version of the service"
}, {
    data: "serviceType",
    title: "Type",
    readonly : true,
    hoverMsg: "Type of service",
    placeholder: "Type of the service"
}, {
    data: "status",
    title: "Status",
    readonly : true,
    hoverMsg: "Status of service",
    placeholder: "Status of the service"
}, {
    data: "endpointUri",
    title: "Endpoint URI",
    readonly : true,
    hoverMsg: "Access point of service",
    placeholder: "Access point of the service"
}];

/**
 * Standard jQuery initialisation of the page.
 */
$(() => {
    // Now also initialise the search map before we need it
    searchMap = L.map('searchMap').setView([54.910, -3.432], 5);
    L.tileLayer('http://{s}.tile.osm.org/{z}/{x}/{y}.png', {
        attribution: '&copy; <a href="http://osm.org/copyright">OpenStreetMap</a> contributors'
    }).addTo(searchMap);

    // FeatureGroup is to store editable layers
    drawnItems = new L.FeatureGroup();
    searchMap.addLayer(drawnItems);
    instanceItems = new L.FeatureGroup();
    searchMap.addLayer(instanceItems);

    // Add the draw toolbar
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
    drawControlEditOnly = new L.Control.Draw({
        edit: {
            featureGroup: drawnItems,
            remove: true
        },
        draw: false
    });

    // Handle the leaflet draw create events
    searchMap.on('draw:created', function (e) {
        // Do whatever else you need to. (save to db, add to map etc)
        var type = e.layerType;
        var layer = e.layer;
        drawnItems.addLayer(layer);

        // Restrict new shapes, only allow edit
        drawControlFull.remove(searchMap);
        drawControlEditOnly.addTo(searchMap)

        // Convert the geometry to WKT if the search mode is enabled
        if(geoSpatialSearchMode === "WKT") {
            populateWKTTextArea();
        }
    });

    // Handle the leaflet draw edit events
    searchMap.on('draw:edited', function (e) {
        // Convert the geometry to WKT if the search mode is enabled
        if(geoSpatialSearchMode === "WKT") {
            populateWKTTextArea();
        }
    });

    // Handle the leaflet draw delete events
    searchMap.on('draw:deleted', function (e) {
        // Allow users to add new shapes, not only editing
        if (drawnItems.getLayers().length === 0){
            drawControlEditOnly.remove(searchMap);
            drawControlFull.addTo(searchMap);
        };

        // Convert the geometry to WKT if the search mode is enabled
        if(geoSpatialSearchMode === "WKT") {
            populateWKTTextArea();
        }
    });

    // Also link the instance search button with the enter key
    $("#queryString").keypress(function(event) {
        if (event.keyCode === 13) {
            $("#instanceSearchButton").click();
        }
    });

    // Monitor the WKT string to update the selected area in the map
    $('#geometryWKT').on("input propertychange", function() {
        // For valid text inputs, try to parse the WKT string
        if(this.value && this.value.trim().length>0) {
            var parsedGeoJson = undefined;
            try {
                parsedGeoJson = Terraformer.WKT.parse(this.value);
            } catch(ex) {
                // Nothing to do
            }

            // If a valid GeoJSON object was parsed, replace it in the map
            if(parsedGeoJson) {
                drawnItems.clearLayers();
                addNonGroupLayers(L.geoJson(parsedGeoJson), drawnItems);
            }
        }
    });
});

/**
 * The primary function to search for instances using the back-end API.
 */
function searchForInstances() {
    // Get the search query string
    var queryString =  $("#queryString").val();

    // Get the search query geometry
    var queryGeometry = getGeoSpatialSearchGeometry();

    // Get the global/local search selection
    var globalSearch = $("#searchType :selected").val() === "global";

    // Sanity Check
    if((!queryString || queryString.trim() === "") && (!queryGeometry)) {
        showError("Please provide a valid query to proceed with the search...");
        destroyInstancesTable();
        return;
    }

    // Perform the api search
    loadInstancesTable(queryString, JSON.stringify(queryGeometry), $("#geometryWKT").val(), globalSearch);
}

/**
 * The primary function to define a geo-spatial search parameter to be used
 * while searching for instances using the back-end API geo-sp.
 */
function geoSearchForInstances() {
    // Refresh the search map control
    drawControlFull.remove(searchMap);
    drawControlEditOnly.remove(searchMap);
    if($("#instanceGeoSearchButton").hasClass("active")) {
        drawControlFull.addTo(searchMap);
        if(geoSpatialSearchMode === "WKT") {
            $("#geometryWKTArea").show();
        }
    } else {
        // Recreate the drawn items feature group
        drawnItems.clearLayers();
        if(geoSpatialSearchMode === "WKT") {
            clearWTKTextArea();
            $("#geometryWKTArea").hide();
        }
    }
}

/**
 * Using the Search API endpoint for instances, this function will load the
 * instance results table and show the matching entries.
 *
 * @param  {string} queryString     The instance query string to be used
 * @param  {string} queryGeoJSON    The instance geometry query GeoJSON string
 * @param  {string} queryWKT        The instance geometry query WKT string
 * @param  {boolean} globalSearch   Whether the global ledger search facility should be user
 */
function loadInstancesTable(queryString, queryGeoJSON, queryWKT, globalSearch) {
    // Destroy the matrix if it already exists
    instanceItems.clearLayers();
    destroyInstancesTable();

    // Now initialise the instances table
    instancesTable = $('#instancesTable').DataTable({
        ajax: {
            url: '/api/_search/instances',
            type: 'GET',
            contentType: 'application/json',
            crossDomain: true,
            data: {
                queryString: queryString,
                geometry: geoSpatialSearchMode === 'geoJson' ? queryGeoJSON : null,
                geometryWKT: geoSpatialSearchMode === 'WKT' ? queryWKT.trim() : null,
                globalSearch: globalSearch,
                page: 0,
                size: 100
            },
            dataSrc: function (json) {
                return json;
            },
            error: function (jqXHR, ajaxOptions, thrownError) {
                showError(getErrorFromHeader(jqXHR, "Error while trying to search for instances!"));
                destroyInstancesTable();
            }
        },
        columns: columnDefs,
        dom: "Brtip",
        select: 'single',
        lengthMenu: [10, 25, 50, 75, 100],
        responsive: true,
        initComplete: (settings, json) => {
            hideLoader();
        }
    });

    // Show the loader on processing
    instancesTable.on( 'processing.dt', function(e, settings, processing) {
        processing ? showLoader(false) : hideLoader();;
    });

    // On an instance selection, draw the area on the map
    instancesTable.on( 'select', function ( e, dt, type, indexes ) {
        if ( type === 'row' ) {
            var idx = dt.cell('.selected', 0).index();
            var data = dt.row(idx.row).data();
            loadInstanceCoverage( e, dt, type, indexes );
        }
    });
}

/**
 * Destroys the instance results table so that it get removed from the DOM and
 * can be re-initialised for the next search.
 */
function destroyInstancesTable() {
    if (instancesTable) {
        instancesTable.destroy();
        instancesTable = undefined;
        $("#instancesTable").empty();
    }
}

/**
 * This function will load the station geometry onto the instanceItems variable
 * so that it is shown in the station maps layers.
 *
 * @param {Event}         event         The event that took place
 * @param {DataTable}     table         The AtoN type table
 * @param {Node}          button        The button node that was pressed
 * @param {Configuration} config        The table configuration
 */
function loadInstanceCoverage(event, table, button, config) {
    var idx = table.cell('.selected', 0).index();
    var data = instancesTable.row({selected : true}).data();
    var geometry = data.geometry;

    // Recreate the drawn items feature group
    instanceItems.clearLayers();
    if(geometry) {
        var geomLayer = L.geoJson(geometry);
        addNonGroupLayers(geomLayer, instanceItems);
        searchMap.fitBounds(geomLayer.getBounds());
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
 */
function getGeoSpatialSearchGeometry() {
    // Initialise a geometry collection
    var queryGeometry = undefined;
    // Add all the draw items GeoJSON definition - We only allow one!!!
    drawnItems.toGeoJSON().features.forEach(feature => {
        queryGeometry = feature.geometry;
    });
    // And return
    return queryGeometry;
}

/**
 * Turns on the WKT mode of the Geo-Spatial search mechanism. This will make
 * the WKT text area visible and fill it in with the geographic area WKT
 * information from the drawn geometries.
 */
function setGeoSpatialSearchMode(searchType) {
    // Save the selection
    $("#" + geoSpatialSearchMode + "Option").removeClass("fw-bold");
    geoSpatialSearchMode = searchType;
    $("#" + geoSpatialSearchMode + "Option").addClass("fw-bold");

    // Show the WKT text area field if the WKT type is enabled
    if(geoSpatialSearchMode === "WKT") {
        // Only show the WKT if we have an active geometry search
        if($("#instanceGeoSearchButton").hasClass("active")) {
            populateWKTTextArea();
            $("#geometryWKTArea").show();
        }
    }
    // Otherwise clear out and hide
    else {
        clearWTKTextArea();
        $("#geometryWKTArea").hide();
    }
}

/**
 * Populates the WKT area with the latest information from the leadlet map
 * drawing session. Note that currently only one shape is allowed at each time.
 * This is mainly due to the fact that the Terraformer WKT library does not
 * support parsing GEOMETRYCOLLECTION WKT strings.
 */
function populateWKTTextArea() {
    var geometry = getGeoSpatialSearchGeometry();
    if(geometry) {
        $("#geometryWKT").val(Terraformer.WKT.convert(geometry));
    } else {
        clearWTKTextArea();
    }
}

/**
 * This function simply clears out the WKT area.
 */
function clearWTKTextArea() {
    $("#geometryWKT").val("");
}
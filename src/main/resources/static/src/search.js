/**
 * Global variables
 */
var searchMap = undefined;
var instancesTable = undefined;
var drawControl = undefined;
var drawnItems = undefined;
var instanceItems = undefined;

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

    // Handle the leaflet draw create events
    searchMap.on('draw:created', function (e) {
        var type = e.layerType;
        var layer = e.layer;

        // Do whatever else you need to. (save to db, add to map etc)
        drawnItems.addLayer(layer);
    });

    // Add the draw toolbar
    drawControl = new L.Control.Draw({
        draw: {
            marker: false,
            polyline: true,
            polygon: true,
            rectangle: true,
            circle: false,
            circlemarker: false,
        },
        edit: {
            featureGroup: drawnItems,
            remove: true
        }
    });

    // Also link the instance search button with the enter key
    $("#queryString").keypress(function(event) {
    if (event.keyCode === 13) {
        $("#instanceSearchButton").click();
    }
});
});

/**
 * The Instances Table Column Definitions
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
 * The primary function to search for instances using the back-end API.
 */
function searchForInstances() {
    // Get the search query string
    var queryString =  $("#queryString").val();

    // Get the search query geometry
    var queryGeometry = {
        type: "GeometryCollection",
        geometries: []
    };
    drawnItems.toGeoJSON().features.forEach(feature => {
        queryGeometry.geometries.push(feature.geometry);
    });

    // Sanity Check
    if((!queryString || queryString.trim() === "") && (!queryGeometry || queryGeometry.geometries.length == 0)) {
        showError("Please provide a valid query to proceed with the search...");
        destroyInstancesTable();
        return;
    }

    // Perform the api search
    loadInstancesTable(queryString, queryGeometry.geometries.length > 0 ? JSON.stringify(queryGeometry) : null);
}

/**
 * The primary function to define a geo-spatial search parameter to be used
 * while searching for instances using the back-end API geo-sp.
 */
function geoSearchForInstances() {
    // Refresh the search map control
    searchMap.removeControl(drawControl);
    if($("#instanceGeoSearchButton").hasClass("active")) {
        searchMap.addControl(drawControl);
    } else {
        // Recreate the drawn items feature group
        drawnItems.clearLayers();
    }
}

/**
 * Using the Search API endpoint for instances, this function will load the
 * instance results table and show the matching entries.
 *
 * @param  {string} queryString     The instance query string to be used
 * @param  {string} queryGeometry   The instance geometry query GeoJSON string
 */
function loadInstancesTable(queryString, queryGeometry) {
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
                geometry: queryGeometry,
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
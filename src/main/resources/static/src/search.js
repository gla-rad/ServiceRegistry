var searchMap = undefined;
var instancesTable = undefined;
var drawControl = undefined;
var drawControlFull = undefined;
var drawControlEditOnly = undefined;
var instanceItems = undefined;
var geoSpatialSearchMode = "geoJson";
var currentTransactionId = null;
var retrieveTimers = [];
var countdownIntervalId = null;
var countdownRemainingMs = 0;
const GLOBAL_COUNTDOWN_TOTAL_MS = 10000;

/**
 * The SECOM search parameters that are defined as lists.
 * @type {Array}
 */
const searchParameterLists = ["keywords"];

/**
 * The SECOM service instance status values, which are transferred as their
 * numeric representation.
 * @type {Object}
 */
const serviceInstanceStatus = {
    0: "PROVISIONAL",
    1: "RELEASED",
    2: "DEPRECATED",
    3: "DELETED"
};

/**
 * The Instances Search Table Column Definitions
 * @type {Array}
 */
var columnDefs = [{
    data: "instanceId",
    title: "InstanceID",
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
    data: "dataProductType",
    title: "Data",
    readonly : true,
    hoverMsg: "Data product type",
    placeholder: "Type of the data product",
    defaultContent: "",
    render: data => Array.isArray(data) ? data.join(", ") : (data || "")
}, {
    data: "status",
    title: "Status",
    readonly : true,
    hoverMsg: "Status of service",
    placeholder: "Status of the service",
    defaultContent: "",
    render: data => serviceInstanceStatus[data] || ""
}, {
    data: "endpointUri",
    title: "Endpoint URI",
    readonly : true,
    hoverMsg: "Access point of service",
    placeholder: "Access point of the service",
    defaultContent: ""
}, {
    data: "coverageArea",
    title: "Coverage Area",
    type: "hidden",
    visible: false,
    searchable: false,
    defaultContent: ""
}, {
    data: "localResult",
    title: "Local Result",
    readonly: true,
    hoverMsg: "Whether the result was found locally",
    placeholder: "Whether the result was found locally",
}];

/**
 * Standard jQuery initialisation of the page.
 */
$(() => {
    // Now also initialise the search map before we need it
    searchMap = initMap('searchMap');

    // FeatureGroup is to store editable layers
    drawnItems = new L.FeatureGroup();
    searchMap.addLayer(drawnItems);
    instanceItems = new L.FeatureGroup();
    searchMap.addLayer(instanceItems);

    // Initialise the draw controls
    initDrawControlFull(drawnItems);
    initDrawControlEditOnly(drawnItems);

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

    // Also initialise the data product type multi-select
    $('#dataProductType').select2({
        placeholder: "Data Product Type",
        theme: "bootstrap-5",
        selectionCssClass: 'select2--small',
        dropdownCssClass: 'select2--small'
    });

    // Initialise the instance edit panel as read-only
    initInstanceEditPanel($('#instanceViewPanel'));
});

/**
 * Loads the PKCS#12 keystore selected by the user, so that the SECOM search
 * envelopes can be signed with the identity it contains.
 */
function loadSigningKeystore() {
    var $status = $("#signingKeystoreStatus");
    var file = $("#signingKeystore").prop("files")[0];

    // Sanity check
    if(!file) {
        SecomSigning.clearKeystore();
        showKeystoreStatus("Please select a keystore file to load...", false);
        return;
    }

    // Try to load the keystore and report back to the user
    $status.removeClass("d-none").html("Loading the keystore...");
    SecomSigning.loadKeystore(file, $("#signingKeystorePassword").val())
        .then(info => {
            showKeystoreStatus(`Signing as <strong>${info.mrn || "an unknown identity"}</strong> `
                + `using ${info.algorithm}, with a certificate chain of ${info.chainLength}.`, true);
        })
        .catch(ex => {
            SecomSigning.clearKeystore();
            showKeystoreStatus(`Unable to load the keystore: ${ex.message}`, false);
        });
}

/**
 * Displays the outcome of the last keystore loading operation.
 *
 * @param  {string} message         The message to be displayed
 * @param  {boolean} success        Whether the operation was successful
 */
function showKeystoreStatus(message, success) {
    $("#signingKeystoreStatus")
        .removeClass("d-none text-success text-danger")
        .addClass(success ? "text-success" : "text-danger")
        .html(message);
}

/**
 * The primary function to search for instances using the back-end API.
 */
function searchForInstances() {
    // Get the search query string
    var queryString =  $("#queryString").val();

    // Get the search query geometry
    var queryGeometry = getSingleGeometryFromMap(drawnItems);

    // Get the global/local search selection
    var globalSearch = $("#searchType :selected").val() === "global";

    // Sanity Check
    if((!queryString || queryString.trim() === "") && (!queryGeometry)) {
        showError("Please provide a valid query to proceed with the search...");
        destroyInstancesTable();
        hideGlobalSearchLoading()
        return;
    }

    // SECOM v2 will only accept signed search envelopes
    if(!SecomSigning.isLoaded()) {
        showError("Please load a signing keystore to proceed with the search...");
        destroyInstancesTable();
        hideGlobalSearchLoading()
        return;
    }
    if (globalSearch) {
        showGlobalSearchLoading()
    } else {
        hideGlobalSearchLoading()
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
 * @param  {boolean} globalSearch   Whether the global search facility should be user
 */
function loadInstancesTable(queryString, queryGeoJSON, queryWKT, globalSearch) {
    // Destroy the matrix if it already exists
    instanceItems.clearLayers();
    destroyInstancesTable();

    // Construct the SECOM search parameters object
    let searchParameters = {}

    // Try to parse the query string
    if (queryString && queryString.trim() !== "") {
        // By default try to use the specified lucene indexing terms
        if(queryString.includes(":")){
            // Now add all terms specified - if possible
            queryString.split(" ").forEach(term => {
                if(term.includes(":")) {
                    termQuery = term.split(":");
                    // The list-valued parameters always have to be provided as
                    // arrays, otherwise the generated envelope signature will
                    // not match the one expected by the server.
                    searchParameters[termQuery[0]] = searchParameterLists.includes(termQuery[0])
                        ? [termQuery[1]]
                        : termQuery[1];
                }
            });
        }
        // If no query terms where specified, just use the keywords
        else {
            searchParameters["keywords"] = queryString.split();
        }
    }

    // Finally we can declare the SECOM search filter envelope
    let searchFilterEnvelope = {
        'query': searchParameters,
        'geometry': geoSpatialSearchMode === 'geoJson' ? queryGeoJSON : queryWKT.trim(),
        'localOnly': !globalSearch
    }

    // Now initialise the instances table
    instancesTable = $('#instancesTable').DataTable({
        processing: true,
        // The SECOM v2 envelopes have to be signed before they are submitted,
        // which is an asynchronous operation, so the request is performed
        // manually here, instead of letting DataTables handle it.
        ajax: function (data, callback, settings) {
            SecomSigning.signSearchFilterObject(searchFilterEnvelope)
                .then(searchFilterObject => $.ajax({
                    url: `api/secom/v2/searchService`,
                    type: 'POST',
                    contentType: 'application/json; charset=utf-8',
                    crossDomain: true,
                    data: JSON.stringify(searchFilterObject)
                }))
                .then(json => {
                    // Pick up the transaction ID for the global search follow-ups
                    if (globalSearch && json && json.envelope && json.envelope.transactionId) {
                        currentTransactionId = json.envelope.transactionId;
                        scheduleRetrieveResults(currentTransactionId);
                    }

                    // Ensure the service instances are an array and tag them as local results
                    const services = json && json.envelope && Array.isArray(json.envelope.serviceInstance)
                        ? json.envelope.serviceInstance
                        : [];
                    callback({data: services.map(service => ({...service, localResult: true}))});
                })
                .catch(error => {
                    // The signing errors are local, while the AJAX ones carry a response
                    showError(error.status
                        ? getErrorFromHeader(error, "Error while trying to search for instances!")
                        : `Error while trying to sign the search request: ${error.message}`);
                    destroyInstancesTable();
                });
        },
        columns: columnDefs,
        dom: "Brtip",
        select: 'single',
        lengthMenu: [10, 25, 50, 75, 100],
        responsive: true
    });

    // On an instance selection, draw the area on the map
    instancesTable.on('select', function (e, dt, type, indexes) {
        if (type === 'row') {
            loadGeometryOnMap(getCoverageAreaGeoJson(dt.row({ selected: true }).data()),
                searchMap, instanceItems, false);
        }
    });

    // Handle double clicks on instance to open the view dialog
    instancesTable.on('dblclick', 'tr', function(e) {
        // Select the double-clicked row in the table
        e.stopPropagation();
        instancesTable.row(this).select();

        // Load and show the instance view dialog
        var $modalDiv = $('#instanceViewPanel');
        loadInstanceEditPanel($modalDiv);
        $modalDiv.modal("toggle");
    });
}

/**
 * Schedule retrieveResults calls at +3s, +6s, +10s for the given transaction ID.
 */
function scheduleRetrieveResults(txId) {
    clearRetrieveTimers();

    [3000, 6000, 10000].forEach(ms => {
        const isLast = (ms === 10000);
        const t = setTimeout(() => fetchAndMergeResults(txId, isLast), ms);
        retrieveTimers.push(t);
    });
}

/**
 * Clear any pending retrieve timers.
 */
function clearRetrieveTimers() {
    retrieveTimers.forEach(clearTimeout);
    retrieveTimers = [];
}

/**
 * Fetch additional results for a transaction and append them to the table.
 * Server handles duplicate suppression.
 */
function fetchAndMergeResults(txId, isLast) {
    // Just like the search, the retrieve result envelope has to be signed
    SecomSigning.signRetrieveResultObject(txId)
        .then(retrieveResultObject => $.ajax({
            url: `api/secom/v2/retrieveResult`,
            type: 'POST',
            contentType: 'application/json; charset=utf-8',
            crossDomain: true,
            data: JSON.stringify(retrieveResultObject)
        }))
        .then(json => {
            const services = json && json.envelope && Array.isArray(json.envelope.serviceInstance)
                ? json.envelope.serviceInstance
                : [];
            if (services.length && instancesTable) {
                instancesTable.rows.add(services.map(s => ({ ...s, localResult: false }))).draw(false);
            }
            if (isLast) markGlobalSearchComplete();
        })
        .catch(() => {
            // Even on error, we consider the last cycle “complete”.
            if (isLast) markGlobalSearchComplete();
        });
}

/**
 * The SECOM v2 service instances describe their coverage areas as a list of
 * WKT strings, so a conversion is required before they can be displayed onto
 * the search map.
 *
 * @param  {Object} instance        The SECOM service instance object
 * @return {Object} the GeoJSON representation of the instance coverage area
 */
function getCoverageAreaGeoJson(instance) {
    // Sanity check
    if(!instance || !Array.isArray(instance.coverageArea) || instance.coverageArea.length === 0) {
        return undefined;
    }

    // Parse all the provided coverage areas and combine them
    try {
        return {
            type: "GeometryCollection",
            geometries: instance.coverageArea.map(wkt => Terraformer.WKT.parse(wkt))
        };
    } catch(ex) {
        console.error(ex);
        return undefined;
    }
}

/**
 * Destroys the instance results table so that it get removed from the DOM and
 * can be re-initialised for the next search.
 */
function destroyInstancesTable() {
    if (instancesTable) {
        instancesTable.clear();
        instancesTable.destroy();
        instancesTable = undefined;
        $("#instancesTable").empty();
    }
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
    var geometry = getSingleGeometryFromMap(drawnItems);
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

/**
 * A helper function that sort out the instance edit panel dialog so that it is
 * shown as read-only and no functionality buttons are available. Also note
 * that the instance coverage tab is hidden since we can see the instance's
 * coverage area in the search map.
 */
function initInstanceEditPanel($modalDiv) {
    // Always init in the info tab
    $('#instanceTabs button:first').tab('show');

    // Hide the coverage and all functionality buttons
    $('#instanceTabs button:last').addClass('d-none');

    // Disable all the input fields
    $('form[name="instanceEditPanelForm"] :input').each(function() {
        $(this).attr('disabled', true);
    });

    // Hide all the functionality buttons, apart from downloading documents
    $('#g1128SideBar button').addClass('d-none');
    $('.btn-clear-instance-doc').addClass('d-none');
    $('.btn-download-instance-doc').attr('disabled', false);

    // Don't allow users to upload new instance documents
    $modalDiv.find("#instanceAsDocWithValue").show();
    $modalDiv.find("#instanceAsDoc").hide();

    // Link the download instance doc button functionality
    $('#instanceViewPanel').on('click', '.btn-download-instance-doc', (e) => {
        var $modalDiv = $(e.delegateTarget);
        var selectedRow = instancesTable.row({selected : true});
        if(selectedRow && selectedRow.data()["instanceAsDoc"]) {
            downloadDoc(selectedRow.data()["instanceAsDoc"].id);
        }
    });
}

/**
 * This helper function loads the XML and field data from the selected instance
 * in the instance table onto the edit dialog.
 *
 * @param {Component}   $modalDiv       The modal component performing the operation
 */
function loadInstanceEditPanel($modalDiv) {
    // First always clear to be sure
    clearInstanceEditPanel($modalDiv);

    // If a row has been selected load the data into the form
    if(instancesTable.row({selected : true})) {
        // Populate the form
        var rowData = instancesTable.row({selected : true}).data();
        var g1128Compliant = rowData['instanceAsXml'] != null;
        $('#g1128CompliantButton').prop('checked', g1128Compliant);

        // Populate all the form fields
        $('form[name="instanceEditPanelForm"] :input').each(function() {
            // Make sure the input element has an ID
            if(!$(this).attr('id')) {
                return;
            }
            // Populate all the input fields with the respective values
            if(!$(this).attr('id').startsWith("instanceAsDoc")) {
                $(this).val(rowData[$(this).attr('id')]);
            }
            // Handle the instance as doc case separately
            else if($(this).attr('id') == "instanceAsDocName") {
                if(rowData["instanceAsDoc"]) {
                    $(this).val(rowData["instanceAsDoc"].name);
                }
            }
        });
        $('form[name="instanceEditPanelForm"] select').each(function() {
            // Make sure the select element has an ID
            if(!$(this).attr('id')) {
                return;
            }
            $(this).val(rowData[$(this).attr('id')]).trigger('change');
        });

        // Augmenting xml content on the data
        if(g1128Compliant) {
            $("#g1128SideBar").removeClass('d-none');
            $("#g1128SideBar").find("#xml-input").val(rowData["instanceAsXml"]);
        } else {
            $("#g1128SideBar").addClass('d-none');
        }
    }
}

/**
 * The instances edit dialog form should be clear every time before it is used
 * so that new entries are not polluted by old data.
 *
 * @param {Component}   $modalDiv       The modal component performing the operation
 */
function clearInstanceEditPanel($modalDiv) {
    // Reset to G1128 Compliant
    $modalDiv.find('#g1128CompliantButton').prop('checked',  true);

    // Do the form
    $modalDiv.find('form[name="instanceEditPanelForm"]').trigger("reset");

    // Don't forget the XML content
    $modalDiv.find('#xml-input').val(null);
}

function startGlobalSearchCountdown() {
    stopGlobalSearchCountdown();
    countdownRemainingMs = GLOBAL_COUNTDOWN_TOTAL_MS;
    updateGlobalSearchCountdownLabel();
    countdownIntervalId = setInterval(() => {
        countdownRemainingMs = Math.max(0, countdownRemainingMs - 1000);
        updateGlobalSearchCountdownLabel();
        if (countdownRemainingMs === 0) {
            stopGlobalSearchCountdown();
        }
    }, 1000);
}

function stopGlobalSearchCountdown() {
    if (countdownIntervalId) {
        clearInterval(countdownIntervalId);
        countdownIntervalId = null;
    }
}

function updateGlobalSearchCountdownLabel() {
    const secs = (countdownRemainingMs / 1000).toFixed(0);
    $("#globalSearchCountdown").text(`(${secs}s)`);
}

function markGlobalSearchComplete() {
    stopGlobalSearchCountdown();
    $("#globalSearchText").text("Global search complete");
    $("#globalSearchCountdown").text(""); // clear countdown
    $("#globalSearchSpinner").hide();
}


function showGlobalSearchLoading() {
    $("#globalSearchText").text("Fetching global search…");
    $("#globalSearchCountdown").text("(10)");
    $("#globalSearchSpinner").show();           // <- make spinner visible
    $("#globalSearchStatus").removeClass("d-none");
    startGlobalSearchCountdown();
}


function hideGlobalSearchLoading() {
    stopGlobalSearchCountdown();
    $("#globalSearchStatus").addClass("d-none");
}

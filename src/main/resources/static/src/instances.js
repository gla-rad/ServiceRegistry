/**
 * The API Call Libraries
 */
var fileUtils;
var instancesApi;
var xmlsApi;
var docsApi;
var ledgerRequestsApi;

/**
 * Global variables
 */
var instancesTable = undefined;
var instanceMap = undefined;
var newInstance = true;

/**
 * The Loads Table Column Definitions
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
    title: "Service Type",
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
}, {
    data: "organizationId",
    title: "Organization ID",
    readonly : true,
    hoverMsg: "MRN of service provider",
    placeholder: "MRN of the service provider (organization)"
}, {
    data: "keywords",
    title: "Keywords",
    readonly : true,
    hoverMsg: "Keywords of service",
    placeholder: "Keywords of the service"
}, {
    data: "instanceId",
    title: "Instance ID",
    readonly : true,
    hoverMsg: "MRN of service instance description",
    placeholder: "MRN of the service instance description"
}, {
    data: "lastUpdatedAt",
    title: "Last Update",
    hoverMsg: "Recent updated date",
    readonly : true,
    searchable: false,
    disabled: true
}, {
    data: "publishedAt",
    type: "hidden",
    readonly : true,
    visible: false,
    searchable: false
}, {
    data: "comment",
    type: "hidden",
    readonly : true,
    visible: false,
    searchable: false
}, {
    data: "geometry",
    type: "hidden",
    readonly : true,
    visible: false,
    searchable: false
}, {
    data: "unlocode",
    type: "hidden",
    readonly : true,
    visible: false,
    searchable: false
}, {
    data: "mmsi",
    type: "hidden",
    readonly : true,
    visible: false,
    searchable: false
}, {
    data: "imo",
    type: "hidden",
    readonly : true,
    visible: false,
    searchable: false
}, {
    data: "endpointType",
    type: "hidden",
    readonly : true,
    visible: false,
    searchable: false
}, {
    data: "ledgerRequestId",
    type: "hidden",
    readonly : true,
    visible: false,
    searchable: false
}, {
    data: "ledgerRequestStatus",
    type: "hidden",
    readonly : true,
    visible: false,
    searchable: false
 }, {
    data: "instanceAsDocId",
    title: "Instance Doc",
    type: "file",
    readonly : true,
    visible: true,
    searchable: false,
    className: 'dt-body-center',
    render: function ( data, type, row ) {
        return (data ? `<i class="fas fa-file-alt" style="color:green" onclick="openInstanceAsDoc(${data})"></i>` : `<i class="fas fa-times-circle" style="color:red"></i>`);
    },
 }];

$(() => {
    // First link the API libs
    this.fileUtils = new FileUtils();
    this.instancesApi = new InstancesApi();
    this.xmlsApi = new XmlsApi();
    this.docsApi = new DocsApi();
    this.ledgerRequestsApi = new LedgerRequestsApi();

    // Now initialise the instances table
    instancesTable = $('#instancesTable').DataTable({
        serverSide: true,
        ajax: {
            "type": "POST",
            "url": "/api/instances/dt",
            "contentType": "application/json",
            "data": function (d) {
                return JSON.stringify(d);
            },
            error: function (jqXHR, ajaxOptions, thrownError) {
                console.error(thrownError);
            }
        },
        columns: columnDefs,
        dom: "<'row'<'col-lg-2 col-md-4'B><'col-lg-2 col-md-4'l><'col-lg-8 col-md-4'f>><'row'<'col-md-12't>><'row'<'col-md-6'i><'col-md-6'p>>",
        select: 'single',
        lengthMenu: [10, 25, 50, 75, 100],
        responsive: true,
        altEditor: true, // Enable altEditor
        buttons: [{
            text: '<i class="fas fa-plus-circle"></i>',
            titleAttr: 'Add Instance',
            className: 'instance-edit-panel-toggle',
            name: 'add-instance', // do not change name
            action: (e, dt, node, config) => {
                clearInstanceEditPanel();
            }
        }, {
            extend: 'selected', // Bind to Selected row
            text: '<i class="fas fa-edit"></i>',
            titleAttr: 'Edit Instance',
            className: 'instance-edit-panel-toggle',
            name: 'edit-instance', // do not change name
            action: (e, dt, node, config) => {
                loadInstanceEditPanel();
            }
        }, {
            extend: 'selected', // Bind to Selected row
            text: '<i class="fas fa-trash-alt"></i>',
            titleAttr: 'Delete Instance',
            name: 'delete' // do not change name
        }, {
            extend: 'selected', // Bind to Selected row
            text: '<i class="fas fa-map-marked-alt"></i>',
            titleAttr: 'Instance Coverage',
            name: 'instance-coverage', // do not change name
            className: 'instance-coverage-toggle',
            action: (e, dt, node, config) => {
                loadInstanceCoverage(e, dt, node, config);
            }
        }, {
            extend: 'selected', // Bind to Selected row
            text: '<i class="fas fa-clipboard-check"></i>',
            titleAttr: 'Instance Status',
            name: 'instance-status', // do not change name
            className: 'instance-status-toggle',
            action: (e, dt, node, config) => {
                loadInstanceStatus(e, dt, node, config);
            }
        }, {
            extend: 'selected', // Bind to Selected row
            text: '<i class="fas fa-cloud-upload-alt"></i>',
            titleAttr: 'Instance Global Ledger Status',
            name: 'instance-ledger-status', // do not change name
            className: 'instance-ledger-toggle',
            action: (e, dt, node, config) => {
                loadInstanceLedgerStatus(e, dt, node, config);
            }
        }],
        onAddRow: (datatable, rowdata, success, error) => {
            this.instancesApi.createInstance(JSON.stringify(rowdata), success, error);
        },
        onDeleteRow: (datatable, rowdata, success, error) => {
            this.instancesApi.deleteInstance(rowdata["id"], success, error);
        },
        onEditRow: (datatable, rowdata, success, error) => {
            this.instancesApi.updateInstance(rowdata["id"], JSON.stringify(rowdata), success, error);
        },
        initComplete: (settings, json) => {
            hideLoader();
        }
    });

    // Show the loader on processing
    instancesTable.on( 'processing.dt', function(e, settings, processing) {
        processing ? showLoader(false) : hideLoader();;
    });

    // We also need to link the instance create/edit toggle buttons with the the
    // modal panel so that by clicking the button the panel pops up. It's easier
    // done with jQuery.
    instancesTable.buttons('.instance-edit-panel-toggle')
        .nodes()
        .attr({ "data-bs-toggle": "modal", "data-bs-target": "#instanceEditPanel" });

    // On confirmation of the XML validation, we need to make an AJAX
    // call back to the service to perform the G-1128 compliance validation.
    $('#instanceEditPanel').on('click', '.btn-validate-xml', (e) => {
        var $modalDiv = $(e.delegateTarget);
        $modalDiv.addClass('loading');
        onValidateXml($modalDiv);
    });

    // On confirmation of the instance saving, we need to make an AJAX
    // call back to the service to save the entry.
    $('#instanceEditPanel').on('click', '.btn-ok', (e) => {
        var $modalDiv = $(e.delegateTarget);
        var columnDefData = columnDefs.map((e) => e["data"]);
        var rowData = initialiseData();

        // If an existing row has been selected, copy the data for an update
        if(!newInstance && instancesTable.row({selected : true}).length != 0) {
            rowData = {...instancesTable.row({selected : true}).data()};
        }

        // Getting the inputs from the modal
        $('form[name="instanceEditPanelForm"] :input').each(function() {
            rowData = alignData(rowData, $(this).attr('id'), $(this).val(), columnDefData);
        });

        // Augmenting xml content on the data
        var xmlContent = $modalDiv.find("#xml-input").val();
        if (xmlContent && xmlContent.length>0) {
            rowData["instanceAsXml"]["content"] = xmlContent;
        }

        // Adding the attached documents
        var uploadFiles = $modalDiv.find("#instanceAsDoc").prop('files');

        // If we have any documents attached, first read them and then save
        if(uploadFiles && uploadFiles.length == 1) {
            showLoader();
            // Initialise the File Reader
            fileUtils.encodeFileToBase64(uploadFiles[0], (base64Data) => {
                rowData["instanceAsDoc"] = {};
                rowData["instanceAsDoc"]["id"] = null;
                rowData["instanceAsDoc"]["name"] = uploadFiles[0].name;
                rowData["instanceAsDoc"]["mimetype"] = uploadFiles[0].type;
                rowData["instanceAsDoc"]["filecontent"] = base64Data;
                rowData["instanceAsDoc"]["filecontentContentType"] = uploadFiles[0].type;
                saveInstanceThroughDatatables(rowData);
            });
        }
        // Otherwise save directly using the datatables
        else {
            // Make sure we don't delete any existing instance as doc files
            // To dot his, we need to translate between the InstanceDtDto and
            // the InstanceDto objects.
            if($modalDiv.find("#instanceAsDocWithValue").is(":visible")) {
                rowData["instanceAsDoc"] = {};
                rowData["instanceAsDoc"]["id"] = rowData["instanceAsDocId"];
            }
            saveInstanceThroughDatatables(rowData);
        }
    });

    // Link the clear instance doc button functionality
    $('#instanceEditPanel').on('click', '.btn-clear-instance-doc', (e) => {
        var $modalDiv = $(e.delegateTarget);
        clearInstanceDoc($modalDiv);
    });

    // Link the download instance doc button functionality
    $('#instanceEditPanel').on('click', '.btn-download-instance-doc', (e) => {
        downloadInstanceDoc(instancesTable.row({selected : true}).data()["instanceAsDocId"]);
    });

    // We also need to link the instance status toggle button with the the
    // modal panel so that by clicking the button the panel pops up. It's easier
    // done with jQuery.
    instancesTable.buttons('.instance-status-toggle')
        .nodes()
        .attr({ "data-bs-toggle": "modal", "data-bs-target": "#instanceStatusPanel" });

    // On confirmation of the instance saving, we need to make an AJAX
    // call back to the service to save the entry.
    $('#instanceStatusPanel').on('click', '.btn-ok', (e) => {
        var $modalDiv = $(e.delegateTarget);
        $modalDiv.addClass('loading');
        onStatusUpdate($modalDiv,
            instancesTable.row({selected : true}).data()["id"],
            $("#instanceStatusPanel").find("#instanceStatusSelect").val());
    });

    // We also need to link the instance ledger toggle button with the the
    // modal panel so that by clicking the button the panel pops up. It's easier
    // done with jQuery.
    instancesTable.buttons('.instance-ledger-toggle')
        .nodes()
        .attr({ "data-bs-toggle": "modal", "data-bs-target": "#instanceLedgerPanel" });

    // On confirmation of the instance saving, we need to make an AJAX
    // call back to the service to save the entry.
    $('#instanceLedgerPanel').on('click', '.btn-ok', (e) => {
        var $modalDiv = $(e.delegateTarget);
        $modalDiv.addClass('loading');
        onLedgerRequestUpdate($modalDiv,
            instancesTable.row({selected : true}).data()["id"],
            $("#instanceLedgerPanel").find("#instanceLedgerStatusSelect").val());
    });

    // We also need to link the instance coverage toggle button with the the modal
    // panel so that by clicking the button the panel pops up. It's easier done with
    // jQuery.
    instancesTable.buttons('.instance-coverage-toggle')
        .nodes()
        .attr({ "data-bs-toggle": "modal", "data-bs-target": "#instanceCoveragePanel" });

    // Now also initialise the instance map before we need it
    instanceMap = L.map('instanceMap').setView([54.910, -3.432], 5);
    L.tileLayer('http://{s}.tile.osm.org/{z}/{x}/{y}.png', {
        attribution: '&copy; <a href="http://osm.org/copyright">OpenStreetMap</a> contributors'
    }).addTo(instanceMap);

    // FeatureGroup is to store editable layers
    drawnItems = new L.FeatureGroup();
    instanceMap.addLayer(drawnItems);

    // Initialise the draw toolbar
    drawControl = new L.Control.Draw({
        draw: {
            marker: false,
            polyline: false,
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

    // Handle the leaflet draw create events
    instanceMap.on('draw:created', function (e) {
        var type = e.layerType;
        var layer = e.layer;

        // Do whatever else you need to. (save to db, add to map etc)
        drawnItems.addLayer(layer);
    });

    // Invalidate the map size on show to fix the presentation
    $('#instanceCoveragePanel').on('shown.bs.modal', function() {
        setTimeout(function() {
            instanceMap.invalidateSize();
        }, 10);
    });
});

/**
 * Performs the instance saving operation through the instances datatables
 * structure so that it table will also be updated on success.
 */
function saveInstanceThroughDatatables(instance) {
    // And call back to the datatables to handle the create/update
    if(newInstance) {
        instancesTable.context[0].oInit.onAddRow(instancesTable,
                instance,
                (data) => { instancesTable.row.add(data).draw(false); },
                (data) => { showError(data.getResponseHeader('X-mcsrApp-error')); hideLoader(); });
    } else {
        instancesTable.context[0].oInit.onEditRow(instancesTable,
                instance,
                (data,b,c,d,e) => { instancesTable.ajax.reload(); },
                (data) => { showError(data.getResponseHeader('X-mcsrApp-error')); hideLoader(); });
    }
}

/**
 * Using an AJAX call we ask the server to validate the provided XML and if
 * proven correct we can use the returned JSON object to populate the instance
 * editor field values. Note that some of the G-1128 field do not correspond
 * to the names of the fields used here so we need to translate them.
 *
 * @param {Component}   $modalDiv   The modal component performing the validation
 */
function onValidateXml($modalDiv) {
    this.xmlsApi.validateInstanceXml($modalDiv.find("#xml-input").val(), (response, status, more) => {
        for (var field in response) {
            $modalDiv.find("input#"+field).val(response[field]);
        }
        $modalDiv.removeClass('loading');
    }, (response, status, more) => {
        $modalDiv.removeClass('loading');
        showError(getErrorFromHeader(response, "Error while trying to validate whether the XML is G-1128 compliant!"));
    });
}

/**
 * Using an AJAX call we ask the server to update the instance status and if
 * proven successful, we can request the instances datatables to reload.
 *
 * @param {Component}   $modalDiv   The modal component performing the update
 * @param {number}      id          The ID of the instance to be updated
 * @param {String}      status      The new status value
 */
function onStatusUpdate($modalDiv, id, status) {
    this.instancesApi.setStatus(id, status, () => {
        $modalDiv.removeClass('loading');
        instancesTable.draw('page');
    }, (response, status, more) => {
        $modalDiv.removeClass('loading');
        showError(getErrorFromHeader(response, "Error while trying to update the instance status!"));
    });
}

/**
 * Using an AJAX call we ask the server to update the instance ledger request
 * status and if proven successful, we can request the instances datatables to
 * reload.
 *
 * @param {Component}   $modalDiv   The modal component performing the update
 * @param {number}      id          The ID of the ledger request to be updated
 * @param {String}      status      The new status value
 */
function onLedgerRequestUpdate($modalDiv, id, status) {
    this.instancesApi.setLedgerStatus(id, status, () => {
        $modalDiv.removeClass('loading');
        instancesTable.draw('page');
    }, (response, status, more) => {
        $modalDiv.removeClass('loading');
        showError(getErrorFromHeader(response, "Error while trying to update the instance global ledger status!"));
    });
}

/**
 * The instances edit dialog form should be clear every time before it is used
 * so that new entries are not polluted by old data.
 */
function clearInstanceEditPanel() {
    // Do the form
    $('form[name="instanceEditPanelForm"]').trigger("reset");

    // Don't forget the XML content
    $("#instanceEditPanel").find("#xml-input").val(null);

    // Mark the a new instance can be created through the edit dialog
    newInstance = true;
}

/**
 * This helper function loads the XML and field data from the selected instance
 * in the instance table onto the edit dialog.
 */
function loadInstanceEditPanel() {
    // First always clear to be sure
    clearInstanceEditPanel();

    // Get the instance edit panel modal dialog
    let $modalDiv = $("#instanceEditPanel");

    // If a row has been selected load the data into the form
    if(instancesTable.row({selected : true})) {
        // Populate the form
        rowData = instancesTable.row({selected : true}).data();
        $('form[name="instanceEditPanelForm"] :input').each(function() {
             $(this).val(rowData[$(this).attr('id')]);
        });

        // Augmenting xml content on the data
        $("#instanceEditPanel").find("#xml-input").val(rowData["instanceAsXml"]["content"]);

        // Handle the instance doc field if populated or not
        rowData.instanceAsDocId ? showInstanceDoc($modalDiv) : clearInstanceDoc($modalDiv);
    } else {
        clearInstanceDoc($modalDiv);
    }

    // Make that an existing instance has been loaded
    newInstance = false;
}

/**
 * This function will load the station geometry onto the drawnItems variable
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

    // Refresh the stations map control - For now leave disabled
    //instanceMap.removeControl(drawControl);
    //instanceMap.addControl(drawControl);

    // Recreate the drawn items feature group
    drawnItems.clearLayers();
    if(geometry) {
        var geomLayer = L.geoJson(geometry);
        addNonGroupLayers(geomLayer, drawnItems);
        instanceMap.setView(geomLayer.getBounds().getCenter(), 5);
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
 * This function will load the instance status onto the instance status select
 * input of the DOM.
 *
 * @param {Event}         event         The event that took place
 * @param {DataTable}     table         The AtoN type table
 * @param {Node}          button        The button node that was pressed
 * @param {Configuration} config        The table configuration
 */
function loadInstanceStatus(event, table, button, config) {
    // If a row has been selected load the data into the form
    if(instancesTable.row({selected : true})) {
        rowData = instancesTable.row({selected : true}).data();
        $("#instanceStatusPanel").find("#instanceStatusSelect").val(rowData["status"]);
    }
}

/**
 * This function will load the instance ledger status onto the instance ledger
 * status select input of the DOM. Always read the ledger status value from
 * the server to pick up successes and failures.
 *
 * @param {Event}         event         The event that took place
 * @param {DataTable}     table         The AtoN type table
 * @param {Node}          button        The button node that was pressed
 * @param {Configuration} config        The table configuration
 */
function loadInstanceLedgerStatus(event, table, button, config) {
    // If a row has been selected load the data into the form
    if(instancesTable.row({selected : true})) {
        rowdata = instancesTable.row({selected : true}).data();
        id = rowdata["id"];
        ledgerRequestId = rowdata["ledgerRequestId"];
        ledgerRequestStatus = rowdata["ledgerRequestStatus"];
        // First always start with the last known value
        $("#instanceLedgerPanel").find("#instanceLedgerStatusSelect").val(ledgerRequestStatus);
        // And then query the server for an update
        this.ledgerRequestsApi.getLedgerRequest(ledgerRequestId, (response) => {
            $("#instanceLedgerPanel").find("#instanceLedgerStatusSelect").val(response["status"]);
        });
    }
}

/**
 * This helper function returns a band new blank instance object to be used
 * for generating new entries.
 */
function initialiseData() {
    // Create the new object
    var newRowData = {}

    // Initialise some of the row data fields with empty values
    newRowData["comment"] = "";
    newRowData["instanceAsDoc"] = null;
    newRowData["geometryContentType"] = null;
    newRowData["designs"] = {};
    newRowData["specifications"] = {};
    newRowData["geometryJson"] = {};
    newRowData["instanceAsDoc"] = null;
    newRowData["instanceAsXml"] = {
        name: "xml",
        comment: "no comment", content: "",
        contentContentType: "G1128 Instance Specification XML"
    };

    // Return the object
    return newRowData;
}

/**
 * This helper function corrects the type of data being read from the instance
 * dialog form since most of it comes back as a string.
 *
 * @param {Array}       rowData         The row data
 * @param {String}      field           The field to be parsed
 * @param {String}      value           The value of the field
 * @param {Array}       columnDefs      The definitions of the column data
 */
function alignData(rowData, field, value, columnDefs){
    if (field && columnDefs.includes(field)){
        if (field === 'id'){
            rowData[field] = parseInt(value);
        }
        else if(field.toUpperCase().endsWith("JSON")) {
            rowData[field] = JSON.stringify(value);
        } else{
            rowData[field] = value;
        }
    }
    return rowData;
}

/**
 * Displays the instance doc file selection field in the instance edit dialogs
 * and hides the doc name field.
 *
 * @param {Component}   $modalDiv   The modal component performing the operation
 */
function showInstanceDoc($modalDiv) {
    $modalDiv.find("#instanceAsDoc").hide();
    $modalDiv.find("#instanceAsDocWithValue").show();
}

/**
 * Clear the instance doc name field from the instance edit dialogs and
 * shows back the file selection field.
 *
 * @param {Component}   $modalDiv   The modal component performing the operation
 */
function clearInstanceDoc($modalDiv) {
    $modalDiv.find("#instanceAsDocWithValue").hide();
    $modalDiv.find("#instanceAsDocWithValue").find("#instanceAsDocName").val("");
    $modalDiv.find("#instanceAsDoc").show();
}

/**
 * Download the selected document ID from the server, decode the data from
 * the provided Base64 format and open's it in the browser.
 *
 * @param {number}      docId           The ID of the document to be opened
 */
function openInstanceAsDoc(docId) {
    showLoader();
    this.docsApi.getDoc(docId, (doc) => {
        fileUtils.openFileWindow(doc.filecontentContentType, doc.filecontent);
        hideLoader();
    }, (response, status, more) => {
         hideLoader();
         showError(getErrorFromHeader(response, "Error while trying to retrieve the instance doc!"));
    })
}

/**
 * Download the selected document ID from the server, decode the data from
 * the provided Base64 format.
 *
 * @param {number}      docId           The ID of the document to be opened
 */
function downloadInstanceDoc(docId) {
    showLoader();
    this.docsApi.getDoc(docId, (doc) => {
        fileUtils.downloadFile(doc.name, doc.filecontentContentType, doc.filecontent);
        hideLoader();
    }, (response, status, more) => {
        hideLoader();
        showError(getErrorFromHeader(response, "Error while trying to retrieve the instance doc!"));
    });
}
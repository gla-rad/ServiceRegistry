/**
 * Global variables
 */
var instancesTable = undefined;
var newInstance = true;

/**
 * Global map variables
 */
var instanceEditCoverageMap = undefined;
var drawnEditMapItems = undefined;
var firstInstanceMapView = true;

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
}, {
    data: "version",
    title: "Version",
}, {
    data: "serviceType",
    title: "Type",
}, {
    data: "dataProductType",
    title: "Product Type",
    visible: false
}, {
    data: "status",
    title: "Status",
}, {
    data: "endpointUri",
    title: "Endpoint URI",
}, {
    data: "organizationId",
    title: "Organization",
}, {
    data: "keywords",
    title: "Keywords",
}, {
    data: "instanceId",
    title: "Instance ID",
}, {
    data: "lastUpdatedAt",
    title: "Last Update",
    type: "hidden",
    visible: false,
    searchable: false,
}, {
    data: "publishedAt",
    title: "Published",
    type: "hidden",
    visible: false,
    searchable: false
}, {
    data: "comment",
    title: "Comment",
    type: "hidden",
    visible: false,
    searchable: false
}, {
    data: "geometry",
    title: "Geometry",
    type: "hidden",
    visible: false,
    searchable: false
}, {
    data: "unlocode",
    title: "UnLoCode",
    type: "hidden",
    visible: false,
    searchable: false
}, {
    data: "mmsi",
    title: "MMSI",
    type: "hidden",
    visible: false,
    searchable: false
}, {
    data: "imo",
    title: "IMO",
    type: "hidden",
    visible: false,
    searchable: false
}, {
    data: "endpointType",
    title: "Endpoint Type",
    type: "hidden",
    visible: false,
    searchable: false
}, {
    data: "ledgerRequestId",
    title: "Ledger Request ID",
    type: "hidden",
    visible: false,
    searchable: false
}, {
    data: "ledgerRequestStatus",
    title: "Ledger Request Status",
    type: "hidden",
    visible: false,
    searchable: false
 }, {
    data: "instanceAsDocId",
    title: "Doc",
    type: "file",
    visible: true,
    searchable: false,
    className: 'dt-body-center',
    render: function ( data, type, row ) {
        return (data ?
            `<i class="fa-solid fa-file" style="color:green" onclick="downloadDoc(${data})"></i>`:
            `<i class="fa-solid xmark" style="color:red"></i>`);
    },
 }, {
    data: "implementsServiceDesign",
    title: "Implements Service Design",
    type: "hidden",
    visible: false,
    searchable: false,
}, {
    data: "implementsServiceDesignVersion",
    title: "Implements Service Design Versin",
    type: "hidden",
    visible: false,
    searchable: false,
}];

/**
 * Standard jQuery initialisation of the page.
 */
$(() => {
    // Now initialise the instances table
    instancesTable = $('#instancesTable').DataTable({
        serverSide: true,
        ajax: {
            type: "POST",
            url: "api/instances/dt",
            contentType: "application/json",
            crossDomain: true,
            data: function (d) {
                return JSON.stringify(d);
            },
            error: function (jqXHR, ajaxOptions, thrownError) {
                hideLoader();
                console.error(thrownError);
            }
        },
        columns: columnDefs,
        dom: "<'row'<'col-md-auto'B><'col-sm-4 pb-1'l><'col-md col-sm-4'f>><'row'<'col-md-12't>><'row'<'col-md-6'i><'col-md-6'p>>",
        select: 'single',
        lengthMenu: [10, 25, 50, 75, 100],
        responsive: true,
        altEditor: true, // Enable altEditor
        buttons: [{
            text: '<i class="fa-solid fa-circle-plus"></i>',
            titleAttr: 'Add Instance',
            className: 'instance-edit-panel-toggle',
            name: 'add-instance', // do not change name
            action: (e, dt, node, config) => {
                loadInstanceEditPanel($('#instanceEditPanel'), true);
            }
        }, {
            extend: 'selected', // Bind to Selected row
            text: '<i class="fa-solid fa-pen-to-square"></i>',
            titleAttr: 'Edit Instance',
            className: 'instance-edit-panel-toggle',
            name: 'edit-instance', // do not change name
            action: (e, dt, node, config) => {
                loadInstanceEditPanel($('#instanceEditPanel'), false);
            }
        }, {
            extend: 'selected', // Bind to Selected row
            text: '<i class="fa-solid fa-trash-can"></i>',
            titleAttr: 'Delete Instance',
            name: 'delete' // do not change name
        }, {
            extend: 'selected', // Bind to Selected row
            text: '<i class="fa-solid fa-paperclip"></i>',
            titleAttr: 'Files Attachments',
            name: 'attachments', // do not change name
            className: 'instance-attachments-toggle',
            action: (e, dt, node, config) => {
                var idx = dt.cell('.selected', 0).index();
                var data = dt.row(idx.row).data();
                loadFileUploader(
                    data["id"],
                    "api/docs",
                    () => { }
                );
            }
       }, {
            extend: 'selected', // Bind to Selected row
            text: '<i class="fa-solid fa-clipboard-check"></i>',
            titleAttr: 'Instance Status',
            name: 'instance-status', // do not change name
            className: 'instance-status-toggle',
            action: (e, dt, node, config) => {
                loadInstanceStatus(e, dt, node, config);
            }
        }, {
            extend: 'selected', // Bind to Selected row
            text: '<i class="fa-solid fa-cloud-arrow-up"></i>',
            titleAttr: 'Instance Global Ledger Status',
            name: 'instance-ledger-status', // do not change name
            className: 'instance-ledger-toggle',
            action: (e, dt, node, config) => {
                loadInstanceLedgerStatus(e, dt, node, config);
            }
        }],
        onAddRow: (datatable, rowdata, success, error) => {
            api.instancesApi.createInstance(JSON.stringify(rowdata), success, error);
        },
        onEditRow: (datatable, rowdata, success, error) => {
            api.instancesApi.updateInstance(rowdata["id"], JSON.stringify(rowdata), success, error);
        },
        onDeleteRow: (datatable, selectedRows, success, error) => {
            selectedRows.every(function (rowIdx, tableLoop, rowLoop) {
                api.instancesApi.deleteInstance(this.data()["id"], success, error);
            });
        },
        initComplete: (settings, json) => {
            hideLoader();
        }
    });

    // Show the loader on processing
    instancesTable.on( 'processing.dt', function(e, settings, processing) {
        processing ? showLoader(false) : hideLoader();
    });

    // We also need to link the instance create/edit toggle buttons with the the
    // modal panel so that by clicking the button the panel pops up. It's easier
    // done with jQuery.
    instancesTable.buttons('.instance-edit-panel-toggle')
        .nodes()
        .attr({ "data-bs-toggle": "modal", "data-bs-target": "#instanceEditPanel" });

    // We also need to link the attachment toggle button with the the modal
    // dialog so that by clicking the button the panel pops up. It's easier done
    // with jQuery.
    instancesTable.buttons('.instance-attachments-toggle')
        .nodes()
        .attr({ "data-bs-toggle": "modal", "data-bs-target": "#attachmentsPanel" });

    // We also need to link the instance status toggle button with the the
    // modal panel so that by clicking the button the panel pops up. It's easier
    // done with jQuery.
    instancesTable.buttons('.instance-status-toggle')
        .nodes()
        .attr({ "data-bs-toggle": "modal", "data-bs-target": "#instanceStatusPanel" });

    // We also need to link the instance ledger toggle button with the the
    // modal panel so that by clicking the button the panel pops up. It's easier
    // done with jQuery.
    instancesTable.buttons('.instance-ledger-toggle')
        .nodes()
        .attr({ "data-bs-toggle": "modal", "data-bs-target": "#instanceLedgerPanel" });

    // On confirmation of the instance saving, we need to make an AJAX
    // call back to the service to save the entry.
    $('#instanceEditPanel').on('click', '.btn-ok', (e) => {
        var $modalDiv = $(e.delegateTarget);
        $modalDiv.addClass('loading');
        saveInstanceEditPanel($modalDiv, newInstance);
    });

    // On confirmation of the XML validation, we need to make an AJAX
    // call back to the service to perform the G-1128 compliance validation.
    $('#instanceEditPanel').on('click', '.btn-validate-xml', (e) => {
        var $modalDiv = $(e.delegateTarget);
        $modalDiv.addClass('loading');
        validateXml($modalDiv);
    });

    // Link the clear instance doc button functionality
    $('#instanceEditPanel').on('click', '.btn-clear-instance-doc', (e) => {
        var $modalDiv = $(e.delegateTarget);
        clearInstanceDoc($modalDiv);
    });

    // Link the download instance doc button functionality
    $('#instanceEditPanel').on('click', '.btn-download-instance-doc', (e) => {
        var $modalDiv = $(e.delegateTarget);
        var selectedRow = instancesTable.row({selected : true}).data();
        if(selectedRow) {
            downloadDoc(instancesTable.row({selected : true}).data()["instanceAsDocId"]);
        }
    });

    // On confirmation of the instance saving, we need to make an AJAX
    // call back to the service to save the entry.
    $('#instanceStatusPanel').on('click', '.btn-ok', (e) => {
        var $modalDiv = $(e.delegateTarget);
        $modalDiv.addClass('loading');
        onStatusUpdate($modalDiv,
            instancesTable.row({selected : true}).data()["id"],
            $("#instanceStatusPanel").find("#instanceStatusSelect").val());
    });

    // On confirmation of the instance saving, we need to make an AJAX
    // call back to the service to save the entry.
    $('#instanceLedgerPanel').on('click', '.btn-ok', (e) => {
        var $modalDiv = $(e.delegateTarget);
        $modalDiv.addClass('loading');
        onLedgerRequestUpdate($modalDiv,
            instancesTable.row({selected : true}).data()["id"],
            $("#instanceLedgerPanel").find("#instanceLedgerStatusSelect").val());
    });

    // Also initialise the instance map before we need it
    drawnEditMapItems = new L.FeatureGroup();
    instanceEditCoverageMap = initMapWithDrawnItems('instanceEditCoverageMap', drawnEditMapItems, true);

    // Invalidate the instance edit coverage map size on tab show to fix the presentation
    $('button[data-bs-toggle="tab"]').on('shown.bs.tab', function() {
        // Make sure this is the coverage tab
        if($(this).attr('id') != "coverage-tab") {
            return;
        }
        // And if so fix the map presentation
        setTimeout(function() {
            instanceEditCoverageMap.invalidateSize();
            // Only load the geometry the first time and only on existing instances
            if(firstInstanceMapView && !newInstance) {
                // Make sure we got a table row selected
                if(instancesTable.row({selected : true})) {
                    var geometry = instancesTable.row({selected : true}).data().geometry;
                    loadGeometryOnMap(geometry, instanceEditCoverageMap, drawnEditMapItems);
                }
            }
            firstInstanceMapView = false;
        }, 50);
    });

    // Also initialise the data product type multi-select
    $('#dataProductType').select2({
        placeholder: "Data Product Type",
        theme: "bootstrap-5",
        selectionCssClass: 'select2--small',
        dropdownCssClass: 'select2--small'
    });
});

/**
 * Update the instance edit panel view based on the selection of the G1128
 * compliant button.
 */
function toggleG1128Compliant() {
    var g1128Compliant = $("#g1128CompliantButton").prop('checked');
    var selector = $("#g1128CompliantButton").data("target");

    // Enable/Disable the form
    g1128Compliant ? $(selector).removeClass('d-none') : $(selector).addClass('d-none');
    $('form[name="instanceEditPanelForm"] :input').filter('[data-g1128="true"]').attr('readonly', g1128Compliant);
    $('form[name="instanceEditPanelForm"] select').filter('[data-g1128="true"]').attr('disabled', g1128Compliant);

    // Control the drawing
    g1128Compliant ? drawControlFull.remove(instanceEditCoverageMap) : drawControlFull.addTo(instanceEditCoverageMap);

    // Refresh the map size due to the change
    setTimeout(function() {
        instanceEditCoverageMap.invalidateSize();
    }, 50);
}

/**
 * Using an AJAX call we ask the server to validate the provided XML and if
 * proven correct we can use the returned JSON object to populate the instance
 * editor field values. Note that some of the G-1128 field do not correspond
 * to the names of the fields used here so we need to translate them.
 *
 * @param {Component}   $modalDiv   The modal component performing the validation
 */
function validateXml($modalDiv) {
    api.xmlsApi.validateInstanceXml($modalDiv.find("#xml-input").val(), (response, status, more) => {
        // Update the instance fields
        for (var field in response) {
            $modalDiv.find("input#"+field).val(response[field]);
        }
        // Update the instance coverage area
        if(response["coversAreas"] && response["coversAreas"]["coversAreasAndUnLoCodes"]) {
            drawnEditMapItems.clearLayers();
            response["coversAreas"]["coversAreasAndUnLoCodes"].forEach((area) => {
                var parsedGeoJson = undefined;
                try {
                    parsedGeoJson = Terraformer.WKT.parse(area.geometryAsWKT);
                } catch(ex) {
                    // Nothing to do
                }

                // If a valid GeoJSON object was parsed, replace it in the map
                if(parsedGeoJson) {
                    addNonGroupLayers(L.geoJson(parsedGeoJson), drawnEditMapItems);
                }
            });
        }
        $modalDiv.removeClass('loading');
    }, (response, status, more) => {
        $modalDiv.removeClass('loading');
        showError(getErrorFromHeader(response, "Error while trying to validate whether the XML is G-1128 compliant!"));
    });
}

/**
 * The instances edit dialog form should be clear every time before it is used
 * so that new entries are not polluted by old data.
 */
function clearInstanceEditPanel() {
    // Always init in the info tab
    $('#instanceTabs button:first').tab('show');

    // Reset to G1128 Compliant
    $('#g1128CompliantButton').prop('checked',  true);
    toggleG1128Compliant();

    // Do the form
    $('form[name="instanceEditPanelForm"]').trigger("reset");
    $("#dataProductType").select2('val', null);

    // And the map
    drawnEditMapItems.clearLayers();
    resetMapView(instanceEditCoverageMap);
    firstInstanceMapView = true;

    // Don't forget the XML content
    $('#instanceEditPanel').find('#xml-input').val(null);

    // And the document option
    clearInstanceDoc($('#instanceEditPanel'));

    // Mark the a new instance can be created through the edit dialog
    newInstance = true;
}

/**
 * This helper function loads the XML and field data from the selected instance
 * in the instance table onto the edit dialog.
 *
 * @param {Component}   $modalDiv       The modal component performing the operation
 * @param {Boolean}     isNewInstance   Whether this is a new instance or not
 */
function loadInstanceEditPanel($modalDiv, isNewInstance) {
    // First always clear to be sure
    clearInstanceEditPanel();

    // Note if a new or an existing instance is to be loaded
    newInstance = isNewInstance;

    // If a row has been selected load the data into the form
    if(!isNewInstance && instancesTable.row({selected : true})) {
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
            $(this).val(rowData[$(this).attr('id')]);
            $(this).filter('[data-g1128="true"]').attr('readonly', g1128Compliant);
        });
        $('form[name="instanceEditPanelForm"] select').each(function() {
            // Make sure the select element has an ID
            if(!$(this).attr('id')) {
                return;
            }
            $(this).val(rowData[$(this).attr('id')]).trigger('change');
            $(this).filter('[data-g1128="true"]').attr('disabled', g1128Compliant);
        });

        // Augmenting xml content on the data
        if(g1128Compliant) {
            $("#g1128SideBar").removeClass('d-none');
            $("#g1128SideBar").find("#xml-input").val(rowData["instanceAsXml"]["content"]);
            drawControlFull.remove(instanceEditCoverageMap);
        } else {
            $("#g1128SideBar").addClass('d-none');
            drawControlFull.addTo(instanceEditCoverageMap);
        }

        // Handle the instance doc field if populated or not
        rowData.instanceAsDocId ? showInstanceDoc($modalDiv) : clearInstanceDoc($modalDiv);
    }
}

/**
 * This function read all the provided instance information from the edit
 * instance modal dialog for both the G1128 compliant and non-compliant
 * instances and then uses the datatables functionality of the instances
 * table to save the changes and update the table itself.
 *
 * @param {Component}   $modalDiv       The modal component performing the operation
 * @param {Boolean}     isNewInstance   Whether this is a new instance or not
 */
function saveInstanceEditPanel($modalDiv, isNewInstance) {
    var g1128Compliant = $modalDiv.find("#g1128CompliantButton").prop('checked');

    // First check the form configuration
    if (!g1128Compliant && !$('#instanceEditPanelForm')[0].checkValidity()) {
        $modalDiv.removeClass('loading');
        $('#instanceTabs button:first').tab('show');
        setTimeout(() => $('#instanceEditPanelForm')[0].reportValidity(), 250);
        return;
    }

    // Load the data to be stored/updated
    var columnDefData = columnDefs.map((e) => e["data"]);
    var rowData = initialiseInstanceData(g1128Compliant);

    // If an existing row has been selected, copy the data for an update
    if(!isNewInstance && instancesTable.row({selected : true}).length != 0) {
        rowData = {...instancesTable.row({selected : true}).data()};
    }

    // Getting the inputs from the modal
    $('form[name="instanceEditPanelForm"] :input').each(function() {
        rowData = alignInstanceData(rowData, $(this).attr('id'), $(this).val(), columnDefData);
    });

    // For G1128-compliant entries, augmenting xml content on the data
    if(g1128Compliant) {
        var xmlContent = $modalDiv.find("#xml-input").val();
        if (xmlContent && xmlContent.length>0) {
            rowData["instanceAsXml"]["content"] = xmlContent;
        }
    } else if(!firstInstanceMapView){
        rowData["geometry"] = getGeometryCollectionFromMap(drawnEditMapItems);
    }

    // Adding the data product types
    rowData["dataProductType"] = $('#dataProductType').val();

    // Adding the attached documents
    var uploadFiles = $modalDiv.find("#instanceAsDoc").prop('files');

    // If we have any documents attached, first read them and then save
    if(uploadFiles && uploadFiles.length == 1) {
        showLoader();
        // Initialise the File Reader
        encodeFileToBase64(uploadFiles[0])
            .then((attachment) => {
                rowData["instanceAsDoc"] = {
                    'name': attachment.file.name,
                    'mimetype': attachment.file.type,
                    'filecontent': attachment.content,
                    'filecontentContentType': attachment.file.type,
                    'instanceId': rowData["id"]
                };
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
    // Finally remove the loading from the dialog and hide it, with a small timeout
    setTimeout(() => {
        $modalDiv.removeClass('loading');
        $modalDiv.modal("toggle");
    }, 50);
}

/**
 * Performs the instance saving operation through the instances datatables
 * structure so that it table will also be updated on success.
 *
 * @param {Instance}      instance      The instance to bve saved through datatables
 */
function saveInstanceThroughDatatables(instance) {
    // And call back to the datatables to handle the create/update
    if(newInstance) {
        instancesTable.context[0].oInit.onAddRow(instancesTable,
                instance,
                (data) => { instancesTable.row.add(data).draw(false); },
                (response, status, more) => { showError(getErrorFromHeader(response, "Unknown error while saving the instance.")); hideLoader(); });
    } else {
        instancesTable.context[0].oInit.onEditRow(instancesTable,
                instance,
                (data,b,c,d,e) => { instancesTable.ajax.reload(); },
                (response, status, more) => { showError(getErrorFromHeader(response, "Unknown error while updating the instance.")); hideLoader(); });
    }
}

/**
 * This function will load the instance status onto the instance status select
 * input of the DOM.
 *
 * @param {Event}       event       The event that took place
 * @param {DataTable}   table       The AtoN type table
 * @param {Node}        button      The button node that was pressed
 * @param {any}         config      The table configuration
 */
function loadInstanceStatus(event, table, button, config) {
    // If a row has been selected load the data into the form
    if(table.row({selected : true})) {
        var rowdata = table.row({selected : true}).data();
        $("#instanceStatusPanel").find("#instanceStatusSelect").val(rowdata["status"]);
    }
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
    api.instancesApi.setStatus(id, status, () => {
        $modalDiv.removeClass('loading');
        instancesTable.draw('page');
    }, (response, status, more) => {
        $modalDiv.removeClass('loading');
        showError(getErrorFromHeader(response, "Error while trying to update the instance status!"));
    });
}

/**
 * This function will load the instance ledger status onto the instance ledger
 * status select input of the DOM. Always read the ledger status value from
 * the server to pick up successes and failures.
 *
 * @param {Event}       event       The event that took place
 * @param {DataTable}   table       The AtoN type table
 * @param {Node}        button      The button node that was pressed
 * @param {any}         config      The table configuration
 */
function loadInstanceLedgerStatus(event, table, button, config) {
    // If a row has been selected load the data into the form
    if(table.row({selected : true})) {
        var rowdata = table.row({selected : true}).data();
        id = rowdata["id"];
        ledgerRequestId = rowdata["ledgerRequestId"];
        ledgerRequestStatus = rowdata["ledgerRequestStatus"];
        // First always start with the last known value
        $("#instanceLedgerPanel").find("#instanceLedgerStatusSelect").val(ledgerRequestStatus);
        // And then query the server for an update
        api.ledgerRequestsApi.getLedgerRequest(ledgerRequestId, (response) => {
            $("#instanceLedgerPanel").find("#instanceLedgerStatusSelect").val(response["status"]);
        });
    }
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
    api.instancesApi.setLedgerStatus(id, status, () => {
        $modalDiv.removeClass('loading');
        instancesTable.draw('page');
    }, (response, status, more) => {
        $modalDiv.removeClass('loading');
        showError(getErrorFromHeader(response, "Error while trying to update the instance global ledger status!"));
    });
}

/**
 * This helper function returns a band new blank instance object to be used
 * for generating new entries.
 */
function initialiseInstanceData(g1128Compliant) {
    // Create the new object
    var newRowData = {}

    // Initialise some of the row data fields with empty values
    newRowData["comment"] = "";
    newRowData["instanceAsDoc"] = null;
    newRowData["geometryContentType"] = null;
    newRowData["specifications"] = {};
    newRowData["geometryJson"] = {};
    newRowData["instanceAsDoc"] = null;
    newRowData["instanceAsXml"] = g1128Compliant ? {
        name: "xml",
        comment: "no comment", content: "",
        contentContentType: "G1128 Instance Specification XML"
    } : null;

    // Return the object
    return newRowData;
}

/**
 * This helper function corrects the type of data being read from the instance
 * dialog form since most of it comes back as a string.
 *
 * @param {Array}       rowData     The row data
 * @param {String}      field       The field to be parsed
 * @param {String}      value       The value of the field
 * @param {Array}       columnDefs  The definitions of the column data
 */
function alignInstanceData(rowData, field, value, columnDefs){
    if (field && columnDefs.includes(field)){
        if (field === 'id'){
            rowData[field] = parseInt(value);
        }
        else if(["keywords", "serviceType", "unlocode"].includes(field)) {
            rowData[field] = value.split(",");
        }
        else if( field === "designs") {
            rowData[field] = value ? { [value.split(",")[0]]: value.split(",")[1] } : null;
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
 * Displays the instance doc file selection field in the instance edit dialogs
 * and hides the doc name field.
 *
 * @param {Component}   $modalDiv   The modal component performing the operation
 */
function showInstanceDoc($modalDiv) {
    $modalDiv.find("#instanceAsDoc").hide();
    $modalDiv.find("#instanceAsDocWithValue").show();
}



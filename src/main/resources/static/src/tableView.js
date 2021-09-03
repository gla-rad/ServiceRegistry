/**
 * Global variables
 */
var instancesTable = undefined;

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
}];

$(document).ready( function () {
    instancesTable = $('#instancesTable').DataTable({
        processing: true,
        language: {
            processing: '<i class="fa fa-spinner fa-spin fa-3x fa-fw"></i><span class="sr-only">Loading...</span>',
        },
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
        dom: "<'row'<'col-lg-2 col-md-4'B><'col-lg-2 col-md-4 pt-1'l><'col-lg-8 col-md-4'f>><'row'<'col-md-12'rt>><'row'<'col-md-6'i><'col-md-6'p>>",
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
        }],
        onAddRow: function (datatable, rowdata, success, error) {
            $.ajax({
                url: '/api/instances',
                type: 'POST',
                contentType: 'application/json',
                dataType: 'json',
                data: JSON.stringify(rowdata),
                success: success,
                error: error
            });
        },
        onDeleteRow: function (datatable, rowdata, success, error) {
            $.ajax({
                url: `/api/instances/${rowdata["id"]}`,
                type: 'DELETE',
                contentType: 'application/json',
                success: success,
                error: error
            });
        },
        onEditRow: function (datatable, rowdata, success, error) {
            console.log($.fn.dataTable);
            $.ajax({
                url: `/api/instances/${rowdata["id"]}`,
                type: 'PUT',
                contentType: 'application/json',
                dataType: 'json',
                data: JSON.stringify(rowdata),
                success: success,
                error: error
            });
        },
        onValidateXml: function (datatable, rowdata, success, error) {
            $.ajax({
                url: `api/xmls/validate/INSTANCE`,
                type: 'POST',
                contentType: 'application/xml',
                dataType: 'json',
                data: rowdata,
                success: success,
                error: error
            });
        },
    });

    // We also need to link the station areas toggle button with the the modal
    // panel so that by clicking the button the panel pops up. It's easier done with
    // jQuery.
    instancesTable.buttons('.instance-edit-panel-toggle')
        .nodes()
        .attr({ "data-toggle": "modal", "data-target": "#instanceEditPanel" });

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
        var rowData = {}
        var create = true;

        // Load the column definitions
        var columnDefData = columnDefs.map((e) => e["data"]);

        // Initialise some of the row data fields with empty values
        rowData["comment"] = "";
        rowData["instanceAsDoc"] = null;
        rowData["geometryContentType"] = null;
        rowData["designs"] = {};
        rowData["specifications"] = {};
        rowData["geometryJson"] = {};
        rowData["docs"] = [];
        rowData["instanceAsXml"] = { name: "xml", comment: "no comment", content: "", contentContentType: "G1128 Instance Specification XML" };

        // If a row has been selected load the data for an update
        if(instancesTable.row({selected : true}).length != 0) {
            rowData = instancesTable.row({selected : true}).data();
            create = false;
        }

        // Getting the inputs from the modal
        $('form[name="instanceEditPanelForm"] :input').each(function() {
            rowData = alignData(rowData, $(this).attr('id'), $(this).val(), columnDefData);
        });

        // Augmenting xml content on the data
        var xmlContent = $modalDiv.find("#xml-input").val();
        if (xmlContent.length>0){
            rowData["instanceAsXml"]["content"] = xmlContent;
        } else {
            rowData["instanceAsXml"]["content"] = {
                name: "xml",
                comment: "no comment",
                content: "", contentContentType: "G1128 Instance Specification XML"
            };
        }

        // And call back to the datatables to handle the create/update
        if(create) {
            instancesTable.context[0].oInit.onAddRow(instancesTable,
                    rowData,
                    (data) => { instancesTable.row.add(data).draw(false); },
                    (data) => { showError(data.responseJSON.message) });
        } else {
            instancesTable.context[0].oInit.onEditRow(instancesTable,
                    rowData,
                    (data,b,c,d,e) => { instancesTable.row({selected : true}).data(data); instancesTable.draw('page'); },
                    (data) => { showError(data.responseJSON.message) });
        }
    });
});

/**
 * Using an AJAX call we ask the server to validate the provided XML and if
 * proven correct we can use the returned JSON object to populate the instance
 * editor field values. Note that some of the G-1128 field do not correspond
 * to the names of the fields used here so we need to translate them.
 */
function onValidateXml($modalDiv) {
    $.ajax({
        url: `api/xmls/validate/INSTANCE`,
        type: 'POST',
        contentType: 'application/xml',
        dataType: 'json',
        data: $modalDiv.find("#xml-input").val(),
        success: (response, status, more) => {
            for (var field in response) {
                var name = field;
                var value = response[name];
                // Translate the G1128 field names to the current model
                if (name == 'id')
                   name = 'instanceId';
                else if(name == 'description')
                   name = 'comment';
                else if(name == 'endpoint')
                   name = 'endpointUri';
                else if(name == 'implementsServiceDesign') {
                   name = 'designs';
                   value = value['id'];
                }
                $modalDiv.find("input#"+name).val(value);
            }
            $modalDiv.removeClass('loading');
        },
        error: (response, status, more) => {
            $modalDiv.removeClass('loading');
            showError("The provided XML does not seems to be G-1128 compliant!");
        }
    });
}

/**
 * The instances edit dialog form should be clear every time before it is used
 * so that new entries are not polluted by old data.
 */
function clearInstanceEditPanel() {
    $('form[name="instanceEditPanelForm"]').trigger("reset");
}

/**
 * This helper function loads the XML and field data from the selected inctance
 * in the instance table onto the edit dialog.
 */
function loadInstanceEditPanel() {
    // First always clear to be sure
    clearInstanceEditPanel();

    // If a row has been selected load the data into the form
    if(instancesTable.row({selected : true})) {
        rowData = instancesTable.row({selected : true}).data();
        $('form[name="instanceEditPanelForm"] :input').each(function() {
             $(this).val(rowData[$(this).attr('id')]);
        });

        // Augmenting xml content on the data
        $("#instanceEditPanel").find("#xml-input").val(rowData["instanceAsXml"]["content"]);
    }
}

/**
 * This helper function corrects the type of data being read from the instance
 * dialog form since most of it comes back as a string.
 */
function alignData(rowDataArray, id, value, columnDefs){
    if (id && columnDefs.includes(id)){
        if (id === 'geometryJson'){
            rowDataArray[id] = JSON.stringify(value);
        }
        else if (id === 'id'){
            rowDataArray[id] = parseInt(value);
        }
        else{
            rowDataArray[id] = value;
        }
    }
    return rowDataArray;
}
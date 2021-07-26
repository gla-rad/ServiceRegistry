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
    orderable: false,
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

function nullIfEmpty(value){
    if (value !== null && value === "")
        return undefined;
    else
        return value;
}

$(document).ready( function () {
    table = $('#table_id').DataTable({
//        processing:: true,
//        language: {
//            processing: '<i class="fa fa-spinner fa-spin fa-3x fa-fw"></i><span class="sr-only">Loading...</span>',
//        },
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
        dom: 'Bfrltip',
        select: 'single',
        lengthMenu: [10, 25, 50, 75, 100],
        responsive: true,
        altEditor: true, // Enable altEditor
        buttons: [{
            text: '<i class="fas fa-plus-circle"></i>',
            titleAttr: 'Add Instance',
            name: 'add' // do not change name
        }, {
            extend: 'selected', // Bind to Selected row
            text: '<i class="fas fa-edit"></i>',
            titleAttr: 'Edit Instance',
            name: 'edit' // do not change name
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
            console.log(rowdata);
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
} );

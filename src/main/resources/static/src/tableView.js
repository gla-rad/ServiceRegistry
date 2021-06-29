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
    title: "name",
    readonly : true,
    hoverMsg: "Name of service",
    placeholder: "Name of the service"
}, {
    data: "version",
    title: "version",
    readonly : true,
    hoverMsg: "Version of service",
    placeholder: "Version of the service"
}, {
    data: "serviceType",
    title: "serviceType",
    readonly : true,
    hoverMsg: "Type of service",
    placeholder: "Type of the service"
}, {
    data: "status",
    title: "status",
    readonly : true,
    hoverMsg: "Status of service",
    placeholder: "Status of the service"
}, {
    data: "endpointUri",
    title: "endpointUri",
    readonly : true,
    hoverMsg: "Access point of service",
    placeholder: "Access point of the service"
}, {
    data: "organizationId",
    title: "organizationId",
    readonly : true,
    hoverMsg: "MRN of service provider",
    placeholder: "MRN of the service provider (organization)"
}, {
    data: "keywords",
    title: "keywords",
    readonly : true,
    hoverMsg: "Keywords of service",
    placeholder: "Keywords of the service"
}, {
    data: "instanceId",
    title: "instanceId",
    readonly : true,
    hoverMsg: "MRN of service instance description",
    placeholder: "MRN of the service instance description"
}, {
    data: "lastUpdatedAt",
    title: "lastUpdatedAt",
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
        ajax: {
            "type": "GET",
            "url": "/api/instances",
            "dataType": "json",
            "cache": false,
            "dataSrc": function (json) {
                return json;
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
                dataType: 'json',
                data: JSON.stringify(rowdata),
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

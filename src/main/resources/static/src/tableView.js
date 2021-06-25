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
    hoverMsg: "Name of service",
    required: true,
    placeholder: "Name of the service"
}, {
    data: "version",
    title: "version",
    hoverMsg: "Version of service",
    placeholder: "Version of the service"
}, {
    data: "serviceType",
    title: "serviceType",
    hoverMsg: "Type of service",
    placeholder: "Type of the service"
}, {
    data: "status",
    title: "status",
    type: "select",
    options: ["PENDING","LIVE", "INACTIVE"],
    hoverMsg: "Status of service",
    placeholder: "Status of the service"
}, {
    data: "endpointUri",
    title: "endpointUri",
    hoverMsg: "Access point of service",
    placeholder: "Access point of the service"
}, {
    data: "organizationId",
    title: "organizationId",
    hoverMsg: "MRN of service provider",
    placeholder: "MRN of the service provider (organization)"
}, {
    data: "keywords",
    title: "keywords",
    hoverMsg: "Keywords of service",
    placeholder: "Keywords of the service"
}, {
    data: "instanceId",
    title: "instanceId",
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
    data: "designs",
    title: "designs",
    hoverMsg: "MRN of service technical design",
    placeholder: "MRN of the service technical design",
    visible: false,
    searchable: false
}, {
    data: "specifications",
    title: "specifications",
    hoverMsg: "MRN of service specification",
    placeholder: "MRN of the service specification",
    visible: false,
    searchable: false
}, {
    data: "publishedAt",
    type: "hidden",
    visible: false,
    searchable: false
}, {
    data: "instanceAsXml",
    type: "hidden",
    visible: false,
    searchable: false
}, {
    data: "instanceAsDoc",
    type: "hidden",
    visible: false,
    searchable: false
}, {
    data: "comment",
    type: "hidden",
    visible: false,
    searchable: false
}, {
    data: "geometry",
    type: "hidden",
    visible: false,
    searchable: false
}, {
    data: "geometryContentType",
    type: "hidden",
    visible: false,
    searchable: false
}, {
    data: "unlocode",
    type: "hidden",
    visible: false,
    searchable: false
}, {
    data: "mmsi",
    type: "hidden",
    visible: false,
    searchable: false
}, {
    data: "imo",
    type: "hidden",
    visible: false,
    searchable: false
}, {
    data: "docs",
    type: "hidden",
    visible: false,
    searchable: false
}, {
    data: "geometryJson",
    type: "hidden",
    visible: false,
    searchable: false
}, {
    data: "endpointType",
    type: "hidden",
    visible: false,
    searchable: false
}];

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
                contentType: 'application/json; charset=utf-8',
                dataType: 'json',
                data: JSON.stringify({
                    model: rowdata["model"],
                    manufacturer: rowdata["manufacturer"],
                    type: rowdata["type"],
                    nomPower: nullIfEmpty(rowdata["nomPower"]),
                    notes: rowdata["notes"]
                }),
                success: success,
                error: error
            });
        },
        onDeleteRow: function (datatable, rowdata, success, error) {
            $.ajax({
                url: `/api/instances/${rowdata["id"]}`,
                type: 'DELETE',
                contentType: 'application/json; charset=utf-8',
                dataType: 'json',
                data: JSON.stringify(rowdata),
                success: success,
                error: error
            });
        },
        onEditRow: function (datatable, rowdata, success, error) {
            $.ajax({
                url: `/api/instances/${rowdata["id"]}`,
                type: 'PUT',
                contentType: 'application/json; charset=utf-8',
                dataType: 'json',
                data: JSON.stringify({
                    id: rowdata["id"],
                    model: rowdata["model"],
                    manufacturer: rowdata["manufacturer"],
                    type: rowdata["type"],
                    nomPower: nullIfEmpty(rowdata["nomPower"]),
                    notes: rowdata["notes"]
                }),
                success: success,
                error: error
            });
        }
    });
} );

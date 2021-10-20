/**
 * Standard jQuery initialisation of the page were all buttons are assigned an
 * operation and the form doesn't really do anything.
 */
$(() => {
    console.log("Content Loaded");
});

/**
 * A helper function that displays the loader spinner.
 */
function showLoader(overlay = true) {
    if(!overlay) {
        $('#pageLoaderBackground').hide();
    } else {
        $('#pageLoaderBackground').show();
    }
    $('#pageLoader').show();
}

/**
 * A helper function that hides the loader spinner.
 */
function hideLoader() {

    $('#pageLoader').hide();
}

/**
 * A helper function that parses the response's and and returns the value
 * from the 'X-mcsrApp-error' header if it exists, or the selected default
 * message if not.
 */
function getErrorFromHeader(response, defaultMessage) {
    return response.getResponseHeader('X-mcsrApp-error') ? response.getResponseHeader('X-mcsrApp-error') : defaultMessage;
}

/**
 * A helper function that shows the error boostrap error dialog and displays
 * the provided error message in it.
 */
function showError(errorMsg) {
    if(!errorMsg || errorMsg.length === 0) {
        errorMsg = "An unknown error occurred!";
    } else {
        errorMsg = errorMsg.replace(/^(error\.)/,"");
    }
    $('#error-dialog').modal('show');
    $('#error-dialog .modal-body').html(`<p class="text-danger">${errorMsg}</p>`);
}

/**
 * The Attachments Table Column Definitions
 * @type {Array}
 */
var attachmentsTableColumnDefs = [{
    data: "id",
    title: "ID",
    type: "hidden",
    "visible": false,
    "searchable": false
}, {
    data: "name",
    title: "Name",
    hoverMsg: "The name of the attachment",
    required: true
}, {
    data: "mimetype",
    title: "Type",
    hoverMsg: "The file type of the attachment"
}];

//Define this globally so that we can manipulate them
var attachmentsTable = undefined;

/**
 * Initialises a new bootstrap file uploader component on the selected container
 * and allows users to upload file to the specified AJAX URL. For easy handling,
 * a success callback is also supported. Note that just to be sure, the file
 * uploaded will first be destroyed (at least if it exists), and then recreated
 * every single time.
 *
 * @param {str} containerSelector   The container selector to initialise the uploader in
 * @param {str} ajaxUrl             The AJAX URL to upload the file to
 * @param {*} callback              The success callback
 */
function loadFileUploader(containerSelector, ajaxUrl, callback) {
    if(attachmentsTable) {
        attachmentsTable.destroy();
    }

    // First popuplate the attachments datatable
    attachmentsTable = $('#attachmentsTable').DataTable({
        ajax: {
            type: "GET",
            url: ajaxUrl,
            dataType: "json",
            cache: false,
            dataSrc: function (json) {
                return json;
            },
            error: function (jqXHR, ajaxOptions, thrownError) {
                console.error(thrownError);
            }
        },
        columns: attachmentsTableColumnDefs,
        dom: "<'row'<'col-md-auto'B><'col-sm-4 pb-1'l><'col-md col-sm-4'f>><'row'<'col-md-12't>><'row'<'col-md-6'i><'col-md-6'p>>",
        select: 'single',
        lengthMenu: [10, 25, 50, 75, 100],
        responsive: true,
        altEditor: true, // Enable altEditor
        buttons: [{
            extend: 'selected', // Bind to Selected row
            text: '<i class="fas fa-trash-alt"></i>',
            titleAttr: 'Delete Attachment',
            name: 'delete' // do not change name
        }, {
            extend: 'selected', // Bind to Selected row
            text: '<i class="fas fa-download"></i>',
            titleAttr: 'Download Attachment',
            name: 'download', // do not change name
            action: (e, dt, node, config) => {
                var idx = dt.cell('.selected', 0).index();
                var data = dt.row(idx.row).data();
                downloadDoc(data["id"]);
            }
        }],
        onDeleteRow: function (datatable, rowdata, success, error) {
            $.ajax({
                url: `${ajaxUrl}/${rowdata["id"]}`,
                type: 'DELETE',
                contentType: 'application/json; charset=utf-8',
                dataType: 'json',
                data: JSON.stringify(rowdata),
                success: success,
                error: error
            });
        }
    });

    // Then create the file uploading component
    $(containerSelector).fileinput("destroy");
    $(containerSelector).fileinput({
        theme: 'fas',
        showPreview: true,
        allowedFileExtensions: ['pdf', 'odt', 'doc', 'docx', 'xls', 'xlsx', 'jpeg', 'jpg', 'png', 'gif'],
        uploadUrl: ajaxUrl,
    }).on('fileuploaded', function(e, params) {
        attachmentsTable.ajax.reload();
    })
}

/**
 * Download the selected document ID from the server, decode the data from
 * the provided Base64 format.
 *
 * @param {number}      docId           The ID of the document to be opened
 */
function downloadDoc(docId) {
    showLoader();
    new DocsApi().getDoc(docId, (doc) => {
        fileUtils.downloadFile(doc.name, doc.filecontentContentType, doc.filecontent);
        hideLoader();
    }, (response, status, more) => {
        hideLoader();
        showError(getErrorFromHeader(response, "Error while trying to retrieve the instance doc!"));
    });
}
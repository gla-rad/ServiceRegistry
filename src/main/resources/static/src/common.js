/**
 * The API Call Libraries
 */
var api = undefined;

/**
 * Standard jQuery initialisation of the page.
 */
$(() => {
    // First link the API libs
    api = {};
    api.instancesApi = new InstancesApi();
    api.xmlsApi = new XmlsApi();
    api.docsApi = new DocsApi();
    api.search = new SearchApi();
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
 * Defines a common way of dealing with the AJAX call errors, i.e if we have a
 * callback use it, otherwise just show the error in the console.
 */
function handleAjaxError(response, status, more, errorCallback) {
    if(errorCallback) {
        errorCallback(response, status, more);
    } else {
        console.error(response);
    }
}

/**
 * A helper function that parses the response's and and returns the value
 * from the 'X-mcsrApp-error' header if it exists, or the selected default
 * message if not. Note that the default message might be overriden if the
 * original request has been forbidden.
 */
function getErrorFromHeader(response, defaultMessage) {
    // Try to figure out if we have a default message and use it if it makes
    // sense. If a forbidden status has been returned though, we are fixed.
    defaultMessage = (response.status === 0 || response.status === 403) ?
        "You do not seem to have the right permissions to perform this action." :
        (defaultMessage ? defaultMessage : "An unknown error occurred while performing this action.");
    // BTW always give priority to the response header messages
    return response.getResponseHeader('X-mcsrApp-error') ? response.getResponseHeader('X-mcsrApp-error') : defaultMessage;
}

/**
 * A helper function that shows the error boostrap error dialog and displays
 * the provided error message in it.
 */
function showError(errorMsg) {
    if(!errorMsg || errorMsg.length === 0) {
        errorMsg = "An unknown error occurred while performing this action.";
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
}, {
     data: "comment",
     title: "Comment",
     hoverMsg: "Comments on the attachment"
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
 * @param {str} instanceId          The ID of the instance the documents belong to
 * @param {str} ajaxUrl             The AJAX URL to upload the file to
 * @param {*} callback              The success callback
 */
function loadFileUploader(instanceId, ajaxUrl, callback) {
    if(attachmentsTable) {
        attachmentsTable.clear();
        attachmentsTable.destroy();
        attachmentsTable = undefined;
        //$('#attachmentsTable').empty();
    }

    // First populate the attachments datatable
    attachmentsTable = $('#attachmentsTable').DataTable({
        serverSide: true,
        processing: true,
        ajax: {
            type: "POST",
            url: `${ajaxUrl}/dt?instanceId=${instanceId}`,
            contentType: "application/json",
            crossDomain: true,
            data: function (d) {
                return JSON.stringify(d);
            },
            error: function (jqXHR, ajaxOptions, thrownError) {
                console.error(thrownError);
            }
        },
        columns: attachmentsTableColumnDefs,
        dom: "<'row'<'col-md-auto'B><'col-sm-4 pb-1'l><'col-md col-sm-4'f>><'row'<'col-md-12'rt>><'row'<'col-md-6'i><'col-md-6'p>>",
        select: 'single',
        lengthMenu: [10, 25, 50, 75, 100],
        responsive: true,
        altEditor: true, // Enable altEditor
        buttons: [{
            extend: 'selected', // Bind to Selected row
            text: '<i class="fa-solid fa-trash-can"></i>',
            titleAttr: 'Delete Attachment',
            name: 'delete' // do not change name
        }, {
            extend: 'selected', // Bind to Selected row
            text: '<i class="fa-solid fa-download"></i>',
            titleAttr: 'Download Attachment',
            name: 'download', // do not change name
            action: (e, dt, node, config) => {
                var idx = dt.cell('.selected', 0).index();
                var data = dt.row(idx.row).data();
                downloadDoc($('#attachmentsPanel'), data["id"]);
            }
        }],
        onDeleteRow: function (datatable, selectedRows, success, error) {
            selectedRows.every(function (rowIdx, tableLoop, rowLoop) {
                $.ajax({
                    url: `${ajaxUrl}/${this.data()["id"]}`,
                    type: 'DELETE',
                    contentType: 'application/json',
                    crossDomain: true,
                    success: success,
                    error: error
                });
            });
        }
    });

    // Then create the file uploading component
    $("#file-uploader-id").fileinput("destroy");
    $("#file-uploader-id").fileinput({
        theme: 'fas',
        showPreview: true,
        showUpload: false,
        allowedFileExtensions: ['pdf', 'doc', 'docx', 'odt', 'xls', 'xlsx', 'jpeg', 'jpg', 'png', 'gif']
    });

    $('#attachmentUploadButton').unbind('click');
    $('#attachmentUploadButton').on('click', (e) => {
        e.preventDefault();
        var uploadFiles = $("#file-uploader-id").prop('files');
        // Sanity Check
        if(!uploadFiles || uploadFiles.length == 0) {
            return;
        }
        var $modalDiv = $('#attachmentsPanel');
        $modalDiv.addClass('loading');
        encodeFilesToBase64(uploadFiles)
            .then((attachments) => {
                var completedUploads = 0;
                for(var attachment of attachments) {
                    // Create the document object
                    var doc = {
                        'name': attachment.file.name,
                        'comment': $("#file-uploader-comment-id").val(),
                        'mimetype': attachment.file.type,
                        'filecontent': attachment.content,
                        'filecontentContentType': attachment.file.type,
                        'instanceId': instanceId
                    };
                    // Upload the document
                    api.docsApi.createDoc(JSON.stringify(doc),
                        (result) => {
                            completedUploads++;
                            // In the last iteration stop the loader
                            if(completedUploads == attachments.length) {
                                $("#attachmentForm").trigger("reset");
                                attachmentsTable.ajax.reload();
                                $modalDiv.removeClass('loading');
                            }
                        },
                        (response, status, more) => {
                           completedUploads++;
                           // In the last iteration stop the loader
                           if(completedUploads == attachments.length) {
                               $("#attachmentForm").trigger("reset");
                               attachmentsTable.ajax.reload();
                               $modalDiv.removeClass('loading');
                           }
                           showError(getErrorFromHeader(response, "Error while trying to upload the attachment!"));
                        }
                    );
                }
            });
    });
}

/**
 * Download the selected document ID from the server, decode the data from
 * the provided Base64 format.
 *
 * @param {Component}   $modalDiv   The modal component performing the operation
 * @param {number}      docId       The ID of the document to be opened
 */
function downloadDoc($modalDiv, docId) {
    $modalDiv.addClass('loading');
    api.docsApi.getDoc(docId, (doc) => {
        downloadFile(doc.name, doc.filecontentContentType, doc.filecontent);
        $modalDiv.removeClass('loading');
    }, (response, status, more) => {
        $modalDiv.removeClass('loading');
        showError(getErrorFromHeader(response, "Error while trying to retrieve the instance doc!"));
    });
}

/**
 *
 */
function addMapEntry(tableId, key, value, mapEntries) {
// Use non-empty key values if you have to
    key = key ? key : "urn:mrn:unknown:"+(mapEntries.size+1);

    // Sanity Check
    if(!mapEntries) {
        mapEntries = new Map();
    }
    mapEntries.set(key,value);
    $("#"+tableId).data('entries', mapEntries);

    // Create a new row
    const $newEntryRow = $("<tr>");

    // Create the key cell
    const $keyCell = $('<td>');
    const $keyInput = $("<input>");
    $keyInput.addClass("form-control form-control-sm").val(key);
    $keyInput.on('change', () => {
        var oldKey = $keyInput.data('val');
        mapEntries.delete(oldKey);
        mapEntries.set($keyInput.val(), $valueInput.val());
        $("#"+tableId).data('entries', mapEntries);
    });
    $keyCell.append($keyInput);
    $newEntryRow.append($keyCell);

    // Create the value cell
    const $valueCell = $('<td>');
    const $valueInput = $("<input>");
    $valueInput.addClass("form-control form-control-sm").val(value);
    $valueInput.on('change', () => {
        mapEntries.set($keyInput.val(), $valueInput.val());
        $("#"+tableId).data('entries', mapEntries);
    });
    $valueCell.append($valueInput);
    $newEntryRow.append($valueCell);

    // Create the button cell
    const $actionsCell = $('<td>');
    const $removeButton = $("<div>");
    $removeButton.addClass("btn btn-sm btn-danger");
    $removeButton.on('click', () => {
        mapEntries.delete($keyInput.val());
        $("#"+tableId).data('entries', mapEntries);
        updateTable(tableId, mapEntries);
    });
    $buttonSpan = $("<span>")
    $buttonSpanI = $("<i>")
    $buttonSpanI.addClass("fa-solid fa-trash-can");
    $buttonSpan.append($buttonSpanI);
    $removeButton.append($buttonSpan);
    $actionsCell.append($removeButton);
    $newEntryRow.append($actionsCell);

    // And the row to the table
    $('#'+tableId+' tbody').append($newEntryRow);
}

/**
 *
 */
function updateTable(tableId, mapEntries) {
    // Sanity Checks
    if(!tableId) {
        return;
    }
    if(!mapEntries) {
        mapEntries = new Map();
    }

    // Clear the table buttons and body
    $('#'+tableId+'AddButtonHeader').html("");
    $('#'+tableId+' tbody').html("");

    // And add a new button
    var $button = $('<button type="button" id="'+tableId+'AddEntryButton"/>');
    $button.addClass("btn btn-sm btn-success");
    $button.on("click", () => {
        addMapEntry(tableId, "", "", mapEntries);
    });
    $buttonSpan = $("<span>")
    $buttonSpanI = $("<i>")
    $buttonSpanI.addClass("fa-solid fa-circle-plus");
    $buttonSpan.append($buttonSpanI);
    $button.append($buttonSpan);
    $button.appendTo($('#'+tableId+'AddButtonHeader'));

    // Now iterate and add all map values
    var index = 1;
    for (const [key, value] of mapEntries) {
        addMapEntry(tableId, key ? key : "urn:mrn:unknown:"+index, value, mapEntries);
        index++;
    }
}
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
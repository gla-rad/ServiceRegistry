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
    defaultMessage = response.status === 403 ?
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
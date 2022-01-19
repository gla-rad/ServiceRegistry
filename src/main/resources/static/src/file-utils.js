/******************************************************************************
 *                               FILE UTILITIES                               *
 ******************************************************************************/
/**
 * Opens the provided file data (based on its type) in a new browser window.
 *
 * @param {string}      type        The file type to be opened
 * @param {string}      data        The Base64 encoded data
 */
function openFileWindow(type, data) {
    window.open('data:' + type + ';base64,' + data, '_blank', 'height=800,width=900');
}

/**
 * Encodes the provided file into a Base64 string and returns it using a
 * promise.
 *
 * @param {file}        file        The file to be encoded
 */
function encodeFileToBase64(file) {
    var fileReader = new FileReader();
    return new Promise((resolve, reject) => {
        fileReader.onload = (e) => {
            var base64Data = e.target.result.substr(e.target.result.indexOf('base64,') + 'base64,'.length);
            resolve({ 'file': file, 'content': base64Data });
        }
        fileReader.readAsDataURL(file)
     });
}

/**
 * Encodes the provided file array into Base64 strings and returns it using
 * a promise all construct.
 *
 * @param {Array}       file        The files to be encoded
 */
function encodeFilesToBase64(files) {
    return Promise.all(Array.prototype.slice.call(files).map(file => encodeFileToBase64(file)));
}

/**
 * Opens a download dialogs for saving the provided file data into a local
 * file.
 *
 * @param {string}      filename    The name of the file to be saved
 * @param {string}      type        The file type to be saved
 * @param {string}      data        The Base64 encoded data
 */
function downloadFile(filename, type, data) {
    // decode base64 string, remove space for IE compatibility
    var binary = atob(data.replace(/\s/g, ''));
    var len = binary.length;
    var buffer = new ArrayBuffer(len);
    var view = new Uint8Array(buffer);
    for (var i = 0; i < len; i++) {
        view[i] = binary.charCodeAt(i);
    }

    var blob = new Blob([view], {type: type});
    if(window.navigator.msSaveOrOpenBlob) {
        window.navigator.msSaveBlob(blob, filename);
    }
    else {
        var elem = window.document.createElement('a');
        elem.href = window.URL.createObjectURL(blob);
        elem.download = filename;
        document.body.appendChild(elem);
        elem.click();
        document.body.removeChild(elem);
    }
}
/******************************************************************************/
/******************************************************************************
 *                               FILE UTILITIES                               *
 ******************************************************************************/
class FileUtils {

    /**
     * The FileUtils Class Constructor.
     */
    constructor() {

    }

    /**
     * Opens the provided file data (based on its type) in a new browser window.
     *
     * @param {string}      type        The file type to be opened
     * @param {string}      data        The Base64 encoded data
     */
    openFileWindow(type, data) {
        window.open('data:' + type + ';base64,' + data, '_blank', 'height=800,width=900');
    }

    /**
     * Encodes the provided file into a Base64 string and returns it using the
     * provided callback.
     *
     * @param {file}        file        The file to be encoded
     * @param {Function}    callback    The callback function to return the encoded file
     */
    encodeFileToBase64(file, callback) {
        var fileReader = new FileReader();
        fileReader.onload = function (e) {
            var base64Data = e.target.result.substr(e.target.result.indexOf('base64,') + 'base64,'.length);
            callback(base64Data);
        };
        fileReader.readAsDataURL(file);
    }

    /**
     * Opens a download dialogs for saving the provided file data into a local
     * file.
     *
     * @param {string}      filename    The name of the file to be saved
     * @param {string}      type        The file type to be saved
     * @param {string}      data        The Base64 encoded data
     */
    downloadFile(filename, type, data) {
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
}
/******************************************************************************/
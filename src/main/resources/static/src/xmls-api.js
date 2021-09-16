/******************************************************************************
 *                               XMLS API CALLS                               *
 ******************************************************************************/
class XmlsApi {

    /**
     * The XMLs API Class Constructor.
     */
    constructor() {

    }
    
    /**
     * API Instance XML reading function.
     *
     * @param  {number} xmlId           The ID of the xml to be retrieved
     * @param  {Function} callback      The callback to be used after the AJAX call
     * @param  {Function} errorCallback The error callback to be used if the AJAX call fails
     */
    getXml(xmlId, callback, errorCallback) {
        $.ajax({
            url: `api/xmls/${xmlId}`,
            type: 'GET',
            contentType: 'application/json',
            crossDomain: true,
            success: callback,
            error: (response, status, more) => {
                if(errorCallback) {
                    errorCallback(response, status, more);
                } else {
                    console.error(response)
                }
            }
        });
    }

    /**
     * API Instance XML validation function.
     *
     * @param  {string} xml             The XML input to be validated
     * @param  {Function} callback      The callback to be used after the AJAX call
     * @param  {Function} errorCallback The error callback to be used if the AJAX call fails
     */
    validateInstanceXml(xml, callback, errorCallback) {
        $.ajax({
            url: `api/xmls/validate/INSTANCE`,
            type: 'POST',
            contentType: 'application/xml',
            crossDomain: true,
            dataType: 'json',
            data: xml,
            success: (response, status, more) => {
                var instance = {};
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
                    instance[name] = value
                }
                callback(instance)
            },
            error: (response, status, more) => {
                if(errorCallback) {
                    errorCallback(response, status, more);
                } else {
                    console.error(response)
                }
            }
        });
    }
}
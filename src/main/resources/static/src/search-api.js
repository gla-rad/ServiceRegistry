/******************************************************************************
 *                             SEARCH API CALLS                               *
 ******************************************************************************/
class SearchApi {

    /**
     * The Instances API Class Constructor.
     */
    constructor() {

    }

    /**
     * API instance search function.
     *
     * @param  {string} queryString     The instance query string to be used
     * @param  {Function} callback      The callback to be used after the AJAX call
     * @param  {Function} errorCallback The error callback to be used if the AJAX call fails
     */
    searchInstances(queryString, callback, errorCallback) {
        $.ajax({
            url: '/api/_search/instances',
            type: 'GET',
            contentType: 'application/json',
            data: {
                query: queryString,
                page: 0,
                size: 100
            },
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
}
/******************************************************************************/
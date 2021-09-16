/******************************************************************************
 *                         LEDGER REQUESTS API CALLS                          *
 ******************************************************************************/
class LedgerRequestsApi {

    /**
     * The Ledger Requests API Class Constructor.
     */
    constructor() {

    }

    /**
     * API getter function for a single ledger request by ID.
     *
     * @param  {int} id                 The ID of the ledger request to be retrieved
     * @param  {Function} callback      The callback to be used after the AJAX call
     * @param  {Function} errorCallback The error callback to be used if the AJAX call fails
     */
    getLedgerRequest(id, status, callback, errorCallback) {
        $.ajax({
            url: `/api/ledgerrequests/${id}`,
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
}
/******************************************************************************/
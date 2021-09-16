/******************************************************************************
 *                            INSTANCES API CALLS                             *
 ******************************************************************************/
class InstancesApi {

    /**
     * The Instances API Class Constructor.
     */
    constructor() {

    }

    /**
     * API instance retrieval function.
     *
     * @param  {number} id              The ID of the instance to be retrieved
     * @param  {Function} callback      The callback to be used after the AJAX call
     * @param  {Function} errorCallback The error callback to be used if the AJAX call fails
     */
    getInstance(id, callback, errorCallback) {
        $.ajax({
            url: `/api/instances/${id}`,
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
     * API instance creation function.
     *
     * @param  {obj} instance           The instance to be created
     * @param  {Function} callback      The callback to be used after the AJAX call
     * @param  {Function} errorCallback The error callback to be used if the AJAX call fails
     */
    createInstance(instance, callback, errorCallback) {
        $.ajax({
            url: '/api/instances',
            type: 'POST',
            contentType: 'application/json',
            crossDomain: true,
            dataType: 'json',
            data: instance,
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
     * API instance update function.
     *
     * @param  {int} id                 The ID of the instance to be updated
     * @param  {obj} instance           The instance to be created
     * @param  {Function} callback      The callback to be used after the AJAX call
     * @param  {Function} errorCallback The error callback to be used if the AJAX call fails
     */
    updateInstance(id, instance, callback, errorCallback) {
        $.ajax({
            url: `/api/instances/${id}`,
            type: 'PUT',
            contentType: 'application/json',
            crossDomain: true,
            dataType: 'json',
            data: instance,
            success: callback,
            error:  (response, status, more) => {
                if(errorCallback) {
                    errorCallback(response, status, more);
                } else {
                    console.error(response)
                }
            }
        });
    }

    /**
     * API instance deletion function.
     *
     * @param  {int} id                 The ID of the instance to be deleted
     * @param  {Function} callback      The callback to be used after the AJAX call
     * @param  {Function} errorCallback The error callback to be used if the AJAX call fails
     */
    deleteInstance(id, callback, errorCallback) {
        $.ajax({
            url: `/api/instances/${id}`,
            type: 'DELETE',
            contentType: 'application/json',
            crossDomain: true,
            success: callback,
            error:  (response, status, more) => {
                if(errorCallback) {
                    errorCallback(response, status, more);
                } else {
                    console.error(response)
                }
           }
        });
    }

    /**
     * API setter function for the instance status.
     *
     * @param  {int} id                 The ID of the instance to be updated
     * @param  {string} status          The status to update the instance with
     * @param  {Function} callback      The callback to be used after the AJAX call
     * @param  {Function} errorCallback The error callback to be used if the AJAX call fails
     */
    setStatus(id, status, callback, errorCallback) {
        $.ajax({
            url: `/api/instances/${id}/status?status=${status}`,
            type: 'PUT',
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
      * API setter function for the instance global ledger status.
      *
      * @param  {int} id                The ID of the instance to be updated
      * @param  {string} status         The global ledger status to update the instance with
      * @param  {Function} callback     The callback to be used after the AJAX call
      * @param  {Function} errorCallback The error callback to be used if the AJAX call fails
      */
    setLedgerStatus(id, status, callback, errorCallback) {
        $.ajax({
            url: `/api/instances/${id}/ledger-status?ledgerStatus=${status}`,
            type: 'PUT',
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
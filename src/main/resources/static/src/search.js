/**
 * Global variables
 */
var searchMap = undefined;

$(() => {
    // Now also initialise the search map before we need it
    instanceMap = L.map('searchMap').setView([54.910, -3.432], 5);
    L.tileLayer('http://{s}.tile.osm.org/{z}/{x}/{y}.png', {
        attribution: '&copy; <a href="http://osm.org/copyright">OpenStreetMap</a> contributors'
    }).addTo(instanceMap);
});
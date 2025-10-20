const Amadeus = require('amadeus');

// Initialize Amadeus client
const amadeus = new Amadeus({
    clientId: process.env.AMADEUS_CLIENT_ID,
    clientSecret: process.env.AMADEUS_CLIENT_SECRET
});

/**
 * Get an instance of the Amadeus client
 * @returns {Amadeus} The Amadeus client instance
 */
function getAmadeusClient() {
    return amadeus;
}

module.exports = {
    getAmadeusClient
}; 
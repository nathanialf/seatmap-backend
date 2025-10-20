const { getAmadeusClient } = require('./amadeus-auth');

/**
 * Search for airports using the Amadeus API
 * @param {string} keyword - The search keyword (city or airport name)
 * @returns {Promise<Array>} Array of formatted airport results
 */
async function searchAirports(keyword) {
    try {
        const amadeus = getAmadeusClient();
        const response = await amadeus.referenceData.locations.get({
            keyword: keyword,
            subType: 'AIRPORT',
            'page[limit]': 10
        });

        return response.data.map(location => ({
            iataCode: location.iataCode,
            name: location.name,
            city: location.address.cityName,
            country: location.address.countryName
        }));
    } catch (error) {
        console.error('Error searching airports:', error);
        throw error;
    }
}

module.exports = {
    searchAirports
}; 
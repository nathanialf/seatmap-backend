const { getAmadeusClient } = require('./amadeus-auth');

/**
 * Search for flights using the Amadeus API
 * @param {Object} params - Search parameters
 * @param {string} params.originLocationCode - Origin airport IATA code
 * @param {string} params.destinationLocationCode - Destination airport IATA code
 * @param {string} params.departureDate - Departure date in YYYY-MM-DD format
 * @param {string} [params.returnDate] - Return date in YYYY-MM-DD format (optional)
 * @param {number} params.adults - Number of adult passengers
 * @param {number} [params.children] - Number of child passengers (optional)
 * @param {number} [params.infants] - Number of infant passengers (optional)
 * @param {string} [params.travelClass] - Travel class (ECONOMY, PREMIUM_ECONOMY, BUSINESS, FIRST)
 * @returns {Promise<Array>} Array of flight offers
 */
async function searchFlights(params) {
    try {
        const amadeus = getAmadeusClient();
        const response = await amadeus.shopping.flightOffersSearch.get({
            originLocationCode: params.originLocationCode,
            destinationLocationCode: params.destinationLocationCode,
            departureDate: params.departureDate,
            returnDate: params.returnDate,
            adults: params.adults,
            children: params.children,
            infants: params.infants,
            travelClass: params.travelClass,
            currencyCode: 'USD',
            max: 20
        });

        return response.data.map(offer => ({
            id: offer.id,
            price: {
                total: offer.price.total,
                currency: offer.price.currency
            },
            itineraries: offer.itineraries.map(itinerary => ({
                duration: itinerary.duration,
                segments: itinerary.segments.map(segment => ({
                    departure: {
                        airport: segment.departure.iataCode,
                        time: segment.departure.at
                    },
                    arrival: {
                        airport: segment.arrival.iataCode,
                        time: segment.arrival.at
                    },
                    carrier: segment.carrierCode,
                    number: segment.number
                }))
            }))
        }));
    } catch (error) {
        console.error('Error searching flights:', error);
        throw error;
    }
}

module.exports = {
    searchFlights
}; 
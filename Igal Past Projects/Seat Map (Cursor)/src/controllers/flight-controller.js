const { searchFlights } = require('../services/flight-search');

/**
 * Handle flight search request
 * @param {Object} req - Express request object
 * @param {Object} res - Express response object
 */
async function searchFlightsHandler(req, res) {
    try {
        const {
            originLocationCode,
            destinationLocationCode,
            departureDate,
            returnDate,
            adults,
            children,
            infants,
            travelClass
        } = req.query;

        // Validate required parameters
        if (!originLocationCode || !destinationLocationCode || !departureDate || !adults) {
            return res.status(400).json({
                error: 'Missing required parameters',
                required: ['originLocationCode', 'destinationLocationCode', 'departureDate', 'adults']
            });
        }

        // Validate date format and if it's in the future
        const departureDateObj = new Date(departureDate + 'T00:00:00');
        const now = new Date();
        now.setHours(0, 0, 0, 0); // Reset time to start of day for comparison

        console.log('Date validation:', {
            departureDate,
            departureDateObj: departureDateObj.toISOString(),
            now: now.toISOString(),
            isValid: !isNaN(departureDateObj.getTime()),
            isFuture: departureDateObj > now
        });

        if (isNaN(departureDateObj.getTime())) {
            return res.status(400).json({
                error: 'Invalid departure date format. Use YYYY-MM-DD'
            });
        }

        if (departureDateObj <= now) {
            return res.status(400).json({
                error: 'Departure date must be in the future',
                details: {
                    providedDate: departureDateObj.toISOString(),
                    currentDate: now.toISOString()
                }
            });
        }

        // Validate return date if provided
        if (returnDate) {
            const returnDateObj = new Date(returnDate + 'T00:00:00');
            if (isNaN(returnDateObj.getTime())) {
                return res.status(400).json({
                    error: 'Invalid return date format. Use YYYY-MM-DD'
                });
            }

            if (returnDateObj <= departureDateObj) {
                return res.status(400).json({
                    error: 'Return date must be after departure date',
                    details: {
                        departureDate: departureDateObj.toISOString(),
                        returnDate: returnDateObj.toISOString()
                    }
                });
            }
        }

        const flights = await searchFlights({
            originLocationCode,
            destinationLocationCode,
            departureDate,
            returnDate,
            adults: parseInt(adults),
            children: children ? parseInt(children) : undefined,
            infants: infants ? parseInt(infants) : undefined,
            travelClass
        });

        res.json(flights);
    } catch (error) {
        console.error('Error in flight search handler:', error);
        
        // Handle Amadeus API specific errors
        if (error.response && error.response.result && error.response.result.errors) {
            const amadeusError = error.response.result.errors[0];
            return res.status(error.response.statusCode).json({
                error: amadeusError.title,
                detail: amadeusError.detail
            });
        }

        res.status(500).json({
            error: 'Failed to search flights',
            message: error.message
        });
    }
}

module.exports = {
    searchFlightsHandler
}; 
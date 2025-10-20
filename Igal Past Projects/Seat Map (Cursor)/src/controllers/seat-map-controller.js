const { getSeatMap } = require('../services/seat-map');

/**
 * Handle seat map request
 * @param {Object} req - Express request object
 * @param {Object} res - Express response object
 */
async function getSeatMapHandler(req, res) {
    try {
        console.log('\n=== SEAT MAP REQUEST START ===');
        console.log('Request body:', JSON.stringify(req.body, null, 2));

        const { flightOffer } = req.body;
        if (!flightOffer) {
            console.error('ERROR: No flight offer provided in request');
            return res.status(400).json({ 
                error: 'Flight offer is required',
                details: 'The request body must include a flightOffer object'
            });
        }

        // Validate required fields
        if (!flightOffer.id) {
            console.error('ERROR: Flight offer is missing required id field');
            return res.status(400).json({ 
                error: 'Invalid flight offer',
                details: 'The flight offer must include an id field'
            });
        }

        if (!flightOffer.itineraries || !Array.isArray(flightOffer.itineraries)) {
            console.error('ERROR: Flight offer is missing required itineraries array');
            return res.status(400).json({ 
                error: 'Invalid flight offer',
                details: 'The flight offer must include an itineraries array'
            });
        }

        console.log('Getting seat map for flight offer:', flightOffer.id);
        const seatMapData = await getSeatMap(flightOffer);

        if (!seatMapData) {
            console.error('ERROR: No seat map data received from service');
            return res.status(404).json({ 
                error: 'No seat map data available',
                details: 'The service could not find seat map data for the provided flight offer'
            });
        }

        // Validate the basic structure of the data
        if (!seatMapData.decks || !Array.isArray(seatMapData.decks)) {
            console.error('ERROR: Invalid seat map data structure:', JSON.stringify(seatMapData, null, 2));
            return res.status(500).json({ 
                error: 'Invalid seat map data structure',
                details: 'The seat map data does not contain the required decks array'
            });
        }

        console.log('Seat map data retrieved successfully');
        console.log('=== SEAT MAP REQUEST END ===\n');
        res.json(seatMapData);
    } catch (error) {
        console.error('\n=== SEAT MAP HANDLER ERROR ===');
        console.error('Error type:', error.constructor.name);
        console.error('Error message:', error.message);
        console.error('Error stack:', error.stack);
        
        // Determine appropriate status code
        let statusCode = 500;
        let errorMessage = 'Internal server error';
        let errorDetails = error?.message || 'Unknown error occurred';

        if (error?.message && error.message.includes('Flight offer is required')) {
            statusCode = 400;
            errorMessage = 'Bad request';
        } else if (error?.message && error.message.includes('No seat map data available')) {
            statusCode = 404;
            errorMessage = 'Not found';
        } else if (error?.message && error.message.includes('Invalid response from Amadeus API')) {
            statusCode = 502;
            errorMessage = 'Bad gateway';
        }

        console.error('Sending error response:', {
            statusCode,
            errorMessage,
            errorDetails
        });
        console.error('=== END ERROR LOG ===\n');

        res.status(statusCode).json({
            error: errorMessage,
            details: errorDetails
        });
    }
}

module.exports = {
    getSeatMapHandler
}; 
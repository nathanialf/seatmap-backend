const express = require('express');
const { searchFlightsHandler } = require('../controllers/flight-controller');

const router = express.Router();

// Flight search endpoint
router.get('/search', searchFlightsHandler);

module.exports = router; 
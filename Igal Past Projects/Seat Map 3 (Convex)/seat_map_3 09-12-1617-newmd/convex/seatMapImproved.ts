"use node";

import { query, mutation, action } from "./_generated/server";
import { v } from "convex/values";
import { api } from "./_generated/api";

// Enhanced action to test Amadeus credentials and APIs
export const testAmadeusCredentials = action({
  args: {},
  handler: async (ctx, args) => {
    try {
      console.log('Testing Amadeus credentials...');
      
      const hasClientId = !!process.env.AMADEUS_CLIENT_ID;
      const hasClientSecret = !!process.env.AMADEUS_CLIENT_SECRET;
      
      console.log('Credentials check:', {
        hasClientId,
        hasClientSecret,
        clientIdLength: process.env.AMADEUS_CLIENT_ID?.length || 0,
        clientSecretLength: process.env.AMADEUS_CLIENT_SECRET?.length || 0
      });
      
      if (!hasClientId || !hasClientSecret) {
        return {
          success: false,
          error: 'Missing credentials',
          details: { hasClientId, hasClientSecret }
        };
      }
      
      const Amadeus = require('amadeus');
      const amadeus = new Amadeus({
        clientId: process.env.AMADEUS_CLIENT_ID,
        clientSecret: process.env.AMADEUS_CLIENT_SECRET,
        hostname: 'production'
      });
      
      // Test with a simple API call
      const response = await amadeus.referenceData.locations.get({
        keyword: 'MAD',
        subType: 'AIRPORT'
      });
      
      return {
        success: true,
        message: 'Credentials working',
        testResult: response.data?.length || 0
      };
      
    } catch (error: any) {
      console.error('Credential test error:', error);
      return {
        success: false,
        error: error.message,
        details: error.response?.result || error.response?.data
      };
    }
  }
});

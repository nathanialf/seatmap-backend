import { httpRouter } from "convex/server";
import { httpAction } from "./_generated/server";
import { api } from "./_generated/api";

const http = httpRouter();

// Amadeus webhook endpoint for seat map updates
http.route({
  path: "/amadeus/seatmap-webhook",
  method: "POST",
  handler: httpAction(async (ctx, request) => {
    try {
      const body = await request.json();
      console.log("Received Amadeus seat map webhook:", body);
      
      // Process webhook data here if needed
      // You can call mutations to update cached seat map data
      
      return new Response(JSON.stringify({ success: true }), {
        status: 200,
        headers: { "Content-Type": "application/json" }
      });
    } catch (error) {
      console.error("Amadeus webhook error:", error);
      return new Response(JSON.stringify({ error: "Webhook processing failed" }), {
        status: 500,
        headers: { "Content-Type": "application/json" }
      });
    }
  }),
});

// Health check endpoint
http.route({
  path: "/health",
  method: "GET",
  handler: httpAction(async (ctx, request) => {
    return new Response(JSON.stringify({ 
      status: "healthy", 
      timestamp: new Date().toISOString(),
      amadeus: "integrated"
    }), {
      status: 200,
      headers: { "Content-Type": "application/json" }
    });
  }),
});

export default http;

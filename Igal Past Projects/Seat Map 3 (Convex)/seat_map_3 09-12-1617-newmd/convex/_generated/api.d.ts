/* eslint-disable */
/**
 * Generated `api` utility.
 *
 * THIS CODE IS AUTOMATICALLY GENERATED.
 *
 * To regenerate, run `npx convex dev`.
 * @module
 */

import type {
  ApiFromModules,
  FilterApi,
  FunctionReference,
} from "convex/server";
import type * as amadeus from "../amadeus.js";
import type * as amadeusRealSeatMap from "../amadeusRealSeatMap.js";
import type * as auth from "../auth.js";
import type * as cache from "../cache.js";
import type * as crons from "../crons.js";
import type * as flights from "../flights.js";
import type * as http from "../http.js";
import type * as router from "../router.js";
import type * as seatMap from "../seatMap.js";
import type * as seatMapImproved from "../seatMapImproved.js";

/**
 * A utility for referencing Convex functions in your app's API.
 *
 * Usage:
 * ```js
 * const myFunctionReference = api.myModule.myFunction;
 * ```
 */
declare const fullApi: ApiFromModules<{
  amadeus: typeof amadeus;
  amadeusRealSeatMap: typeof amadeusRealSeatMap;
  auth: typeof auth;
  cache: typeof cache;
  crons: typeof crons;
  flights: typeof flights;
  http: typeof http;
  router: typeof router;
  seatMap: typeof seatMap;
  seatMapImproved: typeof seatMapImproved;
}>;
export declare const api: FilterApi<
  typeof fullApi,
  FunctionReference<any, "public">
>;
export declare const internal: FilterApi<
  typeof fullApi,
  FunctionReference<any, "internal">
>;

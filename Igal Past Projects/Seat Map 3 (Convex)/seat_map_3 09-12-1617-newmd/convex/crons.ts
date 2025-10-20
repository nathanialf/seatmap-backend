import { cronJobs } from "convex/server";

const crons = cronJobs();

// Cache cleanup will be implemented later
// crons.interval("cleanup cache", { hours: 1 }, internal.crons.cleanupCacheAction, {});

export default crons;

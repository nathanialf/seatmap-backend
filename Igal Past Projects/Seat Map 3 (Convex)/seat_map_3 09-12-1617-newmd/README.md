# SeatMap Pro

A modern flight booking application with real-time seat selection powered by Amadeus API.

## Features

- 🔍 **Flight Search**: Search flights using Amadeus API
- 🗺️ **Interactive Seat Maps**: Real-time seat availability and selection
- 👤 **User Authentication**: Secure login with Convex Auth
- 💺 **Seat Types**: Window, aisle, middle seat detection
- 🎯 **Exit Row Detection**: Special seat identification
- 📱 **Responsive Design**: Works on desktop and mobile

## Tech Stack

- **Frontend**: React + TypeScript + Vite
- **Backend**: Convex (real-time database)
- **Styling**: TailwindCSS
- **API**: Amadeus Travel API
- **Authentication**: Convex Auth

## Getting Started

### Prerequisites

- Node.js 18+
- Convex account
- Amadeus API credentials

### Installation

1. Clone the repository:
```bash
git clone https://github.com/igaldaniels/Seat_Map_3.git
cd Seat_Map_3
```

2. Install dependencies:
```bash
npm install
```

3. Set up Convex:
```bash
npx convex dev
```

4. Configure environment variables in Convex dashboard:
   - `AMADEUS_API_KEY`
   - `AMADEUS_API_SECRET`

5. Start development server:
```bash
npm run dev
```

## Deployment

The app is deployed on Convex. Current deployment: `flexible-firefly-91`

Dashboard: https://dashboard.convex.dev/d/flexible-firefly-91

## Environment Variables

Set these in your Convex dashboard under Settings → Environment Variables:

- `AMADEUS_API_KEY` - Your Amadeus API key
- `AMADEUS_API_SECRET` - Your Amadeus API secret

## Project Structure

```
├── convex/              # Backend functions and schema
│   ├── schema.ts        # Database schema
│   ├── amadeus.ts       # Amadeus API integration
│   ├── flights.ts       # Flight search functions
│   └── seatMap.ts       # Seat map functions
├── src/                 # Frontend React app
│   ├── components/      # React components
│   ├── App.tsx          # Main app component
│   └── main.tsx         # App entry point
└── package.json         # Dependencies and scripts
```

## Backup Information

- **Repository**: https://github.com/igaldaniels/Seat_Map_3.git
- **Convex Deployment**: flexible-firefly-91
- **Dashboard**: https://dashboard.convex.dev/d/flexible-firefly-91

## Contributing

1. Fork the repository
2. Create a feature branch: `git checkout -b feature-name`
3. Commit changes: `git commit -m 'Add feature'`
4. Push to branch: `git push origin feature-name`
5. Submit a pull request

## License

This project is private and proprietary.

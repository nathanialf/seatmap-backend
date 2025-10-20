# Flight Search and Seat Selection Application

A modern web application for searching flights and selecting seats, built with Node.js, Express, and the Amadeus API.

## Prerequisites

- Node.js (v14 or higher)
- npm (v6 or higher)
- Amadeus API credentials (Client ID and Client Secret)

## Environment Variables

Create a `.env` file in the root directory with the following variables:

```env
# Amadeus API credentials
AMADEUS_CLIENT_ID=your_client_id_here
AMADEUS_CLIENT_SECRET=your_client_secret_here

# Server configuration
PORT=3000
NODE_ENV=development
```

## Installation

1. Clone the repository:
```bash
git clone <repository-url>
cd <repository-name>
```

2. Install dependencies:
```bash
npm install
```

3. Create a `.env` file with your Amadeus API credentials (see Environment Variables section above)

4. Start the development server:
```bash
npm run dev
```

The application will be available at `http://localhost:3000`

## Available Scripts

- `npm run dev`: Start the development server with hot reload
- `npm start`: Start the production server
- `npm test`: Run tests
- `npm run lint`: Run ESLint
- `npm run format`: Format code with Prettier

## Project Structure

```
.
├── public/             # Static files
│   ├── css/           # Stylesheets
│   ├── js/            # Client-side JavaScript
│   └── index.html     # Main HTML file
├── src/               # Server-side code
│   ├── services/      # API services
│   └── app.js         # Express application
├── tests/             # Test files
├── .env.example       # Environment variables template
├── package.json       # Project configuration
└── README.md         # Project documentation
```

## Features

- Flight search with airport autocomplete
- Interactive seat selection
- Real-time validation
- Responsive design
- Error handling
- Loading states

## Development Workflow

1. Create a new branch for your feature
2. Write tests first (TDD)
3. Implement the feature
4. Run tests and linting
5. Create a pull request

## Contributing

1. Fork the repository
2. Create your feature branch
3. Commit your changes
4. Push to the branch
5. Create a pull request

## License

This project is licensed under the MIT License - see the LICENSE file for details.

---

> See the `seat_map_2_readme.md` file for a more detailed development guide and project phases.
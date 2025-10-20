# Seat Map 2 - Flight Search Web Application

## Project Description

Seat Map 2 is a modern web application designed for searching flights using a clean, responsive interface. The project combines Bootstrap 5 for the frontend with Node.js and Express for the backend, integrating with the Amadeus Self-Service API to provide real-time flight search capabilities.

This application allows users to search for flights by specifying origin and destination airports, departure dates, and passenger count, delivering comprehensive flight results in an intuitive format.

## Table of Contents

- [Technologies Used](#technologies-used)
- [Development Environment Prerequisites](#development-environment-prerequisites)
- [Development Plan - Project Phases](#development-plan---project-phases)
  - [Phase A: Project Initialization](#phase-a-project-initialization)
  - [Phase B: Environment Configuration](#phase-b-environment-configuration)
  - [Phase C: Frontend Development](#phase-c-frontend-development)
  - [Phase D: Backend Development](#phase-d-backend-development)
  - [Phase E: Frontend-Backend Integration](#phase-e-frontend-backend-integration)
  - [Phase F: Version Control with Git](#phase-f-version-control-with-git)
  - [Phase G: Local Testing](#phase-g-local-testing)
  - [Phase H: UI/UX Improvements (Optional)](#phase-h-uiux-improvements-optional)
- [How to Run the Project](#how-to-run-the-project)
- [Recommended Best Practices](#recommended-best-practices)
- [Credits](#credits)
- [License](#license)

## Technologies Used

### Frontend
- **HTML5** - Semantic markup structure
- **CSS3** - Custom styling and responsive design
- **JavaScript (ES6+)** - Client-side functionality and API communication
- **Bootstrap 5** - CSS framework for responsive design and components

### Backend
- **Node.js** - JavaScript runtime environment
- **Express.js** - Web application framework
- **dotenv** - Environment variable management
- **axios** - HTTP client for API requests

### Development Tools
- **Git** - Version control system
- **npm** - Package manager
- **Amadeus Self-Service API** - Flight data provider

## Development Environment Prerequisites

Before starting development, ensure you have the following installed and configured:

### Required Software
- [ ] **Node.js** (version 16.x or higher recommended)
- [ ] **npm** (comes with Node.js installation)
- [ ] **Git** (latest stable version)
- [ ] Code editor (VS Code, Sublime Text, or similar)
- [ ] Modern web browser for testing

### Required Accounts and Credentials
- [ ] **Amadeus Developer Account** - Register at [developers.amadeus.com](https://developers.amadeus.com)
- [ ] **API Credentials** - Obtain the following from your Amadeus dashboard:
  - Client ID: ``
  - Client Secret: ``

### Verification Steps
- [ ] Verify Node.js installation: Run `node --version` in terminal
- [ ] Verify npm installation: Run `npm --version` in terminal
- [ ] Verify Git installation: Run `git --version` in terminal
- [ ] Test Amadeus API access with provided credentials

## Development Plan - Project Phases

### Phase A: Project Initialization

#### A.1 Project Structure Setup
- [ ] Create main project directory named `seat-map-2`
- [ ] Navigate to project directory in terminal
- [ ] Initialize Git repository with `git init`
- [ ] Create initial project structure:
  - [ ] `src/` directory for source code
  - [ ] `public/` directory for static assets
  - [ ] `views/` directory for HTML templates
  - [ ] `routes/` directory for Express routes

#### A.2 Essential Configuration Files
- [ ] Create `.gitignore` file with following exclusions:
  - [ ] `node_modules/`
  - [ ] `.env`
  - [ ] `*.log`
  - [ ] `.DS_Store`
- [ ] Create `package.json` using `npm init`
- [ ] Create initial `README.md` (this document)
- [ ] Make first commit: "Initial project setup"

#### A.3 Project Documentation
- [ ] Document project purpose and scope
- [ ] Create development workflow documentation
- [ ] Establish coding standards and conventions

### Phase B: Environment Configuration

#### B.1 Dependencies Installation
- [ ] Install Express.js: `npm install express`
- [ ] Install dotenv for environment variables: `npm install dotenv`
- [ ] Install axios for HTTP requests: `npm install axios`
- [ ] Install development dependencies:
  - [ ] `npm install --save-dev nodemon` (for development server)

#### B.2 Environment Variables Setup
- [ ] Create `.env` file in project root
- [ ] Add Amadeus API credentials to `.env`:
  - [ ] `AMADEUS_CLIENT_ID=
  - [ ] `AMADEUS_CLIENT_SECRET=
  - [ ] `PORT=3000`
- [ ] Verify `.env` is listed in `.gitignore`

#### B.3 Basic Server Configuration
- [ ] Create main server file (`app.js` or `server.js`)
- [ ] Configure Express server basics
- [ ] Set up middleware for static files and JSON parsing
- [ ] Configure environment variable loading
- [ ] Test basic server startup

### Phase C: Frontend Development

#### C.1 HTML Structure Development
- [ ] Create main HTML file (`views/index.html`)
- [ ] Implement semantic HTML5 structure
- [ ] Include Bootstrap 5 CDN links
- [ ] Set up responsive viewport meta tag
- [ ] Create basic page layout with header, main, and footer

#### C.2 Flight Search Form Implementation
- [ ] Design search form with Bootstrap 5 components
- [ ] Implement form fields:
  - [ ] **Origin Airport** - Text input with validation
  - [ ] **destination Airport** - Text input with validation
  - [ ] **Departure Date** - Date input with future date validation
  - [ ] **Number of Adults** - Number input with min/max constraints
- [ ] Add form validation using HTML5 and Bootstrap classes
- [ ] Implement responsive design for mobile devices

#### C.3 Results Display Area
- [ ] Create results container for flight listings
- [ ] Design flight card layout using Bootstrap components
- [ ] Implement loading states and empty states
- [ ] Add error message display areas
- [ ] Ensure responsive design across all devices

#### C.4 Client-Side JavaScript
- [ ] Create main JavaScript file for form handling
- [ ] Implement form submission with validation
- [ ] Add client-side data formatting
- [ ] Create functions for DOM manipulation
- [ ] Implement error handling for user interactions

### Phase D: Backend Development

#### D.1 Express Server Configuration
- [ ] Set up Express application with proper middleware
- [ ] Configure CORS if needed for development
- [ ] Set up static file serving for public assets
- [ ] Implement request logging for debugging
- [ ] Configure error handling middleware

#### D.2 Amadeus API Integration
- [ ] Create Amadeus API service module
- [ ] Implement authentication token management
- [ ] Create flight search function using Amadeus API
- [ ] Handle API rate limiting and error responses
- [ ] Implement response data formatting

#### D.3 API Routes Development
- [ ] Create POST route for flight search (`/api/search-flights`)
- [ ] Implement request validation middleware
- [ ] Add request data sanitization
- [ ] Create response formatting functions
- [ ] Implement comprehensive error handling

#### D.4 Data Processing Logic
- [ ] Create functions to process Amadeus API responses
- [ ] Implement data filtering and sorting options
- [ ] Add price formatting and currency handling
- [ ] Create duration calculation and formatting
- [ ] Implement airline and airport code resolution

### Phase E: Frontend-Backend Integration

#### E.1 AJAX Implementation
- [ ] Create JavaScript functions for API communication
- [ ] Implement form submission using fetch API
- [ ] Add loading indicators during API calls
- [ ] Handle success and error responses
- [ ] Implement result display updates

#### E.2 User Experience Enhancement
- [ ] Add form validation feedback
- [ ] Implement progressive enhancement
- [ ] Create smooth transitions between states
- [ ] Add keyboard navigation support
- [ ] Ensure accessibility compliance

#### E.3 Error Handling Integration
- [ ] Implement client-side error display
- [ ] Create user-friendly error messages
- [ ] Add retry mechanisms for failed requests
- [ ] Handle network connectivity issues
- [ ] Implement graceful degradation

### Phase F: Version Control with Git

#### F.1 Repository Management
- [ ] Create meaningful commit messages following conventional commits
- [ ] Set up branching strategy:
  - [ ] `main` - stable production branch
  - [ ] `develop` - integration branch
  - [ ] `feature/*` - feature development branches
- [ ] Create initial development branch
- [ ] Implement regular commit schedule

#### F.2 Remote Repository Setup
- [ ] Create GitHub or GitLab repository
- [ ] Add remote origin to local repository
- [ ] Push initial codebase to remote
- [ ] Set up branch protection rules
- [ ] Create comprehensive repository documentation

#### F.3 Collaboration Workflow
- [ ] Document Git workflow for team members
- [ ] Set up pull request templates
- [ ] Implement code review process
- [ ] Create deployment workflow documentation

### Phase G: Local Testing

#### G.1 Development Server Testing
- [ ] Start development server using `npm run dev`
- [ ] Verify server starts on `http://localhost:3000`
- [ ] Test all form fields for proper validation
- [ ] Verify API integration with valid search queries
- [ ] Test error handling with invalid inputs

#### G.2 Functionality Testing
- [ ] Test various airport code combinations
- [ ] Verify date validation (past dates, future dates)
- [ ] Test passenger count limits and validation
- [ ] Confirm API responses are properly formatted
- [ ] Test error scenarios (no results, API errors)

#### G.3 Cross-Browser Testing
- [ ] Test in Chrome, Firefox, Safari, and Edge
- [ ] Verify responsive design on different screen sizes
- [ ] Test form submission in all browsers
- [ ] Confirm Bootstrap components render correctly
- [ ] Validate accessibility features

### Phase H: UI/UX Improvements (Optional)

#### H.1 Enhanced User Feedback
- [ ] Add loading spinners during API calls
- [ ] Implement progress indicators for multi-step processes
- [ ] Create informative error messages with helpful suggestions
- [ ] Add success confirmations for completed actions

#### H.2 Visual Enhancements
- [ ] Create custom CSS for brand-specific styling
- [ ] Add airline logos and airport imagery
- [ ] Implement custom color scheme and typography
- [ ] Add subtle animations and transitions

#### H.3 Advanced Features
- [ ] Implement flight result sorting options
- [ ] Add price range filtering
- [ ] Create favorite airports functionality
- [ ] Add recent searches history
- [ ] Implement keyboard shortcuts for power users

## How to Run the Project

### Development Mode
1. Clone the repository to your local machine
2. Navigate to the project directory
3. Install dependencies: `npm install`
4. Create `.env` file with required environment variables
5. Start development server: `npm run dev`
6. Open browser and navigate to `http://localhost:3000`

### Production Mode
1. Ensure all dependencies are installed: `npm install --production`
2. Set production environment variables
3. Start production server: `npm start`
4. Monitor application logs for any issues

### Recommended Development Tools
- **nodemon** - Automatically restart server on file changes
- **Browser Developer Tools** - Debug client-side JavaScript
- **Postman** - Test API endpoints independently
- **Git GUI** - Visual interface for Git operations

## Recommended Best Practices

### Security Considerations
- [ ] Never commit `.env` file to version control
- [ ] Use environment variables for all sensitive data
- [ ] Implement input validation on both client and server
- [ ] Add rate limiting to prevent API abuse
- [ ] Keep dependencies updated to latest secure versions

### Code Quality Standards
- [ ] Use consistent indentation and formatting
- [ ] Write descriptive variable and function names
- [ ] Add comments for complex logic
- [ ] Implement error handling at all levels
- [ ] Follow JavaScript ES6+ best practices

### Development Workflow
- [ ] Make frequent, small commits with clear messages
- [ ] Test functionality before committing changes
- [ ] Use feature branches for new development
- [ ] Document all API endpoints and functions
- [ ] Maintain updated project documentation

### Performance Optimization
- [ ] Minimize API calls by caching responses
- [ ] Optimize images and static assets
- [ ] Implement client-side caching where appropriate
- [ ] Use compression for production deployment
- [ ] Monitor application performance metrics

## Credits

This project is based on the official Amadeus Developer tutorials:

- **Part 1 (Frontend - Bootstrap 5)**: [Bootstrap Flight Search Form Tutorial](https://developers.amadeus.com/blog/bootstrap-flight-search-form-part-1)
- **Part 2 (Backend - Node.js + Express)**: [Backend Integration Tutorial](https://developers.amadeus.com/blog/bootstrap-flight-search-form-part-2)

Special thanks to the Amadeus Developer Relations team for providing comprehensive documentation and example implementations.

Additional resources and inspiration:
- Bootstrap 5 official documentation
- Express.js documentation and best practices
- Node.js community guidelines

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

The MIT License allows for free use, modification, and distribution of this codebase while maintaining attribution to the original authors.

---

**Note**: This README serves as both project documentation and development guide. Update this document as the project evolves and new features are added.
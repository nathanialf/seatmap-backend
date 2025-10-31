# SeatMap Backend API Documentation

**Version**: 1.0.0  
**Last Updated**: October 30, 2025

## 📚 Documentation Structure

This documentation is organized into focused sections for easy navigation and maintenance:

### 🔗 Quick Links

| Document | Description |
|----------|-------------|
| **[API Overview](./api/overview.md)** | Authentication, base URLs, and general concepts |
| **[Authentication API](./api/authentication.md)** | User registration, login, guest tokens |
| **[Flight Search API](./api/flight-search.md)** | Flight offer search functionality |
| **[Seat Map API](./api/seat-maps.md)** | Seat map retrieval and bookmark seat maps 🆕 |
| **[Bookmarks API](./api/bookmarks.md)** | Bookmark management for saved flights |
| **[User Profile API](./api/user-profile.md)** | User profile management |
| **[Error Handling](./api/error-handling.md)** | Standard error responses and codes |
| **[Testing Guide](./api/testing-guide.md)** | Complete testing workflows and examples |

### 🚀 Quick Start

1. **Get API Access**: Obtain your API credentials from the administrator
2. **Choose Authentication**: Use guest tokens for testing or register for tiered access  
3. **Test the API**: Follow the [Testing Guide](./api/testing-guide.md) for step-by-step examples
4. **Integration**: Reference specific API docs for your use case

### 🌐 Environment Configuration

Contact administrator for environment-specific configuration details.

### 🆕 Latest Updates

**v1.0.0 - October 30, 2025**:
- ✅ Added new `GET /seat-map/bookmark/{bookmarkId}` endpoint
- ✅ Simplified bookmark-to-seatmap workflow  
- ✅ Updated guest access limits to 30 days
- ✅ Enhanced tiered access system for registered users
- ✅ Complete API documentation with cURL examples
- ✅ Comprehensive testing guide for all endpoints
- ✅ Improved error handling and documentation

### 📋 API Summary

| Endpoint Category | Methods | Authentication Required | Rate Limits |
|------------------|---------|------------------------|-------------|
| **Authentication** | POST | API Key | None |
| **Flight Search** | POST | API Key + JWT Token | None |
| **Seat Maps** | POST, GET | API Key + JWT Token | Varies by tier |
| **Bookmarks** | GET, POST, DELETE | API Key + User JWT | Varies by tier |
| **User Profile** | GET, PUT | API Key + User JWT | None |

### 🔍 Key Features

- **Multi-provider Integration**: Amadeus and Sabre flight data
- **Guest Access**: Limited trial access without registration
- **Tiered User Access**: Multiple access levels for registered users
- **Bookmark System**: Save and retrieve flights easily
- **Direct Bookmark Seat Maps**: New simplified workflow 🆕
- **Comprehensive Error Handling**: Consistent, actionable error responses

---

For detailed API specifications, start with the [API Overview](./api/overview.md) or jump directly to the endpoint documentation you need.
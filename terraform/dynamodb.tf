# DynamoDB Tables for Seatmap Backend
# Based on system-architecture.md specifications

# Users Table
resource "aws_dynamodb_table" "users" {
  name           = "${var.project_name}-users-${var.environment}"
  billing_mode   = var.dynamodb_billing_mode
  hash_key       = "userId"

  attribute {
    name = "userId"
    type = "S"
  }

  attribute {
    name = "email"
    type = "S"
  }

  attribute {
    name = "oauthId"
    type = "S"
  }

  # GSI for email lookup
  global_secondary_index {
    name     = "email-index"
    hash_key = "email"
  }

  # GSI for OAuth ID lookup (Google/Apple users)
  global_secondary_index {
    name     = "oauth-id-index"
    hash_key = "oauthId"
  }

  tags = {
    Name        = "Users Table"
    Description = "Store user account information"
  }
}

# Sessions Table
resource "aws_dynamodb_table" "sessions" {
  name           = "${var.project_name}-sessions-${var.environment}"
  billing_mode   = var.dynamodb_billing_mode
  hash_key       = "sessionId"
  range_key      = "userId"

  attribute {
    name = "sessionId"
    type = "S"
  }

  attribute {
    name = "userId"
    type = "S"
  }

  # TTL for automatic session expiration (24 hours)
  ttl {
    attribute_name = "expiresAt"
    enabled        = true
  }

  tags = {
    Name        = "Sessions Table"
    Description = "Store user and guest sessions with TTL"
  }
}

# Bookmarks Table
resource "aws_dynamodb_table" "bookmarks" {
  name           = "${var.project_name}-bookmarks-${var.environment}"
  billing_mode   = var.dynamodb_billing_mode
  hash_key       = "userId"
  range_key      = "bookmarkId"

  attribute {
    name = "userId"
    type = "S"
  }

  attribute {
    name = "bookmarkId"
    type = "S"
  }

  # TTL for automatic bookmark expiration (departureDate + 1 day)
  ttl {
    attribute_name = "expiresAt"
    enabled        = true
  }

  tags = {
    Name        = "Bookmarks Table"
    Description = "Store user flight bookmarks with TTL, max 50 per user"
  }
}

# API Cache Table
resource "aws_dynamodb_table" "api_cache" {
  name           = "${var.project_name}-api-cache-${var.environment}"
  billing_mode   = var.dynamodb_billing_mode
  hash_key       = "cacheKey"

  attribute {
    name = "cacheKey"
    type = "S"
  }

  # TTL for cache expiration (15 minutes for flight search, 5 minutes for seat maps)
  ttl {
    attribute_name = "expiresAt"
    enabled        = true
  }

  tags = {
    Name        = "API Cache Table"
    Description = "Cache external API responses with TTL"
  }
}

# Subscriptions Table
resource "aws_dynamodb_table" "subscriptions" {
  name           = "${var.project_name}-subscriptions-${var.environment}"
  billing_mode   = var.dynamodb_billing_mode
  hash_key       = "userId"

  attribute {
    name = "userId"
    type = "S"
  }

  attribute {
    name = "stripeCustomerId"
    type = "S"
  }

  # GSI for Stripe customer lookup
  global_secondary_index {
    name     = "stripe-customer-index"
    hash_key = "stripeCustomerId"
  }

  tags = {
    Name        = "Subscriptions Table"
    Description = "Store user subscription data with Stripe integration"
  }
}
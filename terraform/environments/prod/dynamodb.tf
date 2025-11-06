# DynamoDB Tables
resource "aws_dynamodb_table" "users" {
  name           = "seatmap-users-${local.environment}"
  billing_mode   = "PAY_PER_REQUEST"
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

  attribute {
    name = "verificationToken"
    type = "S"
  }

  # GSI for email lookup
  global_secondary_index {
    name            = "email-index"
    hash_key        = "email"
    projection_type = "ALL"
  }

  # GSI for OAuth ID lookup (Google/Apple users)
  global_secondary_index {
    name            = "oauth-id-index"
    hash_key        = "oauthId"
    projection_type = "ALL"
  }

  # GSI for email verification token lookup
  global_secondary_index {
    name            = "verification-token-index"
    hash_key        = "verificationToken"
    projection_type = "ALL"
  }

  tags = merge(local.common_tags, {
    Name        = "Users Table"
    Description = "Store user account information"
  })
}

resource "aws_dynamodb_table" "sessions" {
  name           = "seatmap-sessions-${local.environment}"
  billing_mode   = "PAY_PER_REQUEST"
  hash_key       = "sessionId"

  attribute {
    name = "sessionId"
    type = "S"
  }

  attribute {
    name = "userId"
    type = "S"
  }

  # GSI for user lookup
  global_secondary_index {
    name            = "user-index"
    hash_key        = "userId"
    projection_type = "ALL"
  }

  # TTL configuration
  ttl {
    attribute_name = "expiresAt"
    enabled        = true
  }

  tags = merge(local.common_tags, {
    Name        = "Sessions Table"
    Description = "Store user session tokens and guest session limits"
  })
}

resource "aws_dynamodb_table" "subscriptions" {
  name           = "seatmap-subscriptions-${local.environment}"
  billing_mode   = "PAY_PER_REQUEST"
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
    name            = "stripe-customer-index"
    hash_key        = "stripeCustomerId"
    projection_type = "ALL"
  }

  tags = merge(local.common_tags, {
    Name        = "Subscriptions Table"
    Description = "Store user subscription data with Stripe integration"
  })
}

resource "aws_dynamodb_table" "guest_access" {
  name           = "seatmap-guest-access-${local.environment}"
  billing_mode   = "PAY_PER_REQUEST"
  hash_key       = "ipAddress"

  attribute {
    name = "ipAddress"
    type = "S"
  }

  # TTL configuration for automatic cleanup after 6 months
  ttl {
    attribute_name = "expiresAt"
    enabled        = true
  }

  tags = merge(local.common_tags, {
    Name        = "Guest Access History Table"
    Description = "Store guest access history for IP-based rate limiting"
  })
}

# DynamoDB Table for Bookmarks
resource "aws_dynamodb_table" "bookmarks" {
  name           = "seatmap-bookmarks-${local.environment}"
  billing_mode   = "PAY_PER_REQUEST"
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

  tags = merge(local.common_tags, {
    Name        = "Bookmarks Table"
    Description = "Store user flight bookmarks with complete flight offer data"
  })
}

# Account Tiers Table
resource "aws_dynamodb_table" "account_tiers" {
  name           = "${local.project_name}-account-tiers-${local.environment}"
  billing_mode   = "PAY_PER_REQUEST"
  hash_key       = "tierId"

  attribute {
    name = "tierId"
    type = "S"
  }

  attribute {
    name = "tierName"
    type = "S"
  }

  attribute {
    name = "region"
    type = "S"
  }

  # GSI for tier name lookup
  global_secondary_index {
    name            = "tier-name-index"
    hash_key        = "tierName"
    projection_type = "ALL"
  }

  # GSI for region-based tier lookup
  global_secondary_index {
    name            = "region-index"
    hash_key        = "region"
    projection_type = "ALL"
  }

  tags = merge(local.common_tags, {
    Name        = "Account Tiers Table"
    Description = "Store tier definitions pricing and limits by region"
  })
}

# User Usage Table - Monthly usage tracking for tier-based limits
resource "aws_dynamodb_table" "user_usage" {
  name           = "${local.project_name}-user-usage-${local.environment}"
  billing_mode   = "PAY_PER_REQUEST"
  hash_key       = "userId"
  range_key      = "monthYear"

  attribute {
    name = "userId"
    type = "S"
  }

  attribute {
    name = "monthYear"
    type = "S"
  }

  # TTL configuration for automatic cleanup after 13 months
  ttl {
    attribute_name = "expiresAt"
    enabled        = true
  }

  tags = merge(local.common_tags, {
    Name        = "User Usage Table"
    Description = "Store monthly usage tracking for tier-based limits with TTL"
  })
}
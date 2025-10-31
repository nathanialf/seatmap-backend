terraform {
  required_version = ">= 1.0"
  
  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.0"
    }
  }
  
  backend "s3" {
    bucket = "seatmap-backend-terraform-state-prod"
    key    = "seatmap-backend/terraform.tfstate"
    region = "us-west-1"
    dynamodb_table = "seatmap-backend-terraform-locks-prod"
  }
}

# Configure AWS Provider
provider "aws" {
  region = var.aws_region
  
  default_tags {
    tags = {
      Project     = "seatmap-backend"
      Environment = "prod"
      ManagedBy   = "terraform"
    }
  }
}

# Data sources
data "aws_caller_identity" "current" {}
data "aws_region" "current" {}

# Local values for configuration
locals {
  project_name = "seatmap"
  environment  = "prod"
  
  # Lambda configuration
  lambda_jar_path = "../../../build/libs/SEATMAP-Backend-1.0.0.jar"
  
  common_tags = {
    Project     = local.project_name
    Environment = local.environment
    Region      = data.aws_region.current.name
    Account     = data.aws_caller_identity.current.account_id
  }
}

# Seat Map Lambda Function
resource "aws_lambda_function" "seat_map" {
  filename         = local.lambda_jar_path
  function_name    = "seatmap-seat-map-${local.environment}"
  role            = aws_iam_role.lambda_role.arn
  handler         = "com.seatmap.api.handler.SeatMapHandler::handleRequest"
  runtime         = "java17"
  memory_size     = 1024  # Higher memory for production
  timeout         = 30
  
  source_code_hash = filebase64sha256(local.lambda_jar_path)
  
  environment {
    variables = {
      ENVIRONMENT        = local.environment
      AMADEUS_ENDPOINT   = var.amadeus_endpoint
      AMADEUS_API_KEY    = var.amadeus_api_key
      AMADEUS_API_SECRET = var.amadeus_api_secret
      SABRE_USER_ID      = var.sabre_user_id
      SABRE_PASSWORD     = var.sabre_password
      SABRE_ENDPOINT     = var.sabre_endpoint
      JWT_SECRET         = var.jwt_secret
    }
  }

  tags = local.common_tags
}

# Auth Lambda Function
resource "aws_lambda_function" "auth" {
  filename         = local.lambda_jar_path
  function_name    = "seatmap-auth-${local.environment}"
  role            = aws_iam_role.lambda_role.arn
  handler         = "com.seatmap.auth.handler.AuthHandler::handleRequest"
  runtime         = "java17"
  memory_size     = 1024  # Higher memory for production
  timeout         = 30
  
  source_code_hash = filebase64sha256(local.lambda_jar_path)
  
  environment {
    variables = {
      ENVIRONMENT        = local.environment
      AMADEUS_ENDPOINT   = var.amadeus_endpoint
      AMADEUS_API_KEY    = var.amadeus_api_key
      AMADEUS_API_SECRET = var.amadeus_api_secret
      SABRE_USER_ID      = var.sabre_user_id
      SABRE_PASSWORD     = var.sabre_password
      SABRE_ENDPOINT     = var.sabre_endpoint
      JWT_SECRET         = var.jwt_secret
      BASE_URL          = "https://${aws_api_gateway_rest_api.seatmap_api.id}.execute-api.${data.aws_region.current.name}.amazonaws.com/${local.environment}"
    }
  }

  tags = local.common_tags
}

# Flight Offers Lambda Function
resource "aws_lambda_function" "flight_offers" {
  filename         = local.lambda_jar_path
  function_name    = "seatmap-flight-offers-${local.environment}"
  role            = aws_iam_role.lambda_role.arn
  handler         = "com.seatmap.api.handler.FlightOffersHandler::handleRequest"
  runtime         = "java17"
  memory_size     = 1024  # Higher memory for production
  timeout         = 30
  
  source_code_hash = filebase64sha256(local.lambda_jar_path)
  
  environment {
    variables = {
      ENVIRONMENT        = local.environment
      AMADEUS_ENDPOINT   = var.amadeus_endpoint
      AMADEUS_API_KEY    = var.amadeus_api_key
      AMADEUS_API_SECRET = var.amadeus_api_secret
      SABRE_USER_ID      = var.sabre_user_id
      SABRE_PASSWORD     = var.sabre_password
      SABRE_ENDPOINT     = var.sabre_endpoint
      JWT_SECRET         = var.jwt_secret
    }
  }

  tags = local.common_tags
}

# Lambda Function for Bookmarks
resource "aws_lambda_function" "bookmarks" {
  filename         = local.lambda_jar_path
  function_name    = "seatmap-bookmarks-${local.environment}"
  role            = aws_iam_role.lambda_role.arn
  handler         = "com.seatmap.auth.handler.BookmarkHandler::handleRequest"
  runtime         = "java17"
  memory_size     = 512
  timeout         = 30
  
  source_code_hash = filebase64sha256(local.lambda_jar_path)
  
  environment {
    variables = {
      ENVIRONMENT = local.environment
      JWT_SECRET  = var.jwt_secret
    }
  }

  tags = local.common_tags
}

# IAM Role for Lambda
resource "aws_iam_role" "lambda_role" {
  name = "seatmap-lambda-role-${local.environment}"

  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Action = "sts:AssumeRole"
        Effect = "Allow"
        Principal = {
          Service = "lambda.amazonaws.com"
        }
      }
    ]
  })

  tags = local.common_tags
}

# IAM Policy for Lambda
resource "aws_iam_role_policy" "lambda_policy" {
  name = "seatmap-lambda-policy-${local.environment}"
  role = aws_iam_role.lambda_role.id

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Effect = "Allow"
        Action = [
          "logs:CreateLogGroup",
          "logs:CreateLogStream",
          "logs:PutLogEvents"
        ]
        Resource = "arn:aws:logs:*:*:*"
      },
      {
        Effect = "Allow"
        Action = [
          "dynamodb:GetItem",
          "dynamodb:PutItem",
          "dynamodb:Query",
          "dynamodb:Scan",
          "dynamodb:UpdateItem",
          "dynamodb:DeleteItem",
          "ses:SendEmail",
          "ses:SendRawEmail"
        ]
        Resource = [
          aws_dynamodb_table.users.arn,
          "${aws_dynamodb_table.users.arn}/index/*",
          aws_dynamodb_table.sessions.arn,
          "${aws_dynamodb_table.sessions.arn}/index/*",
          aws_dynamodb_table.subscriptions.arn,
          "${aws_dynamodb_table.subscriptions.arn}/index/*",
          aws_dynamodb_table.guest_access.arn,
          "${aws_dynamodb_table.guest_access.arn}/index/*",
          aws_dynamodb_table.bookmarks.arn,
          "${aws_dynamodb_table.bookmarks.arn}/index/*",
          aws_dynamodb_table.account_tiers.arn,
          "${aws_dynamodb_table.account_tiers.arn}/index/*"
        ]
      }
    ]
  })
}

# DynamoDB Tables with higher capacity for production
resource "aws_dynamodb_table" "users" {
  name           = "seatmap-users-${local.environment}"
  billing_mode   = "PROVISIONED"
  read_capacity  = 20
  write_capacity = 20
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
    read_capacity   = 10
    write_capacity  = 10
    projection_type = "ALL"
  }

  # GSI for OAuth ID lookup (Google/Apple users)
  global_secondary_index {
    name            = "oauth-id-index"
    hash_key        = "oauthId"
    read_capacity   = 10
    write_capacity  = 10
    projection_type = "ALL"
  }

  # GSI for verification token lookup
  global_secondary_index {
    name            = "verification-token-index"
    hash_key        = "verificationToken"
    read_capacity   = 5
    write_capacity  = 5
    projection_type = "ALL"
  }

  tags = merge(local.common_tags, {
    Name        = "Users Table"
    Description = "Store user account information"
  })
}

resource "aws_dynamodb_table" "sessions" {
  name           = "seatmap-sessions-${local.environment}"
  billing_mode   = "PROVISIONED"
  read_capacity  = 20
  write_capacity = 20
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
    read_capacity   = 10
    write_capacity  = 10
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
  billing_mode   = "PROVISIONED"
  read_capacity  = 10
  write_capacity = 10
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
    read_capacity   = 5
    write_capacity  = 5
    projection_type = "ALL"
  }

  tags = merge(local.common_tags, {
    Name        = "Subscriptions Table"
    Description = "Store user subscription data with Stripe integration"
  })
}

resource "aws_dynamodb_table" "guest_access" {
  name           = "seatmap-guest-access-${local.environment}"
  billing_mode   = "PROVISIONED"
  read_capacity  = 10
  write_capacity = 10
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
  billing_mode   = "PROVISIONED"
  hash_key       = "userId"
  range_key      = "bookmarkId"
  read_capacity  = 5
  write_capacity = 5

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
  billing_mode   = "PROVISIONED"
  hash_key       = "tierId"
  read_capacity  = 5
  write_capacity = 5

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
    read_capacity   = 5
    write_capacity  = 5
  }

  # GSI for region-based tier lookup
  global_secondary_index {
    name            = "region-index"
    hash_key        = "region"
    projection_type = "ALL"
    read_capacity   = 5
    write_capacity  = 5
  }

  tags = merge(local.common_tags, {
    Name        = "Account Tiers Table"
    Description = "Store tier definitions pricing and limits by region"
  })
}

# API Gateway
resource "aws_api_gateway_rest_api" "seatmap_api" {
  name        = "seatmap-api-${local.environment}"
  description = "Seatmap Backend API - Production"

  tags = local.common_tags
}

# API Gateway Resource
resource "aws_api_gateway_resource" "seat_map" {
  rest_api_id = aws_api_gateway_rest_api.seatmap_api.id
  parent_id   = aws_api_gateway_rest_api.seatmap_api.root_resource_id
  path_part   = "seat-map"
}

# API Gateway Method (POST)
resource "aws_api_gateway_method" "seat_map_post" {
  rest_api_id   = aws_api_gateway_rest_api.seatmap_api.id
  resource_id   = aws_api_gateway_resource.seat_map.id
  http_method   = "POST"
  authorization = "NONE"
  api_key_required = true
}

# API Gateway Integration
resource "aws_api_gateway_integration" "seat_map_integration" {
  rest_api_id = aws_api_gateway_rest_api.seatmap_api.id
  resource_id = aws_api_gateway_resource.seat_map.id
  http_method = aws_api_gateway_method.seat_map_post.http_method

  integration_http_method = "POST"
  type                    = "AWS_PROXY"
  uri                     = aws_lambda_function.seat_map.invoke_arn
}

# Lambda Permission for API Gateway
resource "aws_lambda_permission" "api_gateway" {
  statement_id  = "AllowExecutionFromAPIGateway"
  action        = "lambda:InvokeFunction"
  function_name = aws_lambda_function.seat_map.function_name
  principal     = "apigateway.amazonaws.com"

  source_arn = "${aws_api_gateway_rest_api.seatmap_api.execution_arn}/*/*"
}

# API Gateway Method Response
resource "aws_api_gateway_method_response" "seat_map_response" {
  rest_api_id = aws_api_gateway_rest_api.seatmap_api.id
  resource_id = aws_api_gateway_resource.seat_map.id
  http_method = aws_api_gateway_method.seat_map_post.http_method
  status_code = "200"

  response_parameters = {
    "method.response.header.Access-Control-Allow-Origin"  = true
    "method.response.header.Access-Control-Allow-Methods" = true
    "method.response.header.Access-Control-Allow-Headers" = true
  }
}

# API Gateway Integration Response
resource "aws_api_gateway_integration_response" "seat_map_integration_response" {
  rest_api_id = aws_api_gateway_rest_api.seatmap_api.id
  resource_id = aws_api_gateway_resource.seat_map.id
  http_method = aws_api_gateway_method.seat_map_post.http_method
  status_code = aws_api_gateway_method_response.seat_map_response.status_code

  response_parameters = {
    "method.response.header.Access-Control-Allow-Origin"  = "'*'"
    "method.response.header.Access-Control-Allow-Methods" = "'GET,POST,PUT,DELETE,OPTIONS'"
    "method.response.header.Access-Control-Allow-Headers" = "'Content-Type,Authorization'"
  }

  depends_on = [aws_api_gateway_integration.seat_map_integration]
}

# API Gateway Resource for bookmark under seat-map
resource "aws_api_gateway_resource" "seat_map_bookmark" {
  rest_api_id = aws_api_gateway_rest_api.seatmap_api.id
  parent_id   = aws_api_gateway_resource.seat_map.id
  path_part   = "bookmark"
}

# API Gateway Resource for bookmarkId parameter under seat-map/bookmark
resource "aws_api_gateway_resource" "seat_map_bookmark_id" {
  rest_api_id = aws_api_gateway_rest_api.seatmap_api.id
  parent_id   = aws_api_gateway_resource.seat_map_bookmark.id
  path_part   = "{bookmarkId}"
}

# API Gateway Method for seat-map/bookmark/{bookmarkId} GET
resource "aws_api_gateway_method" "seat_map_bookmark_get" {
  rest_api_id   = aws_api_gateway_rest_api.seatmap_api.id
  resource_id   = aws_api_gateway_resource.seat_map_bookmark_id.id
  http_method   = "GET"
  authorization = "NONE"
  api_key_required = true
}

# API Gateway Integration for seat-map/bookmark/{bookmarkId}
resource "aws_api_gateway_integration" "seat_map_bookmark_integration" {
  rest_api_id = aws_api_gateway_rest_api.seatmap_api.id
  resource_id = aws_api_gateway_resource.seat_map_bookmark_id.id
  http_method = aws_api_gateway_method.seat_map_bookmark_get.http_method

  integration_http_method = "POST"
  type                    = "AWS_PROXY"
  uri                     = aws_lambda_function.seat_map.invoke_arn
}

# Auth API Gateway Resources
resource "aws_api_gateway_resource" "auth" {
  rest_api_id = aws_api_gateway_rest_api.seatmap_api.id
  parent_id   = aws_api_gateway_rest_api.seatmap_api.root_resource_id
  path_part   = "auth"
}

# Auth Guest Resource
resource "aws_api_gateway_resource" "auth_guest" {
  rest_api_id = aws_api_gateway_rest_api.seatmap_api.id
  parent_id   = aws_api_gateway_resource.auth.id
  path_part   = "guest"
}

# Auth Login Resource
resource "aws_api_gateway_resource" "auth_login" {
  rest_api_id = aws_api_gateway_rest_api.seatmap_api.id
  parent_id   = aws_api_gateway_resource.auth.id
  path_part   = "login"
}

# Auth Register Resource
resource "aws_api_gateway_resource" "auth_register" {
  rest_api_id = aws_api_gateway_rest_api.seatmap_api.id
  parent_id   = aws_api_gateway_resource.auth.id
  path_part   = "register"
}

# Auth Profile Resource
resource "aws_api_gateway_resource" "auth_profile" {
  rest_api_id = aws_api_gateway_rest_api.seatmap_api.id
  parent_id   = aws_api_gateway_resource.auth.id
  path_part   = "profile"
}

# Auth Guest Method (POST)
resource "aws_api_gateway_method" "auth_guest_post" {
  rest_api_id   = aws_api_gateway_rest_api.seatmap_api.id
  resource_id   = aws_api_gateway_resource.auth_guest.id
  http_method   = "POST"
  authorization = "NONE"
  api_key_required = true
}

# Auth Login Method (POST)
resource "aws_api_gateway_method" "auth_login_post" {
  rest_api_id   = aws_api_gateway_rest_api.seatmap_api.id
  resource_id   = aws_api_gateway_resource.auth_login.id
  http_method   = "POST"
  authorization = "NONE"
  api_key_required = true
}

# Auth Register Method (POST)
resource "aws_api_gateway_method" "auth_register_post" {
  rest_api_id   = aws_api_gateway_rest_api.seatmap_api.id
  resource_id   = aws_api_gateway_resource.auth_register.id
  http_method   = "POST"
  authorization = "NONE"
  api_key_required = true
}

# Auth Profile Method (GET)
resource "aws_api_gateway_method" "auth_profile_get" {
  rest_api_id   = aws_api_gateway_rest_api.seatmap_api.id
  resource_id   = aws_api_gateway_resource.auth_profile.id
  http_method   = "GET"
  authorization = "NONE"
  api_key_required = true
}

# Auth Profile Method (PUT)
resource "aws_api_gateway_method" "auth_profile_put" {
  rest_api_id   = aws_api_gateway_rest_api.seatmap_api.id
  resource_id   = aws_api_gateway_resource.auth_profile.id
  http_method   = "PUT"
  authorization = "NONE"
  api_key_required = true
}

# Auth Guest Integration
resource "aws_api_gateway_integration" "auth_guest_integration" {
  rest_api_id = aws_api_gateway_rest_api.seatmap_api.id
  resource_id = aws_api_gateway_resource.auth_guest.id
  http_method = aws_api_gateway_method.auth_guest_post.http_method

  integration_http_method = "POST"
  type                    = "AWS_PROXY"
  uri                     = aws_lambda_function.auth.invoke_arn
}

# Auth Login Integration
resource "aws_api_gateway_integration" "auth_login_integration" {
  rest_api_id = aws_api_gateway_rest_api.seatmap_api.id
  resource_id = aws_api_gateway_resource.auth_login.id
  http_method = aws_api_gateway_method.auth_login_post.http_method

  integration_http_method = "POST"
  type                    = "AWS_PROXY"
  uri                     = aws_lambda_function.auth.invoke_arn
}

# Auth Register Integration
resource "aws_api_gateway_integration" "auth_register_integration" {
  rest_api_id = aws_api_gateway_rest_api.seatmap_api.id
  resource_id = aws_api_gateway_resource.auth_register.id
  http_method = aws_api_gateway_method.auth_register_post.http_method

  integration_http_method = "POST"
  type                    = "AWS_PROXY"
  uri                     = aws_lambda_function.auth.invoke_arn
}

# Auth Profile GET Integration
resource "aws_api_gateway_integration" "auth_profile_get_integration" {
  rest_api_id = aws_api_gateway_rest_api.seatmap_api.id
  resource_id = aws_api_gateway_resource.auth_profile.id
  http_method = aws_api_gateway_method.auth_profile_get.http_method

  integration_http_method = "POST"
  type                    = "AWS_PROXY"
  uri                     = aws_lambda_function.auth.invoke_arn
}

# Auth Profile PUT Integration
resource "aws_api_gateway_integration" "auth_profile_put_integration" {
  rest_api_id = aws_api_gateway_rest_api.seatmap_api.id
  resource_id = aws_api_gateway_resource.auth_profile.id
  http_method = aws_api_gateway_method.auth_profile_put.http_method

  integration_http_method = "POST"
  type                    = "AWS_PROXY"
  uri                     = aws_lambda_function.auth.invoke_arn
}

# Lambda Permission for Auth API Gateway
resource "aws_lambda_permission" "auth_api_gateway" {
  statement_id  = "AllowExecutionFromAPIGateway"
  action        = "lambda:InvokeFunction"
  function_name = aws_lambda_function.auth.function_name
  principal     = "apigateway.amazonaws.com"

  source_arn = "${aws_api_gateway_rest_api.seatmap_api.execution_arn}/*/*"
}

# Flight Offers API Gateway Resource
resource "aws_api_gateway_resource" "flight_offers" {
  rest_api_id = aws_api_gateway_rest_api.seatmap_api.id
  parent_id   = aws_api_gateway_rest_api.seatmap_api.root_resource_id
  path_part   = "flight-offers"
}

# Flight Offers Method (POST)
resource "aws_api_gateway_method" "flight_offers_post" {
  rest_api_id   = aws_api_gateway_rest_api.seatmap_api.id
  resource_id   = aws_api_gateway_resource.flight_offers.id
  http_method   = "POST"
  authorization = "NONE"
  api_key_required = true
}

# Flight Offers Integration
resource "aws_api_gateway_integration" "flight_offers_integration" {
  rest_api_id = aws_api_gateway_rest_api.seatmap_api.id
  resource_id = aws_api_gateway_resource.flight_offers.id
  http_method = aws_api_gateway_method.flight_offers_post.http_method

  integration_http_method = "POST"
  type                    = "AWS_PROXY"
  uri                     = aws_lambda_function.flight_offers.invoke_arn
}

# Lambda Permission for Flight Offers API Gateway
resource "aws_lambda_permission" "flight_offers_api_gateway" {
  statement_id  = "AllowExecutionFromAPIGateway"
  action        = "lambda:InvokeFunction"
  function_name = aws_lambda_function.flight_offers.function_name
  principal     = "apigateway.amazonaws.com"

  source_arn = "${aws_api_gateway_rest_api.seatmap_api.execution_arn}/*/*"
}

# Bookmarks API Gateway Resource
resource "aws_api_gateway_resource" "bookmarks" {
  rest_api_id = aws_api_gateway_rest_api.seatmap_api.id
  parent_id   = aws_api_gateway_rest_api.seatmap_api.root_resource_id
  path_part   = "bookmarks"
}

# Bookmarks Resource (for individual bookmark operations)
resource "aws_api_gateway_resource" "bookmark_id" {
  rest_api_id = aws_api_gateway_rest_api.seatmap_api.id
  parent_id   = aws_api_gateway_resource.bookmarks.id
  path_part   = "{id}"
}

# Bookmarks Methods
resource "aws_api_gateway_method" "bookmarks_get" {
  rest_api_id   = aws_api_gateway_rest_api.seatmap_api.id
  resource_id   = aws_api_gateway_resource.bookmarks.id
  http_method   = "GET"
  authorization = "NONE"
  api_key_required = true
}

resource "aws_api_gateway_method" "bookmarks_post" {
  rest_api_id   = aws_api_gateway_rest_api.seatmap_api.id
  resource_id   = aws_api_gateway_resource.bookmarks.id
  http_method   = "POST"
  authorization = "NONE"
  api_key_required = true
}

resource "aws_api_gateway_method" "bookmark_get" {
  rest_api_id   = aws_api_gateway_rest_api.seatmap_api.id
  resource_id   = aws_api_gateway_resource.bookmark_id.id
  http_method   = "GET"
  authorization = "NONE"
  api_key_required = true
}

resource "aws_api_gateway_method" "bookmark_delete" {
  rest_api_id   = aws_api_gateway_rest_api.seatmap_api.id
  resource_id   = aws_api_gateway_resource.bookmark_id.id
  http_method   = "DELETE"
  authorization = "NONE"
  api_key_required = true
}

# Bookmarks Integrations
resource "aws_api_gateway_integration" "bookmarks_get_integration" {
  rest_api_id = aws_api_gateway_rest_api.seatmap_api.id
  resource_id = aws_api_gateway_resource.bookmarks.id
  http_method = aws_api_gateway_method.bookmarks_get.http_method

  integration_http_method = "POST"
  type                    = "AWS_PROXY"
  uri                     = aws_lambda_function.bookmarks.invoke_arn
}

resource "aws_api_gateway_integration" "bookmarks_post_integration" {
  rest_api_id = aws_api_gateway_rest_api.seatmap_api.id
  resource_id = aws_api_gateway_resource.bookmarks.id
  http_method = aws_api_gateway_method.bookmarks_post.http_method

  integration_http_method = "POST"
  type                    = "AWS_PROXY"
  uri                     = aws_lambda_function.bookmarks.invoke_arn
}

resource "aws_api_gateway_integration" "bookmark_get_integration" {
  rest_api_id = aws_api_gateway_rest_api.seatmap_api.id
  resource_id = aws_api_gateway_resource.bookmark_id.id
  http_method = aws_api_gateway_method.bookmark_get.http_method

  integration_http_method = "POST"
  type                    = "AWS_PROXY"
  uri                     = aws_lambda_function.bookmarks.invoke_arn
}

resource "aws_api_gateway_integration" "bookmark_delete_integration" {
  rest_api_id = aws_api_gateway_rest_api.seatmap_api.id
  resource_id = aws_api_gateway_resource.bookmark_id.id
  http_method = aws_api_gateway_method.bookmark_delete.http_method

  integration_http_method = "POST"
  type                    = "AWS_PROXY"
  uri                     = aws_lambda_function.bookmarks.invoke_arn
}

# Lambda Permission for Bookmarks API Gateway
resource "aws_lambda_permission" "bookmarks_api_gateway" {
  statement_id  = "AllowExecutionFromAPIGateway"
  action        = "lambda:InvokeFunction"
  function_name = aws_lambda_function.bookmarks.function_name
  principal     = "apigateway.amazonaws.com"

  source_arn = "${aws_api_gateway_rest_api.seatmap_api.execution_arn}/*/*"
}

# API Gateway Deployment
resource "aws_api_gateway_deployment" "main" {
  depends_on = [
    aws_api_gateway_integration.seat_map_integration,
    aws_api_gateway_integration_response.seat_map_integration_response,
    aws_api_gateway_integration.seat_map_bookmark_integration,
    aws_api_gateway_integration.auth_guest_integration,
    aws_api_gateway_integration.auth_login_integration,
    aws_api_gateway_integration.auth_register_integration,
    aws_api_gateway_integration.flight_offers_integration,
    aws_api_gateway_integration.bookmarks_get_integration,
    aws_api_gateway_integration.bookmarks_post_integration,
    aws_api_gateway_integration.bookmark_get_integration,
    aws_api_gateway_integration.bookmark_delete_integration
  ]

  rest_api_id = aws_api_gateway_rest_api.seatmap_api.id
  stage_name  = local.environment

  triggers = {
    redeployment = sha1(jsonencode([
      aws_api_gateway_resource.seat_map.id,
      aws_api_gateway_method.seat_map_post.id,
      aws_api_gateway_integration.seat_map_integration.id,
      aws_api_gateway_resource.auth.id,
      aws_api_gateway_resource.auth_guest.id,
      aws_api_gateway_resource.auth_login.id,
      aws_api_gateway_resource.auth_register.id,
      aws_api_gateway_resource.auth_profile.id,
      aws_api_gateway_method.auth_guest_post.id,
      aws_api_gateway_method.auth_login_post.id,
      aws_api_gateway_method.auth_register_post.id,
      aws_api_gateway_method.auth_profile_get.id,
      aws_api_gateway_method.auth_profile_put.id,
      aws_api_gateway_integration.auth_guest_integration.id,
      aws_api_gateway_integration.auth_login_integration.id,
      aws_api_gateway_integration.auth_register_integration.id,
      aws_api_gateway_integration.auth_profile_get_integration.id,
      aws_api_gateway_integration.auth_profile_put_integration.id,
      aws_api_gateway_resource.flight_offers.id,
      aws_api_gateway_method.flight_offers_post.id,
      aws_api_gateway_integration.flight_offers_integration.id,
      aws_api_gateway_resource.bookmarks.id,
      aws_api_gateway_resource.bookmark_id.id,
      aws_api_gateway_method.bookmarks_get.id,
      aws_api_gateway_method.bookmarks_post.id,
      aws_api_gateway_method.bookmark_get.id,
      aws_api_gateway_method.bookmark_delete.id,
      aws_api_gateway_integration.bookmarks_get_integration.id,
      aws_api_gateway_integration.bookmarks_post_integration.id,
      aws_api_gateway_integration.bookmark_get_integration.id,
      aws_api_gateway_integration.bookmark_delete_integration.id,
      aws_api_gateway_resource.seat_map_bookmark.id,
      aws_api_gateway_resource.seat_map_bookmark_id.id,
      aws_api_gateway_method.seat_map_bookmark_get.id,
      aws_api_gateway_integration.seat_map_bookmark_integration.id,
    ]))
  }

  lifecycle {
    create_before_destroy = true
  }
}

# API Gateway API Key
resource "aws_api_gateway_api_key" "client_key" {
  name        = "seatmap-client-key-${local.environment}"
  description = "API key for seatmap client access"
  enabled     = true

  tags = local.common_tags
}

# API Gateway Usage Plan
resource "aws_api_gateway_usage_plan" "main" {
  name         = "seatmap-usage-plan-${local.environment}"
  description  = "Usage plan for seatmap API"

  api_stages {
    api_id = aws_api_gateway_rest_api.seatmap_api.id
    stage  = aws_api_gateway_deployment.main.stage_name
  }

  quota_settings {
    limit  = 50000  # Higher limit for production
    period = "MONTH"
  }

  throttle_settings {
    rate_limit  = 500   # Higher rate limit for production
    burst_limit = 1000  # Higher burst limit for production
  }

  tags = local.common_tags
}

# Link API Key to Usage Plan
resource "aws_api_gateway_usage_plan_key" "main" {
  key_id        = aws_api_gateway_api_key.client_key.id
  key_type      = "API_KEY"
  usage_plan_id = aws_api_gateway_usage_plan.main.id
}
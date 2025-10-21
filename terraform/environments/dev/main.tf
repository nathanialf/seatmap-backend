terraform {
  required_version = ">= 1.0"
  
  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.0"
    }
  }
  
  backend "s3" {
    bucket = "seatmap-backend-terraform-state-dev"
    key    = "seatmap-backend/terraform.tfstate"
    region = "us-west-1"
    dynamodb_table = "seatmap-backend-terraform-locks-dev"
  }
}

# Configure AWS Provider
provider "aws" {
  region = var.aws_region
  
  default_tags {
    tags = {
      Project     = "seatmap-backend"
      Environment = "dev"
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
  environment  = "dev"
  
  # Lambda configuration
  lambda_jar_path = "../../../build/libs/SEATMAP-Backend-1.0.0.jar"
  
  common_tags = {
    Project     = local.project_name
    Environment = local.environment
    Region      = data.aws_region.current.name
    Account     = data.aws_caller_identity.current.account_id
  }
}

# Lambda Function
resource "aws_lambda_function" "seat_map" {
  filename         = local.lambda_jar_path
  function_name    = "seatmap-seat-map-${local.environment}"
  role            = aws_iam_role.lambda_role.arn
  handler         = "com.seatmap.api.handler.SeatMapHandler::handleRequest"
  runtime         = "java17"
  memory_size     = 512
  timeout         = 30
  
  source_code_hash = filebase64sha256(local.lambda_jar_path)
  
  environment {
    variables = {
      ENVIRONMENT        = local.environment
      AMADEUS_ENDPOINT   = var.amadeus_endpoint
      AMADEUS_API_KEY    = var.amadeus_api_key
      AMADEUS_API_SECRET = var.amadeus_api_secret
      JWT_SECRET         = var.jwt_secret
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
          "dynamodb:DeleteItem"
        ]
        Resource = [
          aws_dynamodb_table.users.arn,
          "${aws_dynamodb_table.users.arn}/index/*",
          aws_dynamodb_table.sessions.arn,
          "${aws_dynamodb_table.sessions.arn}/index/*",
          aws_dynamodb_table.subscriptions.arn,
          "${aws_dynamodb_table.subscriptions.arn}/index/*"
        ]
      }
    ]
  })
}

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

# API Gateway
resource "aws_api_gateway_rest_api" "seatmap_api" {
  name        = "seatmap-api-${local.environment}"
  description = "Seatmap Backend API - Development"

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

# API Gateway Deployment
resource "aws_api_gateway_deployment" "main" {
  depends_on = [
    aws_api_gateway_integration.seat_map_integration,
    aws_api_gateway_integration_response.seat_map_integration_response
  ]

  rest_api_id = aws_api_gateway_rest_api.seatmap_api.id
  stage_name  = local.environment

  triggers = {
    redeployment = sha1(jsonencode([
      aws_api_gateway_resource.seat_map.id,
      aws_api_gateway_method.seat_map_post.id,
      aws_api_gateway_integration.seat_map_integration.id,
    ]))
  }

  lifecycle {
    create_before_destroy = true
  }
}
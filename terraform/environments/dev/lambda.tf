# Lambda Functions

# Auth Lambda Function
resource "aws_lambda_function" "auth" {
  filename         = local.lambda_jar_path
  function_name    = "seatmap-auth-${local.environment}"
  role            = aws_iam_role.lambda_role.arn
  handler         = "com.seatmap.auth.handler.AuthHandler::handleRequest"
  runtime         = "java17"
  memory_size     = 256
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
      BASE_URL           = "https://${aws_api_gateway_rest_api.seatmap_api.id}.execute-api.${data.aws_region.current.name}.amazonaws.com/${local.environment}"
    }
  }

  tags = local.common_tags
}

# Flight Search Lambda Function (replaces FlightOffersHandler)
resource "aws_lambda_function" "flight_search" {
  filename         = local.lambda_jar_path
  function_name    = "seatmap-flight-search-${local.environment}"
  role            = aws_iam_role.lambda_role.arn
  handler         = "com.seatmap.api.handler.FlightSearchHandler::handleRequest"
  runtime         = "java17"
  memory_size     = 512
  timeout         = 120   # Longer timeout for multiple API calls
  
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

# Seatmap View Lambda Function (new for usage tracking)
resource "aws_lambda_function" "seatmap_view" {
  filename         = local.lambda_jar_path
  function_name    = "seatmap-view-${local.environment}"
  role            = aws_iam_role.lambda_role.arn
  handler         = "com.seatmap.api.handler.SeatmapViewHandler::handleRequest"
  runtime         = "java17"
  memory_size     = 512   # Same as other handlers
  timeout         = 30    # Short timeout
  
  source_code_hash = filebase64sha256(local.lambda_jar_path)
  
  environment {
    variables = {
      ENVIRONMENT = local.environment
      JWT_SECRET  = var.jwt_secret
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

# Lambda Function for Tiers
resource "aws_lambda_function" "tiers" {
  filename         = local.lambda_jar_path
  function_name    = "seatmap-tiers-${local.environment}"
  role            = aws_iam_role.lambda_role.arn
  handler         = "com.seatmap.api.handler.TierHandler::handleRequest"
  runtime         = "java17"
  memory_size     = 128
  timeout         = 30
  
  source_code_hash = filebase64sha256(local.lambda_jar_path)
  
  environment {
    variables = {
      ENVIRONMENT = local.environment
    }
  }

  tags = local.common_tags
}
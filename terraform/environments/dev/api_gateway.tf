# API Gateway
resource "aws_api_gateway_rest_api" "seatmap_api" {
  name        = "seatmap-api-${local.environment}"
  description = "Seatmap Backend API - Development"

  tags = local.common_tags
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

# Auth Verify Resource
resource "aws_api_gateway_resource" "auth_verify" {
  rest_api_id = aws_api_gateway_rest_api.seatmap_api.id
  parent_id   = aws_api_gateway_resource.auth.id
  path_part   = "verify"
}

# Auth Resend Verification Resource
resource "aws_api_gateway_resource" "auth_resend_verification" {
  rest_api_id = aws_api_gateway_rest_api.seatmap_api.id
  parent_id   = aws_api_gateway_resource.auth.id
  path_part   = "resend-verification"
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

# Auth Verify Method (GET)
resource "aws_api_gateway_method" "auth_verify_get" {
  rest_api_id   = aws_api_gateway_rest_api.seatmap_api.id
  resource_id   = aws_api_gateway_resource.auth_verify.id
  http_method   = "GET"
  authorization = "NONE"
  api_key_required = false  # No API key needed for email verification links
}

# Auth Resend Verification Method (POST)
resource "aws_api_gateway_method" "auth_resend_verification_post" {
  rest_api_id   = aws_api_gateway_rest_api.seatmap_api.id
  resource_id   = aws_api_gateway_resource.auth_resend_verification.id
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

# Auth Verify Integration
resource "aws_api_gateway_integration" "auth_verify_integration" {
  rest_api_id = aws_api_gateway_rest_api.seatmap_api.id
  resource_id = aws_api_gateway_resource.auth_verify.id
  http_method = aws_api_gateway_method.auth_verify_get.http_method

  integration_http_method = "POST"
  type                    = "AWS_PROXY"
  uri                     = aws_lambda_function.auth.invoke_arn
}

# Auth Resend Verification Integration
resource "aws_api_gateway_integration" "auth_resend_verification_integration" {
  rest_api_id = aws_api_gateway_rest_api.seatmap_api.id
  resource_id = aws_api_gateway_resource.auth_resend_verification.id
  http_method = aws_api_gateway_method.auth_resend_verification_post.http_method

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

# Flight Search API Gateway Resource (replaces flight-offers)
resource "aws_api_gateway_resource" "flight_search" {
  rest_api_id = aws_api_gateway_rest_api.seatmap_api.id
  parent_id   = aws_api_gateway_rest_api.seatmap_api.root_resource_id
  path_part   = "flight-search"
}

# Flight Search Bookmark Resource
resource "aws_api_gateway_resource" "flight_search_bookmark" {
  rest_api_id = aws_api_gateway_rest_api.seatmap_api.id
  parent_id   = aws_api_gateway_resource.flight_search.id
  path_part   = "bookmark"
}

# Flight Search Bookmark ID Resource
resource "aws_api_gateway_resource" "flight_search_bookmark_id" {
  rest_api_id = aws_api_gateway_rest_api.seatmap_api.id
  parent_id   = aws_api_gateway_resource.flight_search_bookmark.id
  path_part   = "{bookmarkId}"
}

# Flight Search Method (POST)
resource "aws_api_gateway_method" "flight_search_post" {
  rest_api_id   = aws_api_gateway_rest_api.seatmap_api.id
  resource_id   = aws_api_gateway_resource.flight_search.id
  http_method   = "POST"
  authorization = "NONE"
  api_key_required = true
}

# Flight Search Bookmark Method (GET)
resource "aws_api_gateway_method" "flight_search_bookmark_get" {
  rest_api_id   = aws_api_gateway_rest_api.seatmap_api.id
  resource_id   = aws_api_gateway_resource.flight_search_bookmark_id.id
  http_method   = "GET"
  authorization = "NONE"
  api_key_required = true
}

# Flight Search Integration
resource "aws_api_gateway_integration" "flight_search_integration" {
  rest_api_id = aws_api_gateway_rest_api.seatmap_api.id
  resource_id = aws_api_gateway_resource.flight_search.id
  http_method = aws_api_gateway_method.flight_search_post.http_method

  integration_http_method = "POST"
  type                    = "AWS_PROXY"
  uri                     = aws_lambda_function.flight_search.invoke_arn
}

# Flight Search Bookmark Integration
resource "aws_api_gateway_integration" "flight_search_bookmark_integration" {
  rest_api_id = aws_api_gateway_rest_api.seatmap_api.id
  resource_id = aws_api_gateway_resource.flight_search_bookmark_id.id
  http_method = aws_api_gateway_method.flight_search_bookmark_get.http_method

  integration_http_method = "POST"
  type                    = "AWS_PROXY"
  uri                     = aws_lambda_function.flight_search.invoke_arn
}

# Lambda Permission for Flight Search API Gateway
resource "aws_lambda_permission" "flight_search_api_gateway" {
  statement_id  = "AllowExecutionFromAPIGateway"
  action        = "lambda:InvokeFunction"
  function_name = aws_lambda_function.flight_search.function_name
  principal     = "apigateway.amazonaws.com"

  source_arn = "${aws_api_gateway_rest_api.seatmap_api.execution_arn}/*/*"
}

# Seatmap View API Gateway Resources (new for usage tracking)
resource "aws_api_gateway_resource" "seatmap" {
  rest_api_id = aws_api_gateway_rest_api.seatmap_api.id
  parent_id   = aws_api_gateway_rest_api.seatmap_api.root_resource_id
  path_part   = "seatmap"
}

resource "aws_api_gateway_resource" "seatmap_view" {
  rest_api_id = aws_api_gateway_rest_api.seatmap_api.id
  parent_id   = aws_api_gateway_resource.seatmap.id
  path_part   = "view"
}

# Seatmap View Method (POST)
resource "aws_api_gateway_method" "seatmap_view_post" {
  rest_api_id   = aws_api_gateway_rest_api.seatmap_api.id
  resource_id   = aws_api_gateway_resource.seatmap_view.id
  http_method   = "POST"
  authorization = "NONE"
  api_key_required = true
}

# Seatmap View Integration
resource "aws_api_gateway_integration" "seatmap_view_integration" {
  rest_api_id = aws_api_gateway_rest_api.seatmap_api.id
  resource_id = aws_api_gateway_resource.seatmap_view.id
  http_method = aws_api_gateway_method.seatmap_view_post.http_method

  integration_http_method = "POST"
  type                    = "AWS_PROXY"
  uri                     = aws_lambda_function.seatmap_view.invoke_arn
}

# Lambda Permission for Seatmap View API Gateway
resource "aws_lambda_permission" "seatmap_view_api_gateway" {
  statement_id  = "AllowExecutionFromAPIGateway"
  action        = "lambda:InvokeFunction"
  function_name = aws_lambda_function.seatmap_view.function_name
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


# Tiers API Gateway Resources
resource "aws_api_gateway_resource" "tiers" {
  rest_api_id = aws_api_gateway_rest_api.seatmap_api.id
  parent_id   = aws_api_gateway_rest_api.seatmap_api.root_resource_id
  path_part   = "tiers"
}

resource "aws_api_gateway_resource" "tier_name" {
  rest_api_id = aws_api_gateway_rest_api.seatmap_api.id
  parent_id   = aws_api_gateway_resource.tiers.id
  path_part   = "{tierName}"
}

# Tiers Methods
resource "aws_api_gateway_method" "tiers_get" {
  rest_api_id   = aws_api_gateway_rest_api.seatmap_api.id
  resource_id   = aws_api_gateway_resource.tiers.id
  http_method   = "GET"
  authorization = "NONE"
  api_key_required = true
}

resource "aws_api_gateway_method" "tier_name_get" {
  rest_api_id   = aws_api_gateway_rest_api.seatmap_api.id
  resource_id   = aws_api_gateway_resource.tier_name.id
  http_method   = "GET"
  authorization = "NONE"
  api_key_required = true
}

# Tiers Integrations
resource "aws_api_gateway_integration" "tiers_get_integration" {
  rest_api_id = aws_api_gateway_rest_api.seatmap_api.id
  resource_id = aws_api_gateway_resource.tiers.id
  http_method = aws_api_gateway_method.tiers_get.http_method

  integration_http_method = "POST"
  type                    = "AWS_PROXY"
  uri                     = aws_lambda_function.tiers.invoke_arn
}

resource "aws_api_gateway_integration" "tier_name_get_integration" {
  rest_api_id = aws_api_gateway_rest_api.seatmap_api.id
  resource_id = aws_api_gateway_resource.tier_name.id
  http_method = aws_api_gateway_method.tier_name_get.http_method

  integration_http_method = "POST"
  type                    = "AWS_PROXY"
  uri                     = aws_lambda_function.tiers.invoke_arn
}

# Lambda Permission for Tiers API Gateway
resource "aws_lambda_permission" "tiers_api_gateway" {
  statement_id  = "AllowExecutionFromAPIGateway"
  action        = "lambda:InvokeFunction"
  function_name = aws_lambda_function.tiers.function_name
  principal     = "apigateway.amazonaws.com"

  source_arn = "${aws_api_gateway_rest_api.seatmap_api.execution_arn}/*/*"
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
    aws_api_gateway_integration.auth_guest_integration,
    aws_api_gateway_integration.auth_login_integration,
    aws_api_gateway_integration.auth_register_integration,
    aws_api_gateway_integration.auth_verify_integration,
    aws_api_gateway_integration.auth_resend_verification_integration,
    aws_api_gateway_integration.flight_search_integration,
    aws_api_gateway_integration.flight_search_bookmark_integration,
    aws_api_gateway_integration.seatmap_view_integration,
    aws_api_gateway_integration.bookmarks_get_integration,
    aws_api_gateway_integration.bookmarks_post_integration,
    aws_api_gateway_integration.bookmark_get_integration,
    aws_api_gateway_integration.bookmark_delete_integration,
  ]

  rest_api_id = aws_api_gateway_rest_api.seatmap_api.id
  stage_name  = local.environment

  triggers = {
    redeployment = sha1(jsonencode([
      aws_api_gateway_resource.auth.id,
      aws_api_gateway_resource.auth_guest.id,
      aws_api_gateway_resource.auth_login.id,
      aws_api_gateway_resource.auth_register.id,
      aws_api_gateway_resource.auth_verify.id,
      aws_api_gateway_resource.auth_resend_verification.id,
      aws_api_gateway_resource.auth_profile.id,
      aws_api_gateway_method.auth_guest_post.id,
      aws_api_gateway_method.auth_login_post.id,
      aws_api_gateway_method.auth_register_post.id,
      aws_api_gateway_method.auth_verify_get.id,
      aws_api_gateway_method.auth_resend_verification_post.id,
      aws_api_gateway_method.auth_profile_get.id,
      aws_api_gateway_method.auth_profile_put.id,
      aws_api_gateway_integration.auth_guest_integration.id,
      aws_api_gateway_integration.auth_login_integration.id,
      aws_api_gateway_integration.auth_register_integration.id,
      aws_api_gateway_integration.auth_verify_integration.id,
      aws_api_gateway_integration.auth_resend_verification_integration.id,
      aws_api_gateway_integration.auth_profile_get_integration.id,
      aws_api_gateway_integration.auth_profile_put_integration.id,
      aws_api_gateway_resource.flight_search.id,
      aws_api_gateway_resource.flight_search_bookmark.id,
      aws_api_gateway_resource.flight_search_bookmark_id.id,
      aws_api_gateway_method.flight_search_post.id,
      aws_api_gateway_method.flight_search_bookmark_get.id,
      aws_api_gateway_integration.flight_search_integration.id,
      aws_api_gateway_integration.flight_search_bookmark_integration.id,
      aws_api_gateway_resource.seatmap.id,
      aws_api_gateway_resource.seatmap_view.id,
      aws_api_gateway_method.seatmap_view_post.id,
      aws_api_gateway_integration.seatmap_view_integration.id,
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
      aws_api_gateway_resource.tiers.id,
      aws_api_gateway_resource.tier_name.id,
      aws_api_gateway_method.tiers_get.id,
      aws_api_gateway_method.tier_name_get.id,
      aws_api_gateway_integration.tiers_get_integration.id,
      aws_api_gateway_integration.tier_name_get_integration.id,
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
    limit  = 10000
    period = "MONTH"
  }

  throttle_settings {
    rate_limit  = 100
    burst_limit = 200
  }

  tags = local.common_tags
}

# Link API Key to Usage Plan
resource "aws_api_gateway_usage_plan_key" "main" {
  key_id        = aws_api_gateway_api_key.client_key.id
  key_type      = "API_KEY"
  usage_plan_id = aws_api_gateway_usage_plan.main.id
}
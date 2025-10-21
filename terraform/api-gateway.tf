# API Gateway REST API
resource "aws_api_gateway_rest_api" "seatmap_api" {
  name        = "${var.project_name}-api-${var.environment}"
  description = "Seatmap Backend API for ${var.environment}"

  endpoint_configuration {
    types = ["REGIONAL"]
  }

  tags = local.common_tags
}

# API Gateway Resource for seat map
resource "aws_api_gateway_resource" "seat_map" {
  rest_api_id = aws_api_gateway_rest_api.seatmap_api.id
  parent_id   = aws_api_gateway_rest_api.seatmap_api.root_resource_id
  path_part   = "seat-map"
}

# API Gateway Method for seat map POST
resource "aws_api_gateway_method" "seat_map_post" {
  rest_api_id   = aws_api_gateway_rest_api.seatmap_api.id
  resource_id   = aws_api_gateway_resource.seat_map.id
  http_method   = "POST"
  authorization = "NONE"

  request_parameters = {
    "method.request.header.Authorization" = true
  }
}

# API Gateway Integration for seat map
resource "aws_api_gateway_integration" "seat_map_integration" {
  rest_api_id = aws_api_gateway_rest_api.seatmap_api.id
  resource_id = aws_api_gateway_resource.seat_map.id
  http_method = aws_api_gateway_method.seat_map_post.http_method

  integration_http_method = "POST"
  type                    = "AWS_PROXY"
  uri                     = aws_lambda_function.seat_map.invoke_arn
}

# API Gateway Method Response for seat map
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

# API Gateway Integration Response for seat map
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

# CORS OPTIONS method for seat map
resource "aws_api_gateway_method" "seat_map_options" {
  rest_api_id   = aws_api_gateway_rest_api.seatmap_api.id
  resource_id   = aws_api_gateway_resource.seat_map.id
  http_method   = "OPTIONS"
  authorization = "NONE"
}

resource "aws_api_gateway_integration" "seat_map_options_integration" {
  rest_api_id = aws_api_gateway_rest_api.seatmap_api.id
  resource_id = aws_api_gateway_resource.seat_map.id
  http_method = aws_api_gateway_method.seat_map_options.http_method

  type = "MOCK"
  request_templates = {
    "application/json" = "{\"statusCode\": 200}"
  }
}

resource "aws_api_gateway_method_response" "seat_map_options_response" {
  rest_api_id = aws_api_gateway_rest_api.seatmap_api.id
  resource_id = aws_api_gateway_resource.seat_map.id
  http_method = aws_api_gateway_method.seat_map_options.http_method
  status_code = "200"

  response_parameters = {
    "method.response.header.Access-Control-Allow-Origin"  = true
    "method.response.header.Access-Control-Allow-Methods" = true
    "method.response.header.Access-Control-Allow-Headers" = true
  }
}

resource "aws_api_gateway_integration_response" "seat_map_options_integration_response" {
  rest_api_id = aws_api_gateway_rest_api.seatmap_api.id
  resource_id = aws_api_gateway_resource.seat_map.id
  http_method = aws_api_gateway_method.seat_map_options.http_method
  status_code = aws_api_gateway_method_response.seat_map_options_response.status_code

  response_parameters = {
    "method.response.header.Access-Control-Allow-Origin"  = "'*'"
    "method.response.header.Access-Control-Allow-Methods" = "'GET,POST,PUT,DELETE,OPTIONS'"
    "method.response.header.Access-Control-Allow-Headers" = "'Content-Type,Authorization'"
  }

  depends_on = [aws_api_gateway_integration.seat_map_options_integration]
}

# API Gateway Deployment
resource "aws_api_gateway_deployment" "seatmap_api_deployment" {
  rest_api_id = aws_api_gateway_rest_api.seatmap_api.id

  triggers = {
    redeployment = sha1(jsonencode([
      aws_api_gateway_resource.seat_map.id,
      aws_api_gateway_method.seat_map_post.id,
      aws_api_gateway_integration.seat_map_integration.id,
      aws_api_gateway_method.seat_map_options.id,
      aws_api_gateway_integration.seat_map_options_integration.id,
    ]))
  }

  lifecycle {
    create_before_destroy = true
  }

  depends_on = [
    aws_api_gateway_method.seat_map_post,
    aws_api_gateway_integration.seat_map_integration,
    aws_api_gateway_method.seat_map_options,
    aws_api_gateway_integration.seat_map_options_integration
  ]
}

# API Gateway Stage
resource "aws_api_gateway_stage" "seatmap_api_stage" {
  deployment_id = aws_api_gateway_deployment.seatmap_api_deployment.id
  rest_api_id   = aws_api_gateway_rest_api.seatmap_api.id
  stage_name    = var.environment

  tags = local.common_tags
}
# IAM role for Lambda functions
resource "aws_iam_role" "lambda_execution_role" {
  name = "${var.project_name}-lambda-execution-role-${var.environment}"

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

# IAM policy for Lambda functions
resource "aws_iam_role_policy" "lambda_execution_policy" {
  name = "${var.project_name}-lambda-execution-policy-${var.environment}"
  role = aws_iam_role.lambda_execution_role.id

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
          "${aws_dynamodb_table.users.arn}/*",
          aws_dynamodb_table.sessions.arn,
          "${aws_dynamodb_table.sessions.arn}/*"
        ]
      }
    ]
  })
}

# Lambda function for seat map
resource "aws_lambda_function" "seat_map" {
  filename         = "lambda-artifacts/seatmap-backend-1.0.0.jar"
  function_name    = "${var.project_name}-seat-map-${var.environment}"
  role            = aws_iam_role.lambda_execution_role.arn
  handler         = "com.seatmap.api.handler.SeatMapHandler::handleRequest"
  runtime         = var.lambda_runtime
  memory_size     = var.lambda_memory_size
  timeout         = var.lambda_timeout

  environment {
    variables = {
      ENVIRONMENT = var.environment
      DYNAMODB_USERS_TABLE = aws_dynamodb_table.users.name
      DYNAMODB_SESSIONS_TABLE = aws_dynamodb_table.sessions.name
      JWT_SECRET = var.jwt_secret
      AMADEUS_API_KEY = var.amadeus_api_key
      AMADEUS_API_SECRET = var.amadeus_api_secret
      AMADEUS_ENDPOINT = var.amadeus_endpoint
    }
  }

  depends_on = [
    aws_iam_role_policy.lambda_execution_policy,
    aws_cloudwatch_log_group.seat_map_logs
  ]

  tags = local.common_tags
}

# CloudWatch Log Group for seat map Lambda
resource "aws_cloudwatch_log_group" "seat_map_logs" {
  name              = "/aws/lambda/${var.project_name}-seat-map-${var.environment}"
  retention_in_days = 14

  tags = local.common_tags
}

# Lambda permission for API Gateway
resource "aws_lambda_permission" "seat_map_api_gateway" {
  statement_id  = "AllowExecutionFromAPIGateway"
  action        = "lambda:InvokeFunction"
  function_name = aws_lambda_function.seat_map.function_name
  principal     = "apigateway.amazonaws.com"
  source_arn    = "${aws_api_gateway_rest_api.seatmap_api.execution_arn}/*/*"
}
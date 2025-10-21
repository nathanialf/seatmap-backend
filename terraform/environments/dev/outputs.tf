# Lambda Function Outputs
output "lambda_function_name" {
  description = "Name of the Lambda function"
  value       = aws_lambda_function.seat_map.function_name
}

output "lambda_function_arn" {
  description = "ARN of the Lambda function"
  value       = aws_lambda_function.seat_map.arn
}

output "lambda_invoke_arn" {
  description = "Invoke ARN of the Lambda function"
  value       = aws_lambda_function.seat_map.invoke_arn
}

# API Gateway Outputs
output "api_gateway_id" {
  description = "ID of the API Gateway"
  value       = aws_api_gateway_rest_api.seatmap_api.id
}

output "api_gateway_url" {
  description = "URL of the API Gateway"
  value       = "https://${aws_api_gateway_rest_api.seatmap_api.id}.execute-api.${data.aws_region.current.name}.amazonaws.com/${aws_api_gateway_deployment.main.stage_name}"
}

output "api_gateway_stage" {
  description = "Stage name of the API Gateway deployment"
  value       = aws_api_gateway_deployment.main.stage_name
}

output "seat_map_endpoint" {
  description = "Full URL for the seat map endpoint"
  value       = "https://${aws_api_gateway_rest_api.seatmap_api.id}.execute-api.${data.aws_region.current.name}.amazonaws.com/${aws_api_gateway_deployment.main.stage_name}/seat-map"
}

# DynamoDB Outputs
output "users_table_name" {
  description = "Name of the users DynamoDB table"
  value       = aws_dynamodb_table.users.name
}

output "users_table_arn" {
  description = "ARN of the users DynamoDB table"
  value       = aws_dynamodb_table.users.arn
}

output "sessions_table_name" {
  description = "Name of the sessions DynamoDB table"
  value       = aws_dynamodb_table.sessions.name
}

output "sessions_table_arn" {
  description = "ARN of the sessions DynamoDB table"
  value       = aws_dynamodb_table.sessions.arn
}

output "subscriptions_table_name" {
  description = "Name of the subscriptions DynamoDB table"
  value       = aws_dynamodb_table.subscriptions.name
}

output "subscriptions_table_arn" {
  description = "ARN of the subscriptions DynamoDB table"
  value       = aws_dynamodb_table.subscriptions.arn
}

# IAM Outputs
output "lambda_role_arn" {
  description = "ARN of the Lambda execution role"
  value       = aws_iam_role.lambda_role.arn
}

# Environment Information
output "environment" {
  description = "Environment name"
  value       = local.environment
}

output "project_name" {
  description = "Project name"
  value       = local.project_name
}

output "aws_region" {
  description = "AWS region"
  value       = data.aws_region.current.name
}

output "aws_account_id" {
  description = "AWS account ID"
  value       = data.aws_caller_identity.current.account_id
}
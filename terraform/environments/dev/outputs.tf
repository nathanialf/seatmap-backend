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

output "auth_lambda_function_name" {
  description = "Name of the Auth Lambda function"
  value       = aws_lambda_function.auth.function_name
}

output "auth_lambda_function_arn" {
  description = "ARN of the Auth Lambda function"
  value       = aws_lambda_function.auth.arn
}

output "auth_lambda_invoke_arn" {
  description = "Invoke ARN of the Auth Lambda function"
  value       = aws_lambda_function.auth.invoke_arn
}

output "tiers_lambda_function_name" {
  description = "Name of the Tiers Lambda function"
  value       = aws_lambda_function.tiers.function_name
}

output "tiers_lambda_function_arn" {
  description = "ARN of the Tiers Lambda function"
  value       = aws_lambda_function.tiers.arn
}

output "tiers_lambda_invoke_arn" {
  description = "Invoke ARN of the Tiers Lambda function"
  value       = aws_lambda_function.tiers.invoke_arn
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

output "auth_guest_endpoint" {
  description = "Full URL for the guest authentication endpoint"
  value       = "https://${aws_api_gateway_rest_api.seatmap_api.id}.execute-api.${data.aws_region.current.name}.amazonaws.com/${aws_api_gateway_deployment.main.stage_name}/auth/guest"
}

output "auth_login_endpoint" {
  description = "Full URL for the login authentication endpoint"
  value       = "https://${aws_api_gateway_rest_api.seatmap_api.id}.execute-api.${data.aws_region.current.name}.amazonaws.com/${aws_api_gateway_deployment.main.stage_name}/auth/login"
}

output "auth_register_endpoint" {
  description = "Full URL for the registration authentication endpoint"
  value       = "https://${aws_api_gateway_rest_api.seatmap_api.id}.execute-api.${data.aws_region.current.name}.amazonaws.com/${aws_api_gateway_deployment.main.stage_name}/auth/register"
}

output "tiers_endpoint" {
  description = "Full URL for the tiers endpoint"
  value       = "https://${aws_api_gateway_rest_api.seatmap_api.id}.execute-api.${data.aws_region.current.name}.amazonaws.com/${aws_api_gateway_deployment.main.stage_name}/tiers"
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

output "guest_access_table_name" {
  description = "Name of the guest access DynamoDB table"
  value       = aws_dynamodb_table.guest_access.name
}

output "guest_access_table_arn" {
  description = "ARN of the guest access DynamoDB table"
  value       = aws_dynamodb_table.guest_access.arn
}

output "bookmarks_table_name" {
  description = "Name of the bookmarks DynamoDB table"
  value       = aws_dynamodb_table.bookmarks.name
}

output "bookmarks_table_arn" {
  description = "ARN of the bookmarks DynamoDB table"
  value       = aws_dynamodb_table.bookmarks.arn
}

output "account_tiers_table_name" {
  description = "Name of the account tiers DynamoDB table"
  value       = aws_dynamodb_table.account_tiers.name
}

output "account_tiers_table_arn" {
  description = "ARN of the account tiers DynamoDB table"
  value       = aws_dynamodb_table.account_tiers.arn
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

# API Key Outputs
output "api_key_id" {
  description = "API Gateway API key ID"
  value       = aws_api_gateway_api_key.client_key.id
}

output "api_key_name" {
  description = "API Gateway API key name"
  value       = aws_api_gateway_api_key.client_key.name
}

output "api_key_value" {
  description = "API Gateway API key value"
  value       = aws_api_gateway_api_key.client_key.value
  sensitive   = true
}
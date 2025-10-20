# Terraform Outputs

# DynamoDB Table Names
output "dynamodb_users_table_name" {
  description = "Name of the Users DynamoDB table"
  value       = aws_dynamodb_table.users.name
}

output "dynamodb_sessions_table_name" {
  description = "Name of the Sessions DynamoDB table"
  value       = aws_dynamodb_table.sessions.name
}

output "dynamodb_bookmarks_table_name" {
  description = "Name of the Bookmarks DynamoDB table"
  value       = aws_dynamodb_table.bookmarks.name
}

output "dynamodb_api_cache_table_name" {
  description = "Name of the API Cache DynamoDB table"
  value       = aws_dynamodb_table.api_cache.name
}

output "dynamodb_subscriptions_table_name" {
  description = "Name of the Subscriptions DynamoDB table"
  value       = aws_dynamodb_table.subscriptions.name
}

# DynamoDB Table ARNs
output "dynamodb_users_table_arn" {
  description = "ARN of the Users DynamoDB table"
  value       = aws_dynamodb_table.users.arn
}

output "dynamodb_sessions_table_arn" {
  description = "ARN of the Sessions DynamoDB table"
  value       = aws_dynamodb_table.sessions.arn
}

output "dynamodb_bookmarks_table_arn" {
  description = "ARN of the Bookmarks DynamoDB table"
  value       = aws_dynamodb_table.bookmarks.arn
}

output "dynamodb_api_cache_table_arn" {
  description = "ARN of the API Cache DynamoDB table"
  value       = aws_dynamodb_table.api_cache.arn
}

output "dynamodb_subscriptions_table_arn" {
  description = "ARN of the Subscriptions DynamoDB table"
  value       = aws_dynamodb_table.subscriptions.arn
}

# Environment Information
output "environment" {
  description = "Environment name"
  value       = var.environment
}

output "aws_region" {
  description = "AWS region"
  value       = var.aws_region
}

output "project_name" {
  description = "Project name"
  value       = var.project_name
}
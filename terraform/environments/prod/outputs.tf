# Route53 Hosted Zone outputs
output "myseatmap_hosted_zone_id" {
  description = "Hosted Zone ID for myseatmap.com"
  value       = aws_route53_zone.myseatmap.zone_id
}

output "myseatmap_name_servers" {
  description = "Name servers for myseatmap.com hosted zone"
  value       = aws_route53_zone.myseatmap.name_servers
}

output "myseatmap_zone_arn" {
  description = "ARN of the myseatmap.com hosted zone"
  value       = aws_route53_zone.myseatmap.arn
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
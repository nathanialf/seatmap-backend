# Provider for us-east-1 (required for ACM certificates used with API Gateway)
provider "aws" {
  alias  = "us_east_1"
  region = "us-east-1"
}

# Route53 Hosted Zone for api-dev.myseatmap.com
resource "aws_route53_zone" "api_dev" {
  name = "api-dev.myseatmap.com"

  tags = merge(local.common_tags, {
    Name   = "api-dev.myseatmap.com"
    Region = "us-east-1"  # Override region tag since ACM cert is in us-east-1
  })
}

# ACM Certificate for api-dev.myseatmap.com (must be in us-east-1 for API Gateway)
resource "aws_acm_certificate" "api_dev" {
  provider = aws.us_east_1
  
  domain_name       = "api-dev.myseatmap.com"
  validation_method = "DNS"

  tags = merge(local.common_tags, {
    Name   = "api-dev.myseatmap.com"
    Region = "us-east-1"  # Override region tag since ACM cert is in us-east-1
  })

  lifecycle {
    create_before_destroy = true
  }
}

# Route53 record for ACM certificate validation
resource "aws_route53_record" "api_dev_validation" {
  for_each = {
    for dvo in aws_acm_certificate.api_dev.domain_validation_options : dvo.domain_name => {
      name   = dvo.resource_record_name
      record = dvo.resource_record_value
      type   = dvo.resource_record_type
    }
  }

  allow_overwrite = true
  name            = each.value.name
  records         = [each.value.record]
  ttl             = 60
  type            = each.value.type
  zone_id         = aws_route53_zone.api_dev.zone_id
}

# ACM certificate validation - wait for DNS propagation without timeout
resource "aws_acm_certificate_validation" "api_dev" {
  provider = aws.us_east_1
  
  certificate_arn         = aws_acm_certificate.api_dev.arn
  validation_record_fqdns = [for record in aws_route53_record.api_dev_validation : record.fqdn]

  # Remove default timeout to allow for DNS propagation time
  timeouts {
    create = "10m"
  }
}

# API Gateway Custom Domain Name
resource "aws_api_gateway_domain_name" "api_dev" {
  domain_name              = "api-dev.myseatmap.com"
  regional_certificate_arn = aws_acm_certificate_validation.api_dev.certificate_arn

  endpoint_configuration {
    types = ["REGIONAL"]
  }

  tags = merge(local.common_tags, {
    Name   = "api-dev.myseatmap.com"
    Region = "us-east-1"  # Override region tag since ACM cert is in us-east-1
  })

  depends_on = [aws_acm_certificate_validation.api_dev]
}

# API Gateway Base Path Mapping
resource "aws_api_gateway_base_path_mapping" "api_dev" {
  api_id      = aws_api_gateway_rest_api.seatmap_api.id
  stage_name  = aws_api_gateway_deployment.main.stage_name
  domain_name = aws_api_gateway_domain_name.api_dev.domain_name
}

# Route53 A record to point api-dev.myseatmap.com to API Gateway
resource "aws_route53_record" "api_dev" {
  zone_id = aws_route53_zone.api_dev.zone_id
  name    = "api-dev.myseatmap.com"
  type    = "A"

  alias {
    name                   = aws_api_gateway_domain_name.api_dev.regional_domain_name
    zone_id                = aws_api_gateway_domain_name.api_dev.regional_zone_id
    evaluate_target_health = false
  }
}
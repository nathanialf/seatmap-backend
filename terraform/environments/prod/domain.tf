# Route53 Hosted Zone for api.myseatmap.com
resource "aws_route53_zone" "api_prod" {
  name = "api.myseatmap.com"

  tags = merge(local.common_tags, {
    Name = "api.myseatmap.com"
  })
}

# ACM Certificate for api.myseatmap.com (must be in same region as regional API Gateway)
resource "aws_acm_certificate" "api_prod" {
  domain_name       = "api.myseatmap.com"
  validation_method = "DNS"

  tags = merge(local.common_tags, {
    Name = "api.myseatmap.com"
  })

  lifecycle {
    create_before_destroy = true
  }
}

# Route53 record for ACM certificate validation
resource "aws_route53_record" "api_prod_validation" {
  for_each = {
    for dvo in aws_acm_certificate.api_prod.domain_validation_options : dvo.domain_name => {
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
  zone_id         = aws_route53_zone.api_prod.zone_id
}

# ACM certificate validation - wait for DNS propagation without timeout
resource "aws_acm_certificate_validation" "api_prod" {
  certificate_arn         = aws_acm_certificate.api_prod.arn
  validation_record_fqdns = [for record in aws_route53_record.api_prod_validation : record.fqdn]

  # Remove default timeout to allow for DNS propagation time
  timeouts {
    create = "10m"
  }
}

# API Gateway Custom Domain Name
resource "aws_api_gateway_domain_name" "api_prod" {
  domain_name              = "api.myseatmap.com"
  regional_certificate_arn = aws_acm_certificate_validation.api_prod.certificate_arn

  endpoint_configuration {
    types = ["REGIONAL"]
  }

  tags = merge(local.common_tags, {
    Name = "api.myseatmap.com"
  })

  depends_on = [aws_acm_certificate_validation.api_prod]
}

# API Gateway Base Path Mapping
resource "aws_api_gateway_base_path_mapping" "api_prod" {
  api_id      = aws_api_gateway_rest_api.seatmap_api.id
  stage_name  = aws_api_gateway_deployment.main.stage_name
  domain_name = aws_api_gateway_domain_name.api_prod.domain_name
}

# Route53 A record to point api.myseatmap.com to API Gateway
resource "aws_route53_record" "api_prod" {
  zone_id = aws_route53_zone.api_prod.zone_id
  name    = "api.myseatmap.com"
  type    = "A"

  alias {
    name                   = aws_api_gateway_domain_name.api_prod.regional_domain_name
    zone_id                = aws_api_gateway_domain_name.api_prod.regional_zone_id
    evaluate_target_health = false
  }
}
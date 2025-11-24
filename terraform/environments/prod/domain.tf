# Route53 Hosted Zone for myseatmap.com
resource "aws_route53_zone" "myseatmap" {
  name = "myseatmap.com"

  tags = merge(local.common_tags, {
    Name = "myseatmap.com"
  })
}

# NS Record for api-dev.myseatmap.com subdomain delegation
resource "aws_route53_record" "api_dev_ns" {
  zone_id = aws_route53_zone.myseatmap.zone_id
  name    = "api-dev.myseatmap.com"
  type    = "NS"
  ttl     = 300

  records = [
    "ns-1495.awsdns-58.org",
    "ns-428.awsdns-53.com",
    "ns-1998.awsdns-57.co.uk",
    "ns-628.awsdns-14.net"
  ]
}
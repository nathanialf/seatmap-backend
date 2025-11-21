variable "aws_region" {
  description = "AWS region for resources"
  type        = string
  default     = "us-west-1"
}

# API credentials - currently unused in prod (Route53 only)
# These will be needed when frontend or other services are added
variable "amadeus_api_key" {
  description = "Amadeus API key for seat map integration"
  type        = string
  sensitive   = true
}

variable "amadeus_api_secret" {
  description = "Amadeus API secret for seat map integration"
  type        = string
  sensitive   = true
}

variable "amadeus_endpoint" {
  description = "Amadeus API endpoint"
  type        = string
  default     = "api.amadeus.com"
}

variable "jwt_secret" {
  description = "JWT secret for token signing"
  type        = string
  sensitive   = true
}

variable "sabre_user_id" {
  description = "Sabre API user ID for SOAP integration"
  type        = string
  sensitive   = true
}

variable "sabre_password" {
  description = "Sabre API password for SOAP integration"
  type        = string
  sensitive   = true
}

variable "sabre_endpoint" {
  description = "Sabre SOAP API endpoint"
  type        = string
  default     = "https://webservices.platform.sabre.com"
}
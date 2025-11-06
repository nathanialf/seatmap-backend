terraform {
  required_version = ">= 1.0"
  
  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.0"
    }
  }
  
  backend "s3" {
    bucket = "seatmap-backend-terraform-state-prod"
    key    = "seatmap-backend/terraform.tfstate"
    region = "us-west-1"
    dynamodb_table = "seatmap-backend-terraform-locks-prod"
  }
}

# Configure AWS Provider
provider "aws" {
  region = var.aws_region
  
  default_tags {
    tags = {
      Project     = "seatmap-backend"
      Environment = "prod"
      ManagedBy   = "terraform"
    }
  }
}


# Data sources
data "aws_caller_identity" "current" {}
data "aws_region" "current" {}

# Local values for configuration
locals {
  project_name = "seatmap"
  environment  = "prod"
  
  # Lambda configuration
  lambda_jar_path = "../../../build/libs/SEATMAP-Backend-1.0.0.jar"
  
  common_tags = {
    Project     = local.project_name
    Environment = local.environment
    Region      = data.aws_region.current.name
    Account     = data.aws_caller_identity.current.account_id
  }
}



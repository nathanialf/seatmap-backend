terraform {
  required_version = ">= 1.0"
  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.0"
    }
  }
  
  backend "s3" {
    # Backend configuration will be provided during terraform init
    # bucket = "seatmap-terraform-state-${var.environment}"
    # key    = "seatmap-backend/terraform.tfstate"
    # region = var.aws_region
  }
}

provider "aws" {
  region = var.aws_region
  
  default_tags {
    tags = {
      Project     = "seatmap-backend"
      Environment = var.environment
      ManagedBy   = "terraform"
    }
  }
}

# Data sources
data "aws_caller_identity" "current" {}
data "aws_region" "current" {}

# Local values
locals {
  account_id = data.aws_caller_identity.current.account_id
  region     = data.aws_region.current.name
  
  common_tags = {
    Project     = "seatmap-backend"
    Environment = var.environment
    ManagedBy   = "terraform"
  }
  
  lambda_functions = [
    "auth-register",
    "auth-login", 
    "auth-google",
    "auth-apple",
    "auth-guest",
    "flight-search",
    "seat-map",
    "user-profile",
    "subscriptions",
    "bookmarks",
    "health-monitor"
  ]
}
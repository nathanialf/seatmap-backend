#!/usr/bin/env python3
"""
Script to populate TierDefinition data for the seatmap-backend system.
This script creates the initial tier definitions for FREE, PRO, and BUSINESS tiers.

Usage:
    python3 populate-tier-definitions.py --environment dev
    python3 populate-tier-definitions.py --environment prod

Requirements:
    pip install boto3
"""

import argparse
import boto3
import json
import sys
from datetime import datetime, timezone

def create_tier_definitions():
    """Create tier definition records for FREE, PRO, and BUSINESS tiers."""
    
    current_time = datetime.now(timezone.utc).isoformat()
    
    return [
        {
            "tierId": "free-us-2025",
            "tierName": "FREE",
            "displayName": "Free Tier",
            "description": "Free tier with limited access to seatmap features",
            "maxBookmarks": 0,
            "maxSeatmapCalls": 10,
            "priceUsd": "0.00",
            "billingType": "free",
            "canDowngrade": True,
            "region": "US",
            "active": True,
            "createdAt": current_time,
            "updatedAt": current_time
        },
        {
            "tierId": "pro-us-2025",
            "tierName": "PRO",
            "displayName": "Pro Tier",
            "description": "Pro tier with enhanced bookmark and seatmap access",
            "maxBookmarks": 50,
            "maxSeatmapCalls": 500,
            "priceUsd": "9.99",
            "billingType": "monthly",
            "canDowngrade": True,
            "region": "US",
            "active": True,
            "createdAt": current_time,
            "updatedAt": current_time
        },
        {
            "tierId": "business-us-2025",
            "tierName": "BUSINESS",
            "displayName": "Business Tier",
            "description": "Business tier with unlimited access (one-time purchase)",
            "maxBookmarks": -1,  # -1 indicates unlimited
            "maxSeatmapCalls": -1,  # -1 indicates unlimited
            "priceUsd": "99.99",
            "billingType": "one_time",
            "canDowngrade": False,  # Business tier cannot be downgraded
            "region": "US",
            "active": True,
            "createdAt": current_time,
            "updatedAt": current_time
        }
    ]

def convert_to_dynamodb_item(tier_def):
    """Convert tier definition to DynamoDB item format."""
    
    item = {}
    
    for key, value in tier_def.items():
        if isinstance(value, str):
            item[key] = {"S": value}
        elif isinstance(value, int):
            item[key] = {"N": str(value)}
        elif isinstance(value, bool):
            item[key] = {"BOOL": value}
        elif isinstance(value, float):
            item[key] = {"N": str(value)}
        else:
            # Fallback to string representation
            item[key] = {"S": str(value)}
    
    return item

def populate_tier_definitions(environment, dry_run=False):
    """Populate tier definitions in DynamoDB."""
    
    table_name = f"seatmap-account-tiers-{environment}"
    
    print(f"{'[DRY RUN] ' if dry_run else ''}Populating tier definitions for environment: {environment}")
    print(f"Target table: {table_name}")
    
    if not dry_run:
        try:
            dynamodb = boto3.client('dynamodb')
            
            # Test table exists
            try:
                dynamodb.describe_table(TableName=table_name)
                print(f"✓ Table {table_name} exists")
            except dynamodb.exceptions.ResourceNotFoundException:
                print(f"❌ Error: Table {table_name} does not exist")
                print("Please deploy the Terraform infrastructure first")
                return False
                
        except Exception as e:
            print(f"❌ Error connecting to DynamoDB: {e}")
            return False
    
    tier_definitions = create_tier_definitions()
    
    success_count = 0
    error_count = 0
    
    for tier_def in tier_definitions:
        tier_name = tier_def["tierName"]
        tier_id = tier_def["tierId"]
        
        print(f"\n{'[DRY RUN] ' if dry_run else ''}Processing {tier_name} tier ({tier_id}):")
        print(f"  - Max Bookmarks: {tier_def['maxBookmarks']} ({'unlimited' if tier_def['maxBookmarks'] == -1 else 'per month'})")
        print(f"  - Max Seatmap Calls: {tier_def['maxSeatmapCalls']} ({'unlimited' if tier_def['maxSeatmapCalls'] == -1 else 'per month'})")
        print(f"  - Price: ${tier_def['priceUsd']} ({tier_def['billingType']})")
        print(f"  - Can Downgrade: {tier_def['canDowngrade']}")
        
        if not dry_run:
            try:
                # Check if tier already exists
                existing_item = dynamodb.get_item(
                    TableName=table_name,
                    Key={"tierId": {"S": tier_id}}
                )
                
                if "Item" in existing_item:
                    print(f"  ⚠️  Tier {tier_name} already exists, skipping...")
                    continue
                
                # Convert to DynamoDB format and put item
                dynamodb_item = convert_to_dynamodb_item(tier_def)
                
                dynamodb.put_item(
                    TableName=table_name,
                    Item=dynamodb_item,
                    ConditionExpression="attribute_not_exists(tierId)"  # Prevent overwriting
                )
                
                print(f"  ✓ Successfully created {tier_name} tier")
                success_count += 1
                
            except dynamodb.exceptions.ConditionalCheckFailedException:
                print(f"  ⚠️  Tier {tier_name} already exists (race condition), skipping...")
            except Exception as e:
                print(f"  ❌ Error creating {tier_name} tier: {e}")
                error_count += 1
    
    print(f"\n{'[DRY RUN] ' if dry_run else ''}Summary:")
    if not dry_run:
        print(f"  ✓ Successfully created: {success_count} tiers")
        if error_count > 0:
            print(f"  ❌ Failed to create: {error_count} tiers")
    else:
        print(f"  Would create {len(tier_definitions)} tier definitions")
    
    return error_count == 0

def main():
    parser = argparse.ArgumentParser(description="Populate TierDefinition data for seatmap-backend")
    parser.add_argument("--environment", "-e", required=True, choices=["dev", "prod"],
                       help="Environment to populate (dev or prod)")
    parser.add_argument("--dry-run", action="store_true",
                       help="Show what would be created without actually creating it")
    parser.add_argument("--verbose", "-v", action="store_true",
                       help="Enable verbose output")
    
    args = parser.parse_args()
    
    if args.verbose:
        print("Tier definitions to be created:")
        tier_definitions = create_tier_definitions()
        for tier_def in tier_definitions:
            print(json.dumps(tier_def, indent=2))
        print()
    
    success = populate_tier_definitions(args.environment, args.dry_run)
    
    if not success and not args.dry_run:
        print("\n❌ Some tier definitions failed to create. Please check the errors above.")
        sys.exit(1)
    
    print(f"\n✓ {'Dry run completed' if args.dry_run else 'Tier definitions populated successfully'}")

if __name__ == "__main__":
    main()
pipeline {
    agent any
    
    parameters {
        choice(
            name: 'ENVIRONMENT',
            choices: ['dev', 'prod'],
            description: 'Target environment'
        )
        choice(
            name: 'ACTION',
            choices: ['bootstrap', 'plan', 'apply', 'destroy'],
            description: 'Terraform action to perform'
        )
    }
    
    environment {
        AWS_REGION = 'us-west-1'
        PROJECT_NAME = 'seatmap-backend'
        TF_IN_AUTOMATION = 'true'
        TF_INPUT = 'false'
    }
    
    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }
        
        stage('Setup') {
            steps {
                script {
                    // Set environment-specific variables
                    env.STATE_BUCKET = "${PROJECT_NAME}-terraform-state-${params.ENVIRONMENT}"
                    env.LOCK_TABLE = "${PROJECT_NAME}-terraform-locks-${params.ENVIRONMENT}"
                }
                
                // Change to terraform directory
                dir('terraform') {
                    sh 'terraform version'
                }
            }
        }
        
        stage('Load Credentials') {
            steps {
                script {
                    // Load AWS credentials based on environment
                    if (params.ENVIRONMENT == 'dev') {
                        withCredentials([
                            [$class: 'AmazonWebServicesCredentialsBinding', credentialsId: 'seatmap-dev']
                        ]) {
                            env.AWS_ACCESS_KEY_ID = AWS_ACCESS_KEY_ID
                            env.AWS_SECRET_ACCESS_KEY = AWS_SECRET_ACCESS_KEY
                        }
                    } else if (params.ENVIRONMENT == 'prod') {
                        withCredentials([
                            [$class: 'AmazonWebServicesCredentialsBinding', credentialsId: 'seatmap-prod']
                        ]) {
                            env.AWS_ACCESS_KEY_ID = AWS_ACCESS_KEY_ID
                            env.AWS_SECRET_ACCESS_KEY = AWS_SECRET_ACCESS_KEY
                        }
                    }
                }
            }
        }
        
        stage('Bootstrap') {
            when {
                expression { params.ACTION == 'bootstrap' }
            }
            steps {
                dir('terraform') {
                    sh """
                        echo "Running Terraform bootstrap for ${params.ENVIRONMENT}..."
                        
                        # Initialize terraform without backend (local state for bootstrap)
                        terraform init -reconfigure \\
                            -var="environment=${params.ENVIRONMENT}" \\
                            -var="aws_region=${AWS_REGION}"
                        
                        # Plan bootstrap resources
                        terraform plan \\
                            -target=aws_s3_bucket.terraform_state \\
                            -target=aws_s3_bucket_versioning.terraform_state \\
                            -target=aws_s3_bucket_server_side_encryption_configuration.terraform_state \\
                            -target=aws_s3_bucket_public_access_block.terraform_state \\
                            -target=aws_dynamodb_table.terraform_locks \\
                            -var="environment=${params.ENVIRONMENT}" \\
                            -var="aws_region=${AWS_REGION}" \\
                            -out=bootstrap.tfplan
                        
                        # Apply bootstrap resources
                        terraform apply -auto-approve bootstrap.tfplan
                        
                        # Display outputs
                        terraform output
                    """
                }
            }
        }
        
        stage('Terraform Init') {
            when {
                not { 
                    expression { params.ACTION == 'bootstrap' }
                }
            }
            steps {
                dir('terraform') {
                    sh """
                        echo "Initializing Terraform with S3 backend for ${params.ENVIRONMENT}..."
                        terraform init -reconfigure \\
                            -backend-config="bucket=${STATE_BUCKET}" \\
                            -backend-config="key=seatmap-backend/terraform.tfstate" \\
                            -backend-config="region=${AWS_REGION}" \\
                            -backend-config="dynamodb_table=${LOCK_TABLE}"
                    """
                }
            }
        }
        
        stage('Build Application') {
            when {
                anyOf {
                    expression { params.ACTION == 'plan' }
                    expression { params.ACTION == 'apply' }
                }
            }
            steps {
                sh """
                    echo "Building and testing application for ${params.ENVIRONMENT}..."
                    
                    # Run tests and build
                    ./gradlew clean test
                    ./gradlew buildLambda
                    
                    # Copy JAR to terraform directory for deployment
                    mkdir -p terraform/lambda-artifacts
                    cp build/libs/seatmap-backend-1.0.0.jar terraform/lambda-artifacts/
                """
            }
        }
        
        stage('Terraform Plan') {
            when {
                anyOf {
                    expression { params.ACTION == 'plan' }
                    expression { params.ACTION == 'apply' }
                }
            }
            steps {
                dir('terraform') {
                    sh """
                        echo "Running Terraform plan for ${params.ENVIRONMENT}..."
                        terraform plan \\
                            -var="environment=${params.ENVIRONMENT}" \\
                            -var="aws_region=${AWS_REGION}" \\
                            -out=terraform.tfplan
                    """
                }
            }
        }
        
        stage('Terraform Apply') {
            when {
                expression { params.ACTION == 'apply' }
            }
            steps {
                dir('terraform') {
                    sh """
                        echo "Applying Terraform changes for ${params.ENVIRONMENT}..."
                        terraform apply -auto-approve terraform.tfplan
                        
                        # Display outputs
                        terraform output
                    """
                }
            }
        }
        
        stage('Terraform Destroy') {
            when {
                expression { params.ACTION == 'destroy' }
            }
            steps {
                dir('terraform') {
                    sh """
                        echo "Destroying all resources in ${params.ENVIRONMENT} environment..."
                        echo "WARNING: This will destroy all resources!"
                        
                        terraform destroy -auto-approve \\
                            -var="environment=${params.ENVIRONMENT}" \\
                            -var="aws_region=${AWS_REGION}"
                    """
                }
            }
        }
    }
    
    post {
        always {
            // Clean up plan files
            dir('terraform') {
                sh 'rm -f *.tfplan || true'
            }
        }
        
        success {
            echo "Terraform ${params.ACTION} completed successfully for ${params.ENVIRONMENT} environment"
        }
        
        failure {
            echo "Terraform ${params.ACTION} failed for ${params.ENVIRONMENT} environment"
        }
    }
}
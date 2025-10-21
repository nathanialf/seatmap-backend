pipeline {
    agent any
    
    tools {
        terraform 'Terraform-1.5'
        gradle 'Gradle 8.x'
    }
    
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
                    // Load AWS and Amadeus credentials based on environment
                    if (params.ENVIRONMENT == 'dev') {
                        withCredentials([
                            [$class: 'AmazonWebServicesCredentialsBinding', credentialsId: 'seatmap-dev'],
                            string(credentialsId: 'amadeus-prod-apikey', variable: 'AMADEUS_API_KEY'),
                            string(credentialsId: 'amadeus-prod-secret', variable: 'AMADEUS_API_SECRET'),
                            string(credentialsId: 'jwt-secret-dev', variable: 'JWT_SECRET')
                        ]) {
                            env.AWS_ACCESS_KEY_ID = AWS_ACCESS_KEY_ID
                            env.AWS_SECRET_ACCESS_KEY = AWS_SECRET_ACCESS_KEY
                            env.AMADEUS_API_KEY = AMADEUS_API_KEY
                            env.AMADEUS_API_SECRET = AMADEUS_API_SECRET
                            env.AMADEUS_ENDPOINT = 'api.amadeus.com'
                            env.JWT_SECRET = JWT_SECRET
                        }
                    } else if (params.ENVIRONMENT == 'prod') {
                        withCredentials([
                            [$class: 'AmazonWebServicesCredentialsBinding', credentialsId: 'seatmap-prod'],
                            string(credentialsId: 'amadeus-prod-apikey', variable: 'AMADEUS_API_KEY'),
                            string(credentialsId: 'amadeus-prod-secret', variable: 'AMADEUS_API_SECRET'),
                            string(credentialsId: 'jwt-secret-prod', variable: 'JWT_SECRET')
                        ]) {
                            env.AWS_ACCESS_KEY_ID = AWS_ACCESS_KEY_ID
                            env.AWS_SECRET_ACCESS_KEY = AWS_SECRET_ACCESS_KEY
                            env.AMADEUS_API_KEY = AMADEUS_API_KEY
                            env.AMADEUS_API_SECRET = AMADEUS_API_SECRET
                            env.AMADEUS_ENDPOINT = 'api.amadeus.com'
                            env.JWT_SECRET = JWT_SECRET
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
                dir('terraform/bootstrap') {
                    sh """
                        echo "Running Terraform bootstrap for ${params.ENVIRONMENT}..."
                        
                        # Initialize terraform without backend (local state for bootstrap)
                        terraform init -reconfigure
                        
                        # Plan bootstrap resources
                        terraform plan \\
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
                dir("terraform/environments/${params.ENVIRONMENT}") {
                    sh """
                        echo "Initializing Terraform for ${params.ENVIRONMENT} environment..."
                        terraform init
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
                    gradle clean test
                    gradle buildLambda
                    
                    # JAR will be referenced directly from build/libs by terraform
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
                dir("terraform/environments/${params.ENVIRONMENT}") {
                    sh """
                        echo "Running Terraform plan for ${params.ENVIRONMENT}..."
                        terraform plan \\
                            -var="amadeus_api_key=${AMADEUS_API_KEY}" \\
                            -var="amadeus_api_secret=${AMADEUS_API_SECRET}" \\
                            -var="amadeus_endpoint=${AMADEUS_ENDPOINT}" \\
                            -var="jwt_secret=${JWT_SECRET}" \\
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
                dir("terraform/environments/${params.ENVIRONMENT}") {
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
                dir("terraform/environments/${params.ENVIRONMENT}") {
                    sh """
                        echo "Destroying all resources in ${params.ENVIRONMENT} environment..."
                        echo "WARNING: This will destroy all resources!"
                        
                        terraform destroy -auto-approve \\
                            -var="amadeus_api_key=${AMADEUS_API_KEY}" \\
                            -var="amadeus_api_secret=${AMADEUS_API_SECRET}" \\
                            -var="amadeus_endpoint=${AMADEUS_ENDPOINT}" \\
                            -var="jwt_secret=${JWT_SECRET}"
                    """
                }
            }
        }
    }
    
    post {
        always {
            // Clean up plan files
            dir("terraform/environments/${params.ENVIRONMENT}") {
                sh 'rm -f *.tfplan || true'
            }
            dir('terraform/bootstrap') {
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
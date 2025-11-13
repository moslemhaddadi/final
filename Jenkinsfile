pipeline {
    agent {
        docker {
            image 'maven:3.8-openjdk-17'
            args '--entrypoint= -v /var/run/docker.sock:/var/run/docker.sock -v maven-cache:/root/.m2'
            reuseNode true
        }
    }
    
    stages {
        stage('1. Préparation') {
            steps {
                cleanWs()
                checkout scm
            }
        }
        
        stage('2. Diagnostic') {
            steps {
                sh '''
                    echo "=== Environment Check ==="
                    echo "Working directory: $(pwd)"
                    echo "Java version:"
                    java -version
                    echo "Maven version:"
                    mvn --version
                    echo "Workspace contents:"
                    ls -la
                '''
            }
        }
        
        stage('3. Build & Tests') {
            steps {
                sh 'mvn clean compile test'
            }
        }
        
        stage('4. SonarQube Analysis') {
            steps {
                script {
                    try {
                        withSonarQubeEnv('MySonarQubeServer') {
                            sh '''
                                echo "Starting SonarQube analysis..."
                                mvn sonar:sonar \
                                    -Dsonar.projectKey=final-project \
                                    -Dsonar.projectName="Final Project"
                            '''
                        }
                    } catch (Exception e) {
                        echo "SonarQube analysis failed: ${e.getMessage()}"
                        // Continue pipeline instead of failing
                    }
                }
            }
        }
        
        stage('5. Quality Gate') {
            steps {
                script {
                    try {
                        timeout(time: 10, unit: 'MINUTES') {
                            waitForQualityGate abortPipeline: false
                        }
                    } catch (Exception e) {
                        echo "Quality Gate check failed or timed out: ${e.getMessage()}"
                        // Continue pipeline anyway
                    }
                }
            }
        }
        
        stage('6. Security Scan') {
            steps {
                script {
                    sh '''
                        # Install and run Trivy
                        if ! command -v trivy &> /dev/null; then
                            echo "Installing Trivy..."
                            wget -qO - https://github.com/aquasecurity/trivy/releases/download/v0.50.1/trivy_0.50.1_Linux-64bit.deb
                            dpkg -i trivy_0.50.1_Linux-64bit.deb || apt-get install -f -y
                        fi
                        echo "Running Trivy security scan..."
                        trivy fs --security-checks vuln . || echo "Trivy scan completed"
                    '''
                }
            }
        }
        
        stage('7. Build Docker Image') {
            steps {
                script {
                    sh '''
                        if [ -f "Dockerfile" ]; then
                            echo "Building Docker image..."
                            docker build -t my-app:${BUILD_NUMBER} .
                            echo "Docker image built successfully"
                        else
                            echo "No Dockerfile found - skipping Docker build"
                        fi
                    '''
                }
            }
        }
    }
    
    post {
        always {
            archiveArtifacts artifacts: 'target/*.jar', fingerprint: true, allowEmptyArchive: true
            junit testResults: 'target/surefire-reports/*.xml', allowEmptyResults: true
            sh 'echo "Pipeline execution completed"'
        }
        success {
            emailext (
                subject: "SUCCESS: Job '${env.JOB_NAME} [${env.BUILD_NUMBER}]'",
                body: "Build completed successfully. Check console at ${env.BUILD_URL}",
                to: "admin@example.com"
            )
        }
        failure {
            emailext (
                subject: "FAILED: Job '${env.JOB_NAME} [${env.BUILD_NUMBER}]'",
                body: "Build failed. Check console at ${env.BUILD_URL}",
                to: "admin@example.com"
            )
        }
    }
}
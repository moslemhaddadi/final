pipeline {
    agent {
        docker {
            image 'maven:3.8-openjdk-17'
            args '-v /var/run/docker.sock:/var/run/docker.sock -v maven-cache:/root/.m2'
        }
    }
    
    stages {
        stage('1. Préparation') {
            steps {
                cleanWs()
                checkout scm
            }
        }
        
        stage('2. Analyse Statique (SAST & SCA)') {
            parallel {
                stage('Build, Test & SAST (SonarQube)') {
                    steps {
                        withSonarQubeEnv('MySonarQubeServer') {
                            sh '''
                                mvn clean verify sonar:sonar \
                                    -Dsonar.projectKey=my-project \
                                    -Dsonar.host.url=${SONAR_HOST_URL} \
                                    -Dsonar.login=${SONAR_AUTH_TOKEN}
                            '''
                        }
                    }
                }
                
                stage('Analyse des dépendances (Trivy FS)') {
                    steps {
                        script {
                            // Install Trivy if not available
                            sh 'which trivy || (apt-get update && apt-get install -y wget && wget -qO - https://aquasecurity.github.io/trivy-repo/deb/public.key | apt-key add - && echo "deb https://aquasecurity.github.io/trivy-repo/deb $(lsb_release -sc) main" | tee -a /etc/apt/sources.list.d/trivy.list && apt-get update && apt-get install -y trivy)'
                            
                            // Run Trivy filesystem scan
                            sh 'trivy fs --security-checks vuln,config .'
                        }
                    }
                }
            }
        }
        
        stage('3. Quality Gate SonarQube') {
            steps {
                timeout(time: 1, unit: 'HOURS') {
                    waitForQualityGate abortPipeline: true
                }
            }
        }
        
        stage('4. Build & Scan de l\'image Docker') {
            steps {
                script {
                    // Build Docker image
                    sh 'docker build -t my-app:${BUILD_NUMBER} .'
                    
                    // Scan with Trivy
                    sh 'trivy image --exit-code 0 --severity HIGH,CRITICAL my-app:${BUILD_NUMBER}'
                }
            }
        }
    }
    
    post {
        always {
            archiveArtifacts artifacts: 'target/*.jar', fingerprint: true
            junit 'target/surefire-reports/*.xml'
        }
        failure {
            emailext (
                subject: "FAILED: Job '${env.JOB_NAME} [${env.BUILD_NUMBER}]'",
                body: "Check console output at ${env.BUILD_URL}",
                to: "admin@example.com"
            )
        }
    }
}
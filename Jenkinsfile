pipeline {
    agent any
    
    environment {
        SONAR_TOKEN = credentials('sonar-auth-token')
    }
    
    stages {
        stage('1. Préparation') {
            steps {
                cleanWs()
                checkout scm
                sh 'echo "Pipeline DevSecOps démarré"'
            }
        }
        
        stage('2. SAST - SonarQube') {
            steps {
                withSonarQubeEnv('MySonarQubeServer') {
                    sh 'mvn clean verify sonar:sonar -Dsonar.projectKey=final-project'
                }
            }
        }
        
        stage('3. SCA - Analyse Dépendances') {
            steps {
                sh '''
                    # Installation et scan Trivy
                    curl -sfL https://raw.githubusercontent.com/aquasecurity/trivy/main/contrib/install.sh | sh -s -- -b /usr/local/bin
                    mkdir -p reports
                    trivy fs . --format table --exit-code 0
                '''
            }
        }
        
        stage('4. Détection Secrets') {
            steps {
                sh '''
                    wget https://github.com/gitleaks/gitleaks/releases/download/v8.18.4/gitleaks_8.18.4_linux_x64.tar.gz
                    tar -xzf gitleaks_8.18.4_linux_x64.tar.gz
                    ./gitleaks detect --source . --exit-code 0
                '''
            }
        }
        
        stage('5. Quality Gate') {
            steps {
                timeout(time: 5, unit: 'MINUTES') {
                    waitForQualityGate abortPipeline: true
                }
            }
        }
    }
    
    post {
        always {
            junit testResults: '**/surefire-reports/*.xml', allowEmptyResults: true
            archiveArtifacts artifacts: 'reports/**/*', allowEmptyArchive: true
        }
        success {
            emailext (
                subject: "SUCCÈS: ${env.JOB_NAME} [${env.BUILD_NUMBER}]",
                body: "Pipeline DevSecOps réussi: ${env.BUILD_URL}",
                to: "admin@example.com"
            )
        }
        failure {
            emailext (
                subject: "ÉCHEC: ${env.JOB_NAME} [${env.BUILD_NUMBER}]",
                body: "Pipeline DevSecOps échoué: ${env.BUILD_URL}",
                to: "admin@example.com"
            )
        }
    }
}
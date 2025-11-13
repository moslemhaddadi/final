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
        
        stage('2. Analyse Statique (SAST & SCA)') {
            parallel {
                stage('Build, Test & SAST (SonarQube)') {
                    steps {
                        withSonarQubeEnv('MySonarQubeServer') {
                            sh 'mvn clean verify sonar:sonar -Dsonar.projectKey=final-project'
                        }
                    }
                }
                
                stage('Analyse des dépendances (Trivy FS)') {
                    steps {
                        script {
                            sh '''
                                # Install trivy if not present
                                if ! command -v trivy &> /dev/null; then
                                    wget https://github.com/aquasecurity/trivy/releases/download/v0.50.1/trivy_0.50.1_Linux-64bit.deb
                                    dpkg -i trivy_0.50.1_Linux-64bit.deb
                                fi
                                
                                # Run trivy filesystem scan
                                trivy fs --security-checks vuln . || echo "Trivy scan completed with findings"
                            '''
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
        
        stage('5. Déploiement pour le test DAST') {
            steps {
                script {
                    // Déploiement pour les tests DAST
                    sh 'docker run -d --name test-app -p 8080:8080 my-app:${BUILD_NUMBER}'
                    sh 'sleep 30'
                }
            }
        }
        
        stage('6. Scan Dynamique (DAST avec OWASP ZAP)') {
            steps {
                script {
                    // Installation et execution de ZAP
                    sh '''
                        if ! command -v zap-baseline.py &> /dev/null; then
                            apt-get update && apt-get install -y wget
                            wget -qO - https://github.com/zaproxy/zaproxy/releases/download/v2.14.0/ZAP_2.14.0_Linux.tar.gz | tar -xz
                        fi
                        # Scan DAST avec ZAP
                        zap-baseline.py -t http://localhost:8080 -I || echo "ZAP scan completed"
                    '''
                }
            }
        }
    }
    
    post {
        always {
            archiveArtifacts artifacts: 'target/*.jar', fingerprint: true, allowEmptyArchive: true
            junit 'target/surefire-reports/*.xml', allowEmptyResults: true
            
            // Nettoyage des conteneurs
            sh '''
                docker stop test-app || true
                docker rm test-app || true
            '''
        }
        success {
            emailext (
                subject: "SUCCESS: Job '${env.JOB_NAME} [${env.BUILD_NUMBER}]'",
                body: """
                Bonjour,

                La pipeline CI/CD a été exécutée avec succès.

                Détails:
                - Job: ${env.JOB_NAME}
                - Build: ${env.BUILD_NUMBER}
                - URL: ${env.BUILD_URL}

                Cordialement,
                Jenkins
                """,
                to: "admin@example.com"
            )
        }
        failure {
            emailext (
                subject: "FAILED: Job '${env.JOB_NAME} [${env.BUILD_NUMBER}]'",
                body: """
                Bonjour,

                La pipeline CI/CD a échoué.

                Détails:
                - Job: ${env.JOB_NAME}
                - Build: ${env.BUILD_NUMBER}
                - URL: ${env.BUILD_URL}

                Veuillez vérifier les logs pour plus de détails.

                Cordialement,
                Jenkins
                """,
                to: "admin@example.com"
            )
        }
        unstable {
            emailext (
                subject: "UNSTABLE: Job '${env.JOB_NAME} [${env.BUILD_NUMBER}]'",
                body: """
                Bonjour,

                La pipeline CI/CD est instable.

                Détails:
                - Job: ${env.JOB_NAME}
                - Build: ${env.BUILD_NUMBER}
                - URL: ${env.BUILD_URL}

                Cordialement,
                Jenkins
                """,
                to: "admin@example.com"
            )
        }
    }
}
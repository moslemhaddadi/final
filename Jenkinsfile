// Jenkinsfile - Version Finale, Optimisée et Corrigée

pipeline {
    agent {
        docker {
            image 'maven:3.8-openjdk-17' 
            args '--user 0:0 -v /var/run/docker.sock:/var/run/docker.sock -v maven-cache:/root/.m2'
        }
    }

    environment {
        // CORRECTION : Nom du token plus explicite.
        // Assurez-vous que l'ID de votre secret dans Jenkins est EXACTEMENT 'SONAR_AUTH_TOKEN'
        SONAR_TOKEN_ID = 'SONAR_AUTH_TOKEN' 
        DOCKER_IMAGE_NAME = "votre-nom-user/mon-app-final"
    }

    stages {
        stage('1. Préparation') {
            steps {
                cleanWs()
                // Utilisation de 'checkout scm' pour garantir que le code est à la racine du workspace
                checkout scm
            }
        }

        stage('2. Analyse Statique (SAST & SCA)') {
            parallel {
                stage('Build, Test & SAST (SonarQube)') {
                    steps {
                        // CORRECTION : Nom du serveur plus explicite.
                        // Assurez-vous que le nom de votre serveur dans Jenkins est EXACTEMENT 'MySonarQubeServer'
                        withSonarQubeEnv('MySonarQubeServer') {
                            sh 'mvn clean package sonar:sonar -Dsonar.projectKey=mon-projet-final -Dsonar.login=${SONAR_TOKEN_ID}'
                        }
                    }
                }
                stage('Analyse des dépendances (Trivy FS)') {
                    steps {
                        sh "docker run --rm -v ${env.WORKSPACE}:/app aquasec/trivy:latest fs --severity HIGH,CRITICAL --exit-code 0 /app > trivy-fs-report.txt"
                    }
                }
            }
        }

        stage('3. Quality Gate SonarQube') {
            steps {
                timeout(time: 5, unit: 'MINUTES') {
                    waitForQualityGate abortPipeline: true, credentialsId: SONAR_TOKEN_ID
                }
            }
        }

        stage('4. Build & Scan de l\'image Docker') {
            steps {
                script {
                    def dockerImage = docker.build("${DOCKER_IMAGE_NAME}:${env.BUILD_NUMBER}", ".")
                    
                    sh "docker run --rm -v ${env.WORKSPACE}:/app aquasec/trivy:latest image --severity HIGH,CRITICAL --exit-code 1 ${DOCKER_IMAGE_NAME}:${env.BUILD_NUMBER} > trivy-image-report.txt"
                }
            }
        }

        stage('5. Déploiement pour le test DAST') {
            steps {
                script {
                    sh "docker rm -f mon-app-test || true"
                    sh "docker run -d --name mon-app-test -p 8080:8080 ${DOCKER_IMAGE_NAME}:${env.BUILD_NUMBER}"
                    echo "Attente de 45 secondes que l'application démarre..."
                    sleep 45
                }
            }
        }

        stage('6. Scan Dynamique (DAST avec OWASP ZAP)') {
            steps {
                sh '''
                    docker run --rm --network host -v $(pwd):/zap/wrk:rw \
                    zaproxy/zap-stable zap-baseline.py \
                    -t http://localhost:8080 \
                    -r zap_report.html
                '''
            }
        }
    }

    post {
        always {
            archiveArtifacts artifacts: 'trivy-fs-report.txt, zap_report.html, target/surefire-reports/*.xml', allowEmptyArchive: true
            sh "docker rm -f mon-app-test || true"
        }
    }
}

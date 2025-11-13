// Jenkinsfile - Version Corrigée (avec agent root)

pipeline {
    agent {
        docker {
            // CORRECTION MAJEURE : Ajout de 'user: 0:0' pour forcer l'exécution en tant que root.
            // Cela résout le problème de "Permission denied" sur les fichiers du workspace.
            image 'maven:3.8-openjdk-17' 
            user '0:0' 
            args '-v /var/run/docker.sock:/var/run/docker.sock -v maven-cache:/root/.m2'
        }
    }

    environment {
        SONAR_TOKEN_ID = 'sonar-token'
        DOCKER_IMAGE_NAME = "votre-nom-user/mon-app-final"
    }

    stages {
        stage('1. Préparation') {
            steps {
                cleanWs()
                git branch: 'main', url: 'https://github.com/moslemhaddadi/final.git'
            }
        }

        stage('2. Analyse Statique (SAST & SCA )') {
            parallel {
                stage('Build, Test & SAST (SonarQube)') {
                    steps {
                        withSonarQubeEnv('sonar-server') {
                            sh 'mvn clean package sonar:sonar -Dsonar.projectKey=mon-projet-final -Dsonar.login=${SONAR_TOKEN_ID}'
                        }
                    }
                }
                stage('Analyse des dépendances (Trivy FS)') {
                    steps {
                        // CORRECTION : Utilisation de 'docker run' pour Trivy, car le client n'est pas dans l'image Maven.
                        // On monte le workspace dans le conteneur Trivy.
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
                    // Le client Docker est maintenant accessible car nous avons monté le socket Docker
                    def dockerImage = docker.build("${DOCKER_IMAGE_NAME}:${env.BUILD_NUMBER}", ".")
                    
                    // Scan de l'image construite
                    sh "docker run --rm -v ${env.WORKSPACE}:/app aquasec/trivy:latest image --severity HIGH,CRITICAL --exit-code 1 ${DOCKER_IMAGE_NAME}:${env.BUILD_NUMBER} > trivy-image-report.txt"
                }
            }
        }

        // ... Le reste des étapes (5 et 6) ...
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

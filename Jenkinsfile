// Jenkinsfile - VERSION CORRIGÉE (sans les blocs 'node' dans 'post')
pipeline {
    agent any

    environment {
        SONAR_PROJECT_KEY = "mon-projet-devops"
        SONAR_HOST_URL = "http://localhost:9000"
        SONAR_LOGIN = credentials('sonarqube-auth-token' ) 
    }

    stages {
        stage("1. Checkout Code from GitHub") {
            steps {
                echo "Récupération du code depuis GitHub..."
                git url: "https://github.com/moslemhaddadi/DEVOPS.git", branch: "main"
            }
        }

        stage("2. SAST with SonarQube" ) {
            steps {
                echo "Lancement de l'analyse SAST avec SonarQube..."
                sh """
                    mvn clean verify sonar:sonar \
                        -Dsonar.projectKey=${SONAR_PROJECT_KEY} \
                        -Dsonar.host.url=${SONAR_HOST_URL} \
                        -Dsonar.login=${SONAR_LOGIN}
                """
            }
        }

        stage("3. Quality Gate") {
            steps {
                echo "Vérification du Quality Gate de SonarQube..."
                timeout(time: 5, unit: 'MINUTES') {
                    waitForQualityGate abortPipeline: true
                }
            }
        }
        
        stage("4. SCA - Trivy Scan") {
            steps {
                echo "Lancement de l'analyse des dépendances (SCA) avec Trivy..."
                sh "docker run --rm -v ${env.WORKSPACE}:/path aquasec/trivy:latest fs --format table -o trivy-fs-report.html /path"
                archiveArtifacts artifacts: 'trivy-fs-report.html', allowEmptyArchive: true
            }
        }
        
        stage("5. Build Docker Image") {
            steps {
                echo "Construction de l'image Docker de l'application..."
                sh "docker build -t mon-app:latest ."
            }
        }

        stage("6. DAST with OWASP ZAP (Simulation)") {
            steps {
                echo "Lancement de l'analyse de sécurité dynamique (DAST)..."
                sh """
                    echo "Démarrage de l'application pour le scan DAST..."
                    docker run -d --name app-for-dast mon-app:latest
                    
                    echo "Lancement du scan ZAP..."
                    docker run --rm -v \${env.WORKSPACE}:/zap/wrk/:rw --network host owasp/zap2docker-stable zap-baseline.py \
                        -t http://127.0.0.1:8080 -g gen.conf -r zap-report.html
                    
                    echo "Arrêt du conteneur de l'application..."
                    docker stop app-for-dast
                    docker rm app-for-dast
                """
                archiveArtifacts artifacts: 'zap-report.html', allowEmptyArchive: true
            }
        }
    }

    post {
        // Le bloc 'node' a été retiré. Ces étapes s'exécuteront
        // dans le contexte de l'agent défini en haut du pipeline.
        always {
            echo 'Pipeline terminé.'
            cleanWs( )
        }
        success {
            echo '>>> Le pipeline a réussi toutes les étapes ! <<<'
        }
        failure {
            echo "Le pipeline a échoué à une des étapes."
        }
    }
}

// Jenkinsfile - VERSION FINALE ET COMPLÈTE
pipeline {
    agent any

    environment {
        // Clé de projet unique pour SonarQube. Utiliser le nom du job est une bonne pratique.
        SONAR_PROJECT_KEY = "${env.JOB_NAME}" 
    }

    stages {
        stage("1. SAST Analysis & Quality Gate") {
            steps {
                // !!! ATTENTION : REMPLACER 'simple-app' par le nom exact de votre sous-dossier si nécessaire !!!
                dir('simple-app') { 
                    withSonarQubeEnv('MySonarQubeServer') {
                        echo "Lancement de l'analyse SAST avec SonarQube..."
                        sh "mvn clean verify sonar:sonar -Dsonar.projectKey=${env.SONAR_PROJECT_KEY}"

                        echo "Vérification du Quality Gate de SonarQube..."
                        timeout(time: 5, unit: 'MINUTES') {
                            waitForQualityGate abortPipeline: true
                        }
                    }
                }
            }
        }
        
        stage("2. SCA - Trivy Scan") {
            steps {
                // !!! ATTENTION : REMPLACER 'simple-app' par le nom exact de votre sous-dossier si nécessaire !!!
                dir('simple-app') {
                    echo "Lancement de l'analyse des dépendances (SCA) avec Trivy..."
                    // Le scan Trivy s'exécute sur le répertoire courant (simple-app)
                    sh "docker run --rm -v ${env.WORKSPACE}/simple-app:/path aquasec/trivy:latest fs --format table -o trivy-fs-report.html /path"
                    archiveArtifacts artifacts: 'trivy-fs-report.html', allowEmptyArchive: true
                }
            }
        }
        
        stage("3. Build Docker Image") {
            steps {
                // !!! ATTENTION : REMPLACER 'simple-app' par le nom exact de votre sous-dossier si nécessaire !!!
                dir('simple-app') {
                    echo "Construction de l'image Docker de l'application..."
                    // Le build s'exécute dans le répertoire simple-app
                    sh "docker build -t simple-app:latest ."
                }
            }
        }

        stage("4. DAST with OWASP ZAP (Simulation)") {
            steps {
                echo "Lancement de l'analyse de sécurité dynamique (DAST)..."
                sh """
                    echo "Démarrage de l'application pour le scan DAST..."
                    docker run -d --name app-for-dast simple-app:latest
                    
                    echo "Lancement du scan ZAP..."
                    docker run --rm -v \${env.WORKSPACE}/simple-app:/zap/wrk/:rw --network host owasp/zap2docker-stable zap-baseline.py \
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

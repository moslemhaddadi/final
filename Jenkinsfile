// Jenkinsfile - VERSION DÉFINITIVE ET NETTOYÉE
pipeline {
    agent any

    // On redéfinit la clé du projet ici pour qu'elle soit disponible dans tout le pipeline.
    environment {
        SONAR_PROJECT_KEY = "fromgitpipe-project" // Nom unique pour le projet SonarQube
    }

    stages {
        // Cette étape est maintenant propre et ne fait plus de checkout redondant.
        stage("1. Initialisation") {
            steps {
                echo "Workspace initialisé avec le code de la branche ${env.BRANCH_NAME}."
            }
        }

        stage("2. SAST Analysis & Quality Gate") {
            steps {
                withSonarQubeEnv('MySonarQubeServer') {
                    // 1. Lancer l'analyse en forçant la clé de projet définie ci-dessus.
                    echo "Lancement de l'analyse SAST avec SonarQube..."
                    sh "mvn clean verify sonar:sonar -Dsonar.projectKey=${env.SONAR_PROJECT_KEY}"

                    // 2. Attendre le Quality Gate.
                    echo "Vérification du Quality Gate de SonarQube..."
                    timeout(time: 5, unit: 'MINUTES') {
                        waitForQualityGate abortPipeline: true
                    }
                }
            }
        }
        
        stage("3. SCA - Trivy Scan") {
            steps {
                echo "Lancement de l'analyse des dépendances (SCA) avec Trivy..."
                sh "docker run --rm -v ${env.WORKSPACE}:/path aquasec/trivy:latest fs --format table -o trivy-fs-report.html /path"
                archiveArtifacts artifacts: 'trivy-fs-report.html', allowEmptyArchive: true
            }
        }
        
        stage("4. Build Docker Image") {
            steps {
                echo "Construction de l'image Docker de l'application..."
                sh "docker build -t mon-app:latest ."
            }
        }

        stage("5. DAST with OWASP ZAP (Simulation)") {
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

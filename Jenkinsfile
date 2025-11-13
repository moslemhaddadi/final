// Jenkinsfile - VERSION CORRIGÉE AVEC withSonarQubeEnv
pipeline {
    agent any

    // Le bloc 'environment' pour Sonar a été retiré.
    // 'withSonarQubeEnv' va gérer la configuration.

    stages {
        stage("1. Checkout Code from GitHub") {
            steps {
                echo "Récupération du code depuis GitHub..."
                // Note : Ce 'git' est redondant car Jenkins fait déjà un checkout au début.
                // On peut le garder, mais ce n'est pas strictement nécessaire.
                git url: "https://github.com/moslemhaddadi/DEVOPS.git", branch: "main"
            }
        }

        // On regroupe les étapes Sonar dans un seul stage pour plus de clarté.
        stage("2. SAST Analysis & Quality Gate" ) {
            steps {
                // 'SonarQube-Server' est le nom de votre configuration SonarQube
                // dans "Manage Jenkins > Configure System".
                withSonarQubeEnv('SonarQube-Server') {
                    // 1. Lancer l'analyse
                    echo "Lancement de l'analyse SAST avec SonarQube..."
                    sh "mvn clean verify sonar:sonar"

                    // 2. Attendre le Quality Gate (maintenant dans le même contexte)
                    echo "Vérification du Quality Gate de SonarQube..."
                    timeout(time: 5, unit: 'MINUTES') {
                        waitForQualityGate abortPipeline: true
                    }
                }
            }
        }
        
        stage("3. SCA - Trivy Scan") { // L'index des stages a été décalé
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

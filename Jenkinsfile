// Jenkinsfile - VERSION COMPLÈTE
pipeline {
    // L'agent 'any' est simple, mais pour des outils comme Docker,
    // un agent spécifique (ex: avec un label 'docker') est souvent nécessaire.
    agent any

    // Définition des outils qui seront utilisés dans le pipeline.
    // Assurez-vous que 'M3' et 'sonar' sont bien configurés dans "Global Tool Configuration" de Jenkins.
    tools {
        maven 'M3' // 'M3' est un exemple de nom pour votre configuration Maven
        jdk 'jdk11' // Assurez-vous d'avoir un JDK configuré avec ce nom
    }

    // Variables d'environnement pour le pipeline
    environment {
        // Nom du projet SonarQube, doit être unique
        SONAR_PROJECT_KEY = "mon-projet-devops"
        // URL de votre serveur SonarQube
        SONAR_HOST_URL = "http://localhost:9000" // Remplacez par l'URL de votre SonarQube
        // Token d'authentification SonarQube (à stocker dans les Credentials Jenkins )
        SONAR_LOGIN = credentials('sonar-token') // 'sonar-token' est l'ID de votre credential
    }

    stages {
        stage("1. Checkout Code from GitHub") {
            steps {
                echo "Récupération du code depuis GitHub..."
                git url: "https://github.com/moslemhaddadi/final.git", branch: "main"
            }
        }

        stage("2. SAST with SonarQube" ) {
            steps {
                echo "Lancement de l'analyse SAST avec SonarQube..."
                // Exécute l'analyse SonarQube avec Maven.
                // Les propriétés sont passées en ligne de commande.
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
                // Attend le résultat de l'analyse SonarQube.
                // Si le Quality Gate échoue, le pipeline sera stoppé.
                // Le timeout évite que le pipeline attende indéfiniment.
                timeout(time: 5, unit: 'MINUTES') {
                    waitForQualityGate abortPipeline: true
                }
            }
        }
        
        stage("4. SCA - Trivy Scan") {
            steps {
                echo "Lancement de l'analyse des dépendances (SCA) avec Trivy..."
                // Cette étape nécessite que Docker soit installé sur l'agent Jenkins.
                // Le conteneur Trivy monte le workspace Jenkins pour scanner les fichiers.
                // Le rapport est généré au format HTML.
                sh "docker run --rm -v ${env.WORKSPACE}:/path aquasec/trivy:latest fs --format table -o trivy-fs-report.html /path"
                
                // Archive le rapport pour qu'il soit accessible depuis la page du build.
                archiveArtifacts artifacts: 'trivy-fs-report.html', allowEmptyArchive: true
            }
        }
        
        stage("5. Build Docker Image") {
            steps {
                echo "Construction de l'image Docker de l'application..."
                // Assurez-vous d'avoir un Dockerfile dans votre projet.
                // Remplacez 'mon-app' par le nom souhaité pour votre image.
                sh "docker build -t mon-app:latest ."
            }
        }

        stage("6. DAST with OWASP ZAP (Simulation)") {
            steps {
                echo "Lancement de l'analyse de sécurité dynamique (DAST)..."
                // Cette étape est une simulation plus avancée.
                // 1. Démarre l'application dans un conteneur Docker.
                // 2. Lance le scanner ZAP contre l'application.
                // 3. Arrête le conteneur de l'application après le scan.
                // NOTE : Ceci est un exemple de base. Une vraie intégration DAST est plus complexe.
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
                // Archive le rapport DAST.
                archiveArtifacts artifacts: 'zap-report.html', allowEmptyArchive: true
            }
        }
    } // Fin du bloc 'stages'

    post {
        always {
            echo 'Pipeline terminé.'
            // Nettoie le workspace pour supprimer les fichiers temporaires.
            cleanWs( )
        }
        success {
            echo '>>> Le pipeline a réussi toutes les étapes ! <<<'
        }
        failure {
            echo "Le pipeline a échoué à une des étapes."
            // Vous pourriez ajouter ici des notifications (email, Slack, etc.).
        }
    } // Fin du bloc 'post'
}

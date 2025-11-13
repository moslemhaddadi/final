// Jenkinsfile - VERSION ALLÉGÉE POUR TEST RAPIDE
pipeline {
    agent any

    stages {
        stage("1. Checkout Code from GitHub") {
            steps {
                git url: "https://github.com/moslemhaddadi/final.git", branch: "main"
            }
        }

        stage("2. Build & Test (SAST désactivé )") {
            steps {
                echo "Étape SAST désactivée. Lancement d'un build Maven simple..."
                // On remplace l'analyse Sonar par un simple build pour aller plus vite.
                sh "mvn clean package"
            }
        }

        stage("3. Quality Gate (Désactivé)") {
            steps {
                echo "Étape Quality Gate désactivée pour le test rapide."
                // On commente l'étape qui attend SonarQube.
                // timeout(time: 1, unit: 'MINUTES') {
                //     waitForQualityGate abortPipeline: true
                // }
            }
        }
        
        stage("4. SCA - Trivy (Désactivé)") {
            steps {
                echo "Étape SCA (Trivy) désactivée pour le test rapide."
                // On commente le scan Trivy.
                // sh "docker run --rm -v ${env.WORKSPACE}:/path aquasec/trivy:latest fs --format table -o trivy-fs-report.html /path"
            }
        }
        
        // Les étapes suivantes sont également justes des 'echo' pour valider le flux.
        stage("5. Deploy to Staging (Simulation)") {
            steps {
                echo "Simulation de l'étape de déploiement."
            }
        }

        stage("6. DAST (Simulation)") {
            steps {
                echo "Simulation de l'étape DAST."
            }
        }
    } // Fin du bloc 'stages'

    post {
        always {
            echo 'Pipeline terminé.'
        }
        success {
            // Cette section ne s'exécutera que si toutes les étapes réussissent.
            echo '>>> Le pipeline allégé a réussi de bout en bout ! <<<'
        }
        failure {
            echo "Le pipeline allégé a échoué à une des étapes."
        }
    } // Fin du bloc 'post'
}

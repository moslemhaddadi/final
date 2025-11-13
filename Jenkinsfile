pipeline {
    agent any

    stages {
        stage('Build and Test') {
            steps {
                echo 'Building and testing Maven project...'
                // Exécute les étapes de compilation, test et packaging
                // Le "clean" est important pour s'assurer d'un build propre
                sh 'mvn clean package'
            }
        }

        stage('Quality Gate (Simulation)') {
            steps {
                echo 'Simulating Quality Gate check...'
                // Ici, vous ajouteriez des étapes pour SonarQube, etc.
                sh 'echo "Quality check passed (simulated)"'
            }
        }

        stage('Deploy (Simulation)') {
            steps {
                echo 'Simulating deployment to Staging...'
                // Ici, vous ajouteriez la logique de déploiement (ex: copier le JAR sur un serveur)
                sh 'echo "Deployment of target/simple-app-1.0-SNAPSHOT.jar simulated"'
            }
        }
    }

    post {
        success {
            echo 'Pipeline finished successfully!'
        }
        failure {
            echo 'Pipeline failed. Check the logs for errors.'
        }
    }
}

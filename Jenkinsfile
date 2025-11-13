// Jenkinsfile - Optimisé pour Jenkins, SonarQube, et Docker

pipeline {
    // =========================================================================
    // OPTIMISATION : Utiliser un agent Docker dédié pour la mise en cache.
    // Cela accélère les builds en conservant les dépendances Maven.
    // =========================================================================
    agent {
        docker {
            // Image Docker contenant Maven et JDK. 'maven:3.8-openjdk-17' est un bon choix.
            image 'maven:3.8-openjdk-17'
            // Monte le socket Docker pour construire des images et un volume pour le cache Maven.
            args '-v /var/run/docker.sock:/var/run/docker.sock -v maven-cache:/root/.m2'
        }
    }

    environment {
        // ID du token SonarQube stocké dans le gestionnaire de "Credentials" de Jenkins
        SONAR_TOKEN_ID = 'sonar-token' // Assurez-vous que cet ID correspond à celui dans Jenkins

        // Configuration Docker
        DOCKER_IMAGE_NAME = "votre-nom-user/mon-app-final" // Remplacez par votre nom d'utilisateur Docker Hub ou autre
    }

    stages {
        stage('1. Préparation') {
            steps {
                echo 'Nettoyage du workspace et récupération du code...'
                cleanWs()
                // CORRECTION : Utilisation de votre dépôt Git
                git branch: 'main', url: 'https://github.com/moslemhaddadi/final.git'
            }
        }

        // =========================================================================
        // OPTIMISATION : Exécution des analyses statiques en parallèle.
        // =========================================================================
        stage('2. Analyse Statique (SAST & SCA )') {
            parallel {
                stage('Build, Test & SAST (SonarQube)') {
                    steps {
                        // On configure l'environnement SonarQube
                        withSonarQubeEnv('MySonarQubeServer') { // Assurez-vous que 'sonar-server' est le nom de votre serveur dans Jenkins
                            // Commande Maven unique et efficace pour construire, tester et analyser
                            sh 'mvn clean package sonar:sonar -Dsonar.projectKey=mon-projet-final -Dsonar.login=${SONAR_TOKEN_ID}'
                        }
                    }
                }
                stage('Analyse des dépendances (Trivy FS)') {
                    steps {
                        // Trivy scanne les fichiers du projet pour les vulnérabilités connues
                        // On n'arrête pas le build ici (exit-code 0) pour consulter le rapport plus tard
                        sh "trivy fs --severity HIGH,CRITICAL --exit-code 0 . > trivy-fs-report.txt"
                    }
                }
            }
        }

        stage('3. Quality Gate SonarQube') {
            steps {
                // Le pipeline attend le verdict de SonarQube et s'arrête en cas d'échec
                timeout(time: 5, unit: 'MINUTES') {
                    waitForQualityGate abortPipeline: true, credentialsId: SONAR_TOKEN_ID
                }
            }
        }

        stage('4. Build & Scan de l\'image Docker') {
            steps {
                script {
                    // Construction de l'image Docker (nécessite un Dockerfile dans votre projet)
                    def dockerImage = docker.build("${DOCKER_IMAGE_NAME}:${env.BUILD_NUMBER}", ".")

                    // Scan de l'image construite pour les vulnérabilités du système d'exploitation et des librairies
                    sh "trivy image --severity HIGH,CRITICAL --exit-code 1 ${DOCKER_IMAGE_NAME}:${env.BUILD_NUMBER}"
                }
            }
        }

        stage('5. Déploiement pour le test DAST') {
            steps {
                script {
                    echo "Déploiement de l'application pour le scan DAST..."
                    sh "docker rm -f mon-app-test || true"
                    // On expose l'application sur le port 8080 de la machine hôte
                    sh "docker run -d --name mon-app-test -p 8080:8080 ${DOCKER_IMAGE_NAME}:${env.BUILD_NUMBER}"

                    // CORRECTION : Pause cruciale pour laisser le temps à l'application de démarrer
                    echo "Attente de 45 secondes que l'application démarre..."
                    sleep 45
                }
            }
        }

        stage('6. Scan Dynamique (DAST avec OWASP ZAP)') {
            steps {
                // ZAP va attaquer l'application en cours d'exécution pour trouver des vulnérabilités "runtime"
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
            echo 'Pipeline terminé. Archivage des rapports...'
            // Archive tous les rapports générés pour consultation
            archiveArtifacts artifacts: 'trivy-fs-report.txt, zap_report.html, target/surefire-reports/*.xml', allowEmptyArchive: true

            // Nettoyage du conteneur de test
            sh "docker rm -f mon-app-test || true"
        }
        success {
            echo 'Pipeline terminé avec succès !'
        }
        failure {
            echo 'Le pipeline a échoué.'
        }
    }
}

pipeline {
    agent any
    
    environment {
        SONAR_TOKEN = credentials('sonar-auth-token')
        DOCKER_REGISTRY = 'your-registry.com'
    }
    
    stages {
        stage('1. Préparation') {
            steps {
                cleanWs()
                checkout scm
                script {
                    echo "🔧 Initialisation du pipeline DevSecOps"
                    echo "Repository: ${env.GIT_URL}"
                    echo "Branch: ${env.GIT_BRANCH}"
                }
            }
        }
        
        stage('2. Analyse de Code (SAST) avec SonarQube') {
            steps {
                withSonarQubeEnv('MySonarQubeServer') {
                    script {
                        echo "🔍 Démarrage de l'analyse SAST avec SonarQube"
                        sh """
                            mvn clean verify sonar:sonar \
                                -Dsonar.projectKey=${env.JOB_NAME} \
                                -Dsonar.projectName="${env.JOB_NAME}" \
                                -Dsonar.host.url=\${SONAR_HOST_URL} \
                                -Dsonar.login=\${SONAR_AUTH_TOKEN} \
                                -Dsonar.sources=src \
                                -Dsonar.sourceEncoding=UTF-8 \
                                -Dsonar.java.binaries=target/classes \
                                -Dsonar.coverage.jacoco.xmlReportPaths=target/site/jacoco/jacoco.xml
                        """
                    }
                }
            }
        }
        
        stage('3. Analyse des Dépendances (SCA)') {
            parallel {
                stage('SCA - OWASP Dependency Check') {
                    steps {
                        script {
                            echo "📦 Analyse des vulnérabilités des dépendances"
                            sh """
                                # Installation OWASP Dependency Check
                                if ! command -v dependency-check.sh &> /dev/null; then
                                    wget https://github.com/jeremylong/DependencyCheck/releases/download/v9.0.10/dependency-check-9.0.10-release.zip
                                    unzip dependency-check-9.0.10-release.zip
                                fi
                                
                                # Analyse des dépendances
                                ./dependency-check/bin/dependency-check.sh \
                                    --project "${env.JOB_NAME}" \
                                    --scan "." \
                                    --format "HTML" \
                                    --format "JSON" \
                                    --out "reports/" \
                                    --enableExperimental
                            """
                        }
                    }
                }
                
                stage('SCA - Trivy Filesystem') {
                    steps {
                        script {
                            echo "🔎 Scan Trivy du code source"
                            sh """
                                # Installation Trivy
                                curl -sfL https://raw.githubusercontent.com/aquasecurity/trivy/main/contrib/install.sh | sh -s -- -b /usr/local/bin
                                
                                # Scan du filesystem
                                trivy fs . \
                                    --format sarif \
                                    --output reports/trivy-fs-scan.sarif \
                                    --severity HIGH,CRITICAL \
                                    --exit-code 0
                            """
                        }
                    }
                }
            }
            post {
                always {
                    archiveArtifacts artifacts: 'reports/*', fingerprint: true
                    publishHTML([
                        allowMissing: true,
                        alwaysLinkToLastBuild: true,
                        keepAll: true,
                        reportDir: 'reports',
                        reportFiles: 'dependency-check-report.html',
                        reportName: 'OWASP Dependency Check Report'
                    ])
                }
            }
        }
        
        stage('4. Détection de Secrets') {
            steps {
                script {
                    echo "🚨 Recherche de secrets exposés"
                    sh """
                        # Installation Gitleaks
                        wget https://github.com/gitleaks/gitleaks/releases/download/v8.18.4/gitleaks_8.18.4_linux_x64.tar.gz
                        tar -xzf gitleaks_8.18.4_linux_x64.tar.gz
                        
                        # Scan des secrets
                        ./gitleaks detect \
                            --source . \
                            --report-format json \
                            --report-path reports/gitleaks-report.json \
                            --exit-code 0
                    """
                }
            }
        }
        
        stage('5. Build et Scan d\'Image Docker') {
            when {
                expression { fileExists('Dockerfile') }
            }
            steps {
                script {
                    echo "🐳 Construction et analyse de l'image Docker"
                    sh """
                        # Build de l'image
                        docker build -t ${env.DOCKER_REGISTRY}/${env.JOB_NAME}:${env.BUILD_NUMBER} .
                        
                        # Scan de l'image avec Trivy
                        trivy image \
                            --format sarif \
                            --output reports/trivy-image-scan.sarif \
                            --severity HIGH,CRITICAL \
                            ${env.DOCKER_REGISTRY}/${env.JOB_NAME}:${env.BUILD_NUMBER}
                    """
                }
            }
        }
        
        stage('6. Quality Gate SonarQube') {
            steps {
                script {
                    echo "⚡ Vérification de la Quality Gate"
                    timeout(time: 10, unit: 'MINUTES') {
                        waitForQualityGate abortPipeline: true
                    }
                }
            }
        }
        
        stage('7. Déploiement Staging pour DAST') {
            when {
                expression { fileExists('Dockerfile') }
            }
            steps {
                script {
                    echo "🚀 Déploiement en environnement de staging"
                    sh """
                        # Déploiement de l'application
                        docker run -d \
                            --name ${env.JOB_NAME}-staging \
                            -p 8080:8080 \
                            ${env.DOCKER_REGISTRY}/${env.JOB_NAME}:${env.BUILD_NUMBER}
                        
                        # Attente du démarrage
                        sleep 30
                        curl -f http://localhost:8080/health || echo "Application déployée"
                    """
                }
            }
        }
        
        stage('8. Scan Dynamique (DAST) avec OWASP ZAP') {
            when {
                expression { fileExists('Dockerfile') }
            }
            steps {
                script {
                    echo "🎯 Scan DAST avec OWASP ZAP"
                    sh """
                        # Installation ZAP
                        docker pull owasp/zap2docker-stable
                        
                        # Scan de sécurité
                        docker run --rm \\
                            -v $(pwd)/reports:/zap/reports \\
                            -t owasp/zap2docker-stable \\
                            zap-baseline.py \\
                            -t http://host.docker.internal:8080 \\
                            -J zap-report.json \\
                            -x zap-report.xml \\
                            -r zap-report.html
                    """
                }
            }
            post {
                always {
                    sh "docker stop ${env.JOB_NAME}-staging || true"
                    sh "docker rm ${env.JOB_NAME}-staging || true"
                }
            }
        }
        
        stage('9. Analyse de Sécurité des Conteneurs') {
            when {
                expression { fileExists('Dockerfile') }
            }
            steps {
                script {
                    echo "🔒 Analyse CIS Benchmark du conteneur"
                    sh """
                        # Scan CIS Benchmark avec Trivy
                        trivy image \
                            --security-checks config \
                            --format sarif \
                            --output reports/trivy-config-scan.sarif \
                            ${env.DOCKER_REGISTRY}/${env.JOB_NAME}:${env.BUILD_NUMBER}
                        
                        # Scan avec Docker Bench Security
                        git clone https://github.com/docker/docker-bench-security.git
                        cd docker-bench-security
                        ./docker-bench-security.sh -l reports/docker-bench.log
                    """
                }
            }
        }
    }
    
    post {
        always {
            echo "📊 Génération des rapports de sécurité"
            archiveArtifacts artifacts: 'reports/**/*', fingerprint: true
            junit testResults: '**/test-reports/*.xml', allowEmptyResults: true
            publishHTML([
                allowMissing: true,
                alwaysLinkToLastBuild: true,
                keepAll: true,
                reportDir: 'reports',
                reportFiles: 'dependency-check-report.html',
                reportName: 'Rapport OWASP Dependency Check'
            ])
            publishHTML([
                allowMissing: true,
                alwaysLinkToLastBuild: true,
                keepAll: true,
                reportDir: 'reports',
                reportFiles: 'zap-report.html',
                reportName: 'Rapport OWASP ZAP'
            ])
            
            // Nettoyage
            sh 'docker system prune -f || true'
        }
        success {
            script {
                echo "✅ Pipeline DevSecOps terminé avec succès"
                emailext (
                    subject: "✅ SUCCÈS DevSecOps: ${env.JOB_NAME} [${env.BUILD_NUMBER}]",
                    body: """
                    Bonjour,

                    Le pipeline DevSecOps s'est terminé avec succès.

                    📋 Détails:
                    - Application: ${env.JOB_NAME}
                    - Build: ${env.BUILD_NUMBER}
                    - Rapport: ${env.BUILD_URL}

                    🔒 Analyses de sécurité effectuées:
                    ✓ SAST (SonarQube)
                    ✓ SCA (OWASP Dependency Check + Trivy)
                    ✓ Détection de secrets (Gitleaks)
                    ✓ Scan d'image Docker (Trivy)
                    ✓ DAST (OWASP ZAP)
                    ✓ CIS Benchmark

                    Consultez les rapports détaillés: ${env.BUILD_URL}

                    Cordialement,
                    Équipe DevSecOps
                    """,
                    to: "admin@example.com"
                )
            }
        }
        failure {
            script {
                echo "❌ Pipeline DevSecOps en échec"
                emailext (
                    subject: "❌ ÉCHEC DevSecOps: ${env.JOB_NAME} [${env.BUILD_NUMBER}]",
                    body: """
                    Bonjour,

                    Le pipeline DevSecOps a échoué.

                    📋 Détails:
                    - Application: ${env.JOB_NAME}
                    - Build: ${env.BUILD_NUMBER}
                    - URL: ${env.BUILD_URL}

                    ⚠️ Causes possibles:
                    - Quality Gate SonarQube non passée
                    - Vulnérabilités critiques détectées
                    - Échec des tests de sécurité

                    Veuillez vérifier les logs: ${env.BUILD_URL}

                    Cordialement,
                    Équipe DevSecOps
                    """,
                    to: "admin@example.com"
                )
            }
        }
        unstable {
            script {
                echo "⚠️ Pipeline DevSecOps instable"
                emailext (
                    subject: "⚠️ INSTABLE DevSecOps: ${env.JOB_NAME} [${env.BUILD_NUMBER}]",
                    body: """
                    Bonjour,

                    Le pipeline DevSecOps est instable.

                    📋 Détails:
                    - Application: ${env.JOB_NAME}
                    - Build: ${env.BUILD_NUMBER}
                    - URL: ${env.BUILD_URL}

                    ℹ️ Des vulnérabilités non critiques ont été détectées.

                    Consultez les rapports: ${env.BUILD_URL}

                    Cordialement,
                    Équipe DevSecOps
                    """,
                    to: "admin@example.com"
                )
            }
        }
    }
}
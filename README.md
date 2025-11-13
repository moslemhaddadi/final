# Projet Maven Simple pour Pipeline CI/CD

Ce projet est une application Java Maven très simple conçue pour tester un pipeline d'Intégration et de Déploiement Continu (CI/CD).

## Technologies
- **Langage:** Java
- **Build Tool:** Apache Maven

## Structure du Projet
- `pom.xml`: Fichier de configuration Maven.
- `src/main/java/com/example/App.java`: Classe principale avec une méthode simple `add()`.
- `src/test/java/com/example/AppTest.java`: Tests unitaires pour la méthode `add()`.
- `Jenkinsfile`: Exemple de configuration de pipeline pour Jenkins.

## Instructions de Test
1.  **Cloner** ce dépôt.
2.  **Configurer** votre pipeline CI/CD (par exemple, Jenkins) pour utiliser le `Jenkinsfile` fourni.
3.  **Déclencher** le pipeline. Il devrait exécuter les étapes suivantes :
    *   `Build and Test`: Exécute `mvn clean package` (compilation, tests unitaires, création du JAR).
    *   `Quality Gate (Simulation)`: Simule une vérification de qualité.
    *   `Deploy (Simulation)`: Simule le déploiement du fichier JAR.
4.  Pour tester un échec, modifiez volontairement un test dans `AppTest.java` et poussez les changements.

## Commandes Maven Utiles
- Compiler et exécuter les tests : `mvn clean install`
- Exécuter l'application : `mvn exec:java -Dexec.mainClass="com.example.App"`
"# final" 

FROM python:3.9-slim

WORKDIR /app

# Copier uniquement requirements.txt d'abord
COPY src/requirements.txt ./requirements.txt

# Installer les dépendances
RUN pip install -r requirements.txt

# Copier le reste du projet
COPY src/ .

CMD ["python", "app.py"]

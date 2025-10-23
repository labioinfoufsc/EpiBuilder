🇺🇸 [EN](../README.md) | 🇪🇸 [ES](./README.es.md) | 🇫🇷 [FR](./README.fr.md) | 🇮🇹 [IT](./README.it.md) | 🇧🇷 [PT-BR](./README.pt-br.md)

# EpiBuilder

![logo](https://github.com/labioinfoufsc/EpiBuilder/blob/main/src/web/frontend/src/assets/epibuilder-logo.png)

## Qu'est-ce qu'EpiBuilder?

EpiBuilder est un logiciel scientifique pour l'assemblage, la recherche et la classification des épitopes linéaires des lymphocytes B, destiné à la recherche utilisant des approches avec des protéomes partiels ou complets.

Il fonctionne comme une application web autonome dans un seul conteneur Docker (monolithe), qui inclut:

- Une interface utilisateur graphique (`frontend`)
- Une logique d'analyse et de traitement (`backend` et `core`)
- Un flux de travail avec NextFlow pour utiliser BepiPred 3.0 et BLAST
- Une base de données (PostgreSQL) pour conserver les données des utilisateurs et des tâches

## À qui s'adresse EpiBuilder?

La plateforme est conçue pour les chercheurs et les professionnels travaillant à l'intersection de l'immunologie, de la protéomique et de la bio-informatique. Elle est idéale pour quiconque a besoin d'effectuer des analyses *in silico* d'épitopes pour accélérer ses recherches.

Les domaines d'application clés incluent:
- **Maladies Infectieuses:** identifier des épitopes cibles dans les agents pathogènes, accélérant le développement de vaccins et de tests de diagnostic ;
- **Oncologie:** identifier des épitopes dans les protéines tumorales, permettant la sélection de cibles précises pour le développement d'immunothérapies et de vaccins contre le cancer ;
- **Neurosciences:** prédire des épitopes dans les protéines du système nerveux, facilitant la recherche de biomarqueurs d'auto-anticorps pour le diagnostic des maladies neurodégénératives.

## Prérequis

- [Docker](https://www.docker.com/) doit être installé sur votre ordinateur.
  - Pas besoin d'installer séparément des langages de programmation, des bases de données ou des bibliothèques.
  - Convient pour une utilisation sur des machines personnelles, des ordinateurs de laboratoire ou des serveurs.

## Étape 1: Télécharger l'image Docker (une seule fois)

Exécutez cette commande une seule fois pour télécharger l'image d'EpiBuilder:

```bash
docker pull bioinfoufsc/epibuilder:latest
````

## Étape 2: Créer et démarrer le conteneur EpiBuilder (une seule fois)

Exécutez la commande ci-dessous **une seule fois** pour créer le conteneur. Cela le démarrera également.

### (CPU)

```bash
  docker run -it --name epibuilder \
  -v /var/run/docker.sock:/var/run/docker.sock \
  -v epibuilder-data:/tmp/epibuilder \
  -p 80:80 \
  -p 8080:8080 \
  -p 5435:5432 \
  -p 5005:5005 \
  bioinfoufsc/epibuilder:latest
```

> **Astuce 1:** L'option `--name epibuilder` garantit que le conteneur est réutilisable.

## Étape 3: Accéder à l'interface Web

Après avoir démarré le conteneur, ouvrez votre navigateur et allez à :

```
http://localhost
```

Vous devriez voir l'interface web d'EpiBuilder.

## Étape 4: Réutiliser le conteneur (les fois suivantes)

Vous n'avez **pas** besoin d'exécuter à nouveau `docker run`.

Pour démarrer le conteneur via le terminal ou l'interface de ligne de commande (CLI) :

```bash
docker start epibuilder
```

Pour arrêter le conteneur via le terminal ou l'interface de ligne de commande (CLI) :

```bash
docker stop epibuilder
```

## Identifiants de connexion

Utilisez les informations suivantes pour vous connecter pour la première fois :

  - **Nom d'utilisateur :** `admin`
  - **Mot de passe :** `admin`

> **Note :** Le compte administrateur peut créer d'autres utilisateurs.
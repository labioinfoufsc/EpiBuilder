# backend

The `backend` is a Spring Boot application that serves as the backend for the `epibuilder-frontend` project. It provides an API, and also integrates with **Nextflow** for running bioinformatics workflow.

## Requirements

To contribute to the project, you'll need to set up your local environment. Here are the requirements to run the project:

- **JDK 21** (Java Development Kit)
- **Maven** (Build automation tool)
- **Spring Boot** (The project is built using Spring Boot)
- **Nextflow** (for executing bioinformatics workflows)

### Install JDK

1. **Windows / macOS**: Download and install JDK from [AdoptOpenJDK](https://adoptopenjdk.net/) or [Oracle JDK](https://www.oracle.com/java/technologies/javase-jdk11-downloads.html).
2. **Linux**: You can install JDK using your package manager. For example:
   - **Ubuntu/Debian**:
     ```bash
     sudo apt update
     sudo apt install openjdk-11-jdk
     ```
   - **CentOS/RHEL**:
     ```bash
     sudo yum install java-11-openjdk-devel
     ```

To verify that Java is installed correctly, run the following command:

```bash
java -version
```

### Install Maven

1. **Windows / macOS / Linux**: Download and install Maven from [Apache Maven](https://maven.apache.org/download.cgi).
2. **Linux**: You can install Maven using your package manager. For example:
   - **Ubuntu/Debian**:
     ```bash
     sudo apt install maven
     ```
   - **CentOS/RHEL**:
     ```bash
     sudo yum install maven
     ```

To verify that Maven is installed correctly, run the following command:

```bash
mvn -v
```

### Install Nextflow

To run bioinformatics workflows, **Nextflow** must be installed. Follow the instructions on the [Nextflow installation page](https://www.nextflow.io/docs/latest/getstarted.html) for your platform.

For example, on Linux or macOS, you can install Nextflow via `curl`:

```bash
curl -s https://get.nextflow.io | bash
```

For Windows, use the [Windows installation instructions](https://www.nextflow.io/docs/latest/getstarted.html#installing-nextflow).

To verify that Nextflow is installed correctly, run:

```bash
nextflow -v
```

## Running the Project Locally

1. **Clone the repository**:

```bash
git clone https://github.com/bioinformatics-ufsc/backend.git
```

2. **Navigate to the project directory**:

```bash
cd backend
```

3. **Build the project using Maven**:

```bash
mvn clean install
```

4. **Run the application**:

```bash
mvn spring-boot:run
```

The backend will start and you can interact with it via the exposed APIs.

5. **Test the Nextflow Integration**:

The backend interacts with Nextflow for running workflows. Ensure that you have Nextflow installed and configured properly before testing workflow execution. You can trigger a workflow execution via the backend's API.

## Project Structure

The basic structure of the project is as follows:

```
backend/
├── src/                                       # Application source code
│   ├── main/
│   │   ├── java/                              # Java source code
│   │   │   ├── ufsc/                          # Base package
│   │   │   │   ├── br/                       # Domain
│   │   │   │   │   ├── epibuilder/            # Main project package
│   │   │   │   │   │   ├── config/            # Configuration classes
│   │   │   │   │   │   ├── controller/        # API controllers (handling HTTP requests)
│   │   │   │   │   │   ├── dto/               # Data Transfer Objects (DTOs)
│   │   │   │   │   │   ├── exception/         # Exception handling classes
│   │   │   │   │   │   ├── loader/            # Data loading and pipeline management
│   │   │   │   │   │   ├── model/             # Domain models (entities, objects)
│   │   │   │   │   │   ├── repository/        # Data access layer (repositories for DB interactions)
│   │   │   │   │   │   ├── service/           # Services for business logic
│   │   ├── resources/                         # Application configuration files
│   │   │   ├── application.properties         # Spring Boot and other configurations
├── pom.xml                                    # Maven configuration and dependencies
```

### Explanation of the Folder Structure:

- **`src/main/java/ufsc/br/epibuilder/`**: Contains the main application logic.
  - **`config/`**: Configuration classes such as Spring Boot configurations.
  - **`controller/`**: API controllers that handle HTTP requests, map them to service calls, and return responses.
  - **`dto/`**: Data Transfer Objects used to transfer data between layers.
  - **`exception/`**: Custom exception classes, including global exception handlers and error responses.
  - **`loader/`**: Handles data loading.
  - **`model/`**: Domain models (entities or objects representing the core data in the application).
  - **`repository/`**: The data access layer for database interactions.
  - **`service/`**: Service classes that contain the business logic including NextFlow call.

- **`src/main/resources/`**: Contains configuration files.
  - **`application.properties`**: Configuration file for Spring Boot, including database configurations.

- **`pom.xml`**: The Maven configuration file where dependencies, plugins, and build settings are defined.

## Contributing

If you'd like to contribute to the project, please follow the steps below:

1. Fork the repository.
2. Create a new branch for your feature or fix:

```bash
git checkout -b my-feature
```

3. Make your changes and commit:

```bash
git commit -m "Description of my changes"
```

4. Push your changes to your fork:

```bash
git push origin my-feature
```

5. Open a pull request on the main repository for your changes to be reviewed and merged.

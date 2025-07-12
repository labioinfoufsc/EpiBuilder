# frontend

The `frontend` is the graphical user interface for the `epibuilder-core` project, developed using Angular 12. This repository contains the source code for the frontend part of the application.

## Requirements

To contribute to the project, you'll need to set up your local environment. Here are the requirements to run the project:

- **Node.js** (version 14 or higher)
- **NPM** (Node.js package manager)
- **Angular CLI** (version 12 or higher)

### Install Node.js and NPM

1. **Download Node.js**:
   - For **Windows** and **macOS**, go to the [Node.js download page](https://nodejs.org/) and download the appropriate installer for your operating system.
   - For **Linux**, you can install Node.js using a package manager, depending on your distribution. For example:
     - **Ubuntu/Debian**:
       ```bash
       sudo apt update
       sudo apt install nodejs npm
       ```
     - **CentOS/RHEL**:
       ```bash
       sudo yum install nodejs npm
       ```
     - **macOS** (via Homebrew):
       ```bash
       brew install node
       ```

2. The Node.js installation includes NPM (Node Package Manager), so you don’t need to install it separately.

3. **Verify the installation**:
   After installation, open a terminal (or Command Prompt on Windows) and run the following commands to verify that Node.js and NPM are installed correctly:

   ```bash
   node -v
   npm -v

### Install Angular CLI

If you don't have Angular CLI installed, you can install it by running the following command:

```bash
npm install -g @angular/cli@12
```

## Running the Project Locally

1. Clone the repository:

```bash
git clone https://github.com/bioinformatics-ufsc/frontend.git
```

2. Navigate to the project directory:

```bash
cd frontend
```

3. Install the project dependencies:

```bash
npm i
```

4. After the dependencies are installed, start the development server:

```bash
ng serve
```

5. Open your browser and visit [http://localhost:4200](http://localhost:4200) to view the project.

## Explanation of the Folder Structure:

- **`src/app/`**: Contains the main application code.
  - **`auth/`**: Components and services related to user authentication (e.g., login, registration).
  - **`components/`**: Reusable UI components that can be used across the application.
  - **`models/`**: Data models, including interfaces and classes that represent the data structures used in the app.
  - **`pages/`**: Page-level components, each of which typically corresponds to a route in the application.
  - **`services/`**: Services that handle business logic and data interaction (e.g., API calls).
  - **`app-routing.module.ts`**: The routing configuration that maps routes to page components.
  - **`app.module.ts`**: The main module of the Angular application that imports necessary modules (including the `AppRoutingModule` for routing).

- **`angular.json`**: Configuration file for Angular CLI, which manages the build and development settings.

- **`package.json`**: Contains the project's dependencies, scripts, and other configurations related to npm.

- **`tsconfig.json`**: TypeScript configuration file for the project.

## Contributing

If you'd like to contribute to the project, please follow the steps below:

1. Fork the repository.
2. Create a new branch for your feature:

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

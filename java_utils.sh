#!/bin/bash
set -euo pipefail

read -p "Run 'mvn clean install' skipping tests? (y/n) " run_clean

project_root="$(cd "$(dirname "$0")" && pwd)"

build() {
    local module="$1"     
    local jar="$2"         
    local dir="$project_root/$module"

    [[ -d "$dir" ]] || { echo "Directory not found: $dir"; return; }

    echo "Compiling $module..."
    cd "$dir"
    if [[ "$run_clean" =~ ^[yY]$ ]]; then
        mvn clean install -DskipTests
    else
        mvn install
    fi

    local jar_path="target/$jar"
    if [[ -f "$jar_path" ]]; then
        mv "$jar_path" "$dir/"
        echo "Moved $jar to $dir/"
    else
        echo "JAR not found: $jar_path"
    fi
}

build core    epibuilder-core.jar
build backend epibuilder-backend.jar

read -p "Run Spring Boot application from backend? (y/n) " run_spring
if [[ "$run_spring" =~ ^[yY]$ ]]; then
    cd "$project_root/backend"
    mvn spring-boot:run
fi
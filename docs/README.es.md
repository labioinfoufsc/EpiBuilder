🇺🇸 [EN](./README.md) | 🇪🇸 [ES](./docs/README.es.md) | 🇫🇷 [FR](./docs/README.fr.md) | 🇮🇹 [IT](./docs/README.it.md) | 🇧🇷 [PT-BR](./docs/README.pt-br.md)

# EpiBuilder

## ¿Qué es EpiBuilder?

EpiBuilder es un software científico para el ensamblaje, la búsqueda y la clasificación de epítopos lineales de células B, para investigaciones que utilizan enfoques con proteomas parciales o completos.

Funciona como una aplicación web autónoma dentro de un único contenedor Docker (monolito), que incluye:

- Interfaz gráfica de usuario (`frontend`)
- Lógica de análisis y procesamiento (`backend` y `core`)
- Flujo de trabajo con NextFlow que utiliza BepiPred 3.0 y BLAST
- Base de datos (PostgreSQL) para almacenar usuarios y datos de tareas

## ¿Para quién es EpiBuilder?

La plataforma está diseñada para investigadores y profesionales que trabajan en la intersección de la inmunología, la proteómica y la bioinformática. Es ideal para cualquiera que necesite realizar análisis *in silico* de epítopos para acelerar su investigación.

Las áreas clave de aplicación incluyen:
- **Enfermedades Infecciosas:** identificar epítopos diana en patógenos, acelerando el desarrollo de vacunas y pruebas de diagnóstico;
- **Oncología:** identificar epítopos en proteínas tumorales, permitiendo la selección de dianas precisas para el desarrollo de inmunoterapias y vacunas contra el cáncer;
- **Neurociencia:** predecir epítopos en proteínas del sistema nervioso, facilitando la búsqueda de biomarcadores de autoanticuerpos para el diagnóstico de enfermedades neurodegenerativas.

## Requisitos

- [Docker](https://www.docker.com/) debe estar instalado en su computadora.
  - No es necesario instalar lenguajes de programación, bases de datos ni bibliotecas por separado.
  - Apto para uso en computadoras personales, laboratorios o servidores.

## Paso 1: Descargar la Imagen Docker (Solo una vez)

Ejecute este comando solo una vez para descargar la imagen de EpiBuilder:

- **Si su sistema tiene una GPU NVIDIA con los controladores instalados (basado en Ubuntu):**

```bash
docker pull bioinfoufsc/epibuilder:ubuntu-gpu
````

> **Nota:** Es necesario tener los controladores de GPU NVIDIA instalados para ejecutar este contenedor con soporte GPU.
> Si está usando Linux y quiere usar EpiBuilder con soporte GPU, asegúrese de tener CUDA instalado:
> [https://docs.nvidia.com/cuda/cuda-installation-guide-linux](https://docs.nvidia.com/cuda/cuda-installation-guide-linux)

* **Si su sistema no tiene GPU NVIDIA (basado en Debian):**

```bash
docker pull bioinfoufsc/epibuilder:debian-cpu
```

> **Consejo:** En caso de duda, use la versión para CPU.

## Paso 2: Crear e Iniciar el Contenedor EpiBuilder (Solo una vez)

Ejecute el siguiente comando **solo una vez** para crear el contenedor. Esto también lo iniciará.

### Debian (CPU)

```bash
docker run -it --name epibuilder \
  -p 80:80 \
  -p 8080:8080 \
  -p 5435:5432 \
  bioinfoufsc/epibuilder:debian-cpu
```

O

### Ubuntu (GPU)

```bash
docker run --gpus all -it --name epibuilder \
  -p 80:80 \
  -p 8080:8080 \
  -p 5432:5432 \
  bioinfoufsc/epibuilder:ubuntu-gpu
```

> **Consejo:** La opción `--name epibuilder` permite reutilizar el contenedor.

## Paso 3: Acceder a la Interfaz Web

Después de iniciar el contenedor, abra su navegador y acceda a:

```
http://localhost
```

Debería ver la interfaz web de EpiBuilder.

## Paso 4: Reutilizar el Contenedor (Próximas veces)

No es necesario ejecutar `docker run` nuevamente.

Para iniciar el contenedor desde la terminal o la línea de comandos:

```bash
docker start epibuilder
```

Para detener el contenedor desde la terminal o la línea de comandos:

```bash
docker stop epibuilder
```

## Credenciales de Inicio de Sesión

Use las siguientes credenciales para el primer acceso:

* **Usuario:** `admin`
* **Contraseña:** `admin`

> **Nota:** La cuenta admin puede crear otros usuarios.

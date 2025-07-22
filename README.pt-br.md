🇧🇷 [PT-BR](./README.pt-br.md) | [🇪🇸 ES](./README.es.md) | [🇺🇸 EN](./README.md)

# EpiBuilder

## O que é o EpiBuilder?

EpiBuilder é um software científico para montagem, busca e classificação de epítopos lineares de células B, com foco em pesquisas sobre patógenos e desenvolvimento de vacinas usando abordagens proteoma-wide.

Funciona como uma aplicação web autocontida em um único contêiner Docker (monolito), que inclui:

- Interface gráfica (frontend)
- Lógica de análise e processamento (backend)
- Workflow com NextFlow utilizando BepiPred 3.0 e BLAST
- Banco de dados (PostgreSQL) para persistência de dados de usuários e tarefas

## Requisitos

- [Docker](https://www.docker.com/) deve estar instalado no seu computador.
  - Não é necessário instalar linguagens de programação, bancos de dados ou bibliotecas separadamente.
  - Adequado para uso em máquinas pessoais, computadores de laboratório ou servidores.

## Etapa 1: Baixar a Imagem Docker (Apenas Uma Vez)

Execute este comando apenas uma vez para baixar a imagem do EpiBuilder:

- **Se o seu sistema possui GPU NVIDIA com drivers instalados (base Ubuntu):**

```bash
docker pull bioinfoufsc/epibuilder:ubuntu-gpu
````

> **Nota:** É necessário ter os drivers da GPU NVIDIA instalados para rodar este contêiner com suporte a GPU.
> Se estiver usando Linux e quiser utilizar o EpiBuilder com suporte a GPU, certifique-se de ter o CUDA instalado:
> [https://docs.nvidia.com/cuda/cuda-installation-guide-linux](https://docs.nvidia.com/cuda/cuda-installation-guide-linux)

* **Se o seu sistema não possui GPU NVIDIA (base Debian):**

```bash
docker pull bioinfoufsc/epibuilder:debian-cpu
```

> **Dica:** Em caso de dúvida, utilize a versão para CPU.

## Etapa 2: Criar e Iniciar o Contêiner EpiBuilder (Apenas Uma Vez)

Execute o comando abaixo **apenas uma vez** para criar o contêiner. Isso também irá iniciá-lo.

### Debian (CPU)

```bash
docker run -it --name epibuilder \
  -p 80:80 \
  -p 8080:8080 \
  -p 5435:5432 \
  -e FRONTEND_PORT=80 \
  -e BACKEND_PORT=8080 \
  -e DB_PORT=5435 \
  -e DB_NAME=epibuilder \
  -e DB_USERNAME=epiuser \
  -e DB_PASSWORD=epiuser \
  -e ENV=development \
  bioinfoufsc/epibuilder:debian-cpu
```

Ou

### Ubuntu (GPU)

```bash
docker run --gpus all -it --name epibuilder \
  -p 80:80 \
  -p 8080:8080 \
  -p 5432:5432 \
  -e FRONTEND_PORT=80 \
  -e BACKEND_PORT=8080 \
  -e DB_PORT=5432 \
  -e DB_NAME=epibuilder \
  -e DB_USERNAME=epiuser \
  -e DB_PASSWORD=epiuser \
  -e ENV=development \
  bioinfoufsc/epibuilder:ubuntu-gpu
```

> **Dica:** A opção `--name epibuilder` permite reutilizar o contêiner.

## Etapa 3: Acessar a Interface Web

Após iniciar o contêiner, abra o navegador e acesse:

```
http://localhost
```

Você deverá ver a interface web do EpiBuilder.

## Etapa 4: Reutilizar o Contêiner (Próximas Vezes)

Você **não** precisa executar `docker run` novamente.

Para iniciar o contêiner via terminal ou linha de comando (CLI):

```bash
docker start epibuilder
```

Para parar o contêiner via terminal ou linha de comando (CLI):

```bash
docker stop epibuilder
```

## Credenciais de Acesso

Use as credenciais abaixo para o primeiro login:

* **Usuário:** `admin`
* **Senha:** `admin`

> **Nota:** A conta admin pode criar outros usuários.

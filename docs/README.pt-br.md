🇺🇸 [EN](../README.md) | 🇪🇸 [ES](./README.es.md) | 🇫🇷 [FR](./README.fr.md) | 🇮🇹 [IT](./README.it.md) | 🇧🇷 [PT-BR](./README.pt-br.md)

# EpiBuilder

## O que é o EpiBuilder?

O EpiBuilder é um software científico para montagem, busca e classificação de epítopos lineares de células B, para pesquisas que utilizam abordagens usando proteomas parciais ou completos.

Funciona como uma aplicação web autocontida em um único contêiner Docker (monolito), que inclui:

- Interface gráfica (`frontend`)
- Lógica de análise e processamento (`backend` e `core`)
- Workflow com NextFlow utilizando BepiPred 3.0 e BLAST
- Banco de dados (PostgreSQL) para persistência de dados de usuários e tarefas

## Para quem é o EpiBuilder?

A plataforma é projetada para pesquisadores e profissionais que trabalham na interseção da imunologia, proteômica e bioinformática. É ideal para qualquer pessoa que precise realizar análises *in silico* de epítopos para acelerar suas pesquisas.

As principais áreas de aplicação incluem:
- **Doenças Infecciosas:** identificar epítopos alvo em patógenos, acelerando o desenvolvimento de vacinas e testes diagnósticos;
- **Oncologia:** identificar epítopos em proteínas tumorais, permitindo a seleção de alvos precisos para o desenvolvimento de imunoterapias e vacinas contra o câncer;
- **Neurociência:** prever epítopos em proteínas do sistema nervoso, facilitando a busca por biomarcadores de autoanticorpos para o diagnóstico de doenças neurodegenerativas.

## Requisitos

- [Docker](https://www.docker.com/) deve estar instalado no seu computador.
  - Não é necessário instalar linguagens de programação, bancos de dados ou bibliotecas separadamente.
  - Adequado para uso em máquinas pessoais, computadores de laboratório ou servidores.

## Etapa 1: Baixar a Imagem Docker (Apenas Uma Vez)

Execute este comando apenas uma vez para baixar a imagem do EpiBuilder:

```bash
docker pull bioinfoufsc/epibuilder:latest
````

## Etapa 2: Criar e Iniciar o Contêiner EpiBuilder (Apenas Uma Vez)

Execute o comando abaixo **apenas uma vez** para criar o contêiner. Isso também irá iniciá-lo.

### (CPU)

```bash
docker run -it --name epibuilder \
  -p 80:80 \
  -p 8080:8080 \
  -p 5435:5432 \
  bioinfoufsc/epibuilder:latest
```

Ou

### (GPU)

```bash
docker run --gpus all -it --name epibuilder \
  -p 80:80 \
  -p 8080:8080 \
  -p 5432:5432 \
  bioinfoufsc/epibuilder:latest
```

> **Nota:** É necessário ter os drivers da GPU NVIDIA instalados para rodar este contêiner com suporte a GPU.
> Se estiver usando Linux e quiser utilizar o EpiBuilder com suporte a GPU, certifique-se de ter o CUDA instalado:
> [https://docs.nvidia.com/cuda/cuda-installation-guide-linux](https://docs.nvidia.com/cuda/cuda-installation-guide-linux)

> **Dica 1:** Em caso de dúvida, utilize a versão para CPU.
> **Dica 2:** A opção `--name epibuilder` permite reutilizar o contêiner.

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
# SCRIPTS E2E Tutorial

Tutorial for running SCRIPTS E2E system on a sample Farsi input dataset.

## Installation

Please follow the installation instruction in the main `README.md` page. Once the installation process completes, you can confirm that all docker images are installed by running:

```sh
$ docker images
REPOSITORY
matching-umd
matcher-unique-id
evidence-combination
scriptsmt/systems
umd-nmt
umd-smt
umd-nnltm
material/asr-fa
material/kws-language-independent-clir
material/scripts-morph
indexing-umd
mt_augmentation
document-stemmer
material/nbest
reranker
packing
summarizer
query-analyzer-umd
evaluation
```

## Preparing inputs
All sample data and configuration files for the tutorials are in this workspace directory (`/example/workspace-fa`). Copy this `workspace-fa` directory somewhere locally, and make note of the path since the path will be used multiple times. This tutorial assumes that the workspace path is `/local/nlp/workspace-fa`.

- `NIST-data`: Contains query strings in English and the raw `.wav` and `.txt` files in the source languages.

- `configs`: Contains json configuration files. Each configuration file represents one of four stages--preprocess, index, querysearch, summarization. When the system starts, the system will first look up `configs` directory and start processing one configuration file at a time.
    - `conf1_prep_fa.json`: configuration file for the preprocess stage. During this stage, the system performs transcription, translations, morphology analysis, etc. Do update the `root_absolute_path` field with the actual workspace location.
    - `conf2_prep_fa.json`: indexing stage. Creates a _data_store_structure_ file that is required for querysearch and summarization stage. Do update the `root_absolute_path` field with the actual workspace location.
    - `conf3_query_fa.json`: The CLIR querysearch stage. Matches query and source data. If this file is used without the indexing stage config file, `data_store_structure` field must point to the correct _data_store_structure_ file location.
    - `conf4_summariation.json`: Summarization stage.
- `properties/scripts.properties`: system wide configuration file. Double check that following properties are correctly configured:
    - `scripts.corpora.path`: do update this path with the correct NIST-data location.
    - `script.config.gpu.number`: the number of NVIDIA GPUs in the machine. AWS p3.8xlarge machine has 4 GPUs.
    - `script.config.giga.ram.per.gpu`: GPU RAM per GPU in GB. Each GPU in the AWS p3.8xlarge machine has 16 GB of RAM.

## Running the SCRIPTS system.
You can use `init.sh` file. Replace `/local/nlp/workspace-fa` in the below command with the actual workspace location.

```sh
$ sh init.sh /local/nlp/workspace-fa
```

All outputs from the preprocess and index stage are stored in the `NIST-data` directory. All querysearch and summarization outputs are stored in the `output` directory.

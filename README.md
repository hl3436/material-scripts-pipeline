# material-scripts-pipeline

This repository contains the source code for the executive software that runs other SCRIPTS components.

## System Requirements
- RAM: 100GB
- GPU RAM: 16GB per GPU
- Minimum NVIDIA GPU Driver: 460.32.03

## Installation

### Downloading SCRIPTS docker images

Following files are required to run the system on Farsi data:
- `scripts1.tar` (198.3 GB)
- `scripts2.tar` (98.3 GB)
- `scripts3.tar` (163.9 GB)
- `scripts-smt.tar.a` (194.3 GB)
- `scripts-smt.tar.b` (194.3 GB)
- `scripts-smt.tar.c` (163.3 GB)
- `scripts-qa.tar` (200.4 GB)

(Optional) To run the system on languages other than Farsi, following additional files are needed:
- `scritps-asr1.tar` (177 GB)
- `scripts-asr2.tar` (139.4 GB)

Download links are not public. If you do not have the links please contact us.

### Installing SCRIPTS docker images
`scripts1.tar`, `scripts2.tar`, `scripts3.tar`, `scritps-asr1.tar`, and `scritps-asr2.tar` files contain several archived docker images. Uncompress them and install the images.

Both `scripts-qa.tar` and `scripts-smt.tar` are images on their own. They should not be unpacked but installed directly instead.

```sh 
# Install SCRIPTS Query Analyzer
$ docker load --input scripts-qa.tar

# Install SCRIPTS umd-smt
$ cat scripts-smt.tar.* > scripts-smt.tar
$ docker load --input scripts-smt.tar
```

## Running the SCRIPTS System

Note: please checkout the turorial in the `example/workspace-fa/README.md`.

### Input preparation
The system requires a workspace directory with a specific directory structure:
```sh
SOME_WORKSPACE
├── configs
│   ├── conf1.json
│   ├── conf2.json
│   ├── ...
│   └── confn.json
├── NIST-data
│   └── ...
└── properties
    └── scripts.properties
```
- `configs`: should contain configuration files. Example configuration files for each language are provided under `example/configs` directory.
- `NIST-data`: should contain input data in material format. Intermediate process data is also stored in this directory.
- `propertis/scripts.properties`: system wide configuration file that specifies versions, GPU resources, etc. Example `scripts.properties` is provided under `example/workspace-fa/properties`.

### Run
```sh
$ SCRIPTS_WORKSPACE=/path/to/SOME_WORKSPACE

$ docker run --rm -it --name scripts-pipeline --gpus all \
    -v $SCRIPTS_WORKSPACE:$SCRIPTS_WORKSPACE \
    -v /var/run/docker.sock:/var/run/docker.sock \
    -v $SCRIPTS_WORKSPACE/properties/scripts.properties:/app/scripts-release-20210929-0.1/config/scripts.properties \
    scripts-pipeline:v1.5 $SCRIPTS_WORKSPACE
```


## Build
Run command below to build a new docker image
```sh
$ docker build -t scripts-pipeline:v1.5 .
```



## Compontents
### Source Codes
- materila-asr (https://github.com/ondrejklejch/material-asr)
- material-edinmt (https://github.com/EdinburghNLP/material-edinmt)
- material-umdnmt (https://github.com/umdxling/material-umdnmt)
- material-umdsmt (https://github.com/umdxling/material-umdsmt)
- morphological analyzer (https://github.com/rnd2110/SCRIPTS_Morphological_Analyzer)
- summarizer (https://github.com/eturcan/scripts)
- clir, kws, nnltm
    - https://gitlab.umiacs.umd.edu/petra/material-docker
    - https://gitlab.umiacs.umd.edu/srnair/material

[![Build and test](https://github.com/saveourtool/save-cloud/actions/workflows/build_and_test.yml/badge.svg?branch=master)](https://github.com/saveourtool/save-cloud/actions/workflows/build_and_test.yml?query=branch%3Amaster)
[![License](https://img.shields.io/github/license/saveourtool/save-cloud)](https://github.com/saveourtool/save-cloud/blob/master/LICENSE)
[![FOSSA Status](https://app.fossa.com/api/projects/git%2Bgithub.com%2Fsaveourtool%2Fsave-cloud.svg?type=shield)](https://app.fossa.com/projects/git%2Bgithub.com%2Fsaveourtool%2Fsave-cloud?ref=badge_shield)

## What is save-cloud?
Save-cloud is a Non-profit Opensource Ecosystem with a focus on Code Analysis. 
Together with [save-cli](https://github.com/saveourtool/save-cli) test framework it offers intelligent services tailored for developers of code analysis tools. 
Our key focus is to make life of developers who analyze code easier. 

1. **SAVE** - A distributed Cloud CI platform for testing and benchmarking code analyzers, equipped with a specialized test framework and test format. With SAVE, you can:
    - Swiftly set up testing and **CI for your code analyzer**;
    - **Share your tests** with the community, allowing comparisons of other tools using your benchmarks;
    - Use SAVE to create an **online demo for your analyzer** and set it up for your community's use;
    - Benchmarks Archive with the **list of popular benchmarks** (with a reference to [awesome-benchmarks](https://github.com/saveourtool/awesome-benchmarks)).

2. **COSV** - A platform designed for the **reporting**, aggregation, and deduplication of one-day **vulnerabilities**.

Additionally, on our platform we host **contests** in the field of code analysis.
This provides an opportunity for you to submit your automated solutions for bug detection, and compete with other innovative projects.

## Links
- Collection of Code Analyzers Demo: [Demo](https://saveourtool.com/demo)
- Benchmarks Archive: [Benchmarks](https://saveourtool.com/awesome-benchmarks)
- CI projects: [CI Projects](https://saveourtool.com/projects)
- Vulnerabilities Collection: [1-day Vulnerabilities](https://cosv.gitlink.org.cn)

## Motivation
- [Motivation of **SAVE** and more details](info/SaveMotivation.md)
- Motivation of **VULN** and more details: TBD

## High-level perspective
#### SAVE
![SAVE processing](https://user-images.githubusercontent.com/58667063/146387903-24ba9c91-a2a3-45e7-a07a-cb7bc388e4aa.jpg)

#### COSV
<img width="1306" alt="image" src="https://github.com/saveourtool/save-cloud/assets/58667063/008b0976-98c2-4195-bdf5-570a70b07827">

## Build and deploy
To build the project and run all tests, execute `./gradlew build`. 

For more detailed instructions, including **deployment instructions**, see [save-deploy/README.md](save-deploy/README.md).

## Local deployment
0. Install Java 17 (LTS). We recommend [azul](https://www.azul.com/downloads/#downloads-table-zulu).
1. Ensure that docker daemon is running and `docker compose` is installed. We suggest [Docker Desktop](https://www.docker.com/products/docker-desktop/).
2. Run `./gradlew deployLocal -Psave.profile=dev` to start the MySql DB, Minio and will run all Spring Microservices with Docker Compose.
3. Run `./gradlew -Psave.profile=dev :save-frontend:run` to start save-frontend using webpack-dev-server, requests to REST API will be
  proxied as configured in [dev-server.js](../save-frontend/webpack.config.d/dev-server.js). User will be hardcoded with `admin` user. 

## Architecture and design
Save:

<img src="/info/img/save-diagram.png" width="768px"/>

COSV:

<img src="/info/img/cosv-diagram.PNG" width="768px"/>

[![Build Status](https://jenkins.cessda.eu/buildStatus/icon?job=cessda.cdc.osmh-indexer.cmm%2Fmain)](https://jenkins.cessda.eu/job/cessda.cdc.osmh-indexer.cmm/job/main/)
[![Bugs](https://sonarqube.cessda.eu/api/project_badges/measure?project=eu.cessda.pasc%3Apasc-oci&metric=bugs)](https://sonarqube.cessda.eu/dashboard?id=eu.cessda.pasc%3Apasc-oci)
[![Code Smells](https://sonarqube.cessda.eu/api/project_badges/measure?project=eu.cessda.pasc%3Apasc-oci&metric=code_smells)](https://sonarqube.cessda.eu/dashboard?id=eu.cessda.pasc%3Apasc-oci)
[![Coverage](https://sonarqube.cessda.eu/api/project_badges/measure?project=eu.cessda.pasc%3Apasc-oci&metric=coverage)](https://sonarqube.cessda.eu/dashboard?id=eu.cessda.pasc%3Apasc-oci)
[![Duplicated Lines (%)](https://sonarqube.cessda.eu/api/project_badges/measure?project=eu.cessda.pasc%3Apasc-oci&metric=duplicated_lines_density)](https://sonarqube.cessda.eu/dashboard?id=eu.cessda.pasc%3Apasc-oci)
[![Lines of Code](https://sonarqube.cessda.eu/api/project_badges/measure?project=eu.cessda.pasc%3Apasc-oci&metric=ncloc)](https://sonarqube.cessda.eu/dashboard?id=eu.cessda.pasc%3Apasc-oci)
[![Maintainability Rating](https://sonarqube.cessda.eu/api/project_badges/measure?project=eu.cessda.pasc%3Apasc-oci&metric=sqale_rating)](https://sonarqube.cessda.eu/dashboard?id=eu.cessda.pasc%3Apasc-oci)
[![Quality Gate Status](https://sonarqube.cessda.eu/api/project_badges/measure?project=eu.cessda.pasc%3Apasc-oci&metric=alert_status)](https://sonarqube.cessda.eu/dashboard?id=eu.cessda.pasc%3Apasc-oci)
[![Reliability Rating](https://sonarqube.cessda.eu/api/project_badges/measure?project=eu.cessda.pasc%3Apasc-oci&metric=reliability_rating)](https://sonarqube.cessda.eu/dashboard?id=eu.cessda.pasc%3Apasc-oci)
[![Security Rating](https://sonarqube.cessda.eu/api/project_badges/measure?project=eu.cessda.pasc%3Apasc-oci&metric=security_rating)](https://sonarqube.cessda.eu/dashboard?id=eu.cessda.pasc%3Apasc-oci)
[![Technical Debt](https://sonarqube.cessda.eu/api/project_badges/measure?project=eu.cessda.pasc%3Apasc-oci&metric=sqale_index)](https://sonarqube.cessda.eu/dashboard?id=eu.cessda.pasc%3Apasc-oci)
[![Vulnerabilities](https://sonarqube.cessda.eu/api/project_badges/measure?project=eu.cessda.pasc%3Apasc-oci&metric=vulnerabilities)](https://sonarqube.cessda.eu/dashboard?id=eu.cessda.pasc%3Apasc-oci)

# OSMH Consumer Indexer (PaSC-OCI)

CESSDA CDC Consumer Indexer (an OSMH Consumer) for Metadata harvesting and ingestion into Elasticsearch. See the [OSMH System Architecture Document](https://docs.google.com/document/d/1RrXjpbyUGdd5FKSjrnQmRdbzaCQzE2W-92lYKs1KeCA/edit) for more information about The Open Source Metadata Harvester (OSMH).

## Quality - Software Maturity Level

The overall Software Maturity Level for this product, and the individual scores for each attribute can be found in the [SML](SML.md) file.

## Getting Started

These instructions will get you a copy of the project up and running on your local machine for development and testing purposes. See deployment for notes on how to deploy the project on a live system.

### Prerequisites

The following tools must be installed before compiling

* Java JDK 17

### Test it

    ./mvnw clean test

### Sonar it

To perform SonarQube analysis locally, run SonarQube and then execute

    ./mvnw sonar:sonar

### Build it

    ./mvnw clean package

### Run it

    ./mvnw spring-boot:run

### Run it — with a specified profile

To run the OSMH consumer with a custom profile, use the following command line:

    java -jar target/pasc-oci*.jar --spring.profiles.active=${profile_name}

If no profile is specified, the default profile will be used. This profile is configured to use a local Elasticsearch instance hosted at `http://localhost:9200`.

## Notes

* Makes use of TDD
* When running integration tests, a standalone Elasticsearch server is launched

## Deployment

### At startup

The application loads configuration in this order as defined by the Spring Boot Framework.

* Command line parameters 
    * e.g. `--logging.level.ROOT=DEBUG` sets logging level for all classes
* Environment Variables e.g. `SECURITY_USER_NAME`
    * Spring can use weak binding to convert environment variables into Java properties
    * e.g. `SPRING_BOOT_ADMIN_USERNAME` converts to `spring.boot.admin.username`
* application-[dev,local,prod].yml
    * dev, local and prod refer to Spring profiles
        * A Spring profile can be specified by the command line `--spring.profiles.active` or the environment variable `SPRING_PROFILES_ACTIVE`
    * See <https://docs.spring.io/spring-boot/docs/3.0.x/reference/html/features.html#features.profiles> for more details
* application.yml

See <https://docs.spring.io/spring-boot/docs/3.0.x/reference/html/features.html#features.external-config> for detailed documentation.

### At Runtime

If the application is registered at a [Spring Boot Admin server](https://github.com/codecentric/spring-boot-admin), all environment properties can be changed at runtime.

**Changes made at runtime will be effective after a context reload but are lost after an application restart unless persisted in** `application.yml`

## Configuring the indexer

The OSMH harvester has many settings that change the behaviour of the harvest. These settings are defined under the `osmhConsumer` and `osmhhandler` keys in `application.yml`.

### Elasticsearch Properties

Elasticsearch properties are configured under the `elasticsearch` key.

```yaml
elasticsearch:
  host: localhost # The Elasticsearch host
  username: elastic # The username to use when connecting to a secured Elasticsearch cluster
  password: examplePassword # The password to use when connecting to a secured Elasticsearch cluster
  numberOfShards: 2 # The number of primary shards the created indices will have
  numberOfReplicas: 0 # The number of replicas each primary shard has
```

### Language settings

The languages that the OSMH indexer will attempt to harvest are specified under `languages`. These languages will be parsed and indexed into Elasticsearch. The default languages are specified below.

```yaml
languages: ['cs', 'da', 'de', 'el', 'en', 'et', 'fi', 'fr', 'hu', 'it', 'nl', 'no', 'pt', 'sk', 'sl', 'sr', 'sv']
```

Custom mappings and settings can be defined in [src/main/resources/elasticsearch](src/main/resources/elasticsearch). Mappings are global for all defined languages, whereas settings are selected per language. If the required mappings and settings can't be loaded, the index will not be created and an error will be logged.


### Indexing a repository

In most cases, repositories to index are detected using instances of `pipeline.json`. These are generated by the [CESSDA Metadata Harvester](https://github.com/cessda/cessda.metadata.harvester) and contain all the information needed to index the XMLs present alongside them.

Repositories are discovered by searching for instances of `pipeline.json` in the `baseDirectory`. The `baseDirectory` can be specified using the `--baseDirectory` command line parameter, or by specifying `baseDirectory` in `application.yml`.

### Explicitly declaring a repository

Repositories are declared in [application.yml](/src/main/resources/application.yml) and are specified under the key `endpoints.repos`.

```yaml
endpoints:
  repos:
    - url: http://194.117.18.18:6003/v0/oai
      path: path/to/directory
      code: APIS
      name: 'Portuguese Archive of Social Information (APIS)'
      preferredMetadataParam: oai_ddi25
      defaultLanguage: pt
```

The URL is the OAI-PMH endpoint.

The code is the short name of the repository and acts as a unique identifier. This is a mandatory field.

The name is the friendly name of the repository. This is an optional parameter and will be replaced with the code if the name is not present.

The path is the location where the XML source files for the repository are present.

The preferred metadata parameter sets the `metadataPrefix` parameter and is used when generating source URLs.

The default language is used to set a language on a metadata record that doesn't have a language specified in the record itself. This is an optional field and defaults to `osmhConsumer.oaiPmh.metadataParsingDefaultLang.lang` if not set. This setting is only considered if `osmhConsumer.oaiPmh.metadataParsingDefaultLang.active` is set to `true`.

## Built With

* [Maven](https://maven.apache.org/) - Dependency Management

## Contributing

Please read [Contributing to CESSDA Open Source Software](https://github.com/cessda/cessda.guidelines.public/src/main/CONTRIBUTING.md) for information on contribution to CESSDA software.

## Versioning

## Authors

You can find the list of all contributors [here](CONTRIBUTORS.md).

## License

This project is licensed under the Apache 2 Licence - see the [LICENSE](LICENSE.txt) file for details.

## Acknowledgments

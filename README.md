# RDF ABAC - Attribute-Based Access Control for RDF

RDF ABAC provides access control on data. Each data item has an associated attribute
expression and a data item is only visible to the application if the attribute
expression evaluates to "true".  The attributes are evaluated in the context of a data access
request (query); the user has a number of attributes which represent the
permissions. 

For example: a user making a query request has key-value attributes `"role=engineer"` and
`"status=employee"`. Data visible to the query request includes triples labelled
`"role=engineer"` or `"status=employee"`.

RDF ABAC consists of a ABAC security engine, 
an extension module for [Apache Jena Fuseki](https://jena.apache.org/documentation/fuseki2/),
and a security evaluation service to provide security verification
to non-JVM components of the system.

Documentation: [docs/abac](./docs/abac.md)

## Build

Run
```bash
   mvn clean install
```

which creates the `rdf-abac-fmod` module for Fuseki.

For test coverage report run
```bash
   mvn clean verify
```
Coverage report is then available in the `target/site/jacoco-aggregate` folder of the `rdf-abac-coverage-report` submodule.

## Usage

To use the library directly in your project:

```
    <dependency>
      <groupId>io.telicent.jena</groupId>
      <artifactId>rdf-abac-core</artifactId>
      <version>VERSION</version>
    </dependency>
```

## Use with Apache Jena Fuseki
This project uses the Apache Jena Fuseki Main server and is configured with a
Fuseki configuration file.

See [documentation](docs/abac-fuseki-server.md) for details on how to run the library within a local Fuseki Server.

See "[Configuring Fuseki](https://jena.apache.org/documentation/fuseki2/fuseki-configuration.html)"
for authentication.


### Release
#### Maven
On branch `main`:

Edit and commit `release-setup` to set the correct versions.

```
source release-setup
```
This prints the dry-run command.

If you have run this file, then change it, simply source the file again.

Dry run 
```
mvn $MVN_ARGS -DdryRun=true release:clean release:prepare
```

and for real

```
mvn $MVN_ARGS release:clean release:prepare
```

This updates the version number.

Note that there is no need to do a `mvn release:perform` as the GitHub Actions workflows automatically handles making
the release to Maven Central.

After release, do `git pull` to sync local and remote git repositories.

#### Github Release

After the maven actions above, GitHub will automatically recognise the release (with the actions defined [here]
(https://github.com/Telicent-io/rdf-abac/actions)) and create the corresponding entry within the "[Releases]
(https://github.com/Telicent-io/rdf-abac/releases)" 
section of the repo.  The release change notes are based upon the content present in the [`CHANGELOG.md`](CHANGELOG.md)
file.


### Rollback

If things go wrong, and it is just in the release steps:

```
mvn $MVN_ARGS release:rollback
```

otherwise, checkout out from git or reset the version manually with:

```
mvn versions:set -DnewVersion=...-SNAPSHOT
```

---
<div style="margin-left:5%; font-size: 80%;">
  <p>
  <b>Acknowledgements</b>
  </p><p>
  Apache, Apache Jena and associated open source project names are trademarks of the Apache Software Foundation.
  </p>
</div>

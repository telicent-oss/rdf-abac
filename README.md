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
```
   mvn clean install
```

which creates the `rdf-abac-fmod` module for Fuseki.

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

See "[Configuring Fuseki](https://jena.apache.org/documentation/fuseki2/fuseki-configuration.html)"
for authentication.

This project uses the Apache Jena Fuseki Main server and is configured with a
Fuseki configuration file.

The jar is `rdf-abac-fmod/target/rdf-abac-fmod-VER.jar`

Get a copy of Fuseki Main (check the value of `JENA_VER` with `ver.jena` property in the POM):

```
wget https://repo1.maven.org/maven2/org/apache/jena/jena-fuseki-server/JENA_VER/jena-fuseki-server-JENA_VER.jar
```

Get a copy of the script
[fuseki-main](https://github.com/Telicent-io/jena-fuseki-kafka/blob/main/fuseki-main).

Place the jar in a directory `lib/` then run:

```
fuseki-main jena-fuseki-server-JENA_VER.jar --conf config.ttl
```

where `config.ttl is the configuration file for the server including the
connector setup.

### Deploy with maven

Make sure you have authorized with AWS CodeArtifact (valid for 12 hours):

```
export CODEARTIFACT_AUTH_TOKEN=`aws codeartifact get-authorization-token --domain telicent --domain-owner 098669589541 --query authorizationToken --output text`
```

([Documentation](https://eu-west-2.console.aws.amazon.com/codesuite/codeartifact/d/098669589541/telicent/r/telicent-code-artifacts?packages-meta=eyJmIjp7fSwicyI6e30sIm4iOjIwLCJpIjowfQ&region=eu-west-2#).)

Run
```
   mvn clean deploy
```

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

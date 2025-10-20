package io.telicent.jena.abac.assembler;

import io.telicent.jena.abac.core.AttributesStoreAuthServer;
import io.telicent.jena.abac.core.AttributesStoreLocal;
import io.telicent.jena.abac.core.AttributesStoreRemote;
import io.telicent.jena.abac.core.DatasetGraphABAC;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFParserBuilder;
import org.apache.jena.sparql.core.DatasetGraph;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.nio.file.Files;

public class TestSecureDatasetAssembler {

    public static final String PREAMBLE = """
            @prefix :        <https://example.org/> .
            @prefix fuseki:  <http://jena.apache.org/fuseki#> .
            @prefix rdf:     <http://www.w3.org/1999/02/22-rdf-syntax-ns#> .
            @prefix rdfs:    <http://www.w3.org/2000/01/rdf-schema#> .
            @prefix ja:      <http://jena.hpl.hp.com/2005/11/Assembler#> .
            @prefix authz:   <http://telicent.io/security#> .
            """;
    protected static final String DATASET_URI = "https://example.org/dataset";

    private final SecuredDatasetAssembler assembler = new SecuredDatasetAssembler();
    private static File ATTRIBUTES_FILE;

    @BeforeAll
    public static void setup() throws IOException {
        ATTRIBUTES_FILE = Files.createTempFile("attributes-store", ".ttl").toFile();
    }

    private DatasetGraph parseConfigAndLoadDataset(String config) {
        Model model = RDFParserBuilder.create().source(new StringReader(config)).lang(Lang.TURTLE).toModel();
        Resource dsResource = model.getResource(DATASET_URI);
        return assembler.createDataset(assembler, dsResource);
    }

    private static void verifyAbacDataset(DatasetGraph dsg, Class<?> expectedType) {
        Assertions.assertNotNull(dsg);
        Assertions.assertInstanceOf(DatasetGraphABAC.class, dsg);
        DatasetGraphABAC abac = (DatasetGraphABAC) dsg;
        Assertions.assertNotNull(abac.attributesStore());
        Assertions.assertInstanceOf(expectedType, abac.attributesStore());
    }

    @Test
    public void givenConfigForDatasetForLocalAttributes_whenAssembling_thenDatasetLoaded() {
        // Given
        String config = PREAMBLE + """
                :dataset rdf:type authz:DatasetAuthz ;
                    authz:dataset :datasetBase ;
                    authz:attributes <file:ATTRS-FILE> .
                
                :datasetBase rdf:type ja:MemoryDataset .
                """;
        config = config.replace("ATTRS-FILE", ATTRIBUTES_FILE.getAbsolutePath());

        // When
        DatasetGraph dsg = parseConfigAndLoadDataset(config);

        // Then
        verifyAbacDataset(dsg, AttributesStoreLocal.class);
    }

    @Test
    public void givenConfigForDatasetWithRemoteAttributes_whenAssembling_thenDatasetLoaded() {
        // Given
        String config = PREAMBLE + """
                :dataset rdf:type authz:DatasetAuthz ;
                    authz:dataset :datasetBase ;
                    authz:attributesURL <https://localhost:12345/users/{user}> .
                
                :datasetBase rdf:type ja:MemoryDataset .
                """;

        // When
        DatasetGraph dsg = parseConfigAndLoadDataset(config);

        // Then
        verifyAbacDataset(dsg, AttributesStoreRemote.class);
    }

    @Test
    public void givenConfigForDatasetWithAuthServer_whenAssembling_thenDatasetLoaded() {
        // Given
        String config = PREAMBLE + """
                :dataset rdf:type authz:DatasetAuthz ;
                    authz:dataset :datasetBase;
                    authz:authServer true .
                
                :datasetBase rdf:type ja:MemoryDataset .
                """;

        // When
        DatasetGraph dsg = parseConfigAndLoadDataset(config);

        // Then
        verifyAbacDataset(dsg, AttributesStoreAuthServer.class);
    }

    @Test
    public void givenConfigForDatasetWithSharedAttributeStore_whenAssembling_thenDatasetLoaded() {
        // Given
        String config = PREAMBLE + """
                :dataset rdf:type authz:DatasetAuthz ;
                    authz:dataset :datasetBase;
                    authz:attributesStore :attrs .
                
                :attrs rdf:type authz:AttributesStore ;
                    authz:attributesURL <https://localhost:12345/users/{user}> .
                
                :datasetBase rdf:type ja:MemoryDataset .
                """;

        // When
        DatasetGraph dsg = parseConfigAndLoadDataset(config);

        // Then
        verifyAbacDataset(dsg, AttributesStoreRemote.class);
    }

}

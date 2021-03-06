/**
 * 
 */
package com.impetus.client.schemaManager;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.cassandra.thrift.Cassandra;
import org.apache.cassandra.thrift.InvalidRequestException;
import org.apache.cassandra.thrift.KsDef;
import org.apache.cassandra.thrift.NotFoundException;
import org.apache.thrift.TException;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.impetus.client.cassandra.pelops.PelopsClientFactory;
import com.impetus.client.cassandra.schemamanager.CassandraSchemaManager;
import com.impetus.client.persistence.CassandraCli;
import com.impetus.kundera.Constants;
import com.impetus.kundera.PersistenceProperties;
import com.impetus.kundera.configure.SchemaConfiguration;
import com.impetus.kundera.configure.schema.api.SchemaManager;
import com.impetus.kundera.metadata.model.ApplicationMetadata;
import com.impetus.kundera.metadata.model.ClientMetadata;
import com.impetus.kundera.metadata.model.EntityMetadata;
import com.impetus.kundera.metadata.model.KunderaMetadata;
import com.impetus.kundera.metadata.model.MetamodelImpl;
import com.impetus.kundera.metadata.model.PersistenceUnitMetadata;
import com.impetus.kundera.metadata.processor.TableProcessor;
import com.impetus.kundera.persistence.EntityManagerFactoryImpl;

/**
 * @author Kuldeep.Mishra
 * 
 */
public class CassandraPropertiesTest
{
    /** The configuration. */
    private SchemaConfiguration configuration;

    /** Configure schema manager. */
    private SchemaManager schemaManager;

    /**
     * cassandra client
     */
    private Cassandra.Client client;

    /**
     * keyspace to create.
     */
    private String keyspace = "KunderaExamplesTests1";

    /**
     * persistence unit pu.
     */
    private String pu = "CassandraPropertiesTest";

    /**
     * useLucene
     */
    private final boolean useLucene = true;

    /**
     * @throws java.lang.Exception
     */
    @Before
    public void setUp() throws Exception
    {
        configuration = new SchemaConfiguration(pu);
        CassandraCli.cassandraSetUp();
        client = CassandraCli.getClient();
    }

    /**
     * @throws java.lang.Exception
     */
    @After
    public void tearDown() throws Exception
    {
        CassandraCli.dropKeySpace(keyspace);
    }

    @Test
    public void test() throws NotFoundException, InvalidRequestException, TException, IOException
    {
        getEntityManagerFactory("create");
        schemaManager = new CassandraSchemaManager(PelopsClientFactory.class.getName());
        schemaManager.exportSchema();

        Properties properties = new Properties();
        InputStream inStream = ClassLoader.getSystemResourceAsStream("kundera-cassandra.properties");
        properties.load(inStream);
        String expected_replication = properties.getProperty("replication_factor");
        String expected_strategyClass = properties.getProperty("placement_strategy");
        KsDef ksDef = client.describe_keyspace(keyspace);
        Assert.assertEquals(Integer.parseInt(expected_replication), ksDef.getReplication_factor());
        Assert.assertEquals(expected_strategyClass, ksDef.getStrategy_class());
    }

    /**
     * Gets the entity manager factory.
     * 
     * @param useLucene
     * @param property
     * 
     * @return the entity manager factory
     */
    private EntityManagerFactoryImpl getEntityManagerFactory(String property)
    {
        ClientMetadata clientMetadata = new ClientMetadata();
        Map<String, Object> props = new HashMap<String, Object>();
        // String pu = pu;
        props.put(Constants.PERSISTENCE_UNIT_NAME, pu);
        props.put(PersistenceProperties.KUNDERA_CLIENT_FACTORY, PelopsClientFactory.class.getName());
        props.put(PersistenceProperties.KUNDERA_NODES, "localhost");
        props.put(PersistenceProperties.KUNDERA_PORT, "9160");
        props.put(PersistenceProperties.KUNDERA_KEYSPACE, keyspace);
        props.put(PersistenceProperties.KUNDERA_DDL_AUTO_PREPARE, property);
        if (useLucene)
        {
            props.put(PersistenceProperties.KUNDERA_INDEX_HOME_DIR, "/home/impadmin/lucene");

            clientMetadata.setLuceneIndexDir("/home/impadmin/lucene");
        }
        else
        {

            clientMetadata.setLuceneIndexDir(null);
        }

        KunderaMetadata.INSTANCE.setApplicationMetadata(null);
        ApplicationMetadata appMetadata = KunderaMetadata.INSTANCE.getApplicationMetadata();
        PersistenceUnitMetadata puMetadata = new PersistenceUnitMetadata();
        puMetadata.setPersistenceUnitName(pu);
        Properties p = new Properties();
        p.putAll(props);
        puMetadata.setProperties(p);
        Map<String, PersistenceUnitMetadata> metadata = new HashMap<String, PersistenceUnitMetadata>();
        metadata.put(pu, puMetadata);
        appMetadata.addPersistenceUnitMetadata(metadata);

        Map<String, List<String>> clazzToPu = new HashMap<String, List<String>>();

        List<String> pus = new ArrayList<String>();
        pus.add(pu);
        clazzToPu.put(Doctor.class.getName(), pus);

        appMetadata.setClazzToPuMap(clazzToPu);

        EntityMetadata m = new EntityMetadata(Doctor.class);

        TableProcessor processor = new TableProcessor();
        processor.process(Doctor.class, m);

        m.setPersistenceUnit(pu);

        MetamodelImpl metaModel = new MetamodelImpl();
        metaModel.addEntityMetadata(Doctor.class, m);

        appMetadata.getMetamodelMap().put(pu, metaModel);

        KunderaMetadata.INSTANCE.addClientMetadata(pu, clientMetadata);

        configuration.configure();
        return null;
    }
}

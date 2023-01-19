package forth.ics.isl.runnable;

import forth.ics.isl.service.DBService;
import forth.ics.isl.service.PropertiesService;
import forth.ics.isl.triplestore.VirtuosoRestClient;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;
import java.util.Set;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;

public class H2Manager {

    Statement statement;
    static Connection connection;

    public H2Manager() throws ClassNotFoundException, SQLException {
        String dbUrl, username, password;
        Properties h2Props = PropertiesService.getApplicationProperties();
        dbUrl = h2Props.getProperty("H2Manager.datasource.url");
        username = h2Props.getProperty("spring.datasource.username");
        password = h2Props.getProperty("spring.datasource.password");
        ///
        Class.forName("org.h2.Driver");
        connection = DriverManager.getConnection(dbUrl, username, password);
        statement = connection.createStatement();
    }

    public H2Manager(Statement statement, Connection connection) {
        this.statement = statement;
        H2Manager.connection = connection;
    }

    public void init() throws SQLException {

//        deleteTable("namedgraph_category");
//        deleteTable("namedgraph");
//        deleteTable("entity");
//        deleteTable("relation");
//        deleteTable("relations_material");
//        deleteTable("user_favorites");
//
//        createTableCategory();
//        createTableNamedgraph();
//        createTableEntity();
//        createTableRelation();
//        createTableRelationsMatUpdates();
//        createTableUserFavorites();
//
//        insertEntitiesVirtuoso();
//        insertNamedgraphCategories();
//        insertNamedgraphs();
//        insertRelationsMatUpdates();
        // NEW 
        deleteTable("entityspec");
        createTableEntitySpec();
        insertEntitySpec(
                "Person",
                "SELECT DISTINCT ?uri "
                + "[[FROM_GRAPHS]] "
                + "WHERE { ?uri a <http://www.cidoc-crm.org/cidoc-crm/E21_Person> } ",
                "SELECT DISTINCT ?uri ?firstname ?lastname ?birth_year ?profession "
                + "[[FROM_GRAPHS]] "
                + "WHERE { ?uri a <http://www.cidoc-crm.org/cidoc-crm/E21_Person> . OPTIONAL { ?uri <http://www.sealit.gr/ontology/sealit#has_first_name> ?firstname } OPTIONAL { ?uri <http://www.sealit.gr/ontology/sealit#has_last_name> ?lastname } OPTIONAL { ?uri <http://www.sealit.gr/ontology/sealit#has_profession> ?prof . ?prof <http://www.w3.org/2000/01/rdf-schema#label> ?profession } OPTIONAL { ?uri <http://www.cidoc-crm.org/cidoc-crm/P98i_was_born> ?born . ?born <http://www.cidoc-crm.org/cidoc-crm/P4_has_time-span> ?timespan . ?timespan <http://www.cidoc-crm.org/cidoc-crm/P82_at_some_time_within> ?birth_year } } ",
                "SELECT DISTINCT ?uri ?firstname ?lastname ?birth_year ?profession "
                + "[[FROM_GRAPHS]] "
                + "WHERE { ?uri a <http://www.cidoc-crm.org/cidoc-crm/E21_Person> ; <http://www.w3.org/2000/01/rdf-schema#label> ?label . ?label bif:contains [[FILTER]] . OPTIONAL { ?uri <http://www.sealit.gr/ontology/sealit#has_first_name> ?firstname } OPTIONAL { ?uri <http://www.sealit.gr/ontology/sealit#has_last_name> ?lastname } OPTIONAL { ?uri <http://www.sealit.gr/ontology/sealit#has_profession> ?prof . ?prof <http://www.w3.org/2000/01/rdf-schema#label> ?profession } OPTIONAL { ?uri <http://www.cidoc-crm.org/cidoc-crm/P98i_was_born> ?born . ?born <http://www.cidoc-crm.org/cidoc-crm/P4_has_time-span> ?timespan . ?timespan <http://www.cidoc-crm.org/cidoc-crm/P82_at_some_time_within> ?birth_year } } ",
                "SELECT ?subject ?predicate ?object "
                + "[[FROM_GRAPHS]] "
                + "WHERE { { ?subject ?predicate ?object FILTER (?subject = <[[URI]]>) } UNION { ?subject ?predicate ?object FILTER (?object = <[[URI]]>) } }",
                "",
                false);
        insertEntitySpec(
                "Place",
                "SELECT DISTINCT ?uri "
                + "[[FROM_GRAPHS]] "
                + "WHERE { ?uri a <http://www.cidoc-crm.org/cidoc-crm/E53_Place> } ",
                "SELECT DISTINCT ?uri ?name "
                + "[[FROM_GRAPHS]] "
                + "WHERE { ?uri a <http://www.cidoc-crm.org/cidoc-crm/E53_Place> . OPTIONAL { ?uri <http://www.w3.org/2000/01/rdf-schema#label> ?name } } ",
                "SELECT DISTINCT ?uri ?label "
                + "[[FROM_GRAPHS]] "
                + "WHERE { ?uri a <http://www.cidoc-crm.org/cidoc-crm/E53_Place> ; <http://www.w3.org/2000/01/rdf-schema#label> ?label . ?label bif:contains \"''[[TERM]]''\" } ",
                "SELECT ?subject ?predicate ?object "
                + "[[FROM_GRAPHS]] "
                + "WHERE { { ?subject ?predicate ?object FILTER (?subject = <[[URI]]>) } UNION { ?subject ?predicate ?object FILTER (?object = <[[URI]]>) } }",
                "",
                true);

        insertEntitySpec(
                "Ship",
                "SELECT DISTINCT ?uri "
                + "[[FROM_GRAPHS]] "
                + "WHERE { ?uri a <http://www.sealit.gr/ontology/sealit#Ship> } ",
                "SELECT DISTINCT ?uri ?name ?type ?tonnage "
                + "[[FROM_GRAPHS]] "
                + "WHERE { ?uri a <http://www.sealit.gr/ontology/sealit#Ship> . OPTIONAL { ?uri <http://www.w3.org/2000/01/rdf-schema#label> ?name } OPTIONAL { ?uri <http://www.cidoc-crm.org/cidoc-crm/P2_has_type> ?shiptype . ?shiptype <http://www.w3.org/2000/01/rdf-schema#label> ?type } OPTIONAL { ?uri <http://www.sealit.gr/ontology/sealit#has_tonnage> ?shiptonnage .  ?shiptonnage <http://www.cidoc-crm.org/cidoc-crm/P90_has_value> ?tonnage } } ",
                "SELECT DISTINCT ?uri ?label "
                + "[[FROM_GRAPHS]] "
                + "WHERE { ?uri a <http://www.sealit.gr/ontology/sealit#Ship> ; <http://www.w3.org/2000/01/rdf-schema#label> ?label . ?label bif:contains \"''[[TERM]]''\" } ",
                "SELECT ?subject ?predicate ?object "
                + "[[FROM_GRAPHS]] "
                + "WHERE { { ?subject ?predicate ?object FILTER (?subject = <[[URI]]>) } UNION { ?subject ?predicate ?object FILTER (?object = <[[URI]]>) } }",
                "",
                false);

        insertEntitySpec(
                "Voyage",
                "SELECT DISTINCT ?uri "
                + "[[FROM_GRAPHS]] "
                + "WHERE { ?uri a <http://www.sealit.gr/ontology/sealit#Voyage> } ",
                "SELECT DISTINCT ?uri ?name ?destination "
                + "[[FROM_GRAPHS]] "
                + "WHERE { ?uri a <http://www.sealit.gr/ontology/sealit#Voyage> . OPTIONAL { ?uri <http://www.w3.org/2000/01/rdf-schema#label> ?name } OPTIONAL { ?uri <http://www.sealit.gr/ontology/sealit#destination> ?voyageDestination . ?voyageDestination <http://www.w3.org/2000/01/rdf-schema#label> ?destination } } ",
                "SELECT DISTINCT ?uri ?label "
                + "[[FROM_GRAPHS]] "
                + "WHERE { ?uri a <http://www.sealit.gr/ontology/sealit#Voyage> ; <http://www.w3.org/2000/01/rdf-schema#label> ?label . ?label bif:contains \"''[[TERM]]''\" } ",
                "SELECT ?subject ?predicate ?object "
                + "[[FROM_GRAPHS]] "
                + "WHERE { { ?subject ?predicate ?object FILTER (?subject = <[[URI]]>) } UNION { ?subject ?predicate ?object FILTER (?object = <[[URI]]>) } }",
                "",
                false);

        deleteTable("relationspec");
        createTableRelationSpec();
        insertRelationSpec(
                "related to",
                1,
                2,
                "?person a <http://www.cidoc-crm.org/cidoc-crm/E21_Person> . ?place a  <http://www.cidoc-crm.org/cidoc-crm/E53_Place> . "
                + " ?person <http://www.cidoc-crm.org/cidoc-crm/P53_has_former_or_current_location> ?place ",
                "person",
                "place",
                "?person <http://www.w3.org/2000/01/rdf-schema#label> ?personlabel . ?personlabel bif:contains [[FILTER]]",
                "?place <http://www.w3.org/2000/01/rdf-schema#label> ?placelabel . ?placelabel bif:contains [[FILTER]]",
                "");

        insertRelationSpec(
                "was born",
                1,
                2,
                "?person a <http://www.cidoc-crm.org/cidoc-crm/E21_Person> . ?place a  <http://www.cidoc-crm.org/cidoc-crm/E53_Place> . "
                + " ?person <http://www.cidoc-crm.org/cidoc-crm/lelele> ?place ",
                "person",
                "place",
                "?person <http://www.w3.org/2000/01/rdf-schema#label> ?personlabel . ?personlabel bif:contains [[FILTER]]",
                "?place <http://www.w3.org/2000/01/rdf-schema#label> ?placelabel . ?placelabel bif:contains [[FILTER]]",
                "");

        insertRelationSpec(
                "related to",
                2,
                1,
                "?person a <http://www.cidoc-crm.org/cidoc-crm/E21_Person> . ?place a  <http://www.cidoc-crm.org/cidoc-crm/E53_Place> . "
                + " ?person <http://www.cidoc-crm.org/cidoc-crm/P53_has_former_or_current_location> ?place ",
                "place",
                "person",
                "?place <http://www.w3.org/2000/01/rdf-schema#label> ?placelabel . ?placelabel bif:contains [[FILTER]]",
                "?person <http://www.w3.org/2000/01/rdf-schema#label> ?personlabel . ?personlabel bif:contains [[FILTER]]",
                "");

        insertRelationSpec(
                "was crew at",
                1,
                3,
                "?person a <http://www.cidoc-crm.org/cidoc-crm/E21_Person> . ?ship a  <http://www.sealit.gr/ontology/sealit#Ship> . "
                + " ?ship <http://www.sealit.gr/ontology/sealit#voyages> ?voyage . ?voyage <http://www.cidoc-crm.org/cidoc-crm/P14_carried_out_by> ?person ",
                "person",
                "ship",
                "?person <http://www.w3.org/2000/01/rdf-schema#label> ?personlabel . ?personlabel bif:contains [[FILTER]]",
                "?ship <http://www.w3.org/2000/01/rdf-schema#label> ?shiplabel . ?shiplabel bif:contains [[FILTER]]",
                "");

        insertRelationSpec(
                "had crew",
                3,
                1,
                "?person a <http://www.cidoc-crm.org/cidoc-crm/E21_Person> . ?ship a  <http://www.sealit.gr/ontology/sealit#Ship> . "
                + " ?ship <http://www.sealit.gr/ontology/sealit#voyages> ?voyage . ?voyage <http://www.cidoc-crm.org/cidoc-crm/P14_carried_out_by> ?person ",
                "ship",
                "person",
                "?ship <http://www.w3.org/2000/01/rdf-schema#label> ?shiplabel . ?shiplabel bif:contains [[FILTER]]",
                "?person <http://www.w3.org/2000/01/rdf-schema#label> ?personlabel . ?personlabel bif:contains [[FILTER]]",
                "");
    }

    public int deleteTable(String tableName) throws SQLException {
        return statement.executeUpdate("drop table " + tableName + " if exists");
    }

    public int insertNamedGraph(String uri, String name, String description, int category) throws SQLException {
        return statement.executeUpdate("insert into namedgraph values ('" + uri + "','" + name + "','" + description + "'," + category + ")");
    }

    public int insertNamedGraphCategory(String name) throws SQLException {
        return statement.executeUpdate("insert into namedgraph_category (`name`,`description`) values ('" + name + "', '')");
    }

    public int insertEntity(String name, String uri, String thesaurus, String query, String geoQuery, String textGeoQuery, boolean geospatial, String selectionList, String selectionPattern, String keywordSearch, String geoSearch, String filterGeoSearch, String varName) throws SQLException {
        return statement.executeUpdate("insert into entity(`name`, `uri`, `thesaurus`, `query`, `geo_query`, `text_geo_query`, `geospatial`, `selection_list`, `selection_pattern`, `keyword_search`, `geo_search`, `filter_geo_search`, `var_name`)"
                + " values ('" + name + "','" + uri + "','" + thesaurus + "','" + query + "','" + geoQuery + "', '" + textGeoQuery + "', " + geospatial + ", '" + selectionList + "', '" + selectionPattern + "', '" + keywordSearch + "', '" + geoSearch + "', '" + filterGeoSearch + "', '" + varName + "')");
    }

    // NEW
    public int insertEntitySpec(String name, String instances_query, String data_query, String keyword_search_query, String properties_query, String materialised_uri, boolean geospatial) throws SQLException {
        return statement.executeUpdate("insert into entityspec(`name`, `instances_query`, `data_query`, `keyword_search_query`, `properties_query`, `materialised_uri`, `geospatial`)"
                + " values ('" + name + "','" + instances_query + "','" + data_query + "','" + keyword_search_query + "','" + properties_query + "', '" + materialised_uri + "', " + geospatial + ")");
    }

    // NEW
    public int insertRelationSpec(String name, int source_entity, int destination_entity, String relation_graph_pattern, String source_uri_variable, String destination_uri_variable, String source_filter_pattern, String destination_filter_pattern, String materialised_property) throws SQLException {
        return statement.executeUpdate("insert into relationspec(`name`, `source_entity`, `destination_entity`, `relation_graph_pattern`, `source_uri_variable`, `destination_uri_variable`, `source_filter_pattern`, `destination_filter_pattern`, `materialised_property`)"
                + " values ('" + name + "'," + source_entity + "," + destination_entity + ",'" + relation_graph_pattern + "','" + source_uri_variable + "','" + destination_uri_variable + "','" + source_filter_pattern + "','" + destination_filter_pattern + "','" + materialised_property + "')");
    }

    public boolean relationExists(String uri, String name, int sourceEntity, int destinationEntity, String graph) throws SQLException {
        ResultSet result = statement.executeQuery("SELECT * FROM RELATION where "
                + "URI='" + uri + "' AND "
                + "NAME = '" + name + "' AND "
                + "SOURCE_ENTITY = " + sourceEntity + " AND "
                + "DESTINATION_ENTITY = " + destinationEntity + " AND "
                + "GRAPH = '" + graph + "'");
        return result.next();
    }

    public int insertRelation(String uri, String name, int sourceEntity, int destinationEntity, String graph) throws SQLException {
        return statement.executeUpdate("insert into relation(`uri`, `name`, `source_entity`, `destination_entity`, `graph`)"
                + " values ('" + uri + "','" + name + "', " + sourceEntity + ", " + destinationEntity + ", '" + graph + "')");
    }

    public int insertRelationMatUpdate(String relatedEntities, String update) throws SQLException {
        return statement.executeUpdate("insert into relations_material(`related_entities`, `update`)"
                + " values ('" + relatedEntities + "', '" + update + "')");
    }

    public int updateEntityGeospatial(String entityName, String columnName, boolean columnValue) throws SQLException {
        return statement.executeUpdate("update entity set geospatial = " + columnValue + " where name = '" + entityName + "'");
    }

    public int createTableNamedgraph() throws SQLException {
        return statement.executeUpdate("CREATE TABLE namedgraph ( \n"
                + "uri varchar(30) not null, \n"
                + "name varchar(20), \n"
                + "description clob, \n"
                + "category int, \n"
                + "PRIMARY KEY (`uri`), \n"
                + "FOREIGN KEY (`category`) REFERENCES `namedgraph_category` (`id`) ON DELETE CASCADE"
                + ");");
    }

    public int createTableCategory() throws SQLException {
        return statement.executeUpdate("CREATE TABLE namedgraph_category ( \n"
                + "id int NOT NULL AUTO_INCREMENT, \n"
                + "name varchar(20), "
                + "description clob,\n"
                + "PRIMARY KEY (`id`)\n"
                + ");");
    }

    public ResultSet executeSelectQuery(String query) throws SQLException {
        return statement.executeQuery(query);
    }

    public int executeUpdateQuery(String update) throws SQLException {
        return statement.executeUpdate(update);
    }

    // NEW
    public int createTableEntitySpec() throws SQLException {
        return statement.executeUpdate("CREATE TABLE ENTITYSPEC ( \n"
                + "id int NOT NULL AUTO_INCREMENT, \n"
                + "name varchar(30), \n"
                + "instances_query clob, \n"
                + "data_query clob, \n"
                + "keyword_search_query clob, \n"
                + "properties_query clob, \n"
                + "materialised_uri clob, \n"
                + "geospatial boolean, \n"
                + "PRIMARY KEY (`id`)\n"
                + ");");
    }

    // NEW
    public int createTableRelationSpec() throws SQLException {
        return statement.executeUpdate("CREATE TABLE RELATIONSPEC ( \n"
                + "id int NOT NULL AUTO_INCREMENT, \n"
                + "name varchar(30), \n"
                + "source_entity int, \n"
                + "destination_entity int, \n"
                + "relation_graph_pattern clob, \n"
                + "source_uri_variable clob, \n"
                + "destination_uri_variable clob, \n"
                + "source_filter_pattern clob, \n"
                + "destination_filter_pattern clob, \n"
                + "materialised_property clob, \n"
                + "PRIMARY KEY (`id`)\n"
                + ");");
    }

    public int createTableEntity() throws SQLException {
        return statement.executeUpdate("CREATE TABLE entity ( \n"
                + "id int NOT NULL AUTO_INCREMENT, \n"
                + "uri clob, \n"
                + "name varchar(30), \n"
                + "query clob, \n"
                + "geo_query clob, \n"
                + "text_geo_query clob, \n"
                + "thesaurus varchar(50), \n"
                + "geospatial boolean, \n"
                + "selection_list clob, \n"
                + "selection_pattern clob, \n"
                + "keyword_search clob,\n"
                + "geo_search clob,\n"
                + "filter_geo_search clob,\n"
                + "var_name varchar(10),"
                + "PRIMARY KEY (`id`)\n"
                + ");");
    }

    public int createTableRelation() throws SQLException {
        return statement.executeUpdate("CREATE TABLE relation ( \n"
                + "id int NOT NULL AUTO_INCREMENT, \n"
                + "uri clob, \n"
                + "name clob, \n"
                + "source_entity int, \n"
                + "destination_entity int, \n"
                + "graph varchar(30),"
                + "PRIMARY KEY (`id`),\n"
                + "FOREIGN KEY (`source_entity`) REFERENCES `entity` (`id`) ON DELETE CASCADE,"
                + "FOREIGN KEY (`destination_entity`) REFERENCES `entity` (`id`) ON DELETE CASCADE,"
                + "FOREIGN KEY (`graph`) REFERENCES `namedgraph` (`uri`) ON DELETE CASCADE"
                + ");");
    }

    private int createTableRelationsMatUpdates() throws SQLException {
        return statement.executeUpdate("CREATE TABLE relations_material ( \n"
                + "id int NOT NULL AUTO_INCREMENT, \n"
                + "related_entities varchar(100),\n"
                + "update clob,\n"
                + "PRIMARY KEY (`id`)\n"
                + ");");
    }

    public int createTableUserFavorites() throws SQLException {
        return statement.executeUpdate("CREATE TABLE user_favorites ( \n"
                + "id int NOT NULL AUTO_INCREMENT, \n"
                + "username varchar(250),\n"
                + "title varchar(250),\n"
                + "description clob,\n"
                + "query_model clob,\n"
                + "PRIMARY KEY (`id`)\n"
                + ");");
    }

    public void terminate() throws SQLException {
        if (statement != null && !statement.isClosed()) {
            statement.close();
        }
        if (connection != null && !connection.isClosed()) {
            connection.close();
        }
    }


    private void insertNamedgraphs() throws SQLException {
        insertNamedGraph("http://example", "example", "", 1);

    }

    public ResultSet fetchEntities() throws SQLException {
        return statement.executeQuery("select * from entity");
    }

    public boolean namedGraphExists(String graphName) throws SQLException {
        ResultSet result = statement.executeQuery("SELECT * FROM NAMEDGRAPH where NAME='" + graphName + "'");
        return result.next();
    }

    public static void main(String[] args) throws ClassNotFoundException, SQLException, UnsupportedEncodingException, IOException, ParseException, Exception {
        H2Manager h2 = new H2Manager();
        h2.insertNamedGraph("http://namedgraph/", "name", "", 2);
        h2.terminate();

    }

    public static void enrichMatRelationsTable(String endpoint, String authorizationToken, String graphUri, Set<String> matRelEntityNames) throws SQLException, UnsupportedEncodingException, ClassNotFoundException, IOException {
        if (matRelEntityNames.isEmpty()) {
            return;
        }
        System.out.println("-> in H2Manager.java ");
        JSONArray matRelEntities = DBService.retrieveAllEntities(false);
        Connection conn = DBService.initConnection();
        H2Manager h2 = new H2Manager(conn.createStatement(), conn);
//        RestClient client = new RestClient(endpoint, namespace, authorizationToken);
        VirtuosoRestClient virtClient = new VirtuosoRestClient(endpoint, authorizationToken);
        ////////
        for (int i = 0; i < matRelEntities.size(); i++) {
            JSONObject targetEntity = (JSONObject) matRelEntities.get(i);
            String targetEntityURI = (String) targetEntity.get("uri");
            int targetEntityID = (int) targetEntity.get("id");
            String targetEntityName = (String) targetEntity.get("name");
            if (!matRelEntityNames.contains(targetEntityName)) {
                continue;
            }
            int cnt = 0;
            for (int j = 0; j < matRelEntities.size(); j++) {
                StringBuilder sparqlQuery = new StringBuilder();
                JSONObject relatedEntity = (JSONObject) matRelEntities.get(j);
                String relatedEntityURI = (String) relatedEntity.get("uri");
                int relatedEntityID = (int) relatedEntity.get("id");
                String relatedEntityName = (String) relatedEntity.get("name");
//                if (j == i) {
//                    continue;
//                }
                if (!matRelEntityNames.contains(relatedEntityName)) {
                    continue;
                }
                sparqlQuery.append("select distinct ?relation from <" + graphUri + "> where {\n").
                        append("?target_inst a <" + targetEntityURI + ">.\n").
                        append("?target_inst ?relation [a <" + relatedEntityURI + ">].\n").
                        append("}");
//                String response = client.executeSparqlQuery(sparqlQuery.toString(), "text/csv", 0).readEntity(String.class);
                String response = virtClient.executeSparqlQuery(sparqlQuery.toString(), "text/csv", 0).readEntity(String.class);
                String[] data = response.split("\\n");
                for (int k = 1; k < data.length; k++) {
                    String relationUri = data[k].replaceAll("\\\"", "");
                    String relationName = URLDecoder.decode(relationUri, "UTF-8").substring(relationUri.lastIndexOf("/") + 1);
                    if (!h2.relationExists(relationUri.trim(), relationName.trim(), targetEntityID, relatedEntityID, graphUri)) {
                        h2.insertRelation(relationUri.trim(), relationName.trim(), targetEntityID, relatedEntityID, graphUri);
                    }
                }
            }
        }
        h2.terminate();
    }

    public Connection getConnection() {
        return this.connection;
    }

}

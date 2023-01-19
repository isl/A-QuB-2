package forth.ics.isl.service;

import forth.ics.isl.data.model.RelationModel;
import forth.ics.isl.triplestore.VirtuosoRestClient;
import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.PostConstruct;
import javax.ws.rs.ClientErrorException;
import javax.ws.rs.core.Response;
import javax.sql.DataSource;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.CannotGetJdbcConnectionException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.stereotype.Repository;

@Repository
public class DBService {

    @Autowired
    private static JdbcTemplate jdbcTemplate;
    private static DataSource dataSource;
    private static final String inGraphProp = "http://in_graph";

    @Autowired
    public void setDataSource(DataSource dataSource) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
    }

    private static Connection connection;

    public static Connection getConnection() {
        return connection;
    }

    public static void setConnection(Connection connection) {
        DBService.connection = connection;
    }

    static boolean jdbcTemplateUsed;// = true;

    public static boolean isJdbcTemplateUsed() {
        return jdbcTemplateUsed;
    }

    public static void setJdbcTemplateUsed(boolean jdbcTemplateUsed) {
        DBService.jdbcTemplateUsed = jdbcTemplateUsed;
    }

    @PostConstruct
    public void init() {
        System.out.println(">>> In init() of DBService.java");
        System.out.println(" @PostConstruct - DBService");
        setJdbcTemplateUsed(true);
    }

    public static Connection initConnection() throws CannotGetJdbcConnectionException, SQLException {
        System.out.println(">>> In initConnection() of DBService.java");
        if (jdbcTemplateUsed) // Used only when a jdbcTemplate is spring injected
        {
            return DataSourceUtils.getConnection(jdbcTemplate.getDataSource());
        } else {
            String dbUrl, username, password;
            Properties h2Props = PropertiesService.getApplicationProperties();
            dbUrl = h2Props.getProperty("H2Manager.datasource.url");
            username = h2Props.getProperty("spring.datasource.username");
            password = h2Props.getProperty("spring.datasource.password");
            return DriverManager.getConnection(dbUrl, username, password);
        }
    }

    public static boolean isValidUser(String username, String password) {
        boolean isValid = false;
        System.out.println(">>> In isValidUser() of DBService.java");
        try {
            Connection conn = initConnection();
            Statement statement = conn.createStatement();
            String hassPass = hashPassword(password);
            ResultSet result = statement.executeQuery("select USERNAME, PASSWORD  from AQUB_USERS where USERNAME = '" + username + "' and password = '" + hassPass + "'");
            if (result.next()) {
                isValid = true;
            }
            System.out.println("isValidQ::" + isValid);
            result.close();
            statement.close();
            conn.close();
        } catch (SQLException ex) {
            Logger.getLogger(DBService.class.getName()).log(Level.SEVERE, null, ex);
        }
        return isValid;
    }

    public static JSONObject getProfile(String username) {
        System.out.println(">>> In getProfile of DBService.java");
        JSONObject responseJsonObject = new JSONObject();

        try {
            Connection conn = initConnection();
            Statement statement = conn.createStatement();
            ResultSet result = statement.executeQuery("SELECT USERNAME, PASSWORD, AQUB_USERS.NAME, EMAIL, ORGANIZATION, ORGANIZATIONURL , AQUB_ROLES.NAME as ROLENAME\n"
                    + "  FROM AQUB_ROLES , AQUB_USERS, AQUB_USERS_ROLES where USERNAME = '" + username + "' and AQUB_USERS.USER_ID =  AQUB_USERS_ROLES.USER_ID and AQUB_USERS_ROLES.ROLE_ID = AQUB_ROLES.ROLE_ID ");

            if (result.next()) {
                responseJsonObject.put("message", "success");
                responseJsonObject.put("email", result.getString("EMAIL"));
                responseJsonObject.put("name", result.getString("NAME"));
                responseJsonObject.put("organization", result.getString("ORGANIZATION"));
                responseJsonObject.put("organizationURL", result.getString("ORGANIZATIONURL"));
                responseJsonObject.put("password", result.getString("PASSWORD"));
                responseJsonObject.put("role", result.getString("ROLENAME"));
                responseJsonObject.put("userId", username);
            } else {
                responseJsonObject.put("message", "fail");

            }
            result.close();
            statement.close();
            conn.close();
        } catch (SQLException ex) {
            responseJsonObject.put("message", "fail");

            Logger.getLogger(DBService.class.getName()).log(Level.SEVERE, null, ex);
        }
        return responseJsonObject;
    }

    public static JSONObject getProfiles() {
        System.out.println(">>> In getProfiles of DBService.java");
        JSONObject responseJsonObject = new JSONObject();
        JSONArray userArr = new JSONArray();
        try {
            Connection conn = initConnection();
            Statement statement = conn.createStatement();
            ResultSet result = statement.executeQuery("SELECT USERNAME, PASSWORD, AQUB_USERS.NAME, EMAIL, ORGANIZATION, ORGANIZATIONURL , AQUB_ROLES.NAME as ROLENAME\n"
                    + "  FROM AQUB_ROLES , AQUB_USERS, AQUB_USERS_ROLES\n"
                    + "where AQUB_USERS.USER_ID =  AQUB_USERS_ROLES.USER_ID and AQUB_USERS_ROLES.ROLE_ID = AQUB_ROLES.ROLE_ID ");
            if (!result.next()) {
                responseJsonObject.put("message", "Error");

            }
            while (result.next()) {
                JSONObject userObj = new JSONObject();
                userObj.put("message", "success");
                userObj.put("email", result.getString("EMAIL"));
                userObj.put("name", result.getString("NAME"));
                userObj.put("organization", result.getString("ORGANIZATION"));
                userObj.put("organizationURL", result.getString("ORGANIZATIONURL"));
                userObj.put("password", result.getString("PASSWORD"));
                userObj.put("role", result.getString("ROLENAME"));
                userObj.put("userId", result.getString("USERNAME"));
                userArr.add(userObj);

            }
            responseJsonObject.put("response", userArr);

            result.close();
            statement.close();
            conn.close();
        } catch (SQLException ex) {
            responseJsonObject.put("message", "Error");
            Logger.getLogger(DBService.class.getName()).log(Level.SEVERE, null, ex);
        }
        return responseJsonObject;
    }

    public static JSONObject updateProfile(JSONObject userInfo) {
        System.out.println(">>> In updateProfile of DBService.java");
        JSONObject responseJsonObject = new JSONObject();
        try {
            String username = userInfo.get("userid").toString();
            String name = userInfo.get("name").toString();
            String email = userInfo.get("email").toString();

            String password = userInfo.get("password").toString();
            String hassPass = "";
            String passStr = "";
            if (password != null) {
                hassPass = hashPassword(password);
                passStr = ", password = '" + hassPass + "'\n";
            }
            String organization = userInfo.get("organization").toString();
            String organizationURL = userInfo.get("organizationURL").toString();
            String role = userInfo.get("role").toString();
            Connection conn = initConnection();

            String getRoleIdQ = "SELECT ROLE_ID \n"
                    + "  FROM AQUB_ROLES \n"
                    + "where name  = '" + role + "'";

            Statement statement = conn.createStatement();
            ResultSet result = statement.executeQuery(getRoleIdQ);
            String roleId = "";
            String roleIdStr = "";
            if (result.next()) {
                roleId = result.getString("ROLE_ID");
                roleIdStr = "SET ROLE_ID = '" + roleId + "'\n";
            }
            result.close();
            statement.close();

            String getUserIdQ = "SELECT USER_ID \n"
                    + "  FROM AQUB_USERS \n"
                    + "where username  = '" + username + "'";

            statement = conn.createStatement();
            result = statement.executeQuery(getUserIdQ);
            String userId = "";
            if (result.next()) {
                userId = result.getString("USER_ID");
            }
            result.close();
            statement.close();

            String updateQueryUsers = "UPDATE  AQUB_USERS \n"
                    + "SET AQUB_USERS.NAME = '" + name + "'\n"
                    + ", email = '" + email + "'\n"
                    + passStr
                    + ", organization = '" + organization + "'\n"
                    + ", organizationURL = '" + organizationURL + "'\n"
                    + "where username = '" + username + "'";

            statement = conn.createStatement();

            statement.executeUpdate(updateQueryUsers);

            String updateQueryRole = "update AQUB_USERS_ROLES \n"
                    + roleIdStr
                    + "where USER_ID = '" + userId + "'";
            Statement statement1 = conn.createStatement();
            statement1.executeUpdate(updateQueryRole);

            responseJsonObject.put("message", "Profile updated successfully");
            responseJsonObject.put("status", "SUCCEED");

            statement1.close();

            statement.close();

            conn.close();

        } catch (SQLException ex) {
            responseJsonObject.put("message", "Error");
            Logger.getLogger(DBService.class.getName()).log(Level.SEVERE, null, ex);
        }
        return responseJsonObject;
    }

    public static JSONObject registerUser(JSONObject userInfo) {
        System.out.println(">>> In registerUser of DBService.java");
        JSONObject responseJsonObject = new JSONObject();

        try {
            String username = userInfo.get("userid").toString();
            Connection conn = initConnection();
            Statement statement = conn.createStatement();
            ResultSet result = statement.executeQuery("select USERNAME from AQUB_USERS where USERNAME = '" + username + "'");
            if (result.next()) {
                responseJsonObject.put("message", "Username already exists");
                responseJsonObject.put("status", "FAIL");
                statement.close();
                conn.close();
                return responseJsonObject;
            }
            String name = userInfo.get("name").toString();
            String email = userInfo.get("email").toString();

            String password = userInfo.get("password").toString();
            String hassPass = "";
            if (password != null) {
                hassPass = hashPassword(password);
            }
            String organization = userInfo.get("organization").toString();
            String organizationURL = userInfo.get("organizationURL").toString();

            String insertUserQ = "insert INTO  AQUB_USERS (USERNAME, PASSWORD, NAME, EMAIL, ORGANIZATION, ORGANIZATIONURL, ENABLED)\n"
                    + " values('" + username + "', '" + hassPass + "', '" + name + "', '" + email + "', '" + organization + "', '" + organizationURL + "', TRUE);\n";
            System.out.println("insertUserQ " + insertUserQ);
            statement = conn.createStatement();

            statement.executeUpdate(insertUserQ);
            statement.close();

            String getUserIdQ = "SELECT USER_ID \n"
                    + "  FROM AQUB_USERS \n"
                    + "where username  = '" + username + "'";

            statement = conn.createStatement();
            result = statement.executeQuery(getUserIdQ);
            String userId = "";
            if (result.next()) {
                userId = result.getString("USER_ID");
                System.out.println("userId; " + userId);
            }
            result.close();
            statement.close();

            String inserRoleQ = "INSERT INTO AQUB_USERS_ROLES (USER_ID, ROLE_ID) \n"
                    + " values(" + userId + ", 1);";

            statement = conn.createStatement();
            statement.executeUpdate(inserRoleQ);
            statement.close();

            responseJsonObject.put("message", "Registration completed successfully!");
            responseJsonObject.put("status", "SUCCEED");

            conn.close();

        } catch (SQLException ex) {
            responseJsonObject.put("message", "Error");
            Logger.getLogger(DBService.class.getName()).log(Level.SEVERE, null, ex);
        }
        return responseJsonObject;
    }

    public static String hashPassword(String password) {
        String hashword = null;
        try {
            MessageDigest md5 = MessageDigest.getInstance("MD5");
            md5.update(password.getBytes());
            BigInteger hash = new BigInteger(1, md5.digest());
            hashword = hash.toString(16);
        } catch (NoSuchAlgorithmException nsae) {
// ignore
        }
        return hashword;
    }

    private String getFilePath(String fileName) {
        System.out.println(">>> In getFilePath() of DBService.java");
        // Get file from resources folder
        ClassLoader classLoader = getClass().getClassLoader();
        File file = new File(classLoader.getResource(fileName).getFile());
        return file.getAbsolutePath();
    }

    public static JSONObject retrieveEntityFromName(String entity) {
        System.out.println(">>> In retrieveEntityFromName() of DBService.java");
        JSONObject entityJSON = new JSONObject();
        try {
            Connection conn = initConnection();
            Statement statement = conn.createStatement();
            if (entity.equals("OrganisationUnit")) {
                entity = "Organisation";
            }
            ResultSet entities = statement.executeQuery("select * from entity where name = '" + entity + "'");
            while (entities.next()) {
                entityJSON.put("id", entities.getInt("id"));
                entityJSON.put("uri", entities.getString("uri"));
                entityJSON.put("name", entities.getString("name"));
                entityJSON.put("thesaurus", entities.getString("thesaurus"));
                JSONObject queryModel = new JSONObject();
                entityJSON.put("queryModel", queryModel);
                queryModel.put("format", "application/json");
                queryModel.put("query", entities.getString("query"));
                entityJSON.put("geospatial", entities.getBoolean("geospatial"));
                entityJSON.put("selection_list", entities.getString("selection_list"));
                entityJSON.put("keyword_search", entities.getString("keyword_search"));
                entityJSON.put("geo_search", entities.getString("geo_search"));
                entityJSON.put("filter_geo_search", entities.getString("filter_geo_search"));
                entityJSON.put("var_name", entities.getString("var_name"));
                entityJSON.put("selection_pattern", entities.getString("selection_pattern"));
            }
            entities.close();
            statement.close();
            conn.close();
        } catch (SQLException ex) {
            Logger.getLogger(DBService.class.getName()).log(Level.SEVERE, null, ex);
        }
        return entityJSON;
    }

    public static JSONObject retrieveEntityFromURI(String uri) {
        System.out.println(">>> In retrieveEntityFromURI() of DBService.java");
        JSONObject entityJSON = new JSONObject();
        try {
            Connection conn = initConnection();
            Statement statement = conn.createStatement();
            ResultSet entities = statement.executeQuery("select * from entity where uri = '" + uri + "'");
            while (entities.next()) {
                entityJSON.put("id", entities.getInt("id"));
                entityJSON.put("uri", entities.getString("uri"));
                entityJSON.put("name", entities.getString("name"));
                entityJSON.put("thesaurus", entities.getString("thesaurus"));
                JSONObject queryModel = new JSONObject();
                entityJSON.put("queryModel", queryModel);
                queryModel.put("format", "application/json");
                queryModel.put("query", entities.getString("query"));
                entityJSON.put("geospatial", entities.getBoolean("geospatial"));
                entityJSON.put("selection_list", entities.getString("selection_list"));
                entityJSON.put("keyword_search", entities.getString("keyword_search"));
                entityJSON.put("geo_search", entities.getString("geo_search"));
                entityJSON.put("filter_geo_search", entities.getString("filter_geo_search"));
                entityJSON.put("var_name", entities.getString("var_name"));
                entityJSON.put("selection_pattern", entities.getString("selection_pattern"));
            }
            entities.close();
            statement.close();
            conn.close();
        } catch (SQLException ex) {
            Logger.getLogger(DBService.class.getName()).log(Level.SEVERE, null, ex);
        }
        return entityJSON;
    }

    public static List<String> retrieveAllEntityNames() {
        System.out.println(">>> In retrieveAllEntityNames() of DBService.java");
        List<String> entities = new ArrayList<>();
        try {
            Connection conn = initConnection();
            Statement statement = conn.createStatement();
            ResultSet result = statement.executeQuery("select name from entity");
            while (result.next()) {
                entities.add(result.getString("name"));
            }
            result.close();
            statement.close();
            conn.close();
        } catch (SQLException ex) {
            Logger.getLogger(DBService.class.getName()).log(Level.SEVERE, null, ex);
        }
        return entities;
    }

    // NEW
    public static JSONArray retrieveAllEntitySpecs() {
        System.out.println(">>> In retrieveAllEntitySpecs() of DBService.java");
        JSONArray results = new JSONArray();
        try {
            Connection conn = initConnection();
            Statement statement = conn.createStatement();
            String query = "select * from entityspec";

            ResultSet entities = statement.executeQuery(query); // ignore location
            while (entities.next()) {
                JSONObject entity = new JSONObject();
                entity.put("id", entities.getInt("id"));
                entity.put("name", entities.getString("name"));
                entity.put("instances_query", entities.getString("instances_query"));
                entity.put("data_query", entities.getString("data_query"));
                entity.put("search_query", entities.getString("search_query"));
                entity.put("materialised_uri", entities.getString("materialised_uri"));
                entity.put("geospatial", entities.getString("geospatial"));
                results.add(entity);
            }
            entities.close();
            statement.close();
            conn.close();
        } catch (SQLException ex) {
            Logger.getLogger(DBService.class.getName()).log(Level.SEVERE, null, ex);
        }
        return results;
    }

    public static JSONArray retrieveAllEntities(boolean fetchTargetEntities) {
        System.out.println(">>> In retrieveAllEntities() of DBService.java");
        JSONArray results = new JSONArray();
        try {
            Connection conn = initConnection();
            Statement statement = conn.createStatement();
            String query = "select * from entity";
            if (fetchTargetEntities) {
                query += " where selection_list!=''";
            }
            ResultSet entities = statement.executeQuery(query); // ignore location
            while (entities.next()) {
                JSONObject entity = new JSONObject();
                entity.put("id", entities.getInt("id"));
                entity.put("name", entities.getString("name"));
                entity.put("thesaurus", entities.getString("thesaurus"));
                entity.put("uri", entities.getString("uri"));
                JSONObject queryModel = new JSONObject();
                entity.put("queryModel", queryModel);
                queryModel.put("format", "application/json");
                queryModel.put("query", entities.getString("query"));
                queryModel.put("geo_query", entities.getString("geo_query"));
                queryModel.put("text_geo_query", entities.getString("text_geo_query"));
                entity.put("geospatial", entities.getString("geospatial"));
                entity.put("selection_list", entities.getString("selection_list"));
                entity.put("keyword_search", entities.getString("keyword_search"));
                entity.put("geo_search", entities.getString("geo_search"));
                entity.put("filter_geo_search", entities.getString("filter_geo_search"));
                entity.put("var_name", entities.getString("var_name"));
                entity.put("selection_pattern", entities.getString("selection_pattern"));
                results.add(entity);
            }
            entities.close();
            statement.close();
            conn.close();
        } catch (SQLException ex) {
            Logger.getLogger(DBService.class.getName()).log(Level.SEVERE, null, ex);
        }
        return results;
    }

    public static List<String> retrieveAllNamedgraphUris() {
        System.out.println(">>> In retrieveAllNamedgraphUris() of DBService.java");
        List<String> uris = new ArrayList<>();
        try {
            Connection conn = initConnection();
            Statement statement = conn.createStatement();
            ResultSet result = statement.executeQuery("select uri from namedgraph");
            while (result.next()) {
                uris.add(result.getString("uri"));
            }
            result.close();
            statement.close();
            conn.close();
        } catch (SQLException ex) {
            Logger.getLogger(DBService.class.getName()).log(Level.SEVERE, null, ex);
        }
        return uris;
    }

    public static Map<String, String> retrieveAllNamedgraphUrisLabels() {
        System.out.println(">>> In retrieveAllNamedgraphUrisLabels() of DBService.java");
        Map<String, String> graphs = new HashMap<>();
        try {
            Connection conn = initConnection();
            Statement statement = conn.createStatement();
            ResultSet result = statement.executeQuery("select uri, name from namedgraph");
            while (result.next()) {
                graphs.put(result.getString("uri"), result.getString("name"));
            }
            result.close();
            statement.close();
            conn.close();
        } catch (SQLException ex) {
            Logger.getLogger(DBService.class.getName()).log(Level.SEVERE, null, ex);
        }
        return graphs;
    }

    public static JSONArray retrieveAllRelationsMatUpdates() {
        System.out.println(">>> In retrieveAllRelationsMatUpdates() of DBService.java");
        JSONArray queries = new JSONArray();
        try {
            Connection conn = initConnection();
            Statement statement = conn.createStatement();
            ResultSet result = statement.executeQuery("select related_entities, update from relations_material");
            while (result.next()) {
                JSONObject obj = new JSONObject();
                obj.put("update", result.getString("update"));
                obj.put("related_entities", result.getString("related_entities"));
                queries.add(obj);
            }
            result.close();
            statement.close();
            conn.close();
        } catch (SQLException ex) {
            Logger.getLogger(DBService.class.getName()).log(Level.SEVERE, null, ex);
        }
        return queries;
    }

    public static Map<String, String> retrieveAllRelations() {
        System.out.println(">>> In retrieveAllRelations() of DBService.java");
        Map<String, String> relations = new HashMap<>();
        try {
            Connection conn = initConnection();
            Statement statement = conn.createStatement();
            ResultSet result = statement.executeQuery("select uri, name from relation");
            int cnt = 0;
            while (result.next()) {
                relations.put(result.getString("uri"), result.getString("name"));
                cnt++;
            }
            System.out.println(cnt);
            result.close();
            statement.close();
            conn.close();
        } catch (SQLException ex) {
            Logger.getLogger(DBService.class.getName()).log(Level.SEVERE, null, ex);
        }
        return relations;
    }

    public static ArrayList<RelationModel> retrieveAllRelationsSpecs() {
        System.out.println(">>> In retrieveAllRelationsSpecs() of DBService.java");
        ArrayList<RelationModel> relations = new ArrayList<>();
        try {
            Connection conn = initConnection();
            Statement statement = conn.createStatement();
            ResultSet result = statement.executeQuery(
                    "select id, name, source_entity, destination_entity, relation_graph_pattern, source_uri_variable, destination_uri_variable, source_filter_pattern, destination_filter_pattern, materialised_property from relationspec");
            int cnt = 0;
            while (result.next()) {

                int id = result.getInt("id");
                String name = result.getString("name");
                int sourceEntity = result.getInt("source_entity");
                int destinationEntity = result.getInt("destination_entity");
                String relationGraphPattern = result.getString("relation_graph_pattern");
                String source_uri_variable = result.getString("source_uri_variable");
                String source_shown_variables = result.getString("source_shown_variables");
                String destination_uri_variable = result.getString("destination_uri_variable");
                String destination_shown_variables = result.getString("destination_shown_variables");
                String source_filter_pattern = result.getString("source_filter_pattern");
                String destination_filter_pattern = result.getString("destination_filter_pattern");
                RelationModel relModel = new RelationModel(id, name, sourceEntity, destinationEntity,
                        relationGraphPattern, source_uri_variable, source_shown_variables, destination_uri_variable, destination_shown_variables, source_filter_pattern,
                        destination_filter_pattern);
                relations.add(relModel);
                cnt++;
            }
            // System.out.println(" Num of relations: "+cnt);
            result.close();
            statement.close();
            conn.close();
        } catch (SQLException ex) {
            Logger.getLogger(DBService.class.getName()).log(Level.SEVERE, null, ex);
        }
        return relations;
    }

    public static JSONArray retrieveAllNamedgraphs() {
        System.out.println(">>> In retrieveAllNamedgraphs() of DBService.java");
        JSONArray results = new JSONArray();
        try {
            Connection conn = initConnection();
            Statement statement = conn.createStatement();
            ResultSet namedGraphs = statement.executeQuery("select g.uri, g.name, c.id, c.name from \n"
                    + "namedgraph g, namedgraph_category c where g.category = c.id");
            while (namedGraphs.next()) {
                String gUri = namedGraphs.getString(1);
                String gName = namedGraphs.getString(2);
                int cID = namedGraphs.getInt(3);
                String cName = namedGraphs.getString(4);
                boolean found = false;
                for (int i = 0; i < results.size(); i++) {
                    JSONObject category = (JSONObject) results.get(i);
                    if (cID == (int) category.get("id")) {
                        found = true;
                        JSONArray children = (JSONArray) category.get("children");
                        JSONObject child = new JSONObject();
                        child.put("id", gUri);
                        child.put("label", gName);
                        child.put("value", gUri);
                        children.add(child);
                    }
                }
                if (!found) {
                    JSONObject category = new JSONObject();
                    category.put("id", cID);
                    category.put("label", cName);
                    JSONArray children = new JSONArray();
                    category.put("children", children);
                    JSONObject child = new JSONObject();
                    child.put("id", gUri);
                    child.put("label", gName);
                    child.put("value", gUri);
                    children.add(child);
                    results.add(category);
                }
            }
            namedGraphs.close();
            statement.close();
            conn.close();
        } catch (SQLException ex) {
            Logger.getLogger(DBService.class.getName()).log(Level.SEVERE, null, ex);
        }
        return results;
    }

    public static JSONArray retrieveAllAndDefaultNamedgraphs() {
        System.out.println(">>> In retrieveAllNamedgraphs() of DBService.java");
        JSONArray results = new JSONArray();

        try {
            Connection conn = initConnection();
            Statement statement = conn.createStatement();
            ResultSet namedGraphs = statement.executeQuery("select g.uri, g.name, c.id, c.name from \n"
                    + "namedgraph g, namedgraph_category c where g.category = c.id");
            while (namedGraphs.next()) {
                String gUri = namedGraphs.getString(1);
                String gName = namedGraphs.getString(2);
                int cID = namedGraphs.getInt(3);
                String cName = namedGraphs.getString(4);
                boolean found = false;

                Properties properties = PropertiesService.getConfigProperties();
                String defaultNamedgraphs = properties.getProperty("defaultNamedgraphs");
                String[] selectedGraphs = {};
                if (defaultNamedgraphs != null && !defaultNamedgraphs.equals("")) {
                    selectedGraphs = defaultNamedgraphs.split(",");
                }

                for (int i = 0; i < results.size(); i++) {
                    JSONObject category = (JSONObject) results.get(i);
                    if (cID == (int) category.get("id")) {
                        found = true;
                        JSONArray children = (JSONArray) category.get("children");
                        JSONObject child = new JSONObject();
                        child.put("id", gUri);
                        child.put("label", gName);
                        child.put("value", gUri);
                        for (String selectedGraph : selectedGraphs) {
                            if (selectedGraph.trim().equals(gUri)) {
                                child.put("selected", true);
                                break;
                            } else {
                                child.put("selected", false);
                            }
                        }
                        children.add(child);
                    }
                }
                if (!found) {
                    JSONObject category = new JSONObject();
                    category.put("id", cID);
                    category.put("label", cName);
                    JSONArray children = new JSONArray();
                    category.put("children", children);
                    JSONObject child = new JSONObject();
                    child.put("id", gUri);
                    child.put("label", gName);
                    child.put("value", gUri);
                    for (String selectedGraph : selectedGraphs) {
                        if (selectedGraph.trim().equals(gUri)) {
                            child.put("selected", true);
                        } else {
                            child.put("selected", false);
                        }
                    }
                    children.add(child);
                    results.add(category);
                }
            }
            namedGraphs.close();
            statement.close();
            conn.close();
        } catch (SQLException | IOException ex) {
            Logger.getLogger(DBService.class.getName()).log(Level.SEVERE, null, ex);
        }
        return results;
    }

    public static JSONArray retrieveRelationsEntities(List<String> graphs, String targetEntityName,
            ArrayList<LinkedHashMap> entities) {
        System.out.println(">>> In retrieveRelationsEntities() of DBService.java");
        JSONObject targetEntity = DBService.retrieveEntityFromName(targetEntityName);
//        JSONArray entities = DBService.retrieveAllEntities();
        HashMap<Integer, JSONObject> entitiesMap = new HashMap<>();
        for (int i = 0; i < entities.size(); i++) {
            JSONObject entity = new JSONObject(entities.get(i));
            entitiesMap.put((int) entity.get("id"), entity);
        }
        try {
            Connection conn = initConnection();
            Statement statement = conn.createStatement();
            StringBuilder query = new StringBuilder(
                    "select distinct uri, name, destination_entity from relation where source_entity = "
                    + targetEntity.get("id") + " and (");
            int cnt = 0;
            for (String graph : graphs) {
                query.append("graph = '" + graph + "'");
                cnt++;
                if (cnt < graphs.size()) {
                    query.append(" or ");
                }
            }
            query.append(")");
            ResultSet relations = statement.executeQuery(query.toString());
            JSONArray result = new JSONArray();
            int id = 0;
            while (relations.next()) {
                String relationURI = relations.getString("uri");
                String relationName = relations.getString("name");
                JSONObject relatedEntity = entitiesMap.get(relations.getInt("destination_entity"));
                // if we want to omit an entity (e.g., location)
                if (relatedEntity == null) {
                    continue;
                }
                relatedEntity.put("id", id);
                JSONObject obj = new JSONObject();
                obj.put("related_entity", relatedEntity);
                JSONObject relJSON = new JSONObject();
                relJSON.put("uri", relationURI);
                relJSON.put("name", relationName);
                obj.put("relation", relJSON);
                result.add(obj);
                id++;
            }
            relations.close();
            statement.close();
            conn.close();
            return result;
        } catch (SQLException ex) {
            Logger.getLogger(DBService.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }

    public static JSONArray retrieveRelations(List<String> graphs, String targetEntityName, String relatedEntityName) {
        System.out.println(">>> In retrieveRelations() of DBService.java");
        JSONObject targetEntity = DBService.retrieveEntityFromName(targetEntityName);
        JSONObject relatedEntity = DBService.retrieveEntityFromName(relatedEntityName);
        try {
            Connection conn = initConnection();
            Statement statement = conn.createStatement();
            StringBuilder query = new StringBuilder("select * from relation where source_entity = "
                    + targetEntity.get("id") + " " + "and destination_entity = " + relatedEntity.get("id") + " and (");
            int cnt = 0;
            for (String graph : graphs) {
                query.append("graph = '" + graph + "'");
                cnt++;
                if (cnt < graphs.size()) {
                    query.append(" or ");
                }
            }
            query.append(")");
            ResultSet relations = statement.executeQuery(query.toString());
            JSONArray result = new JSONArray();
            while (relations.next()) {
                String relationURI = relations.getString("uri");
                String relationName = relations.getString("name");
                JSONObject obj = new JSONObject();
                obj.put("uri", relationURI);
                obj.put("name", relationName);
                result.add(obj);
            }
            relations.close();
            statement.close();
            conn.close();
            return result;
        } catch (SQLException ex) {
            Logger.getLogger(DBService.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }

    public static Set<String> retrieveRelationNames(List<String> graphs, String targetEntityName,
            String relatedEntityName) {
        System.out.println(">>> In retrieveRelationNames() of DBService.java");
        JSONObject targetEntity = DBService.retrieveEntityFromName(targetEntityName);
        JSONObject relatedEntity = DBService.retrieveEntityFromName(relatedEntityName);
        try {
            Connection conn = initConnection();
            Statement statement = conn.createStatement();
            StringBuilder query = new StringBuilder("select * from relation where source_entity = "
                    + targetEntity.get("id") + " " + "and destination_entity = " + relatedEntity.get("id") + " and (");
            int cnt = 0;
            for (String graph : graphs) {
                query.append("graph = '" + graph + "'");
                cnt++;
                if (cnt < graphs.size()) {
                    query.append(" or ");
                }
            }
            query.append(")");
            ResultSet relations = statement.executeQuery(query.toString());
            Set<String> result = new HashSet();
            while (relations.next()) {
                String relationName = relations.getString("name");
                result.add(relationName);
            }
            relations.close();
            statement.close();
            conn.close();
            return result;
        } catch (SQLException ex) {
            Logger.getLogger(DBService.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }

    public static JSONObject saveIntoFavorites(String username, String title, String description, String queryModel, String sparql,
            String favoriteId) {
        System.out.println(">>> In saveIntoFavorites() of DBService.java");
        JSONObject statusObject = new JSONObject();
        try {
            Connection conn = initConnection();

            String sql = "";
            PreparedStatement preparedStatement = conn.prepareStatement(sql);

            if (favoriteId == null) {
                sql = "INSERT INTO user_favorites (username, title, description, query_model, sparql)" + "VALUES (?, ?, ?, ?, ?)";

                preparedStatement = conn.prepareStatement(sql);
                preparedStatement.setString(1, username);
                preparedStatement.setString(2, title);
                preparedStatement.setString(3, description);
                preparedStatement.setString(4, queryModel);
                preparedStatement.setString(5, sparql);

            } else {
                sql = "UPDATE user_favorites " + "SET query_model=?, sparql=? " + "WHERE id=?";

                preparedStatement = conn.prepareStatement(sql);
                preparedStatement.setString(1, queryModel);
                preparedStatement.setString(2, sparql);
                preparedStatement.setString(3, favoriteId);
            }

            preparedStatement.executeUpdate();

            // Get the autogenerated id
            ResultSet generatedKeys = preparedStatement.getGeneratedKeys();
            if (generatedKeys.next()) {
                long id = generatedKeys.getLong(1);
                statusObject.put("generatedId", id);
            }

            if (preparedStatement != null) {
                preparedStatement.close();
            }
            if (conn != null) {
                conn.close();
            }

            statusObject.put("dbStatus", "success");

        } catch (SQLException ex) {
            Logger.getLogger(DBService.class.getName()).log(Level.SEVERE, null, ex);
            statusObject.put("dbStatus", "fail");
        }
        return statusObject;
    }

    public static JSONObject removeFromFavoritesById(String id) {
        System.out.println(">>> In removeFromFavoritesById() of DBService.java");
        JSONObject statusObject = new JSONObject();
        try {
            Connection conn = initConnection();

            String sql = "DELETE FROM user_favorites WHERE id = ?";
            PreparedStatement preparedStatement = conn.prepareStatement(sql);
            preparedStatement.setString(1, id);
            preparedStatement.executeUpdate();

            if (preparedStatement != null) {
                preparedStatement.close();
            }
            if (conn != null) {
                conn.close();
            }

            statusObject.put("dbStatus", "success");

        } catch (SQLException ex) {
            Logger.getLogger(DBService.class.getName()).log(Level.SEVERE, null, ex);
            statusObject.put("dbStatus", "fail");
        }
        return statusObject;
    }

    public static JSONObject retrieveFavoriteQueryModelsByUsername(String usernameStr) throws ParseException {
        System.out.println(">>> In retrieveFavoriteQueryModelsByUsername() of DBService.java");
        JSONObject statusObject = new JSONObject();
        try {

            JSONArray favoriteModels = new JSONArray();

            Connection conn = initConnection();
            String sql = "SELECT * FROM user_favorites WHERE username = ?";
            PreparedStatement preparedStatement = conn.prepareStatement(sql);
            preparedStatement.setString(1, usernameStr);

            ResultSet rs = preparedStatement.executeQuery();
            while (rs.next()) {
                long favoriteId = rs.getLong("id");
                String username = rs.getString("username");
                String title = rs.getString("title");
                String description = rs.getString("description");
                String queryModel = rs.getString("query_model");

                JSONObject favoriteModel = new JSONObject();
                favoriteModel.put("favoriteId", favoriteId);
                favoriteModel.put("username", username);
                favoriteModel.put("title", title);
                favoriteModel.put("description", description);
                JSONParser parser = new JSONParser();
                JSONObject queryModelJson = (JSONObject) parser.parse(queryModel);
                favoriteModel.put("queryModel", queryModelJson);
                favoriteModels.add(favoriteModel);
            }

            if (rs != null) {
                rs.close();
            }
            if (preparedStatement != null) {
                preparedStatement.close();
            }
            if (conn != null) {
                conn.close();
            }

            statusObject.put("dbStatus", "success");
            statusObject.put("favoriteModels", favoriteModels);

        } catch (SQLException ex) {
            Logger.getLogger(DBService.class.getName()).log(Level.SEVERE, null, ex);
            statusObject.put("dbStatus", "fail");
        }
        return statusObject;
    }

    public static JSONObject retrieveFavoriteQueriesByUsername(String usernameStr) throws ParseException {
        System.out.println(">>> In retrieveFavoriteQueriesByUsername() of DBService.java");
        JSONObject statusObject = new JSONObject();
        try {
            JSONArray favoriteModels = new JSONArray();
            Connection conn = initConnection();
            String sql = "SELECT * FROM user_favorites WHERE username = ?";
            PreparedStatement preparedStatement = conn.prepareStatement(sql);
            preparedStatement.setString(1, usernameStr);
            ResultSet rs = preparedStatement.executeQuery();
            while (rs.next()) {
                long favoriteId = rs.getLong("id");
                String username = rs.getString("username");
                String title = rs.getString("title");
                String description = rs.getString("description");
                String sparql = rs.getString("sparql");
                JSONObject favoriteModel = new JSONObject();
                favoriteModel.put("favoriteId", favoriteId);
                favoriteModel.put("username", username);
                favoriteModel.put("sparql", sparql);
                favoriteModel.put("title", title);
                favoriteModel.put("description", description);
                favoriteModels.add(favoriteModel);
            }
            if (rs != null) {
                rs.close();
            }
            if (preparedStatement != null) {
                preparedStatement.close();
            }
            if (conn != null) {
                conn.close();
            }

            statusObject.put("dbStatus", "success");
            statusObject.put("favoriteQueries", favoriteModels);

        } catch (SQLException ex) {
            Logger.getLogger(DBService.class.getName()).log(Level.SEVERE, null, ex);
            statusObject.put("dbStatus", "fail");
        }
        return statusObject;
    }

//    for each considered entity stored in the H2 database, we execute a query to 
//    link its corresponging instance with the namedgraph it belongs
    public static List<String> executeBelongsInQueries(String endpoint, String authorizationToken, String graphUri,
            String graphLabel) throws Exception {
        System.out.println(">>> In executeBelongsInQueries() of DBService.java");
        VirtuosoRestClient client = new VirtuosoRestClient(endpoint, authorizationToken);
        JSONArray entities = DBService.retrieveAllEntities(true);
        List<String> entityUris = new ArrayList<>();
        for (int i = 0; i < entities.size(); i++) {
            JSONObject entity = (JSONObject) entities.get(i);
            String entityURI = (String) entity.get("uri");
            String query = "with <" + graphUri + ">\n" + "insert{\n" + "?uri <" + inGraphProp + "> \"" + graphLabel
                    + "\".\n" + "} where {\n" + "?uri a <" + entityURI + ">.\n" + "}";
            entityUris.add(entityURI);
            Response response = client.executeUpdatePOSTJSON(query);
            int status = response.getStatus();
            if (status != 200) {
                return null;
            }
        }
        return entityUris;

    }

//    executes the SPARQL update queries stored in table RELATIONS_MATERIAL 
//    of the H2 database to materialize the relation "shortcuts" between the 
//    interesting entities for the GUI
    public static Set<String> executeRelationsMatQueries(String endpoint, String namespace, String authorizationToken,
            String graphUri) throws SQLException, ParseException, ClientErrorException, IOException {
        System.out.println(">>> In executeRelationsMatQueries() of DBService.java");
        Set<String> matRelationsEntities = new HashSet<>();
//        RestClient client = new RestClient(endpoint, namespace, authorizationToken);
        VirtuosoRestClient client = new VirtuosoRestClient(endpoint, authorizationToken);
        JSONArray updates = DBService.retrieveAllRelationsMatUpdates();
        StringBuilder sb = new StringBuilder();
        sb.append(graphUri + "\n");
        for (int i = 0; i < updates.size(); i++) {
            JSONObject obj = (JSONObject) updates.get(i);
            String update = ((String) obj.get("update")).replace("@#$%FROM%$#@", "<" + graphUri + ">");
            String relatedEntities = (String) obj.get("related_entities");
            ///
            relatedEntities = relatedEntities.replaceAll("OrganisationUnit", "Organisation");
            ///
//            Response response = client.executeUpdatePOSTJSON(update, namespace, authorizationToken);
            Response response = client.executeUpdatePOSTJSON(update);
            int status = response.getStatus();
            String respString = response.readEntity(String.class);
            // the update query added new triples
            if (status == 200 && !respString.contains("mutationCount=0")) {
                String[] entit = relatedEntities.split("-");
                if (entit.length > 1) {
                    matRelationsEntities.add(entit[0]);
                    matRelationsEntities.add(entit[1]);
                }
            }
            sb.append(
                    relatedEntities + " -> " + ((JSONObject) new JSONParser().parse(respString)).get("status") + "\n");
        }
        sb.append("-------\n");
        System.out.println(sb.toString());
        return matRelationsEntities;
    }

}

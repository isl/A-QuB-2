package forth.ics.isl.service;

import forth.ics.isl.data.model.parser.Utils;
import forth.ics.isl.triplestore.VirtuosoRestClient;
import java.io.IOException;
import java.net.URL;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.ws.rs.core.Response;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

public class BeautifyQueryResultsService {

    private static final String VREPrefix = "http://139.91.183.70:8090/vre4eic/";
    private static final String CERIFPrefix = "http://eurocris.org/ontology/cerif#";
    ///////
    private String authorizationToken;
    private String endpoint;
    private String namespace;

    private JSONObject instanceInfo;

    public JSONObject getInstanceRelations() {
        return instanceInfo;
    }

    public BeautifyQueryResultsService(String authorizationToken, String endpoint) {
        this.authorizationToken = authorizationToken;
        this.endpoint = endpoint;
//        this.namespace = namespace;
        this.instanceInfo = new JSONObject();
        this.instanceInfo.put("related_entity_types", new JSONArray());
    }

    public void enrichEntityClassifications(String entityUri, String fromClause) throws IOException, ParseException {
        String query = "prefix cerif: <http://eurocris.org/ontology/cerif#>\n"
                + "prefix vre4eic: <http://139.91.183.70:8090/vre4eic/>\n"
                + "select distinct ?instance_uri ?instance_term ?instance_role ?type " + fromClause + " where \n"
                + "{\n"
                + "  ?instance_uri <http://eurocris.org/ontology/cerif#is_source_of> ?pou. \n"
                + "  ?pou a <http://eurocris.org/ontology/cerif#SimpleLinkEntity>. \n"
                + "  ?pou <http://eurocris.org/ontology/cerif#has_classification> ?classif. \n"
                + "  ?classif <http://eurocris.org/ontology/cerif#has_term> ?instance_term. \n"
                + "  ?classif <http://eurocris.org/ontology/cerif#has_roleExpression> ?instance_role. \n"
                + "  ?classif a ?type. \n"
                + "  filter (?instance_uri = <" + entityUri + ">).\n"
                + "}";
        VirtuosoRestClient client = new VirtuosoRestClient(endpoint, authorizationToken);
        Response resp = client.executeSparqlQuery(query, "application/json", 0);
        JSONParser parser = new JSONParser();
        JSONObject result = (JSONObject) parser.parse(resp.readEntity(String.class));
        JSONArray results = (JSONArray) ((JSONObject) result.get("results")).get("bindings");
        HashSet<JSONObject> relEntitiesSet = null;
        JSONObject entitiesOfType = null;
        String relatedEntityType = "";
        for (int i = 0; i < results.size(); i++) {
            JSONObject row = (JSONObject) results.get(i);
            String type = getJSONObjectValue(row, "type");
            if (!relatedEntityType.equals(type)) {
                entitiesOfType = new JSONObject();
                relEntitiesSet = new HashSet<>();
                entitiesOfType.put("related_entity_type", getJSONObjectValue(row, "type").replace(CERIFPrefix, ""));
                entitiesOfType.put("related_entities_of_type", relEntitiesSet);
                ((JSONArray) instanceInfo.get("related_entity_types")).add(entitiesOfType);
                relatedEntityType = type;
            }
            ///
            String instanceTerm = getJSONObjectValue(row, "instance_term");
            String instanceRole = getJSONObjectValue(row, "instance_role");
            JSONObject relEntity = new JSONObject();
            relEntity.put("relation", instanceRole);
            relEntity.put("term", instanceTerm);
            relEntitiesSet.add(relEntity);
        }
    }

    public void enrichAllInAndOutProperties(String entityUri, String fromClause) throws IOException, ParseException {
        System.out.println("==>Getting all IN and Out properties! ");
        String query = "SELECT ?subject ?predicate ?object [[FROM_GRAPHS]] WHERE { { ?subject ?predicate ?object FILTER (?subject = <[[URI]]>) } UNION { ?subject ?predicate ?object FILTER (?object = <[[URI]]>) } }";
        query = query.replace("[[FROM_GRAPHS]]", fromClause);
        query = query.replace("[[URI]]", entityUri);

        VirtuosoRestClient client = new VirtuosoRestClient(endpoint, authorizationToken);
        Response resp = client.executeSparqlQuery(query, "application/json", 0);
        JSONParser parser = new JSONParser();
        JSONObject result = (JSONObject) parser.parse(resp.readEntity(String.class));
        JSONArray results = (JSONArray) ((JSONObject) result.get("results")).get("bindings");
        //System.out.println(results);
        JSONArray newResultsArray = new JSONArray();

        for (int i = 0; i < results.size(); i++) {
            JSONObject row = (JSONObject) results.get(i);
            String subject = getJSONObjectValue(row, "subject");
            String predicate = getJSONObjectValue(row, "predicate");
            String object = getJSONObjectValue(row, "object");
//            System.out.println("  >subject: " + subject);
//            System.out.println("  >predicate: " + predicate);
//            System.out.println("  >object: " + object);
            JSONObject resultObj = new JSONObject();

            resultObj.put("subject", subject);
            resultObj.put("predicate", predicate);
            resultObj.put("object", object);
            newResultsArray.add(resultObj);
        }
        //System.out.println("json results: "+newResultsArray);
        instanceInfo.put("entityUri", entityUri);
        instanceInfo.put("triples", newResultsArray);
    }

    public void enrichEntityResults(String entityUri, String fromClause) throws IOException, ParseException {
        String query = "prefix cerif: <http://eurocris.org/ontology/cerif#>\n"
                + "prefix vre4eic: <http://139.91.183.70:8090/vre4eic/>\n"
                + "select distinct  ?instance_uri "
                + "?instance_label "
                + "?instance_name "
                + "?instance_abstract "
                + "?instance_descr "
                + "GROUP_CONCAT(?instance_keyw ; separator=\", \") as ?instance_params "
                + "?instance_title "
                + "?instance_acronym "
                + "?instance_type "
                + "?instance_ext_uri \n"
                //                + "?instance_classif "
                + fromClause + " where \n"
                + "{\n"
                + "  ?instance_uri a ?instance_type. \n"
                + "  optional { ?instance_uri rdfs:label ?instance_label .}\n"
                + "  optional { ?instance_uri cerif:has_URI ?instance_ext_uri.}\n"
                + "  optional { ?instance_uri cerif:has_name ?instance_name.}\n"
                + "  optional { ?instance_uri cerif:has_abstract ?instance_abstract.}\n"
                + "  optional { ?instance_uri cerif:has_description ?instance_descr.}\n"
                + "  optional { ?instance_uri cerif:has_keywords ?instance_keyw.}\n"
                + "  optional { ?instance_uri cerif:has_title ?instance_title.}\n"
                + "  optional { ?instance_uri cerif:has_acronym ?instance_acronym.}\n"
                //                + "  optional { ?instance_uri cerif:has_description ?instance_description. }\n"
                //                + "  optional { ?instance_uri <http://eurocris.org/ontology/cerif#is_source_of> ?pou. \n"
                //                + "  ?pou a <http://eurocris.org/ontology/cerif#SimpleLinkEntity>. \n"
                //                + "  ?pou <http://eurocris.org/ontology/cerif#has_classification> [<http://eurocris.org/ontology/cerif#has_term> ?instance_classif]. } \n"
                + "  filter (?instance_uri = <" + entityUri + ">).\n"
                + "} limit 1";
//        RestClient client = new RestClient(endpoint, namespace, authorizationToken);
        VirtuosoRestClient client = new VirtuosoRestClient(endpoint, authorizationToken);
        Response resp = client.executeSparqlQuery(query, "application/json", 0);
        JSONParser parser = new JSONParser();
        JSONObject result = (JSONObject) parser.parse(resp.readEntity(String.class));
        JSONArray results = (JSONArray) ((JSONObject) result.get("results")).get("bindings");
        if (!results.isEmpty()) {
            JSONObject row = (JSONObject) results.get(0);
            String instanceUri = getJSONObjectValue(row, "instance_uri");
            String instanceType = getJSONObjectValue(row, "instance_type");
            String instanceLabel = getJSONObjectValue(row, "instance_label");
            String instanceDescr = getJSONObjectValue(row, "instance_descr");
            String instanceParams = getJSONObjectValue(row, "instance_params");
            String instanceName = getJSONObjectValue(row, "instance_name");
            String instanceAbstract = getJSONObjectValue(row, "instance_abstract");
            String instanceTitle = getJSONObjectValue(row, "instance_title");
            String instanceAcronym = getJSONObjectValue(row, "instance_acronym");
            String instanceExtUri = getJSONObjectValue(row, "instance_ext_uri");
            ////
            instanceInfo.put("instance_uri", instanceUri);
            if (instanceName != null) {
                instanceInfo.put("instance_name", instanceName);
            }
            if (instanceAbstract != null) {
                instanceInfo.put("instance_abstract", instanceAbstract);
            }
            if (instanceTitle != null) {
                instanceInfo.put("instance_title", instanceTitle);
            }
            if (instanceDescr != null) {
                //quick and dirty solution for the workflows local path)
                if (instanceType.endsWith("Workflow") && instanceExtUri != null) {
                    instanceDescr += " (local path: " + instanceExtUri + ")";
                }
                ////
                instanceInfo.put("instance_description", instanceDescr);
            }
            if (instanceParams != null) {
                instanceInfo.put("instance_params", instanceParams);
            }
            if (instanceAcronym != null) {
                instanceInfo.put("instance_acronym", instanceAcronym);
            }
            if (instanceExtUri != null) {
                try {
                    URL url = new URL(instanceExtUri);
                    url.toURI();
                    instanceInfo.put("instance_ext_uri", instanceExtUri);
                } catch (Exception ex) {
                    ;
                }
            }
            if (instanceName == null && instanceTitle == null) {
                instanceInfo.put("instance_label", instanceLabel);
            }
            instanceInfo.put("instance_type", instanceType.replace(CERIFPrefix, ""));
        }
    }

    public void enrichDstEntityResults(String entityUri, String fromClause) throws IOException, ParseException {
        String query = "prefix cerif: <http://eurocris.org/ontology/cerif#>\n"
                + "prefix vre4eic: <http://139.91.183.70:8090/vre4eic/>\n"
                + "select distinct  ?ent ?role ?roleOpposite ?term ?ent_label ?ent_name ?ent_title ?ent_acronym ?ent_type\n"
                + fromClause + " where \n"
                + "{\n"
                + "  ?instance_uri cerif:is_source_of ?x.\n"
                + "  ?x rdfs:label ?xlabel; \n"
                + "     cerif:has_classification ?classif;\n"
                + "     cerif:has_destination ?ent.\n"
                + "  ?classif cerif:has_roleExpression ?role. \n"
                + "  ?classif cerif:has_roleExpressionOpposite ?roleOpposite. \n"
                + "  ?classif cerif:has_term ?term. \n"
                + "  ?ent a ?ent_type.\n"
                + "  ?ent rdfs:label ?ent_label.\n"
                + "  optional {?ent cerif:has_name ?ent_name.} \n"
                + "  optional {?ent cerif:has_title ?ent_title.} \n"
                + "  optional {?ent cerif:has_acronym ?ent_acronym.} \n"
                //                + "   optional {?ent cerif:has_description ?ent_description. }\n"
                + "  filter (?instance_uri = <" + entityUri + ">).\n"
                + "} order by ?ent_type";
//        RestClient client = new RestClient(endpoint, namespace, authorizationToken);
        VirtuosoRestClient client = new VirtuosoRestClient(endpoint, authorizationToken);
        Response resp = client.executeSparqlQuery(query, "application/json", 0);

        manageQueryResults(resp, fromClause);
    }

    public void enrichSrcEntityResults(String entityUri, String fromClause) throws IOException, ParseException {
        String query = "prefix cerif: <http://eurocris.org/ontology/cerif#>\n"
                + "prefix vre4eic: <http://139.91.183.70:8090/vre4eic/>\n"
                + "select distinct ?ent ?role ?roleOpposite ?term ?ent_label ?ent_name ?ent_title ?ent_acronym ?ent_type \n"
                + fromClause + " where \n"
                + "{\n"
                + "  ?ent cerif:is_source_of ?pfle. \n"
                + "  ?pfle rdfs:label ?pflelabel; \n"
                + "        cerif:has_classification ?classif;\n"
                + "        cerif:has_destination ?instance_uri.\n"
                + "  ?classif cerif:has_roleExpression ?role. \n"
                + "  ?classif cerif:has_roleExpressionOpposite ?roleOpposite. \n"
                + "  ?classif cerif:has_term ?term. \n"
                + "  ?ent rdfs:label ?ent_label.\n"
                + "  ?ent a ?ent_type.\n"
                + "  optional {?ent cerif:has_name ?ent_name.}         \n"
                + "  optional {?ent cerif:has_title ?ent_title.} \n"
                + "  optional {?ent cerif:has_acronym ?ent_acronym.} \n"
                //                + "    optional {?ent cerif:has_description ?ent_description.} \n"
                + "  filter (?instance_uri = <" + entityUri + ">).\n"
                + "} order by ?ent_type";
//        RestClient client = new RestClient(endpoint, namespace, authorizationToken);
        VirtuosoRestClient client = new VirtuosoRestClient(endpoint, authorizationToken);
        Response resp = client.executeSparqlQuery(query, "application/json", 0);
        manageQueryResults(resp, fromClause);
    }

    private void manageQueryResults(Response resp, String fromClause) throws ParseException {
        List<String> graphs = Utils.getGraphsFromClause(fromClause);
        JSONParser parser = new JSONParser();
        JSONObject result = (JSONObject) parser.parse(resp.readEntity(String.class));
        JSONArray results = (JSONArray) ((JSONObject) result.get("results")).get("bindings");
        String relatedEntityType = "";
        JSONObject entitiesOfType;
        HashSet<JSONObject> relEntitiesSet = null;
        Set<String> potentialRelations = null;
        String currentType = (String) instanceInfo.get("instance_type");
        for (int i = 0; i < results.size(); i++) {
            JSONObject row = (JSONObject) results.get(i);
            String type = getJSONObjectValue(row, "ent_type");
            if (!relatedEntityType.equals(type)) {
                potentialRelations = DBService.retrieveRelationNames(graphs, currentType, type.replace(CERIFPrefix, ""));
                entitiesOfType = new JSONObject();
                relEntitiesSet = new HashSet<>();
                entitiesOfType.put("related_entity_type", getJSONObjectValue(row, "ent_type").replace(CERIFPrefix, ""));
                entitiesOfType.put("related_entities_of_type", relEntitiesSet);
                ((JSONArray) instanceInfo.get("related_entity_types")).add(entitiesOfType);
                relatedEntityType = type;
            }

            String role = getJSONObjectValue(row, "role");
            String roleOpposite = getJSONObjectValue(row, "roleOpposite");
            String term = getJSONObjectValue(row, "term");
            String relation;
            if (potentialRelations.contains(role) || type.equals("http://eurocris.org/ontology/cerif#PostalAddress")) {
                relation = role;
            } else {
                relation = roleOpposite;
            }
            if (relatedEntityType.contains("Medium")) {
                relation = role;
            }

            String entDstLabel = getJSONObjectValue(row, "ent_label");
            String entDstName = getJSONObjectValue(row, "ent_name");
            String entDstTitle = getJSONObjectValue(row, "ent_title");

            String entDstAcronym = getJSONObjectValue(row, "ent_acronym");
            String entUri = getJSONObjectValue(row, "ent");
            ////
            JSONObject relEntity = new JSONObject();
            relEntity.put("relation", relation);
            relEntity.put("term", term);
            relEntity.put("related_entity_uri", entUri);
            if (entDstName != null) {
                relEntity.put("related_entity_label", entDstName);
            }
            if (entDstTitle != null) {
                relEntity.put("related_entity_label", entDstTitle);
            }
            if (entDstName == null && entDstTitle == null) {
                relEntity.put("related_entity_label", entDstLabel);
            }
            relEntity.put("related_entity_acronym", entDstAcronym);
//            relEntities.add(relEntity);
            relEntitiesSet.add(relEntity);
        }
    }

    private String getJSONObjectValue(JSONObject obj, String key) {
        return obj.get(key) == null ? null : (String) ((JSONObject) obj.get(key)).get("value");
    }

    public JSONObject getInstanceInfo() {
        return instanceInfo;
    }

    public static void main(String[] args) throws Exception {

        String endpoint = "http://139.91.183.97:8080/EVREMetadataServices-1.0-SNAPSHOT";
        String namespace = "vre4eic";
        String token = "3d791107-6bb4-4e7b-9efd-38e1e33af05b";
        String entityUri = "http://139.91.183.70:8090/vre4eic/EKT.Person.1908";
        entityUri = "http://139.91.183.70:8090/vre4eic/EPOS.Organisation.www.forth.gr/";
//        entityUri = "http://139.91.183.70:8090/vre4eic/EKT.Project.7602";
        String fromClause = "from <http://ekt-data>";

        BeautifyQueryResultsService beauty = new BeautifyQueryResultsService(token, endpoint);
//        beauty.enrichEntityResults(entityUri, fromClause);
//        beauty.enrichDstEntityResults(entityUri, fromClause);
        beauty.enrichSrcEntityResults(entityUri, fromClause);
        System.out.println(beauty.getInstanceInfo());

    }

}

package forth.ics.isl.controller;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import javax.annotation.PostConstruct;
import org.apache.commons.lang.SerializationUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import com.fasterxml.jackson.databind.ObjectMapper;
import forth.ics.isl.data.model.EntityModel;
import forth.ics.isl.data.model.parser.QueryDataModel;
import forth.ics.isl.data.model.parser.Utils;
import forth.ics.isl.data.model.suggest.EntitiesSuggester;
import forth.ics.isl.service.DBService;
import forth.ics.isl.service.PropertiesService;
import forth.ics.isl.triplestore.VirtuosoRestClient;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.ClientErrorException;
import javax.ws.rs.core.Response;
import org.apache.commons.io.IOUtils;
import org.json.JSONException;

import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import static org.springframework.web.bind.annotation.RequestMethod.GET;
import static org.springframework.web.bind.annotation.RequestMethod.POST;

@Scope(scopeName = "session", proxyMode = ScopedProxyMode.TARGET_CLASS)
@Controller
public class EntityManagerController {


//    private RestClient restClient;
    private VirtuosoRestClient restClient;

    @Value("${service.url}")
    private String serviceUrl;
    @Value("${service.timeout}")
    private int timeout;
    @Value("${triplestore.namespace}")
    private String namespace;

    // Holding the results of the final query
    private JSONObject currFinalQueryResult;
    private JSONObject resultsForCSV;
    private JSONArray tableHead;
    @Autowired
    private DBService dbService;

    @PostConstruct
    public void init() throws IOException, SQLException {
        dbService = new DBService();
    }

    /**
     * Request get for retrieving a number of items that correspond to the
     * passed page and returning them in the form of EndPointDataPage object.
     *
     * @param requestParams A map that holds all the request parameters
     * @return An EndPointDataPage object that holds the items of the passed
     * page.
     */
//    @RequestMapping(value = "/get_all_entities", method = RequestMethod.GET, produces = {"application/json"})
//    public @ResponseBody
//    JSONArray loadEntitiesDataForPage(@RequestParam Map<String, String> requestParams) {//, Model model) {
//        System.out.println("Works");
//
//        /*
//    	int page = new Integer(requestParams.get("page")).intValue();
//    	int itemsPerPage = new Integer(requestParams.get("itemsPerPage")).intValue();
//
//    	// The EndPointForm for the page
//    	EndPointDataPage endPointDataPage = new EndPointDataPage();
//    	endPointDataPage.setPage(page);
//    	endPointDataPage.setTotalItems(currQueryResult.get("results").get("bindings").size());
//    	endPointDataPage.setResult(getDataOfPageForCurrentEndPointForm(page, itemsPerPage));
//    	    	
//		return endPointDataPage;
//         */
//        //JSONArray arr = retrieveAllEntities(h2ServiceUrl, h2ServiceUsername, h2ServicePassword);
//        JSONArray entities = DBService.retrieveAllEntities();
//        return entities;
//    }
    /**
     * This service retrieves the entities which are stored within the H2
     * database and for each entity it is examined if it has geospatial nature
     * in the considered namedgraphs (provided in the fromSearch parameter).
     */
    @RequestMapping(value = "/get_entities", method = RequestMethod.POST, produces = {
        "application/json;charset=utf-8"})
    public @ResponseBody
    JSONObject loadEntitiesDataForPagePOST(
            @RequestHeader(value = "Authorization") String authorizationToken, @RequestBody JSONObject requestParams)
            throws IOException, ParseException {
        System.out.println("\n-> loadEntitiesDataForPagePOST() in EntityManagerController.java \n");
        System.out.println(" all requestParams: " + requestParams.keySet());
        JSONArray resultEntitiesJSON = new JSONArray();
        JSONObject finalResult = new JSONObject();
        String fromSearch = (String) requestParams.get("fromSearch");
        if (fromSearch == null || fromSearch.equals("")) {
            finalResult.put("remote_status", 200);
            finalResult.put("entities", resultEntitiesJSON);
            return finalResult;
        }
        List<String> graphs = Utils.getGraphsFromClause(fromSearch);

        JSONArray initEntitySpecsJSON = retrieveEntitiesConf();
        for (int i = 0; i < initEntitySpecsJSON.size(); i++) {
            JSONObject entityJSON = (JSONObject) initEntitySpecsJSON.get(i);
            JSONParser parser = new JSONParser();

            String instances_query = (String) entityJSON.get("instances_query");
            if (!instances_query.equals("")) {
                instances_query = instances_query.replace("[[FROM_GRAPHS]]", " " + fromSearch + " ");
                restClient = new VirtuosoRestClient(serviceUrl, authorizationToken);
                Response response = restClient.executeSparqlQuery(instances_query, 0, "application/json",
                        authorizationToken);
                if (response.getStatus() != 200) {
                    finalResult.put("remote_status", response.getStatus());
                    return finalResult;
                }
                JSONObject result = (JSONObject) parser.parse(response.readEntity(String.class));
                JSONObject callret = (JSONObject) ((JSONArray) ((JSONObject) result.get("results")).get("bindings"))
                        .get(0);
                int size = Integer.parseInt((String) ((JSONObject) callret.get("callret-0")).get("value"));

                // System.out.println(">>> bindings = " + bindings);
                if (size == 0) {
                    continue;
                }
                entityJSON.put("size", size);
            } else {
                entityJSON.put("size", 0);

            }
            resultEntitiesJSON.add(entityJSON);
        }

        finalResult.put("remote_status", 200);
        finalResult.put("entities", resultEntitiesJSON);
        return finalResult;
    }

    /**
     * This service retrieves all the namedgraphs which are stored within the H2
     * database.
     */
    @RequestMapping(value = "/get_all_namedgraphs", method = RequestMethod.GET, produces = {
        "application/json;charset=utf-8"})
    public @ResponseBody
    JSONArray loadNGraphsDataForPage(@RequestParam Map<String, String> requestParams) {// , Model
        // model) {
        System.out.println("\n-> loadNGraphsDataForPage() in EntityManagerController.java \n");
        System.out.println("  All params: " + requestParams.keySet());
        JSONArray arr = DBService.retrieveAllNamedgraphs();
        return arr;
    }

    /**
     * This service retrieves the default namedgraphs from config.properties. If
     * empty or not declared returns all namedgraphs from H2
     *
     */
    @RequestMapping(value = "/get_default_namedgraphs", method = RequestMethod.GET, produces = {
        "application/json;charset=utf-8"})
    public @ResponseBody
    JSONArray getDefaultNamedgraphs(@RequestParam Map<String, String> requestParams) {// , Model
        JSONArray responseArray = DBService.retrieveAllAndDefaultNamedgraphs();
        System.out.println(responseArray);

        return responseArray;
    }

    /**
     * Web Service that accepts two parameters and returns a dynamically
     * constructed query for the related entity search
     *
     * @param authorizationToken A string holding the authorization token
     * @param requestParams A Json object holding the search-text and the from
     * section of the query
     * @return An EndPointDataPage object that holds the items of the passed
     * page.
     */
    @RequestMapping(value = "/related_entity_query", method = RequestMethod.POST, produces = {
        "application/json;charset=utf-8"})
    public @ResponseBody
    JSONObject searchEntityQuery(@RequestHeader(value = "Authorization") String authorizationToken,
            @RequestBody JSONObject requestParams) throws IOException {
        System.out.println("\n-> searchEntityQuery() in EntityManagerController.java \n");
        System.out.println("running executequery_json...");
        System.out.println("relatedEntity:" + requestParams.get("entity"));
        System.out.println("searchText:" + requestParams.get("searchText"));
        System.out.println("fromSearch:" + requestParams.get("fromSearch"));
        System.out.println("requestParams:: \n" + requestParams);
        // without authorization at the moment
//        System.out.println("authorizationToken: " + authorizationToken);
//        String entity = (String) requestParams.get("entity");
        String fromClause = (String) requestParams.get("fromSearch");
        String searchClause = (String) requestParams.get("searchText");
        JSONArray searchChips = new JSONArray();
        for (LinkedHashMap chip : (ArrayList<LinkedHashMap>) requestParams.get("relatedChips")) {
            searchChips.add(new JSONObject(chip));
        }
        if (requestParams.get("query") != null) {
            searchClause = Utils.getChipsFilter(searchChips).toString();
            System.out.println("searchClause " + searchClause);
            System.out.println("expr  " + getFilterExpression(searchClause));

            // String sourceFilterExpression =
            // QueryController.getFilterExpression(searchClause);
            String query = (String) requestParams.get("query");
            System.out.println("Query template: " + query);

            query = query.replace("[[FROM_GRAPHS]]", " " + requestParams.get("fromSearch") + " ");
            query = QueryController.getLiteralFilter(query, getFilterExpression(searchClause));
            System.out.println("Here the query is: " + query);

            // query = query.replace("@#$%FROM%$#@", fromClause).replace("@#$%TERM%$#@",
            // searchClause);
            JSONObject responseJsonObject = new JSONObject();
            responseJsonObject.put("query", query);

            return responseJsonObject;
        } else if (requestParams.get("geo_query") != null) {

            String geoQuery = (String) requestParams.get("geo_query");
            String northClause = "" + requestParams.get("north");
            String southClause = "" + requestParams.get("south");
            String eastClause = "" + requestParams.get("east");
            String westClause = "" + requestParams.get("west");

            geoQuery = geoQuery.replace("[[FROM_GRAPHS]]", " " + requestParams.get("fromSearch") + " ");

            System.out.println("geoQuery : " + geoQuery);
            System.out.println("northClause : " + northClause);
            System.out.println("southClause : " + southClause);
            System.out.println("eastClause : " + eastClause);
            System.out.println("westClause : " + westClause);
            searchClause = Utils.getChipsFilter(searchChips).toString();
            geoQuery = QueryController.getLiteralFilter(geoQuery, getFilterExpression(searchClause));
            System.out.println("searchClause " + searchClause);
            System.out.println("expr  " + getFilterExpression(searchClause));
            System.out.println("geoQuery: " + geoQuery);
            geoQuery = QueryController.getGeoFilter(geoQuery, northClause, southClause, eastClause, westClause);

            JSONObject responseJsonObject = new JSONObject();
            responseJsonObject.put("query", geoQuery);
            System.out.println("geoQuery: " + geoQuery);
            return responseJsonObject;

        } else {
            JSONObject responseJsonObject = new JSONObject();
            responseJsonObject.put("query", null);
            return responseJsonObject;
        }
    }

    /**
     * This service populates the lists containing relations and related
     * entities considering the selected target entity and the selected named
     * graphs.
     *
     * @param authorizationToken A string holding the authorization token
     * @param requestParams A Json object holding the selected entity URI and
     * the from section of the query
     * @return A JSONArray in which each object contains information about the
     * relation and the corresponding related entity w.r.t. the target entity.
     * @throws IOException
     */
    @RequestMapping(value = "/get_relations_related_entities", method = RequestMethod.POST, produces = {
        "application/json;charset=utf-8"})
    public @ResponseBody
    JSONArray populateRelationsEntities(
            @RequestHeader(value = "Authorization") String authorizationToken, @RequestBody JSONObject requestParams)
            throws IOException, ParseException {
//        System.out.println("targetEntity:" + requestParams.get("targetEntity"));
//        System.out.println("fromSearch:" + requestParams.get("fromSearch"));
//        String fromClause = (String) requestParams.get("fromSearch");
//        String targetEntity = (String) requestParams.get("name");
        // without authorization at the moment
//        System.out.println("authorizationToken: " + authorizationToken);

        System.out.println("\n-> populateRelationsEntities() in EntityManagerController.java \n");
        System.out.println(" allParams:" + requestParams.keySet());
        EntitiesSuggester suggester = new EntitiesSuggester((String) requestParams.get("model"), namespace, serviceUrl,
                authorizationToken);
        QueryDataModel model = suggester.getModel();
        String fromClause = model.getSelectedGraphsClause();
        String targetEntity = model.getTargetModel().getName();
        ArrayList<LinkedHashMap> entities = (ArrayList) requestParams.get("entities");
        List<String> graphs = new ArrayList<>();
        Pattern regex = Pattern.compile("(?<=<)[^>]+(?=>)");
        Matcher regexMatcher = regex.matcher(fromClause);
        while (regexMatcher.find()) {
            graphs.add(regexMatcher.group());
        }
        return DBService.retrieveRelationsEntities(graphs, targetEntity, entities);
    }

    /**
     *
     * @param authorizationToken
     * @param requestParams
     * @return
     * @throws IOException
     */
    @RequestMapping(value = "/get_relations", method = RequestMethod.POST, produces = {
        "application/json;charset=utf-8"})
    public @ResponseBody
    JSONArray populateRelations(@RequestHeader(value = "Authorization") String authorizationToken,
            @RequestBody JSONObject requestParams) throws IOException {
        System.out.println("\n-> populateRelations() in EntityManagerController.java \n");
//        System.out.println("targetEntity:" + requestParams.get("targetEntity"));
//        System.out.println("relatedEntity:" + requestParams.get("relatedEntity"));
//        System.out.println("fromSearch:" + requestParams.get("fromSearch"));
        // without authorization at the moment
//        System.out.println("authorizationToken: " + authorizationToken);
//        String fromClause = (String) requestParams.get("fromSearch");
//        String targetEntity = (String) requestParams.get("targetEntity");
//        String relatedEntity = (String) requestParams.get("relatedEntity");
        EntitiesSuggester suggester = new EntitiesSuggester((String) requestParams.get("model"), namespace, serviceUrl,
                namespace);
        QueryDataModel model = suggester.getModel();
        String fromClause = model.getSelectedGraphsClause();
        String targetEntity = model.getTargetModel().getName();
        String relatedEntity = suggester.getRowModel().getRelatedName();
        List<String> graphs = Utils.getGraphsFromClause(fromClause);
        return DBService.retrieveRelations(graphs, targetEntity, relatedEntity);
    }

    /**
     * Method used for calling the respective service in order to run a query,
     * the results of which are stored in a variable
     *
     * @param authorizationToken A valid token ensuring security
     * @param requestParams A JSON Object holding the parameters (query and
     * format)
     *
     * @return
     * @throws ParseException
     */
    @RequestMapping(value = "/execute_final_query", method = RequestMethod.POST, produces = {
        "application/json; charset=utf-8"})
    public @ResponseBody
    ResponseEntity<?> executeFinalQuery(
            @RequestHeader(value = "Authorization") String authorizationToken, @RequestBody JSONObject requestParams)
            throws IOException, ParseException {
        System.out.println("\n-> executeFinalQuery() in EntityManagerController.java \n");
        System.out.println("all request params: " + requestParams);

        ResponseEntity responseEntity = new ResponseEntity(HttpStatus.NOT_ACCEPTABLE);
//        restClient = new RestClient(serviceUrl, namespace, authorizationToken);
        System.out.println("   ServiceURL: " + serviceUrl);
        System.out.println("   AuthorizationToken: " + authorizationToken);
        restClient = new VirtuosoRestClient(serviceUrl, authorizationToken);
        String query = (String) requestParams.get("query");

        System.out.println("Query:::  " + query);
        JSONObject responseJsonObject = new JSONObject();
        responseJsonObject.put("query", query);
        try {
//            Response serviceResponce = restClient.executeSparqlQuery(requestParams.get("query").toString(), namespace, timeout, "application/json", authorizationToken);
            Response serviceResponce = restClient.executeSparqlQuery(requestParams.get("query").toString(), 0,
                    "application/json", authorizationToken);
            // System.out.println("serviceResponce.getStatus(): " +
            // serviceResponce.getStatusInfo());

            // Setting Response status to POJO
            responseJsonObject.put("statusCode", serviceResponce.getStatus());
            responseJsonObject.put("statusInfo", serviceResponce.getStatusInfo().toString());
            System.out.println(" ===> STATUS = " + serviceResponce.getStatus());
            // In case of OK status handle the response
            if (serviceResponce.getStatus() == 200) {

                ObjectMapper mapper = new ObjectMapper();
                // Holding JSON result in jsonNode globally (The whole results, which can be a
                // lot)
                JSONParser parser = new JSONParser();

                ArrayList<LinkedHashMap> relatedModels = (ArrayList) requestParams.get("relatedModels");

                LinkedHashMap targetModel = (LinkedHashMap) requestParams.get("targetModel");
                JSONObject qFinalRsults = (JSONObject) serviceResponce.readEntity(JSONObject.class);
                resultsForCSV = qFinalRsults;

                ArrayList<String> resultVars = ((ArrayList) ((LinkedHashMap) qFinalRsults.get("head")).get("vars"));
                System.out.println("  resultsVars = " + resultVars.toString());
                ArrayList<String> resultVarsNoChange = (ArrayList<String>) SerializationUtils
                        .clone(((ArrayList) ((LinkedHashMap) qFinalRsults.get("head")).get("vars")));

                LinkedHashMap sel = (LinkedHashMap) ((LinkedHashMap) relatedModels.get(0)).get("selectedRelation");
                String groupingUriVar = "";

                HashMap<String, String> varToUri = new HashMap();
                LinkedHashMap variablesToEntitiesName = new <String, String>LinkedHashMap();

                if (sel != null) {
                    for (int j = 0; j < relatedModels.size(); j++) {
                        LinkedHashMap relatedModel = relatedModels.get(j);
                        LinkedHashMap selectedReleation = (LinkedHashMap) relatedModel.get("selectedRelation");
                        if (selectedReleation != null) {
                            String source_shown_variables = ((String) selectedReleation.get("source_shown_variables"))
                                    .trim();
                            String destination_shown_variables = ((String) selectedReleation
                                    .get("destination_shown_variables")).trim();
                            String destination_uri_variable = ((String) selectedReleation
                                    .get("destination_uri_variable")).trim();
                            String source_uri_variable = ((String) selectedReleation.get("source_uri_variable")).trim();
                            String destination_name = (String) ((LinkedHashMap) relatedModel
                                    .get("selectedRelatedEntity")).get("name");
                            ArrayList targetEntities = (ArrayList) targetModel.get("targetEntities");
                            String source_name = "";
                            int source_id = (int) selectedReleation.get("source_entity");
                            for (int k = 0; k < targetEntities.size(); k++) {
                                LinkedHashMap targetEntity = (LinkedHashMap) targetEntities.get(k);
                                int id = (int) targetEntity.get("id");
                                if (id == source_id) {
                                    source_name = (String) targetEntity.get("name");
                                    break;
                                }
                            }
                            if (source_shown_variables != "") {
                                String[] shown_vars_shown = source_shown_variables.split(" ");

                                int count = 0;
                                for (String source_var : shown_vars_shown) {

                                    for (int v = 0; v < resultVars.size(); v++) {
                                        String var = resultVars.get(v);

                                        String source_tmp = source_var.substring(0, source_var.indexOf("["));
                                        String source_end = var.substring(var.lastIndexOf("_"), var.length());
                                        String var_tmp = "?" + var.substring(0, var.lastIndexOf("_"));
                                        String uri_tmp = source_uri_variable.substring(0,
                                                source_uri_variable.indexOf("["));
                                        if (source_tmp.equals(var_tmp)) {
                                            count++;
                                            variablesToEntitiesName.put(var, source_name);
                                            varToUri.put(var, uri_tmp + source_end);
                                            groupingUriVar = (uri_tmp + source_end).replace("?", "");
                                            resultVars.remove(v);
                                            break;
                                        }
                                    }
                                }

                            }

                            if (destination_shown_variables != "") {
                                destination_shown_variables = destination_shown_variables.replace(" as ?", "###as###?").replace(" AS ", "###as###?");
                                String[] shown_vars_dest = destination_shown_variables.split(" ");
                                int count = 0;
                                for (String dest_var : shown_vars_dest) {
                                    //System.out.println("   dest_var = " + dest_var);

                                    for (int v = 0; v < resultVars.size(); v++) {
                                        String var = resultVars.get(v);

                                        String dest_temp = "";
                                        if (dest_var.contains("###as###")) {
                                            dest_temp = dest_var.substring(dest_var.indexOf("###?") + 3, dest_var.indexOf("[", dest_var.indexOf("###?")));
                                        } else {
                                            dest_temp = dest_var.substring(0, dest_var.indexOf("["));
                                        }

                                        String dest_end = var.substring(var.lastIndexOf("_"), var.length());

                                        String var_tmp = "?" + var.substring(0, var.lastIndexOf("_"));
                                        String uri_tmp = "";
                                        if (!destination_uri_variable.equals("")) {
                                            uri_tmp = destination_uri_variable.substring(0, destination_uri_variable.indexOf("["));
                                        }
                                        if (dest_temp.equals(var_tmp)) {
                                            count++;
                                            variablesToEntitiesName.put(var, destination_name);
                                            varToUri.put(var, uri_tmp + dest_end);
                                            resultVars.remove(v);
                                            break;
                                        }
                                    }

                                }

                            }

                        } else {
                            break;
                        }

                    }

                } else {
                    groupingUriVar = ((String) ((LinkedHashMap) targetModel.get("selectedTargetEntity"))
                            .get("sourceUriVariable")).replace("?", "");
                    String entity_name = (String) ((LinkedHashMap) targetModel.get("selectedTargetEntity")).get("name");
                    for (int i = 0; i < resultVars.size(); i++) {
                        if (!resultVars.get(i).equals(groupingUriVar)) {
                            varToUri.put((String) resultVars.get(i), "?" + groupingUriVar);

                            variablesToEntitiesName.put((String) resultVars.get(i), entity_name);

                        }
                    }

                }
                ArrayList bindings = ((ArrayList) ((LinkedHashMap) qFinalRsults.get("results")).get("bindings"));
                currFinalQueryResult = new JSONObject(qFinalRsults);
                LinkedHashMap bindingObject = new LinkedHashMap();
                ArrayList bingingArr = new ArrayList();

                LinkedHashMap uriToValues = new <String, LinkedHashMap<String, ArrayList>>LinkedHashMap();

                for (int b = 0; b < bindings.size(); b++) {
                    LinkedHashMap binding = (LinkedHashMap) bindings.get(b);
                    String sourceUriValue = "";
                    for (int v = 0; v < resultVarsNoChange.size(); v++) {
                        String var_name = resultVarsNoChange.get(v);
                        LinkedHashMap tmp_res = (LinkedHashMap) binding.get(var_name);
                        String value = "";
                        if (tmp_res != null) {
                            value = (String) tmp_res.get("value");
                        }
                        if (groupingUriVar.equals(var_name)) {
                            sourceUriValue = value;
                        }
                        if (varToUri.containsKey(var_name)) {
                            String var_uri = varToUri.get(var_name).replace("?", "");
                            LinkedHashMap tmp = (LinkedHashMap) binding.get(var_uri);
                            String uri = "";
                            if (tmp != null) {
                                uri = (String) tmp.get("value");
                            }

                            if (!uriToValues.containsKey(sourceUriValue)) {
                                LinkedHashMap varToValues = new <String, LinkedHashMap>LinkedHashMap();
                                // ArrayList values = new <String>ArrayList();
                                LinkedHashMap internalUriToValues = new <String, ArrayList>LinkedHashMap();
                                ArrayList values = new <String>ArrayList();
                                values.add(value);
                                internalUriToValues.put(uri, values);
                                // values.add(uri + "___" + value);
                                varToValues.put(var_name, internalUriToValues);
                                uriToValues.put(sourceUriValue, varToValues);
                            } else {

                                LinkedHashMap varToValues = (LinkedHashMap) uriToValues.get(sourceUriValue);
                                if (!varToValues.containsKey(var_name)) {
                                    // ArrayList values = new <String>ArrayList();
                                    LinkedHashMap internalUriToValues = new <String, ArrayList>LinkedHashMap();
                                    ArrayList values = new <String>ArrayList();
                                    values.add(value);
                                    internalUriToValues.put(uri, values);
                                    // values.add(uri + "___" + value);
                                    varToValues.put(var_name, internalUriToValues);
                                    uriToValues.put(sourceUriValue, varToValues);
                                } else {
                                    LinkedHashMap internalUriToValues = (LinkedHashMap) varToValues.get(var_name);
                                    Set keys = internalUriToValues.keySet();
                                    if (!keys.contains(uri)) {
                                        ArrayList values = new <String>ArrayList();
                                        values.add(value);
                                        internalUriToValues.put(uri, values);
                                    } else {
                                        ArrayList values = (ArrayList) internalUriToValues.get(uri);
                                        if (!values.contains(value)) {
                                            values.add(value);
                                            internalUriToValues.put(uri, values);
                                        }
                                    }
                                    varToValues.put(var_name, internalUriToValues);
                                    uriToValues.put(sourceUriValue, varToValues);

                                }
                            }

                        }

                    }

                }
                Iterator keys = uriToValues.keySet().iterator();
                while (keys.hasNext()) {
                    LinkedHashMap varToValues = (LinkedHashMap<String, ArrayList>) uriToValues.get(keys.next());
                    LinkedHashMap currVarObjt = new LinkedHashMap();
                    Iterator variables = varToValues.keySet().iterator();
                    while (variables.hasNext()) {
                        String variable = (String) variables.next();
                        String entityName = (String) variablesToEntitiesName.get(variable);
                        LinkedHashMap internalUriToValues = (LinkedHashMap<String, ArrayList>) varToValues
                                .get(variable);
                        Iterator it = internalUriToValues.keySet().iterator();
                        ArrayList val = new ArrayList();

                        while (it.hasNext()) {
                            String uri = (String) it.next();
                            ArrayList values = (ArrayList) internalUriToValues.get(uri);
                            String value = "";
                            LinkedHashMap currVar = new LinkedHashMap();
                            for (int i = 0; i < values.size(); i++) {

                                if (!values.get(i).equals("")) {
                                    if (i > 0 && i < values.size()) {
                                        value += "   |   ";
                                    }
                                    value += values.get(i);
                                }
                            }
                            if (!value.equals("")) {
                                currVar.put("uri", uri);
                                currVar.put("value", value);
                                currVar.put("entityName", entityName);
                            } else {
                                currVar.put("uri", "");
                                currVar.put("value", "");
                                currVar.put("entityName", "");
                            }
                            val.add(currVar);
                        }

                        currVarObjt.put(variable, val);

                    }
                    bingingArr.add(currVarObjt);

                }

                bindingObject.put("bindings", bingingArr);
                LinkedHashMap results = new LinkedHashMap();

                LinkedHashMap firstBinding = (LinkedHashMap) bingingArr.get(0);
                Iterator names = firstBinding.keySet().iterator();
                LinkedHashMap namesTocount = new <String, Integer>LinkedHashMap();

                while (names.hasNext()) {
                    String name = (String) names.next();
                    if (variablesToEntitiesName.containsKey(name)) {
                        String entity_name = (String) variablesToEntitiesName.get(name);
                        if (!namesTocount.containsKey(entity_name)) {
                            namesTocount.put(entity_name, 1);
                        } else {
                            int count = (int) namesTocount.get(entity_name);
                            count++;
                            namesTocount.put(entity_name, count);
                        }
                    }
                }
                names = namesTocount.keySet().iterator();
                tableHead = new JSONArray();
                while (names.hasNext()) {
                    String name = (String) names.next();
                    int count = (int) namesTocount.get(name);
                    JSONObject tmp = new JSONObject();
                    tmp.put("name", name);
                    tmp.put("count", count);
                    tableHead.add(tmp);

                }

                results.put("results", bindingObject);
                currFinalQueryResult = new JSONObject(results);
                // Setting total items for the response
                int totalItems = bingingArr.size();
                responseJsonObject.put("totalItems", totalItems);

                // Holding the first page results in a separate JsonNode
                JSONObject firstPageQueryResult = getDataOfPageForCurrentFinalQuery(1,
                        (int) requestParams.get("itemsPerPage"), totalItems);

                // Setting results for the response (for now we set them all and
                // later we will replace them with those at the first page)
                responseJsonObject.put("results", firstPageQueryResult);
                responseJsonObject.put("head", tableHead);
                System.out.println(responseJsonObject);
                return ResponseEntity.status(HttpStatus.OK).body(responseJsonObject);

            } else if (serviceResponce.getStatus() == 400) {
                return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
            } else if (serviceResponce.getStatus() == 401) {
                return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
            } else if (serviceResponce.getStatus() == 408) {
                return new ResponseEntity<>(HttpStatus.REQUEST_TIMEOUT);
            } else {
                return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
            }
        } catch (IOException e) {
            System.out.println("CATCH");
            e.printStackTrace();
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }

        // return responseJsonObject;
    }

    @RequestMapping(value = "/enrich_profile_graph", method = RequestMethod.POST, produces = {
        "application/json; charset=utf-8"})
    public @ResponseBody
    ResponseEntity<?> enrichProfileGraph(
            @RequestHeader(value = "Authorization") String authorizationToken, @RequestBody JSONObject requestParams)
            throws IOException, ParseException {
        System.out.println("\n-> enrichProfileGraph() in EntityManagerController.java \n");
        ResponseEntity responseEntity = new ResponseEntity(HttpStatus.NOT_ACCEPTABLE);
//        restClient = new RestClient(serviceUrl, namespace, authorizationToken);
        restClient = new VirtuosoRestClient(serviceUrl, authorizationToken);
        String query = (String) requestParams.get("query");
        System.out.println("query:" + query);
        JSONObject responseJsonObject = new JSONObject();
        responseJsonObject.put("query", query);

        // custom code which creates a graph to store the service data w.r.t. the user
        // profile
        // and the dynamic generated query
        Response response;
        if (query.indexOf("http://eurocris.org/ontology/cerif#WebService") > 0) {
            response = createUserGraph(requestParams, query);
            return ResponseEntity.status(response.getStatus()).body(response.readEntity(String.class));
        } else {
            return ResponseEntity.status(HttpStatus.OK).body("");
        }
        ////////////////////
    }

    public ArrayList<HashMap<String, String>> getListOfBindings(JSONArray bindingsArr) {
        ArrayList<HashMap<String, String>> list = new ArrayList<>();
        for (int ii = 0; ii < bindingsArr.size(); ii++) {
            JSONObject bindingObj = (JSONObject) bindingsArr.get(ii);
            HashMap<String, String> temp = new HashMap<>();
            for (Object variable : bindingObj.keySet()) {
                JSONObject binding = (JSONObject) bindingObj.get((String) variable);
                String value = binding.get("value").toString();
                temp.put((String) variable, value);
            }
            list.add(temp);
        }
        return list;
    }

    @RequestMapping(value = "/export_to_csv", method = RequestMethod.POST)
    public void getFile(HttpServletRequest request, HttpServletResponse response) {

        System.out.println("Export to csv");
        response.setContentType("text/csv");
        response.setHeader("Content-Disposition", "attachment; filename=export.csv;");
        try {
            String csv = "";
            try {

                LinkedHashMap map = (LinkedHashMap) this.resultsForCSV.get("results");
                ArrayList bindings = (ArrayList) map.get("bindings");

                LinkedHashMap tmp = (LinkedHashMap) bindings.get(0);

                Iterator keys = tmp.keySet().iterator();
                while (keys.hasNext()) {
                    String key = (String) keys.next();
                    if (key.contains("_")) {
                        key = key.split("_")[0];
                    }
                    csv += key + ",";

                }
                csv = csv.replaceAll(",$", "");
                csv += "\n";
                System.out.println("1");
                for (int i = 0; i < bindings.size(); i++) {
                    // System.out.println("binding: " + bindings.get(i));
                    tmp = (LinkedHashMap) bindings.get(i);
                    keys = tmp.keySet().iterator();
                    while (keys.hasNext()) {
                        String key = (String) keys.next();
                        //	System.out.println("key: " + key);

                        LinkedHashMap innerObject = (LinkedHashMap) tmp.get(key);

                        // System.out.println("innerObject: " + innerObject);
                        csv += innerObject.get("value") + ",";

                    }
                    csv = csv.replaceAll(",$", "");
                    csv += "\n";
                    // System.out.println("-----------------------------------");
                }
                // System.out.println(csv);

            } catch (JSONException e) {
                e.printStackTrace();
            }
            System.out.println("End");

            InputStream targetStream = new ByteArrayInputStream(csv.getBytes());
            IOUtils.copy(targetStream, response.getOutputStream());
            response.flushBuffer();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    private Response createUserGraph(JSONObject requestParams, String query) throws ClientErrorException {
        System.out.println("\n-> createUserGraph() in EntityManagerController.java \n");
        String userId = (String) ((LinkedHashMap) requestParams.get("userProfile")).get("userId");
        String graph = "http://profile/" + userId;
        int start = query.indexOf("from"); // consider selected namedgraphs
//        int start = query.indexOf("where");
        int end = query.lastIndexOf("}");
        StringBuilder updateQuery = new StringBuilder();
        updateQuery.append("insert into <" + graph + "> {\n")
                .append("?webserv a <http://eurocris.org/ontology/cerif#WebService>.\n")
                .append("?webserv <http://eurocris.org/ontology/cerif#has_URI>         ?webservUri .\n")
                .append("?webserv <http://eurocris.org/ontology/cerif#has_description> ?webservDescr.\n")
                .append("?webserv <http://eurocris.org/ontology/cerif#has_keywords>    ?webservKeywords .\n")
                .append("?webserv <http://eurocris.org/ontology/cerif#has_name>        ?webservName .\n")
                .append("?webserv <http://eurocris.org/ontology/cerif#is_source_of> ?webservmediumLE .\n")
                .append("?webservmediumLE  <http://eurocris.org/ontology/cerif#has_destination> ?webservMedium.\n")
                .append("?webservMedium a <http://eurocris.org/ontology/cerif#Medium>. \n")
                .append("?webserv <http://searchable_text> ?webservLabel. \n").append("} where {\n")
                .append("select distinct ?webserv ?webservUri ?webservDescr ?webservKeywords ?webservName ?webservmediumLE ?webservMedium ?webservLabel ")
                .append(query.substring(start, end + 1) + "\n }");
        restClient.executeUpdatePOSTJSON("clear graph <" + graph + ">");
        return restClient.executeUpdatePOSTJSON(updateQuery.toString());
    }

    /**
     * Method used for calling the respective service in order to run a query,
     * the results of which are stored in a variable
     *
     * @param authorizationToken A valid token ensuring security
     * @param requestParams A JSON Object holding the parameters (query and
     * format)
     *
     * @return
     * @throws ParseException
     */
    @RequestMapping(value = "/get_final_query_results_per_page", method = RequestMethod.POST, produces = {
        "application/json;charset=utf-8"})
    public @ResponseBody
    JSONObject getFinalQueryResultsPerPage(
            @RequestHeader(value = "Authorization") String authorizationToken, @RequestBody JSONObject requestParams)
            throws IOException, ParseException {
        System.out.println("\n-> getFinalQueryResultsPerPage() in EntityManagerController.java \n");
        JSONObject responseJsonObject = new JSONObject();

        // Total Items
        int totalItems = ((ArrayList) ((LinkedHashMap) currFinalQueryResult.get("results")).get("bindings")).size();

        // Setting total items for the response
        responseJsonObject.put("totalItems", totalItems);
        responseJsonObject.put("head", tableHead);

        JSONObject pageQueryResult = getDataOfPageForCurrentFinalQuery((int) requestParams.get("page"),
                (int) requestParams.get("itemsPerPage"), totalItems);
        responseJsonObject.put("results", pageQueryResult);

        return responseJsonObject;
    }

    /**
     * Constructs an ObjectNode for one page only, based on the passed page and
     * the whole data
     *
     * @param page The page number
     * @param itemsPerPage The number of items per page
     * @return the constructed ObjectNode
     */
    private JSONObject getDataOfPageForCurrentFinalQuery(int page, int itemsPerPage, int totalItems) {
        System.out.println("\n-> getDataOfPageForCurrentFinalQuery() in EntityManagerController.java \n");
        // {"head":{"vars":["s","p","o"]},"results":{"bindings":[{"s":{... "p":{...
        // bindings
        JSONArray bindingsJsonArray = new JSONArray();
        LinkedHashMap resultsObj = (LinkedHashMap) currFinalQueryResult.get("results");
        ArrayList bindingsArr = (ArrayList) resultsObj.get("bindings");
        List<String> bindingsList = new ArrayList<String>();

        if (((page - 1) * itemsPerPage) + itemsPerPage + 1 > totalItems) {
            bindingsList = bindingsArr.subList(((page - 1) * itemsPerPage), totalItems);
        } else {
            bindingsList = bindingsArr.subList(((page - 1) * itemsPerPage), ((page - 1) * itemsPerPage) + itemsPerPage);
        }

        for (int i = 0; i < bindingsList.size(); i++) {
            bindingsJsonArray.add(bindingsList.get(i));
        }

        JSONObject pageResultsObj = new JSONObject();
        pageResultsObj.put("bindings", bindingsJsonArray);
        return pageResultsObj;

    }

    public JSONArray retrieveEntitiesConf() throws IOException {
        System.out.println("-> retrieveEntitiesConf() of EntityManagerController.java");

        JSONArray results = new JSONArray();
        HashMap<Integer, EntityModel> configEntities = PropertiesService.getEntities();

        for (int entid : configEntities.keySet()) {
            EntityModel entmodel = configEntities.get(entid);

            JSONObject entity = new JSONObject();
            entity.put("id", entid);
            entity.put("name", entmodel.getName());
            entity.put("instances_query", entmodel.getInstancesQuery());
            entity.put("search_query", entmodel.getSearchQuery());
            entity.put("geo_query", entmodel.getGeoQuery());
            entity.put("entityType", entmodel.getEntityType());
            entity.put("isVisibleInTarget", entmodel.isVisibleInTarget());
            entity.put("sourceUriVariable", entmodel.getSourceUriVariable());
            results.add(entity);
        }

        return results;
    }

    public static void main(String[] args) throws SQLException, IOException {
        String fromClause = "from <http://ekt-data> from <http://rcuk-data>";
        String uri = "http://eurocris.org/ontology/cerif#Person";
        String targetName = "Person";
        String relatedName = "Project";

        List<String> graphs = new ArrayList<String>();
        Pattern regex = Pattern.compile("(?<=<)[^>]+(?=>)");
        Matcher regexMatcher = regex.matcher(fromClause);
        while (regexMatcher.find()) {
            graphs.add(regexMatcher.group());
        }
//        DBService.retrieveRelationsEntities(graphs, targetName);

//        DBService.retrieveRelations(graphs, targetName, relatedName);
    }

    private String getFilterExpression(String keyword) {
        String expr = "";
        if (!keyword.equals("")) {
            String[] parts = keyword.split(" or ");
            for (int i = 0; i < parts.length; i++) {
                int index = parts[i].lastIndexOf("'");
                String tmp = parts[i].substring(0, index);
                tmp += "*'";
                expr += tmp;
                if (i < parts.length - 1) {
                    expr += " or ";
                }

            }
        }
        return "\"" + expr + "\"";

    }

}

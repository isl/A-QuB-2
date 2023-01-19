package forth.ics.isl.controller;

import java.io.IOException;
import java.util.Iterator;
import javax.annotation.PostConstruct;
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
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import forth.ics.isl.runnable.H2Manager;
import forth.ics.isl.service.DBService;
import forth.ics.isl.triplestore.VirtuosoRestClient;
import java.sql.Connection;
import java.util.List;
import java.util.Set;
import javax.ws.rs.core.Response;
import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;
import org.springframework.web.bind.annotation.ResponseBody;

@Scope(scopeName = "session", proxyMode = ScopedProxyMode.TARGET_CLASS)
@Controller
public class ImportController {

    @Value("${service.url}")
    private String serviceUrl;
    @Value("${triplestore.namespace}")
    private String namespace;
    private JsonNode currQueryResult;
    private String linkingUpdateQuery = null;
    private String provUpdateQuery = null;

    @PostConstruct
    public void init() throws IOException {
        currQueryResult = new ObjectNode(JsonNodeFactory.instance);
//        restClient = new RestClient(serviceUrl, namespace);
//        restClient = new VirtuosoRestClient(serviceUrl);
    }

    /*
     * Saving meta-data information for the file to be uploaded in the database
     */
    @RequestMapping(value = "/createGraphMetadata", method = RequestMethod.POST, produces = {"application/json;charset=utf-8"})
    public @ResponseBody
    JSONObject importData(@RequestHeader(value = "Authorization") String authorizationToken, @RequestBody JSONObject requestParams) throws IOException, ParseException {

        System.out.println("Saving metadata into the database...");

        String namedGraphLabelParam = null;
        String namedGraphIdParam = null;
        String selectedCategoryLabel = null;
        String selectedCategoryId = null;

        // Retrieving the label of the named graph
        if (requestParams.get("namedGraphLabelParam") != null) {
            namedGraphLabelParam = requestParams.get("namedGraphLabelParam").toString();
        }
        // Retrieving the id of the named graph
        if (requestParams.get("namedGraphIdParam") != null) {
            namedGraphIdParam = requestParams.get("namedGraphIdParam").toString();
        }
        // Retrieving the label of the category of the named graph
        if (requestParams.get("selectedCategoryLabel") != null) {
            selectedCategoryLabel = requestParams.get("selectedCategoryLabel").toString();
        }
        // Retrieving the id of the category of the named graph
        if (requestParams.get("selectedCategoryId") != null) {
            selectedCategoryId = requestParams.get("selectedCategoryId").toString();
        }

        System.out.println("namedGraphLabelParam: " + namedGraphLabelParam);
        System.out.println("namedGraphIdParam: " + namedGraphIdParam);
        System.out.println("selectedCategoryLabel: " + selectedCategoryLabel);
        System.out.println("selectedCategoryId: " + selectedCategoryId);
        String graphUri = null;

        JSONObject responseJsonObject = new JSONObject();
        try {
            if (namedGraphIdParam == null) {
                Connection conn = DBService.initConnection();
                H2Manager h2 = new H2Manager(conn.createStatement(), conn);
                if (h2.namedGraphExists(namedGraphLabelParam)) {
                    responseJsonObject.put("success", false);
                    responseJsonObject.put("message", "Name: \"" + namedGraphLabelParam + "\" is already assigned.");
                    responseJsonObject.put("namedGraphIdParam", null);

                } else {
                    graphUri = "http://graph/" + System.currentTimeMillis();
                    responseJsonObject.put("success", true);
                    responseJsonObject.put("message", "The graph was successfully created.");
                    responseJsonObject.put("namedGraphIdParam", graphUri);
                    h2.insertNamedGraph(graphUri, namedGraphLabelParam, "", Integer.parseInt(selectedCategoryId));
                }
                conn.close();
            } else {
                responseJsonObject.put("success", true);
                responseJsonObject.put("message", "The graph will be enriched with new data.");
                responseJsonObject.put("namedGraphIdParam", namedGraphIdParam);
            }
        } catch (Exception e) {
            responseJsonObject.put("success", false);
            responseJsonObject.put("message", e.getMessage());
            responseJsonObject.put("namedGraphIdParam", namedGraphIdParam);
        }

        return responseJsonObject;
    }

    /*
     * Saving meta-data information for the file to be uploaded in the database
     */
    @RequestMapping(value = "/insertUserProfileMetadata", method = RequestMethod.POST, produces = {"application/json;charset=utf-8"})
    public @ResponseBody
    JSONObject insertUserProfileMetadata(@RequestHeader(value = "Authorization") String authorizationToken, @RequestBody JSONObject requestParams) throws IOException, ParseException {

//        System.out.println("Saving user-profile metadata into the database...");
//
//        String nameStr = null;
//        String emailStr = null;
//        String roleStr = null;
//        String organizationNameStr = null;
//        String organizationUrlStr = null;
//        String namedGraphIdStr = null;
//        String namedGraphLabelParam = null;
//
//        // Retrieving user's ID (username)
//        if (requestParams.get("name") != null) {
//            nameStr = requestParams.get("name").toString();
//        }
//        if (requestParams.get("email") != null) {
//            emailStr = requestParams.get("email").toString();
//        }
//        if (requestParams.get("role") != null) {
//            roleStr = requestParams.get("role").toString();
//        }
//        if (requestParams.get("organization") != null) {
//            organizationNameStr = requestParams.get("organization").toString();
//        }
//        if (requestParams.get("organizationURL") != null) {
//            organizationUrlStr = requestParams.get("organizationURL").toString();
//        }
//        // Dummy hard coded organization URL (temporarily)
//        //organizationUrlStr = "https://www.ics.forth.gr/";
//        if (requestParams.get("namedGraphId") != null) {
//            namedGraphIdStr = requestParams.get("namedGraphId").toString();
//        }
//
//        if (requestParams.get("namedGraphLabel") != null) {
//            namedGraphLabelParam = requestParams.get("namedGraphLabel").toString();
//        }
//
//        System.out.println("nameStr: " + nameStr);
//        System.out.println("emailStr: " + emailStr);
//        System.out.println("roleStr: " + roleStr);
//        System.out.println("organizationNameStr: " + organizationNameStr);
//        System.out.println("organizationUrlStr: " + organizationUrlStr);
//        System.out.println("namedGraphIdStr: " + namedGraphIdStr);
//        System.out.println("namedGraphLabelStr: " + namedGraphLabelParam);
        //////
//
//        RestClient client = new RestClient(serviceUrl, namespace, authorizationToken);
//        VirtuosoRestClient client = new VirtuosoRestClient(serviceUrl, authorizationToken);
//        Response resp = client.executeUpdatePOSTJSON(q1);
        //resp = client.executeUpdatePOSTJSON(q2);
        JSONObject responseJsonObject = new JSONObject();
        // Dummy response (under development) always success
        responseJsonObject.put("success", true);
        responseJsonObject.put("message", "User Profile metadata inserted successfully!");
        responseJsonObject.put("linkingUpdateQuery", linkingUpdateQuery);

        // Below is the respective code for creating new namedgraph 
        // (left here for guideline for the new code)
        /*
        try {
            if (namedGraphIdParam == null) {
                Connection conn = DBService.initConnection();
                H2Manager h2 = new H2Manager(conn.createStatement(), conn);
                if (h2.namedGraphExists(namedGraphLabelParam)) {
                    responseJsonObject.put("success", false);
                    responseJsonObject.put("message", "Name: \"" + namedGraphLabelParam + "\" is already assigned.");
                    responseJsonObject.put("namedGraphIdParam", null);

                } else {
                    graphUri = "http://graph/" + System.currentTimeMillis();
                    responseJsonObject.put("success", true);
                    responseJsonObject.put("message", "The graph was successfully created.");
                    responseJsonObject.put("namedGraphIdParam", graphUri);
                    h2.insertNamedGraph(graphUri, namedGraphLabelParam, "", Integer.parseInt(selectedCategoryId));
                }
                conn.close();
            } else {
                responseJsonObject.put("success", true);
                responseJsonObject.put("message", "The graph will be enriched with new data.");
                responseJsonObject.put("namedGraphIdParam", namedGraphIdParam);
            }
        } catch (Exception e) {
            responseJsonObject.put("success", false);
            responseJsonObject.put("message", e.getMessage());
            responseJsonObject.put("namedGraphIdParam", namedGraphIdParam);
        }
         */
        return responseJsonObject;
    }

    @RequestMapping(value = "/upload", method = RequestMethod.POST)
    public ResponseEntity uploadFile(MultipartHttpServletRequest request) {

        System.out.println("Uploading...");
        String contentTypeParam = request.getParameter("contentTypeParam"); // Retrieving param that holds the file's content-type
        String namedGraphIdParam = request.getParameter("namedGraphIdParam");
        String authorizationToken = request.getParameter("authorizationParam");//.getHeader("Authorization");		// Retrieving the authorization token
        String linkingUpdateQueryParam = request.getParameter("linkingUpdateQuery");

        System.out.println("authorizationToken: " + authorizationToken);
        System.out.println("contentTypeParam: " + contentTypeParam);
        System.out.println("namedGraphIdParam: " + namedGraphIdParam);
        System.out.println("linkingUpdateQueryParam: " + linkingUpdateQueryParam);

        String importResponseJsonString = null;
        /////
        try {
            ///////
            request.setCharacterEncoding("UTF-8");
            Iterator<String> itr = request.getFileNames();

//            RestClient client = new RestClient(serviceUrl, namespace, authorizationToken);
            VirtuosoRestClient restClient = new VirtuosoRestClient(serviceUrl, authorizationToken);
            while (itr.hasNext()) {
                String uploadedFile = itr.next();

                MultipartFile multipartFile = request.getFile(uploadedFile);
                String mimeType = multipartFile.getContentType();
                String filename = multipartFile.getOriginalFilename();
                System.out.println("# FILE: " + filename);
                System.out.println("# MIME: " + mimeType);
                byte[] bytes = multipartFile.getBytes();

                String fileContent = new String(bytes);
                //System.out.println("# FILE CONTENS: " + fileContent);

                Response importResponse = restClient.importFile(fileContent, contentTypeParam, namedGraphIdParam, authorizationToken);
                int status = importResponse.getStatus();
                importResponseJsonString = importResponse.readEntity(String.class);
                if (status == 500) {
                    importResponseJsonString = "There was an internal error. please check that you have selected the correct content-type.";
                    System.out.println("status=500 -- importResponseJsonString");
                    return new ResponseEntity<>(importResponseJsonString, HttpStatus.INTERNAL_SERVER_ERROR);
                } //
            }
        } catch (Exception e) {
            return new ResponseEntity<>(importResponseJsonString, HttpStatus.INTERNAL_SERVER_ERROR);
        }

        return new ResponseEntity<>(importResponseJsonString, HttpStatus.OK);
    }

    @RequestMapping(value = "/after_upload_process", method = RequestMethod.POST, produces = {"application/json;charset=utf-8"})
    public @ResponseBody
    JSONObject afterUploadProc(@RequestHeader(value = "Authorization") String authorizationToken, @RequestBody JSONObject requestParams) {
        JSONObject responseJsonObject = new JSONObject();
        try {
            System.out.println("after all uploads...");
            String namedGraphIdParam = null;
            String namedGraphLabel = null;
            // Retrieving the label of the named graph
            if (requestParams.get("namedGraphIdParam") != null) {
                namedGraphIdParam = requestParams.get("namedGraphIdParam").toString();
            }
            if (requestParams.get("namedGraphLabel") != null) {
                namedGraphLabel = requestParams.get("namedGraphLabel").toString();
            }
            VirtuosoRestClient restClient = new VirtuosoRestClient(serviceUrl, authorizationToken);
            List<String> entityUris = DBService.executeBelongsInQueries(serviceUrl, authorizationToken, namedGraphIdParam, namedGraphLabel);
            int status = 200;
            Response resp;
            for (String uri : entityUris) {
                resp = restClient.executeUpdatePOSTJSON(linkingUpdateQuery.replace("@#$%ENTITY%$#@", uri));
                status = resp.getStatus();
                if (status != 200) {
                    break;
                }
            }
            if (status == 200) {
                System.out.println("linking data with provdata ->> " + status);
            } else {
                responseJsonObject.put("success", false);
                responseJsonObject.put("message", "Metadata materialization process failed");
                return responseJsonObject;
            }
            resp = restClient.executeUpdatePOSTJSON(provUpdateQuery);
            status = resp.getStatus();
            if (status == 200) {
                System.out.println("inserting user profile metadata ->> " + status);
            } else {
                responseJsonObject.put("success", false);
                responseJsonObject.put("message", "Metadata materialization process failed");
                return responseJsonObject;
            }
            ////
            Set<String> matRelationEntities = DBService.executeRelationsMatQueries(serviceUrl, namespace, authorizationToken, namedGraphIdParam);
            H2Manager.enrichMatRelationsTable(serviceUrl, authorizationToken, namedGraphIdParam, matRelationEntities);
            responseJsonObject.put("success", true);
            responseJsonObject.put("message", "Metadata materialization process was completed successfully!");
            System.out.println("Metadata materialization process was completed successfully!");
        } catch (Exception ex) {
            responseJsonObject.put("success", false);
            responseJsonObject.put("message", ex.getMessage());
        }
        return responseJsonObject;
    }
}

package forth.ics.isl.triplestore;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import javax.ws.rs.ClientErrorException;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.apache.http.client.ClientProtocolException;
import org.json.simple.JSONObject;
import java.util.Set;
import java.util.Arrays;
import java.util.HashSet;
import org.json.simple.JSONArray;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;

public class VirtuosoRestClient {

    private String serviceUrl;
    private String authorizationToken;

    public VirtuosoRestClient(String serviceUrl, String authorizationToken) throws IOException {
        // Be put in comments due to conflicts with log4j when creating the fat jar
        Set<String> loggers = new HashSet<>(Arrays.asList("org.apache.http", "groovyx.net.http"));
//        for (String log : loggers) {
//            Logger logger = (Logger) LoggerFactory.getLogger(log);
//            logger.setLevel(Level.INFO);
//            logger.setAdditive(false);
//        }
        ///
        this.serviceUrl = serviceUrl;
        this.authorizationToken = authorizationToken;
    }

    public VirtuosoRestClient(String serviceUrl) throws IOException {
        // Be put in comments due to conflicts with log4j when creating the fat jar
        Set<String> loggers = new HashSet<>(Arrays.asList("org.apache.http", "groovyx.net.http"));
//        for (String log : loggers) {
//            Logger logger = (Logger) LoggerFactory.getLogger(log);
//            logger.setLevel(Level.INFO);
//            logger.setAdditive(false);
//        }
        ///
        this.serviceUrl = serviceUrl;
    }

    /**
     * Imports an RDF like file on the server using post synchronously
     *
     * @param file A String holding the path of the file, the contents of which
     * will be uploaded.
     * @param format	The RDF format
     * @param namespace A String representation of the nameSpace
     * @param namedGraph A String representation of the nameGraph
     * @return A response from the service.
     */
    public Response importFile(String content, String format, String namedGraph, String authorizationToken)
            throws ClientProtocolException, IOException {
        String restURL;
        restURL = serviceUrl + "/import/virtuoso";
        // Taking into account nameGraph in the construction of the URL
        if (namedGraph != null && !namedGraph.isEmpty()) {
            restURL = restURL + "?graph=" + namedGraph;
        }
        System.out.println("restURL (virtuoso rest client): " + restURL);

        String mimeType = format;
        Client client = ClientBuilder.newClient();
        WebTarget webTarget = client.target(restURL).queryParam("graph", namedGraph);
        System.out.println("authorizationToken NEW: " + authorizationToken);
        System.out.println("URI: " + webTarget.getUri().toString());
        //System.out.println("CONTENT: " + content);
        Response response = webTarget.request()
                .header("Authorization", authorizationToken)
                .post(Entity.entity(content, mimeType));
        return response;
    }

    /**
     * Imports an RDF-like file on the server
     *
     * @param queryStr A String that holds the query to be submitted on the
     * server.
     * @param namespace A String representation of the nameSpace to be used
     * @param format
     * @return The output of the query
     */
    public Response executeSparqlQuery(String queryStr, int timeout, String format, String authorizationToken) throws UnsupportedEncodingException {
        Client client = ClientBuilder.newClient();
        WebTarget webTarget = client.target(serviceUrl + "/query/virtuoso")
                .queryParam("timeout", timeout)
                .queryParam("format", format)
                .queryParam("query", URLEncoder.encode(queryStr, "UTF-8").replaceAll("\\+", "%20"));
        //System.out.println("HttpHeaders.AUTHORIZATION: " + authorizationToken);
        Invocation.Builder invocationBuilder = webTarget.request().header("Authorization", authorizationToken);
        Response response = invocationBuilder.get();
//        System.out.println(response.getStatus());
        return response;
    }

    public Response executeSparqlQuery(String queryStr, String format, int timeout) throws UnsupportedEncodingException {
        Client client = ClientBuilder.newClient();
        WebTarget webTarget = client.target(serviceUrl + "/query/virtuoso")
                .queryParam("format", format)
                .queryParam("timeout", timeout)
                .queryParam("query", URLEncoder.encode(queryStr, "UTF-8").replaceAll("\\+", "%20"));
        //System.out.println("HttpHeaders.AUTHORIZATION: " + authorizationToken);
        Invocation.Builder invocationBuilder = webTarget.request().header("Authorization", authorizationToken);
        Response response = invocationBuilder.get();
        return response;
    }

    public Response executeBatchSparqlQueryPOST(JSONArray queries, String format) throws UnsupportedEncodingException {
        Client client = ClientBuilder.newClient();
        JSONObject json = new JSONObject();
        json.put("query", queries.toJSONString());
        json.put("format", format);
        WebTarget webTarget = client.target(serviceUrl + "/query/virtuoso/batch");
        return webTarget.request(MediaType.APPLICATION_JSON).
                header("Authorization", authorizationToken).post(Entity.json(json.toJSONString()));
    }

    public Response executeCountQueryPOST(String query, String format) throws UnsupportedEncodingException {
        Client client = ClientBuilder.newClient();
        JSONObject json = new JSONObject();
        json.put("query", query);
        json.put("format", format);
        WebTarget webTarget = client.target(serviceUrl + "/query/virtuoso/count");
        return webTarget.request(MediaType.APPLICATION_JSON).
                header("Authorization", authorizationToken).post(Entity.json(json.toJSONString()));
    }

    public Response executeUpdatePOSTJSON(String updateQuery) throws ClientErrorException {
        Client client = ClientBuilder.newClient();
        JSONObject json = new JSONObject();
        json.put("query", updateQuery);
        WebTarget webTarget = client.target(serviceUrl + "/update/virtuoso");
        return webTarget.request(MediaType.APPLICATION_JSON).
                header("Authorization", authorizationToken).post(Entity.json(json.toJSONString()));
    }

    public static String readFileData(String filename) {
        File f = new File(filename);
        StringBuilder sb = new StringBuilder();
        try {
            BufferedReader br = new BufferedReader(new FileReader(f));
            String line = null;
            while ((line = br.readLine()) != null) {
                sb.append(line + "\n");
            }
            br.close();
        } catch (Exception e) {
            System.out.println("Exception while reading import data occured .");
            return null;
        }
        return sb.toString();
    }

}

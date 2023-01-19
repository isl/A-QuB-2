package forth.ics.isl.controller;

import java.io.IOException;
import org.json.simple.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

@Scope(scopeName = "session", proxyMode = ScopedProxyMode.TARGET_CLASS)
@Controller
public class ConfigurationController {

    @Value("${service.url}")
    private String serviceUrl;
    @Value("${triplestore.namespace}")
    private String namespace;
    @Value("${uri.prefix}")
    private String uriPrefix;
    @Value("${service.max.result.count}")
    private String maxResultCountLimit;
    @Value("${portal.state}")
    private String portalState;

    @RequestMapping(value = "/retrieve_service_model", method = RequestMethod.POST, produces = {"application/json"})
    public @ResponseBody
    JSONObject retrieveServiceModelOptions(@RequestHeader(value = "Authorization") String authorizationToken) throws IOException {
        System.out.println("-> retrieveSERVICE MODEL Options in ConfigurationController.java");
        JSONObject serviceModelJsonObject = new JSONObject();
        serviceModelJsonObject.put("url", serviceUrl);
        serviceModelJsonObject.put("namespace", namespace);
        serviceModelJsonObject.put("uriPrefix", uriPrefix);
        serviceModelJsonObject.put("maxResultCountLimit", maxResultCountLimit);
        return serviceModelJsonObject;
    }

    @RequestMapping(value = "/retrieve_portal_state", method = RequestMethod.POST, produces = {"application/json"})
    public @ResponseBody
    JSONObject retrievePortalStateOption() throws IOException {
        System.out.println("retrieve PORTAL STATE Option() in ConfigurationController.java");
        JSONObject portaStateJsonObject = new JSONObject();
        portaStateJsonObject.put("portalState", portalState);
        return portaStateJsonObject;
    }
}

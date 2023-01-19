
package forth.ics.isl.controller;

import forth.ics.isl.service.DBService;
import java.io.IOException;
import java.sql.SQLException;
import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.eclipse.jetty.client.api.Authentication;
import org.json.simple.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import static org.springframework.web.bind.annotation.RequestMethod.POST;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 *
 * @author konsolak
 */
@Controller

public class ProfileController {

    @Autowired
    private DBService dbService;

    @PostConstruct
    public void init() throws IOException, SQLException {
        dbService = new DBService();
    }

    @RequestMapping(value = "/toLogin", method = POST)
    @ResponseBody
    JSONObject toLogin(@RequestParam("username") String username, @RequestParam("password") String password) {
        boolean isValid = dbService.isValidUser(username, password);
        JSONObject responseJsonObject = new JSONObject();
        if (isValid) {
            responseJsonObject.put("message", "success");
            responseJsonObject.put("role", "RESEARCHER");
            responseJsonObject.put("status", "SUCCEED");
            responseJsonObject.put("token", "");

        } else {
            responseJsonObject.put("message", "error");
        }
        return responseJsonObject;
    }

    @RequestMapping(value = "/getprofile", method = RequestMethod.POST, produces = {"application/json"})
    public @ResponseBody
    JSONObject getProfile(@RequestBody JSONObject requestParams) {
        System.out.println("getProfile:" + requestParams.get("username").toString());
        String username = requestParams.get("username").toString();
        JSONObject responseJsonObject = dbService.getProfile(username);
        return responseJsonObject;
    }

    @RequestMapping(value = "/getprofiles", method = RequestMethod.POST, produces = {"application/json"})
    public @ResponseBody
    JSONObject getProfiles() {
        System.out.println("/getProfiles");
        JSONObject responseJsonObject = dbService.getProfiles();
        return responseJsonObject;
    }

    @RequestMapping(value = "/updateprofile", method = RequestMethod.POST, produces = {"application/json"})
    public @ResponseBody
    JSONObject updateProfile(@RequestBody JSONObject requestParams) {
        System.out.println("updateProfile:" + requestParams.get("userid").toString());
        JSONObject responseJsonObject = dbService.updateProfile(requestParams);
        return responseJsonObject;
    }

    @RequestMapping(value = "/registerUser", method = RequestMethod.POST, produces = {"application/json"})
    public @ResponseBody
    JSONObject registerUser(@RequestBody JSONObject requestParams) {
        System.out.println("Register:" + requestParams.get("name").toString());
        JSONObject responseJsonObject = dbService.registerUser(requestParams);
        return responseJsonObject;
    }

}

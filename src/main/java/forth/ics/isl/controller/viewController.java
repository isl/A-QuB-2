package forth.ics.isl.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
public class viewController {

//	@RequestMapping({ "/welcome", "/login", "/registration", "/welcome", "/navigation", "/favorites", "/userProfile", "/userManagement", "/privacyPolicy", "/import", "/tabs", "/query", "/geoQuery", "/advancedQuery", "/import", 
//		"/retrieve_service_model", "/retrieve_portal_state",  })
//	public String index() {
//		return "forward:/index.html";
//	}
    @RequestMapping(value = "/**/{[path:[^\\.]*}")
    public String redirect() {
        return "forward:/";
    }

}

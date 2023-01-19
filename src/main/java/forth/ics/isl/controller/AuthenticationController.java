//package forth.ics.isl.controller;
//
//import java.util.Arrays;
//import java.util.HashSet;
//import java.util.List;
//import org.json.simple.JSONArray;
//import org.json.simple.JSONObject;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.security.core.context.SecurityContextHolder;
//import org.springframework.security.core.userdetails.UserDetails;
//import org.springframework.security.core.userdetails.UsernameNotFoundException;
//import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
//import org.springframework.stereotype.Controller;
//import org.springframework.web.bind.annotation.RequestBody;
//import org.springframework.web.bind.annotation.RequestMapping;
//import org.springframework.web.bind.annotation.RequestMethod;
//import org.springframework.web.bind.annotation.ResponseBody;
//import forth.ics.isl.data.model.Role;
//import forth.ics.isl.data.model.User;
//import forth.ics.isl.repository.RoleRepository;
//import forth.ics.isl.repository.UserRepository;
//import forth.ics.isl.service.UserDetailsServiceImpl;
//
//@Controller
//public class AuthenticationController {
//
//    private static final String Roles = null;
//
//    @Autowired
//    UserRepository userRepository;
//
//    @Autowired
//    private BCryptPasswordEncoder passwordEncoder;
//
//    @Autowired
//    UserDetailsServiceImpl userDetail;
//
//    @Autowired
//    RoleRepository roleRepository;
//
//    @RequestMapping(value = "/loginSucess", method = RequestMethod.GET)
//    public @ResponseBody
//    JSONObject loginsucess() {
//        System.out.println("/loginSucess");
//        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
//        String username = "";
//        String role = "RESEARCHER";
//        if (principal instanceof UserDetails) {
//            username = ((UserDetails) principal).getUsername();
//            if (((UserDetails) principal).getAuthorities().size() == 1) {
//                role = ((UserDetails) principal).getAuthorities().iterator().next().getAuthority();
//            }
//        } else {
//            username = principal.toString();
//        }
//        JSONObject responseJsonObject = new JSONObject();
//        responseJsonObject.put("message", "success");
//        responseJsonObject.put("role", role);
//        responseJsonObject.put("status", "SUCCEED");
//        responseJsonObject.put("token", "");
//        return responseJsonObject;
//    }
//
//    @RequestMapping(value = "/registerUser", method = RequestMethod.POST, produces = {"application/json"})
//    public @ResponseBody
//    JSONObject registerUser(@RequestBody JSONObject requestParams) {
//        System.out.println("Register:" + requestParams.get("name").toString());
//        JSONObject responseJsonObject = new JSONObject();
//        String username = requestParams.get("userid").toString();
//        try {
//            UserDetails userDetails = userDetail.loadUserByUsername(username);
//            responseJsonObject.put("message", "Username already exists");
//            responseJsonObject.put("status", "FAIL");
//        } catch (UsernameNotFoundException ex) {
//            User user = new User();
//            setProfile(user, requestParams);
//            responseJsonObject.put("message", "Registration completed successfully!");
//            responseJsonObject.put("status", "SUCCEED");
//        }
//        System.out.println("Response: \n " + responseJsonObject);
//        return responseJsonObject;
//    }
//
//    @RequestMapping(value = "/getprofile", method = RequestMethod.POST, produces = {"application/json"})
//    public @ResponseBody
//    JSONObject getProfile(@RequestBody JSONObject requestParams) {
//        System.out.println("getProfile:" + requestParams.get("username").toString());
//        JSONObject responseJsonObject = new JSONObject();
//        String username = requestParams.get("username").toString();
//
//        User user = userRepository.findByUsername(username);
//        if (user != null) {
//            String role = "RESEARCHER";
//            responseJsonObject.put("message", "success");
//            responseJsonObject.put("email", user.getEmail());
//            responseJsonObject.put("name", user.getName());
//            responseJsonObject.put("organization", user.getOrganization());
//            responseJsonObject.put("organizationURL", user.getOrganizationUrl());
//            responseJsonObject.put("password", user.getPassword());
//            if (user.getRoles().size() >= 1) {
//                role = user.getRoles().iterator().next().getName();
//            }
//            responseJsonObject.put("role", role);
//            responseJsonObject.put("userId", username);
//        } else {
//            responseJsonObject.put("message", "fail");
//        }
//
//        System.out.println("Response: \n " + responseJsonObject);
//        return responseJsonObject;
//    }
//
//    @RequestMapping(value = "/getprofiles", method = RequestMethod.POST, produces = {"application/json"})
//    public @ResponseBody
//    JSONObject getProfiles() {
//        System.out.println("/getProfiles");
//        JSONObject responseJsonObject = new JSONObject();
//        JSONArray userArr = new JSONArray();
//        List<User> users = userRepository.findAll();
//        if (users.size() > 0) {
//            for (User user : users) {
//                JSONObject userObj = new JSONObject();
//
//                String role = "RESEARCHER";
//                userObj.put("message", "success");
//                userObj.put("email", user.getEmail());
//                userObj.put("name", user.getName());
//                userObj.put("organization", user.getOrganization());
//                userObj.put("organizationURL", user.getOrganizationUrl());
//                userObj.put("password", user.getPassword());
//                if (user.getRoles().size() >= 1) {
//                    role = user.getRoles().iterator().next().getName();
//                }
//                userObj.put("role", role);
//                userObj.put("userId", user.getUsername());
//
//                userArr.add(userObj);
//            }
//            responseJsonObject.put("response", userArr);
//        } else {
//            responseJsonObject.put("message", "Error");
//        }
//        System.out.println("Response: \n " + responseJsonObject);
//        return responseJsonObject;
//    }
//
//    @RequestMapping(value = "/updateprofile", method = RequestMethod.POST, produces = {"application/json"})
//    public @ResponseBody
//    JSONObject updateProfile(@RequestBody JSONObject requestParams) {
//        System.out.println("updateProfile:" + requestParams.get("userid").toString());
//        System.out.println(requestParams);
//        JSONObject responseJsonObject = new JSONObject();
//        String username = requestParams.get("userid").toString();
//
//        User user = userRepository.findByUsername(username);
//        if (user != null) {
//            setProfile(user, requestParams);
//            responseJsonObject.put("message", "Profile updated successfully");
//            responseJsonObject.put("status", "SUCCEED");
//
//        } else {
//            responseJsonObject.put("message", "Fail");
//        }
//
//        System.out.println("Response: \n " + responseJsonObject);
//        return responseJsonObject;
//    }
//
//    private void setProfile(User user, JSONObject userInfo) {
//        user.setName(userInfo.get("name").toString());
//        user.setEmail(userInfo.get("email").toString());
//        user.setUsername(userInfo.get("userid").toString());
//        String password = userInfo.get("password").toString();
//        if (user.getPassword() != null) {
//            if (!user.getPassword().contentEquals(password)) {
//                user.setPassword(passwordEncoder.encode(password));
//            }
//        } else {
//            user.setPassword(passwordEncoder.encode(password));
//        }
//        user.setOrganization(userInfo.get("organization").toString());
//        user.setOrganizationUrl(userInfo.get("organizationURL").toString());
//        user.setEnabled(true);
//        Role userRole = roleRepository.findByName(userInfo.get("role").toString());
//        user.setRoles(new HashSet<Role>(Arrays.asList(userRole)));
//        userRepository.save(user);
//    }
//}

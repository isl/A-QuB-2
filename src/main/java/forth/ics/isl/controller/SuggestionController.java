package forth.ics.isl.controller;

import com.fasterxml.jackson.databind.JsonNode;
import forth.ics.isl.data.model.EntityModel;
import forth.ics.isl.data.model.RelationModel;
import forth.ics.isl.data.model.parser.Utils;
import forth.ics.isl.service.PropertiesService;
import forth.ics.isl.triplestore.VirtuosoRestClient;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.PostConstruct;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

@Scope(scopeName = "session", proxyMode = ScopedProxyMode.TARGET_CLASS)
@Controller
public class SuggestionController {

    @Value("${service.url}")
    private String serviceUrl;
    @Value("${triplestore.namespace}")
    private String namespace;
    private JsonNode currQueryResult;
//    private RestClient restClient;
    private VirtuosoRestClient restClient;

    @PostConstruct
    public void init() throws IOException {
        // before controller
    }

    @RequestMapping(value = "/dynamic/get_relations_related_entities", method = RequestMethod.POST, produces = {"application/json;charset=utf-8"})
    public @ResponseBody
    JSONArray populateRelationsEntities(@RequestHeader(value = "Authorization") String authorizationToken, @RequestBody JSONObject requestParams) throws IOException, ParseException, SQLException {
        System.out.println(" in populateRelationsEntities() in SuggestionController.java");
        System.out.println("   all params: " + requestParams.keySet());
        System.out.println("   namespace: " + namespace);
        System.out.println("   serviceUrl: " + serviceUrl);
        System.out.println("   authorizationToken: " + authorizationToken);

        // NEW
        JSONObject jsonModel = Utils.parse((String) requestParams.get("model"));
//        System.out.println(" > model: ");
        System.out.println(jsonModel);
        String fromClause = (String) jsonModel.get("queryFrom");
        JSONObject queryModel = (JSONObject) jsonModel.get("queryModel");
        JSONObject rowModel = (JSONObject) jsonModel.get("rowModel");
        JSONObject targetModel = (JSONObject) queryModel.get("targetModel");
        EntityModel entityModel = new EntityModel((JSONObject) targetModel.get("selectedTargetEntity"));
        int entityId = entityModel.getId();

        if (rowModel != null) {
            System.out.println(" > The row model is not null!");
            EntityModel entityModel2 = new EntityModel((JSONObject) rowModel.get("selectedRelatedEntity"));
            System.out.println("   We need associations of entity type: " + entityModel2.getName());
            entityId = entityModel2.getId();
        }

        ArrayList<Map> allEntities = (ArrayList) requestParams.get("entities");
        HashMap<Integer, JSONObject> entitiesMap = new HashMap<>();
        for (int i = 0; i < allEntities.size(); i++) {
            JSONObject entity = new JSONObject(allEntities.get(i));
            entitiesMap.put((Integer) entity.get("id"), entity);
        }

        HashMap<Integer, RelationModel> configRelations = PropertiesService.getRelations();

        JSONArray result = new JSONArray();
        ArrayList<Integer> connectedEntities = new ArrayList<>();

        for (int relid : configRelations.keySet()) {
            RelationModel rm = configRelations.get(relid);

            if (rm.getSourceEntity() == entityId) {
                connectedEntities.add(rm.getDestinationEntity());
                JSONObject obj = new JSONObject();
                obj.put("related_entity", entitiesMap.get(rm.getDestinationEntity()));
                JSONObject relJSON = new JSONObject();
                relJSON.put("id", rm.getId());
                relJSON.put("name", rm.getName());
                relJSON.put("source_entity", rm.getSourceEntity());
                relJSON.put("destination_entity", rm.getDestinationEntity());
                relJSON.put("relation_graph_pattern", rm.getRelationGraphPattern());
                relJSON.put("source_uri_variable", rm.getSourceUriVariable());
                relJSON.put("source_shown_variables", rm.getSourceShownVariables());
                relJSON.put("destination_uri_variable", rm.getDestinationUriVariable());
                relJSON.put("destination_shown_variables", rm.getDestinationShownVariables());
                relJSON.put("source_filter_pattern", rm.getSourceFilterPattern());
                relJSON.put("destination_filter_pattern", rm.getDestinationFilterPattern());
                obj.put("relation", relJSON);
                result.add(obj);
            }
        }
        System.out.println("   connected entities: " + connectedEntities);
        System.out.println("================");
        return result;

    }

    @RequestMapping(value = "/dynamic/get_relations", method = RequestMethod.POST, produces = {"application/json;charset=utf-8"})
    public @ResponseBody
    JSONArray populateRelations(@RequestHeader(value = "Authorization") String authorizationToken, @RequestBody JSONObject requestParams) throws IOException, ParseException {
        System.out.println("   populateRelations() in SuggestionController.java");
        System.out.println("  all params: " + requestParams.keySet());
//        System.out.println("  model: " + requestParams.get("model"));

        JSONObject jsonModel = Utils.parse((String) requestParams.get("model"));
        JSONObject jsonQueryModel = (JSONObject) jsonModel.get("queryModel");

        JSONObject jsonTargetModel = (JSONObject) jsonQueryModel.get("targetModel");
        EntityModel targetEntityModel = new EntityModel((JSONObject) jsonTargetModel.get("selectedTargetEntity"));
        System.out.println("   >>> target entity: " + targetEntityModel.getName());

        JSONObject jsonRowModel = (JSONObject) jsonModel.get("rowModel");
        JSONObject jsonRelatedModel = (JSONObject) jsonRowModel.get("selectedRelatedEntity");
        EntityModel relatedEntityModel = new EntityModel(jsonRelatedModel);
        System.out.println("   >>> related entity: " + relatedEntityModel.getName());

        JSONArray relatedEntityRelationTuples = (JSONArray) new JSONParser().parse((String) jsonRowModel.get("relatedEntityRelationTuples"));
        //System.out.println(" >>> relationTuples: " + relatedEntityRelationTuples);

        JSONArray relations = new JSONArray();
        for (int i = 0; i < relatedEntityRelationTuples.size(); i++) {
            JSONObject obj = (JSONObject) relatedEntityRelationTuples.get(i);
            Long idlong = (Long) ((JSONObject) (obj.get("related_entity"))).get("id");
            int relEntityId = idlong.intValue();
            if (relEntityId == relatedEntityModel.getId()) {
                JSONObject relation = new JSONObject();
                relation.put("id", (Long) ((JSONObject) (obj.get("relation"))).get("id"));
                relation.put("name", (String) ((JSONObject) (obj.get("relation"))).get("name"));
                relation.put("source_entity", (Long) ((JSONObject) (obj.get("relation"))).get("source_entity"));
                relation.put("destination_entity", (Long) ((JSONObject) (obj.get("relation"))).get("destination_entity"));
                relation.put("relation_graph_pattern", (String) ((JSONObject) (obj.get("relation"))).get("relation_graph_pattern"));
                relation.put("source_uri_variable", (String) ((JSONObject) (obj.get("relation"))).get("source_uri_variable"));
                relation.put("source_shown_variables", (String) ((JSONObject) (obj.get("relation"))).get("source_shown_variables"));
                relation.put("destination_uri_variable", (String) ((JSONObject) (obj.get("relation"))).get("destination_uri_variable"));
                relation.put("destination_shown_variables", (String) ((JSONObject) (obj.get("relation"))).get("destination_shown_variables"));
                relation.put("source_filter_pattern", (String) ((JSONObject) (obj.get("relation"))).get("source_filter_pattern"));
                relation.put("destination_filter_pattern", (String) ((JSONObject) (obj.get("relation"))).get("destination_filter_pattern"));
                relation.put("materialised_property", (String) ((JSONObject) (obj.get("relation"))).get("materialised_property"));
                relations.add(relation);
            }
        }

        return relations;
    }
}

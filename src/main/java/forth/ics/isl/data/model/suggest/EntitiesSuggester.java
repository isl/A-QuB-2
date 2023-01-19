package forth.ics.isl.data.model.suggest;

import forth.ics.isl.data.model.RelationModel;
import forth.ics.isl.data.model.parser.FilterExp;
import forth.ics.isl.data.model.parser.QueryDataModel;
import forth.ics.isl.data.model.parser.RelatedModel;
import forth.ics.isl.data.model.parser.Utils;
import forth.ics.isl.service.DBService;
import forth.ics.isl.triplestore.VirtuosoRestClient;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

public class EntitiesSuggester {

    private QueryDataModel model;
    private String fromClause, logicalExpression;
    private RelatedModel rowModel;
    private String namespace, endpoint, token;

    public EntitiesSuggester(JSONObject jsonModel, String namespace, String endpoint, String token) {
        init(jsonModel);
        this.namespace = namespace;
        this.endpoint = endpoint;
        this.token = token;
    }

    public EntitiesSuggester(String rowModel, String queryModel, String namespace, String endpoint, String token) {
        this.model = new QueryDataModel(queryModel);
        this.rowModel = new RelatedModel(rowModel);
        this.fromClause = model.getSelectedGraphsClause();
        this.namespace = namespace;
        this.endpoint = endpoint;
        this.token = token;
    }

    public EntitiesSuggester(String jsonModelString, String namespace, String endpoint, String token) {
        JSONObject jsonModel = Utils.parse(jsonModelString);
        this.model = new QueryDataModel(jsonModelString);
        this.fromClause = (String) jsonModel.get("queryFrom");
        this.logicalExpression = (String) jsonModel.get("logicalExpression");
        JSONObject row = (JSONObject) jsonModel.get("rowModel");
        if (row != null) {
            this.rowModel = new RelatedModel(row, model.getSelectedGraphs());
        } else {
            this.rowModel = null;
        }
        this.namespace = namespace;
        this.endpoint = endpoint;
        this.token = token;
    }

    private void init(JSONObject jsonModel) {
        this.model = new QueryDataModel(jsonModel);
        this.fromClause = (String) jsonModel.get("queryFrom");
        this.rowModel = new RelatedModel((String) jsonModel.get("rowModel"));
    }

    public String getFromClause() {
        return fromClause;
    }

    public RelatedModel getRowModel() {
        return rowModel;
    }

    public QueryDataModel getModel() {
        return model;
    }

    // NEW
    public JSONArray retrieveRelationsEntitiesNEW(ArrayList<Map> entities) throws IOException, ParseException, SQLException {
        System.out.println("-> retrieveRelationsEntities() in EntitiesSuggester.java");

        ArrayList<RelationModel> relations = DBService.retrieveAllRelationsSpecs();
        int sourceId = this.model.getEntityModel().getId();
        System.out.println("   relations: " + relations);
        System.out.println("   source id: " + sourceId);

        HashMap<Integer, JSONObject> entitiesMap = new HashMap<>();
        for (int i = 0; i < entities.size(); i++) {
            JSONObject entity = new JSONObject(entities.get(i));
            entitiesMap.put((Integer) entity.get("id"), entity);
        }

        JSONArray result = new JSONArray();
        ArrayList<Integer> connectedEntities = new ArrayList<>();
        for (RelationModel relModel : relations) {
            if (relModel.getSourceEntity() == sourceId) {
                connectedEntities.add(relModel.getDestinationEntity());
                JSONObject obj = new JSONObject();
                obj.put("related_entity", entitiesMap.get(relModel.getDestinationEntity()));
                JSONObject relJSON = new JSONObject();
                //relJSON.put("uri", relModel.getMaterialisedProperty());
                relJSON.put("name", relModel.getName());
                obj.put("relation", relJSON);
                result.add(obj);
            }
        }
        System.out.println("   connected entities: " + connectedEntities);

        return result;
    }

    public JSONArray retrieveRelationsEntities(ArrayList<Map> entities) throws IOException, ParseException, SQLException {
        System.out.println("-> retrieveRelationsEntities() in EntitiesSuggester.java");

        Map<String, String> relations = DBService.retrieveAllRelations();
        HashMap<String, JSONObject> entitiesMap = new HashMap<>();
        for (int i = 0; i < entities.size(); i++) {
            JSONObject entity = new JSONObject(entities.get(i));
            entitiesMap.put((String) entity.get("uri"), entity);
        }
        JSONArray result = new JSONArray();
        JSONArray queries = new JSONArray();
        //find the related model selected
        List<RelatedModel> relModels = getModel().getRelatedModels();
        RelatedModel row = getRowModel();
        RelatedModel testingRelModel = findNullRelatedModel(relModels, null);
        testingRelModel.setSelectedGraphs(this.model.getSelectedGraphs());
        HashMap<String, List<String>> sugRelationRelEntities;
        if (row != null) {
            sugRelationRelEntities = row.getSugRelationsRelatedEntities();
        } else {
            sugRelationRelEntities = testingRelModel.findRelationsRelatedEntities(model.getTargetModel().getName());
        }
        if (testingRelModel.getFilterExp() == FilterExp.OR) {
            for (String relation : sugRelationRelEntities.keySet()) {
                for (String relatedEntityUri : sugRelationRelEntities.get(relation)) {
                    JSONObject obj = new JSONObject();
                    JSONObject relEntity = entitiesMap.get(relatedEntityUri);
                    if (relEntity == null) {
                        continue;
                    }
                    obj.put("related_entity", relEntity);
                    JSONObject relJSON = new JSONObject();
                    relJSON.put("uri", relation);
                    relJSON.put("name", relations.get(relation));
                    obj.put("relation", relJSON);
                    result.add(obj);
                }
            }
        } else {
            for (String relation : sugRelationRelEntities.keySet()) {
//                String relEntityUri = sugRelationRelEntities.get(relation);
                for (String relEntityUri : sugRelationRelEntities.get(relation)) {
                    JSONObject relEntity = entitiesMap.get(relEntityUri);
                    if (relEntity == null) {
                        continue;
                    }
                    testingRelModel.setRelatedVarName((String) relEntity.get("var_name"));
                    testingRelModel.setRelatedUri(relEntityUri);
                    testingRelModel.setRelationUri(relation);
                    testingRelModel.setRelationName(relations.get(relation));
                    String query = model.toSPARQL();
                    if (query != null) {
                        query = query.replace("distinct", "") + " limit 1";
                        queries.add(query);
                        System.out.println(query);
                    }
                }
            }
            if (!queries.isEmpty()) {
                // execute the set of queries
//                RestClient client = new RestClient(endpoint, namespace, token);
                VirtuosoRestClient client = new VirtuosoRestClient(endpoint, token);
                JSONArray results = (JSONArray) new JSONParser().parse(client.executeBatchSparqlQueryPOST(queries, "application/json").readEntity(String.class));
//                System.out.println(results.toString());
                int i = 0;
                for (String relation : sugRelationRelEntities.keySet()) {
                    for (String relEntityUri : sugRelationRelEntities.get(relation)) {
                        JSONObject relEntity = entitiesMap.get(relEntityUri);
                        if (relEntity == null) {
                            continue;
                        }
                        JSONObject qRes = (JSONObject) new JSONParser().parse((String) results.get(i));
                        JSONArray res = (JSONArray) ((JSONObject) qRes.get("results")).get("bindings");
                        if (!res.isEmpty()) {
//                            String relatedEntityUri = sugRelationRelEntities.get(relation);
                            JSONObject obj = new JSONObject();
                            JSONObject relatedEntity = entitiesMap.get(relEntityUri);
                            obj.put("related_entity", relatedEntity);
                            JSONObject relJSON = new JSONObject();
                            relJSON.put("uri", relation);
                            relJSON.put("name", relations.get(relation));
                            obj.put("relation", relJSON);
                            result.add(obj);
                            System.out.println(relation + " -> " + sugRelationRelEntities.get(relation));
                        }
                        i++;
                    }
                }
            }
        }
        return result;
    }

    private RelatedModel findRelatedModelFromRow(List<RelatedModel> relModels, RelatedModel searched, RelatedModel found) {
        for (RelatedModel relModel : relModels) {
            if (found != null) {
                break;
            }
            if (relModel.equals(searched)) {
                return findRelatedModelFromRow(relModel.getRelatedModels(), searched, relModel);
            }
            if (!relModel.getRelatedModels().isEmpty()) {
                return findRelatedModelFromRow(relModel.getRelatedModels(), searched, found);
            }
        }
        return found;
    }

    private RelatedModel findNullRelatedModel(List<RelatedModel> relModels, RelatedModel found) {
        if (relModels != null) {
            for (RelatedModel relModel : relModels) {
                if (found != null) {
                    break;
                }
                if (relModel.isNullRelatedModel()) {
                    return findNullRelatedModel(relModel.getRelatedModels(), relModel);
                }
                if (relModel.getRelatedModels() != null && !relModel.getRelatedModels().isEmpty()) {
                    RelatedModel result = findNullRelatedModel(relModel.getRelatedModels(), found);
                    if (result != null) {
                        return result;
                    }
                }
            }
        }
        return found;
    }

    public static void main(String[] args) throws IOException, ParseException, SQLException {

    }

}

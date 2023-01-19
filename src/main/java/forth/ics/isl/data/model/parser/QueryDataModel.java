package forth.ics.isl.data.model.parser;

import forth.ics.isl.data.model.EntityModel;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

public class QueryDataModel {

    private TargetModel targetModel;
    private EntityModel entityModel;
    private List<RelatedModel> relatedModels;
    private List<String> selectedGraphs;
    private String selectedGraphsClause;

    public QueryDataModel() {
    }

    public QueryDataModel(JSONObject jsonModel) {
        init(jsonModel);
    }

    public QueryDataModel(String jsonModelString) {
        JSONObject jsonModel = Utils.parse(jsonModelString);
        init(jsonModel);
    }

    private void init(JSONObject jsonModel) {
        selectedGraphs = Utils.getGraphsFromClause((String) jsonModel.get("queryFrom"));
        selectedGraphsClause = (String) jsonModel.get("queryFrom");
        JSONObject queryModel = (JSONObject) jsonModel.get("queryModel");

        //NEW
        System.out.println(" Creating the entity model....");
        JSONObject jsonTargetModel = (JSONObject) queryModel.get("targetModel");
        entityModel = new EntityModel((JSONObject) jsonTargetModel.get("selectedTargetEntity"));

        // PREVIOUS //
        targetModel = new TargetModel((JSONObject) queryModel.get("targetModel"));

        this.relatedModels = new ArrayList<>();
        JSONArray related = (JSONArray) queryModel.get("relatedModels");
        boolean containsKeywordFilters = false;
        boolean containsGeoFilters = false;
        for (int i = 0; i < related.size(); i++) {
            RelatedModel relModel = new RelatedModel((JSONObject) related.get(i), selectedGraphs);
            //I have not selected any related models
//            if (relModel.getRelatedName() == null) {
//                break;
//            }
            relatedModels.add(relModel);
            if (relModel.getKeywordSearchPattern() != null || relModel.containsKeywordSearch()) {
                containsKeywordFilters = true;
            }
            if (relModel.getGeoSearchPattern() != null || relModel.containsGeoSearch()) {
                containsGeoFilters = true;
            }
        }
        //check if I have related entities with both keyword and geofilters
        //if so, then use only the filter geosearch.
        if (containsKeywordFilters || containsGeoFilters) {
            for (RelatedModel relModel : relatedModels) {
                relModel.setGeoSearchPattern(relModel.getFilterGeoSearchPattern());
            }
        }
    }

    public TargetModel getTargetModel() {
        return targetModel;
    }

    public List<RelatedModel> getRelatedModels() {
        return relatedModels;
    }

    public List<String> getSelectedGraphs() {
        return selectedGraphs;
    }

    public String getSelectedGraphsClause() {
        return selectedGraphsClause;
    }

    public EntityModel getEntityModel() {
        return entityModel;
    }

    public String toSPARQL() {
        String targetVar = targetModel.getVarName();
        String selectionList = targetModel.getSelectionList(targetVar);
        int relCnt = 0;
        List<String> relEntitiesBlocks = new ArrayList<>();
        boolean relatedExist = true;
        for (RelatedModel relModel : relatedModels) {
            if (!relModel.isNullRelatedModel()) {
//            query.append(relModel.createSPARQLBlock(targetVar, relCnt));
                relEntitiesBlocks.add(relModel.createSPARQLBlock(targetVar, "" + relCnt));
                relCnt++;
            }
        }
        if (relatedModels.size() != relEntitiesBlocks.size()) {
            relatedExist = false;
        }
        StringBuilder query = new StringBuilder();
        query.append("select " + selectionList + " " + getSelectedGraphsClause() + " where {\n");
        query.append(targetModel.getSelectionPattern(targetVar));
        query.append((targetModel.getKeywordSearchPattern(targetVar)).trim() + "\n");
        if (relatedModels.size() > 0 && relEntitiesBlocks.size() > 0 && relatedExist) {
            if (relatedModels.size() == 1 && relEntitiesBlocks.size() == 1) {
                query.append(relEntitiesBlocks.get(0));
            } else {
                String currentBlock = relEntitiesBlocks.get(0);
                for (int i = 1; i < relatedModels.size(); i++) {
                    String block = relEntitiesBlocks.get(i);
                    RelatedModel relModel = relatedModels.get(i);
                    if (relModel.getFilterExp() == FilterExp.AND) {
                        currentBlock = currentBlock + block;
                    } else {
                        currentBlock = "{\n" + currentBlock + "} UNION {\n" + block + "}\n";
                    }
                }
                query.append(currentBlock);
            }
        } else {
            query.append("\n");
        }
        Set<String> queryLines = new LinkedHashSet<>(Arrays.asList(query.toString().split("\\\n")));
        query.setLength(0);
        for (String line : queryLines) {
            query.append(line + "\n");
        }
        query.append("}");
        return query.toString().trim().replaceAll("(?m)^\\s", "");
    }

    public static void main(String[] args) throws SQLException {

    }

}

package forth.ics.isl.data.model.parser;

import forth.ics.isl.service.DBService;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.springframework.beans.factory.annotation.Autowired;

public class RelatedModel {

    private long id;
    private String relationName, relationUri;
    private String relatedName, relatedUri, relatedVarName;
    //this hashmap is used to keep for each entity the potential relations/related entities 
    private HashMap<String, List<String>> sugRelationsRelatedEntities;
    private String relatedEntitySearchText;
    private List<String> selectedUris;
    private FilterExp filterExp;
    private List<RelatedModel> relatedModels;
    private RelatedModel parentModel;
    private String keywordSearchPattern;
    private List<String> selectedGraphs;
    private JSONArray relatedEntityRelationTuples;
    private String fromDate, endDate;
    private String geoSearchPattern;
    private String filterGeoSearchPattern;
    private boolean containsKeywordSearch;

    @Autowired
    private DBService dbService;
    private boolean containsGeoSearch;

    public RelatedModel(JSONObject jsonModel, List<String> selectedGraphs) {
        this.selectedGraphs = selectedGraphs;
        init(jsonModel);
    }

    //this constructor is used when we construct a rowModel
    public RelatedModel(String jsonModelString) {
        JSONObject jsonModel = Utils.parse(jsonModelString);
        init(jsonModel);
    }

    public RelatedModel(String jsonModelString, List<String> selectedGraphs) {
        this.selectedGraphs = selectedGraphs;
        JSONObject jsonModel = Utils.parse(jsonModelString);
        init(jsonModel);
    }

    private void init(JSONObject jsonModel) {
        this.id = (long) jsonModel.get("id");
        String expr = (String) jsonModel.get("outerSelectedFilterExpression");
        this.filterExp = FilterExp.fromString(expr);
        System.out.println(" in init() of RelatedModel.java ");
        System.out.println(" >>>>>>> selectedRelatedEntity: " + jsonModel.get("selectedRelatedEntity"));
        System.out.println(" >>>>>>> relatedEntityRelationTuples: " + jsonModel.get("relatedEntityRelationTuples"));
        System.out.println(" >>>>>>> selectedRelation: " + jsonModel.get("selectedRelation"));
        if (jsonModel.get("relatedEntityRelationTuples") != null) {
            try {
                this.relatedEntityRelationTuples = (JSONArray) new JSONParser().parse((String) jsonModel.get("relatedEntityRelationTuples"));
            } catch (ParseException ex) {
                Logger.getLogger(RelatedModel.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        if (jsonModel.get("selectedRelatedEntity") == null) {
            return;
        }
        if (jsonModel.get("selectedRelatedEntity") != null) {
            this.relatedName = (String) ((JSONObject) jsonModel.get("selectedRelatedEntity")).get("name");
            this.relatedVarName = (String) ((JSONObject) jsonModel.get("selectedRelatedEntity")).get("var_name");
            this.relatedUri = (String) ((JSONObject) jsonModel.get("selectedRelatedEntity")).get("uri");
            System.out.println("   relatedName: " + relatedName);
            System.out.println("   relatedVarName: " + relatedVarName);
            System.out.println("   relatedUri: " + relatedUri);
        }
        if (jsonModel.get("selectedRelation") != null) {
            this.relationName = (String) ((JSONObject) jsonModel.get("selectedRelation")).get("name");
            this.relationUri = (String) ((JSONObject) jsonModel.get("selectedRelation")).get("uri");
            System.out.println("   relationName: " + relationName);
            System.out.println("   relationUri: " + relationUri);
        }
        ///
        StringBuilder sb = new StringBuilder();
        JSONArray searchChips = (JSONArray) jsonModel.get("relatedChips");
        sb.append(Utils.getChipsFilter(searchChips));
//        for (int i = 0; i < searchChips.size(); i++) {
//            sb.append((String) ((JSONObject) searchChips.get(i)).get("name") + " ");
//        }
        sb.append((String) jsonModel.get("relatedEntitySearchText"));
        this.relatedEntitySearchText = sb.toString().trim();
        this.keywordSearchPattern = null;
        if (!this.relatedEntitySearchText.equals("")) {
            this.containsKeywordSearch = true;
            this.keywordSearchPattern = (String) ((JSONObject) jsonModel.get("selectedRelatedEntity")).get("keyword_search");
//            this.keywordSearchPattern = getKeywordSearchPattern(relatedVarName);
            this.keywordSearchPattern = this.keywordSearchPattern.replace("@#$%TERM%$#@", this.relatedEntitySearchText);
        }
        ///
        this.fromDate = (String) ((JSONObject) jsonModel.get("rangeOfDates")).get("from");
        this.endDate = (String) ((JSONObject) jsonModel.get("rangeOfDates")).get("until");
        ///
        if (jsonModel.get("boundingBox") != null) {
            this.containsGeoSearch = true;
            this.geoSearchPattern = (String) ((JSONObject) jsonModel.get("selectedRelatedEntity")).get("geo_search");
            JSONObject boundingBox = (JSONObject) jsonModel.get("boundingBox");
            geoSearchPattern = geoSearchPattern.replace("@#$%NORTH%$#@", "" + boundingBox.get("north"));
            geoSearchPattern = geoSearchPattern.replace("@#$%SOUTH%$#@", "" + boundingBox.get("south"));
            geoSearchPattern = geoSearchPattern.replace("@#$%EAST%$#@", "" + boundingBox.get("east"));
            geoSearchPattern = geoSearchPattern.replace("@#$%WEST%$#@", "" + boundingBox.get("west"));
            ////
            this.filterGeoSearchPattern = (String) ((JSONObject) jsonModel.get("selectedRelatedEntity")).get("filter_geo_search");
            filterGeoSearchPattern = filterGeoSearchPattern.replace("@#$%NORTH%$#@", "" + boundingBox.get("north"));
            filterGeoSearchPattern = filterGeoSearchPattern.replace("@#$%SOUTH%$#@", "" + boundingBox.get("south"));
            filterGeoSearchPattern = filterGeoSearchPattern.replace("@#$%EAST%$#@", "" + boundingBox.get("east"));
            filterGeoSearchPattern = filterGeoSearchPattern.replace("@#$%WEST%$#@", "" + boundingBox.get("west"));
        }
        ///
        JSONArray instances = (JSONArray) jsonModel.get("selectedRelatedInstanceList");
        this.selectedUris = new ArrayList<>();
        for (int i = 0; i < instances.size(); i++) {
            JSONObject instance = (JSONObject) instances.get(i);
            selectedUris.add((String) ((JSONObject) instance.get("uri")).get("value"));
        }
        //if I have some uris, then ignore the search clause
        if (!selectedUris.isEmpty() && (boolean) jsonModel.get("allRelatedSearchResultsIsSelected") == false) {
            this.keywordSearchPattern = "";
        }
        relatedModels = new ArrayList<>();
        if (selectedGraphs != null) {
            manageEmbeddedRelatedModels((JSONArray) jsonModel.get("rowModelList"));
            sugRelationsRelatedEntities = findRelationsRelatedEntities(relatedName);
        }
    }

    public HashMap<String, List<String>> findRelationsRelatedEntities(String fromEntityName) {
        HashMap<String, List<String>> result = new HashMap<>();
        System.out.println("-> in findRelationsRelatedEntities() of RelatedModel.java ");
        JSONArray entities = DBService.retrieveAllEntities(false);
        JSONArray relationsEntities = DBService.retrieveRelationsEntities(selectedGraphs, fromEntityName, Utils.jsonArrayToList(entities));
        for (int i = 0; i < relationsEntities.size(); i++) {
            JSONObject obj = (JSONObject) relationsEntities.get(i);
            String key = (String) ((JSONObject) obj.get("relation")).get("uri");
            String value = (String) ((JSONObject) obj.get("related_entity")).get("uri");
            if (!result.containsKey(key)) {
                result.put(key, new ArrayList<>());
            }
            result.get(key).add(value);
        }
        return result;
    }

    public void setRelationName(String relationName) {
        this.relationName = relationName;
    }

    public void setRelationUri(String relationUri) {
        this.relationUri = relationUri;
    }

    public void setRelatedName(String relatedName) {
        this.relatedName = relatedName;
    }

    public void setRelatedUri(String relatedUri) {
        this.relatedUri = relatedUri;
    }

    public void setRelatedVarName(String relatedVarName) {
        this.relatedVarName = relatedVarName;
    }

    public void setSelectedGraphs(List<String> selectedGraphs) {
        this.selectedGraphs = selectedGraphs;
    }

    public void setFilterExp(String filterExp) {
        this.filterExp = FilterExp.fromString(filterExp);
    }

    public String getRelationName() {
        return relationName;
    }

    public String getFromDate() {
        return fromDate;
    }

    public String getEndDate() {
        return endDate;
    }

    public String getRelationUri() {
        return relationUri;
    }

    public String getRelatedName() {
        return relatedName;
    }

    public String getRelatedUri() {
        return relatedUri;
    }

    public String getRelatedEntitySearchText() {
        return relatedEntitySearchText;
    }

    public List<String> getSelectedUris() {
        return selectedUris;
    }

    public FilterExp getFilterExp() {
        return filterExp;
    }

    public List<RelatedModel> getRelatedModels() {
        return relatedModels;
    }

    public String getKeywordSearchPattern(String var, String target) {
        String result = keywordSearchPattern != null ? keywordSearchPattern.replaceAll("@#\\$%VAR%\\$#@", var) : "";
        if (result != null && result.contains("TARGET")) {
            return result.replaceAll("@#\\$%TARGET%\\$#@", target);
        }
        return result;
    }

    public String getGeoSearchPattern(String var) {
        return geoSearchPattern != null ? geoSearchPattern.replaceAll("@#\\$%VAR%\\$#@", var) : "";
    }

    public String getFilterGeoSearchPattern(String var) {
        return filterGeoSearchPattern != null ? filterGeoSearchPattern.replaceAll("@#\\$%VAR%\\$#@", var) : "";
    }

    public String getRelatedVarName() {
        return relatedVarName;
    }

    public HashMap<String, List<String>> getSugRelationsRelatedEntities() {
        return sugRelationsRelatedEntities;
    }

    public long getId() {
        return id;
    }

    public JSONArray getRelatedEntityRelationTuples() {
        return relatedEntityRelationTuples;
    }

    public void setId(long id) {
        this.id = id;
    }

    public RelatedModel getParentModel() {
        return parentModel;
    }

    public void setParentModel(RelatedModel parentModel) {
        this.parentModel = parentModel;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("RelatedModel{" + "id=" + id + ",\n ").
                append("relationName=" + relationName + ",\n ").
                append("relationUri=" + relationUri + ",\n ").
                append("relatedName=" + relatedName + ",\n ").
                append("relatedUri=" + relatedUri + ",\n ").
                append("relatedEntitySearchText=" + relatedEntitySearchText + ",\n ").
                append("selectedUris=" + selectedUris + ",\n ").
                append("filterExp=" + filterExp + ",\n ");
        if (relatedModels == null || relatedModels.isEmpty()) {
            sb.append("relatedModels=" + "[ ]" + " \n");
        } else {
            sb.append("relatedModels= [");
            for (RelatedModel model : relatedModels) {
                sb.append(model.getRelatedName() + " ");
            }
            sb.append("], \n");
        }
        sb.append("parentModel=" + parentModel + ",\n ").
                append("keywordSearchPattern=" + keywordSearchPattern + "}");
        return sb.toString();
    }

    private void manageEmbeddedRelatedModels(JSONArray relatedEntities) {
        if (relatedEntities == null || relatedEntities.isEmpty()) {
            return;
        } else {
            for (int i = 0; i < relatedEntities.size(); i++) {
                JSONObject obj = (JSONObject) relatedEntities.get(i);
//                if (obj.get("selectedRelation") == null || obj.get("selectedRelatedEntity") == null) {
//                    return;
//                }
                RelatedModel relModel = new RelatedModel(obj, selectedGraphs);
                relModel.setParentModel(this);
                if (this.containsKeywordSearch) {
//                    relModel.setContainsKeywordSearch();
                    relModel.setGeoSearchPattern(relModel.getFilterGeoSearchPattern());
                }
                this.relatedModels.add(relModel);
                if (relModel.getKeywordSearchPattern() != null) {
                    containsKeywordSearch = true;
                    setGeoSearchPattern(getFilterGeoSearchPattern());
                }
            }
        }
    }

    public boolean isNullRelatedModel() {
        if (this.relatedUri == null && this.relationName == null) {
            return true;
        }
        return false;
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = (int) (23 * hash + this.id);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final RelatedModel other = (RelatedModel) obj;
        if (this.id != other.id) {
            return false;
        }
        return true;
    }

    public String createSPARQLBlock(String targetEntity, String relCnt) {
        StringBuilder block = new StringBuilder();
        String relVar = getRelatedVarName() + "_" + relCnt;
        block.append("?" + targetEntity + " <" + getRelationUri() + "> ?" + relVar + ".\n");
        if (getSelectedUris() != null && !getSelectedUris().isEmpty()) {
            block.append("filter(?" + relVar + " in (");
            int cnt = 0;
            for (String selUri : getSelectedUris()) {
                block.append("<" + selUri + ">");
                if (cnt < getSelectedUris().size() - 1) {
                    block.append(", ");
                }
                cnt++;
            }
            block.append(")).\n");
        }
        if (keywordSearchPattern == null) {
            block.append((getGeoSearchPattern(relVar) + "\n").trim() + "\n");
        } else {
            String keywordBlock = (getKeywordSearchPattern(relVar, targetEntity) + "\n").trim() + "\n";
            block.append(keywordBlock);
            block.append((getFilterGeoSearchPattern(relVar) + "\n").trim() + "\n");
        }
        createDateFilterBlock(targetEntity, relVar, block);
        ////
        List<String> relEntitiesBlocks = new ArrayList<>();
        if (relatedModels != null && !relatedModels.isEmpty()) {
            int newCnt = 0;
            for (RelatedModel relModel : relatedModels) {
                String curBlock = relModel.createSPARQLBlock(relVar, relCnt + "_" + newCnt);
                relEntitiesBlocks.add(curBlock);
                newCnt++;
            }
            if (relatedModels.size() == 1) {
                block.append(relEntitiesBlocks.get(0));
            } else {
                String currentBlock = relEntitiesBlocks.get(0);
                for (int i = 1; i < relatedModels.size(); i++) {
                    String bl = relEntitiesBlocks.get(i);
                    RelatedModel relModel = relatedModels.get(i);
                    if (relModel.getFilterExp() == FilterExp.AND) {
                        currentBlock = currentBlock + bl;
                    } else {
                        currentBlock = "{\n" + currentBlock + "} UNION {\n" + bl + "}\n";
                    }
                }
                block.append(currentBlock);
            }
        }

        return block.toString();
    }

    private void createDateFilterBlock(String targetEntity, String relVar, StringBuilder block) {
        ///consider start, end dates
        if (fromDate != null || endDate != null) {
            String concStr = targetEntity + "_" + relVar;
            block.append("?" + targetEntity + " <http://eurocris.org/ontology/cerif#is_source_of> ?" + concStr + ".\n");
            block.append("?" + relVar + " <http://eurocris.org/ontology/cerif#is_destination_of> ?" + concStr + ".\n");
            if (fromDate != null) {
                block.append("?" + concStr + " <http://eurocris.org/ontology/cerif#has_startDate> ?" + concStr + "_start_date.\n");
                block.append("filter (xsd:dateTime('" + fromDate + "T00:00:00') >= xsd:dateTime(?" + concStr + "_start_date)).\n");
                block.append("OPTIONAL { ?" + concStr + " <http://eurocris.org/ontology/cerif#has_endDate> ?" + concStr + "_end_date.\n").
                        append("filter (xsd:dateTime('" + fromDate + "T00:00:00') <= xsd:dateTime(?" + concStr + "_end_date)). }\n");
            }
            if (endDate != null) {
                block.append("?" + concStr + " <http://eurocris.org/ontology/cerif#has_endDate> ?" + concStr + "_end_date.\n").
                        append("filter (xsd:dateTime('" + endDate + "T00:00:00') <= xsd:dateTime(?" + concStr + "_end_date)).\n");
                block.append("OPTIONAL { ?" + concStr + " <http://eurocris.org/ontology/cerif#has_startDate> ?" + concStr + "_start_date.\n").
                        append("filter (xsd:dateTime('" + endDate + "T00:00:00') >= xsd:dateTime(?" + concStr + "_start_date)). }\n");
            }
//            block.append("?" + concStr + " <http://eurocris.org/ontology/cerif#has_startDate> ?" + concStr + "_start_date.\n");
//            block.append("?" + concStr + " <http://eurocris.org/ontology/cerif#has_endDate> ?" + concStr + "_end_date.\n");
//            if (fromDate != null) {
            //block.append("filter (xsd:dateTime(?" + concStr + "_start_date) >= xsd:dateTime('" + fromDate + "T00:00:00')).\n");
//            }
//            if (endDate != null) {
            //block.append("filter (xsd:dateTime(?" + concStr + "_end_date) <= xsd:dateTime('" + endDate + "T00:00:00')).\n");
//            }
        }
    }

    public ArrayList<LinkedHashMap> get(String relatedEntityRelationTuples) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    public void setKeywordSearchPattern(String keywordSearchPattern) {
        this.keywordSearchPattern = keywordSearchPattern;
    }

    public void setGeoSearchPattern(String geoSearchPattern) {
        this.geoSearchPattern = geoSearchPattern;
    }

    public void setFilterGeoSearchPattern(String geoKeywordSearchPattern) {
        this.filterGeoSearchPattern = geoKeywordSearchPattern;
    }

    public String getKeywordSearchPattern() {
        return keywordSearchPattern;
    }

    public String getGeoSearchPattern() {
        return geoSearchPattern;
    }

    public String getFilterGeoSearchPattern() {
        return filterGeoSearchPattern;
    }

    public boolean containsKeywordSearch() {
        return containsKeywordSearch;
    }

    public void setContainsKeywordSearch() {
        this.containsKeywordSearch = true;
    }

    boolean containsGeoSearch() {
        return containsGeoSearch;
    }

}

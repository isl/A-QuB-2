package forth.ics.isl.data.model.parser;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

public class TargetModel {

    private String name, uri, selectionList, varName, selectionPattern;
    private String targetEntitySearchText;
    private String keywordSearchPattern;

    public TargetModel(JSONObject jsonModel) {
        init(jsonModel);
    }

    public TargetModel(String jsonModelString) {
        JSONObject jsonModel = Utils.parse(jsonModelString);
        init(jsonModel);
    }

    private void init(JSONObject jsonModel) {
        if (jsonModel == null) {
            return;
        }
        //NEW

        this.name = (String) ((JSONObject) jsonModel.get("selectedTargetEntity")).get("name");
        this.varName = (String) ((JSONObject) jsonModel.get("selectedTargetEntity")).get("var_name");
        this.selectionPattern = (String) ((JSONObject) jsonModel.get("selectedTargetEntity")).get("selection_pattern");
        this.uri = (String) ((JSONObject) jsonModel.get("selectedTargetEntity")).get("uri");
        this.selectionList = (String) ((JSONObject) jsonModel.get("selectedTargetEntity")).get("selection_list");
        ///
        StringBuilder sb = new StringBuilder();
        JSONArray searchChips = (JSONArray) jsonModel.get("targetChips");
        sb.append(Utils.getChipsFilter(searchChips));
        sb.append((String) jsonModel.get("searchTargetKeywords"));
        this.targetEntitySearchText = sb.toString().trim();
        this.keywordSearchPattern = "";
        if (!this.targetEntitySearchText.equals("")) {
            this.keywordSearchPattern = (String) ((JSONObject) jsonModel.get("selectedTargetEntity")).get("keyword_search");
            this.keywordSearchPattern = this.keywordSearchPattern.replace("@#$%TERM%$#@", this.targetEntitySearchText);
        }
    }

    public String getKeywordSearchPattern(String var) {
        return keywordSearchPattern.replaceAll("@#\\$%VAR%\\$#@", var);
    }

    public String getName() {
        return name;
    }

    public String getUri() {
        return uri;
    }

    public String getSelectionList(String var) {
        if (var == null) {
            return selectionList;
        }
        return selectionList.replaceAll("@#\\$%VAR%\\$#@", var);
    }

    public String getVarName() {
        return varName;
    }

    public String getSelectionPattern(String var) {
        if (var == null) {
            return selectionPattern;
        }
        return selectionPattern.replaceAll("@#\\$%VAR%\\$#@", var);
    }

    @Override
    public String toString() {
        return "TargetModel{" + "name=" + name + ", uri=" + uri + '}';
    }
}

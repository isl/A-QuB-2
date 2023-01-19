package forth.ics.isl.data.model;

import org.json.simple.JSONObject;

import forth.ics.isl.data.model.EntityTypes.EntityType;

public class EntityModel {

    private int id;
    private String name;
    private String instancesQuery;
    private String searchQuery;
    private String geoQuery;
    private String exploreQuery;
    private String exploreVariableTypes;
    private EntityType entityType;
    private boolean isVisibleInTarget;
    private String sourceUriVariable;

    public EntityModel(int id, String name, String instancesQuery, String searchQuery, String geoQuery, String exploreQuery, String exploreVariableTypes, EntityType entityType, String sourceUriVariable, boolean isVisibleInTarget) {
        this.id = id;
        this.name = name;
        this.instancesQuery = instancesQuery;
        this.searchQuery = searchQuery;
        this.geoQuery = geoQuery;
        this.exploreQuery = exploreQuery;
        this.exploreVariableTypes = exploreVariableTypes;
        this.entityType = entityType;
        this.isVisibleInTarget = isVisibleInTarget;
        this.sourceUriVariable = sourceUriVariable;

    }

    public String getExploreVariableTypes() {
        return exploreVariableTypes;
    }

    public void setExploreVariableTypes(String exploreVariableTypes) {
        this.exploreVariableTypes = exploreVariableTypes;
    }

    public String getSourceUriVariable() {
        return sourceUriVariable;
    }

    public void setSourceUriVariable(String sourceUriVariable) {
        this.sourceUriVariable = sourceUriVariable;
    }

    /**
     * @param jsonModel
     */
    public EntityModel(JSONObject jsonModel) {

        if (jsonModel.get("id") instanceof Long) {
            Long idlong = (Long) jsonModel.get("id");
            this.id = idlong.intValue();
        } else {
            this.id = (Integer) jsonModel.get("id");
        }
        this.name = (String) jsonModel.get("name");
        this.instancesQuery = (String) jsonModel.get("instances_query");
        this.searchQuery = (String) jsonModel.get("search_query");
        this.geoQuery = (String) jsonModel.get("geo_query");
        this.exploreQuery = (String) jsonModel.get("properties_query");
        this.entityType = EntityTypes.getEntityType((String) jsonModel.get("entityType"));
        this.isVisibleInTarget = (boolean) jsonModel.get("isVisibleInTarget");
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getInstancesQuery() {
        return instancesQuery;
    }

    public void setInstancesQuery(String instancesQuery) {
        this.instancesQuery = instancesQuery;
    }

    public String getSearchQuery() {
        return searchQuery;
    }

    public void setSearchQuery(String searchQuery) {
        this.searchQuery = searchQuery;
    }

    public String getGeoQuery() {
        return geoQuery;
    }

    public void setGeoQuery(String geoQuery) {
        this.geoQuery = geoQuery;
    }

    public String getExploreQuery() {
        return exploreQuery;
    }

    public void setExploreQuery(String exploreQuery) {
        this.exploreQuery = exploreQuery;
    }

    public EntityType getEntityType() {
        return entityType;
    }

    public void setEntityType(EntityType entitytype) {
        this.entityType = entitytype;
    }

    public boolean isVisibleInTarget() {
        return isVisibleInTarget;
    }

    public void setSourceVisible(boolean isVisibleInTarget) {
        this.isVisibleInTarget = isVisibleInTarget;
    }

    @Override
    public String toString() {
        return "EntityModel{" + "id=" + id + ", name=" + name + ", instancesQuery=" + instancesQuery + ", searchQuery=" + searchQuery + ", exploreQuery=" + exploreQuery + ", exploreVariableTypes=" + exploreVariableTypes + ", entityType=" + entityType + ", sourceUriVariable=" + sourceUriVariable + ", isVisibleInTarget=" + isVisibleInTarget + '}';
    }

}

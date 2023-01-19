package forth.ics.isl.data.model;

public class RelationModel {

    private int id;
    private String name;
    private int sourceEntity;
    private int destinationEntity;
    private String relationGraphPattern;
    private String sourceUriVariable;
    private String sourceShownVariables;
    private String destinationUriVariable;
    private String destinationShownVariables;
    private String sourceFilterPattern;
    private String destinationFilterPattern;

    public RelationModel(int id, String name, int sourceEntity, int destinationEntity, String relationGraphPattern, String sourceUriVariable, String sourceShownVariables, String destinationUriVariable, String destinationShownVariables, String sourceFilterPattern, String destinationFilterPattern) {
        this.id = id;
        this.name = name;
        this.sourceEntity = sourceEntity;
        this.destinationEntity = destinationEntity;
        this.relationGraphPattern = relationGraphPattern;
        this.sourceUriVariable = sourceUriVariable;
        this.sourceShownVariables = sourceShownVariables;
        this.destinationUriVariable = destinationUriVariable;
        this.destinationShownVariables = destinationShownVariables;
        this.sourceFilterPattern = sourceFilterPattern;
        this.destinationFilterPattern = destinationFilterPattern;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getSourceShownVariables() {
        return sourceShownVariables;
    }

    public void setSourceShownVariables(String sourceShownVariables) {
        this.sourceShownVariables = sourceShownVariables;
    }

    public String getDestinationShownVariables() {
        return destinationShownVariables;
    }

    public void setDestinationShownVariables(String destinationShownVariables) {
        this.destinationShownVariables = destinationShownVariables;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getSourceEntity() {
        return sourceEntity;
    }

    public void setSourceEntity(int sourceEntity) {
        this.sourceEntity = sourceEntity;
    }

    public int getDestinationEntity() {
        return destinationEntity;
    }

    public void setDestinationEntity(int destinationEntity) {
        this.destinationEntity = destinationEntity;
    }

    public String getRelationGraphPattern() {
        return relationGraphPattern;
    }

    public void setRelationGraphPattern(String relationGraphPattern) {
        this.relationGraphPattern = relationGraphPattern;
    }

    public String getSourceUriVariable() {
        return sourceUriVariable;
    }

    public void setSourceUriVariable(String sourceUriVariable) {
        this.sourceUriVariable = sourceUriVariable;
    }

    public String getDestinationUriVariable() {
        return destinationUriVariable;
    }

    public void setDestinationUriVariable(String destinationUriVariable) {
        this.destinationUriVariable = destinationUriVariable;
    }

    public String getSourceFilterPattern() {
        return sourceFilterPattern;
    }

    public void setSourceFilterPattern(String sourceFilterPattern) {
        this.sourceFilterPattern = sourceFilterPattern;
    }

    public String getDestinationFilterPattern() {
        return destinationFilterPattern;
    }

    public void setDestinationFilterPattern(String destinationFilterPattern) {
        this.destinationFilterPattern = destinationFilterPattern;
    }

    @Override
    public String toString() {
        return "RelationModel{" + "id=" + id + ", name=" + name + ", sourceEntity=" + sourceEntity + ", destinationEntity=" + destinationEntity + ", relationGraphPattern=" + relationGraphPattern + ", sourceUriVariable=" + sourceUriVariable + ", sourceShownVariables=" + sourceShownVariables + ", destinationUriVariable=" + destinationUriVariable + ", destinationShownVariables=" + destinationShownVariables + ", sourceFilterPattern=" + sourceFilterPattern + ", destinationFilterPattern=" + destinationFilterPattern + '}';
    }

}

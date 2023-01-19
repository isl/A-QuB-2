package forth.ics.isl.service;

import forth.ics.isl.data.model.EntityModel;
import forth.ics.isl.data.model.EntityTypes;
import forth.ics.isl.data.model.RelationModel;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

public class PropertiesService {

    private static final String configFilePath = "config.properties";
    private static final String applicationFilePath = "application.properties";

    public static Properties getConfigProperties() throws IOException {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        InputStream input = classLoader.getResourceAsStream(configFilePath);
        Properties prop = new Properties();
        if (input != null) {
            try {
                prop.load(input);
            } catch (IOException ex) {
                Logger.getLogger(PropertiesService.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        return prop;
    }

    public static Properties getApplicationProperties() {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        InputStream input = classLoader.getResourceAsStream(applicationFilePath);
        Properties prop = new Properties();
        if (input != null) {
            try {
                prop.load(input);
            } catch (IOException ex) {
                Logger.getLogger(PropertiesService.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        return prop;
    }

    public static HashMap<Integer, EntityModel> getEntities() throws IOException {

        Properties properties = PropertiesService.getConfigProperties();

        String entitiesProp = properties.getProperty("entities");
        String[] entitiesStr = entitiesProp.split(",");
        HashMap<Integer, EntityModel> entities = new HashMap<>();
        for (String entid : entitiesStr) {
            String name = properties.getProperty("entities." + entid.trim() + ".name").trim();
            String instancesQuery = properties.getProperty("entities." + entid.trim() + ".instancesQuery");
            if (instancesQuery != null) {
                instancesQuery = instancesQuery.trim();
            } else {
                instancesQuery = "";
            }
            String searchQuery = properties.getProperty("entities." + entid.trim() + ".searchQuery");
            if (searchQuery != null) {
                searchQuery = searchQuery.trim();
            } else {
                searchQuery = "";
            }

            String geoQuery = properties.getProperty("entities." + entid.trim() + ".geoQuery");
            if (geoQuery != null) {
                geoQuery = geoQuery.trim();
            } else {
                geoQuery = "";
            }

            String exploreQuery = properties.getProperty("entities." + entid.trim() + ".exploreQuery");
            if (exploreQuery != null) {
                exploreQuery = exploreQuery.trim();
            } else {
                exploreQuery = "";
            }

            String exploreVariableTypes = properties.getProperty("entities." + entid.trim() + ".exploreVariableTypes");
            if (exploreVariableTypes != null) {
                exploreVariableTypes = exploreVariableTypes.trim();
            } else {
                exploreVariableTypes = "";
            }

            String entityType = properties.getProperty("entities." + entid.trim() + ".entityType");
            EntityTypes.EntityType type = forth.ics.isl.data.model.EntityTypes.EntityType.LITERAL;

            if (entityType != null) {
                entityType = entityType.trim();
                type = EntityTypes.getEntityType(entityType);

            }

            String targetVisible = properties.getProperty("entities." + entid.trim() + ".isVisibleInTarget");
            boolean isVisibleInTarget = true;
            if (targetVisible != null) {
                if (targetVisible.equals("false")) {
                    isVisibleInTarget = false;
                }
            }

            String sourceUriVariable = properties.getProperty("entities." + entid.trim() + ".sourceUriVariable");
            if (sourceUriVariable != null) {
                sourceUriVariable = sourceUriVariable.trim();
            } else {
                sourceUriVariable = "";
            }

            EntityModel entityModel = new EntityModel(Integer.valueOf(entid.trim()), name, instancesQuery, searchQuery,
                    geoQuery, exploreQuery, exploreVariableTypes, type, sourceUriVariable, isVisibleInTarget);
            entities.put(Integer.valueOf(entid.trim()), entityModel);
        }

        return entities;
    }

    public static HashMap<Integer, RelationModel> getRelations() throws IOException {

        Properties properties = PropertiesService.getConfigProperties();

        String relationsProps = properties.getProperty("relations");
        String[] relationsStr = relationsProps.split(",");
        HashMap<Integer, RelationModel> relations = new HashMap<>();

        for (String relid : relationsStr) {
            String name = properties.getProperty("relations." + relid.trim() + ".name").trim();
            String sourceEntity = properties.getProperty("relations." + relid.trim() + ".sourceEntity").trim();
            String destinationEntity = properties.getProperty("relations." + relid.trim() + ".destinationEntity")
                    .trim();
            String relationGraphPattern = properties.getProperty("relations." + relid.trim() + ".relationGraphPattern")
                    .trim();
            String sourceUriVariable = properties.getProperty("relations." + relid.trim() + ".sourceUriVariable");

            if (sourceUriVariable != null) {
                sourceUriVariable = sourceUriVariable.trim();
            } else {
                sourceUriVariable = "";
            }

            String sourceShownVariables = properties.getProperty("relations." + relid.trim() + ".sourceShownVariables");

            if (sourceShownVariables != null) {
                sourceShownVariables = sourceShownVariables.trim();
            } else {
                sourceShownVariables = "";
            }

            String destinationUriVariable = properties
                    .getProperty("relations." + relid.trim() + ".destinationUriVariable");
            if (destinationUriVariable != null) {
                destinationUriVariable = destinationUriVariable.trim();
            } else {
                destinationUriVariable = "";
            }

            String destinationShownVariables = properties
                    .getProperty("relations." + relid.trim() + ".destinationShownVariables");
            if (destinationShownVariables != null) {
                destinationShownVariables = destinationShownVariables.trim();
            } else {
                destinationShownVariables = "";
            }
            String sourceFilterPattern = properties.getProperty("relations." + relid.trim() + ".sourceFilterPattern");

            if (sourceFilterPattern != null) {
                sourceFilterPattern = sourceFilterPattern.trim();
            } else {
                sourceFilterPattern = "";
            }
            String destinationFilterPattern = properties
                    .getProperty("relations." + relid.trim() + ".destinationFilterPattern");
            if (destinationFilterPattern != null) {
                destinationFilterPattern = destinationFilterPattern.trim();
            } else {
                destinationFilterPattern = "";
            }
            RelationModel relationModel = new RelationModel(Integer.valueOf(relid.trim()), name,
                    Integer.valueOf(sourceEntity), Integer.valueOf(destinationEntity), relationGraphPattern,
                    sourceUriVariable, sourceShownVariables, destinationUriVariable, destinationShownVariables, sourceFilterPattern, destinationFilterPattern);
            relations.put(Integer.valueOf(relid.trim()), relationModel);

        }

        return relations;

    }

}

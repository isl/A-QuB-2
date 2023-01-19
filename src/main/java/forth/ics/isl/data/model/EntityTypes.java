package forth.ics.isl.data.model;

public class EntityTypes {

    public enum EntityType {
        GEOSPATIAL, TEMPORAL, LITERAL, LITERAL_GEOSPATIAL, NUMERIC, STRING, BOOLEAN, TIME_PRIMITIVE, BLANK
    }

    public static EntityType getEntityType(String type) {
        if (type.equals("GEOSPATIAL")) {
            return EntityType.GEOSPATIAL;
        } else if (type.equals("TEMPORAL")) {
            return EntityType.TEMPORAL;
        } else if (type.equals("LITERAL")) {
            return EntityType.LITERAL;
        } else if (type.equals("LITERAL_GEOSPATIAL")) {
            return EntityType.LITERAL_GEOSPATIAL;
        } else if (type.equals("NUMERIC")) {
            return EntityType.NUMERIC;
        } else if (type.equals("STRING")) {
            return EntityType.STRING;
        } else if (type.equals("BOOLEAN")) {
            return EntityType.BOOLEAN;
        } else if (type.equals("BLANK")) {
            return EntityType.BLANK;
        } else if (type.equals("TIME_PRIMITIVE")) {
            return EntityType.TIME_PRIMITIVE;
        } else {
            return EntityType.LITERAL;
        }
    }

}

package forth.ics.isl.data.model.parser;

public enum FilterExp {
    AND, OR;

    public static FilterExp fromString(String expr) {
        if (expr != null) {
            if (expr.equals("AND")) {
                return FilterExp.AND;
            } else if (expr.equals("OR")) {
                return FilterExp.OR;
            }
        }
        return null;
    }
}

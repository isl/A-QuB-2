package forth.ics.isl.data.model;

public class InputAdvancedRequest {

    int itemsPerPage;
    String QueryToExecute;

    public int getItemsPerPage() {
        return itemsPerPage;
    }

    public void setItemsPerPage(int itemsPerPage) {
        this.itemsPerPage = itemsPerPage;
    }

    public String getQueryToExecute() {
        return QueryToExecute;
    }

    public void setQueryToExecute(String queryToExecute) {
        QueryToExecute = queryToExecute;
    }

}

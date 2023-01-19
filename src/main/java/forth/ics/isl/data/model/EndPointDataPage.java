package forth.ics.isl.data.model;

import com.fasterxml.jackson.databind.node.ObjectNode;

public class EndPointDataPage {

    private ObjectNode result;
    private int page;
    private int totalItems;

    public ObjectNode getResult() {
        return result;
    }

    public void setResult(ObjectNode result) {
        this.result = result;
    }

    public int getPage() {
        return page;
    }

    public void setPage(int page) {
        this.page = page;
    }

    public int getTotalItems() {
        return totalItems;
    }

    public void setTotalItems(int totalItems) {
        this.totalItems = totalItems;
    }

}

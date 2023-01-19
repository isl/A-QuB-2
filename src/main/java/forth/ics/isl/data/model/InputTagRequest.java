package forth.ics.isl.data.model;

import java.util.List;

public class InputTagRequest {

    int itemsPerPage;
    List<NgTag> terms;
    String selectedEntity;
    String selectedProjection;
    List<String> selectedNamegraphs;

    public int getItemsPerPage() {
        return itemsPerPage;
    }

    public void setItemsPerPage(int itemsPerPage) {
        this.itemsPerPage = itemsPerPage;
    }

    public List<NgTag> getTerms() {
        return terms;
    }

    public void setTerms(List<NgTag> terms) {
        this.terms = terms;
    }

    public String getSelectedEntity() {
        return selectedEntity;
    }

    public void setSelectedEntity(String selectedEntity) {
        this.selectedEntity = selectedEntity;
    }

    public String getSelectedProjection() {
        return selectedProjection;
    }

    public void setSelectedProjection(String selectedProjection) {
        this.selectedProjection = selectedProjection;
    }

    public List<String> getSelectedNamegraphs() {
        return selectedNamegraphs;
    }

    public void setSelectedNamegraphs(List<String> selectedNamegraphs) {
        this.selectedNamegraphs = selectedNamegraphs;
    }

}

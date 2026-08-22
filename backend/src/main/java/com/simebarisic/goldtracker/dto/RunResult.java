package com.simebarisic.goldtracker.dto;

import java.util.List;

public class RunResult {
    private int pagesChecked;
    private int productsFound;
    private List<ChangeDto> changes;

    public RunResult(int pagesChecked, int productsFound, List<ChangeDto> changes) {
        this.pagesChecked = pagesChecked;
        this.productsFound = productsFound;
        this.changes = changes;
    }

    public int getPagesChecked() { return pagesChecked; }
    public int getProductsFound() { return productsFound; }
    public List<ChangeDto> getChanges() { return changes; }
}

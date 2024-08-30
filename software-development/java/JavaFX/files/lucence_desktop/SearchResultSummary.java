package com.thomaswilde.lucene_desktop;

import java.util.Collection;

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.collections.FXCollections;

public class SearchResultSummary<T> {

    private final IntegerProperty totalItems = new SimpleIntegerProperty(this, "totalItems");

    private final IntegerProperty totalPages = new SimpleIntegerProperty(this, "totalPages");

    private final IntegerProperty currentPage = new SimpleIntegerProperty(this, "currentPage");

    private final SimpleListProperty<SearchResultWrapper<T>> data = new SimpleListProperty<>(this, "data", FXCollections.observableArrayList());


    public SearchResultSummary() {
    }


    public SimpleListProperty<SearchResultWrapper<T>> getData() {
        return data;
    }

    public void setData(Collection<SearchResultWrapper<T>> data) {
        this.data.setAll(data);
    }

    public final IntegerProperty currentPageProperty() {
       return currentPage;
    }
    public final int getCurrentPage() {
       return currentPage.get();
    }
    public final void setCurrentPage(int value) {
        currentPage.set(value);
    }


    public final IntegerProperty totalPagesProperty() {
       return totalPages;
    }
    public final int getTotalPages() {
       return totalPages.get();
    }
    public final void setTotalPages(int value) {
        totalPages.set(value);
    }


    public final IntegerProperty totalItemsProperty() {
       return totalItems;
    }
    public final int getTotalItems() {
       return totalItems.get();
    }
    public final void setTotalItems(int value) {
        totalItems.set(value);
    }

}

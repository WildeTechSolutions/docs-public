package com.thomaswilde.lucene_desktop;

import java.lang.reflect.InvocationTargetException;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class SearchResultWrapper<T> {


    private final DoubleProperty score = new SimpleDoubleProperty(this, "score");

    private final StringProperty highlightFragment = new SimpleStringProperty(this, "highlightFragment");

    private final ObjectProperty<T> data = new SimpleObjectProperty<>(this, "data");

    public SearchResultWrapper(){

    }
    public SearchResultWrapper(Class<T> clazz) {
        try {
            setData(clazz.getConstructor().newInstance());
        } catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException
                | NoSuchMethodException | SecurityException e) {
            e.printStackTrace();
        }
    }
    public SearchResultWrapper(T object){
        setData(object);
    }

    public final ObjectProperty<T> dataProperty() {
       return data;
    }
    public final T getData() {
       return data.get();
    }
    public final void setData(T value) {
        data.set(value);
    }


    public final StringProperty highlightFragmentProperty() {
       return highlightFragment;
    }
    public final String getHighlightFragment() {
       return highlightFragment.get();
    }
    public final void setHighlightFragment(String value) {
        highlightFragment.set(value);
    }


    public final DoubleProperty scoreProperty() {
       return score;
    }
    public final double getScore() {
       return score.get();
    }
    public final void setScore(double value) {
        score.set(value);
    }

}

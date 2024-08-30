package com.thomaswilde.intellijborderpane;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;

public class ToolBarPreferences {

    // viewModeProperty
    // viewModeProperty
    private final ObjectProperty<ToolWindowViewMode> viewMode = new SimpleObjectProperty<>(this, "viewMode", ToolWindowViewMode.DOCK_PINNED);

    // barPositionProperty
    private final ObjectProperty<BarPosition> barPosition = new SimpleObjectProperty<>(this, "barPosition", BarPosition.LEFT_TOP);

    // selectedProperty
    private final BooleanProperty selected = new SimpleBooleanProperty(this, "selected");

    // visibleProperty
    private final BooleanProperty visible = new SimpleBooleanProperty(this, "visible");

    public final BooleanProperty visibleProperty() {
       return visible;
    }
    public final boolean isVisible() {
       return visible.get();
    }
    public final void setVisible(boolean value) {
        visible.set(value);
    }


    public final BooleanProperty selectedProperty() {
       return selected;
    }
    public final boolean isSelected() {
       return selected.get();
    }
    public final void setSelected(boolean value) {
        selected.set(value);
    }


    public final ObjectProperty<BarPosition> barPositionProperty() {
       return barPosition;
    }
    public final BarPosition getBarPosition() {
       return barPosition.get();
    }
    public final void setBarPosition(BarPosition value) {
        barPosition.set(value);
    }


    public final ObjectProperty<ToolWindowViewMode> viewModeProperty() {
       return viewMode;
    }
    public final ToolWindowViewMode getViewMode() {
       return viewMode.get();
    }
    public final void setViewMode(ToolWindowViewMode value) {
        viewMode.set(value);
    }


}

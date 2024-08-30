package com.thomaswilde.wildebeans;

import com.thomaswilde.foenix_utils.JFXDoubleTextField;
import com.thomaswilde.foenix_utils.JFXIntTextField;
import com.thomaswilde.icons.GlyphButton;
import com.thomaswilde.wildebeans.application.WildeDBApplication;
import com.thomaswilde.wildebeans.interfaces.Fillable;
import com.thomaswilde.wildebeans.interfaces.Suggestable;
import com.thomaswilde.wildebeans.ComboBoxConvertable;
import com.thomaswilde.wildebeans.customcontrols.*;

import com.thomaswilde.wildebeans.annotations.UiProperty;

import com.google.common.base.Strings;
import com.jfoenix.controls.*;
import com.jfoenix.skins.JFXDatePickerContent;
import com.thomaswilde.wildebeans.ui.dbsearch.DateRange;
import com.thomaswilde.wildebeans.ui.dbsearch.DateRangePicker;
import com.thomaswilde.wildebeans.ui.dbsearch.QueryBeanProperty;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.paint.Color;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.util.Callback;
import javafx.util.StringConverter;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Objects;

public class CustomPropertyEditorFactory implements Callback<UiBeanProperty, WildePropertyEditor<?>> {

    private static Logger log = LoggerFactory.getLogger(CustomPropertyEditorFactory.class);

//    static{
//        try {
//
//            // Get field instance
//            Field field = JFXDatePickerContent.class.getDeclaredField("DEFAULT_COLOR");
//            field.setAccessible(true); // Suppress Java language access checking
//
//            // Remove "final" modifier
//            Field modifiersField = Field.class.getDeclaredField("modifiers");
//            modifiersField.setAccessible(true);
//            modifiersField.setInt(field, field.getModifiers() & ~Modifier.FINAL);
//
//            // Get value
//            //            Boolean fieldValue = (Boolean) field.get(null);
//            //            
//
//            // Set value
//            field.set(null, Color.GRAY);
//        } catch (NoSuchFieldException e) {
//            e.printStackTrace();
//        } catch (IllegalAccessException e) {
//            e.printStackTrace();
//        }
//    }
    // Set appframe callback to database calls
    // set whether to bind the nodes or node

    private boolean bindBidirectional;
    private boolean useFoenix;
    private boolean editable;
    private boolean floatLabel = true;

    public CustomPropertyEditorFactory(boolean bindBidirectional, boolean useFoenix, boolean editable){

        this.bindBidirectional = bindBidirectional;
        this.useFoenix = useFoenix;
        this.editable = editable;
    }

    public void setUseFoenix(boolean useFoenix) {
        this.useFoenix = useFoenix;
    }

    public boolean isUseFoenix() {
        return useFoenix;
    }

    public void setEditable(boolean editable) {
        this.editable = editable;
    }

    public boolean isEditable() {
        return editable;
    }

    public boolean isEditable(UiBeanProperty<?> uiProperty){
        return uiProperty.isEditable() && uiProperty.isEverEditable();
    }

    public boolean isFloatLabel() {
        return floatLabel;
    }

    public void setFloatLabel(boolean floatLabel) {
        this.floatLabel = floatLabel;
    }

    public WildePropertyEditor<?> call(ToggleGroup toggleGroup, WildeBeanProperty propertyItem) {
        WildeBeanProperty item = (WildeBeanProperty) propertyItem;
//        Class<?> type = item.getType();
        UiProperty uiProperty = item.getUiProperty();

        return createBooleanToggleEditor(item, toggleGroup, null, isEditable(propertyItem));
    }

    public WildePropertyEditor<?> call(Node node, WildeBeanProperty propertyItem) {

        WildeBeanProperty item = (WildeBeanProperty) propertyItem;
        Class<?> type = item.getType();
        UiProperty uiProperty = item.getUiProperty();
        log.trace("Creating node for property: {}, node type: {}", propertyItem.getFieldName(), node.getClass());
        // JFoenix
//        if(node instanceof JFXTextArea){
//            return createJFXTextAreaEditor(propertyItem, (JFXTextArea) node, isEditable(propertyItem));
//        }
//        if(node instanceof JFXTextField){
//            if(type == int.class || type == Integer.class){
//                return createJFXSpinnerIntegerEditor(item, (JFXIntTextField) node, false);
//            }else if(type == double.class || type == Double.class){
//                return createJFXSpinnerDoubleEditor(item, (JFXDoubleTextField) node, false);
//            }else{
//                System.out.println("returning createJFXTextEditor for property: " + item.getFieldName());
////                return createJFXTextEditor(item, (JFXTextField) node, isEditable(uiProperty));
//            }
//        }
//        if(node instanceof JFXDatePicker){
//            return createLocalDateEditor(item, (JFXDatePicker) node, isEditable(propertyItem));
//        }


        if(node instanceof TextArea){
            return createTextAreaEditor(propertyItem, (TextArea) node, isEditable(propertyItem));
        }
        if(node instanceof TextField){
            log.trace("Node was an instance of TextField");
            if(type == int.class || type == Integer.class){
                return createTextEditorForInteger(item, (TextField) node);
            }else if(type == double.class || type == Double.class){
                return createTextEditorForDouble(item, (TextField) node);
            }else if(type == LocalDateTime.class){
                return createTextEditorForLocalDateTime(item, (TextField) node);
            }
            else{
                return createTextEditor(item, (TextField) node, isEditable(propertyItem));
            }
        }
        if(node instanceof ComboBox<?> && !Strings.isNullOrEmpty(uiProperty.preferredEditor()) && uiProperty.preferredEditor().equals("ComboBox")){
//        	System.out.println("Creating combobox, useJFoenix is set to " + useFoenix);
            log.debug("Creating property editor combobox for custom node");
            if(!uiProperty.editableComboBox()){
                return createComboBoxEditor(item, (ComboBox) node, FXCollections.observableArrayList(WildeDBApplication.getInstance().getComboBoxValues(item, null)), false, editable);
            }

            if(uiProperty.cachedList()){
                return createComboBoxEditor(item, (ComboBox) node, FXCollections.observableArrayList(WildeDBApplication.getInstance().getComboBoxValues(item, null)), true, editable);
            }

            UIUtil.ComboBoxGetValuesCallback comboBoxGetValuesCallback = null;
            // We should utilize some mechanism in the annotation to know what database method should be called
            comboBoxGetValuesCallback = (entry) -> FXCollections.observableArrayList(WildeDBApplication.getInstance().getComboBoxValues(item, entry));
            return createComboBoxEditor(item, (ComboBox<?>) node, comboBoxGetValuesCallback, editable);
//            return createCustomComboBoxForStringProperty(node, item, false);
        }
        if (node instanceof ComboBox<?>) {
            if (Suggestable.class.isAssignableFrom(type)) {
                Suggestable<?> suggestable = (Suggestable<?>) item.getValue();
                return createComboBoxEditor(item, (ComboBox<?>) node, (entry) -> {
                    try {
                        return suggestable.getSuggestedEntries(item, entry, null);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    return FXCollections.observableArrayList();
                }, isEditable(propertyItem));
            }

            if (Fillable.class.isAssignableFrom(type)) {
                Fillable<?> fillable = (Fillable<?>) item.getValue();
                return createComboBoxEditor(item, (ComboBox) node, fillable.getEntries(), false, isEditable(propertyItem));
            }
        }
        if (node instanceof Spinner<?>) {
            if(type == int.class || type == Integer.class){
                return createSpinnerIntegerEditor(item, (Spinner<Integer>) node, isEditable(propertyItem));
            }

            if(type == double.class || type == Double.class){
                return createSpinnerDoubleEditor(item, (Spinner<Double>) node, isEditable(propertyItem));
            }
        }
        if(node instanceof DatePicker){
            return createLocalDateEditor(item, (DatePicker) node, isEditable(propertyItem));
        }

//        if(node instanceof JFXComboBox<?> && !Strings.isNullOrEmpty(uiProperty.preferredEditor()) && uiProperty.preferredEditor().equals("ComboBox")){
//
//        }
//        if (node instanceof JFXComboBox<?>) {
//            if (Suggestable.class.isAssignableFrom(type)) {
//                Suggestable<?> suggestable = (Suggestable<?>) item.getValue();
//                return createComboBoxEditor(item, (JFXComboBox<?>) node, (entry) -> suggestable.getSuggestedEntries(item, entry, null), isEditable(propertyItem));
//            }
//
//            if (Fillable.class.isAssignableFrom(type)) {
//                Fillable<?> fillable = (Fillable<?>) item.getValue();
//                return createComboBoxEditor(item, (JFXComboBox) node, fillable.getEntries(), isEditable(propertyItem));
//            }
//        }


        return null;
    }

    @Override public WildePropertyEditor<?> call(UiBeanProperty item) {

//        UiBeanProperty item = (UiBeanProperty) propertyItem;
        Class<?> type = item.getType();
        UiProperty uiProperty = item.getUiProperty();

        log.trace("Creating editor for field: {}, type: {}", item.getFieldName(), item.getType());
//        
        if(!useFoenix) {

            if (uiProperty != null && !Strings.isNullOrEmpty(uiProperty.preferredEditor())) {
                switch (uiProperty.preferredEditor()) {
                    case "DirectoryChooser":
                        return createTextEditorFileChooser(item, isEditable(item), "directory");
                    case "FileChooser":
                        return createTextEditorFileChooser(item, isEditable(item), "file");
                    case "TextArea":
//                        
                        return createTextAreaEditor(item, isEditable(item));
                    case "TextField":
                        if(type == int.class || type == Integer.class){
//                            
                            return createTextEditorForInteger(item);
                        }else if(type == double.class || type == Double.class){
//                            
                            return createTextEditorForDouble(item);
                        }else if(type == LocalDateTime.class){
                            return createTextEditorForLocalDateTime(item);
                        }else{
                            return createTextEditor(item, false);
                        }
                    case "ComboBox":

                        if(!uiProperty.editableComboBox()){
                            return createComboBoxEditor(item, FXCollections.observableArrayList(WildeDBApplication.getInstance().getComboBoxValues(item, null)), false, editable);
                        }

                        if(uiProperty.cachedList()){
                            return createComboBoxEditor(item, FXCollections.observableArrayList(WildeDBApplication.getInstance().getComboBoxValues(item, null)), true, editable);
                        }

                        UIUtil.ComboBoxGetValuesCallback comboBoxGetValuesCallback = null;
                        // We should utilize some mechanism in the annotation to know what database method should be called
                        comboBoxGetValuesCallback = (entry) -> FXCollections.observableArrayList(WildeDBApplication.getInstance().getComboBoxValues(item, entry));
                        return createComboBoxEditor(item, comboBoxGetValuesCallback, editable);
                    default:
                        
                        break;
                }
            }

            if (type == String.class) {
                return createTextEditor(item, isEditable(item));
            }

            if(type == int.class || type == Integer.class){
//                
                return createSpinnerIntegerEditor(item, isEditable(item));
            }

            if(type == double.class || type == Double.class){
//                
                return createSpinnerDoubleEditor(item, isEditable(item));
            }

//
            if (Suggestable.class.isAssignableFrom(type)) {
//                
                Suggestable<?> suggestable = (Suggestable<?>) item.getValue();
                if(suggestable == null){
                    try {
                        suggestable = (Suggestable<?>) type.getConstructor().newInstance();
                    } catch (InstantiationException e) {
                        e.printStackTrace();
                    } catch (IllegalAccessException e) {
                        e.printStackTrace();
                    } catch (InvocationTargetException e) {
                        e.printStackTrace();
                    } catch (NoSuchMethodException e) {
                        e.printStackTrace();
                    }
                }
                Suggestable<?> finalSuggestable = suggestable;
                return createComboBoxEditor(item, (entry) -> {
                    try {
                        return finalSuggestable.getSuggestedEntries(item, entry, null);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    return FXCollections.observableArrayList();
                }, isEditable(item));
            }

            if (Fillable.class.isAssignableFrom(type)) {
                Fillable<?> fillable = (Fillable<?>) item.getValue();
                if(fillable == null){
                    try {
                        fillable = (Fillable<?>) type.getConstructor().newInstance();
                    } catch (InstantiationException e) {
                        e.printStackTrace();
                    } catch (IllegalAccessException e) {
                        e.printStackTrace();
                    } catch (InvocationTargetException e) {
                        e.printStackTrace();
                    } catch (NoSuchMethodException e) {
                        e.printStackTrace();
                    }
                }
                return createComboBoxEditor(item, fillable.getEntries(), fillable.autoCompleteComboBox(), isEditable(item));
            }

            if (type == LocalDate.class) {
                if(item instanceof WildeBeanProperty<?>) {
                    return createLocalDateEditor(item, isEditable(item));
                }else if(item instanceof QueryBeanProperty<?>){
                    return createLocalDateRangeEditor(item, isEditable(item));
                }
            }

            if (type == Boolean.class) {

                boolean[] pauseListeners = {false};

                String[] trueFalseText = uiProperty.booleanDisplayType().split("/");

                // Switch statement to add support for additional editors like ToggleButton, right now only RadioButton
                switch (uiProperty.preferredEditor()) {
                    default:
//				RadioButton trueButton = useFoenix ? new JFXRadioButton() : new RadioButton();
//				RadioButton falseButton = useFoenix ? new JFXRadioButton() : new RadioButton();

                        ObjectProperty<Boolean> booleanProperty = new SimpleObjectProperty<>();

                        CheckBox trueButton = new CheckBox();
                        CheckBox falseButton = new CheckBox();

                        // Add listeners to CheckBoxes
                        trueButton.selectedProperty().addListener((obs, wasSelected, isSelected) -> {
                            if (pauseListeners[0]) return;
                            pauseListeners[0] = true;

                            if (isSelected) {
                                booleanProperty.set(true);
                                falseButton.setSelected(false);
                            } else {
                                booleanProperty.set(null);
                            }

                            pauseListeners[0] = false;
                        });

                        falseButton.selectedProperty().addListener((obs, wasSelected, isSelected) -> {
                            if (pauseListeners[0]) return;
                            pauseListeners[0] = true;

                            if (isSelected) {
                                booleanProperty.set(false);
                                trueButton.setSelected(false);
                            } else {
                                booleanProperty.set(null);
                            }

                            pauseListeners[0] = false;
                        });

                        // Add listener to booleanProperty
                        booleanProperty.addListener((obs, oldValue, newValue) -> {
                            if (pauseListeners[0]) return;
                            pauseListeners[0] = true;

                            if (newValue == null) {
                                trueButton.setSelected(false);
                                falseButton.setSelected(false);
                            } else if (newValue) {
                                trueButton.setSelected(true);
                                falseButton.setSelected(false);
                            } else {
                                trueButton.setSelected(false);
                                falseButton.setSelected(true);
                            }

                            pauseListeners[0] = false;
                        });


                        trueButton.setUserData("true");
                        falseButton.setUserData("false");
                        if (trueFalseText.length == 2) {
                            trueButton.setText(trueFalseText[0]);
                            falseButton.setText(trueFalseText[1]);
                        }else{
                            trueButton.setText("Yes");
                            falseButton.setText("No");
                        }

                        HBox hBox = new HBox();
                        hBox.setAlignment(Pos.CENTER);
                        hBox.getChildren().addAll(trueButton, falseButton);
                        HBox.setMargin(falseButton, new Insets(0,0,0,10));

                        if(useFoenix) {
                            Label titleLabel = new Label(item.getName());
                            hBox.getChildren().add(0, titleLabel);
                            hBox.setAlignment(Pos.CENTER_LEFT);
                            HBox.setMargin(titleLabel, new Insets(0, 5, 0, 0));
                        }

                        return createBooleanNullableToggleEditor(item, booleanProperty, hBox, isEditable(item));
                }
            }
            
            else if (type == boolean.class || type == Boolean.class) {
                String[] trueFalseText = uiProperty.booleanDisplayType().split("/");

                // Switch statement to add support for additional editors like ToggleButton, right now only RadioButton
                switch (uiProperty.preferredEditor()) {
                    default:                    	
                        RadioButton trueButton = new RadioButton();
                        RadioButton falseButton = new RadioButton();
                        trueButton.setUserData("true");
                        falseButton.setUserData("false");
                        if (trueFalseText.length == 2) {
                            trueButton.setText(trueFalseText[0]);
                            falseButton.setText(trueFalseText[1]);
                        }else{
                            trueButton.setText("Yes");
                            falseButton.setText("No");
                        }
                        ToggleGroup toggleGroup = new ToggleGroup();
                        trueButton.setToggleGroup(toggleGroup);
                        falseButton.setToggleGroup(toggleGroup);

                        HBox hBox = new HBox();
                        hBox.setAlignment(Pos.CENTER);
                        hBox.getChildren().addAll(trueButton, falseButton);
                        HBox.setMargin(falseButton, new Insets(0,0,0,10));
                        return createBooleanToggleEditor(item, toggleGroup, hBox, isEditable(item));
                }
            }

        }else{

            if (!Strings.isNullOrEmpty(uiProperty.preferredEditor())) {
                switch (uiProperty.preferredEditor()) {
                    case "TextArea":
                        return createJFXTextAreaEditor(item, isEditable(item));
                    case "TextField":
//                        
//                        
                        if(type == int.class || type == Integer.class){
                            return createJFXSpinnerIntegerEditor(item, editable);
                        }else if(type == double.class || type == Double.class){
                            return createJFXSpinnerDoubleEditor(item, editable);
                        }else{
                            return createJFXTextEditor(item, false);
                        }
                    case "ComboBox":
                        UIUtil.ComboBoxGetValuesCallback comboBoxGetValuesCallback = null;
//                        switch (item.getFieldName()) {
//                            case "missionArea":
//                                comboBoxGetValuesCallback = (entry, task) -> FXCollections.observableArrayList(DBTools.getInstance().getSuggestedMissionAreas(entry));
//                                break;
//                            case "majorProgram":
//                                comboBoxGetValuesCallback = (entry, task) -> FXCollections.observableArrayList(DBTools.getInstance().getSuggestedMajorPrograms(entry));
//                                break;
//                            case "sec":
//                                return createJFXComboBoxEditor(item, FXCollections.observableArrayList(Arrays.asList("TS", "FA", "DA", "CT")), editable);
//                        }
                        return createJFXComboBoxEditor(item, comboBoxGetValuesCallback, editable);
                    default:

                        break;
                }
            }

            if (type == String.class) {
                return createJFXTextEditor(item, isEditable(item));
            }

            if(type == int.class || type == Integer.class){
                return createJFXSpinnerIntegerEditor(item, isEditable(item));
            }

            if(type == double.class || type == Double.class){
                return createJFXSpinnerDoubleEditor(item, isEditable(item));
            }

            if (Suggestable.class.isAssignableFrom(type)) {
                Suggestable<?> suggestable = (Suggestable<?>) item.getValue();
                if(suggestable == null){
                    try {
                        suggestable = (Suggestable<?>) type.getConstructor().newInstance();
                    } catch (InstantiationException e) {
                        e.printStackTrace();
                    } catch (IllegalAccessException e) {
                        e.printStackTrace();
                    } catch (InvocationTargetException e) {
                        e.printStackTrace();
                    } catch (NoSuchMethodException e) {
                        e.printStackTrace();
                    }
                }
                Suggestable<?> finalSuggestable = suggestable;
                return createJFXComboBoxEditor(item, (entry) -> {
                    try {
                        return finalSuggestable.getSuggestedEntries(item, entry, null);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    return FXCollections.observableArrayList();
                }, isEditable(item));
            }

            if (Fillable.class.isAssignableFrom(type)) {
                Fillable<?> fillable = (Fillable<?>) item.getValue();
                if(fillable == null){
                    try {
                        fillable = (Fillable<?>) type.getConstructor().newInstance();
                    } catch (InstantiationException e) {
                        e.printStackTrace();
                    } catch (IllegalAccessException e) {
                        e.printStackTrace();
                    } catch (InvocationTargetException e) {
                        e.printStackTrace();
                    } catch (NoSuchMethodException e) {
                        e.printStackTrace();
                    }
                }
                return createJFXComboBoxEditor(item, fillable.getEntries(), isEditable(item));
            }

            if (type == LocalDate.class) {
                return createJFXLocalDateEditor(item, isEditable(item));
            }
            
            if (type == boolean.class || type == Boolean.class) {
                String[] trueFalseText = uiProperty.booleanDisplayType().split("/");

                // Switch statement to add support for additional editors like ToggleButton, right now only RadioButton
                switch (uiProperty.preferredEditor()) {
                    default:
                        RadioButton trueButton = new RadioButton();
                        RadioButton falseButton = new RadioButton();
                        trueButton.setUserData("true");
                        falseButton.setUserData("false");
                        if (trueFalseText.length == 2) {
                            trueButton.setText(trueFalseText[0]);
                            falseButton.setText(trueFalseText[1]);
                        }else{
                            trueButton.setText("Yes");
                            falseButton.setText("No");
                        }
                        ToggleGroup toggleGroup = new ToggleGroup();
                        trueButton.setToggleGroup(toggleGroup);
                        falseButton.setToggleGroup(toggleGroup);

                        HBox hBox = new HBox(10);
                        hBox.setAlignment(Pos.CENTER);
                        Label label = new Label(item.getName());
                        hBox.getChildren().addAll(label, trueButton, falseButton);
//                        HBox.setMargin(falseButton, new Insets(0,0,0,10));
                        return createBooleanToggleEditor(item, toggleGroup, hBox, isEditable(item));
                }
            }
        }

        return null;
    }

    public final WildePropertyEditor<?> createTextEditor(UiBeanProperty property, boolean editable) {
        return createTextEditor(property, new TextField(), editable);
    }

    public final WildePropertyEditor<?> createTextEditor(UiBeanProperty property, TextField textField, boolean editable) {

        return new WildeAbstractPropertyEditor<String, TextField>(property, textField, bindBidirectional) {

            {
                UiProperty uiProperty = ((UiBeanProperty) property).getUiProperty();
                getEditor().setEditable(editable && property.isEverEditable());
                if (uiProperty.charLimit() > 0) {
                    UIUtil.setMaximumLimitToTextField(getEditor(), uiProperty.charLimit());
                }

            }

            @Override public StringProperty getObservableValue() {
                return getEditor().textProperty();
            }

            @Override public void setValue(String value) {
                getEditor().setText(value);
            }

            @Override
            public void setEditable(boolean editable) {
                UiProperty uiProperty = ((UiBeanProperty) property).getUiProperty();
                getEditor().setEditable(editable && property.isEverEditable());
            }
            
            @Override
			public void selectAll() {
            	Platform.runLater(() -> {
            		getEditor().requestFocus();
    				getEditor().selectAll();	
            	});							
			}
        };
    }

    public final WildePropertyEditor<?> createTextEditorForInteger(UiBeanProperty property) {
        return createTextEditorForInteger(property, new TextField());
    }

    public final WildePropertyEditor<?> createTextEditorForInteger(UiBeanProperty property, TextField textField) {

        return new WildeAbstractPropertyEditor<Integer, TextField>(property, textField, false) {

            {

                IntegerProperty valueProperty = new SimpleIntegerProperty();
                valueProperty.bind(Bindings.createIntegerBinding(() -> {
                    try {
                        return Integer.parseInt(getEditor().getText());
                    } catch (NumberFormatException e) {
                        return 0;
                    }
                }, getEditor().textProperty()));
                getEditor().setUserData(valueProperty);


            }

            @Override
            public ObservableValue<Integer> getObservableValue() {

                return (ObservableValue<Integer>) getEditor().getUserData();
            }

            @Override public void setValue(Integer value) {

                getEditor().setText(Integer.toString(value));
            }

            @Override
            public void setEditable(boolean editable) {
                getEditor().setEditable(editable && property.isEverEditable());
            }
            
            @Override
			public void selectAll() {
            	Platform.runLater(() -> {
            		getEditor().requestFocus();
    				getEditor().selectAll();	
            	});							
			}
        };
    }

    public final WildePropertyEditor<?> createTextEditorForDouble(UiBeanProperty property) {

        return createTextEditorForDouble(property, new TextField());
    }

    public final WildePropertyEditor<?> createTextEditorForDouble(UiBeanProperty property, TextField textField) {

        return new WildeAbstractPropertyEditor<Double, TextField>(property, textField, false) {

            {

                DoubleProperty valueProperty = new SimpleDoubleProperty();
                valueProperty.bind(Bindings.createDoubleBinding(() -> {
                    try {
                        return Double.parseDouble(getEditor().getText());
                    } catch (NumberFormatException e) {
                        return 0.0;
                    }
                }, getEditor().textProperty()));
                getEditor().setUserData(valueProperty);
            }

            @Override
            public ObservableValue<Double> getObservableValue() {
                return (ObservableValue<Double>) getEditor().getUserData();
            }

            @Override public void setValue(Double value) {
                getEditor().setText(Double.toString(value));
            }

            @Override
            public void setEditable(boolean editable) {
                getEditor().setEditable(editable && property.isEverEditable());
            }
            
            @Override
			public void selectAll() {
            	Platform.runLater(() -> {
            		getEditor().requestFocus();
    				getEditor().selectAll();	
            	});							
			}
        };
    }

    public final WildePropertyEditor<?> createTextEditorForLocalDateTime(UiBeanProperty property) {

        return createTextEditorForLocalDateTime(property, new TextField());
    }

    public final WildePropertyEditor<?> createTextEditorForLocalDateTime(UiBeanProperty property, TextField textField) {

        DateTimeFormatter df = DateTimeFormatter.ofPattern("MM/dd/yyyy hh:mm a");

        return new WildeAbstractPropertyEditor<LocalDateTime, TextField>(property, textField, false) {

            {



                ObjectProperty<LocalDateTime> valueProperty = new SimpleObjectProperty<>();
                valueProperty.bind(Bindings.createObjectBinding(() -> {
                    if(Strings.isNullOrEmpty(getEditor().getText())) return null;

                    try {
                        return LocalDateTime.parse(getEditor().getText(), df);
                    } catch (Exception e) {
                        return null;
                    }
                }, getEditor().textProperty()));
                getEditor().setUserData(valueProperty);



            }

            @Override
            public ObservableValue<LocalDateTime> getObservableValue() {
                return (ObservableValue<LocalDateTime>) property.getObservableValue();
            }

            @Override public void setValue(LocalDateTime value) {
                log.trace("LocalDateTime text editor value set: {}", value);
                if(value == null){
                    getEditor().setText("");
                }else{
                    getEditor().setText(df.format(value));
                }

            }

            @Override
            public void setEditable(boolean editable) {
                getEditor().setEditable(editable && property.isEverEditable());
            }

            @Override
            public void selectAll() {
                Platform.runLater(() -> {
                    getEditor().requestFocus();
                    getEditor().selectAll();
                });
            }
        };
    }

    public final WildePropertyEditor<?> createTextAreaEditor(UiBeanProperty property, boolean editable) {
        return createTextAreaEditor(property, new TextArea(), editable);
    }

    public final WildePropertyEditor<?> createTextAreaEditor(UiBeanProperty property, TextArea textArea, boolean editable) {

        return new WildeAbstractPropertyEditor<String, TextArea>(property, textArea, bindBidirectional) {

            {
                UiProperty uiProperty = ((UiBeanProperty) property).getUiProperty();
                getEditor().setEditable(editable && property.isEverEditable());
                getEditor().setWrapText(true);
                UIUtil.setDynamicTextArea(getEditor());
                if (uiProperty.charLimit() > 0) {
                    UIUtil.setMaximumLimitToTextField(getEditor(), uiProperty.charLimit());
                }

                // To eliminate blurryness per https://stackoverflow.com/questions/23728517/blurred-text-in-javafx-textarea
                getEditor().setCache(false);
                Platform.runLater(() -> {
                    try {
                        ScrollPane sp = (ScrollPane) getEditor().getChildrenUnmodifiable().get(0);
                        sp.setCache(false);
                        for (Node n : sp.getChildrenUnmodifiable()) {
                            n.setCache(false);
                        }
                    } catch (Exception e) {
                    }
                });

            }

            @Override public StringProperty getObservableValue() {
                return getEditor().textProperty();
            }

            @Override public void setValue(String value) {
                getEditor().setText(value);
            }

            @Override
            public void setEditable(boolean editable) {
                getEditor().setEditable(editable && property.isEverEditable());
            }
            
            @Override
			public void selectAll() {
            	Platform.runLater(() -> {
            		getEditor().requestFocus();
    				getEditor().selectAll();	
            	});							
			}
        };
    }

    public final WildePropertyEditor<?> createJFXTextEditor(UiBeanProperty property, boolean editable) {

        return new WildeAbstractPropertyEditor<String, JFXTextField>(property, new JFXTextField(), bindBidirectional) {

            {
                UiProperty uiProperty = ((UiBeanProperty) property).getUiProperty();
                getEditor().setEditable(editable && property.isEverEditable());
                getEditor().getStylesheets().add(Application.getUserAgentStylesheet());
                if (uiProperty.charLimit() > 0) {
                    UIUtil.setMaximumLimitToTextField(getEditor(), uiProperty.charLimit());
                }
                getEditor().getStylesheets().add(Application.getUserAgentStylesheet());

            }

            @Override public StringProperty getObservableValue() {
                return getEditor().textProperty();
            }

            @Override public void setValue(String value) {
                getEditor().setText(value);
            }

            @Override
            public void setEditable(boolean editable) {
                UiProperty uiProperty = ((UiBeanProperty) property).getUiProperty();
                getEditor().setEditable(editable && property.isEverEditable());
            }

            @Override
			public void selectAll() {
            	Platform.runLater(() -> {
            		getEditor().requestFocus();
    				getEditor().selectAll();	
            	});							
			}

        };
    }

//    public final WildePropertyEditor<?> createJFXTextEditorForInteger(UiBeanProperty property) {
//
//        return new WildeAbstractPropertyEditor<Integer, JFXTextField>(property, new JFXTextField(), false) {
//
//            {
//                getEditor().setEditable(false);
//                getEditor().getStylesheets().add(Application.getUserAgentStylesheet());
//            }
//
//            @Override
//            public ObservableValue<Integer> getObservableValue() {
//                return null;
//            }
//
//            @Override public void setValue(Integer value) {
//                getEditor().setText(Integer.toString(value));
//            }
//
//            @Override
//            public void setEditable(boolean editable) {
//                UiProperty uiProperty = ((UiBeanProperty) property).getUiProperty();
//                getEditor().setEditable(editable && property.isEverEditable()());
//            }
//        };
//    }
//
//    public final WildePropertyEditor<?> createJFXTextEditorForDouble(UiBeanProperty property) {
//
//        return new WildeAbstractPropertyEditor<Double, JFXTextField>(property, new JFXTextField(), false) {
//
//            {
//                getEditor().setEditable(false);
//                getEditor().getStylesheets().add(Application.getUserAgentStylesheet());
//            }
//
//            @Override
//            public ObservableValue<Double> getObservableValue() {
//                return null;
//            }
//
//            @Override public void setValue(Double value) {
//                getEditor().setText(Double.toString(value));
//            }
//
//            @Override
//            public void setEditable(boolean editable) {
//                UiProperty uiProperty = ((UiBeanProperty) property).getUiProperty();
//                getEditor().setEditable(editable && property.isEverEditable()());
//            }
//        };
//    }

    public final WildePropertyEditor<?> createJFXTextAreaEditor(UiBeanProperty property, boolean editable) {

        return new WildeAbstractPropertyEditor<String, JFXTextArea>(property, new JFXTextArea(), bindBidirectional) {

            {
                UiProperty uiProperty = ((UiBeanProperty) property).getUiProperty();
                getEditor().setEditable(editable && property.isEverEditable());
                UIUtil.setDynamicTextArea(getEditor());
                if (uiProperty.charLimit() > 0) {
                    UIUtil.setMaximumLimitToTextField(getEditor(), uiProperty.charLimit());
                }
                getEditor().getStylesheets().add(Application.getUserAgentStylesheet());
            }

            @Override public StringProperty getObservableValue() {
                return getEditor().textProperty();
            }

            @Override public void setValue(String value) {
                getEditor().setText(value);
            }

            @Override
            public void setEditable(boolean editable) {
                UiProperty uiProperty = ((UiBeanProperty) property).getUiProperty();
                getEditor().setEditable(editable && property.isEverEditable());
            }

            @Override
			public void selectAll() {
            	Platform.runLater(() -> {
            		getEditor().requestFocus();
    				getEditor().selectAll();	
            	});							
			}

        };
    }

    public final WildePropertyEditor<?> createLocalDateRangeEditor(UiBeanProperty property, boolean editable) {
        return new WildeAbstractPropertyEditor<DateRange, DateRangePicker>(property, new DateRangePicker(), bindBidirectional) {

            @Override
            public void setValue(DateRange dateRange) {
                getEditor().setDateRange(dateRange);
            }

            @Override
            public void setEditable(boolean editable) {
                getEditor().setEditable(editable);
            }

            @Override
            public void selectAll() {

            }

            @Override
            public ObservableValue<DateRange> getObservableValue() {
                return getEditor().dateRangeProperty();
            }
        };
    }

    public final WildePropertyEditor<?> createLocalDateEditor(UiBeanProperty property, boolean editable) {

        return createLocalDateEditor(property, new DatePicker(), editable);
    }

    public final WildePropertyEditor<?> createLocalDateEditor(UiBeanProperty property, DatePicker datePicker, boolean editable) {

        return new WildeAbstractPropertyEditor<LocalDate, DatePicker>(property, datePicker, bindBidirectional) {

            {
                UiProperty uiProperty = ((UiBeanProperty) property).getUiProperty();
                getEditor().setMouseTransparent(!(editable && property.isEverEditable()));
                
            }

            @Override
            public ObservableValue<LocalDate> getObservableValue() {
                return getEditor().valueProperty();
            }

            @Override
            public void setValue(LocalDate value) {
                getEditor().setValue(value);
            }

            @Override
            public void setEditable(boolean editable) {
                UiProperty uiProperty = ((UiBeanProperty) property).getUiProperty();
                getEditor().setMouseTransparent(!(editable && property.isEverEditable()));
                
            }
            
            @Override
			public void selectAll() {
            	Platform.runLater(() -> {
            		getEditor().getEditor().requestFocus();
    				getEditor().getEditor().selectAll();	
            	});							
			}

        };
    }

    public final WildePropertyEditor<?> createSpinnerIntegerEditor(UiBeanProperty property, boolean editable) {

        return createSpinnerIntegerEditor(property, new Spinner<>(), editable);
    }

    public final WildePropertyEditor<?> createSpinnerIntegerEditor(UiBeanProperty property, Spinner<Integer> spinner, boolean editable) {

        return new WildeAbstractPropertyEditor<Integer, Spinner<Integer>>(property, spinner, bindBidirectional) {

            {
                UiProperty uiProperty = ((UiBeanProperty) property).getUiProperty();
                getEditor().setMouseTransparent(!(editable && property.isEverEditable()));

                getEditor().setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0,100000000, 0,1));
                getEditor().setEditable(editable && property.isEverEditable());

                getEditor().focusedProperty().addListener((observable, oldValue, newValue) -> {
                if (!newValue) {
                    getEditor().increment(0); // won't change value, but will commit editor
                }
            });
            }

            @Override
            public ObservableValue<Integer> getObservableValue() {
                return getEditor().valueProperty();
            }

            @Override
            public void setValue(Integer t) {
                getEditor().getValueFactory().setValue(t);
            }

            @Override
            public void setEditable(boolean editable) {
                UiProperty uiProperty = ((UiBeanProperty) property).getUiProperty();
                getEditor().setMouseTransparent(!(editable && property.isEverEditable()));
                
                getEditor().setEditable(editable && property.isEverEditable());
            }
            
            @Override
			public void selectAll() {
            	Platform.runLater(() -> {
            		getEditor().getEditor().requestFocus();
    				getEditor().getEditor().selectAll();	
            	});							
			}
        };
    }

    public final WildePropertyEditor<?> createJFXSpinnerIntegerEditor(UiBeanProperty property, boolean editable) {

        return new WildeAbstractPropertyEditor<Integer, JFXIntTextField>(property, new JFXIntTextField(0, Integer.MAX_VALUE, 0), bindBidirectional) {

            {
                getEditor().setEditable((editable && property.isEverEditable()));
                getEditor().getStylesheets().add(Application.getUserAgentStylesheet());

            }

            @Override
            public ObservableValue<Integer> getObservableValue() {
                return getEditor().valueProperty().asObject();
            }

            @Override
            public void setValue(Integer t) {
                getEditor().setValue(t);
            }

            @Override
            public void setEditable(boolean editable) {
                getEditor().setEditable((editable && property.isEverEditable()));
            }
            
            @Override
			public void selectAll() {
            	Platform.runLater(() -> {
            		getEditor().requestFocus();
    				getEditor().selectAll();	
            	});							
			}
        };
    }

    public final WildePropertyEditor<?> createSpinnerDoubleEditor(UiBeanProperty property, boolean editable) {

        return createSpinnerDoubleEditor(property, new Spinner<>(), editable);
    }

    public final WildePropertyEditor<?> createSpinnerDoubleEditor(UiBeanProperty property, Spinner<Double> spinner, boolean editable) {

        return new WildeAbstractPropertyEditor<Double, Spinner<Double>>(property, spinner, bindBidirectional) {

            {
                UiProperty uiProperty = ((UiBeanProperty) property).getUiProperty();
                getEditor().setMouseTransparent(!(editable && property.isEverEditable()));
                
//                getEditor().getEditor().setEditable(true);
                getEditor().setValueFactory(new SpinnerValueFactory.DoubleSpinnerValueFactory(uiProperty.spinnerMin(),uiProperty.spinnerMax(), uiProperty.spinnerInitialValue(),uiProperty.spinnerStepBy()));

                getEditor().setEditable(editable && property.isEverEditable());

                getEditor().focusedProperty().addListener((observable, oldValue, newValue) -> {
                    if (!newValue) {
                        if(getEditor() != null) {
                            getEditor().increment(0); // won't change value, but will commit editor
                        }
                    }
                });
                
                StringConverter<Double> doubleConverter = new StringConverter<Double>() {
                	private final DecimalFormat df = new DecimalFormat(uiProperty.spinnerDecimalFormat());
                	@Override
                	public String toString(Double object) {
                	    if (object == null) {return "";}
                	    return df.format(object);}
                	@Override
                	public Double fromString(String string) {
                	    try {
                	        if (string == null) {return null;}
                	        string = string.trim();
                	        if (string.length() < 1) {return null;}     
                	        return df.parse(string).doubleValue();
                	    } catch (ParseException ex) {throw new RuntimeException(ex);}
                	    }
                	};
                	
                getEditor().getValueFactory().setConverter(doubleConverter);
            }

            @Override
            public ObservableValue<Double> getObservableValue() {
                return getEditor().valueProperty();
            }

            @Override
            public void setValue(Double t) {
                getEditor().getValueFactory().setValue(t);
            }

            @Override
            public void setEditable(boolean editable) {
                UiProperty uiProperty = ((UiBeanProperty) property).getUiProperty();
                getEditor().setMouseTransparent(!(editable && property.isEverEditable()));
                getEditor().setEditable(editable && property.isEverEditable());
            }
            
            @Override
			public void selectAll() {
            	Platform.runLater(() -> {
            		getEditor().getEditor().requestFocus();
    				getEditor().getEditor().selectAll();	
            	});							
			}
        };
    }

    public final WildePropertyEditor<?> createJFXSpinnerDoubleEditor(UiBeanProperty property, boolean editable) {

        return new WildeAbstractPropertyEditor<Double, JFXDoubleTextField>(property, new JFXDoubleTextField(0, Double.MAX_VALUE, 0), bindBidirectional) {

            {
//                getEditor().setEditable(false);
                getEditor().setEditable((editable && property.isEverEditable()));

                getEditor().getStylesheets().add(Application.getUserAgentStylesheet());
            }

            @Override
            public ObservableValue<Double> getObservableValue() {
                return getEditor().valueProperty().asObject();
            }

            @Override
            public void setValue(Double t) {
                getEditor().setValue(t);
            }

            @Override
            public void setEditable(boolean editable) {

//                getEditor().setMouseTransparent(!(editable && property.isEverEditable()));
                getEditor().setEditable(editable && property.isEverEditable());

//                getEditor().setEditable(false);
            }
            
            @Override
			public void selectAll() {
            	Platform.runLater(() -> {
            		getEditor().requestFocus();
    				getEditor().selectAll();	
            	});							
			}
        };
    }

    public final WildePropertyEditor<?> createJFXLocalDateEditor(UiBeanProperty property, boolean editable) {

        return new WildeAbstractPropertyEditor<LocalDate, JFXDatePicker>(property, new JFXDatePicker(), bindBidirectional) {

            {
                UiProperty uiProperty = ((UiBeanProperty) property).getUiProperty();
                getEditor().setMouseTransparent(!(editable && property.isEverEditable()));
//                getEditor().setEditable(editable && property.isEverEditable());
//                Button arrowButton = (Button) getEditor().lookup(".arrow-button");
//                arrowButton.setDisable(!(editable && property.isEverEditable()));

                getEditor().getStylesheets().add(Application.getUserAgentStylesheet());


                getEditor().setDayCellFactory(dp -> new DateCell() {

                    {
                        addEventHandler(MouseEvent.MOUSE_ENTERED, evt -> {
                            setStyle("-fx-text-fill: BLACK;");
                        });

                        addEventHandler(MouseEvent.MOUSE_EXITED, evt -> {
                            setStyle("-fx-text-fill: -fx-text-background-color;");
                        });
                    }

                    @Override
                    public void updateItem(LocalDate item, boolean empty) {
                        super.updateItem(item, empty);

                        if (LocalDate.now().isEqual(item)) {
                            setStyle("-fx-text-fill: -fx-primarycolor;");
                        }else{
                            setStyle("-fx-text-fill: -fx-text-background-color;");
                        }
                    }
                });
            }

            @Override
            public ObservableValue<LocalDate> getObservableValue() {
                return getEditor().valueProperty();
            }

            @Override
            public void setValue(LocalDate value) {
                getEditor().setValue(value);
            }

            @Override
            public void setEditable(boolean editable) {
                UiProperty uiProperty = ((UiBeanProperty) property).getUiProperty();
                getEditor().setMouseTransparent(!(editable && property.isEverEditable()));
                
            }
            
            @Override
			public void selectAll() {
            	Platform.runLater(() -> {
            		getEditor().getEditor().requestFocus();
    				getEditor().getEditor().selectAll();	
            	});							
			}

        };
    }

    public final <T> WildePropertyEditor<?> createComboBoxEditor(UiBeanProperty property, final UIUtil.ComboBoxGetValuesCallback comboBoxGetValuesCallback, boolean editable) {
        return createComboBoxEditor(property, new ComboBox<T>(), comboBoxGetValuesCallback, editable);
    }

    public final <T> WildePropertyEditor<?> createComboBoxEditor(UiBeanProperty property, ComboBox<T> comboBox, final UIUtil.ComboBoxGetValuesCallback comboBoxGetValuesCallback, boolean editable) {

        return new WildeAbstractPropertyEditor<T, ComboBox<T>>(property, comboBox, bindBidirectional) {

            {
                if(ComboBoxConvertable.class.isAssignableFrom(property.getType())) {
//                    
                    UIUtil.setComboBoxConverter(getEditor());

                }else{
                    
                }

                UIUtil.autoCompleteComboBoxPlus(getEditor(), null, null, comboBoxGetValuesCallback);

                UiProperty uiProperty = ((UiBeanProperty) property).getUiProperty();
                getEditor().setMouseTransparent(!(editable && property.isEverEditable()));
                
            }

            @Override
            public ObservableValue<T> getObservableValue() {
                return getEditor().valueProperty();
            }

            @Override
            public void setValue(T value) {
                getEditor().setValue(value);
                if(value != null){
                    getEditor().setTooltip(new Tooltip(value.toString()));
                }else{
                    getEditor().setTooltip(null);
                }
            }

            @Override
            public void setEditable(boolean editable) {
                UiProperty uiProperty = ((UiBeanProperty) property).getUiProperty();
                getEditor().setMouseTransparent(!(editable && property.isEverEditable()));
                
            }
            
            @Override
			public void selectAll() {
            	Platform.runLater(() -> {
            		getEditor().getEditor().requestFocus();
    				getEditor().getEditor().selectAll();	
            	});							
			}

        };
    }

    public final <T> WildePropertyEditor<?> createComboBoxEditor(UiBeanProperty property, final ObservableList<T> choices, boolean autocomplete, boolean editable) {

        return createComboBoxEditor(property, new ComboBox<>(), choices, autocomplete, editable);
    }

    public final <T> WildePropertyEditor<?> createComboBoxEditor(UiBeanProperty property, ComboBox<T> comboBox, final ObservableList<T> choices, boolean autocomplete, boolean editable) {

        return new WildeAbstractPropertyEditor<T, ComboBox<T>>(property, comboBox, bindBidirectional) {

            {
                if(ComboBoxConvertable.class.isAssignableFrom(property.getType()))
                    UIUtil.setComboBoxConverter(getEditor());
                getEditor().getItems().setAll(choices);



                UiProperty uiProperty = ((UiBeanProperty) property).getUiProperty();
                getEditor().setMouseTransparent(!(editable && property.isEverEditable()));

                if(autocomplete){
                    UIUtil.autoCompleteComboBoxPlus(getEditor(), new UIUtil.AutoCompleteComparator<T>() {
                        @Override
                        public boolean matches(String typedText, T objectToCompare) {
                            if(objectToCompare instanceof String){
                                return ((String) objectToCompare).toLowerCase().contains(typedText.toLowerCase());
                            }else if(objectToCompare instanceof ComboBoxConvertable){
                                return ((ComboBoxConvertable)objectToCompare).comboBoxCellFactoryText().toLowerCase().contains(typedText.toLowerCase());
                            }else{
                                return objectToCompare.toString().toLowerCase().contains(typedText.toLowerCase());
                            }

                        }
                    });
                }

                
            }

            @Override
            public ObservableValue<T> getObservableValue() {
                return getEditor().valueProperty();
            }

            @Override
            public void setValue(T value) {
                getEditor().setValue(value);
                if(value != null){
                    getEditor().setTooltip(new Tooltip(value.toString()));
                }else{
                    getEditor().setTooltip(null);
                }
            }

            @Override
            public void setEditable(boolean editable) {
                UiProperty uiProperty = ((UiBeanProperty) property).getUiProperty();
                getEditor().setMouseTransparent(!(editable && property.isEverEditable()));
                
            }
            
            @Override
			public void selectAll() {
            	Platform.runLater(() -> {
            		getEditor().getEditor().requestFocus();
    				getEditor().getEditor().selectAll();	
            	});							
			}

        };
    }

    public final <T> WildePropertyEditor<?> createJFXComboBoxEditor(UiBeanProperty property, final ObservableList<T> choices, boolean editable) {

        return new WildeAbstractPropertyEditor<T, JFXComboBox<T>>(property, new JFXComboBox<>(), bindBidirectional) {

            {
                if(ComboBoxConvertable.class.isAssignableFrom(property.getType()))
                    UIUtil.setComboBoxConverter(getEditor());


                getEditor().getItems().setAll(choices);

                UiProperty uiProperty = ((UiBeanProperty) property).getUiProperty();
                getEditor().setMouseTransparent(!(editable && property.isEverEditable()));
                getEditor().getStylesheets().add(Application.getUserAgentStylesheet());
                
            }

            @Override
            public ObservableValue<T> getObservableValue() {
                return getEditor().valueProperty();
            }

            @Override
            public void setValue(T value) {
                getEditor().setValue(value);
            }

            @Override
            public void setEditable(boolean editable) {
                UiProperty uiProperty = ((UiBeanProperty) property).getUiProperty();
                getEditor().setMouseTransparent(!(editable && property.isEverEditable()));
                
            }
            
            @Override
			public void selectAll() {
            	Platform.runLater(() -> {
            		getEditor().getEditor().requestFocus();
    				getEditor().getEditor().selectAll();	
            	});							
			}

        };
    }

    public final <T> WildePropertyEditor<?> createJFXComboBoxEditor(UiBeanProperty property, UIUtil.ComboBoxGetValuesCallback comboBoxGetValuesCallback, boolean editable) {

        return new WildeAbstractPropertyEditor<T, JFXComboBox<T>>(property, new JFXComboBox<>(), bindBidirectional) {

            {
                if(ComboBoxConvertable.class.isAssignableFrom(property.getType()))
                    UIUtil.setComboBoxConverter(getEditor());


                UIUtil.autoCompleteComboBoxPlus(getEditor(), null, null, comboBoxGetValuesCallback);

                UiProperty uiProperty = ((UiBeanProperty) property).getUiProperty();
                getEditor().setMouseTransparent(!(editable && property.isEverEditable()));
                getEditor().getStylesheets().add(Application.getUserAgentStylesheet());
            }

            @Override
            public ObservableValue<T> getObservableValue() {
                return getEditor().valueProperty();
            }

            @Override
            public void setValue(T value) {
                getEditor().setValue(value);
            }

            @Override
            public void setEditable(boolean editable) {
                UiProperty uiProperty = ((UiBeanProperty) property).getUiProperty();
                getEditor().setMouseTransparent(!(editable && property.isEverEditable()));
                
            }
            
            @Override
			public void selectAll() {
            	Platform.runLater(() -> {
            		getEditor().getEditor().requestFocus();
    				getEditor().getEditor().selectAll();	
            	});							
			}

        };
    }

    public final WildePropertyEditor<?> createBooleanNullableToggleEditor(UiBeanProperty property, ObjectProperty<Boolean> selectedProperty, Node toggleNode, boolean editable){
        return new WildeAbstractBooleanNullablePropertyEditor(property, selectedProperty, toggleNode, bindBidirectional) {

            @Override
            public void setValue(Boolean var1) {
                selectedProperty.setValue(var1);
            }

            @Override
            public void setEditable(boolean editable) {
                toggleNode.setDisable(!(editable && property.isEverEditable()));
                if(toggleNode instanceof Parent) {
                    ((Parent) toggleNode).getChildrenUnmodifiable().forEach(child -> child.setDisable(!(editable && property.isEverEditable())));
                }
            }

            @Override
            public void selectAll() {

            }

            @Override
            public ObservableValue<Boolean> getObservableValue() {
                return selectedProperty;
            }

        };
    }

//    public final WildePropertyEditor<?> createPathEditor(UiBeanProperty property, Parent parent, TextField textField, ObjectProperty<Path> editorPathProperty, boolean editable) {
//
//        return new WildeAbstractPropertyEditor<Path, TextField>(property, textField, bindBidirectional) {
//
//            {
//                UiProperty uiProperty = ((UiBeanProperty) property).getUiProperty();
//                getEditor().setEditable(editable && property.isEverEditable());
//                if (uiProperty.charLimit() > 0) {
//                    UIUtil.setMaximumLimitToTextField(getEditor(), uiProperty.charLimit());
//                }
//
//
//
//            }
//
//            @Override
//            public ObservableValue<Path> getObservableValue() {
//                return editorPathProperty;
//            }
//
//            @Override public void setValue(Path value) {
////                getEditor().setText(Objects.toString(value));
//                editorPathProperty.setValue(value);
//            }
//
//            @Override
//            public void setEditable(boolean editable) {
//                UiProperty uiProperty = ((UiBeanProperty) property).getUiProperty();
//                getEditor().setEditable(editable && property.isEverEditable());
//            }
//
//            @Override
//            public void selectAll() {
//                Platform.runLater(() -> {
//                    getEditor().requestFocus();
//                    getEditor().selectAll();
//                });
//            }
//        };
//    }

    public final WildePropertyEditor<?> createTextEditorFileChooser(UiBeanProperty property, boolean editable, String type) {



        TextField pathTextField = new TextField();

        Button directoryChooserButton = new GlyphButton(GlyphButton.EDIT);
        switch(type){
            case "directory":
                directoryChooserButton.setOnAction(event -> {
                    DirectoryChooser directoryChooser = new DirectoryChooser();
                    if(!Strings.isNullOrEmpty(pathTextField.getText()) && Files.exists(Paths.get(pathTextField.getText()))){
                        directoryChooser.setInitialDirectory(new File(pathTextField.getText()));
                    }
                    File file = directoryChooser.showDialog(directoryChooserButton.getScene().getWindow());
                    if(file != null){
                        pathTextField.setText(file.toString());
                    }
                });
                break;
            case "file":
                directoryChooserButton.setOnAction(event -> {
                    FileChooser directoryChooser = new FileChooser();
                    if(!Strings.isNullOrEmpty(pathTextField.getText()) && Files.exists(Paths.get(pathTextField.getText()))){
                        directoryChooser.setInitialDirectory(new File(pathTextField.getText()));
                    }
                    File file = directoryChooser.showOpenDialog(directoryChooserButton.getScene().getWindow());
                    if(file != null){
                        pathTextField.setText(file.toString());
                    }
                });
                break;
        }

        HBox hBox = new HBox(5, directoryChooserButton, pathTextField);
        HBox.setHgrow(pathTextField, Priority.ALWAYS);
        hBox.setAlignment(Pos.CENTER_LEFT);

        return new WildeAbstractPropertyEditor<String, HBox>(property, hBox, bindBidirectional) {

            {
                UiProperty uiProperty = ((UiBeanProperty) property).getUiProperty();
                pathTextField.setEditable(editable && property.isEverEditable());
                if (uiProperty.charLimit() > 0) {
                    UIUtil.setMaximumLimitToTextField(pathTextField, uiProperty.charLimit());
                }

            }

            @Override public StringProperty getObservableValue() {
                return pathTextField.textProperty();
            }

            @Override public void setValue(String value) {
                pathTextField.setText(value);
            }

            @Override
            public void setEditable(boolean editable) {
                UiProperty uiProperty = ((UiBeanProperty) property).getUiProperty();
                pathTextField.setEditable(editable && property.isEverEditable());
            }

            @Override
            public void selectAll() {
                Platform.runLater(() -> {
                    pathTextField.requestFocus();
                    pathTextField.selectAll();
                });
            }
        };
    }
    
    public final WildePropertyEditor<?> createBooleanToggleEditor(UiBeanProperty property, ToggleGroup toggleGroup, Node toggleNode, boolean editable){
        return new WildeAbstractBooleanPropertyEditor(property, toggleGroup, toggleNode, bindBidirectional) {
            @Override
            public boolean getValueFromUi() {
                for (Toggle toggle : toggleGroup.getToggles()) {
                    if (Objects.equals(toggle.getUserData(), "true")) {
                        return toggle.isSelected();
                    }
                }
                return false;
            }

            @Override
            public void setEditable(boolean editable) {
                for (Toggle toggle : toggleGroup.getToggles()) {
                    if(toggle instanceof Node){
                        ((Node) toggle).setDisable(!(editable && property.isEverEditable()));
                    }
                }
                if(toggleNode instanceof Pane){
                    ((Pane) toggleNode).getChildren().forEach(node -> node.setDisable(!(editable && property.isEverEditable())));
                }
            }

            @Override
            public void setValue(Boolean aBoolean) {
                for (Toggle toggle : toggleGroup.getToggles()) {
                    if (Objects.equals(toggle.getUserData(), "true")) {
                        toggle.setSelected(aBoolean);
                    }else{
                        toggle.setSelected(!aBoolean);
                    }
                }
            }
            
            @Override
			public ObservableValue<Boolean> getObservableValue() {
				for (Toggle toggle : toggleGroup.getToggles()) {
                    if (Objects.equals(toggle.getUserData(), "true")) {
                        return toggle.selectedProperty();
                    }
                }
				return null;
			}
            
            @Override
			public void selectAll() {
            							
			}
        };
    }
    
    public boolean isBindBidirectional() {
		return bindBidirectional;
	}

//    public final <T> WildePropertyEditor<?> createCheckComboBoxEditor(UiBeanProperty property, final Collection<T> choices) {
//
//        return new AbstractPropertyEditor<ObservableList<T>, CheckComboBox<T>>(property, new CheckComboBox<>()) {
//
//            private ListProperty<T> list;
//
//            {
//                getEditor().getItems().setAll(choices);
//            }
//
//            @Override
//            public ListProperty<T> getObservableValue() {
//                if (list == null) {
//                    list = new SimpleListProperty<>(getEditor().getCheckModel().getCheckedItems());
//                }
//                return list;
//            }
//
//            @Override
//            public void setValue(ObservableList<T> checked) {
//                checked.forEach(getEditor().getCheckModel()::check);
//            }
//        };
//    }
}

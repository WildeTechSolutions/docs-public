package com.thomaswilde.wildebeans;


import com.thomaswilde.wildebeans.WildePropertySheet3.CommitChangesParam;
import com.thomaswilde.wildebeans.WildePropertySheet3.WildePropertySheetBuilder;
import com.thomaswilde.wildebeans.application.WildeDBApplication;

import java.util.HashMap;
import java.util.Map;

import javafx.event.ActionEvent;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.stage.Stage;

public class WildePropertySheet3Alert<T> {

	private Alert alert;
	private WildePropertySheet3<T> wildePropertySheet;
	
	public WildePropertySheet3Alert() {
		
	}

	public WildePropertySheet3Alert(Alert alert, WildePropertySheet3<T> wildePropertySheet) {
		this.alert = alert;
		this.wildePropertySheet = wildePropertySheet;
	}

	public static<T> WildePropertySheet3Alert<T> createWildePropertySheetAlert(T bean, String headerText){
		return createWildePropertySheetAlert(bean, null, headerText, null, null, null, null);
	}

	public static<T> WildePropertySheet3Alert<T> createWildePropertySheetAlert(T bean, String headerText, String propertySheetName){
		return createWildePropertySheetAlert(bean, null, headerText, propertySheetName, null, null, null);
	}

	public static<T> WildePropertySheet3Alert<T> createWildePropertySheetAlert(T bean, String headerText, HashMap<String, UiPosition> uiPositions){
		return createWildePropertySheetAlert(bean, null, headerText, null, uiPositions, null, null);
	}

	public static<T> WildePropertySheet3Alert<T> createWildePropertySheetAlert(T bean, String headerText, WildePropertySheet3.WildeBeanValidationCallback<T> wildeBeanValidationCallback){
		return createWildePropertySheetAlert(bean, null, headerText, null, null, null, wildeBeanValidationCallback);
	}

	public static<T> WildePropertySheet3Alert<T> createWildePropertySheetAlert(T bean, String headerText, Map<Class<?>, WildeEditableBeanTable.WildeListBeanValidationCallback<?>> customValidators){
		return createWildePropertySheetAlert(bean, null, headerText, null, null, customValidators, null);
	}

	public static<T> WildePropertySheet3Alert<T> createWildePropertySheetAlert(T bean, CustomPropertyEditorFactory propertyEditorFactory, String headerText, String propertySheetName, HashMap<String, UiPosition> uiPositions, Map<Class<?>, WildeEditableBeanTable.WildeListBeanValidationCallback<?>> customValidators, WildePropertySheet3.WildeBeanValidationCallback<T> wildeBeanValidationCallback){


		if(propertyEditorFactory == null) {
        	propertyEditorFactory = new CustomPropertyEditorFactory(false, false, true);
        }
    	
    	WildePropertySheet3<T> propertySheet = new WildePropertySheetBuilder<T>(propertyEditorFactory)
    			.setPropertyTableName(propertySheetName)
    			.setObject(bean)
				.setFieldUiPositions(uiPositions)
    			.build();
    	propertySheet.setEditable(true);
//    	propertySheet.setMaxWidth(800);
    	
    	Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
    	
        alert.setTitle(WildeDBApplication.getInstance().getApplicationName());
        alert.setHeaderText(headerText);
        alert.getDialogPane().setContent(propertySheet);
        alert.setResizable(true);
		alert.getDialogPane().setMaxWidth(1300);
//		((Stage)alert.getDialogPane().getScene().getWindow()).setMaxWidth(800);
        
        /* Default validationParam */
        CommitChangesParam<T> validationParam = new CommitChangesParam<T>()
				.setRequireNonNullSqlFields(true);
        
        Button okButton = (Button) alert.getDialogPane().lookupButton(ButtonType.OK);
        okButton.addEventFilter(ActionEvent.ACTION, event -> {
    		if(!propertySheet.validateProperties(validationParam)) {
    			event.consume();
    		}
    	});
        
        return new WildePropertySheet3Alert<T>(alert, propertySheet);
    }

	public static<T> WildePropertySheet3Alert<T> createWildePropertySheetAlert(WildePropertySheet3<T> propertySheet, String headerText){
		Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
		alert.setTitle(WildeDBApplication.getInstance().getApplicationName());
		alert.setHeaderText(headerText);
		alert.getDialogPane().setContent(propertySheet);
		alert.setResizable(true);

		// Add icons
//		Stage stage = (Stage) alert.getDialogPane().getScene().getWindow();
//		stage.getIcons().add(new Image("res/images/analysis64.png"));
//		alert.setGraphic(new ImageView(new Image("/res/images/analysis64.png")));



		/* Default validationParam */
		CommitChangesParam<T> validationParam = new CommitChangesParam<T>()
				.setRequireNonNullSqlFields(true);

		// Add filter to the okButton to consume the event if a required input field is not filled
		alert.setOnCloseRequest(event -> {
			if(!propertySheet.validateProperties(validationParam)) {
				event.consume();
			}
		});

		return new WildePropertySheet3Alert<T>(alert, propertySheet);
	}
    
    public static<T> WildePropertySheet3Alert<T> createWildePropertySheetAlert(WildePropertySheet3<T> propertySheet, String headerText, CommitChangesParam<?> validationParam){

    	Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
    	
    	alert.setTitle(WildeDBApplication.getInstance().getApplicationName());
        alert.setHeaderText(headerText);
        alert.getDialogPane().setContent(propertySheet);
        alert.setResizable(true);
//		propertySheet.setMaxWidth(1600);
//		((Stage)alert.getDialogPane().getScene().getWindow()).setMaxWidth(800);
		alert.getDialogPane().setMaxWidth(1300);
        
        Button okButton = (Button) alert.getDialogPane().lookupButton(ButtonType.OK);
    	
    	// Add filter to the okButton to consume the event if a required input field is not filled
    	okButton.addEventFilter(ActionEvent.ACTION, event -> {
    		if(!propertySheet.validateProperties(validationParam)) {
    			event.consume();
    		}
    	});
        
        return new WildePropertySheet3Alert<T>(alert, propertySheet);
    }

	public Alert getAlert() {
		return alert;
	}

	public void setAlert(Alert alert) {
		this.alert = alert;
	}

	public WildePropertySheet3<T> getWildePropertySheet() {
		return wildePropertySheet;
	}

	public void setWildePropertySheet(WildePropertySheet3<T> wildePropertySheet) {
		this.wildePropertySheet = wildePropertySheet;
	}
    
    
}

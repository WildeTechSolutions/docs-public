package com.thomaswilde.wildebeans.ui;

import com.thomaswilde.fxcore.DialogUtil;
import com.thomaswilde.fxcore.TablePreferences;
import com.thomaswilde.wildebeans.WildeBeanTable;
import com.thomaswilde.wildebeans.WildePropertySheet3Alert;
import com.thomaswilde.wildebeans.WildePropertySheet3.CommitChangesParam;
import com.thomaswilde.wildebeans.annotations.ClassDescriptor;
import com.thomaswilde.wildebeans.application.WildeDBApplication;
import com.thomaswilde.wildebeans.database.DbMethods;
import com.thomaswilde.wildebeans.interfaces.Identifiable;
import com.thomaswilde.wildebeans.util.PermissionUtil;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Optional;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.util.StringConverter;

public class DatabaseTablesPane extends BorderPane{
	private static final Logger log = LogManager.getLogger();
	
	private StackPane topNode;
    private HBox indicatorContainer;
    
    ChoiceBox<TablePickerOption> tableChoiceBox;
    
    private ObjectProperty<WildeBeanTable<?>> wildeBeanTable;
    private ObservableList<?> tableItems;
    private OnObjectSelectedListener onObjectSelectedListener;
    
    public interface OnObjectSelectedListener{
    	void onObjectSelected(Object object);
    }

    public interface OnRefreshListener{
        void onRefreshInvoked(List<?> objectsLoaded);
    }
    
    public DatabaseTablesPane(OnObjectSelectedListener onObjectSelectedListener, List<Class<?>> classesToShow, Set<String> userPermissions) {
    	this.onObjectSelectedListener = onObjectSelectedListener;
    	initUi();
    	initChoiceBoxSelectionOptions(classesToShow, userPermissions);
    }

    public DatabaseTablesPane(OnObjectSelectedListener onObjectSelectedListener, List<TablePickerOption> pickerOptions) {
        this.onObjectSelectedListener = onObjectSelectedListener;
        initUi();
        initChoiceBoxSelectionOptions(pickerOptions);
    }
    
    private void initUi() {
    	wildeBeanTable = new SimpleObjectProperty<>();
        topNode = new StackPane();
        indicatorContainer = new HBox();
        indicatorContainer.setAlignment(Pos.CENTER_RIGHT);
        ProgressIndicator progressIndicator = new ProgressIndicator();
        indicatorContainer.getChildren().setAll(progressIndicator);
        progressIndicator.setPrefSize(40,40);



        tableChoiceBox = new ChoiceBox<>();
        
        // Init values and convert
        
        
        // Add selection listener
        tableChoiceBox.getSelectionModel().selectedItemProperty().addListener(tableChoiceBoxListener);

        HBox choiceBoxWrapper = new HBox();
        choiceBoxWrapper.setPadding(new Insets(5));
        choiceBoxWrapper.setAlignment(Pos.CENTER);
        choiceBoxWrapper.getChildren().setAll(tableChoiceBox);

        topNode.getChildren().add(choiceBoxWrapper);
        setTop(topNode);
        
    }

    private void initChoiceBoxSelectionOptions(List<TablePickerOption> pickerOptions){
        pickerOptions.sort(Comparator.comparing(TablePickerOption::getDisplayName));
        pickerOptions.add(0, new TablePickerOption(null, "Pick a table..."));

        tableChoiceBox.setItems(FXCollections.observableArrayList(pickerOptions));


        tableChoiceBox.setConverter(new StringConverter<TablePickerOption>() {
            @Override
            public String toString(TablePickerOption object) {
                if(object != null){
                    return object.getDisplayName();
                }
                return null;
            }

            @Override
            public TablePickerOption fromString(String string) {
                return tableChoiceBox.getSelectionModel().getSelectedItem();
            }
        });
        tableChoiceBox.getSelectionModel().selectFirst();
    }
    
    private void initChoiceBoxSelectionOptions(List<Class<?>> classesToShow, Set<String> userPermissions) {
    	
    	List<TablePickerOption> pickerOptions = new ArrayList<>();
    	
    	for(Class<?> clazz : classesToShow) {
    		ClassDescriptor classDescriptor = clazz.getAnnotation(ClassDescriptor.class);
    		if(classDescriptor == null) {
    			log.error("Class must have ClassDescriptor annotation");
    			continue;
    		}
    		
    		/* Check that any required view permissions are contained in the userPermissions */
    		boolean canView = true;
    		if(classDescriptor.viewPermissions().length > 0) {
    			if(!PermissionUtil.containsAny(userPermissions, classDescriptor.viewPermissions())) {
    				canView = false;
    			}
    		}
    		
    		if(canView) {
    			pickerOptions.add(new TablePickerOption(clazz));
    		}
    		
    	}

        initChoiceBoxSelectionOptions(pickerOptions);
    	

    }
    
    /**
     * Simple choice box listener to generate a new table for the selected class
     */
    private ChangeListener<TablePickerOption> tableChoiceBoxListener = (observable, oldValue, newValue) -> {
    	if(tableChoiceBox.getSelectionModel().getSelectedItem().getTableClass() == null){

            setCenter(null);
            return;
        }

    	generateTableList();
    };
    
    /**
     * Runs a task to pull the items that get populated in the table
     * The table is created after the task is completed
     */
    private void generateTableList() {
    	Task<List<?>> getCTITask = new Task<List<?>>() {
            @Override
            protected List<?> call() throws Exception {
//                return WildeDBApplication.getInstance().query(tableChoiceBox.getSelectionModel().getSelectedItem().getTableClass(),
//                        tableChoiceBox.getSelectionModel().getSelectedItem().getWhereFilter());
                TablePickerOption tablePickerOption = tableChoiceBox.getSelectionModel().getSelectedItem();

                Class<?> selectedClass = tablePickerOption.getTableClass();
                if (!WildeDBApplication.getInstance().getEntityRepositoryMap().containsKey(selectedClass)) {
                    log.warn("No registered repository for class {}", selectedClass);
                    return new ArrayList<>();
                }
                Map<String, Object> paramMap = tablePickerOption.getParamMap();
                if (paramMap != null && !paramMap.isEmpty()) {
                    return WildeDBApplication.getInstance().getEntityRepositoryMap().get(selectedClass).findBy(paramMap);
                }
                return WildeDBApplication.getInstance().getEntityRepositoryMap().get(selectedClass).findAll();
            }
        };
        getCTITask.setOnRunning(event -> {

        	log.trace("Loading objects for dropdown menu item {}", tableChoiceBox.getSelectionModel().getSelectedItem().getDisplayName());
            if(!topNode.getChildren().contains(indicatorContainer))
                topNode.getChildren().add(indicatorContainer);
            
        });
        getCTITask.setOnSucceeded(event -> {
        	
        	log.trace("Loaded {}, task succeeded", tableChoiceBox.getSelectionModel().getSelectedItem().getDisplayName());
        	
            topNode.getChildren().remove(indicatorContainer);
            tableItems = FXCollections.observableArrayList(getCTITask.getValue());

            TablePickerOption tablePickerOption = tableChoiceBox.getSelectionModel().getSelectedItem();
            if(tablePickerOption.getOnRefreshListener() != null) tablePickerOption.getOnRefreshListener().onRefreshInvoked(tableItems);

            setWildeBeanTable(new WildeBeanTable<>());
            getWildeBeanTable().populateTable(tableChoiceBox.getSelectionModel().getSelectedItem().getTableClass(), tableItems, tableView1 -> WildeDBApplication.getInstance().saveTableColumnSelections(tableView1));
            
            TablePreferences.loadPreferencesToTable(getWildeBeanTable().getTableView(), WildeDBApplication.getInstance().getTablePreferencesList());

            
            getWildeBeanTable().getTableView().getSelectionModel().selectedItemProperty().addListener((observable1, oldValue1, newValue1) -> {
                if(newValue1 != null) {
                	if(newValue1 instanceof Identifiable<?>) {

                        ClassDescriptor classDescriptor = tableChoiceBox.getSelectionModel().getSelectedItem().getTableClass().getAnnotation(ClassDescriptor.class);

                        boolean permissionToEdit = true;
                        if(classDescriptor.editPermissions().length > 0){
                            if(!PermissionUtil.containsAny(WildeDBApplication.getInstance().getUserInterface().getPermissions(), classDescriptor.editPermissions())){
                                permissionToEdit = false;
                            }
                        }
                        if(permissionToEdit){
                            onObjectSelectedListener.onObjectSelected(newValue1);
                        }else{
                            DialogUtil.showAlertDialog(getScene().getWindow(), "User does not have permission to edit", "Sorry, you do not have permission to edit this table");
                        }

                	}else {
                		throw new NullPointerException("Class did not implement Identifiable");
                	}
                }
            });
            
            getWildeBeanTable().setRefreshButtonListener(() -> generateTableList());
            getWildeBeanTable().setRefreshButtonVisible(true);
            
            // If class is insertable and user has edit permission, activate the add button
            ClassDescriptor classDescriptor = tableChoiceBox.getSelectionModel().getSelectedItem().getTableClass().getAnnotation(ClassDescriptor.class);
            boolean classIsInsertable = classDescriptor != null ? classDescriptor.isInsertable() : false;
            
            //TODO Use permission to activate the add button
            boolean permissionToAdd = true;
            if(classDescriptor.createPermissions().length > 0){
                if(!PermissionUtil.containsAny(WildeDBApplication.getInstance().getUserInterface().getPermissions(), classDescriptor.createPermissions())){
                    permissionToAdd = false;
                }
            }
            if(permissionToAdd){
                getWildeBeanTable().setAddNewButtonListener(() -> addNewObject());
                getWildeBeanTable().setAddNewButtonVisible(true);
            }

            // To be able to add, check that user has cx_edit permission, or specific permissino associated with the class (i.e. CTI)
//            if((ApplicationMemory.getInstance().getUser().isAdmin())
//            		&& classIsInsertable) {
//            	getWildeBeanTable().setAddNewButtonListener(() -> addNewObject());
//            	getWildeBeanTable().setAddNewButtonVisible(true);
//            }
            
            setCenter(getWildeBeanTable().getNode());
        });
        getCTITask.setOnFailed(event -> {
        	log.error(ExceptionUtils.getStackTrace(getCTITask.getException()));
            topNode.getChildren().remove(indicatorContainer);
        });
        getCTITask.setOnCancelled(event -> {
            topNode.getChildren().remove(indicatorContainer);
        });
        new Thread(getCTITask).start();
        
    }
    
    /**
     * Method called to create a new object in the selected table
     * Opens up a wildePropertySheet alert and requires the required SQL fields
     */
    private void addNewObject() {
    	TablePickerOption currentSelection = tableChoiceBox.getSelectionModel().getSelectedItem();
    	
    	// Display alert if no selection
    	if(currentSelection.getTableClass() == null) {
//    		DialogUtil.showAlertDialog(Main.APPLICATION_NAME, "No Table Selection", "Please select a table first");
    		return;
    	}
    	
    	// Instantiate a new object
    	try {
			Object newObject = currentSelection.getTableClass().newInstance();
			
			// Create the header text
			ClassDescriptor classDescriptor = currentSelection.getTableClass().getAnnotation(ClassDescriptor.class);
			String headerText = "Create a new " + classDescriptor.displayName();
			
			// Create the property sheet alert
			WildePropertySheet3Alert<?> wildePropertySheet3Alert = WildePropertySheet3Alert.createWildePropertySheetAlert(newObject, headerText, newObject.getClass().getName());
			
			// Make the properties editable and require validation
//			wildePropertySheetAlert.getWildePropertySheet().setPropertiesEditableForDbInsert();			
//			wildePropertySheetAlert.requireNonNullSqlFields();
			
			// Show dialog
			Optional<ButtonType> dialogResult = DialogUtil.showAlertAndWait(getScene().getWindow(), wildePropertySheet3Alert.getAlert());
			
			if(dialogResult.isPresent() && dialogResult.get().equals(ButtonType.OK)) {
				// Commit the changes
//				if(newObject instanceof Loggable) {
//					Loggable loggable = (Loggable) newObject;
//					loggable.addLogItem(ApplicationMemory.getInstance().getUser(), classDescriptor.displayName(), "Created this item into the database");
//					wildePropertySheetAlert.getWildePropertySheet().commitChanges(loggable, ApplicationMemory.getInstance().getUser());
//				}else {
//					wildePropertySheetAlert.getWildePropertySheet().commitChanges();
//				}
				
				//TODO incorporate Loggable
				// no options need to be passed to commit changes param, as they are passed in the validation param of the alert


                try {
                    CommitChangesParam<?> commitChangesParam = new CommitChangesParam<>();
                    wildePropertySheet3Alert.getWildePropertySheet().commitChanges(commitChangesParam);

                    // Insert new object into database
                    log.trace("Inserting new object into database: {}", newObject.toString());

                    ((DbMethods) WildeDBApplication.getInstance().getEntityRepositoryMap().get(currentSelection.getTableClass())).insert(newObject);

//				WildeDBApplication.getInstance().insert(newObject);


                    log.trace("Object inserted into database: {}", newObject.toString());

                    // Update log if applicable
//				if(newObject instanceof Loggable) {
//					((Loggable) newObject).insertLogItemsToDatabase();
//				}

                    // Refresh the list
                    generateTableList();
                    // Add new object into summary view
                    onObjectSelectedListener.onObjectSelected(newObject);
                    // Select the object in the table
//				getWildeBeanTable().getTableView().getSelectionModel().select(newObject);
                } catch (SQLException e) {
                    log.warn(ExceptionUtils.getStackTrace(e));
                    DialogUtil.showExceptionDialog(e);
                }

			}
			
			
			
		} catch (InstantiationException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		} catch (Exception throwables) {
            throwables.printStackTrace();
            log.error("Error saving to database");
        }
    }
    
    
    
    public WildeBeanTable<?> getWildeBeanTable() {
        return wildeBeanTable.get();
    }

    public ObjectProperty<WildeBeanTable<?>> wildeBeanTableProperty() {
        return wildeBeanTable;
    }

    public void setWildeBeanTable(WildeBeanTable<?> wildeBeanTable) {
        this.wildeBeanTable.set(wildeBeanTable);
    }
    
    
    /**
     * @author 1115095
     * Helper class to store a display name along with the class associated with the drop down
     */
    public static class TablePickerOption{
        private Class<?> tableClass;
        private String displayName;
        private Map<String, Object> paramMap;
        private OnRefreshListener onRefreshListener;

        public TablePickerOption(Class<?> clazz, String displayName, Map<String, Object> paramMap, OnRefreshListener onRefreshListener) {
            this.tableClass = clazz;
            this.displayName = displayName;
            this.paramMap = paramMap;
            this.onRefreshListener = onRefreshListener;
        }

        public TablePickerOption(Class<?> clazz, String displayName, Map<String, Object> paramMap) {
            this.tableClass = clazz;
            this.displayName = displayName;
            this.paramMap = paramMap;
        }

        public TablePickerOption(Class<?> clazz, String displayName) {
            this.tableClass = clazz;
            this.displayName = displayName;
        }
        
        public TablePickerOption(Class<?> clazz) {
            this.tableClass = clazz;
            this.displayName = clazz.getAnnotation(ClassDescriptor.class).displayName();
        }

        public Class<?> getTableClass() {
            return tableClass;
        }

        public void setTableClass(Class<?> clazz) {
            this.tableClass = clazz;
        }

        public String getDisplayName() {
            return displayName;
        }

        public void setDisplayName(String displayName) {
            this.displayName = displayName;
        }

        public Map<String, Object> getParamMap() {
            return paramMap;
        }

        public void setParamMap(Map<String, Object> paramMap) {
            this.paramMap = paramMap;
        }

        public OnRefreshListener getOnRefreshListener() {
            return onRefreshListener;
        }

        public void setOnRefreshListener(OnRefreshListener onRefreshListener) {
            this.onRefreshListener = onRefreshListener;
        }
    }
}

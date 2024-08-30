package com.thomaswilde.wildebeans;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;
import javafx.scene.control.TableRow;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeTableRow;

@SuppressWarnings("rawtypes")
public class WildeBean<T> {

	private static final Logger log = LoggerFactory.getLogger(WildeBean.class);

	private UUID id = UUID.randomUUID();

	private T object;
	
	private List<WildeBeanProperty<?>> wildeBeanProperties;
	
	private final BooleanProperty toDelete = new SimpleBooleanProperty(this, "toDelete", false);
	private final BooleanProperty toAdd = new SimpleBooleanProperty(this, "toAdd", false);
	private final BooleanProperty dirty = new SimpleBooleanProperty(this, "dirty", false);
	private final BooleanProperty containsInvalidProperty = new SimpleBooleanProperty(this, "containsInvalidProperty", false);
	
	private final ObjectProperty<TableRow<T>> tableRow = new SimpleObjectProperty<>(this, "tableRow");
	private final ObjectProperty<TreeTableRow<T>> treeTableRow = new SimpleObjectProperty<>(this, "treeTableRow");
	private final ObjectProperty<TreeItem<T>> treeItem = new SimpleObjectProperty<>(this, "treeItem");

//	private Map<String, ObservableList<WildeBean<?>>> beanListObjectsMap = new HashMap<>();
	
	// A map of the WildeBeanProperty representing the ObservableList to the ObservableList
//	private Map<WildeBeanProperty<?>, WildeListBean<T,?>> beanListObjectsMap = new HashMap<>();
	private final List<WildeListBean<T, ?>> listBeans = new ArrayList<>();
	
	
	public WildeBean(T object){
		this.object = object;
	}
	
	@SuppressWarnings({ "unchecked"})
	public WildeBean(T object, CustomPropertyEditorFactory editorFactory, Map<String, UiPosition> fieldUiPositions){
		this.object = object;
		List<WildeBeanProperty<?>> properties = (List<WildeBeanProperty<?>>) (List<T>) WildePropertyUtils.getWildeBeanProperties(object, fieldUiPositions);
		
		init(properties, editorFactory);
		
//		
//		beanListObjectsMap.putAll(WildePropertyUtils.getUiLists(object));
		listBeans.addAll(WildePropertyUtils.getUiLists(object));
	}
	
	@SuppressWarnings({ "unchecked"})
	public WildeBean(T object, CustomPropertyEditorFactory editorFactory){
		this.object = object;
		List<WildeBeanProperty<?>> properties = (List<WildeBeanProperty<?>>) (List<T>) WildePropertyUtils.getWildeBeanProperties(object);

		properties.forEach(wildeBeanProperty -> log.trace("Registered Property field nameK: {}", wildeBeanProperty.getFieldName()));
		init(properties, editorFactory);
		
//		
//		beanListObjectsMap.putAll(WildePropertyUtils.getUiLists(object));
		listBeans.addAll(WildePropertyUtils.getUiLists(object));
	}
	
	@SuppressWarnings({ "unchecked"})
	private void init(List<WildeBeanProperty<?>> properties, CustomPropertyEditorFactory editorFactory) {
		
		
		
		setWildeBeanProperties(properties);
		getNewEditors(editorFactory);

		properties.forEach(property -> property.setWildeBean(this));
		
		
		toDelete.addListener(statusChangeListener);
		toAdd.addListener(statusChangeListener);
		dirty.addListener(statusChangeListener);
		
		tableRow.addListener((observable, oldValue, newValue) -> {
			if(oldValue != null) {
				oldValue.setStyle("");
				removeTableRowStyles(oldValue);
			}
			if(newValue != null) {
				setRowStyle();
			}else {
				for(WildeBeanProperty<?> property : getWildeBeanProperties()) {
					property.setWildeBeanTableCell(null);
				}
//				if(isContainsInvalidProperty()) {
//					for(WildeBeanProperty<?> property : getWildeBeanProperties()) {
//						if(property.getWildeBeanTableCell() != null) {
//							property.getWildeBeanTableCell().setStyle("");
//						}
//					}
//				}
			}
			
		});

		treeTableRow.addListener((observable, oldValue, newValue) -> {
			if(oldValue != null) {
				oldValue.setStyle("");
				removeTableRowStyles(oldValue);
			}
			if(newValue != null) {
				setTreeTableRowStyle();
			}else {
				for(WildeBeanProperty<?> property : getWildeBeanProperties()) {
					property.setPlanTreeTableCell(null);
				}
			}

		});
	}
	
	public void getNewEditors(CustomPropertyEditorFactory editorFactory) {
		for(WildeBeanProperty<?> property : getWildeBeanProperties()) {

			log.trace("getting editor for {}", property.getFieldName());
			WildePropertyEditor wildePropertyEditor = editorFactory.call(property);
			property.setPropertyEditor(wildePropertyEditor);
			wildePropertyEditor.setValue(property.getValue());
			
			wildePropertyEditor.getEditor().focusedProperty().addListener((observable, oldValue, newValue) -> {
				if(!newValue) {
					if(property.getWildeBeanTableCell() != null) {
						property.getWildeBeanTableCell().commitEdit(property.getValue());
					}
				}
			});
		}

		// Now that all editors have been set, loop back through and set relationship bindings (i.e. disables)
		for(WildeBeanProperty<?> property : getWildeBeanProperties()) {
			if(property.getUiProperty().disablesFields().length > 0){
				for (String fieldToDisable : property.getUiProperty().disablesFields()) {

					WildeBeanProperty<?> propertyToBindDisable = getWildeBeanProperty(fieldToDisable);
					if(propertyToBindDisable != null){
						if(property.getUiProperty().disablesFieldsOnFalse()){
							propertyToBindDisable.getPropertyEditor().getEditor().disableProperty().bind(((BooleanProperty) property.getPropertyEditor().getObservableValue()).not());
						}else{
							propertyToBindDisable.getPropertyEditor().getEditor().disableProperty().bind((BooleanProperty) property.getPropertyEditor().getObservableValue());
						}
					}

				}
			}
		}

	}
	
	private ChangeListener<Boolean> statusChangeListener = (observable, oldValue, newValue) ->{
		if(getTableRow() != null) {
			setRowStyle();
		}
		if(getTreeTableRow() != null) {
			setTreeTableRowStyle();
		}
		
	};
	
	private void setRowStyle() {
		removeTableRowStyles(getTableRow()) ;
		
		if(isDirty() && !isToAdd() && !isToDelete()) {
			
//			getTableRow().setStyle("-fx-background-color: YELLOW; -fx-text-fill: BLACK;");       
			getTableRow().getStyleClass().add("dirty-table-row");
		}else if(isToAdd()){
//			getTableRow().setStyle("-fx-background-color: GREEN; -fx-text-fill: WHITE;");
			getTableRow().getStyleClass().add("added-table-row");
		}else if(isToDelete()){
//			getTableRow().setStyle("-fx-background-color: RED; -fx-text-fill: WHITE;");
//			getTableRow().getStyleClass().clear();
			getTableRow().getStyleClass().add("deleted-table-row");
		}else {
			getTableRow().setStyle("");
			
		}
	}

	private void setTreeTableRowStyle() {
		removeTableRowStyles(getTreeTableRow()) ;

		if(isDirty() && !isToAdd() && !isToDelete()) {
//			getTreeTableRow().getStyleClass().add("dirty-table-row");
		}else if(isToAdd()){
			getTreeTableRow().getStyleClass().add("added-table-row");
			log.trace("setting tree table style to added");
			getWildeBeanProperties().forEach(property -> property.getPlanTreeTableCell());
		}else if(isToDelete()){
			getTreeTableRow().getStyleClass().add("deleted-table-row");
		}else {
			getTreeTableRow().setStyle("");
			getWildeBeanProperties().forEach(property -> property.updatePlanTreeTableCell());
		}
	}
	
	private void removeTableRowStyles(TableRow<?> tableRow) {
		tableRow.getStyleClass().remove("deleted-table-row");
		tableRow.getStyleClass().remove("dirty-table-row");
		tableRow.getStyleClass().remove("added-table-row");
	}

	private void removeTableRowStyles(TreeTableRow<?> tableRow) {
		tableRow.getStyleClass().remove("deleted-table-row");
		tableRow.getStyleClass().remove("dirty-table-row");
		tableRow.getStyleClass().remove("added-table-row");
	}

	public void removeTableRowStyles() {
		if(getTableRow() != null) {
			removeTableRowStyles(getTableRow());
		}
		if(getTreeTableRow() != null) {
			log.debug("Removing tree table row style");
			removeTableRowStyles(getTreeTableRow());
		}else{
			log.warn("There was no tabletreerow to remove styles from");
		}
	}
	
	public T getObject() {
		return object;
	}
	public void setObject(T object) {
		this.object = object;
	}
	public List<WildeBeanProperty<?>> getWildeBeanProperties() {
		return wildeBeanProperties;
	}
	public void setWildeBeanProperties(List<WildeBeanProperty<?>> wildeBeanProperties) {
		this.wildeBeanProperties = wildeBeanProperties;
	}
	public BooleanProperty toDeleteProperty() {
		return this.toDelete;
	}
	
	public boolean isToDelete() {
		return this.toDeleteProperty().get();
	}
	
	public void setToDelete(final boolean toDelete) {
		this.toDeleteProperty().set(toDelete);
	}
	
	public BooleanProperty toAddProperty() {
		return this.toAdd;
	}
	
	public boolean isToAdd() {
		return this.toAddProperty().get();
	}
	
	public void setToAdd(final boolean toAdd) {
		this.toAddProperty().set(toAdd);
	}

	public BooleanProperty dirtyProperty() {
		return this.dirty;
	}	

	public boolean isDirty() {
		return this.dirtyProperty().get();
	}	

	public void setDirty(final boolean dirty) {
		this.dirtyProperty().set(dirty);
	}

	public ObjectProperty<TableRow<T>> tableRowProperty() {
		return this.tableRow;
	}
	

	public TableRow<T> getTableRow() {
		return this.tableRowProperty().get();
	}
	

	public void setTableRow(final TableRow<T> tableRow) {
		this.tableRowProperty().set(tableRow);
	}

	public BooleanProperty containsInvalidPropertyProperty() {
		return this.containsInvalidProperty;
	}
	

	public boolean isContainsInvalidProperty() {
		return this.containsInvalidPropertyProperty().get();
	}
	

	public void setContainsInvalidProperty(final boolean containsInvalidProperty) {
		this.containsInvalidPropertyProperty().set(containsInvalidProperty);
		
		if(!containsInvalidProperty) {
			getWildeBeanProperties().forEach(property -> property.setInvalidSqlValue(false));
		}
	}
	
	public WildeBeanProperty getWildeBeanProperty(String fieldName){
        return getWildeBeanProperties().stream().filter(wildeBeanProperty -> Objects.equals(wildeBeanProperty.getFieldName(), fieldName)).findFirst().orElse(null);
    }

	public List<WildeListBean<T, ?>> getListBeans() {
		return listBeans;
	}

	public ObjectProperty<TreeTableRow<T>> treeTableRowProperty() {
		return this.treeTableRow;
	}


	public TreeTableRow<T> getTreeTableRow() {
		return this.treeTableRowProperty().get();
	}


	public void setTreeTableRow(final TreeTableRow<T> treeTableRow) {
		this.treeTableRowProperty().set(treeTableRow);
	}

	public ObjectProperty<TreeItem<T>> treeItemProperty() {
		return this.treeItem;
	}


	public TreeItem<T> getTreeItem() {
		return this.treeItemProperty().get();
	}


	public void setTreeItem(final TreeItem<T> treeItem) {
		this.treeItemProperty().set(treeItem);
	}

	public UUID getId() {
		return id;
	}

//	public Map<WildeBeanProperty<?>, ObservableList<?>> getBeanListObjectsMap() {
//		return beanListObjectsMap;
//	}
	
	
	
	

	
	
	
}

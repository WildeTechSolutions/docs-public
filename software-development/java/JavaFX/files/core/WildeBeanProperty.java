package com.thomaswilde.wildebeans;

import com.google.common.base.Strings;

import com.thomaswilde.fxcore.FXPropertyUtils;
import com.thomaswilde.wildebeans.annotations.PrimaryKey;
import com.thomaswilde.wildebeans.annotations.SqlProperty;
import com.thomaswilde.wildebeans.annotations.UiProperty;
import com.thomaswilde.wildebeans.tableutil.ModifiableTreeTableCell;
import com.thomaswilde.wildebeans.tableutil.WildeBeanTableCell;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ObservableValue;
import javafx.scene.Node;
import javafx.scene.control.TreeTableCell;

import org.apache.commons.beanutils.PropertyUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.ParameterizedType;
import java.util.Optional;

public class WildeBeanProperty<T> implements UiBeanProperty{

    private static final Logger log = LoggerFactory.getLogger(WildeBeanProperty.class);

    private Object bean;
    private WildeBean<?> wildeBean;

    private UiProperty uiProperty;

    @Deprecated
    private SqlProperty sqlProperty;

    @Deprecated
    private PrimaryKey primaryKey;

    private Field field;
    private int rowPreference = -1;
    private int colPreference = -1;
    private String description;
    private String displayCategory;
    private boolean isEverEditable;


    private WildePropertyEditor<T> propertyEditor;
    private GetMoreInfoCallback getMoreInfoCallback;
    private String displayName;

    private String fieldName;
    private String fullFieldName;

    // Reference for when the beanproperty is within a WildeBeanTableCell
    @SuppressWarnings("rawtypes")
	private ObjectProperty<WildeBeanTableCell> wildeBeanTableCell;
    private ObjectProperty<TreeTableCell<Object, ?>> planTreeTableCell;

    private BooleanProperty invalidSqlValue = new SimpleBooleanProperty(false);

    private BooleanProperty dirty = new SimpleBooleanProperty(false);

    /* Reference to the info node so it doesn't have to be recreated all time time */
    private Node getMoreInfoNode;

    public interface GetMoreInfoCallback{
        void onGetMoreInfo(String methodName);
    }

    public WildeBeanProperty(Object bean, Field field, UiPosition uiPosition) {
        this.bean = bean;
        this.field = field;
        fieldName = field.getName();
        fullFieldName = fieldName;

        this.uiProperty = field.getAnnotation(UiProperty.class);
        this.sqlProperty = field.getAnnotation(SqlProperty.class);
        this.primaryKey = field.getAnnotation(PrimaryKey.class);

        if (Strings.isNullOrEmpty(uiPosition.getPreferredDisplayName())) {
            this.displayName = uiProperty.displayName();
        }else{
            this.displayName = uiPosition.getPreferredDisplayName();
        }

        if (Strings.isNullOrEmpty(uiPosition.getPreferredDescription())) {
            this.description = uiProperty.shortDescription();
        }else{
            this.description = uiPosition.getPreferredDescription();
        }

        if (Strings.isNullOrEmpty(uiPosition.getPreferredDisplayCategory())) {
            this.displayCategory = uiProperty.displayCategory();
        }else{
            this.displayCategory = uiPosition.getPreferredDisplayCategory();
        }

        if(uiPosition.getEditable() == null){
            this.isEverEditable = uiProperty.editable();
        }else{
            this.isEverEditable = uiPosition.getEditable();
        }

        this.rowPreference = uiPosition.getRow();
        this.colPreference = uiPosition.getCol();

        addInvalidationListener();

    }

    public WildeBeanProperty(Object bean, String fullFieldName, UiPosition uiPosition) {
        this.bean = bean;
//        this.field = field;
//
        this.field = FXPropertyUtils.getField(bean.getClass(), fullFieldName);
        if(field == null){
            log.warn("did not find a field {} in class {}", fullFieldName, bean.getClass());
            return;
        }
        this.uiProperty = field.getAnnotation(UiProperty.class);
        this.sqlProperty = field.getAnnotation(SqlProperty.class);
        this.primaryKey = field.getAnnotation(PrimaryKey.class);

        this.fieldName = field.getName();
        this.fullFieldName = fullFieldName;

        if (Strings.isNullOrEmpty(uiPosition.getPreferredDisplayName())) {
            this.displayName = uiProperty.displayName();
        }else{
            this.displayName = uiPosition.getPreferredDisplayName();
        }

        if (Strings.isNullOrEmpty(uiPosition.getPreferredDescription())) {
            this.description = uiProperty.shortDescription();
        }else{
            this.description = uiPosition.getPreferredDescription();
        }

        if (Strings.isNullOrEmpty(uiPosition.getPreferredDisplayCategory())) {
            this.displayCategory = uiProperty.displayCategory();
        }else{
            this.displayCategory = uiPosition.getPreferredDisplayCategory();
        }

        if(uiPosition.getEditable() == null){
            this.isEverEditable = uiProperty.editable();
        }else{
            this.isEverEditable = uiPosition.getEditable();
        }

        this.rowPreference = uiPosition.getRow();
        this.colPreference = uiPosition.getCol();

        addInvalidationListener();

    }

    public WildeBeanProperty(Object bean, Field field) {
        this.bean = bean;
        this.uiProperty = field.getAnnotation(UiProperty.class);
        this.sqlProperty = field.getAnnotation(SqlProperty.class);
        this.primaryKey = field.getAnnotation(PrimaryKey.class);

        if(uiProperty != null) {
        	this.displayName = uiProperty.displayName();
        	this.description = uiProperty.shortDescription();
        	this.displayCategory = uiProperty.displayCategory();
        	this.isEverEditable = uiProperty.editable();
        	rowPreference = uiProperty.gridPosition().gridRow();
        	colPreference = uiProperty.gridPosition().gridCol();
        }


        this.field = field;
        this.fieldName = field.getName();
        this.fullFieldName = fieldName;

        addInvalidationListener();
    }

    public WildeBeanProperty(Object bean, String fullFieldName) {
        this.bean = bean;
        this.fullFieldName = fullFieldName;
        String[] splitFieldName = StringUtils.split(fullFieldName, ".");
        fieldName = splitFieldName[splitFieldName.length - 1];

        this.field = FXPropertyUtils.getField(bean.getClass(), fullFieldName);

        this.uiProperty = field.getAnnotation(UiProperty.class);
        this.sqlProperty = field.getAnnotation(SqlProperty.class);
        this.primaryKey = field.getAnnotation(PrimaryKey.class);

        if(uiProperty != null) {
        	this.displayName = uiProperty.displayName();
        	this.description = uiProperty.shortDescription();
        	this.displayCategory = uiProperty.displayCategory();
        	this.isEverEditable = uiProperty.editable();

        	rowPreference = uiProperty.gridPosition().gridRow();
        	colPreference = uiProperty.gridPosition().gridCol();
        }

        addInvalidationListener();
//        this.field = field;

    }

    private void addInvalidationListener() {
    	invalidSqlValue.addListener((observable, oldValue, newValue) -> {



        	if(getWildeBeanTableCell() != null) {
        		if(newValue) {

        			getWildeBeanTableCell().setStyle("-fx-background-color: RED; -fx-text-fill: WHITE;");
        		}else {

        			getWildeBeanTableCell().setStyle("");
        		}
        	}else {

        	}
		});
    }

    public int getRowPreference() {
        return rowPreference;
    }

    public void setRowPreference(int rowPreference) {
        this.rowPreference = rowPreference;
    }

    public int getColPreference() {
        return colPreference;
    }

    public void setColPreference(int colPreference) {
        this.colPreference = colPreference;
    }


    public String getName() {
//        return super.getName();
//        return uiProperty.displayName();
        return displayName;
    }

    public String getFieldName(){
        return this.fieldName;
    }

    public String getDescription() {
//        return super.getDescription();
        return description;
    }

    public boolean isEverEditable() {
        return isEverEditable;
    }

    public Class<?> getType() {
//        return super.getType();
//        return clazz;

//        if(this.field != null){
//            return WildePropertyUtils.getFieldType(bean.getClass(), field);
//        }else{
//            return WildePropertyUtils.getFieldType(bean.getClass(), fullFieldName);
//        }

    	return FXPropertyUtils.getFieldType(bean.getClass(), fullFieldName);
    }

    public Object getBean() {
        return bean;
    }

    public Object getValue() {

//        if(field != null){
//            return WildePropertyUtils.getProperty(getBean(), field.getName());
//        }else{
//
//        }

//

        try {
            return PropertyUtils.getNestedProperty(getBean(), fullFieldName);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        }

        return null;
//
//        return null;

//        return super.getValue();
    }

    public void setValue(Object value) {


        try {
//            PropertyUtils.setProperty(getBean(), field.getName(), value);
            PropertyUtils.setNestedProperty(getBean(), fullFieldName, value);

        } catch (IllegalAccessException e) {

            e.printStackTrace();
        } catch (InvocationTargetException e) {

            e.printStackTrace();
        } catch (NoSuchMethodException e) {

            e.printStackTrace();
        }


//        super.setValue(value);

    }

    public String getCategory() {

        return displayCategory;
    }

    public ObservableValue<?> getObservableValue() {
//        return this.observableValue;
//        return super.getObservableValue();
//        BeanProperty
//        Optional<ObservableValue<? extends Object>> optional = Optional.of((ObservableValue) WildePropertyUtils.getObservableValue(getBean(), fullFieldName));
        return FXPropertyUtils.getObservableValue(getBean(), fullFieldName);
    }

    public WildePropertyEditor<T> getPropertyEditor() {
        return propertyEditor;
    }

    public void setPropertyEditor(WildePropertyEditor<T> propertyEditor) {
        this.propertyEditor = propertyEditor;
    }

//    @Override
//    public Optional<Class<? extends PropertyEditor<?>>> getPropertyEditorClass() {
//
////        return super.getPropertyEditorClass();
////        if ((this.beanPropertyDescriptor.getPropertyEditorClass() != null) &&
////
////                PropertyEditor.class.isAssignableFrom(this.beanPropertyDescriptor.getPropertyEditorClass())) {
////
////
////
////            return Optional.of((Class<PropertyEditor<?>>)this.beanPropertyDescriptor.getPropertyEditorClass());
////
////        }
//
//
//
//        return super.getPropertyEditorClass();
//
////        return (Optional)(Optional.of(CustomPropertyEditor.class));
////        return Optional.empty();
//    }

    public UiProperty getUiProperty() {
        return uiProperty;
    }


    @Deprecated
    public SqlProperty getSqlProperty() {
		return sqlProperty;
	}

    @Deprecated
	public PrimaryKey getPrimaryKey() {
		return primaryKey;
	}

	public boolean isEditable() {
        return true;
    }

    public GetMoreInfoCallback getGetMoreInfoCallback() {
        return getMoreInfoCallback;
    }

    public void setGetMoreInfoCallback(GetMoreInfoCallback getMoreInfoCallback) {
        this.getMoreInfoCallback = getMoreInfoCallback;
    }

    public boolean isBeansOwnField() {
    	return !fullFieldName.contains(".");
    }

	public Field getField() {
		return field;
	}

	public void setEverEditable(boolean isEverEditable) {
		this.isEverEditable = isEverEditable;
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

	@SuppressWarnings("rawtypes")
	public ObjectProperty<WildeBeanTableCell> wildeBeanTableCellProperty() {
		initWildeBeanTableCell();
		return this.wildeBeanTableCell;
	}


	@SuppressWarnings("rawtypes")
	public WildeBeanTableCell getWildeBeanTableCell() {
		initWildeBeanTableCell();
		return this.wildeBeanTableCellProperty().get();
	}


	@SuppressWarnings("rawtypes")
	public void setWildeBeanTableCell(final WildeBeanTableCell wildeBeanTableCell) {
		initWildeBeanTableCell();
		this.wildeBeanTableCellProperty().set(wildeBeanTableCell);
	}


	private boolean wildeBeanTableCellInitialized = false;
	private void initWildeBeanTableCell() {
		if(!wildeBeanTableCellInitialized) {
			if(this.wildeBeanTableCell == null) {
				this.wildeBeanTableCell = new SimpleObjectProperty<>();
			}

			if(invalidSqlValue == null) {
				invalidSqlValue = new SimpleBooleanProperty();
			}

			wildeBeanTableCell.addListener((observable, oldValue, newValue) -> {
				if(oldValue != null) {
					oldValue.setStyle("");
				}
				if(newValue != null) {
					if(isInvalidSqlValue()) {
						newValue.setStyle("-fx-background-color: RED; -fx-text-fill: WHITE;");
					}else {
						newValue.setStyle("");
					}
				}else {

				}
			});



		}

		wildeBeanTableCellInitialized = true;

	}

	public BooleanProperty invalidSqlValueProperty() {
		return this.invalidSqlValue;
	}


	public boolean isInvalidSqlValue() {
		return this.invalidSqlValueProperty().get();
	}


	public void setInvalidSqlValue(final boolean invalidSqlValue) {
		this.invalidSqlValueProperty().set(invalidSqlValue);
	}

	/**
	 * If the Object is a List of type wildcard, this method will return the Type for the list
	 * https://stackoverflow.com/questions/1942644/get-generic-type-of-java-util-list
	 * @return
	 */
	public Class<?> getParameterizedType(){
		ParameterizedType integerListType = (ParameterizedType) getField().getGenericType();
        Class<?> type = (Class<?>) integerListType.getActualTypeArguments()[0];

        return type;
	}

	public Node getGetMoreInfoNode() {
		return getMoreInfoNode;
	}

	public void setGetMoreInfoNode(Node getMoreInfoNode) {
		this.getMoreInfoNode = getMoreInfoNode;
	}

    public ObjectProperty<TreeTableCell<Object, ?>> planTreeTableCellProperty() {
        initPlanTreeTableCell();
        return this.planTreeTableCell;
    }


    public TreeTableCell<Object, ?> getPlanTreeTableCell() {
        initPlanTreeTableCell();
        return this.planTreeTableCellProperty().get();
    }


    public void setPlanTreeTableCell(final TreeTableCell<Object, ?> planTreeTableCell) {
        initPlanTreeTableCell();
        this.planTreeTableCellProperty().set(planTreeTableCell);
    }

    private boolean planTreeTableCellInitialized = false;
    private void initPlanTreeTableCell() {


        if(!planTreeTableCellInitialized) {
            if (planTreeTableCell == null) {
                planTreeTableCell = new SimpleObjectProperty<>(this, "planTreeTableCell");
            }

            if(invalidSqlValue == null) {
                invalidSqlValue = new SimpleBooleanProperty();
            }

            planTreeTableCell.addListener((observable, oldValue, newValue) -> {
                if(oldValue != null) {
                    oldValue.setStyle("");
                }
                if(newValue != null) {
                    if(isInvalidSqlValue()) {
                        newValue.setStyle("-fx-background-color: RED; -fx-text-fill: WHITE;");
                    }else {
                        newValue.setStyle("");
                    }
                }else {

                }
            });



        }

        planTreeTableCellInitialized = true;
    }

    @SuppressWarnings("unchecked")
    // Could try to use reflection to update plantreetable cell, so we don't have to actually
    // get an instance of it, which will be application dependent
    public void updatePlanTreeTableCell() {
        if(getPlanTreeTableCell() instanceof ModifiableTreeTableCell){
            ((ModifiableTreeTableCell) getPlanTreeTableCell()).updateItem();
        }
//            (getPlanTreeTableCell()).updateItem(getValue(), false);
    }

    public void updateCell() {
//        if(getPlanTreeTableCell() != null)
//            getPlanTreeTableCell().updateItem(getValue(), false);

        if(getWildeBeanTableCell() != null) {
            getWildeBeanTableCell().updateItem(getValue(), false);
        }
    }

    public WildeBean<?> getWildeBean() {
        return wildeBean;
    }

    public void setWildeBean(WildeBean<?> wildeBean) {
        this.wildeBean = wildeBean;
    }







}

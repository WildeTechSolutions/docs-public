package com.thomaswilde.wildebeans;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Timer;
import java.util.TimerTask;
import java.util.TreeMap;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.controlsfx.control.SegmentedButton;
import org.controlsfx.control.textfield.TextFields;
import org.controlsfx.tools.Borders;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Strings;


import com.thomaswilde.foenix_utils.FoenixUtil;
import com.thomaswilde.fxcore.StandardEditSavePane;
import com.thomaswilde.icons.GlyphButton;
import com.thomaswilde.wildebeans.application.RepositoryUtil;
import com.thomaswilde.wildebeans.interfaces.Identifiable;
import com.thomaswilde.wildebeans.interfaces.Loggable;
import com.thomaswilde.wildebeans.customcontrols.*;

import com.thomaswilde.wildebeans.annotations.ClassDescriptor;
import com.thomaswilde.wildebeans.annotations.MoreInfo;
import com.thomaswilde.wildebeans.application.WildeDBApplication;
import com.thomaswilde.wildebeans.WildeBeanProperty.GetMoreInfoCallback;
import com.thomaswilde.wildebeans.WildeEditableBeanTable.TableCommitCallback;
import com.thomaswilde.wildebeans.WildeEditableBeanTable.WildeListBeanValidationCallback;

import javax.persistence.criteria.CriteriaBuilder;

import animatefx.animation.Shake;
import de.jensd.fx.glyphs.GlyphIcon;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Accordion;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TitledPane;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToolBar;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;

public class WildePropertySheet3<T> extends BorderPane{

	private static Logger log = LoggerFactory.getLogger(WildePropertySheet3.class);

	public enum WildePropertySheetMode{
        NAME, CATEGORY, MULTIGRID, WRAPPED_GRIDS
    }
	
	/* Object that is being wrapped and displayed in property sheet */
	private ObjectProperty<WildeBean<T>> wildeBean = new SimpleObjectProperty<>();
	
	/* UI Objects */
	private ScrollPane scroller;
    private Region propertySheetGrid;
    private ToolBar toolbar;	
    private SegmentedButton modeButton;
    private ToggleButton floatingLabelButton;
    private TextField searchField;
	
    private StandardEditSavePane standardEditSavePane;
    /* Properties */
    private IntegerProperty preferredMultiGridNumberOfColumns = new SimpleIntegerProperty(this, "preferredMultiGridNumberOfColumns", 2);
    private CustomPropertyEditorFactory editorFactory;
//    private GetMoreInfoFactory getMoreInfoFactory;
    private ObjectProperty<WildePropertySheetMode> wildeMode = new SimpleObjectProperty<>(this, "wildeMode");
    private BooleanProperty modeSwitcherVisible = new SimpleBooleanProperty(this, "modeSwitcherVisible", true);
    private BooleanProperty searchBoxVisible = new SimpleBooleanProperty(this, "searchBoxVisible", true);
    private StringProperty titleFilter = new SimpleStringProperty(this, "titleFilter", "");
    private boolean useScrollPane = true;
    private String propertyTableName;
    private BooleanProperty editable = new SimpleBooleanProperty(this, "editable", false);
    // showListTablesProperty
    private final BooleanProperty showListTables = new SimpleBooleanProperty(this, "showListTables", true);


    
    private Timer timer;

    private final int DEFAULT_NUM_OF_COLUMNS = 2;
    
    private boolean initPreferences = true;
    
    public interface TemporaryAdditionCallback{
    	void onNewObjectRequested(WildeListBean<?, Object> listBean);
    }
    
    private Map<Class<?>, TemporaryAdditionCallback> listAdditionCallbacks = new HashMap<>();
    
    /* Builder */
    public static class WildePropertySheetBuilder<T>{
    	private CustomPropertyEditorFactory editorFactory;
        private Map<Class<?>, TemporaryAdditionCallback> listAdditionCallbacks = new HashMap<>();
        private Map<String, UiPosition> fieldUiPositions;
        private T object;
        private String propertyTableName;
        private boolean useDefaultEditButtons = false;
        private int preferredNumberOfColumns = 0;
        private List<StandardEditSavePane.OnSaveCallback> onSaveCallbacks = new ArrayList<>();
		private boolean showListTables = true;
        
        public WildePropertySheetBuilder(CustomPropertyEditorFactory editorFactory) {
        	setEditorFactory(editorFactory);
        }
        
        public WildePropertySheetBuilder() {
        	setEditorFactory(new CustomPropertyEditorFactory(false, false, true));
        }
        
		public WildePropertySheetBuilder<T> setEditorFactory(CustomPropertyEditorFactory editorFactory) {
			this.editorFactory = editorFactory;
			return this;
		}
		public WildePropertySheetBuilder<T> setListAdditionCallbacks(Map<Class<?>, TemporaryAdditionCallback> listAdditionCallbacks) {
			this.listAdditionCallbacks = listAdditionCallbacks;
			return this;
		}
		public WildePropertySheetBuilder<T> setFieldUiPositions(Map<String, UiPosition> fieldUiPositions) {
			this.fieldUiPositions = fieldUiPositions;
			return this;
		}
		public WildePropertySheetBuilder<T> setObject(T object) {
			this.object = object;
			return this;
		}        
		public WildePropertySheetBuilder<T> setPropertyTableName(String propertyTableName) {
			this.propertyTableName = propertyTableName;
			return this;
		}	
		public WildePropertySheetBuilder<T> setUseDefaultEditButtons(boolean useDefaultEditButtons) {
			this.useDefaultEditButtons = useDefaultEditButtons;
			return this;
		}
		
		public WildePropertySheetBuilder<T> setPreferredNumberOfColumns(int preferredNumberOfColumns) {
			this.preferredNumberOfColumns = preferredNumberOfColumns;
			return this;
		}

		public WildePropertySheetBuilder<T> setShowListTables(boolean showListTables) {
			this.showListTables = showListTables;
			return this;
		}
		
		public WildePropertySheetBuilder<T> addOnSaveCallback(StandardEditSavePane.OnSaveCallback onSaveCallback) {
			onSaveCallbacks.add(onSaveCallback);
			return this;
		}

		public WildePropertySheet3<T> build() {
			WildePropertySheet3<T> propertySheet = new WildePropertySheet3<T>(object, editorFactory, fieldUiPositions, propertyTableName, listAdditionCallbacks, preferredNumberOfColumns, showListTables);
			if(useDefaultEditButtons) {
				propertySheet.setDefaultEditSaveButtons();
				propertySheet.getStandardEditSavePane().addOnSaveCallbacks(onSaveCallbacks);
				
			}
			propertySheet.setEditable(false);
			return propertySheet;
		}
        
    }
    
    /* Constructors */
    public WildePropertySheet3() {
		editorFactory = new CustomPropertyEditorFactory(false, false, true);
		constructorInit(null, editorFactory, null, null, null, 0, true);
	}
	
	public WildePropertySheet3(T object, CustomPropertyEditorFactory editorFactory, Map<Class<?>, TemporaryAdditionCallback> listAdditionCallbacks) {		
		constructorInit(object, editorFactory, null, null, listAdditionCallbacks, 0, true);
	}
	
	public WildePropertySheet3(T object, CustomPropertyEditorFactory editorFactory) {		
		constructorInit(object, editorFactory, null, null, null, 0, true);
	}
	
	public WildePropertySheet3(T object, Map<String, UiPosition> fieldUiPositions) {
		constructorInit(object, null, fieldUiPositions, object.getClass().getName(), null, 0, true);
	}
	
	public WildePropertySheet3(T object, CustomPropertyEditorFactory editorFactory, Map<String, UiPosition> fieldUiPositions, Map<Class<?>, TemporaryAdditionCallback> listAdditionCallbacks) {
		constructorInit(object, editorFactory, fieldUiPositions, object.getClass().getName(), listAdditionCallbacks, 0, true);
	}
	
	public WildePropertySheet3(CustomPropertyEditorFactory editorFactory) {
		constructorInit(null, editorFactory, null, null, null, 0, true);
	}
	
	public WildePropertySheet3(T object, CustomPropertyEditorFactory editorFactory, Map<String, UiPosition> fieldUiPositions, String propertyTableName, Map<Class<?>, TemporaryAdditionCallback> listAdditionCallbacks, int preferredNumberOfColumns) {
		constructorInit(object, editorFactory, fieldUiPositions, propertyTableName, listAdditionCallbacks, preferredNumberOfColumns, true);
	}

	public WildePropertySheet3(T object, CustomPropertyEditorFactory editorFactory, Map<String, UiPosition> fieldUiPositions, String propertyTableName, Map<Class<?>, TemporaryAdditionCallback> listAdditionCallbacks, int preferredNumberOfColumns, boolean showListTables) {
		constructorInit(object, editorFactory, fieldUiPositions, propertyTableName, listAdditionCallbacks, preferredNumberOfColumns, showListTables);
	}
	
	/* Ultimate Constructor/Build Method */
	private void constructorInit(T object, CustomPropertyEditorFactory editorFactory, Map<String, UiPosition> fieldUiPositions, String propertyTableName, Map<Class<?>, TemporaryAdditionCallback> listAdditionCallbacks,
			int preferredNumberOfColumns, boolean showListTables) {
		setShowListTables(showListTables);
		if(editorFactory == null) {
			this.editorFactory = new CustomPropertyEditorFactory(false, false, true);
		}else {
			this.editorFactory = editorFactory;
		}
		
		if(listAdditionCallbacks != null) {
			this.listAdditionCallbacks.putAll(listAdditionCallbacks);
		}
		
		initUi();

		if(preferredNumberOfColumns == 0 && object != null && object.getClass().isAnnotationPresent(ClassDescriptor.class)){
			preferredNumberOfColumns = object.getClass().getAnnotation(ClassDescriptor.class).defaultPropertyGridColumns();
		}else{
			preferredNumberOfColumns = DEFAULT_NUM_OF_COLUMNS;
		}

		setPreferredMultiGridNumberOfColumns(preferredNumberOfColumns);
		
//		getMoreInfoFactory = new GetMoreInfoFactory();
		
		if(propertyTableName != null) {
			setPropertyTableName(propertyTableName);
		}else if(object != null) {
			setPropertyTableName(object.getClass().getName());
		}
		
		wildeBeanProperty().addListener((observable, oldValue, newValue) -> {
			if(oldValue == null && initPreferences) {
				initPreferences();
			}else if(newValue != null && oldValue != null && newValue.getClass() == oldValue.getClass() && initPreferences) {
				initPreferences();
			}
		});
		
		if(object != null) {
			if(fieldUiPositions == null) {
				setWildeBean(new WildeBean<T>(object, editorFactory));
			}else {
				setWildeBean(new WildeBean<T>(object, editorFactory, fieldUiPositions));
			}
			
			refreshPropertySheet();
		}
	}
	
	public void setEditorFactory(CustomPropertyEditorFactory editorFactory) {
		this.editorFactory = editorFactory;
	}
	
	public void setObject(T object) {
		if(getWildeBean() == null || getWildeBean().getObject().getClass() != object.getClass()) {
			setPropertyTableName(object.getClass().getName());
		}
		setWildeBean(new WildeBean<T>(object, editorFactory));
		
		refreshPropertySheet();
	}
	
	/* Set Object methods which can be called after Property Sheet has been created */
	public void setObject(T object, String propertyTableName) {
		if(object.getClass().isAnnotationPresent(ClassDescriptor.class)){
			setPreferredMultiGridNumberOfColumns(object.getClass().getAnnotation(ClassDescriptor.class).defaultPropertyGridColumns());
		}else if(getPreferredMultiGridNumberOfColumns() == 0){
			setPreferredMultiGridNumberOfColumns(DEFAULT_NUM_OF_COLUMNS);
		}

		setWildeBean(new WildeBean<T>(object, editorFactory));
		setPropertyTableName(propertyTableName);
		
//		initPreferences();
		refreshPropertySheet();
	}
	
	public void setObject(T object, Map<String, UiPosition> fieldUiPositions) {
		if(object.getClass().isAnnotationPresent(ClassDescriptor.class)){
			setPreferredMultiGridNumberOfColumns(object.getClass().getAnnotation(ClassDescriptor.class).defaultPropertyGridColumns());
		}else if(getPreferredMultiGridNumberOfColumns() == 0){
			setPreferredMultiGridNumberOfColumns(DEFAULT_NUM_OF_COLUMNS);
		}


		if(getWildeBean() == null || getWildeBean().getObject().getClass() != object.getClass()) {
			setPropertyTableName(object.getClass().getName());
		}
		setWildeBean(new WildeBean<T>(object, editorFactory, fieldUiPositions));
		
		refreshPropertySheet();
	}
	
	public void setObject(T object, Map<String, UiPosition> fieldUiPositions, String propertyTableName) {

		if(object.getClass().isAnnotationPresent(ClassDescriptor.class)){
			setPreferredMultiGridNumberOfColumns(object.getClass().getAnnotation(ClassDescriptor.class).defaultPropertyGridColumns());
		}else if(getPreferredMultiGridNumberOfColumns() == 0){
			setPreferredMultiGridNumberOfColumns(DEFAULT_NUM_OF_COLUMNS);
		}


		if(getWildeBean() == null || getWildeBean().getObject().getClass() != object.getClass()) {
			setPropertyTableName(propertyTableName);
		}
		setWildeBean(new WildeBean<T>(object, editorFactory, fieldUiPositions));
		
		refreshPropertySheet();
	}
	
	public void setObject(T object, Map<String, UiPosition> fieldUiPositions, String propertyTableName, boolean initPreferences) {
		this.initPreferences = initPreferences;

		if(object.getClass().isAnnotationPresent(ClassDescriptor.class)){
			setPreferredMultiGridNumberOfColumns(object.getClass().getAnnotation(ClassDescriptor.class).defaultPropertyGridColumns());
		}else if(getPreferredMultiGridNumberOfColumns() == 0){
			setPreferredMultiGridNumberOfColumns(DEFAULT_NUM_OF_COLUMNS);
		}

		if(getWildeBean() == null || getWildeBean().getObject().getClass() != object.getClass()) {
			setPropertyTableName(propertyTableName);
		}
		setWildeBean(new WildeBean<T>(object, editorFactory, fieldUiPositions));
				
		refreshPropertySheet();
	}
	
	public void setDefaultEditSaveButtons() {
		standardEditSavePane = new StandardEditSavePane()
				.addOnEditCallback(() -> setEditable(true))
				.addOnSaveSucceededCallback(() -> setEditable(false))
				.addOnCancelCallback(() -> {
					decommit();
					setEditable(false);
				})
				.addOnSaveCallback(() -> {
					
					/* Create the validation parameters */
					CommitChangesParam<?> validationParam = new CommitChangesParam<>()
							.setRequireNonNullSqlFields(true);
					
					/* Check Validation */
					boolean validated = validateProperties(validationParam);
					if(!validated) {
						return false;
					}
					
					/* Add commit parameters */
					CommitChangesParam<?> commitChangesParam = new CommitChangesParam<>()
							.setTableCommitCallback(WildeDBApplication.getInstance());
					
					/* If object is loggable, add to commit params */
					if(this.getWildeBean().getObject() instanceof Loggable) {
			    		commitChangesParam = commitChangesParam
			    				.setLoggableObject((Loggable) this.getWildeBean().getObject());
			    	}
					
					/* Commit the changes */
					try {
						commitChanges(commitChangesParam);

						// If loggable, find repository and add log items in background task
						if(this.getWildeBean().getObject() instanceof Loggable) {
							Task<Void> sendLogItemsTask = new Task<Void>() {
								@Override
								protected Void call() throws Exception {
									RepositoryUtil.insert(((Loggable<?>) getWildeBean().getObject()).getLogItems());
									return null;
								}
							};
							sendLogItemsTask.setOnSucceeded(event -> log.debug("Log Items updated"));
							sendLogItemsTask.setOnFailed(event -> log.error(ExceptionUtils.getStackTrace(sendLogItemsTask.getException())));
							new Thread(sendLogItemsTask, "sendLogItemsTask").start();
						}
					} catch (Exception throwables) {
						log.error("Error commiting changings");
						throwables.printStackTrace();

					}

					return true;
				});
		
		setBottom(standardEditSavePane);
	}
	
	public StandardEditSavePane getStandardEditSavePane() {
		return standardEditSavePane;
	}
	
	private void initPreferences() {
		
		loadFromPreferences();
		setUpSavePreferences();
        
	}
	
	private void loadFromPreferences() {
		Class<?> beanType = getWildeBean().getObject().getClass();

		log.trace("loading from preferences, table name: {}", getPropertyTableName());
		log.trace("loading from preferences, using table name: {}", (Strings.isNullOrEmpty(getPropertyTableName()) ? beanType.getName():getPropertyTableName()));
		log.trace("propertysheet preferences size: {}", WildeDBApplication.getInstance().getWildePropertySheetModePreferences().size());
		WildeDBApplication.getInstance().getWildePropertySheetModePreferences().forEach((key, value) -> {
			log.trace("Existing pref {}, value: {}", key, value);
		});
        String prefWildeMode = WildeDBApplication.getInstance().getWildePropertySheetModePreferences().get(Strings.isNullOrEmpty(getPropertyTableName()) ? beanType.getName():getPropertyTableName());
        Boolean prefFloatingLabelMode = WildeDBApplication.getInstance().getWildePropertySheetFloatingLabels().get(Strings.isNullOrEmpty(getPropertyTableName()) ? beanType.getName():getPropertyTableName());
        
        if (prefWildeMode != null) {
			log.trace("setting pref mode to {}", prefWildeMode);
            setWildeMode(WildePropertySheetMode.valueOf(prefWildeMode));
        }else if(getWildeMode() == null) {
			log.trace("setting pref mode to default multi grid");
        	setWildeMode(WildePropertySheetMode.MULTIGRID);
        }
        if(prefFloatingLabelMode != null){
            if (!Objects.equals(prefFloatingLabelMode, floatingLabelButton.isSelected())) {
                floatingLabelButton.setSelected(prefFloatingLabelMode);
            }
            editorFactory.setUseFoenix(prefFloatingLabelMode);
        }
	}
	
	private void setUpSavePreferences() {
//		
//		Class<?> beanType = getWildeBean().getObject().getClass();
//		// Add listeners to modify preferences
//        wildeMode.addListener((observable, oldValue, newValue) -> {
//        	
//            AppPreferences.getInstance().getWildePropertySheetModePreferences().put(getPropertyTableName() == null ? beanType.getName():getPropertyTableName(), newValue.toString());
//        });
//        floatingLabelButton.selectedProperty().addListener((observable, oldValue, newValue) -> {
//            AppPreferences.getInstance().getWildePropertySheetFloatingLabels().put(getPropertyTableName() == null ? beanType.getName():getPropertyTableName(), newValue);
//        });
	}
	
	private void initUi() {
		
		/* Init Scroll Pane */
		scroller = new ScrollPane();
        scroller.setFitToWidth(true);
        
        /* Init ToolBar */
        toolbar = new ToolBar();
        toolbar.setFocusTraversable(true);
        
        /* Init Toggle Buttons */
        initModeButton();   
            
        /* Init Search Field */
        initSearchField();
        
        
        toolbar.getItems().addAll(modeButton, floatingLabelButton, searchField); 
        
        setTop(toolbar);
        
        /* Remove the tool bar from the top when both the mode switcher and search box are set visible to false */
        topProperty().bind(Bindings.when(modeSwitcherVisible.or(searchBoxVisible)).then(toolbar).otherwise((ToolBar) null));
	}
	
	private void initModeButton() {
		/* Init Toggle Buttons */
		ToggleButton byNameButton = new ToggleButton("", new ImageView(new Image("list.png")));
        byNameButton.setUserData(WildePropertySheetMode.NAME);
        ToggleButton byCategoryButton = new ToggleButton("", new ImageView(new Image("indent.png")));
        byCategoryButton.setUserData(WildePropertySheetMode.CATEGORY);
        ToggleButton byGridButton = new ToggleButton("", new ImageView(new Image("grid.png")));
        byGridButton.setUserData(WildePropertySheetMode.MULTIGRID);
        ToggleButton byFlowButton = new ToggleButton("", new ImageView(new Image("multiwindow.png")));
        byFlowButton.setUserData(WildePropertySheetMode.WRAPPED_GRIDS);

        modeButton = new SegmentedButton();
        modeButton.getButtons().addAll(byNameButton, byCategoryButton, byGridButton, byFlowButton);
        
        /* For each button, have it set the WildeMode to the WildeMode it was assigned */
        modeButton.getButtons().forEach(button -> button.setOnAction(event -> {
        	setWildeMode((WildePropertySheetMode) button.getUserData());
        	button.setSelected(true);
        }));
        
        /* Set selected the mode button of the initial WildeMode */
        modeButton.getButtons().stream().filter(toggleButton -> Objects.equals(getWildeMode(), toggleButton.getUserData())).findFirst().ifPresent(toggleButton -> toggleButton.setSelected(true));
        
        /* Add listener to the WildeMode itself to select the toggle button and refresh the property sheet */
        wildeMode.addListener((observable, oldValue, newValue) -> {
        		modeButton.getButtons().stream().filter(toggleButton -> Objects.equals(newValue.toString(), toggleButton.getUserData().toString())).findFirst().ifPresent(toggleButton -> toggleButton.setSelected(true));

        		log.trace("Wilde Mode switched to {}", newValue);
        		/* Only refresh the property sheet if and object has been set */
        		if(getWildeBean() != null && getWildeBean().getObject() != null) {
        			Class<?> beanType = getWildeBean().getObject().getClass();

        			log.trace("putting {} with value {} into preferences", getPropertyTableName() == null ? beanType.getName():getPropertyTableName(), newValue.toString());
        			WildeDBApplication.getInstance().getWildePropertySheetModePreferences().put(getPropertyTableName() == null ? beanType.getName():getPropertyTableName(), newValue.toString());
					WildeDBApplication.getInstance().savePreferences();
        			refreshPropertySheet();
        		}
        });
        
        /* Init JPhoenix Floating Lable Toggle Button */
        floatingLabelButton = new ToggleButton();
        floatingLabelButton.setGraphic(new ImageView(new Image("above16.png")));
        
        
        /* JFoenix requires new editors prior to refresh */
        floatingLabelButton.selectedProperty().addListener((observable, oldValue, newValue) -> {
            editorFactory.setUseFoenix(newValue);
            getWildeBean().getNewEditors(editorFactory);
            refreshPropertySheet();
            
            if(getWildeBean() != null && getWildeBean().getObject() != null) {
    			Class<?> beanType = getWildeBean().getObject().getClass();
    			WildeDBApplication.getInstance().getWildePropertySheetFloatingLabels().put(getPropertyTableName() == null ? beanType.getName():getPropertyTableName(), newValue);
    		}
        });
        
	}
	
	private void initSearchField() {
		searchField = TextFields.createClearableTextField();
        searchField.getStylesheets().add(Application.getUserAgentStylesheet());
        searchField.setPromptText("Search");
        searchField.setMinWidth(0);
        HBox.setHgrow(searchField, Priority.SOMETIMES);
        
        /* Allow the user to finish typing prior to refreshing the property sheet */
        timer = new Timer();
        searchField.textProperty().addListener((observable, oldValue, newValue) -> {
        	timer.cancel();
            timer = new Timer();
            timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    // Must run on the UI thread
                    Platform.runLater(() -> {
                    	refreshPropertySheet();                    	
                    });
                }
            }, 200);
        });
        titleFilter.bindBidirectional(searchField.textProperty());
        
        
	}
	
	/**
	 * Essentially a wrapper depending on if ScrollPane is requested or not
	 * This method calls buildPropertySheetContainer() and sets the result to the container
	 */
	public void refreshPropertySheet() {

		log.trace("Refreshing property sheet.  Mode is {}", getWildeMode());

        if(useScrollPane){

            propertySheetGrid = (Region) buildPropertySheetContainer();
            if(editorFactory.isUseFoenix()){
                propertySheetGrid.setStyle("-fx-background-color: derive(-fx-base, +12%);" +
                        "-fx-background-radius: 10;" +
                        "-fx-effect: dropshadow(two-pass-box , -fx-shadow-highlight-color, 4, 0.0 , 0, 1.4);");
            }

            scroller.setContent(propertySheetGrid);
            setCenter(scroller);
        }else{
            propertySheetGrid = (Region) buildPropertySheetContainer();
            if(editorFactory.isUseFoenix()){
                propertySheetGrid.setStyle("-fx-background-color: derive(-fx-base, +12%);" +
                        "-fx-background-radius: 10;" +
                        "-fx-effect: dropshadow(two-pass-box , -fx-shadow-highlight-color, 4, 0.0 , 0, 1.4);");
            }
            setCenter(propertySheetGrid);
        }

    }
	
	/**
	 * Depending on the WildeMode category, the method will create the property sheet
	 * @return Node, the property sheet
	 */
	private Node buildPropertySheetContainer() {
		
		// Get blank slate of editors
//		getWildeBean().getNewEditors(editorFactory);
		
		switch(getWildeMode() ) {

		case CATEGORY:
			return buildCategoryNode();

		case WRAPPED_GRIDS:
			return buildWrappedGridsNode();

		case MULTIGRID:
			VBox vBoxContainer = new VBox();
            PropertyGridPane propertyGridPane = new PropertyGridPane(getWildeBean(), 0);
            vBoxContainer.getChildren().setAll(propertyGridPane);
            return vBoxContainer;

		default:
			PropertyPane props = new PropertyPane(getWildeBean());
            VBox vBoxContainer3 = new VBox();
            vBoxContainer3.getChildren().add(props);
            return vBoxContainer3;
		}
	}
	
	private Node buildCategoryNode() {
		Map<String, List<WildeBeanProperty<?>>> categoryMap = new TreeMap<>();
        for(WildeBeanProperty<?> p: getWildeBean().getWildeBeanProperties()) {
            String category = p.getCategory();
            List<WildeBeanProperty<?>> list = categoryMap.get(category);
            if ( list == null ) {
                list = new ArrayList<>();
                categoryMap.put( category, list);
            }
            list.add(p);
        }

        // create category-based accordion
        Accordion accordion = new Accordion();
        for( String category: categoryMap.keySet() ) {
            PropertyPane props = new PropertyPane(categoryMap.get(category));
            VBox vBoxContainer = new VBox();
            vBoxContainer.getChildren().setAll(props);
            // Only show non-empty categories
            if ( props.getChildrenUnmodifiable().size() > 0 ) {
                TitledPane pane = new TitledPane( category, vBoxContainer );
                pane.setExpanded(true);
                accordion.getPanes().add(pane);
            }
        }
        
        /* Now Create titled panes for WildeListBeans */
        for(WildeListBean<T, ?> wildeListBean : getWildeBean().getListBeans()) {
        	String category = wildeListBean.getTitle();
        	BorderPane borderPane = new BorderPane(getWildeBeanEditableTable(wildeListBean));
        	TitledPane pane = new TitledPane( category, borderPane);
            pane.setExpanded(true);
            accordion.getPanes().add(pane);
        }
        
        if ( accordion.getPanes().size() > 0 ) {
            accordion.setExpandedPane(accordion.getPanes().get(0));
        }
        return accordion;
	}
	
	private Node buildWrappedGridsNode() {
		
		Map<String, List<WildeBeanProperty<?>>> categoryMap = new TreeMap<>();
        ArrayList<String> orderedCategories = new ArrayList<>();
        ClassDescriptor classDescriptor = getWildeBean().getObject().getClass().getAnnotation(ClassDescriptor.class);
        if (classDescriptor != null && classDescriptor.orderedDisplayCategories() != null && classDescriptor.orderedDisplayCategories().length > 0) {
            orderedCategories.addAll(Arrays.asList(classDescriptor.orderedDisplayCategories()));
            for(String category : orderedCategories){
                categoryMap.put(category, new ArrayList<WildeBeanProperty<?>>());
            }
        }
        
        
        for(WildeBeanProperty<?> p: getWildeBean().getWildeBeanProperties()) {
            String category = p.getCategory();
            List<WildeBeanProperty<?>> list = categoryMap.get(category);
            if ( list == null ) {
                orderedCategories.add(category);
                list = new ArrayList<>();
                categoryMap.put( category, list);
            }
            list.add(p);
        }
        // create different grids based on category
        FlowPane flowPane = new FlowPane();
        flowPane.setOrientation(Orientation.HORIZONTAL);
        for( String category: orderedCategories ) {
            List<WildeBeanProperty<?>> categoryBeanProperties = categoryMap.get(category);
            Pane pane = null;
            if(categoryBeanProperties.size() > 5) {

                pane = new PropertyGridPane(categoryMap.get(category), 5);
            }else{
                pane = new PropertyPane(categoryMap.get(category));
            }
            
            // Only show non-empty categories
            if ( categoryBeanProperties.size() > 0 ) {
                Node borderedPropertyPane = Borders.wrap(pane).lineBorder().title(category).color(Color.GRAY).buildAll();
                flowPane.getChildren().add(borderedPropertyPane);
            }
        }
        
        /* Now Create bordered panes for WildeListBeans */
        for(WildeListBean<T, ?> wildeListBean : getWildeBean().getListBeans()) {
        	String category = wildeListBean.getTitle();
        	Node borderedPropertyPane = Borders.wrap(getWildeBeanEditableTable(wildeListBean)).lineBorder().title(category).color(Color.GRAY).buildAll();
        	// ((Region) borderedPropertyPane).setPrefWidth(getWildeBeanEditableTable(wildeListBean).getTableView().getColumns().stream().mapToDouble(column -> column.getWidth() * 2).sum());
        	// For now just add it to the second to last position so they're before the TextArea's, later best to create a solution to add a category to the uilist annotation
			((Region) borderedPropertyPane).setPrefWidth(400);
			((Region) borderedPropertyPane).maxWidthProperty().bind(widthProperty());
			flowPane.getChildren().add(flowPane.getChildren().size()-1, borderedPropertyPane);
        }
        return flowPane;
	}

	private WildeBeanProperty.GetMoreInfoCallback getMoreInfoCallback(WildeBeanProperty item) {
//        if (getMoreInfoFactory != null)
//            return getMoreInfoFactory.call(item);
//        return null;
        return WildeDBApplication.getInstance().call(item);
    }
	
	private static final int MIN_COLUMN_WIDTH = 100;
	
	private class PropertyPane extends VBox {
		
		private GridPane gridPane;
		
		public PropertyPane(List<WildeBeanProperty<?>> properties) {
			
            initGridPane(properties);
		}
		
		public PropertyPane(WildeBean<T> wildeBean) {
			
            initGridPane(wildeBean.getWildeBeanProperties());
            createEditableTables(wildeBean);
		}
		
		private void initGridPane(List<WildeBeanProperty<?>> properties) {
			gridPane = new GridPane();
			if (editorFactory.isUseFoenix() && editorFactory.isFloatLabel()) {
                VBox.setMargin(gridPane, new Insets(10, 0, 0, 0));
                gridPane.setVgap(20);
//                setStyle("-fx-background-color: -fx-base");
            } else {
                gridPane.setVgap(5);
            }
            gridPane.setHgap(5);
            setPadding(new Insets(5, 15, 5, 15));
//            getStyleClass().add("property-pane"); //$NON-NLS-1$
            setItems(properties);

            getChildren().add(gridPane);
            
            if(!editorFactory.isUseFoenix()) {
            	ColumnConstraints secondColumn = new ColumnConstraints();
//            	secondColumn.setPrefWidth(200);
            	secondColumn.setHgrow(Priority.ALWAYS);
            	secondColumn.setFillWidth(true);

            	gridPane.getColumnConstraints().addAll(new ColumnConstraints(), secondColumn);
            }else {
            	ColumnConstraints secondColumn = new ColumnConstraints();
//            	secondColumn.setPrefWidth(200);
            	secondColumn.setHgrow(Priority.ALWAYS);
            	secondColumn.setFillWidth(true);

            	gridPane.getColumnConstraints().addAll(secondColumn);
            }
		}
		
		private void createEditableTables(WildeBean<T> wildeBean) {
        	for(WildeListBean<T, ?> listBean : wildeBean.getListBeans()) {
        		this.getChildren().add(getWildeBeanEditableTable(listBean));        		
        	}        	
        }
		
		public void setItems(List<WildeBeanProperty<?>> properties) {
			getChildren().clear();

            String filter = getTitleFilter();
            filter = filter == null ? "" : filter.trim().toLowerCase(); //$NON-NLS-1$

            int row = 0;
            
            for (WildeBeanProperty item : properties) {
				if(item.getUiProperty().isUnitsField()) continue;

            	// filter properties
                String title = item.getName();
                if (!filter.isEmpty() && title.toLowerCase().indexOf(filter) < 0) continue;
                
                if (!editorFactory.isUseFoenix() || (editorFactory.isUseFoenix() && !editorFactory.isFloatLabel())) {
                    // setup property label
                    Label label = new Label(title);
                    label.setPrefWidth(Region.USE_COMPUTED_SIZE);
                    label.setMinWidth(Region.USE_PREF_SIZE);

                    // show description as a tooltip
                    String description = item.getDescription();
                    if (description != null && !description.trim().isEmpty()) {
                        label.setTooltip(new Tooltip(description));
                    }

                    WildeBeanProperty.GetMoreInfoCallback infoCallback = getMoreInfoCallback(item);
                    HBox labelHolder = null;
                    if (infoCallback != null) {
                        labelHolder = new HBox();
                        addGetMoreInfoNode(labelHolder, label, item, infoCallback);
                    }


                    gridPane.add(labelHolder == null ? label : labelHolder, 0, row);

                    // setup property editor
                    WildePropertyEditor<?> WildePropertyEditor = item.getPropertyEditor();
                    Node editor = WildePropertyEditor.getEditor();
                    item.setPropertyEditor(WildePropertyEditor);

                    if (editor instanceof Region) {
//                        ((Region) editor).setMinWidth(MIN_COLUMN_WIDTH);
                        ((Region) editor).setMaxWidth(Double.MAX_VALUE);
                    }
                    
//                    if(editor instanceof TextArea) {
//                    	((Region) editor).setMinWidth(1000);
//                    }
                    
                    label.setLabelFor(editor);

					HBox editorHolder = null;
					if(!item.getUiProperty().unitsField().isEmpty()){
						// Get the units property editor and place in hbox
						Node unitsEditor = getWildeBean().getWildeBeanProperty(item.getUiProperty().unitsField()).getPropertyEditor().getEditor();

						HBox.setHgrow(editor, Priority.ALWAYS);
						HBox.setHgrow(unitsEditor, Priority.SOMETIMES);
						((Region) unitsEditor).setMinWidth(60);
						editorHolder = new HBox(2, editor, unitsEditor);
					}

                    gridPane.add(editorHolder == null ? editor:editorHolder, 1, row);
//                    GridPane.setHgrow(editor, Priority.ALWAYS);
                    

                    row++;
                } else {
                    WildePropertyEditor<?> WildePropertyEditor = item.getPropertyEditor();
                    Node editor = WildePropertyEditor.getEditor();
                    item.setPropertyEditor(WildePropertyEditor);
                    if (editor instanceof Region) {
//                        ((Region) editor).setMinWidth(MIN_COLUMN_WIDTH);
                        ((Region) editor).setMaxWidth(Double.MAX_VALUE);
                        
                        
                    }

                    WildeBeanProperty.GetMoreInfoCallback infoCallback = getMoreInfoCallback(item);
                    HBox labelHolder = null;
                    if(infoCallback != null){
                        labelHolder = new HBox();
                        addGetMoreInfoNode(labelHolder, null, item, infoCallback);
                        
                        labelHolder.getChildren().add(editor);
                    }

					if(!item.getUiProperty().unitsField().isEmpty()){
						// Get the units property editor and place in hbox
						WildeBeanProperty<?> unitsProperty = getWildeBean().getWildeBeanProperty(item.getUiProperty().unitsField());
						Node unitsEditor = unitsProperty.getPropertyEditor().getEditor();
						FoenixUtil.setTitleAndFloat(unitsEditor, unitsProperty.getName());

						if(labelHolder == null){
							labelHolder = new HBox(editor, unitsEditor);
						}else{
							labelHolder.getChildren().add(unitsEditor);
						}

					}
                    
//                    if(editor instanceof TextArea) {
//                    	((Region) editor).setMinWidth(1000);
//                    }

                    FoenixUtil.setTitleAndFloat(editor, title);
                    gridPane.add(labelHolder == null ? editor : labelHolder, 0, row);
//                    GridPane.setHgrow(editor, Priority.ALWAYS);

                    row++;

                }
                
            }
        }
		
		
	}
	
	private class PropertyGridPane extends VBox {

        private GridPane upperGridPane;
        private GridPane lowerGridPane;
        private VBox listPropertiesVBox;
        private int maxNumberOfRowsPerColumn;

        public PropertyGridPane( WildeBean<T> wildeBean ) {
            this( wildeBean, 0 );
        }

        public PropertyGridPane(WildeBean<T> wildeBean, int maxNumberOfRowsPerColumn ) {

        	initUi();
        	this.maxNumberOfRowsPerColumn = maxNumberOfRowsPerColumn;

            setItems(wildeBean.getWildeBeanProperties());

			getStylesheets().add(Application.getUserAgentStylesheet());
            getChildren().addAll(upperGridPane, lowerGridPane);
//            setGridLinesVisible(true);
            
            createEditableTables(wildeBean);
        }
        
        public PropertyGridPane( List<WildeBeanProperty<?>> properties ) {
            this( properties, 0 );
        }

        public PropertyGridPane(List<WildeBeanProperty<?>> properties, int maxNumberOfRowsPerColumn ) {
        	initUi();
        	this.maxNumberOfRowsPerColumn = maxNumberOfRowsPerColumn;
            setItems(properties);

            getChildren().addAll(upperGridPane, lowerGridPane);
        }
        
        private void initUi() {
        	upperGridPane = new GridPane();
			upperGridPane.getStylesheets().add(Application.getUserAgentStylesheet());
			upperGridPane.getStyleClass().add("vertical-lines-grid");
            lowerGridPane = new GridPane();
            
//            setItems(wildeBean.getWildeBeanProperties());
            
            listPropertiesVBox = new VBox();
//            upperGridPane.setGridLinesVisible(true);
            this.maxNumberOfRowsPerColumn = maxNumberOfRowsPerColumn;

            
            if (editorFactory.isUseFoenix() && editorFactory.isFloatLabel()) {
                upperGridPane.setVgap(20);
                lowerGridPane.setVgap(20);
                VBox.setMargin(lowerGridPane, new Insets(10,0,0,0));

                VBox.setMargin(upperGridPane, new Insets(10,0,0,0));
                VBox.setMargin(lowerGridPane, new Insets(20,0,0,0));
                ColumnConstraints secondColumn = new ColumnConstraints();
                secondColumn.setPrefWidth(150);
                ColumnConstraints fourthColumn = new ColumnConstraints();
                fourthColumn.setPrefWidth(150);
                secondColumn.setHgrow(Priority.ALWAYS);
                fourthColumn.setHgrow(Priority.ALWAYS);

                upperGridPane.getColumnConstraints().addAll(secondColumn, fourthColumn);


//                setStyle("-fx-background-color: -fx-base");
                
                ColumnConstraints lowerSecondColumn = new ColumnConstraints();
                lowerSecondColumn.setPrefWidth(200);
                lowerSecondColumn.setHgrow(Priority.ALWAYS);
                lowerSecondColumn.setFillWidth(true);

                lowerGridPane.getColumnConstraints().addAll(lowerSecondColumn);


            }else{
                upperGridPane.setVgap(5);
                lowerGridPane.setVgap(5);
                VBox.setMargin(lowerGridPane, new Insets(20,0,0,0));

                ColumnConstraints spacerColumn = new ColumnConstraints();
                spacerColumn.setPrefWidth(10);
                spacerColumn.setMaxWidth(10);
                spacerColumn.setMinWidth(10);

                ColumnConstraints secondColumn = new ColumnConstraints();
                secondColumn.setPrefWidth(200);
                secondColumn.setHgrow(Priority.ALWAYS);
                secondColumn.setFillWidth(true);
                ColumnConstraints fourthColumn = new ColumnConstraints();
                fourthColumn.setPrefWidth(200);
                fourthColumn.setHgrow(Priority.ALWAYS);
                fourthColumn.setFillWidth(true);

                upperGridPane.getColumnConstraints().addAll(new ColumnConstraints(), secondColumn, new ColumnConstraints(), fourthColumn);

//                for (int i = 0; i < getPreferredMultiGridNumberOfColumns(); i++) {
//                	ColumnConstraints secondColumn = new ColumnConstraints();
//                    secondColumn.setPrefWidth(200);
//                    secondColumn.setHgrow(Priority.ALWAYS);
//                    upperGridPane.getColumnConstraints().addAll(new ColumnConstraints(), secondColumn);
//				}

                ColumnConstraints lowerSecondColumn = new ColumnConstraints();
                lowerSecondColumn.setPrefWidth(200);
                lowerSecondColumn.setHgrow(Priority.ALWAYS);
                lowerSecondColumn.setFillWidth(true);

                lowerGridPane.getColumnConstraints().addAll(new ColumnConstraints(), lowerSecondColumn);

            }
            
            



            upperGridPane.setHgap(5);
            lowerGridPane.setHgap(5);


//            setStyle("-fx-background-color: -fx-base");
//            lowerGridPane.setStyle("-fx-background-color: -fx-base");

            setPadding(new Insets(5, 15, 5, 15 ));
//            getStyleClass().add("property-pane"); //$NON-NLS-1$
        }
        
        private void createEditableTables(WildeBean<T> wildeBean) {
        	
        	for(WildeListBean<T, ?> listBean : wildeBean.getListBeans()) {
        		WildeEditableBeanTable<?> wildeEditableBeanTable = getWildeBeanEditableTable(listBean);
//        		if(wildeEditableBeanTable.getParent() != null) {
//        		((Pane) wildeEditableBeanTable.getParent()).getChildren().remove(wildeEditableBeanTable);
//        		}
        		
        		this.getChildren().add(1, wildeEditableBeanTable);        		
        	}        	
        }

        public void setItems( List<WildeBeanProperty<?>> properties ) {
            getChildren().clear();

            String filter = getTitleFilter();
            filter = filter == null? "": filter.trim().toLowerCase(); //$NON-NLS-1$
            int lowerCol = 0;
            int lowerRow = 0;
            int upperCol = 0;
            int upperRow = 0;
            int numberOfColumns;
            boolean customCells = false;
            if(maxNumberOfRowsPerColumn > 0){
                numberOfColumns = (int) Math.ceil((double) properties.size() / maxNumberOfRowsPerColumn);
                customCells = false;
            }else {
                customCells = !properties.isEmpty() && properties.get(0).getColPreference() >= 0;
                numberOfColumns = getPreferredMultiGridNumberOfColumns();
                log.trace("Number of columns set for {} is {}", getWildeBean().getObject().getClass().getSimpleName(), numberOfColumns);
            }

            int numGridProperties = (int) properties.stream().filter(wildeBeanProperty -> !wildeBeanProperty.getUiProperty().preferredEditor().equals("TextArea")).count();
            log.debug("Number of grid properties: {}", numGridProperties);
			int rowsPerColumn;
			while(numGridProperties % numberOfColumns != 0){
				numGridProperties++;
			}
			rowsPerColumn = (numGridProperties / numberOfColumns);

			log.debug("rows per column: {}", rowsPerColumn);

            // Set constraints on any additional columns
            if (editorFactory.isUseFoenix() && editorFactory.isFloatLabel()) {
                if(numberOfColumns > 2){
                    ColumnConstraints sixthColumn = new ColumnConstraints();
                    sixthColumn.setPrefWidth(200);
                    sixthColumn.setHgrow(Priority.ALWAYS);
                    upperGridPane.getColumnConstraints().addAll(sixthColumn);
                }

                if(numberOfColumns > 3){
                    ColumnConstraints eighthColumn = new ColumnConstraints();
                    eighthColumn.setPrefWidth(200);
                    eighthColumn.setHgrow(Priority.ALWAYS);
                    upperGridPane.getColumnConstraints().addAll(eighthColumn);
                }
            }else{
                if(numberOfColumns > 2){
                    ColumnConstraints sixthColumn = new ColumnConstraints();
                    sixthColumn.setPrefWidth(200);
                    sixthColumn.setHgrow(Priority.ALWAYS);
                    sixthColumn.setFillWidth(true);
                    upperGridPane.getColumnConstraints().addAll(new ColumnConstraints(), sixthColumn);
                }

                if(numberOfColumns > 3){
                    ColumnConstraints eighthColumn = new ColumnConstraints();
                    eighthColumn.setPrefWidth(200);
                    eighthColumn.setHgrow(Priority.ALWAYS);
                    eighthColumn.setFillWidth(true);
                    upperGridPane.getColumnConstraints().addAll(new ColumnConstraints(), eighthColumn);
                }
            }



//            boolean customCells = !properties.isEmpty() && properties.get(0).getColPreference() >= 0;
//            customCells = false;

            for (WildeBeanProperty item : properties) {
            	if(item.getUiProperty().isUnitsField()) continue;

                // filter properties
                String title = item.getName();

                if ( !filter.isEmpty() && title.toLowerCase().indexOf( filter ) < 0) continue;

                // setup property label


                // NOT FLOATING LABELS
                if(!editorFactory.isUseFoenix() || (editorFactory.isUseFoenix() && !editorFactory.isFloatLabel())) {

                    Label label = new Label(title);
                    label.setMinWidth(MIN_COLUMN_WIDTH);

                    // show description as a tooltip
                    String description = item.getDescription();
                    if (description != null && !description.trim().isEmpty()) {
                        label.setTooltip(new Tooltip(description));
                    }

                    WildeBeanProperty.GetMoreInfoCallback infoCallback = getMoreInfoCallback(item);

//                    StackPane labelHolder = null;
//                    if(infoCallback != null){
//                        labelHolder = new StackPane();
//                        labelHolder.setAlignment(Pos.CENTER_LEFT);
//                        HBox infoButtonHolder = new HBox();
//                        infoButtonHolder.setAlignment(Pos.CENTER_RIGHT);
//                        GlyphButton infoButton = new GlyphButton(item.getUiProperty().infoIconName());
//                        infoButtonHolder.getChildren().add(infoButton);
//                        infoButton.setOnAction(event -> infoCallback.onGetMoreInfo());
//                        labelHolder.getChildren().addAll(label, infoButtonHolder);
//                    }

                    HBox labelHolder = null;
                    if(infoCallback != null){
                        labelHolder = new HBox();
                        addGetMoreInfoNode(labelHolder, label, item, infoCallback);
                    }

                    // setup property editor
                    WildePropertyEditor<?> wildePropertyEditor = item.getPropertyEditor();
                    Node editor = wildePropertyEditor.getEditor();



                    // Location of the label will depend on if the text editor is a TextArea of not, as these go at the bottom

                    if(customCells) {
//                        
                        if(editor instanceof TextArea){
                            lowerGridPane.add(labelHolder == null ? label : labelHolder, lowerCol*2, lowerRow);
                        }else {

                            upperGridPane.add(labelHolder == null ? label : labelHolder, item.getColPreference() * 2, item.getRowPreference());
                        }
                    }else{
                        if(editor instanceof TextArea){
                            lowerGridPane.add(labelHolder == null ? label : labelHolder, lowerCol*2, lowerRow);
                        }else{

                            upperGridPane.add(labelHolder == null ? label : labelHolder, upperCol*2, upperRow);
                        }

                    }



                    item.setPropertyEditor(wildePropertyEditor);

                    if (editor instanceof Region) {
//                        ((Region) editor).setMinWidth(MIN_COLUMN_WIDTH);
                        ((Region) editor).setMaxWidth(Double.MAX_VALUE);
                        
                        
                    }
                    label.setLabelFor(editor);


                    HBox editorHolder = null;
                    if(!item.getUiProperty().unitsField().isEmpty()){
                    	// Get the units property editor and place in hbox
						Node unitsEditor = getWildeBean().getWildeBeanProperty(item.getUiProperty().unitsField()).getPropertyEditor().getEditor();

						HBox.setHgrow(editor, Priority.ALWAYS);
						HBox.setHgrow(unitsEditor, Priority.SOMETIMES);
						((Region) unitsEditor).setMinWidth(60);
						editorHolder = new HBox(2, editor, unitsEditor);
					}

                    if(customCells) {
                        if(editor instanceof TextArea){
                            lowerGridPane.add(editorHolder == null ? editor:editorHolder, lowerCol*2+1, lowerRow);
                            lowerRow++;
                        }else {
							if(item.getColPreference() < getPreferredMultiGridNumberOfColumns()){
								GridPane.setMargin(editor, new Insets(0,20, 0,0));
							}
                            upperGridPane.add(editorHolder == null ? editor:editorHolder, item.getColPreference() * 2 + 1, item.getRowPreference());
                        }
                    }else{
                        if(editor instanceof TextArea){
                            lowerGridPane.add(editorHolder == null ? editor:editorHolder, lowerCol*2+1, lowerRow);
                            lowerRow++;
                        }else {
							if(upperCol < getPreferredMultiGridNumberOfColumns()){
								GridPane.setMargin(editor, new Insets(0,20, 0,0));
							}
                        	
                            upperGridPane.add(editorHolder == null ? editor:editorHolder, upperCol * 2 + 1, upperRow);
                            upperRow++;
                            if (upperRow >= rowsPerColumn) {
                                upperRow = 0;
                                upperCol++;
                            }
//                            upperCol++;
//                            if(upperCol > numberOfColumns-1){
//                                upperCol = 0;
//                                upperRow++;
//                            }
                        }
                    }
//                    GridPane.setHgrow(editor, Priority.ALWAYS);
                }else{
                    WildeBeanProperty.GetMoreInfoCallback infoCallback = getMoreInfoCallback(item);


                    // label is part of the editor node
                    WildePropertyEditor<?> WildePropertyEditor = item.getPropertyEditor();
                    Node editor = WildePropertyEditor.getEditor();
                    item.setPropertyEditor(WildePropertyEditor);

                    HBox labelHolder = null;
                    if(infoCallback != null){
                        labelHolder = new HBox();
                        addGetMoreInfoNode(labelHolder, null, item, infoCallback);
                        
                        labelHolder.getChildren().add(editor);

                    }

					if(!item.getUiProperty().unitsField().isEmpty()){
						// Get the units property editor and place in hbox
						WildeBeanProperty<?> unitsProperty = getWildeBean().getWildeBeanProperty(item.getUiProperty().unitsField());
						Node unitsEditor = unitsProperty.getPropertyEditor().getEditor();
						FoenixUtil.setTitleAndFloat(unitsEditor, unitsProperty.getName());

						if(labelHolder == null){
							labelHolder = new HBox(editor, unitsEditor);
						}else{
							labelHolder.getChildren().add(unitsEditor);
						}

					}

                    if (editor instanceof Region) {
//                        ((Region) editor).setMinWidth(MIN_COLUMN_WIDTH);
                        ((Region) editor).setMaxWidth(Double.MAX_VALUE);
                        
                        if(labelHolder != null) {
                        	HBox.setHgrow(editor, Priority.ALWAYS);
                        }
                    }

                    FoenixUtil.setTitleAndFloat(editor, title);





                    if(customCells) {
                        if(editor instanceof TextArea){
                            lowerGridPane.add(labelHolder == null ? editor : labelHolder, lowerCol, lowerRow);
                            lowerRow++;
                        }else {
                            upperGridPane.add(labelHolder == null ? editor : labelHolder, item.getColPreference(), item.getRowPreference());
                        }
                    }else{
                        if(editor instanceof TextArea){
                            lowerGridPane.add(labelHolder == null ? editor : labelHolder, lowerCol, lowerRow);
                            lowerRow++;
                        }else {
                            upperGridPane.add(labelHolder == null ? editor : labelHolder, upperCol, upperRow);

//                            
                            upperRow++;
                            if (upperRow >= (rowsPerColumn - 1)) {
                                upperRow = 0;
                                upperCol++;
                            }

//                            upperCol++;
//                            if(upperCol > numberOfColumns-1){
//                                upperCol = 0;
//                                upperRow++;
//                            }
                        }
                    }

//                    GridPane.setHgrow(editor, Priority.ALWAYS);
                }


            }

        }
	}
	
	private void addGetMoreInfoNode(HBox labelHolder, Label label, WildeBeanProperty item, GetMoreInfoCallback infoCallback) {
    	
        labelHolder.setAlignment(Pos.CENTER_LEFT);
        
        if(label != null)
        	labelHolder.getChildren().add(label);
        
        if(item.getUiProperty().moreInfos().length > 0) {
        	
        	if(label != null) {
        		Pane expandingPane = new Pane();
        		expandingPane.setPrefWidth(0);
        		HBox.setHgrow(expandingPane, Priority.ALWAYS);
        		labelHolder.getChildren().add(expandingPane);
        	}
        	labelHolder.setSpacing(5);
        	
        	if(item.getUiProperty().useEllipsisMoreInfo()) {
        		if(item.getUiProperty().moreInfos().length == 1) {
                	GlyphButton infoButton = new GlyphButton(item.getUiProperty().moreInfos()[0].infoIconName());
                    infoButton.setOnAction(event -> infoCallback.onGetMoreInfo(item.getUiProperty().moreInfos()[0].showMoreInfoMethod()));
                    labelHolder.getChildren().add(infoButton);
                }else {
                	GlyphButton ellipsesButton = new GlyphButton(GlyphButton.ELLIPSIS_V);
                    labelHolder.getChildren().add(ellipsesButton);
                    
                    CirclePopupMenu menu = new CirclePopupMenu(ellipsesButton, MouseButton.PRIMARY);
                    menu.setAnimationInterpolation(CircularPane::animateOverTheArcWithFade);
                    //menu.getCircularPane().setGap(40d);
                    
                    for(MoreInfo moreInfo : item.getUiProperty().moreInfos()) {
                    	Node graphic;
                    	//TODO Will likely need a different mechanism to pull in png from other projects
                    	if(moreInfo.infoIconName().contains(".png")) {
                    		graphic = new ImageView(new Image(moreInfo.infoIconName()));
                    		((ImageView) graphic).setFitHeight(50);
                    		((ImageView) graphic).setFitWidth(50);
                    	}else {
                    		graphic = GlyphButton.getGlyphIcon(moreInfo.infoIconName());
                        	((GlyphIcon<?>) graphic).setGlyphSize(20);
                    	}
                    	
                    	MenuItem popupMenuItem = new MenuItem(moreInfo.tooltip(), graphic);
                    	popupMenuItem.setOnAction(event -> infoCallback.onGetMoreInfo(moreInfo.showMoreInfoMethod()));
                    	menu.getItems().add(popupMenuItem);
                    	
                    	
                    }
                    
                }
        	}else {
        		for(MoreInfo moreInfo : item.getUiProperty().moreInfos()) {
                	Node graphic;
                	if(moreInfo.infoIconName().contains(".png")) {
                		graphic = new ImageView(new Image(moreInfo.infoIconName()));
                		((ImageView) graphic).setFitHeight(10);
                		((ImageView) graphic).setFitWidth(10);
                	}else {
                		graphic = GlyphButton.getGlyphIcon(moreInfo.infoIconName());
                    	((GlyphIcon<?>) graphic).setGlyphSize(20);
                	}

                	graphic.setOnMouseClicked(event -> infoCallback.onGetMoreInfo(moreInfo.showMoreInfoMethod()));
                	
                	labelHolder.getStylesheets().add(Application.getUserAgentStylesheet());
                    labelHolder.getChildren().add(graphic);
                }
        	}        	
        	
        }
    }

	public static interface WildeBeanValidationCallback<T>{
		boolean performCustomValidation(WildeBean<T> wildeBean);
	}
	
	/*
	 * Settings, Commits, Decommits
	 */
	public BooleanProperty editableProperty() {
		return this.editable;
	}	

	public boolean isEditable() {
		return this.editableProperty().get();
	}
	
	public void setEditable(boolean editable) {
		this.editable.set(editable);
        // set all the current editors to the choice but also set the factory to the choice for if refreshes occur
		if(getWildeBean() != null) {
			getWildeBean().getWildeBeanProperties().forEach(item -> {
				if(item.getPropertyEditor() != null){
					log.trace("Setting item {} editable to {}", item.getFieldName(), (editable && item.isEverEditable()));
					item.getPropertyEditor().setEditable(editable && item.isEverEditable());
				}
			});
			// If editors get rebuilt, the default of the factory needs to be changed
			editorFactory.setEditable(editable);

			// Set editable of any WildeListBeans
			getWildeBean().getListBeans().forEach(wildeListBean -> getWildeBeanEditableTable(wildeListBean).getTableView().setEditable(editable));
			
			// Commenting this out, set it to be bound in WildeEditableTable
			//getWildeBean().getListBeans().forEach(wildeListBean -> getWildeBeanEditableTable(wildeListBean).setAddNewButtonVisible(editable));
		}
    }
	
	public void decommit() {
		getWildeBean().getWildeBeanProperties().forEach(item -> {

            if(item.getPropertyEditor() == null){
//                
            }else{
                if (item.getUiProperty().editable()) {
                    WildePropertyEditor propertyEditor = item.getPropertyEditor();
                    propertyEditor.setValue(item.getValue());
                }

            }

        });
		
		// Decommit all listBeans
		getWildeBean().getListBeans().forEach(wildeListBean -> getWildeBeanEditableTable(wildeListBean).decommit());
	}
	
	/**
	 * Utility builder class to make it easier to construct the params that go into the commit methodommit
	 * @author 1115095
	 *
	 */
	public static class CommitChangesParam<T>{
		private Loggable loggableObject;
		private TableCommitCallback<?> tableCommitCallback;
		private boolean requireNonNullSqlFields = false;
		private WildeBeanValidationCallback<T> wildeBeanValidationCallback;
		private Map<Class<?>, WildeListBeanValidationCallback<?>> customValidators;
		private String loggablePrefix;
		
		public CommitChangesParam<T> setLoggableObject(Loggable loggableObject) {
			this.loggableObject = loggableObject;
			return this;
		}
		public CommitChangesParam<T> setTableCommitCallback(TableCommitCallback<?> tableCommitCallback) {
			this.tableCommitCallback = tableCommitCallback;
			return this;
		}
		public CommitChangesParam<T> setRequireNonNullSqlFields(boolean requireNonNullSqlFields) {
			this.requireNonNullSqlFields = requireNonNullSqlFields;
			return this;
		}
		public CommitChangesParam<T> setWildeBeanValidationCallback(WildeBeanValidationCallback<T> wildeBeanValidationCallback) {
			this.wildeBeanValidationCallback = wildeBeanValidationCallback;
			return this;
		}
		public CommitChangesParam<T> setCustomValidators(Map<Class<?>, WildeListBeanValidationCallback<?>> customValidators) {
			this.customValidators = customValidators;
			return this;
		}
		public CommitChangesParam<T> setLoggablePrefix(String loggablePrefix){
			this.loggablePrefix = loggablePrefix;
			return this;
		}
		public Loggable getLoggableObject() {
			return loggableObject;
		}
		public TableCommitCallback<?> getTableCommitCallback() {
			return tableCommitCallback;
		}
		public boolean isRequireNonNullSqlFields() {
			return requireNonNullSqlFields;
		}
		public WildeBeanValidationCallback<T> getWildeBeanValidationCallback() {
			return wildeBeanValidationCallback;
		}
		public Map<Class<?>, WildeListBeanValidationCallback<?>> getCustomValidators() {
			return customValidators;
		}
		public String getLoggablePrefix() {
			return loggablePrefix;
		}
		
		
		
	}
	
	public boolean validateProperties(CommitChangesParam param) {
		return validateProperties(param.isRequireNonNullSqlFields(), param.getWildeBeanValidationCallback(), param.getCustomValidators());
	}
	
	public boolean validateProperties(boolean requireNonNullSqlFields, WildeBeanValidationCallback<T> wildeBeanValidationCallback,
			Map<Class<?>, WildeListBeanValidationCallback<?>> customValidators) {
		/* First Validate the Tables or ListBeans */
		for(WildeListBean<T, ?> listBean : getWildeBean().getListBeans()) {
			if(requireNonNullSqlFields || (customValidators != null && customValidators.containsKey(listBean.getParentBeanProperty().getParameterizedType()))) {
				if(getWildeBeanEditableTable(listBean)
						.checkSqlValidityOfDirtyBeans(requireNonNullSqlFields, customValidators == null ? null:customValidators.get(listBean.getParentBeanProperty().getParameterizedType()))) {
					return false;
				}
			}
		}
		
		/* Validate the bean itself */
		
		/* First, check if there's a custom Validator */
		if(wildeBeanValidationCallback != null) {
			if(wildeBeanValidationCallback.performCustomValidation(getWildeBean())) {
				return false;
			}
		}
		
		/* Now validate the NonNull SQL fields */
		if(requireNonNullSqlFields) {
			SimpleBooleanProperty allRequiredEditorsFilled = new SimpleBooleanProperty(true);
			WildePropertyUtils.validateSqlNonNullEditors((List<WildeBeanProperty<?>>) (List<?>) getWildeBean().getWildeBeanProperties(), wildeBeanProperty -> {
				Shake wobbleAnimation = new Shake(wildeBeanProperty.getPropertyEditor().getEditor());
				wobbleAnimation.play();
				allRequiredEditorsFilled.set(false);
    		});
			if(!allRequiredEditorsFilled.get()) return false;
		}
		
		return true;
	}
	
	public boolean commitChanges(CommitChangesParam param) throws Exception {
		return commitChanges(param.getLoggableObject(), param.getTableCommitCallback(), param.isRequireNonNullSqlFields(),
				param.getWildeBeanValidationCallback(), param.getCustomValidators(), param.getLoggablePrefix());
	}
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public boolean commitChanges(Loggable loggableObject, 
			TableCommitCallback beanListCallback, boolean requireNonNullSqlFields, WildeBeanValidationCallback<T> wildeBeanValidationCallback,
			Map<Class<?>, WildeListBeanValidationCallback<?>> customValidators, String loggablePrefix) throws Exception {
		
		/* Perform validation if it's requested (it may not if done prior to this call */
		if(requireNonNullSqlFields || wildeBeanValidationCallback != null || customValidators != null) {
			if(!validateProperties(requireNonNullSqlFields, wildeBeanValidationCallback, customValidators)) {
				return false;
			}
		}
		
		/* 
		 * Now make the commits, all validation has been performed
		 */
		
		/* Commit Changes to WildeListBeans */
		for(WildeListBean<T, ?> listBean : getWildeBean().getListBeans()) {
			getWildeBeanEditableTable(listBean).commitChanges(loggableObject, (TableCommitCallback) beanListCallback, false, null);
		}
		
		/* Commit Changes to the Bean itself */
		List<String> fieldsChanged = commitChangesToBean(loggableObject,loggablePrefix);
		if(!fieldsChanged.isEmpty() && beanListCallback != null) {
			beanListCallback.updateDirtyBean(getWildeBean().getObject(), fieldsChanged);
		}
		
		
		return true;
	}
	
	private List<String> commitChangesToBean(Loggable loggableObject,  String logPrefix){
        // Create a log item here for changes i.e. Log(previousValue, newValue) if they are different
		List<String> fieldsChanged = new ArrayList<>();
		
        getWildeBean().getWildeBeanProperties().forEach(item -> {
        	log.trace("Checking for changes to field {}", item.getFieldName());
            if(item.getPropertyEditor() == null){
                log.trace("No property editor for {}", item.getFieldName());
            }else{
//                if (item.isEverEditable()) {
//                    

                    if (!Objects.equals(item.getPropertyEditor().getValue(), item.getValue())) {

                        String oldValue = item.getValue() == null ? "empty" : item.getValue().toString();
                        String newValue = item.getPropertyEditor().getValue() == null ? "empty" : item.getPropertyEditor().getValue().toString();

                        if (Strings.isNullOrEmpty(oldValue)) {
                            oldValue = "empty";
                        }
                        if (Strings.isNullOrEmpty(newValue)) {
                            newValue = "empty";
                        }
                        // We want to treat null and empty string values the same
                        if(!Objects.equals(oldValue, newValue)) {

                        	if(loggableObject != null) {
                        		String prefix;

                        		String beanName = "";
                        		Object bean = getWildeBean().getObject();
                        		
                        		if(logPrefix == null && !Objects.equals(bean, loggableObject) && bean instanceof Identifiable<?>) {
                    				Identifiable<?> identifiable = (Identifiable<?>) bean;
                    				prefix = identifiable.getIdentifiableDescription() + " ";
                    			}else {
                    				prefix = logPrefix != null ? logPrefix + " " : "";
                    			}
                        		
                        		if(bean.getClass().getAnnotation(ClassDescriptor.class) != null) {
                        			beanName = bean.getClass().getAnnotation(ClassDescriptor.class).displayName();
                        		}else {
                        			beanName = bean.getClass().getName();
                        		}

                        		if (!item.getUiProperty().preferredEditor().equals("TextArea")) {



                        			loggableObject.addLogItem(beanName, prefix + item.getName() + " was changed from " +
                        					oldValue + " to " +
                        					newValue);
                        		} else {
                        			loggableObject.addLogItem(beanName, prefix + item.getName() + " was changed");
                        		}
                        	}

							log.debug("Field {} was changed from {} to {}", item.getFieldName(), oldValue, newValue);
                            fieldsChanged.add(item.getFieldName());
                        }else{
							log.trace("Field {} was effectively not changed", item.getFieldName());
						}
                    }else{
                    	log.trace("Field {} was not changed", item.getFieldName());
					}
                    item.setValue(item.getPropertyEditor().getValue());

                }

//            }

        });
        
        return fieldsChanged;
    }
	
	private List<String> commitChangesToBean(){
		return commitChangesToBean(null, null);
        // Create a log item here for changes i.e. Log(previousValue, newValue) if they are different
//    	List<String> fieldsChanged = new ArrayList<>();
//        wildeBean.getWildeBeanProperties().forEach(item -> {
//            if(item.getPropertyEditor() == null){
////                
//            }else{
//            	
//            	// Even though 
//            	if (item.isEverEditable()) {
////                  
//
//                  if (!Objects.equals(item.getPropertyEditor().getValue(), item.getValue())) {
//                      String oldValue = item.getValue() == null ? "empty" : item.getValue().toString();
//                      String newValue = item.getPropertyEditor().getValue() == null ? "empty" : item.getPropertyEditor().getValue().toString();
//
//                      if (Strings.isNullOrEmpty(oldValue)) {
//                          oldValue = "empty";
//                      }
//                      if (Strings.isNullOrEmpty(newValue)) {
//                          newValue = "empty";
//                      }
//                      // We want to treat null and empty string values the same
//                      if(!Objects.equals(oldValue, newValue)) {
//                          
//                          fieldsChanged.add(item.getFieldName());
//                      }
//                  }
//                  item.setValue(item.getPropertyEditor().getValue());
//
//              }
//
//            }
//
//        });
//        
//        return fieldsChanged;
    }
	
	
	public WildeEditableBeanTable<?> getWildeBeanEditableTable(WildeListBean<T,?> wildeListBean){
		if(wildeListBean.getWildeEditableBeanTable() == null) {
			wildeListBean.initWildeEditableBeanTable(editorFactory.isBindBidirectional());

			if(!wildeListBean.isShowTitleBox()){
				wildeListBean.getWildeEditableBeanTable().minimalToolBar();
			}

			if(listAdditionCallbacks.containsKey(wildeListBean.getParentBeanProperty().getParameterizedType())) {
				wildeListBean.getWildeEditableBeanTable().setAddNewButtonListener(() -> {
					listAdditionCallbacks.get(wildeListBean.getParentBeanProperty().getParameterizedType()).onNewObjectRequested((WildeListBean<T,Object>)wildeListBean);
				});
			}
		}
		
		return wildeListBean.getWildeEditableBeanTable();
	}
	
	/*
	 * JavaFX Properties
	 */
	
	public IntegerProperty preferredMultiGridNumberOfColumnsProperty() {
		return this.preferredMultiGridNumberOfColumns;
	}
	

	public int getPreferredMultiGridNumberOfColumns() {
		return this.preferredMultiGridNumberOfColumnsProperty().get();
	}
	

	public void setPreferredMultiGridNumberOfColumns(final int preferredMultiGridNumberOfColumns) {
		this.preferredMultiGridNumberOfColumnsProperty().set(preferredMultiGridNumberOfColumns);
	}
	

	public ObjectProperty<WildePropertySheetMode> wildeModeProperty() {
		return this.wildeMode;
	}
	

	public WildePropertySheetMode getWildeMode() {
		return this.wildeModeProperty().get();
	}
	

	public void setWildeMode(final WildePropertySheetMode wildeMode) {
		this.wildeModeProperty().set(wildeMode);
	}
	

	public BooleanProperty modeSwitcherVisibleProperty() {
		return this.modeSwitcherVisible;
	}
	

	public boolean isModeSwitcherVisible() {
		return this.modeSwitcherVisibleProperty().get();
	}
	

	public void setModeSwitcherVisible(final boolean modeSwitcherVisible) {
		this.modeSwitcherVisibleProperty().set(modeSwitcherVisible);
	}
	

	public BooleanProperty searchBoxVisibleProperty() {
		return this.searchBoxVisible;
	}
	

	public boolean isSearchBoxVisible() {
		return this.searchBoxVisibleProperty().get();
	}
	

	public void setSearchBoxVisible(final boolean searchBoxVisible) {
		this.searchBoxVisibleProperty().set(searchBoxVisible);
	}
	

	public StringProperty titleFilterProperty() {
		return this.titleFilter;
	}
	

	public String getTitleFilter() {
		return this.titleFilterProperty().get();
	}
	

	public void setTitleFilter(final String titleFilter) {
		this.titleFilterProperty().set(titleFilter);
	}

	public String getPropertyTableName() {
		return propertyTableName;
	}

	public void setPropertyTableName(String propertyTableName) {
		this.propertyTableName = propertyTableName;
	}

	public boolean isUseScrollPane() {
		return useScrollPane;
	}

	public void setUseScrollPane(boolean useScrollPane) {
		this.useScrollPane = useScrollPane;
	}

	
	
	public BooleanProperty floatingLabelSelectedProperty() {
		return floatingLabelButton.selectedProperty();
	}

	public ObjectProperty<WildeBean<T>> wildeBeanProperty() {
		return this.wildeBean;
	}
	

	public WildeBean<T> getWildeBean() {
		return this.wildeBeanProperty().get();
	}
	

	public void setWildeBean(final WildeBean<T> wildeBean) {
		this.wildeBeanProperty().set(wildeBean);
	}

	public final BooleanProperty showListTablesProperty() {
		return showListTables;
	}
	public final boolean isShowListTables() {
		return showListTables.get();
	}
	public final void setShowListTables(boolean value) {
		showListTables.set(value);
	}
	
	
}

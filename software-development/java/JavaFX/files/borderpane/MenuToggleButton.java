package com.thomaswilde.intellijborderpane;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.css.PseudoClass;
import javafx.event.EventHandler;
import javafx.geometry.Bounds;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.RadioMenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.Toggle;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

/**
 * The bar's UserData is its ToggleGroup that effects what pane is shown in its respective wrapper
 */
public class MenuToggleButton extends ToggleButton {

    private static final Logger log = LoggerFactory.getLogger(MenuToggleButton.class);

    //<editor-fold desc="Class Fields">
    /**
     * A unique ID for this Button
     */
    private final UUID uuid = UUID.randomUUID();

    /**
     * This is the Pane the ToggleButton carries
     */
    private final BorderPane container = new BorderPane();

    /**
     * The "main content" pane which goes in the center of container.
     */
    private final ObjectProperty<Parent> mainContent = new SimpleObjectProperty<>(this, "mainContent");

    /**
     * Header, this goes in the Top Node of container.  May be customized after the fact
     */
    private final ContentWrapperHeader contentWrapperHeader = new ContentWrapperHeader(this::createViewModeContextMenu);

    /**
     * View mode (WINDOW, DOCKED)
     */
    private final ObjectProperty<ToolWindowViewMode> viewMode = new SimpleObjectProperty<>(this, "viewMode", ToolWindowViewMode.DOCK_PINNED);

    /**
     * Toggle Group for the View Mode Menu options.  Bound to View mode property.
     */
    private final ToggleGroup viewModeToggleGroup = new ToggleGroup();

    /**
     * Stage for when in WINDOW mode
     */
    private Stage stage;

    // barPositionProperty
    private final ObjectProperty<BarPosition> barPosition = new SimpleObjectProperty<>(this, "barPosition");
    private BarPosition lastBarPosition;
    private Map<BarPosition, Pane> menuBarToggleContainers;

    private MoveButtonCallback moveButtonCallback;

    private double savedWidth;
    private double savedHeight;

    private boolean unPinnedHidePause = false;

    public interface MoveButtonCallback{
        void call(MenuToggleButton menuToggleButton, BarPosition barPosition);
    }
    //</editor-fold>

    //<editor-fold desc="Constructors">
    public MenuToggleButton() {
        init();
    }

    public MenuToggleButton(String name, Parent mainContent) {
        init();
        setMainContent(mainContent);
        setText(name);
    }

//    public MenuToggleButton(String name, Node graphic, Parent mainContent) {
//        init();
//        setMainContent(mainContent);
//        setText(name);
//        setGraphic(graphic);
//    }

    public MenuToggleButton(String name, Node graphic, Parent mainContent, MoveButtonCallback moveButtonCallback, Map<BarPosition, Pane> menuBarToggleContainers) {
        init();
        setMainContent(mainContent);
        setText(name);
        setGraphic(graphic);
        this.menuBarToggleContainers = menuBarToggleContainers;
        this.moveButtonCallback = moveButtonCallback;
    }
    //</editor-fold>

    /**
     * Main Initialization Method
     */
    private void init(){

        // Set the generic Header to the Top
        container.setTop(contentWrapperHeader);

        // Bind the Button text to the Pane title and Stage title
        initTitleBindings();

        // Have the remove button simply toggle the MenuToggleButton
        contentWrapperHeader.setRemoveButtonOnAction(event -> this.setSelected(!isSelected()));
        // Bind the container center to the main content property, this way is can be changed later if needed
        // and will still be reflected in the center
        mainContent.addListener((observable, oldValue, newValue) -> {
            BorderPane.setMargin(newValue, new Insets(5, 0, 0, 0));
        });
        container.centerProperty().bind(mainContent);

        // Add the style class
//        getStylesheets().add("menu-toggle-button.css");
        getStyleClass().add("menu-toggle-button");

        // Init View Mode Listener
        initViewModeListeners();

        // Init Listener on Selection (when in WINDOW mode)
        initSelectedListener();

        // Init Context Menu
        setContextMenu(createViewModeContextMenu(viewModeToggleGroup));

        // Keep track of the last bar pane
        barPosition.addListener((observable, oldValue, newValue) -> {
            if(newValue != null){
                setLastBarPosition(newValue);

                // Set the appropriate StackPane position on the container, which will be used in floating mode
                switch (newValue){
                    case LEFT_TOP:
                        StackPane.setAlignment(container, Pos.TOP_LEFT);
                        container.pseudoClassStateChanged(PseudoClass.getPseudoClass("SW"), false);
                        container.pseudoClassStateChanged(PseudoClass.getPseudoClass("SE"), false);
                        container.pseudoClassStateChanged(PseudoClass.getPseudoClass("NE"), false);
                        container.pseudoClassStateChanged(PseudoClass.getPseudoClass("NW"), true);
                        break;
                    case LEFT_BOTTOM:
                    case BOTTOM_LEFT:
                        StackPane.setAlignment(container, Pos.BOTTOM_LEFT);
                        container.pseudoClassStateChanged(PseudoClass.getPseudoClass("NW"), false);
                        container.pseudoClassStateChanged(PseudoClass.getPseudoClass("SE"), false);
                        container.pseudoClassStateChanged(PseudoClass.getPseudoClass("NE"), false);
                        container.pseudoClassStateChanged(PseudoClass.getPseudoClass("SW"), true);
                        break;
                    case BOTTOM_RIGHT:
                    case RIGHT_BOTTOM:
                        StackPane.setAlignment(container, Pos.BOTTOM_RIGHT);
                        container.pseudoClassStateChanged(PseudoClass.getPseudoClass("NW"), false);
                        container.pseudoClassStateChanged(PseudoClass.getPseudoClass("SW"), false);
                        container.pseudoClassStateChanged(PseudoClass.getPseudoClass("NE"), false);
                        container.pseudoClassStateChanged(PseudoClass.getPseudoClass("SE"), true);
                        break;
                    case RIGHT_TOP:
                        StackPane.setAlignment(container, Pos.TOP_RIGHT);
                        container.pseudoClassStateChanged(PseudoClass.getPseudoClass("NW"), false);
                        container.pseudoClassStateChanged(PseudoClass.getPseudoClass("SW"), false);
                        container.pseudoClassStateChanged(PseudoClass.getPseudoClass("SE"), false);
                        container.pseudoClassStateChanged(PseudoClass.getPseudoClass("NE"), true);
                        break;
                }

            }
            // rotate icon
            rotateIcon(newValue);
        });

        // Rotate icon based on bar position
        rotateIcon(getBarPosition());
    }

    private void rotateIcon(BarPosition barPosition){
        if(barPosition != null){
            switch (barPosition){
                case LEFT_TOP:
                case LEFT_BOTTOM:
                    getGraphic().setRotate(90);
                    break;
                case RIGHT_TOP:
                case RIGHT_BOTTOM:
                    getGraphic().setRotate(-90);
                    break;
                default:
                    getGraphic().setRotate(0);
                    break;
            }
        }
    }

    /**
     * Grouped methods for setting the title to other places
     */
    private void initTitleBindings(){
        contentWrapperHeader.getTitleLabel().textProperty().bind(this.textProperty());

    }

    /**
     * Creates the biDirectional binding between viewMode and viewModeToggleGroup (in context menu)
     */
    private void initViewModeListeners(){
        viewModeToggleGroup.selectedToggleProperty().addListener(viewModeMenuToggleListener);
        contentWrapperHeader.getViewModeToggleGroup().selectedToggleProperty().addListener(viewModeMenuToggleListener);
        viewMode.addListener(viewModeListener);
    }

    private final ChangeListener<Boolean> selectedListener = (observable, oldValue, newValue) -> {
        if(getViewMode() == ToolWindowViewMode.WINDOW){
            if(newValue){
                showStage();
            }else{
                getStage().close();
            }
        }
    };
    /**
     * Selection Listener on the MenuToggleButton is needed when it is in WINDOW mode
     * It's no longer in the Toggle Group of the Bar Pane, which handles it's action when docked
     */
    private void initSelectedListener(){
        ChangeListener<Boolean> selectedListener = (observable, oldValue, newValue) -> {
            if(getViewMode() == ToolWindowViewMode.WINDOW){
                if(newValue){

//                    if(getStage().isIconified()){
////                        getStage().setWidth(savedWidth);
////                        getStage().setHeight(savedHeight);
//                        Platform.runLater(() -> {
//                            getStage().setIconified(false);
//                            showStage();
//                        });
//
//                    }else{
//                        Platform.runLater(() -> {
//                            showStage();
//                        });
//
//                    }
                    showStage();

                }else{
                    if(getStage().isIconified()){
                        savedWidth = getStage().getWidth();
                        savedHeight = getStage().getHeight();
                    }
                    getStage().close();
                }
            } else if (getViewMode() == ToolWindowViewMode.DOCK_UNPINNED || getViewMode() == ToolWindowViewMode.FLOAT_UNPINNED) {
                unPinnedHidePause = true;
            }
        };
        selectedProperty().addListener(selectedListener);

    }

    /**
     * Initialize Content Menu (View Mode, Move To, Remove from bar)
     */
    private ContextMenu createViewModeContextMenu(ToggleGroup viewModeToggleGroup){

        // Init View Mode Menu
        RadioMenuItem dockPinnedMenuItem = new RadioMenuItem("Dock Pinned");
        RadioMenuItem dockUnpinnedMenuItem = new RadioMenuItem("Dock Unpinned");

        RadioMenuItem floatPinnedMenuItem = new RadioMenuItem("Float Pinned");
        RadioMenuItem floatUnpinnedMenuItem = new RadioMenuItem("Float Unpinned");

        RadioMenuItem windowMenuItem = new RadioMenuItem("Window");

        // Set the View Mode to the user data of the Menu Item
        dockPinnedMenuItem.setUserData(ToolWindowViewMode.DOCK_PINNED);
        dockUnpinnedMenuItem.setUserData(ToolWindowViewMode.DOCK_UNPINNED);
        floatPinnedMenuItem.setUserData(ToolWindowViewMode.FLOAT_PINNED);
        floatUnpinnedMenuItem.setUserData(ToolWindowViewMode.FLOAT_UNPINNED);
        windowMenuItem.setUserData(ToolWindowViewMode.WINDOW);

        // Set the radio menu items to the viewModeToggleGroup
        dockPinnedMenuItem.setToggleGroup(viewModeToggleGroup);
        dockUnpinnedMenuItem.setToggleGroup(viewModeToggleGroup);
        floatPinnedMenuItem.setToggleGroup(viewModeToggleGroup);
        floatUnpinnedMenuItem.setToggleGroup(viewModeToggleGroup);
        windowMenuItem.setToggleGroup(viewModeToggleGroup);

        // Default selection of Docked
        dockPinnedMenuItem.setSelected(true);

        Menu viewModeMenu = new Menu("View Mode");
        viewModeMenu.getItems().addAll(dockPinnedMenuItem, dockUnpinnedMenuItem, new SeparatorMenuItem(),
                floatPinnedMenuItem, floatUnpinnedMenuItem, new SeparatorMenuItem(), windowMenuItem);

        // Init Move To Menu
        Menu moveToMenu = new Menu("Move To");
        MenuItem leftTopMenuItem = new MenuItem("Left Top");
        MenuItem leftBottomMenuItem = new MenuItem("Left Bottom");
        MenuItem bottomLeftMenuItem = new MenuItem("Bottom Left");
        MenuItem bottomRightMenuItem = new MenuItem("Bottom Right");
        MenuItem rightBottomMenuItem = new MenuItem("Right Bottom");
        MenuItem rightTopMenuItem = new MenuItem("Right Top");

        moveToMenu.getItems().addAll(leftTopMenuItem, leftBottomMenuItem, bottomLeftMenuItem, bottomRightMenuItem, rightBottomMenuItem, rightTopMenuItem);
        leftTopMenuItem.setOnAction(event -> moveButtonCallback.call(this, BarPosition.LEFT_TOP));
        leftBottomMenuItem.setOnAction(event -> moveButtonCallback.call(this, BarPosition.LEFT_BOTTOM));
        bottomLeftMenuItem.setOnAction(event -> moveButtonCallback.call(this, BarPosition.BOTTOM_LEFT));
        bottomRightMenuItem.setOnAction(event -> moveButtonCallback.call(this, BarPosition.BOTTOM_RIGHT));
        rightBottomMenuItem.setOnAction(event -> moveButtonCallback.call(this, BarPosition.RIGHT_BOTTOM));
        rightTopMenuItem.setOnAction(event -> moveButtonCallback.call(this, BarPosition.RIGHT_TOP));

        // Remove from side bar
        MenuItem removeFromSideBarMenuItem = new MenuItem("Remove from Sidebar");
        removeFromSideBarMenuItem.setOnAction(event -> {
            setToggleGroup(null);
            ((Pane) this.getParent()).getChildren().remove(this);
        });

        // Set context menu
        return new ContextMenu(viewModeMenu, moveToMenu, new SeparatorMenuItem(), removeFromSideBarMenuItem);
    }

    /**
     * When the toggle from the view mode menu changes
     * Need to change the view mode
     */
    private final ChangeListener<Toggle> viewModeMenuToggleListener = (observable, oldValue, newValue) -> {
            setViewMode((ToolWindowViewMode) newValue.getUserData());
    };

    // Utility switch for the viewModeListener
    private boolean pauseViewModeListener = false;

    /**
     * When the view mode changes, two things need to happen
     * 1. The toggle for the context menu needs to change and not trigger this listener again (pauselistener set to true)
     * 2. Take an action based on the selection (viewModeOnAction)
     */
    private final ChangeListener<ToolWindowViewMode> viewModeListener = (observable, oldValue, newValue) -> {
        if(!pauseViewModeListener){
            pauseViewModeListener = true;
            // Turn off toggle listener prior to setting toggle
            viewModeToggleGroup.getToggles().stream()
                    .filter(toggle -> Objects.equals(toggle.getUserData(), newValue))
                    .findFirst()
                    .ifPresent(toggle -> toggle.setSelected(true));

            contentWrapperHeader.getViewModeToggleGroup().getToggles().stream()
                    .filter(toggle -> Objects.equals(toggle.getUserData(), newValue))
                    .findFirst()
                    .ifPresent(toggle -> toggle.setSelected(true));

            viewModeOnAction(newValue);

            pauseViewModeListener = false;
        }
    };

    /**
     * Handles changes to the view mode
     * When set to WINDOW, we need to set the toggle group to null so that other buttons in the bar
     * can be selected.  Also need to call showStage()
     *
     * When set to DOCKED.  Need to close the stage, and set the ToggleGroup back to the ToggleGroup
     * owned by the bar.
     *
     * @param viewMode WINDOW, DOCKED etc.
     */
    //TODO condition for DOCK_UNPINNED
    //todo condition for FLOAT_PINNED, FLOAT_UNPINNED
    private void viewModeOnAction(ToolWindowViewMode viewMode){
        // viewMode may be null if loading saved deprecated mode "DOCKED"
        if(viewMode == null) viewMode = ToolWindowViewMode.DOCK_PINNED;
        log.debug("view mode changed to {}", viewMode.name());

        FloatUtil.decommitFloatingPane(getContainer());
        if(getScene() == null){
            log.warn("Scene is null for trying to remove event filter");
        }
        if(getScene() != null){
            getScene().removeEventFilter(MouseEvent.MOUSE_CLICKED, unpinnedClickHandler);
        }

        switch (viewMode){
            case WINDOW:

                setToggleGroup(null);
                showStage();
                break;
            case FLOAT_UNPINNED:
                // Add event filter on scene
                log.debug("Adding unpicked click handler");
                setMinFloatingPaneHeights();
                FloatUtil.convertToFloatingPane(getContainer());
                unPinnedHidePause = true;

                if(getScene() != null){
                    getScene().addEventFilter(MouseEvent.MOUSE_CLICKED, unpinnedClickHandler);
                }else{
                    addUnPinnedClickHandlerWhenReady();
                }


                getStage().close();
                refreshToggle();

                break;
            case FLOAT_PINNED:
                setMinFloatingPaneHeights();

                FloatUtil.convertToFloatingPane(getContainer());
                getStage().close();
                refreshToggle();
                break;

            case DOCK_UNPINNED:
                // Add event filter on scene
                log.debug("Adding unpicked click handler");
                unPinnedHidePause = true;
                if(getScene() != null){
                    getScene().addEventFilter(MouseEvent.MOUSE_CLICKED, unpinnedClickHandler);
                }else{
                    addUnPinnedClickHandlerWhenReady();
                }

                getStage().close();
                refreshToggle();
                break;
            case DOCK_PINNED:

                getStage().close();
                refreshToggle();
                break;
        }
    }

    private void addUnPinnedClickHandlerWhenReady(){
        sceneProperty().addListener(new ChangeListener<>() {
            @Override
            public void changed(ObservableValue<? extends Scene> observable, Scene oldValue, Scene newValue) {
                newValue.addEventFilter(MouseEvent.MOUSE_CLICKED, unpinnedClickHandler);
                sceneProperty().removeListener(this);
            }
        });
    }

    private void refreshToggle(){
        setToggleGroup(getBarToggleGroup());

        // Refresh selection to activate `addToggleListener` to remove docking wrapper
        if(isSelected()){
            setSelected(false);
            setSelected(true);
        }
    }

    private void setMinFloatingPaneHeights(){
        getContainer().setMaxWidth(getWidth());
        getContainer().setMaxHeight(getHeight());

        if(getContainer().getMaxWidth() < 500){
            getContainer().setMaxWidth(500);
            getContainer().setMinWidth(500);
        }
        if(getContainer().getMaxHeight() < 500){
            getContainer().setMaxHeight(500);
            getContainer().setMinHeight(500);
        }
    }

    private final EventHandler<MouseEvent> unpinnedClickHandler = event -> {
        if(isSelected() && !unPinnedHidePause){

            Bounds boundsInScene = getContainer().localToScene(getContainer().getBoundsInLocal());

            if (!boundsInScene.contains(event.getSceneX(), event.getSceneY())) {
                log.debug("unselecting unpinned tool");
                setSelected(false);
            }

//            if(!event.getPickResult().getIntersectedNode().isMouseTransparent()
//                    && !getContainer().equals(event.getPickResult().getIntersectedNode())
//                    && !getContainer().getChildren().contains(event.getPickResult().getIntersectedNode())){
//
//                log.debug("unselecting unpinned tool");
//                setSelected(false);
//
//
//            }
        }
        unPinnedHidePause = false;
    };

    /**
     * Utility method to get the ToggleGroup owned by the Bar Pane
     * This is needed when changing the view mode back to docked
     * @return ToggleGroup
     */
    private ToggleGroup getBarToggleGroup(){
        log.debug("Getting toggle group for bar position: {}", getBarPosition());
        if(menuBarToggleContainers == null){
            log.warn("menu bars is null");
        }else if(menuBarToggleContainers.get(getBarPosition()) == null) {
            log.warn("there was not a map value for {}", getBarPosition());
        }
        return (ToggleGroup) menuBarToggleContainers.get(getBarPosition()).getUserData();
    }

    /**
     * Shows stage but checks if Scene has already been set
     */
    private void showStage(){

        Platform.runLater(() -> {
            getStage().setScene(new Scene(new BorderPane(getContainer()), getContainer().getWidth(), getContainer().getHeight()));

            getStage().show();
        });

    }

    //<editor-fold desc="Getters Setter">
    public final ObjectProperty<Parent> mainContentProperty() {
        return mainContent;
    }
    public final Parent getMainContent() {
        return mainContent.get();
    }
    public final void setMainContent(Parent value) {
        mainContent.set(value);
    }
    public BorderPane getContainer() {
        return container;
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
    public UUID getUuid() {
        return uuid;
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

    public BarPosition getLastBarPosition() {
        return lastBarPosition;
    }

    public void setLastBarPosition(BarPosition lastBarPosition) {
        this.lastBarPosition = lastBarPosition;
    }

    private void initStage(){
        stage = new Stage();
        stage.titleProperty().bind(this.textProperty());

        stage.iconifiedProperty().addListener((observable, oldValue, newValue) -> {
            log.debug("Stage width is: {}", stage.getWidth());
        });
        stage.widthProperty().addListener((observable, oldValue, newValue) -> {
            log.debug("Stage width changed to: {}", newValue);
        });

        stage.setOnCloseRequest(event -> {

            selectedProperty().removeListener(selectedListener);
            setSelected(false);
            selectedProperty().addListener(selectedListener);
        });
    }

    public Stage getStage() {
        if(stage == null){
            initStage();
        }
        return stage;
    }
    //</editor-fold>
}

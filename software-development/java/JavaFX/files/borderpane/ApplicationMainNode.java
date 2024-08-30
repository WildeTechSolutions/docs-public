package com.thomaswilde.intellijborderpane;

import com.thomaswilde.icons.GlyphButton;

import org.apache.commons.collections4.BidiMap;
import org.apache.commons.collections4.bidimap.DualHashBidiMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Consumer;

import javafx.application.Platform;
import javafx.collections.ListChangeListener;
import javafx.css.PseudoClass;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.SnapshotParameters;
import javafx.scene.control.Label;
import javafx.scene.control.SplitPane;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.DataFormat;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.transform.Transform;

public class ApplicationMainNode extends BorderPane {
    private static final Logger log = LoggerFactory.getLogger(ApplicationMainNode.class);



    //<editor-fold desc="Class Variables">
    // Content Wrappers
    private final BorderPane leftTopWrapper = new BorderPane();
    private final BorderPane leftBottomWrapper = new BorderPane();

    private final BorderPane rightTopWrapper = new BorderPane();
    private final BorderPane rightBottomWrapper = new BorderPane();

    private final BorderPane bottomLeftWrapper = new BorderPane();
    private final BorderPane bottomRightWrapper = new BorderPane();

    private final BorderPane centerContentWrapper = new BorderPane();

    // Content Split Panes
    private final SplitPane leftSplitPane = new SplitPane(leftTopWrapper, leftBottomWrapper);
    private final SplitPane rightSplitPane = new SplitPane(rightTopWrapper, rightBottomWrapper);
    private final SplitPane bottomSplitPane = new SplitPane(bottomLeftWrapper, bottomRightWrapper);

    private final SplitPane topContentPane = new SplitPane(leftSplitPane, centerContentWrapper, rightSplitPane);
    private final SplitPane mainContentSplitPane = new SplitPane(topContentPane, bottomSplitPane);
    private final StackPane mainContentStackPane = new StackPane(mainContentSplitPane);

    // Outline Border Nodes
    // Bottom
    private final ToggleGroup bottomLeftToggleGroup = new ToggleGroup();
    private final HBox bottomLeft = new HBox();
    private final ToggleGroup bottomRightToggleGroup = new ToggleGroup();
    private final HBox bottomRight = new HBox();

    // Left
    private final ToggleGroup leftTopToggleGroup = new ToggleGroup();
    private final HBox leftTop = new HBox();
    private final ToggleGroup leftBottomToggleGroup = new ToggleGroup();
    private final HBox leftBottom = new HBox();

    private final Group leftTopGroup = new Group(leftTop);
    private final Group leftBottomGroup = new Group(leftBottom);

    private final VBox leftTopGroupWrapper = new VBox(leftTopGroup);
    private final VBox leftBottomGroupWrapper = new VBox(leftBottomGroup);

    // Right
    private final ToggleGroup rightTopToggleGroup = new ToggleGroup();
    private final HBox rightTop = new HBox();
    private final ToggleGroup rightBottomToggleGroup = new ToggleGroup();
    private final HBox rightBottom = new HBox();

    private final Group rightTopGroup = new Group(rightTop);
    private final Group rightBottomGroup = new Group(rightBottom);

    private final VBox rightTopGroupWrapper = new VBox(rightTopGroup);
    private final VBox rightBottomGroupWrapper = new VBox(rightBottomGroup);

    private final Map<UUID, MenuToggleButton> toggleButtonsById = new HashMap<>();
    private final Map<String, MenuToggleButton> toggleButtonsByTitle = new HashMap<>();

    private final Map<ToggleGroup, BorderPane> toggleGroupContentPaneMap = Map.of(
            bottomLeftToggleGroup, bottomLeftWrapper,
            bottomRightToggleGroup, bottomRightWrapper,
            leftTopToggleGroup, leftTopWrapper,
            leftBottomToggleGroup, leftBottomWrapper,
            rightTopToggleGroup, rightTopWrapper,
            rightBottomToggleGroup, rightBottomWrapper
    );

//    private final Map<BarPosition, Pane> menuBars = Map.of(
//            BarPosition.LEFT_TOP, leftTopGroupWrapper,
//            BarPosition.LEFT_BOTTOM, leftBottomGroupWrapper,
//            BarPosition.BOTTOM_LEFT, bottomLeft,
//            BarPosition.BOTTOM_RIGHT, bottomRight,
//            BarPosition.RIGHT_TOP, rightTopGroupWrapper,
//            BarPosition.RIGHT_BOTTOM, rightBottomGroupWrapper
//    );
    private final BidiMap<BarPosition, Pane> menuBars = new DualHashBidiMap<>();
    private final BidiMap<BarPosition, Pane> menuBarToggleContainers = new DualHashBidiMap<>();
    //</editor-fold>

    public ApplicationMainNode(){

        menuBars.put(BarPosition.LEFT_TOP, leftTopGroupWrapper);
        menuBars.put(BarPosition.LEFT_BOTTOM, leftBottomGroupWrapper);
        menuBars.put(BarPosition.BOTTOM_LEFT, bottomLeft);
        menuBars.put(BarPosition.BOTTOM_RIGHT, bottomRight);
        menuBars.put(BarPosition.RIGHT_TOP, rightTopGroupWrapper);
        menuBars.put(BarPosition.RIGHT_BOTTOM, rightBottomGroupWrapper);

        menuBarToggleContainers.put(BarPosition.LEFT_TOP, leftTop);
        menuBarToggleContainers.put(BarPosition.LEFT_BOTTOM, leftBottom);
        menuBarToggleContainers.put(BarPosition.BOTTOM_LEFT, bottomLeft);
        menuBarToggleContainers.put(BarPosition.BOTTOM_RIGHT, bottomRight);
        menuBarToggleContainers.put(BarPosition.RIGHT_TOP, rightTop);
        menuBarToggleContainers.put(BarPosition.RIGHT_BOTTOM, rightBottom);

        // Set split pane orientations
        setSplitPaneOrientations();

        // Init Content Wrappers
        initWrappers();

        // Set Main Content
        centerContentWrapper.setCenter(new Label("Hello Main Content"));

        // Set Border Pane nodes
        setCenter(mainContentStackPane);


        // Initialize the menu bars
        initBars();

        // Populate some Buttons
//        addButtonsToBottom();
//        addButtonsToLeft();
//        addButtonsToRight();

        // Select the first Toggles
        Platform.runLater(this::selectFirstToggles);

    }

    private void initWrappers(){
//        leftTopWrapper.setTop(new ContentWrapperHeader());
//        leftBottomWrapper.setTop(new ContentWrapperHeader());
//        rightTopWrapper.setTop(new ContentWrapperHeader());
//        rightBottomWrapper.setTop(new ContentWrapperHeader());
//        bottomLeftWrapper.setTop(new ContentWrapperHeader());
//        bottomRightWrapper.setTop(new ContentWrapperHeader());
    }

    private void setSplitPaneOrientations(){
        leftSplitPane.setOrientation(Orientation.VERTICAL);
        rightSplitPane.setOrientation(Orientation.VERTICAL);
        mainContentSplitPane.setOrientation(Orientation.VERTICAL);

        bottomSplitPane.setOrientation(Orientation.HORIZONTAL);
        topContentPane.setOrientation(Orientation.HORIZONTAL);
    }

    /**
     * Utility method to select the first toggle for each group
     */
    private void selectFirstToggles(){

//        bottomLeftToggleGroup.selectToggle(bottomLeftToggleGroup.getToggles().get(0));
//        bottomRightToggleGroup.selectToggle(bottomRightToggleGroup.getToggles().get(0));
//        leftTopToggleGroup.selectToggle(leftTopToggleGroup.getToggles().get(0));
//        leftBottomToggleGroup.selectToggle(leftBottomToggleGroup.getToggles().get(0));
//        rightTopToggleGroup.selectToggle(rightTopToggleGroup.getToggles().get(0));
//        rightBottomToggleGroup.selectToggle(rightBottomToggleGroup.getToggles().get(0));
    }


    /**
     * Initializes the menu bars (left, bottom, right)
     */
    private void initBars(){
        HBox bottomBar = new HBox(bottomLeft, bottomRight);
        VBox leftBar = new VBox(leftTopGroupWrapper, leftBottomGroupWrapper);
        VBox rightBar = new VBox(rightTopGroupWrapper, rightBottomGroupWrapper);

        setBottom(bottomBar);
        setLeft(leftBar);
        setRight(rightBar);

        initBarDragHandler(bottomLeft, bottomRight, leftTopGroupWrapper, leftBottomGroupWrapper, rightTopGroupWrapper, rightBottomGroupWrapper);

        // Set styles
        bottomBar.getStylesheets().add("outline-border.css");
        leftBar.getStylesheets().add("outline-border.css");
        rightBar.getStylesheets().add("outline-border.css");

        bottomBar.getStyleClass().add("outline-border-container-bottom");
        leftBar.getStyleClass().add("outline-border-container-left");
        rightBar.getStyleClass().add("outline-border-container-right");

        leftTopGroupWrapper.getStyleClass().add("bar");
        leftBottomGroupWrapper.getStyleClass().add("bar");
        rightTopGroupWrapper.getStyleClass().add("bar");
        rightBottomGroupWrapper.getStyleClass().add("bar");
        bottomLeft.getStyleClass().add("bar");
        bottomRight.getStyleClass().add("bar");

        bottomLeft.setAlignment(Pos.CENTER_LEFT);
        bottomRight.setAlignment(Pos.CENTER_RIGHT);

        leftBottomGroupWrapper.setAlignment(Pos.BOTTOM_CENTER);
        leftTopGroupWrapper.setAlignment(Pos.TOP_CENTER);

        rightBottomGroupWrapper.setAlignment(Pos.BOTTOM_CENTER);
        rightTopGroupWrapper.setAlignment(Pos.TOP_CENTER);
//        leftBottomGroupWrapper.setNodeOrientation(NodeOrientation.RIGHT_TO_LEFT);

        leftTop.setRotate(270);
        leftBottom.setRotate(270);

        rightTop.setRotate(90);
        rightBottom.setRotate(90);

        HBox.setHgrow(bottomLeft, Priority.ALWAYS);
        HBox.setHgrow(bottomRight, Priority.ALWAYS);
        HBox.setHgrow(leftTop, Priority.ALWAYS);
        HBox.setHgrow(leftBottom, Priority.ALWAYS);

        VBox.setVgrow(leftTopGroupWrapper, Priority.ALWAYS);
        VBox.setVgrow(leftBottomGroupWrapper, Priority.ALWAYS);

        VBox.setVgrow(rightTopGroupWrapper, Priority.ALWAYS);
        VBox.setVgrow(rightBottomGroupWrapper, Priority.ALWAYS);

        // Associate Toggle Groups with the Containers
        leftTop.setUserData(leftTopToggleGroup);
        leftBottom.setUserData(leftBottomToggleGroup);
        rightTop.setUserData(rightTopToggleGroup);
        rightBottom.setUserData(rightBottomToggleGroup);
        bottomLeft.setUserData(bottomLeftToggleGroup);
        bottomRight.setUserData(bottomRightToggleGroup);

        addBarButtonAddedListener(leftTop, leftBottom, bottomLeft, bottomRight, rightTop, rightBottom);


        addWrapperContentListeners();
        initToggleListeners();
    }

    private void addBarButtonAddedListener(Pane... panes){

        for(Pane pane : panes){
            pane.getChildren().addListener((ListChangeListener<Node>) c -> {
                while(c.next()){

                    List<MenuToggleButton> toggleButtonsList = (List<MenuToggleButton>)(List<?>)c.getAddedSubList();
                    MenuToggleButton[] toggleButtons = (toggleButtonsList).toArray(new MenuToggleButton[0]);

                    // Any added buttons need to be added to the toggle group associated with the pane
                    toggleButtonsList.forEach(menuToggleButton -> {
                        menuToggleButton.setToggleGroup((ToggleGroup) pane.getUserData());

                        log.debug("setting bar position to {} ", menuBarToggleContainers.getKey(pane));
                        menuToggleButton.setBarPosition(menuBarToggleContainers.getKey(pane));
                    });

                    // Put button into button map
                    addButtonsToIdMap(toggleButtons);

                    // Init drag handler on button (if it already hasn't been done)
                    initButtonDragHandler(toggleButtons);
                }
            });
        }
    }

    private void addButtonsToIdMap(MenuToggleButton... buttons){
        for(MenuToggleButton button : buttons){
            toggleButtonsById.put(button.getUuid(), button);
            toggleButtonsByTitle.put(button.getText(), button);
        }
    }

    /**
     * Adds listeners to each wrappers content to add/remove from parent SplitPane
     * Adds listeners to the SplitPanes to add/remove from their parent SplitPane
     */
    private void addWrapperContentListeners(){

        addSplitPaneChildrenListeners();

        addWrapperListenerChild1(bottomLeftWrapper, bottomSplitPane);
        addWrapperListenerChild2(bottomRightWrapper, bottomSplitPane);

        addWrapperListenerChild1(leftTopWrapper, leftSplitPane);
        addWrapperListenerChild2(leftBottomWrapper, leftSplitPane);

        addWrapperListenerChild1(rightTopWrapper, rightSplitPane);
        addWrapperListenerChild2(rightBottomWrapper, rightSplitPane);


    }

    private void addSplitPaneChildrenListeners(){
        bottomSplitPane.getItems().addListener((ListChangeListener<Node>) c -> {
            if (bottomSplitPane.getItems().isEmpty()) {
                mainContentSplitPane.getItems().remove(bottomSplitPane);
            }else if(!mainContentSplitPane.getItems().contains(bottomSplitPane)){
                mainContentSplitPane.getItems().add(bottomSplitPane);
            }
        });

        rightSplitPane.getItems().addListener((ListChangeListener<Node>) c -> {
            if (rightSplitPane.getItems().isEmpty()) {
                topContentPane.getItems().remove(rightSplitPane);
            }else if(!topContentPane.getItems().contains(rightSplitPane)){
                topContentPane.getItems().add(topContentPane.getItems().size(), rightSplitPane);
            }
        });

        leftSplitPane.getItems().addListener((ListChangeListener<Node>) c -> {
            if (leftSplitPane.getItems().isEmpty()) {
                leftSplitPane.setUserData(topContentPane.getDividerPositions()[0]);
                topContentPane.getItems().remove(leftSplitPane);
            }else if(!topContentPane.getItems().contains(leftSplitPane)){


                topContentPane.getItems().add(0, leftSplitPane);

                if(leftSplitPane.getUserData() != null){
                    topContentPane.setDividerPosition(0, (double) leftSplitPane.getUserData());
                }
            }
        });
    }

    /**
     * Adds listener to the wrappers content so that it removes itself from its parent splitpane
     * or addes it back in
     * @param wrapper Content wrapper
     * @param parentSplitPane The content wrapper's parent splitpane
     */
    private void addWrapperListenerChild1(BorderPane wrapper, SplitPane parentSplitPane){

        // In initial state, if there's no content, remove from parent splitpane
        if(wrapper.getCenter() == null){
            parentSplitPane.getItems().remove(wrapper);
        }

        wrapper.centerProperty().addListener((observable, oldValue, newValue) -> {
            if(newValue == null){
                parentSplitPane.getItems().remove(wrapper);
            }else{
                if (!parentSplitPane.getItems().contains(wrapper)) {
                    parentSplitPane.getItems().add(0, wrapper);
                }
            }
        });
    }

    /**
     * Adds listener to the wrappers content so that it removes itself from its parent splitpane
     * or addes it back in
     * @param wrapper Content wrapper
     * @param parentSplitPane The content wrapper's parent splitpane
     */
    private void addWrapperListenerChild2(BorderPane wrapper, SplitPane parentSplitPane){
        // In initial state, if there's no content, remove from parent splitpane
        if(wrapper.getCenter() == null){
            parentSplitPane.getItems().remove(wrapper);
        }

        wrapper.centerProperty().addListener((observable, oldValue, newValue) -> {
            if(newValue == null){
                parentSplitPane.getItems().remove(wrapper);
            }else{
                if (!parentSplitPane.getItems().contains(wrapper)) {
                    if (!parentSplitPane.getItems().isEmpty()) {
                        parentSplitPane.getItems().add(parentSplitPane.getItems().size(), wrapper);
                    }else{
                        parentSplitPane.getItems().add(wrapper);
                    }
                }
            }
        });
    }

    /**
     * Initializes the toggle listeners so that they control their corresponding wrappers
     */
    private void initToggleListeners(){
        addToggleListener(bottomLeftToggleGroup, bottomLeftWrapper);
        addToggleListener(bottomRightToggleGroup, bottomRightWrapper);
        addToggleListener(leftTopToggleGroup, leftTopWrapper);
        addToggleListener(leftBottomToggleGroup, leftBottomWrapper);
        addToggleListener(rightTopToggleGroup, rightTopWrapper);
        addToggleListener(rightBottomToggleGroup, rightBottomWrapper);

    }



    /**
     * Listens for changes to the selected toggle and updates it's corresponding wrapper
     * with the Toggle's Node
     * @param toggleGroup the toggle group
     * @param wrapper the content wrapper associated with the toggle group
     */
    private void addToggleListener(ToggleGroup toggleGroup, BorderPane wrapper){
        toggleGroup.selectedToggleProperty().addListener((observable, oldValue, newValue) -> {
            if(newValue == null){
                wrapper.setCenter(null);
                // for floating, we remove the oldValue from the StackPane
                mainContentStackPane.getChildren().remove(((MenuToggleButton) oldValue).getContainer());
            }else{
                MenuToggleButton menuToggleButton = (MenuToggleButton) newValue;

                // if it's not floating, then we set the container to the wrapper
                if (menuToggleButton.getViewMode() == ToolWindowViewMode.DOCK_PINNED || menuToggleButton.getViewMode() == ToolWindowViewMode.DOCK_UNPINNED) {
                    if(oldValue != null){
                        mainContentStackPane.getChildren().remove(((MenuToggleButton) oldValue).getContainer());
                    }

                    wrapper.setCenter(menuToggleButton.getContainer());
                }
                // if it's floating, then we're going to add it to the stackpane
                else if (menuToggleButton.getViewMode() == ToolWindowViewMode.FLOAT_PINNED || menuToggleButton.getViewMode() == ToolWindowViewMode.FLOAT_UNPINNED) {
                    wrapper.setCenter(null);
                    mainContentStackPane.getChildren().add(menuToggleButton.getContainer());
                }


//                ((ContentWrapperHeader) wrapper.getTop()).setTitle(((MenuToggleButton) newValue).getText());
            }
        });
    }

    private void onSelectedToggle(MenuToggleButton oldValue, MenuToggleButton newValue){

    }

    private static final PseudoClass DRAG_OVER_MENU_BUTTON_PSEUDO_CLASS = PseudoClass.getPseudoClass("dragover");
    private static final DataFormat draggedToggleButtonFormat = new DataFormat("ToggleButton");
    private void initButtonDragHandler(MenuToggleButton... toggleButtons){
        for(MenuToggleButton toggleButton : toggleButtons) {
            if(toggleButton.getOnDragDetected() == null){
                toggleButton.setOnDragDetected(event -> {
                    Dragboard dragboard = toggleButton.startDragAndDrop(TransferMode.MOVE);
                    ClipboardContent content = new ClipboardContent();
                    content.put(draggedToggleButtonFormat, toggleButton.getUuid().toString());
                    dragboard.setContent(content);
                    SnapshotParameters params = new SnapshotParameters();
                    params.setTransform(Transform.scale(2, 2));
                    dragboard.setDragView(toggleButton.snapshot(params, null));
                });
            }

            toggleButton.setOnDragOver(event -> {

                Dragboard dragboard = event.getDragboard();
                Object draggedObject = dragboard.getContent(draggedToggleButtonFormat);

                if(draggedObject != null){
                    event.acceptTransferModes(TransferMode.MOVE);
                    toggleButton.pseudoClassStateChanged(DRAG_OVER_MENU_BUTTON_PSEUDO_CLASS, true);
                }

                event.consume();
            });

            toggleButton.setOnDragDropped(event -> {
                Dragboard dragboard = event.getDragboard();
                String draggedUuid = (String) dragboard.getContent(draggedToggleButtonFormat);
                MenuToggleButton draggedButton = toggleButtonsById.get(UUID.fromString(draggedUuid));

                if(Objects.equals(toggleButton, draggedButton)){
                    event.consume();
                    return;
                }

                ((Pane) draggedButton.getParent()).getChildren().remove(draggedButton);

                // Get index of toggleButton within parent, add to its index
                int dropIndex = toggleButton.getParent().getChildrenUnmodifiable().indexOf(toggleButton);
                ((HBox) toggleButton.getParent()).getChildren().add(dropIndex, draggedButton);

                toggleButton.pseudoClassStateChanged(DRAG_OVER_MENU_BUTTON_PSEUDO_CLASS, false);
                event.consume();

            });

            toggleButton.setOnDragExited(event -> {
                toggleButton.pseudoClassStateChanged(DRAG_OVER_MENU_BUTTON_PSEUDO_CLASS, false);
            });

        }
    }

    private void initBarDragHandler(Pane... panes){
        for (Pane pane : panes) {
            pane.setOnDragOver(event -> {

                Dragboard dragboard = event.getDragboard();
                Object draggedObject = dragboard.getContent(draggedToggleButtonFormat);

                if(draggedObject != null){
                    event.acceptTransferModes(TransferMode.MOVE);
                }

                pane.pseudoClassStateChanged(DRAG_OVER_MENU_BUTTON_PSEUDO_CLASS, true);
            });
            pane.setOnDragDropped(event -> {
                Dragboard dragboard = event.getDragboard();
                String draggedUuid = (String) dragboard.getContent(draggedToggleButtonFormat);
                MenuToggleButton draggedButton = toggleButtonsById.get(UUID.fromString(draggedUuid));

                ((Pane) draggedButton.getParent()).getChildren().remove(draggedButton);

                moveMenuToggleButtonToBar(draggedButton, pane);
                pane.pseudoClassStateChanged(DRAG_OVER_MENU_BUTTON_PSEUDO_CLASS, false);
            });
            pane.setOnDragExited(event -> {
                pane.pseudoClassStateChanged(DRAG_OVER_MENU_BUTTON_PSEUDO_CLASS, false);
            });
        }
    }

    private void moveMenuToggleButtonToBar(MenuToggleButton button, BarPosition barPosition){
        moveMenuToggleButtonToBar(button, menuBars.get(barPosition));
    }

    private void moveMenuToggleButtonToBar(MenuToggleButton button, Pane bar){
        // If it's the bottom, add it to the children
        // If it's a side, need to add to the embedded grouped VBox
        if(bar instanceof HBox){
            if(bar == bottomLeft){
                bar.getChildren().add(button);
            }else{
                bar.getChildren().add(0, button);
            }

        }else{
            Pane targetParent = ((Pane) ((Group) bar.getChildren().get(0)).getChildren().get(0));
            if(bar == leftBottomGroupWrapper || bar == rightTopGroupWrapper){
                targetParent.getChildren().add(button);
            }else{
                targetParent.getChildren().add(0, button);
            }
        }
    }




    /**
     * Utility method to populate the bottom button groups
     */
    private void addButtonsToBottom(){
        MenuToggleButton runButton = new MenuToggleButton("Run", GlyphButton.getGlyphIconStyled(GlyphButton.EDIT), new BorderPane(new Label("Run")), this::moveMenuToggleButtonToBar, menuBarToggleContainers);
        MenuToggleButton todoButton = new MenuToggleButton("TODO", GlyphButton.getGlyphIconStyled(GlyphButton.EDIT), new BorderPane(new Label("TODO")), this::moveMenuToggleButtonToBar, menuBarToggleContainers);
        MenuToggleButton problemsButton = new MenuToggleButton("Problems", GlyphButton.getGlyphIconStyled(GlyphButton.EDIT), new BorderPane(new Label("Problems")), this::moveMenuToggleButtonToBar, menuBarToggleContainers);

        bottomLeft.getChildren().setAll(runButton, todoButton, problemsButton);

        ToggleButton terminalButton = new MenuToggleButton("Terminal", GlyphButton.getGlyphIconStyled(GlyphButton.EDIT), new BorderPane(new Label("Terminal")), this::moveMenuToggleButtonToBar, menuBarToggleContainers);
        ToggleButton buildButton = new MenuToggleButton("Build", GlyphButton.getGlyphIconStyled(GlyphButton.EDIT), new BorderPane(new Label("Build\"")), this::moveMenuToggleButtonToBar, menuBarToggleContainers);

        bottomRight.getChildren().setAll(terminalButton, buildButton);
    }

    public Node getToolBarNode(String title){
        if(toggleButtonsByTitle.containsKey(title)){
            MenuToggleButton menuToggleButton = toggleButtonsByTitle.get(title);
            return menuToggleButton.getMainContent();
        }
        return null;
    }

    public void addNewToolWindow(String title, Parent content, Node icon, ToolBarPreferences preferences, Consumer<MenuToggleButton> onConfigChanged){
        addNewToolWindow(title, content, icon, preferences.getBarPosition(), preferences.isSelected(), preferences.getViewMode(), onConfigChanged);
    }

    public void addNewToolWindow(String title, Parent content, Node icon, BarPosition barPosition, Consumer<MenuToggleButton> onConfigChanged){
        addNewToolWindow(title, content, icon, barPosition, true, ToolWindowViewMode.DOCK_PINNED, onConfigChanged);
    }

    public void addNewToolWindow(String title, Parent content, Node icon, BarPosition barPosition, boolean select, ToolWindowViewMode viewMode, Consumer<MenuToggleButton> onConfigChanged){
        // Check to see if the MenuToggleButton already exists for the title, if so, then just have it toggle on
        if(toggleButtonsByTitle.containsKey(title)){
            MenuToggleButton menuToggleButton = toggleButtonsByTitle.get(title);
            if(menuToggleButton.getParent() == null){
                if(menuToggleButton.getBarPosition() != null){
                    moveMenuToggleButtonToBar(menuToggleButton, menuToggleButton.getBarPosition());
                }else{
                    //TODO maybe have a default position assign in constructor
                    moveMenuToggleButtonToBar(menuToggleButton, BarPosition.LEFT_TOP);
                }
            }
            menuToggleButton.setViewMode(viewMode);
            menuToggleButton.setSelected(select);
        }else{
            // If not then create a new one
            MenuToggleButton newToggleButton = new MenuToggleButton(title, icon, content, this::moveMenuToggleButtonToBar, menuBarToggleContainers);

            moveMenuToggleButtonToBar(newToggleButton, barPosition);
            newToggleButton.setViewMode(viewMode);
            newToggleButton.setSelected(select);

            newToggleButton.viewModeProperty().addListener((observable, oldValue, newValue) -> onConfigChanged.accept(newToggleButton));
            newToggleButton.selectedProperty().addListener((observable, oldValue, newValue) -> onConfigChanged.accept(newToggleButton));
            newToggleButton.barPositionProperty().addListener((observable, oldValue, newValue) -> onConfigChanged.accept(newToggleButton));

            newToggleButton.parentProperty().addListener(((observable, oldValue, newValue) -> {
                onConfigChanged.accept(newToggleButton);
            }));
        }

    }

    public void setMainContent(Node node){
        if(node == null){
            centerContentWrapper.setCenter(new Label("No content to show"));
        }else {
            centerContentWrapper.setCenter(node);
        }
    }

    /**
     * Utility method to populate the left button groups
     */
    private void addButtonsToLeft(){
        ToggleButton structureButton = new MenuToggleButton("Structure", GlyphButton.getGlyphIconStyled(GlyphButton.EDIT), new BorderPane(new Label("Structure")), this::moveMenuToggleButtonToBar, menuBarToggleContainers);
        ToggleButton favoritesButton = new MenuToggleButton("Favorites", GlyphButton.getGlyphIconStyled(GlyphButton.EDIT), new BorderPane(new Label("Favorites")), this::moveMenuToggleButtonToBar, menuBarToggleContainers);

        leftBottom.getChildren().setAll(structureButton, favoritesButton);


        ToggleButton projectButton = new MenuToggleButton("Project", GlyphButton.getGlyphIconStyled(GlyphButton.EDIT), new BorderPane(new Label("Project")), this::moveMenuToggleButtonToBar, menuBarToggleContainers);

        projectButton.setToggleGroup(leftTopToggleGroup);
        leftTop.getChildren().setAll(projectButton);

    }

    /**
     * Utility method to populate the right button groups
     */
    private void addButtonsToRight(){
        ToggleButton gradleButton = new MenuToggleButton("Gradle", GlyphButton.getGlyphIconStyled(GlyphButton.EDIT), new BorderPane(new Label("Gradle")), this::moveMenuToggleButtonToBar, menuBarToggleContainers);
        ToggleButton deviceManager = new MenuToggleButton("Device Manager", GlyphButton.getGlyphIconStyled(GlyphButton.EDIT), new BorderPane(new Label("Device Manager")), this::moveMenuToggleButtonToBar, menuBarToggleContainers);


        rightTop.getChildren().setAll(gradleButton, deviceManager);


        ToggleButton emulatorButton = new MenuToggleButton("Emulator", GlyphButton.getGlyphIconStyled(GlyphButton.EDIT), new BorderPane(new Label("Emulator")), this::moveMenuToggleButtonToBar, menuBarToggleContainers);
        ToggleButton deviceExplorerButton = new MenuToggleButton("Device File Explorer", GlyphButton.getGlyphIconStyled(GlyphButton.EDIT), new BorderPane(new Label("Device File Explorer")), this::moveMenuToggleButtonToBar, menuBarToggleContainers);

        emulatorButton.setToggleGroup(rightBottomToggleGroup);
        deviceExplorerButton.setToggleGroup(rightBottomToggleGroup);

        rightBottom.getChildren().setAll(emulatorButton, deviceExplorerButton);

    }


}

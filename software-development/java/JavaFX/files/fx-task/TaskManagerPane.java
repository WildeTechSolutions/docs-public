package com.thomaswilde.fx.task;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javafx.beans.binding.Bindings;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;

public class TaskManagerPane extends BorderPane{

	private ScrollPane scrollPane;
	private VBox taskUiContainer;
	private ObservableList<Node> taskNodes = FXCollections.observableArrayList();
	private Map<Task<?>, Node> taskToNodeMap = new HashMap<>();
	
	public TaskManagerPane() {
		taskUiContainer = new VBox();
		taskUiContainer.setPadding(new Insets(10));
		taskUiContainer.setSpacing(10);
		Bindings.bindContentBidirectional(taskUiContainer.getChildren(), taskNodes);
		
		
		scrollPane = new ScrollPane(taskUiContainer);
		scrollPane.setFitToWidth(true);
		setCenter(scrollPane);
		
		initCurrentTasks();
	}
	
	private void initCurrentTasks() {
		for(Task<?> task : TaskManager.getInstance().getAppTasks()) {
			Node node = createTaskProgressPane(task);
			node.setStyle("-fx-background-color: derive(-fx-base, +12%);" +
                    "-fx-background-radius: 10;" +
                    "-fx-effect: dropshadow(two-pass-box , -fx-shadow-highlight-color, 4, 0.0 , 0, 1.4);");
			taskNodes.add(node);
			taskToNodeMap.put(task, node);
		}
		
		TaskManager.getInstance().getAppTasks().addListener((ListChangeListener.Change<? extends Task<?>> c) -> {
			while (c.next()) {
				for(Task<?> task : c.getAddedSubList()) {
					Node node = createTaskProgressPane(task);
					node.setStyle("-fx-background-color: derive(-fx-base, +12%);" +
	                        "-fx-background-radius: 10;" +
	                        "-fx-effect: dropshadow(two-pass-box , -fx-shadow-highlight-color, 4, 0.0 , 0, 1.4);");
					taskNodes.add(node);
					taskToNodeMap.put(task, node);
				}				
				for(Task<?> task : c.getRemoved()) {
					taskNodes.remove(taskToNodeMap.get(task));
					taskToNodeMap.remove(task);
				}
			}
		});
	}
	
	private Node createTaskProgressPane(Task<?> task) {
		FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/com/thomaswilde/matdb/application/task_progress_pane.fxml"));
		Node node = null;
		try {
			node = fxmlLoader.load();
		} catch (IOException e) {
			e.printStackTrace();
		}
		TaskProgressPaneController controller = fxmlLoader.getController();
		controller.setTask(task);
		return node;
	}
	
	
	
}

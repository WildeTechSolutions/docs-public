package com.thomaswilde.fx.task;

import com.jfoenix.controls.JFXProgressBar;

import javafx.beans.property.ReadOnlyDoubleProperty;
import javafx.beans.value.ChangeListener;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.Region;

public class TaskProgressPaneController {

	@FXML private Region parent;
	@FXML private Label titleLabel;
	@FXML private Label messageLabel;
	@FXML private Label progressLabel;	
	@FXML private JFXProgressBar progressBar;
	
	@FXML private void initialize() {
		titleLabel.setText("");
		messageLabel.setText("");
		progressLabel.setText("");
		progressBar.setProgress(ProgressBar.INDETERMINATE_PROGRESS);
	}
	
	public void setTask(Task<?> task) {
		titleLabel.textProperty().bind(task.titleProperty());
		messageLabel.textProperty().bind(task.messageProperty());		
		task.progressProperty().addListener(progressInitiatedListener);
		
	}
	
	private ChangeListener<Number> progressInitiatedListener = (observable, oldValue, newValue) -> {
		Task<?> task = (Task<?>) ((ReadOnlyDoubleProperty) observable).getBean();
		if(newValue.doubleValue() > 0) {
			progressBar.progressProperty().bind(task.progressProperty());
			removeProgressListener(task);
		}
	};
	
	private void removeProgressListener(Task<?> task) {
		task.progressProperty().removeListener(progressInitiatedListener);
	}
	
}

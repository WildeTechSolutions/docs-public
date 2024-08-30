package com.thomaswilde.fx.task;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javafx.beans.property.ReadOnlyProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.beans.value.ChangeListener;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.concurrent.Task;
import javafx.concurrent.Worker;

public class TaskManager {
	private static final Logger log = LoggerFactory.getLogger(TaskManager.class);

	private static TaskManager taskManager;
	
	public static TaskManager getInstance() {
		if(taskManager == null) {
			taskManager = new TaskManager();
		}
		return taskManager;
	}
	
	private SimpleListProperty<Task<?>> appTasks = new SimpleListProperty<>(this, "appTasks", FXCollections.observableArrayList());

	private TaskManager() {
		appTasks.addListener((ListChangeListener.Change<? extends Task<?>> c) -> {
			while (c.next()) {
				for(Task<?> task : c.getAddedSubList()) {
					task.stateProperty().addListener(taskStateChangeListener);
				}				
				for(Task<?> task : c.getRemoved()) {
					task.stateProperty().removeListener(taskStateChangeListener);
				}
			}
		});
	}
	
	private ChangeListener<Worker.State> taskStateChangeListener = (observable, oldValue, newValue) -> {
		
		Task<?> task = (Task<?>) (((ReadOnlyProperty<?>) observable).getBean());
		if(newValue == Worker.State.SUCCEEDED) {
			log.trace("Task {} succeeded", task.getTitle());
			getAppTasks().remove(task);
		}else if(newValue == Worker.State.CANCELLED) {
			log.trace("Task {} was cancelled", task.getTitle());
			getAppTasks().remove(task);
		}else if(newValue == Worker.State.FAILED) {
			log.warn("Task {} failed", task.getTitle());
			getAppTasks().remove(task);
		}
	};
	
	public SimpleListProperty<Task<?>> getAppTasks() {
		return appTasks;
	}
	
	public void addTask(Task<?> task) {
		getAppTasks().add(task);
	}

}

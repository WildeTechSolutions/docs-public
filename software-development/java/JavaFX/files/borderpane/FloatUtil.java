package com.thomaswilde.intellijborderpane;

import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;

public class FloatUtil {

    public static void convertToFloatingPane(Pane floatingPane){
        // The delta x and y represent the difference between
        // the mouse and the top-left corner of the floating pane
        final Delta dragDelta = new Delta();

        // Mouse pressed event handler
        floatingPane.setOnMousePressed(mouseEvent -> {
            dragDelta.x = mouseEvent.getX();
            dragDelta.y = mouseEvent.getY();
            dragDelta.width = floatingPane.getWidth();
            dragDelta.height = floatingPane.getHeight();
        });

        // Mouse dragged event handler
        floatingPane.setOnMouseDragged(mouseEvent -> {
            // If the cursor is set for resize, adjust the size of the floatingPane
            if (floatingPane.getCursor() != Cursor.DEFAULT) {
                if (floatingPane.getCursor() == Cursor.S_RESIZE) {

                    if(isFloatingTop(floatingPane)){
                        double newHeight = dragDelta.height + (mouseEvent.getY() - dragDelta.y);
                        floatingPane.setMinHeight(newHeight);
                        floatingPane.setMaxHeight(newHeight);
                    }else{
                        floatingPane.setMinHeight(floatingPane.getHeight() - mouseEvent.getY());
                        floatingPane.setMaxHeight(floatingPane.getHeight() - mouseEvent.getY());
                    }


                } else if (floatingPane.getCursor() == Cursor.W_RESIZE) {

                    if (isFloatingRight(floatingPane)) {
                        floatingPane.setMinWidth(floatingPane.getWidth() - mouseEvent.getX());
                        floatingPane.setMaxWidth(floatingPane.getWidth() - mouseEvent.getX());
                    }else{

                        double newWidth = dragDelta.width + (mouseEvent.getX() - dragDelta.x);
                        floatingPane.setMinWidth(newWidth);
                        floatingPane.setMaxWidth(newWidth);
                    }

                } else if (floatingPane.getCursor() == Cursor.NW_RESIZE ||
                        floatingPane.getCursor() == Cursor.SW_RESIZE ||
                        floatingPane.getCursor() == Cursor.SE_RESIZE ||
                        floatingPane.getCursor() == Cursor.NE_RESIZE) {

                    if(isFloatingTop(floatingPane)){
                        double newHeight = dragDelta.height + (mouseEvent.getY() - dragDelta.y);
                        floatingPane.setMinHeight(newHeight);
                        floatingPane.setMaxHeight(newHeight);
                    }else{
                        floatingPane.setMinHeight(floatingPane.getHeight() - mouseEvent.getY());
                        floatingPane.setMaxHeight(floatingPane.getHeight() - mouseEvent.getY());
                    }

                    if (isFloatingRight(floatingPane)) {
                        floatingPane.setMinWidth(floatingPane.getWidth() - mouseEvent.getX());
                        floatingPane.setMaxWidth(floatingPane.getWidth() - mouseEvent.getX());
                    }else{

                        double newWidth = dragDelta.width + (mouseEvent.getX() - dragDelta.x);
                        floatingPane.setMinWidth(newWidth);
                        floatingPane.setMaxWidth(newWidth);
                    }

                }
            }
        });

        // Mouse moved event handler
        floatingPane.setOnMouseMoved(mouseEvent -> {

            double diffMinX;
            if (isFloatingRight(floatingPane)) {
                diffMinX = Math.abs(mouseEvent.getX());
            }else{
                diffMinX = Math.abs(mouseEvent.getX() - floatingPane.getWidth());
            }

            double diffMaxY;
            if(isFloatingTop(floatingPane)){
                diffMaxY = Math.abs(mouseEvent.getY() - floatingPane.getHeight());
            }else{
                diffMaxY = Math.abs(mouseEvent.getY());
            }

            final double offset = 10.0;

            if (diffMinX < offset && diffMaxY < offset) {
                if(StackPane.getAlignment(floatingPane) == Pos.TOP_RIGHT){
                    floatingPane.setCursor(Cursor.SW_RESIZE);
                }else if(StackPane.getAlignment(floatingPane) == Pos.BOTTOM_RIGHT){
                    floatingPane.setCursor(Cursor.NW_RESIZE);
                }else if(StackPane.getAlignment(floatingPane) == Pos.BOTTOM_LEFT){
                    floatingPane.setCursor(Cursor.NE_RESIZE);
                }else if(StackPane.getAlignment(floatingPane) == Pos.TOP_LEFT){
                    floatingPane.setCursor(Cursor.SE_RESIZE);
                }

            } else if (diffMinX < offset) {
                floatingPane.setCursor(Cursor.W_RESIZE);
            } else if (diffMaxY < offset) {
                floatingPane.setCursor(Cursor.S_RESIZE);
            } else {
                floatingPane.setCursor(Cursor.DEFAULT);
            }
        });

        floatingPane.getStyleClass().add("float-pane");
    }

    public static void decommitFloatingPane(Pane floatingPane){

        floatingPane.setOnMousePressed(null);
        floatingPane.setOnMouseDragged(null);
        floatingPane.setOnMouseMoved(null);
        floatingPane.setMinHeight(-1);
        floatingPane.setMaxHeight(-1);

        floatingPane.setMinWidth(-1);
        floatingPane.setMaxWidth(-1);

        floatingPane.getStyleClass().remove("float-pane");
    }

    public static boolean isFloatingTop(Pane pane){
        return StackPane.getAlignment(pane) == Pos.TOP_RIGHT || StackPane.getAlignment(pane) == Pos.TOP_LEFT || StackPane.getAlignment(pane) == Pos.TOP_LEFT;
    }

    public static boolean isFloatingRight(Pane pane){
        return StackPane.getAlignment(pane) == Pos.TOP_RIGHT || StackPane.getAlignment(pane) == Pos.CENTER_RIGHT || StackPane.getAlignment(pane) == Pos.BOTTOM_RIGHT;
    }

    // used for dragging the floating pane
    private static class Delta {
        double x, y, width, height;
    }

}

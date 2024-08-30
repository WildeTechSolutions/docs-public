package com.thomaswilde.icons;

//import com.sun.javafx.scene.control.skin.TreeViewSkin;

import de.jensd.fx.glyphs.GlyphIcon;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIconView;
//import de.jensd.fx.glyphs.fontawesome.FontAwesomeIconView;
import de.jensd.fx.glyphs.icons525.Icons525View;
import javafx.application.Application;
import javafx.beans.property.Property;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.css.PseudoClass;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.layout.StackPane;

public class GlyphButton extends Button{

	private static final String STYLE_TRANSPARENT = "-fx-background-color: transparent; -fx-border-color: transparent;";
	public static final String REMOVE = "remove-icon";
	public static final String TRASH = "trash-icon";
	public static final String PLUS = "plus-icon";
	public static final String WORD = "word-icon";
	public static final String EXCEL = "excel-icon";
	public static final String LEVEL_UP = "level-up-icon";
	public static final String INFO = "info-icon";
	public static final String EXTERNAL_LINK = "external-link-icon";
	public static final String EDIT = "edit-icon";
	public static final String SAVE = "save-icon";
	public static final String ANGLE_LEFT = "angle-left-icon";
	public static final String ANGLE_RIGHT = "angle-right-icon";
	
	public static final String REFRESH = "refresh-icon";
	public static final String FULLSCREEN = "fullscreen-icon";
	public static final String FULLSCREEN_EXIT = "fullscreen-exit-icon";
	public static final String USER = "user-icon";
	public static final String OUTLOOK = "outlook-icon";
	public static final String SKYPE = "skype-icon";
	public static final String ELLIPSIS_V = "ellipsis-v-icon";
	public static final String GO2 = "go2-icon";
	public static final String CHECK = "check-icon";
	public static final String ANGLE_DOWN = "angle-down-icon";
	public static final String ANGLE_UP = "angle-up-icon";
	public static final String INDENT = "indent-icon";
	public static final String JUSTIFY = "justify-icon";
	public static final String COPY = "copy-icon";
	public static final String PDF = "pdf-icon";
	public static final String SIGNATURE = "signature-icon";
	public static final String NOTIFICATIONS = "notifications-icon";
	public static final String PAINTBRUSH = "paint-icon";
	public static final String HISTORY = "history-icon";
	public static final String GEAR = "gear-icon";
	public static final String MINUS = "minus-icon";
	public static final String FOLDER = "folder-icon";
	public static final String STAR_UNFILLED = "star-unfilled-icon";
	public static final String STAR_FILLED = "star-filled-icon";
	public static final String PRINT = "print-icon";
	public static final String HOME = "home-icon";
	public static final String FILTER = "filter-icon";
	public static final String DATABASE = "database-icon";
	public static final String LIST = "list-icon";
	public static final String SEARCH = "search-icon";
	public static final String INBOX = "inbox-icon";
	public static final String DOWNLOAD = "download-icon";
	public static final String PIE_CHART = "pie-chart-icon";
	public static final String BAR_CHART = "bar-chart-icon";
	public static final String AREA_CHART = "area-chart-icon";
	public static final String SHOPPING_BASKET = "shopping-basket-icon";
	public static final String RECYCLE = "recycle-icon";
	public static final String BASKET = "basket-icon";
	public static final String FLASK = "flask-icon";

	public static void addGlyphAsGraphic(Button button, String glyphName){
		FontAwesomeIconView icon = new FontAwesomeIconView();
		button.getStylesheets().add(Application.getUserAgentStylesheet());
		icon.setStyleClass(glyphName);
		button.setGraphic(icon);
	}

	public static Node getGlyphIcon(String glyphName){
		GlyphIcon<?> icon = null;
		switch (glyphName){
		case WORD:
		case EXCEL:
		case FULLSCREEN:
		case FULLSCREEN_EXIT:
		case OUTLOOK:
		case SKYPE:
		case GO2:
		case BASKET:
//		case PAINTBRUSH:
			icon = new Icons525View();
			break;
		default:
			icon = new FontAwesomeIconView();
			break;
		}

		icon.setStyleClass(glyphName);

		return icon;
	}

	public static void convertToGlyphButtonTransparent(Button button, String glyphName){

		GlyphIcon<?> icon = (GlyphIcon<?>) getGlyphIcon(glyphName);

		button.getStylesheets().add(Application.getUserAgentStylesheet());
		icon.setStyleClass(glyphName);
		button.setGraphic(icon);
		button.setStyle(STYLE_TRANSPARENT);

	}

	public static void convertToGlyphButton(Button button, String glyphName){

		GlyphIcon<?> icon = (GlyphIcon<?>) getIcon(glyphName);
		button.setPadding(new Insets(0));
		button.getStylesheets().add(Application.getUserAgentStylesheet());
		icon.setStyleClass(glyphName);
		button.setGraphic(icon);
		

	}
	//    }

	public GlyphButton(String glyphName){

		GlyphIcon<?> icon = (GlyphIcon<?>) getIcon(glyphName);
		setPadding(new Insets(0));

		this.getStylesheets().add(Application.getUserAgentStylesheet());
		icon.setStyleClass(glyphName);
		this.setGraphic(icon);
		this.setStyle(STYLE_TRANSPARENT);

	}
	
	public GlyphButton(String text, String glyphName){

		GlyphIcon<?> icon = (GlyphIcon<?>) getIcon(glyphName);
		setPadding(new Insets(0));
		this.getStylesheets().add(Application.getUserAgentStylesheet());
		icon.setStyleClass(glyphName);
		this.setGraphic(icon);
		setText(text);

	}
	
	public static Node getGlyphIconStyled(String glyphName){
    	GlyphIcon<?> icon = (GlyphIcon<?>) getIcon(glyphName);
        
        icon.setStyleClass(glyphName);        
        
        icon.parentProperty().addListener(new IconChangeListener(icon));
        
        return icon;
    }
	
	private static Node getIcon(String glyphName) {

		return getGlyphIcon(glyphName);
	}
	private static final PseudoClass HOVER = PseudoClass.getPseudoClass("hover");
    private static class IconChangeListener implements ChangeListener<Parent>{

    	private GlyphIcon<?> icon;
    	
    	public IconChangeListener(GlyphIcon<?> icon) {
    		this.icon = icon;
    	}
		@Override
		public void changed(ObservableValue<? extends Parent> observable, Parent oldValue, Parent newValue) {
			if(newValue != null) {
				newValue.getStylesheets().add(Application.getUserAgentStylesheet());
				icon.parentProperty().removeListener(this);
				
//				
//				
				/* For menu items, the glyphs parent is a stackpane, and need to go one more level up to get to the menu item */
				if(newValue instanceof StackPane) {
					newValue.parentProperty().addListener((observable2, oldValue2, newValue2) -> {
						if(newValue2 != null) {
							newValue2.hoverProperty().addListener((observable3, oldValue3, newValue3) -> {
								icon.pseudoClassStateChanged(HOVER, newValue3);
							});
						}
					});	
				}else {
					newValue.hoverProperty().addListener((observable3, oldValue3, newValue3) -> {
						icon.pseudoClassStateChanged(HOVER, newValue3);
					});
				}
				
			}			
		}
    	
    }
    

}

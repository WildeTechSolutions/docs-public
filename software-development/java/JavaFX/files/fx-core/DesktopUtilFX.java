package com.thomaswilde.fxcore;


import java.io.File;
import java.util.Arrays;

import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;

public class DesktopUtilFX {

    public static void copyFilesToClipboard(File... files){
        Clipboard clipboard = Clipboard.getSystemClipboard();
        ClipboardContent content = new ClipboardContent();

        content.putFiles(Arrays.asList(files));
        clipboard.setContent(content);
    }
}

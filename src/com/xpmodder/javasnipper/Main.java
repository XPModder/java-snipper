package com.xpmodder.javasnipper;

import java.awt.Color;

public class Main {
    public static void main(String[] args) {

        SnippingTool tool = new SnippingTool();
        tool.setSelectionColor(Color.CYAN);

        tool.showAndWait();

    }
}




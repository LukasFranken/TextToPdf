package de.rundp.texttopdf;

import com.itextpdf.text.Font;

public class metaStyle {
	Font.FontFamily fontFamily;
	int fontSize;
	int fontStyle;
	
	public metaStyle(Font.FontFamily fontFamily, int fontSize, int fontStyle) {
		this.fontFamily = fontFamily;
		this.fontSize = fontSize;
		this.fontStyle = fontStyle;
	}
}

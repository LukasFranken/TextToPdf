package de.rundp.texttopdf;

import java.io.FileOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Array;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Comparator;
import java.util.HashMap;
import java.text.SimpleDateFormat;

import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Font;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.Phrase;
import com.itextpdf.text.pdf.BaseFont;
import com.itextpdf.text.pdf.PdfWriter;

public class Application {

	private static final String DATE_FORMAT_NOW = "dd-MM-yyyy";
	private static int monoSpace = Font.NORMAL;
	private static int monoSpaceBold = Font.BOLD;
	private static int monoSpaceItalic = Font.ITALIC;
	private static int monoSpaceStroke = Font.STRIKETHRU;
	private static int monoSpaceBoldItalic = Font.BOLDITALIC;

	private static Font.FontFamily defaultFont = Font.FontFamily.HELVETICA;
	private static int defaultFontSize = 11;
	private static int defaultFontStyle = Font.NORMAL;

	public static Map<String, Font.FontFamily> fontMap = new HashMap();
	public static Map<String, Integer> fontStyleMap = new HashMap();


	private static String[] markUpCollection = new String[] { "**", "§§", "--", "h1", "h2" };
	private static String metaStyleCommand = "##";
	private static String fontFamilyCommand = ".FF.";
	private static String fontSizeCommand = ".FS.";
	private static String fontStyleCommand = ".FST.";

	private static String IN = "c:/users/braun/desktop/input.txt";
	private static String OUT = "c:/users/braun/desktop/output.pdf";

	private static String endOfLineMarker = ";";
	private static String commandMarker = "$$";

	private static String DateCommand = "Date";
	private static String KundeCommand = "Kunde";
	private static String AddresseCommand = "Addresse";
	private static String SignaturCommand = "Signatur";

	public static void main(String[] args) {
		// Fontmap
		fontMap.put("HELVETICA", Font.FontFamily.HELVETICA);
		fontMap.put("COURIER", Font.FontFamily.COURIER);
		// FontStyle Map
		fontStyleMap.put("BOLD", Font.BOLD);
		fontStyleMap.put("NORMAL", Font.NORMAL);
		fontStyleMap.put("ITALIC", Font.ITALIC);
		fontStyleMap.put("ITALICBOLD", Font.BOLDITALIC);
		fontStyleMap.put("STRIKETHRU", Font.STRIKETHRU);
		

		try {
			// Create PDF to write
			Document document = createPdf(OUT);
			document.open();

			// Add TextFile Content
			addTextFileContent(document, IN);
			document.close();

		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	//Meta deta collection and assigning default values. 
	private static void setDefaults(String text) {
		metaStyle metaStyle = getMetaStyle(text);
		
		
		defaultFont = metaStyle.fontFamily;
		defaultFontSize = metaStyle.fontSize;
		defaultFontStyle = metaStyle.fontStyle;
			
	}
	
	private static metaStyle getMetaStyle(String text) {
			metaStyle thisMetaStyle = new metaStyle(Font.FontFamily.COURIER, 11, Font.NORMAL);
			
			String raw = getStringInCommand(text, metaStyleCommand);
			
			if(raw.contains(fontFamilyCommand))thisMetaStyle.fontFamily = fontMap.get(getStringInCommand(raw, fontFamilyCommand));
			if(raw.contains(fontSizeCommand))thisMetaStyle.fontSize = Integer.parseInt(getStringInCommand(raw, fontSizeCommand));
			if(raw.contains(fontStyleCommand))thisMetaStyle.fontStyle = fontStyleMap.get(getStringInCommand(raw, fontStyleCommand));
			
			return thisMetaStyle;
	}
	
	private static String getStringInCommand(String text, String command) {
		
		List<Integer> commandIndexList = new ArrayList<Integer>();
		int index = text.indexOf(command);
		while (index != -1) {
			commandIndexList.add(index);
			index = text.indexOf(command, index + 1);
		}
		
		String content = text.substring(commandIndexList.get(0),commandIndexList.get(1));
		content = cleanStringFromCommand(content, command);
		
		return content;
	}
	
	private static Document createPdf(String path) throws FileNotFoundException, DocumentException {
		Document document = new Document();
		PdfWriter.getInstance(document, new FileOutputStream(path));
		return document;

	}

	private static void addTextFileContent(Document document, String path) throws IOException, DocumentException {
		// read Textfile and dump each line into a list.
		BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(new File(path)), "UTF8"));

		String line;
		List<String> splittedText = new ArrayList<String>();
		while ((line = br.readLine()) != null) {
			splittedText.add(line.split(endOfLineMarker)[0]);
		}

		//addingCommandBlocks
		String afterCommands = "";
		for (String newLine : splittedText) {
			afterCommands += (addCommands(newLine) + "\n");
		}
		
		//getting metadata
		setDefaults(afterCommands);
		/////////////////

		List<Marker> currentMarker = new ArrayList<Marker>();
		currentMarker = addMarkers(currentMarker, afterCommands);

		Paragraph p = new Paragraph();
		
		
		p = formatText(currentMarker, afterCommands);

		document.add(p);
	}

	private static Paragraph formatText(List<Marker> currentMarker, String text) {
		Paragraph p = new Paragraph();

		Boolean[] activeMods = new Boolean[markUpCollection.length];
		for(int i = 0; i < activeMods.length; i++) {
			activeMods[i] = false;
		}
	
		Font activeFont;
		
		int lastIndex = 0;
		for (Marker marker : currentMarker) {
			// cut text
			String strip = text.substring(lastIndex, marker.index);
			// remove unwanted markers
			strip = cleanText(strip);
			// apply correct font
			activeFont = findCorrectFont(activeMods);
			// add to p
			p.add(new Phrase(strip, activeFont));
			// update modifiers
			activeMods = markerToggle(activeMods, marker.modifier);
			// updateCurrentindex
			lastIndex = marker.index;
		}

		String lastStrip = cleanText(text.substring(lastIndex));
		activeFont = findCorrectFont(activeMods);
		p.add(new Phrase(lastStrip, activeFont));

		return p;
	}

	private static String cleanText(String strip) {
		for(String markerType : markUpCollection) {
			strip = cleanStringFromCommand(strip, markerType);
		}
		return strip;
	}
	
	private static String cleanStringFromCommand(String input, String command) {
		String strip = input;
		try {
			strip = strip.replaceAll(command, "");
		}
		catch (Exception e){
			strip = strip.replaceAll("\\" + command, "");
		}
		return strip;
	}

	private static Boolean[] markerToggle(Boolean[] current, Boolean[] modifier) {

		Boolean[] newActives = current;
		int i = 0;
		for (Boolean mod : modifier) {
			if (current[i] && mod) {
				newActives[i] = false;
			} else if (mod) {
				newActives[i] = true;
			} else {
				newActives[i] = current[i];
			}

			i++;
		}

		return newActives;
	}

	private static Font findCorrectFont(Boolean[] modifier) {
		Font correctFont = new Font();

		Font.FontFamily fontFamily = defaultFont;
		int fontSize = defaultFontSize;
		int fontStyle = defaultFontStyle;

		// check for font style
		if (modifier[2]) {
			fontStyle = monoSpaceStroke;
		} else if (modifier[0] && modifier[1] && !modifier[2]) {
			fontStyle = monoSpaceBoldItalic;
		} else if (modifier[0]) {
			fontStyle = monoSpaceBold;
		} else {
			fontStyle = monoSpace;
		}

		// apply font family
		if (modifier[3]) {
			fontFamily = Font.FontFamily.COURIER;
		}
		if (modifier[4]) {
			fontFamily = Font.FontFamily.TIMES_ROMAN;
		}

		return new Font(fontFamily, fontSize, fontStyle);
	}

	private static String addCommands(String line) {
		// adding Date
		String addedCommands = line.replace(commandMarker + DateCommand + commandMarker, now());

		// adding Kunde
		addedCommands = addedCommands.replace(commandMarker + KundeCommand + commandMarker, "Peter Hanse");

		// adding Adresse
		addedCommands = addedCommands.replace(commandMarker + AddresseCommand + commandMarker,
				"Lindenstraße 11, 47506 Neukirchen-Vluyn");

		// adding Signatur
		addedCommands = addedCommands.replace(commandMarker + SignaturCommand + commandMarker,
				"Lieb Grüße \nAlexander Braun");

		return addedCommands;
	}

	private static List<Marker> addMarkers(List<Marker> currentMarker, String text) {
		//create markers for each marker type in the markUp array
		int a = 0;
		for(String markerType : markUpCollection) {
			int index = text.indexOf(markerType);
			while (index != -1) {
				//Create marker for marker type a
				Marker thisMarker = new Marker(index, new Boolean[markUpCollection.length]);
				//fill in the array that defines the makrer tpye --> this is dumb idk
				for(int i = 0; i < markUpCollection.length; i++) {
					thisMarker.modifier[i] = false;
					if(i == a) {
						thisMarker.modifier[i] = true;
					}
				}
				// add the marker to the list
				currentMarker.add(thisMarker);
				// advance index
				index = text.indexOf(markerType, index + 1);
			}
			a++;
		}

		currentMarker.sort(new Comparator<Marker>() {
			public int compare(Marker m1, Marker m2) {
				return m1.index.compareTo(m2.index);
			}
		});

		return currentMarker;
	}

	private static String now() {
		Calendar cal = Calendar.getInstance();
		SimpleDateFormat sdf = new SimpleDateFormat(DATE_FORMAT_NOW);
		return sdf.format(cal.getTime());
	}

}

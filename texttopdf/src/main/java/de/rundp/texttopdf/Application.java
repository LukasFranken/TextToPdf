package de.rundp.texttopdf;

import java.io.FileOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;
import java.util.regex.Pattern;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Comparator;
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
	private static Font monoSpace = new Font(Font.FontFamily.COURIER, 11, Font.NORMAL);
	private static Font monoSpaceBold = new Font(Font.FontFamily.COURIER, 11, Font.BOLD);
	private static Font monoSpaceItalic = new Font(Font.FontFamily.COURIER, 11, Font.ITALIC);
	private static Font monoSpaceStroke = new Font(Font.FontFamily.COURIER, 11, Font.STRIKETHRU);
	private static Font monoSpaceBoldItalic = new Font(Font.FontFamily.COURIER, 11, Font.BOLDITALIC);
	
	private static String boldMarker = "**";
	private static String italicMarker = "§§";
	private static String strikeMarker = "--";

	private static String IN = "c:/users/braun/desktop/input.txt";
	private static String OUT = "c:/users/braun/desktop/output.pdf";

	private static String endOfLineMarker = ";";
	private static String commandMarker = "$$";

	private static String DateCommand = "Date";
	private static String KundeCommand = "Kunde";
	private static String AddresseCommand = "Addresse";
	private static String SignaturCommand = "Signatur";

	public static void main(String[] args) {
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

		
		String afterCommands = "";
		for (String newLine : splittedText) {
			afterCommands += (addCommands(newLine) + "\n");
		}
		
		List<Marker> currentMarker = new ArrayList<Marker>();
		currentMarker = addMarkers(currentMarker, afterCommands);
		
		Paragraph p = new Paragraph();
		p = formatText(currentMarker, afterCommands);
		
		document.add(p);
		
		
	}
	
	private static Paragraph formatText(List<Marker> currentMarker, String text) {
		Paragraph p = new Paragraph();
		
		Boolean[] activeMods = new Boolean[]{false,false,false};
		Font activeFont = monoSpace;
		int lastIndex = 0;
		
		for(Marker marker : currentMarker) {
			//cut text
			String strip = text.substring(lastIndex, marker.index);
			//remove unwanted markers
			strip = strip.replaceAll("\\" + boldMarker, "");
			strip = strip.replaceAll("\\" + italicMarker, "");
			strip = strip.replaceAll("\\" + strikeMarker, "");
			//apply correct font
			activeFont = findCorrectFont(activeMods);
			//add to p
			p.add(new Phrase(strip, activeFont));
			//update modifiers
			activeMods = markerToggle(activeMods, marker.modifier);
			//updateCurrentindex
			lastIndex = marker.index;
		}
		return p;
	}

	private static Boolean[] markerToggle(Boolean[] current,Boolean[] modifier) {
		
		Boolean[] newActives = current;
		int i = 0;
		for(Boolean mod: modifier)
		{
			if(current[i] && mod) {
				newActives[i] = false;
			}
			else if(mod) {
				newActives[i] =  true;
			}
			else {
				newActives[i] = current[i];
			}
			
			i++;
		}
		
		return newActives;
	}
	
	private static Font findCorrectFont(Boolean[] modifier) {
		Font correctFont = new Font();
		if(modifier[2]) {
			correctFont = monoSpaceStroke;
		}
		else if(modifier[0] && modifier[1] && !modifier[2]) {
			correctFont = monoSpaceBoldItalic;
		}
		else if(modifier[0]) {
			correctFont = monoSpaceBold;
		}
		else {
			correctFont = monoSpace;
		}
		return correctFont;
	}
	
	
	
	
	private static String addCommands(String line) {
		// adding Date
		String addedCommands = line.replace(commandMarker + DateCommand + commandMarker, now());

		// adding Kunde
		addedCommands = addedCommands.replace(commandMarker + KundeCommand + commandMarker, "Peter Hanse");

		// adding Adresse
		addedCommands = addedCommands.replace(commandMarker + AddresseCommand + commandMarker, "Lindenstraße 11, 47506 Neukirchen-Vluyn");

		// adding Signatur
		addedCommands = addedCommands.replace(commandMarker + SignaturCommand + commandMarker,"Lieb Grüße \nAlexander Braun");

		return addedCommands;
	}
	
	
	private static List<Marker> addMarkers (List<Marker> currentMarker, String text){
		//fill list with bold markers
        int indexB = text.indexOf(boldMarker);
        while (indexB != -1) {
            currentMarker.add(new Marker(indexB, new Boolean[]{true,false,false}));
            indexB = text.indexOf(boldMarker, indexB + 1);
        }
      //fill list with italic markers
        int indexI = text.indexOf(italicMarker);
        while (indexI != -1) {
            currentMarker.add(new Marker(indexI, new Boolean[]{false,true,false}));
            indexI = text.indexOf(italicMarker, indexI + 1);
        }
      //fill list with strik markers
        int indexS = text.indexOf(strikeMarker);
        while (indexS != -1) {
            currentMarker.add(new Marker(indexS, new Boolean[]{false,false,true}));
            indexS = text.indexOf(strikeMarker, indexS + 1);
        }
        
        currentMarker.sort(new Comparator<Marker>() {       	
        	public int compare(Marker m1, Marker m2)
            {
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

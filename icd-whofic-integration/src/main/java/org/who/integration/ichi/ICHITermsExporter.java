package org.who.integration.ichi;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.who.integration.util.StringUtils;

public class ICHITermsExporter {
	
	private static transient Logger log = Logger.getLogger(ICHITermsExporter.class);
	
	public static final String COL_SEPARATOR = "\t";
	public static final String VALUE_SEPARATOR = "; *|\\\\n";
	
	public static final String NOT_ELSEWHERE_CLASSIFIED = ", not elsewhere classified";
	public static final String NOS1_CLASSIFIED = ", NOS";
	
	public static final String THAT = "that";
	
	public static final String INCLUSION_SEPARATOR = "-";
	public static final String INCLUSION_SEPARATOR_VIA = "via";
	public static final String INCLUSION_SEPARATOR_USING = "using";
	public static final String INCLUSION_SEPARATOR_BY = "by";
	public static final String INCLUSION_SEPARATOR_WITH = "with";
	
	public static final String ATTACHMENT_BY = "attachment by";
	public static final String ATTACHED_BY = "attached by";
	
	
	public static final List<String> NAME = Arrays.asList("Dextran", "Barlow", "Ludwig", "Mullerian",
			"McDonald", "Shirodkar", "Skene", "Ortolani", "Nissen", "Takeuchi", "Baffle", "Rashkind",
			"Cervagem", "Prostoglandin", "Prostin", "Oxytocin", "Syntocinon", "Argon", "Boari", "Sano",
			"Wolff"
			);
	
	public static final List<String> INCLUSION_PREPOSITIONS = Arrays.asList("via", "by", "for", "with",
			"performed", "in", "using", "under");
	
	public static final List<String> VIA_METHOD_ENDINGS = Arrays.asList("opy", "omy", "osis", "lation", "plasty");
	
	public static final List<String> VIA_METHOD_CONTAINS = Arrays.asList("flap");
	
	public static final List<String> USING_METHOD_CONTAINS = Arrays.asList("method", "approach", "technique",
			"donor limbal stem cells");
	
	public static final List<String> WITH_CONTAINS = Arrays.asList("debridement");
	
	public static final List<String> BY_METHOD_CONTAINS = Arrays.asList("insertion", "removing", "removal");
	
	private BufferedWriter outputWriter;
	
	private Map<String, String> term2warn = new HashMap<String, String>();
	
	public static void main(String[] args) throws IOException {
		if (args.length < 2) {
			log.error("Expected 3 arguments: (1) ICHI CSV file;"
					+ " (2) Terms output CSV file; "
					+ "(3) Optional: Expansion type: "
					+ "(a) title + inclusion [TI], or "
					+ "(b) index + inclusion [II], or"
					+ "(c) both [TII], "
					+ "default: TII");
			System.exit(1);
		}
		
		ICHITermsExporter exporter = new ICHITermsExporter();
		exporter.setOutputWriter(new BufferedWriter(new FileWriter(new File(args[1]))));
		
		String expandIndexTerms = args.length == 3 ? args[2] : "TII";
		
		exporter.exportTerms(args[0], expandIndexTerms);
		
		exporter.closeWriter();
	}
	

	private void exportTerms(String inputCSV, String expandIndexTerms) throws IOException {
		BufferedReader csvReader = null;
		
		csvReader = new BufferedReader(new FileReader(inputCSV));
		
		if (expandIndexTerms.equalsIgnoreCase("TI")) {
			writeExpandedTitleHeader(); 
		} else {
			writeExpandedIndexHeader();
		}
		
		int lineCount = 0;
		
		String row = null;
		try {
			while (( row = csvReader.readLine()) != null) {
				processLine(row, expandIndexTerms);
				if (lineCount % 100 == 0) {
					//log.info("Processed " + lineCount + " lines");
				}
				lineCount ++;
			}
		} catch (IOException e) {
			log.error("IO Exception at processing row: " + row, e);
		}
		csvReader.close();
	}
	
	protected void processLine(String row, String expandIndexTerms) throws IOException {
		String[] data = row.split(COL_SEPARATOR);
		
		String ichiCode = getString(data, 3);
		String title = getString(data, 4);
		//String definition = getString(data, 5);
		String indexTerms = getString(data, 6);
		
		String inclNotes = getString(data, 7);
		//String exclusion = getString(data, 9);
		
		ichiCode = makeReplacements(ichiCode);
		title = makeReplacements(title);
		indexTerms = makeReplacements(indexTerms);
		String inclusions = makeReplacements(inclNotes);
		
		List<String> inclusionList = getTerms(ichiCode, inclusions, "INCL NOTES");
		
		if (inclusionList.size() == 0) {
			return;
		}
		
		if (expandIndexTerms.equalsIgnoreCase("TI")) {
			processExpandedTitle(ichiCode, title, inclusions, inclusionList);
		} else {
			List<String> indexTermsList = getTerms(ichiCode, makeReplacements(indexTerms), "INDEX TERMS");
			processExpandedIndex(ichiCode, title, indexTermsList, inclusionList, indexTerms, inclNotes,
					expandIndexTerms.equalsIgnoreCase("TII"));
		}
		
	}


	private void processExpandedTitle(String ichiCode, String title, String terms, List<String> inclusionList) throws IOException {
		
		writeExpandedTitleLine(ichiCode, title, "", "", terms, "");
		
		for (int i = 0; i < inclusionList.size(); i++) {
			String incl = inclusionList.get(i);
			String expandedIncl = getExpandedInclusion(ichiCode, title, incl);
			writeExpandedTitleLine("", "", incl, expandedIncl, "", term2warn.get(incl));
		}
	}


	private void processExpandedIndex(String ichiCode, String title, List<String> indexTermsList,
			List<String> inclusionList, String fullIndexTerms, String fullInclusions, 
			boolean expandAlsoTitleWithIncl) throws IOException {
		
		writeExpandedIndexLine(ichiCode, title, "", "", "", fullIndexTerms, fullInclusions, "");
		
		if (expandAlsoTitleWithIncl == true) {
			for (int i = 0; i < inclusionList.size(); i++) {
				String incl = inclusionList.get(i);
				String expandedIncl = getExpandedInclusion(ichiCode, title, incl);
				writeExpandedIndexLine("", "", "", incl, expandedIncl, "", "", term2warn.get(ichiCode + title + incl));
			}
		}
		
		for (int i = 0; i < indexTermsList.size(); i++) {
			String indexTerm = indexTermsList.get(i);
			String expandedIndexTerm = getExpandedIndex(ichiCode, title, indexTerm);
			//String warningIndex = term2warn.get(indexTerm);
			
			writeExpandedIndexLine("", "", indexTerm, "", "", "", "", "");
					
			for (int j = 0; j < inclusionList.size(); j++) {
				String incl = inclusionList.get(j);
				String expandedFullName = getExpandedInclusion(ichiCode, expandedIndexTerm, incl);
				String inclWarning = term2warn.get(ichiCode + expandedIndexTerm + incl);
				
				writeExpandedIndexLine("", "", "", incl, expandedFullName, 
						"", "", inclWarning);
			}
		}
	}
	
	private void writeExpandedTitleLine(String ichiCode, String title, String term, String expandedTerm, String fullTerm, String warning) throws IOException {
		outputWriter.write(
				ichiCode + COL_SEPARATOR + 
				title + COL_SEPARATOR +
				term + COL_SEPARATOR +
				expandedTerm + COL_SEPARATOR +
				fullTerm + COL_SEPARATOR +
				warning);
		outputWriter.newLine();
	}
	
	private void writeExpandedIndexLine(String ichiCode, String title, String indexTerm, String inclusion, 
			String expandedTerm, String fullIndexTerms, String fullInclusions, String warning) throws IOException {
		
		warning = warning == null ? "" : warning;
		
		outputWriter.write(
				ichiCode + COL_SEPARATOR + 
				title + COL_SEPARATOR +
				indexTerm + COL_SEPARATOR +
				inclusion + COL_SEPARATOR +
				expandedTerm + COL_SEPARATOR +
				warning + COL_SEPARATOR +
				fullIndexTerms + COL_SEPARATOR +
				fullInclusions 
				);
		outputWriter.newLine();
	}


	protected List<String> getTerms(String ichiCode, String fullText, String errorType) {
		List<String> terms = new ArrayList<String>();
		
		if (fullText == null || fullText.length() == 0) {
			return terms;
		}
		
		String[] termsArray = fullText.split(ICHIImporter.VALUE_SEPARATOR);
		
		String prefix = "";
		for (int i = 0; i < termsArray.length; i++) {
			String term = termsArray[i];
			term = term.trim();
			
			if (term.length() == 0) {
				continue;
			}
			
			String warning = "";
			
			//a new prefix is starting
			if (term.endsWith(":")) {
				prefix = term.substring(0, term.length() - 1);
				prefix = prefix.trim();
				continue;
			} else if (term.contains(":")) {
				int colonIndex = term.indexOf(":");
				
				String preTerm = term.substring(0, colonIndex);
				preTerm = preTerm.trim();
				
				term = term.substring(colonIndex + 1);
				term = term.trim();
				
				if (isStartOfNewTerm(term) == true) { //wrong separator
					terms.add(preTerm);
					term2warn.put(preTerm, "");
					warning = "Possibly wrong separator between this and the previous term ':'"; //warning for term, not preterm
					reportWarning(ichiCode, errorType, warning, term, fullText);
				} else {
					prefix = preTerm;
				}
			} else if (prefix.length() > 0 &&  //TODO: maybe check if it is the first term after :
					isStartOfNewTerm(term)) { 
				//If we had a prefix, and the term starts with uppercase and is not an abbrev, 
				//then it is the end of the prefix
				prefix = "";
				warning = "End prefix";
				reportWarning(ichiCode, errorType, warning, term, fullText);
			}
			
			term = prefix.length() > 0 ? prefix + " " + term : term;
			
			terms.add(term);
			term2warn.put(term, warning);
		}
		
		return terms;
	}
	
	private String getExpandedIndex(String ichiCode, String title, String index) {
		if (index.toLowerCase().startsWith(THAT)) {
			return title + " " + index.substring(5).trim();
		}
		return index;
	}
	
	
	private String getExpandedInclusion(String ichiCode, String baseTerm, String term) {
		
		if (baseTerm.contains(term)) {
			term2warn.put(ichiCode + baseTerm + term, "Include note repeated in index/title");
			reportWarning(ichiCode, "INCLUSION", "Repeat", term, "Include note repeated in index/title: " + baseTerm);
		}
		
		String str = getExpandedInclusionUnhandledNEC(ichiCode, baseTerm, term);
		str = moveNECAtEnd(str);
		
		//replace "unspecified site
		str = str.replaceAll(", unspecified site", " to unspecified site");
		str = str.replaceAll(", site unspecified", " to unspecified site");
		
		List<String> repeatedWords = StringUtils.getRepeatedWords(str);
		if (repeatedWords.size() > 0) {
			term2warn.put(ichiCode + baseTerm + term, "Repeated words: " + repeatedWords);
			reportWarning(ichiCode, "GENERATED TERM", "Repeated words", term, "Repeated words in generated term: " + repeatedWords +
					". Generated term: " + str);
		}
		
		return str;
	}
	
	//it may or may not contain ", not elsewhere classified"
	private String getExpandedInclusionUnhandledNEC(String ichiCode, String baseTerm, String term) {
		String lcTerm = term.toLowerCase();
		
		/*
		if (baseTerm.contains(term)) {
			term2warn.put(ichiCode+baseTerm+term, "Repeated include note in base term");
			reportWarning(ichiCode, "INCLUDE", "Repeated include note in base term", term,
					"Repeated include note in base term. Base term: " + baseTerm);
			System.out.println("Repeated include note in base term. Base term: " + baseTerm + ". Include note: " + term);
			return "";
		}
		*/
		
		if (lcTerm.startsWith(THAT)) {
			return baseTerm + " " + term.substring(5).trim();
		}
		
		if (lcTerm.startsWith(ATTACHMENT_BY)) {
			return baseTerm + " " + ATTACHED_BY + " " + term.substring(ATTACHMENT_BY.length()).trim();
		}
		
		if (lcTerm.startsWith("includes")) {
			lcTerm = lcTerm.substring(8).trim();
			term = term.substring(8).trim();
		}
		
		for (String prep : INCLUSION_PREPOSITIONS) {
			if (lcTerm.startsWith(prep + " ")) {
				return baseTerm + " " + term;
			}
		}
		
		for (String ending : VIA_METHOD_ENDINGS) {
			if (lcTerm.matches(".*" + ending + "\\s*")) {
				if (isUppercase(term) && isAbbrev(term) == false) {
					term = Character.toLowerCase(term.charAt(0)) + term.substring(1);
				}
				//System.out.println(title + " " + INCLUSION_SEPARATOR_VIA + " " + term);
				return baseTerm + " " + INCLUSION_SEPARATOR_VIA + " " + term;
			}
		}
		
		for (String str : USING_METHOD_CONTAINS) {
			if (lcTerm.matches(".*" + str + ".*")) {
				if (isUppercase(term) && isAbbrev(term) == false) {
					term = Character.toLowerCase(term.charAt(0)) + term.substring(1);
				}
				//System.out.println(title + " " + INCLUSION_SEPARATOR_USING + " " + term);
				return baseTerm + " " + INCLUSION_SEPARATOR_USING + " " + term;
			}
		}
		
		for (String str : BY_METHOD_CONTAINS) {
			if (lcTerm.matches(".*" + str + ".*")) {
				if (isUppercase(term) && isAbbrev(term) == false) {
					term = Character.toLowerCase(term.charAt(0)) + term.substring(1);
				}
				//System.out.println(title + " " + INCLUSION_SEPARATOR_BY + " " + term);
				return baseTerm + " " + INCLUSION_SEPARATOR_BY + " " + term;
			}
		}
		
		for (String str : VIA_METHOD_CONTAINS) {
			if (lcTerm.matches(".*" + str + ".*")) {
				if (isUppercase(term) && isAbbrev(term) == false) {
					term = Character.toLowerCase(term.charAt(0)) + term.substring(1);
				}
				//System.out.println(title + " " + INCLUSION_SEPARATOR_BY + " " + term);
				return baseTerm + " " + INCLUSION_SEPARATOR_VIA + " " + term;
			}
		}
		
		for (String str : WITH_CONTAINS) {
			if (lcTerm.matches(".*" + str + ".*")) {
				if (isUppercase(term) && isAbbrev(term) == false) {
					term = Character.toLowerCase(term.charAt(0)) + term.substring(1);
				}
				//System.out.println(title + " " + INCLUSION_SEPARATOR_BY + " " + term);
				return baseTerm + " " + INCLUSION_SEPARATOR_WITH + " " + term;
			}
		}
		
		//handle "graft"
		if (lcTerm.matches(".*graft\\b.*")) {
			String prep = INCLUSION_SEPARATOR_WITH;
			
			if (baseTerm.matches(".*\\b" + INCLUSION_SEPARATOR_WITH + "\\b.*")) {
				prep = INCLUSION_SEPARATOR_VIA;
			}
			
			//System.out.println(baseTerm + " " + prep + " " + term);
			return baseTerm + " " + prep + " " + term;
		}
		
		if (lcTerm.contains("breastfeeding")) {
			int bfIndex = baseTerm.indexOf("breastfeeding");
			if (bfIndex > 0) {
				// System.out.println(baseTerm.substring(0, bfIndex -1) + " the " + term);
				return baseTerm.substring(0, bfIndex -1) + " the " + term;
			}
		}
		
		if (lcTerm.contains("digital technology")) {
			int bfIndex = baseTerm.indexOf("digital technology");
			if (bfIndex > 0) {
				//System.out.println(baseTerm.substring(0, bfIndex -1) + " " + term);
				return baseTerm.substring(0, bfIndex -1) + " " + term;
			}
		}
		
		if (lcTerm.contains("electrode or lead") && baseTerm.contains("electrode or lead")) {
			String str = baseTerm.replaceAll("electrode or lead", term);
			//System.out.println(str);
			return str;
		}
		
		if (lcTerm.contains("online gambling") && baseTerm.contains("gambling")) {
			String str = baseTerm.replaceAll("gambling", term);
			//System.out.println(str);
			return str;
		}
		
		if (lcTerm.contains("biopsy")) {
			String str = null;
			if (baseTerm.contains("biopsy")) {
				str = baseTerm.replaceAll("biopsy", term);
			} else if (baseTerm.contains("Biopsy")) {
				str = baseTerm.replaceAll("Biopsy", Character.toUpperCase(term.charAt(0)) + term.substring(1));
			}
			
			if (str != null) {
				//System.out.println(str);
				return str;
			}
		}
		
		if (lcTerm.contains("prosthesis") && baseTerm.contains("prosthesis")) {
			String[] protWordList = term.split(" ");
			String newTerm = term;
			if (protWordList.length == 2) {
				newTerm = protWordList[1] + " " + protWordList[0];
			}
			
			String str = baseTerm.replaceAll("prosthesis", newTerm);
			
			//System.out.println(str);
			
			return str;
		}
		
		if (baseTerm.contains("abdominal or pelvic artery")) {
			String str = term.replaceAll("[Aa]rtery", "").trim();
			if (str.matches("\\w+")) { //include note is only one word
				String ret = baseTerm.replaceAll("abdominal or pelvic artery", str + " artery");
				//System.out.println(ret);
				return ret;
			}
		}
		
		if (baseTerm.contains("vein of thorax")) {
			String str = term.replaceAll("[Vv]ein", "").trim();
			if (str.matches("\\w+")) { //include note is only one word
				String ret = baseTerm.replaceAll("vein of thorax", str + " vein");
				//System.out.println(ret);
				return ret;
			}
		}
		
		if (baseTerm.contains("drug use") && lcTerm.contains("drug use")) {
			String str = baseTerm.replaceAll("drug use", lcTerm);
			//System.out.println(str);
			return str;
		}
		
		String intersection = StringUtils.longestCommonToken(baseTerm, term);
		if (intersection != null && intersection.length() > 0  ){
				//&& (term.startsWith(intersection) || term.endsWith(intersection))) {
		
			String baseSubstr = baseTerm.substring(baseTerm.indexOf(intersection));
			//System.out.println(" -- " + baseTerm + "\t" + term + "\t" + intersection + "\t" + baseSubstr);
		}
		
		return baseTerm + " " + INCLUSION_SEPARATOR + " " + term;
	}

	
	/********************** Util methods *********************/
	
	
	private boolean isUppercase(String term) {
		//If it starts with an abbreviation, it should return false;
		if (term.length() > 1) {
			return Character.isUpperCase(term.charAt(0)) &&
					Character.isLowerCase(term.charAt(1));
		}
		
		return Character.isUpperCase(term.charAt(0));
	}
	
	private boolean isLowercase(String term) {
		return Character.isLowerCase(term.charAt(0));
	}
	
	private boolean isAbbrev(String term) {
		for (int i = 0; i < term.length(); i++) {
			char ch = term.charAt(i);
			
			if (Character.isUpperCase(ch) == false && Character.isDigit(ch) == false && 
					Character.isWhitespace(ch) == false) {
				return false;
			}
		}
		return true;
	}
	
	private boolean isStartOfNewTerm(String term) {
		return isUppercase(term) && 
				isAbbrev(term) == false &&
				isName(term) == false;
	}
	
	private boolean isName(String term) {
		for (String name : NAME) {
			if (term.startsWith(name)) {
				return true;
			}
		}
		return false;
	}
	
	private String moveNECAtEnd(String term) {
		//replace " NOS" with ", NOS" as it should be
		term = term.replaceAll("([^, ]) +NOS", "$1, NOS");
		
		String str = moveNECAtEnd(term, NOT_ELSEWHERE_CLASSIFIED);
		str = moveNECAtEnd(str, NOS1_CLASSIFIED);
		return str;
	}
	
	private String moveNECAtEnd(String term, String necString) {
		if (term.contains(necString) == false) {
			return term;
		}
		
		int index = term.indexOf(necString);
		int endIndex = index + necString.length();
		return term.substring(0, index) + " " + term.substring(endIndex).trim() + necString;
	}
	
	private void writeExpandedTitleHeader() throws IOException {
		writeExpandedTitleLine("ICHI code", "Title", "Include note", "Expanded title with include note", "Original full include notes", "Warning");
	}
	
	private void writeExpandedIndexHeader() throws IOException {
		writeExpandedIndexLine("ICHI code", "Title", "Index term", "Include note", "Expanded index with include note", "Original full index terms", "Original full include notes", "Warning");
	}


	protected String getString(String[] data, int index) {
		if (data.length <= index) {
			return null;
		}
		String text = data[index];
		
		return text == null ? null : text.trim();
	}

	
	private String makeReplacements(String term) {
		if (term == null) {
			return null;
		}
		//account for errors in the input csv
		
		
		if (term.startsWith("\"")) {
			term = term.substring(1);
		}
		if (term.endsWith("\"")) {
			term = term.substring(0, term.length()-1);
		}
		
		term = term.trim();
		
		if (term.endsWith(";")) {
			term = term.substring(0, term.length()-1);
		}
			
		term = term.replaceAll(";\\ \\:", ":;"); 
		
		term = term.trim();
		
		return term;
	}

	
	private void reportWarning(String code, String errorType, String suberrorType, String term, String fullText) {
		/*log.warn(code + COL_SEPARATOR +
				"CHECK " + errorType + COL_SEPARATOR +
				suberrorType + COL_SEPARATOR +
				fullText);
				*/
		System.out.println(code + COL_SEPARATOR +
				"CHECK " + errorType + COL_SEPARATOR +
				suberrorType + COL_SEPARATOR +
				term + COL_SEPARATOR +
				fullText);
				
	}
	
	public void setOutputWriter(BufferedWriter outputWriter) {
		this.outputWriter = outputWriter;
	}
	
	public void closeWriter() throws IOException {
		outputWriter.close();
	}
}

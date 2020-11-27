package org.who.integration.icf;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.who.integration.ichi.ICHIImporter;

import edu.stanford.bmir.whofic.icd.ICDContentModel;
import edu.stanford.smi.protege.model.Project;
import edu.stanford.smi.protegex.owl.model.OWLModel;
import edu.stanford.smi.protegex.owl.model.RDFResource;
import edu.stanford.smi.protegex.owl.model.RDFSNamedClass;

public class ImportICFDefinitions {
	private static transient Logger log = Logger.getLogger(ICHIImporter.class);
	
	public static final String COL_SEPARATOR = "\t";
	
	private static OWLModel owlModel;
	private static ICDContentModel cm;
	
	private static Map<String, String> code2def = new HashMap<String, String>();
	
	public static void main(String[] args) throws IOException {
		
		if (args.length < 2) {
			log.error("Needs 2 argument: (1) Path to pprj file; (2) CSV file with defs");
		}
		
		String log4jConfPath="log4j.properties";
	    PropertyConfigurator.configure(log4jConfPath);
		
		Project prj = Project.loadProjectFromFile(args[0], new ArrayList());
		owlModel = (OWLModel) prj.getKnowledgeBase();
		cm = new ICDContentModel(owlModel);
		
		fillMap(args[1]);
		addDefs();
		
		log.info("Saving..");
		prj.save(new ArrayList<>());
	}
	
	private static void fillMap(String inputCSV) throws IOException {
		BufferedReader csvReader = null;
		
		csvReader = new BufferedReader(new FileReader(inputCSV));
		
		int lineCount = 0;
		
		String row = null;
		try {
			while (( row = csvReader.readLine()) != null) {
				processLine(row);
				if (lineCount % 100 == 0) {
					log.info("Processed " + lineCount + " lines");
				}
				lineCount ++;
			}
		} catch (IOException e) {
			log.error("IO Exception at processing row: " + row, e);
		}
		csvReader.close();
	}

	private static void processLine(String row) {
		String[] data = row.split(COL_SEPARATOR);
		
		String icfClsName = data[0];
		String def = data[1];
		
		String existingDef = code2def.get(icfClsName);
		if (existingDef != null) {
			def = existingDef + "<br />" + def; //TODO: \n or <br>?
		}
		
		code2def.put(icfClsName, def);
		
	}
	
	private static void addDefs() {
		
		for (String icfClsName : code2def.keySet()) {
			String def = code2def.get(icfClsName);
			
			RDFSNamedClass cls = owlModel.getRDFSNamedClass(icfClsName);
			if (cls == null) {
				log.warn("Could not find class: " + icfClsName);
				continue;
			}
			
			RDFResource defTerm = cm.createDefinitionTerm();
			cm.fillTerm(defTerm, null, def, "en");
			cm.addDefinitionTermToClass(cls, defTerm);
			
			//System.out.println(icfClsName + "\t\"" + def + "\"");
		}
	}

}

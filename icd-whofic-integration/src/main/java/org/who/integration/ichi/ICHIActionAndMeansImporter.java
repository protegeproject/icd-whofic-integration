package org.who.integration.ichi;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

import org.apache.log4j.Logger;

import edu.stanford.smi.protege.model.Project;
import edu.stanford.smi.protegex.owl.model.OWLModel;
import edu.stanford.smi.protegex.owl.model.RDFSNamedClass;

public class ICHIActionAndMeansImporter {
	private static transient Logger log = Logger.getLogger(ICHIActionAndMeansImporter.class);
	
	public static final String COL_SEPARATOR = "\t";
	public static final String VALUE_SEPARATOR = ";|\\\\n";

	private OWLModel owlModel;
	private RDFSNamedClass topCls;

	
	public ICHIActionAndMeansImporter(OWLModel owlModel, RDFSNamedClass topCls) {
		this.owlModel = owlModel;
		this.topCls = topCls;
	}


	public static void main(String[] args) throws IOException {
		if (args.length < 3) {
			log.error("Expected 2 arguments: (1) PPRJ file to import into; (2) CSV file to import;"
					+ " (3) Top class under which to import content");
			System.exit(1);
		}
		
		Project prj = Project.loadProjectFromFile(args[0], new ArrayList());
		OWLModel owlModel = (OWLModel) prj.getKnowledgeBase();
		
		if (owlModel == null) {
			log.error("Could not load project file: " + args[0]);
			System.exit(1);
		}
		
		RDFSNamedClass topCls = owlModel.getRDFSNamedClass(args[2]);
		if (topCls == null) {
			log.error("Could not find top class: " + args[2]);
			System.exit(1);
		}
		
		ICHIActionAndMeansImporter importer = new ICHIActionAndMeansImporter(owlModel, topCls);
		importer.importCSV(args[1]);
		
		log.info("Saving project..");
		prj.save(new ArrayList());
		
		log.info("Done with import.");
		
	}


	private void importCSV(String inputCSV) throws IOException {
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

	private void processLine(String row) {
		String[] data = row.split(COL_SEPARATOR);
		
		String type = getString(data, 0);
		String action = getString(data, 1);
		String title = getString(data, 2);
		String definition = getString(data, 3);
		String inclusion = getString(data, 4);
		String exclusion = getString(data, 5);
		String codeAlso = getString(data, 6);
		
		if (action == null) {
			log.warn("Action column (icdTitle) is null for: " + row);
		}	
		
		if (type == null) {
			log.warn("Type column (superclass) is null for: " + row);
		}
		
		RDFSNamedClass cls = ICHIUtil.getOrCreateCls(owlModel, action);
		RDFSNamedClass superCls = ICHIUtil.getSupercls(owlModel, topCls, type);
		
		ICHIClassImporter clsImporter = new ICHIClassImporter(owlModel, cls);
		clsImporter.importCls(superCls, action, title, definition, inclusion, exclusion, codeAlso);
	}


	private String getString(String[] data, int index) {
		if (data.length <= index) {
			return null;
		}
		String text = data[index];
		
		return text == null ? null : text.trim();
	}
}

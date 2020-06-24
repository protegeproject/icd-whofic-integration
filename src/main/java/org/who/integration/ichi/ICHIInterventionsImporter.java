package org.who.integration.ichi;

import java.io.IOException;

import org.apache.log4j.Logger;

import edu.stanford.smi.protegex.owl.model.RDFSNamedClass;

public class ICHIInterventionsImporter extends ICHIImporter {
	
	private static transient Logger log = Logger.getLogger(ICHIInterventionsImporter.class);
	
	private static final String INTERVENTIONS_EXCLUSION_PATTERN = "(.*?)[ ,]*\\(([A-Z][A-Z][A-Z1-9][ .][A-Z][A-Z][ .][A-Z][A-Z])\\)(.*)";
	
	private static final String INTERVENTIONS_ON = "Interventions on ";
	
	public ICHIInterventionsImporter() {
		super();
	}
	
	public static void main(String[] args) throws IOException {
		if (args.length < 3) {
			log.error("Expected 3 arguments: (1) PPRJ file to import into; (2) CSV file to import;"
					+ " (3) Top class under which to import content");
			System.exit(1);
		}
		
		ICHIInterventionsImporter importer = new ICHIInterventionsImporter();
		importer.importClses(args[0], args[2], args[1]);
	}
	
	@Override
	protected void processLine(String row) {
		String[] data = row.split(COL_SEPARATOR);
		
		String chapter = getString(data, 0);
		String section = getString(data, 1);
		String group = getString(data, 2);
		
		String ichiCode = getString(data, 3);
		String title = getString(data, 4);
		String definition = getString(data, 5);
		String indexTerms = getString(data, 6);
		
		String inclNotes = getString(data, 7);
		String codeAlso = getString(data, 8);
		
		String exclusion = getString(data, 9);
		
		if (ichiCode == null) {
			log.warn("Target column (icdTitle) is null for: " + row);
		}	
		
		RDFSNamedClass cls = ICHIUtil.createInterventionCls(owlModel, ichiCode);
		
		group = getGroupName(group);
		RDFSNamedClass superCls = createSuperclses(chapter, section, group);
		
		InterventionClassImporter clsImporter = new InterventionClassImporter(owlModel, cls);
		clsImporter.importInterventionsCls(superCls, ichiCode, title, definition, indexTerms, 
				inclNotes, codeAlso, exclusion);
	}
	
	private RDFSNamedClass createSuperclses(String chapter, String section, String group) {
		RDFSNamedClass chapterSuperCls = ICHIUtil.getAtmSupercls(cm, topCls, chapter);
		RDFSNamedClass sectionSuperCls = ICHIUtil.getAtmSupercls(cm, chapterSuperCls, section);
		RDFSNamedClass groupSuperCls = ICHIUtil.getAtmSupercls(cm, sectionSuperCls, group);
		
		return groupSuperCls;
	}

	private String getGroupName(String title) {
		int index = title.indexOf("-");
		if (index > 0) { //not the best check
			title = title.substring(index + 1);
			title = title.trim();
		}
		title = INTERVENTIONS_ON + title;
		return title;
	}
	
	@Override
	protected String getExclusionCodePattern() {
		return INTERVENTIONS_EXCLUSION_PATTERN;
	}
}

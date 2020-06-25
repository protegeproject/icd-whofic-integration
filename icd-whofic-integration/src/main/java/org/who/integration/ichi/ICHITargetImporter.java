package org.who.integration.ichi;

import java.io.IOException;

import org.apache.log4j.Logger;

import edu.stanford.smi.protegex.owl.model.OWLModel;
import edu.stanford.smi.protegex.owl.model.RDFProperty;
import edu.stanford.smi.protegex.owl.model.RDFSNamedClass;

public class ICHITargetImporter extends ICHIImporter {
	
	private static transient Logger log = Logger.getLogger(ICHITargetImporter.class);
	
	private static final String TARGET_EXCLUSION_PATTERN = "(.*).\\(([A-Z][A-Z][A-Z1-9])\\)(.*)";

	
	public ICHITargetImporter() {
		super();
	}
	
	public static void main(String[] args) throws IOException {
		if (args.length < 3) {
			log.error("Expected 3 arguments: (1) PPRJ file to import into; (2) CSV file to import;"
					+ " (3) Top class under which to import content");
			System.exit(1);
		}
		
		ICHITargetImporter importer = new ICHITargetImporter();
		importer.importClses(args[0], args[2], args[1]);
	}
	
	@Override
	protected void init(OWLModel owlModel, RDFSNamedClass topCls) {
		super.init(owlModel, topCls);
	
		RDFProperty icfMapProp = ICHIUtil.getIcfMapProperty(getOwlModel());
		if (icfMapProp == null) {
			icfMapProp = owlModel.createAnnotationProperty(ICHIUtil.ICF_MAP_PROP);
		}
	}
	
	@Override
	protected void processLine(String row) {
		String[] data = row.split(COL_SEPARATOR);
		
		String chapter = getString(data, 0);
		String section = getString(data, 1);
		String group = getString(data, 2);
		
		String target = getString(data, 3);
		String title = getString(data, 4);
		String definition = getString(data, 5);
		String indexTerms = getString(data, 6);
		String exclusion = getString(data, 7);
		String icfMap = getString(data, 8);
		
		if (target == null) {
			log.warn("Target column (icdTitle) is null for: " + row);
		}	
		
		RDFSNamedClass cls = ICHIUtil.createAtmCls(owlModel, target);
		
		RDFSNamedClass superCls = createSuperclses(chapter, section, group);
		
		ICHIClassImporter clsImporter = new ICHIClassImporter(owlModel, cls);
		clsImporter.importTargetCls(superCls, target, title, definition, indexTerms, exclusion, icfMap);
	}
	
	private RDFSNamedClass createSuperclses(String chapter, String section, String group) {
		RDFSNamedClass chapterSuperCls = ICHIUtil.getSupercls(cm, topCls, chapter);
		RDFSNamedClass sectionSuperCls = ICHIUtil.getSupercls(cm, chapterSuperCls, section);
		RDFSNamedClass groupSuperCls = ICHIUtil.getSupercls(cm, sectionSuperCls, group);
		
		return groupSuperCls;
	}

	@Override
	protected String getExclusionCodePattern() {
		return TARGET_EXCLUSION_PATTERN;
	}
}

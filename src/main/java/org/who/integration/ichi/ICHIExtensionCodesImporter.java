package org.who.integration.ichi;

import java.io.IOException;

import org.apache.log4j.Logger;

import edu.stanford.smi.protegex.owl.model.RDFSNamedClass;

public class ICHIExtensionCodesImporter extends ICHIImporter {
	
	private static transient Logger log = Logger.getLogger(ICHIExtensionCodesImporter.class);
	
	//(XP100.10), (XB02.5), (XT11.03)
	private static final String EXT_CODES_EXCLUSION_PATTERN = "(.*).\\((X[\\w.]+)\\)(.*)";
	
	
	public ICHIExtensionCodesImporter() {
		super();
	}
	
	public static void main(String[] args) throws IOException {
		if (args.length < 3) {
			log.error("Expected 3 arguments: (1) PPRJ file to import into; (2) CSV file to import;"
					+ " (3) Top class under which to import content");
			System.exit(1);
		}
		
		ICHIExtensionCodesImporter importer = new ICHIExtensionCodesImporter();
		importer.importClses(args[0], args[2], args[1]);
	}
	
	//TODO: intermediate-level classes are missing
	//TODO: fix exclusion pattern
	
	@Override
	protected void processLine(String row) {
		String[] data = row.split(COL_SEPARATOR);
		
		String type = getString(data, 0);
		String parent = getString(data, 1);
		String xcode = getString(data, 2);
		String title = getString(data, 3);
		String definition = getString(data, 4);
		String indexTerms = getString(data, 5);
		String exclusion = getString(data, 6);
		String codeAlso = getString(data, 7);
		
		RDFSNamedClass cls = ICHIUtil.createAtmCls(owlModel, xcode);
		
		RDFSNamedClass superCls = null;
		if (parent == null || parent.length() == 0) {
			superCls = getTopCls();
			ICHIUtil.addToCode2ClsMap(title, cls);
		} else {
			superCls = ICHIUtil.getSupercls(cm, topCls, parent);
		}
		
		ICHIClassImporter clsImporter = new ICHIClassImporter(owlModel, cls);
		clsImporter.importAtmCls(superCls, xcode, title, definition, indexTerms, exclusion, codeAlso);
	}
	
	@Override
	protected String getExclusionCodePattern() {
		return EXT_CODES_EXCLUSION_PATTERN;
	}
	
}

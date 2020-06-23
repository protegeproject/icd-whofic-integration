package org.who.integration.ichi;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;

import edu.stanford.bmir.whofic.icd.ICDContentModel;
import edu.stanford.smi.protege.model.Project;
import edu.stanford.smi.protegex.owl.model.OWLModel;
import edu.stanford.smi.protegex.owl.model.RDFResource;
import edu.stanford.smi.protegex.owl.model.RDFSNamedClass;

public class ICHIImporter {

	private static transient Logger log = Logger.getLogger(ICHIImporter.class);
	
	public static final String COL_SEPARATOR = "\t";
	public static final String VALUE_SEPARATOR = "; *|\\\\n";
	
	protected OWLModel owlModel;
	protected ICDContentModel cm;
	protected RDFSNamedClass topCls;

	public void importClses(String prjName, String topClsName, String csvFilePath) throws IOException {
		
		Project prj = Project.loadProjectFromFile(prjName, new ArrayList());
		OWLModel owlModel = (OWLModel) prj.getKnowledgeBase();
		
		if (owlModel == null) {
			log.error("Could not load project file: " + prjName);
			System.exit(1);
		}
		
		RDFSNamedClass topCls = owlModel.getRDFSNamedClass(topClsName);
		if (topCls == null) {
			log.error("Could not find top class: " + topClsName);
			System.exit(1);
		}
		
		init(owlModel, topCls);
		
		importCSV(csvFilePath);
		
		log.info("Post-processing..");
		postprocess();
		
		log.info("Saving project..");
		prj.save(new ArrayList());
		
		log.info("Done with import.");
	}

	protected void init(OWLModel owlModel, RDFSNamedClass topCls) {
		this.owlModel = owlModel;
		this.cm = new ICDContentModel(owlModel);
		this.topCls = topCls;
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

	protected void processLine(String row) {
		String[] data = row.split(COL_SEPARATOR);
		
		String type = getString(data, 0);
		String action = getString(data, 1);
		String title = getString(data, 2);
		String definition = getString(data, 3);
		String indexTerms = getString(data, 4);
		String exclusion = getString(data, 5);
		String codeAlso = getString(data, 6);
		
		if (action == null) {
			log.warn("Action column (icdTitle) is null for: " + row);
		}	
		
		if (type == null) {
			log.warn("Type column (superclass) is null for: " + row);
		}
		
		RDFSNamedClass cls = ICHIUtil.createAtmCls(owlModel, action);
		RDFSNamedClass superCls = ICHIUtil.getAtmSupercls(cm, topCls, type);
		
		ICHIClassImporter clsImporter = new ICHIClassImporter(owlModel, cls);
		clsImporter.importCls(superCls, action, title, definition, indexTerms, exclusion, codeAlso);
	}

	private String getString(String[] data, int index) {
		if (data.length <= index) {
			return null;
		}
		String text = data[index];
		
		return text == null ? null : text.trim();
	}

	protected void postprocess() {
		addExclusions();
	}

	private void addExclusions() {
		log.info("Adding exclusions..");
		for (RDFSNamedClass cls : ICHIUtil.getClsesWithExclusions()) {
			addExclusions(cls);
		}
	}

	private void addExclusions(RDFSNamedClass cls) {
		for (String excl : ICHIUtil.getExclusions(cls)) {
			addExclusion(cls, excl);
		}
	}

	private void addExclusion(RDFSNamedClass cls, String excl) {
		ArrayList<String> codes = new ArrayList<String>();
		final Pattern CODE_PATTERN = Pattern.compile("(.*).\\(([A-Z][A-Z])\\)(.*)");
		Matcher m = CODE_PATTERN.matcher(excl);
		String label = excl;
		while ( m.find() ) {
		    codes.add(m.group(2));
		    label = m.group(1) + m.group(3);
		    m = CODE_PATTERN.matcher(label);
		}
		
		//System.out.println(cls.getName() + "\t" + excl + "\t" + label + "\t" + codes);
		
		if (codes.size() == 0) {
			log.warn("EXCLUSION: " + cls.getName() + " has no reference codes for exclusion " + excl);
			addIncompleteExclusion(cls, label);
		}
		
		if (codes.size() > 1) {
			log.warn("EXCLUSION: " + cls.getName() + " has multiple reference codes for exclusion " + excl + ": " + codes);
		}
		
		for (String code : codes) {
			addExclusion(cls, excl, label, code);
		}
		
	}

	private void addExclusion(RDFSNamedClass cls, String excl, String label, String code) {
		RDFSNamedClass exclCls = ICHIUtil.getCls(code);
		
		if (exclCls == null) {
			log.warn("EXCLUSION: Could not find excluded class: " + code + 
					" for class: " + cls.getName() + " and exclusion: " + excl);
		}
		
		if (label == null || label.length() == 0) {
			log.warn("EXCLUSION: No label for excluded class: " + code + 
					" for class: " + cls.getName() + " and exclusion: " + excl);
		}
		
		RDFResource exclTerm = cm.createBaseExclusionTerm();
		
		if (exclCls != null) {
			exclTerm.setPropertyValue(cm.getReferencedCategoryProperty(), exclCls);
		
			String title = cm.getTitleLabel(exclCls);
			if (label != null && label.length() > 0 && title.equalsIgnoreCase(label) == false) {
				exclTerm.setPropertyValue(cm.getLabelProperty(), label);
			}
		} else {
			if (label != null && label.length() > 0) {
				exclTerm.setPropertyValue(cm.getLabelProperty(), label);
			}
		}
		
		cm.addBaseExclusionTermToClass(cls, exclTerm);
	}

	private void addIncompleteExclusion(RDFSNamedClass cls, String label) {
		RDFResource exclTerm = cm.createBaseExclusionTerm();
		exclTerm.setPropertyValue(cm.getLabelProperty(), label);
		cm.addBaseExclusionTermToClass(cls, exclTerm);
	}

}
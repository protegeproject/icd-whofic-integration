package org.who.integration.ictm;

import java.util.HashSet;
import java.util.Set;

import org.apache.log4j.Logger;

import edu.stanford.bmir.whofic.icd.ICDContentModel;
import edu.stanford.smi.protegex.owl.model.OWLModel;
import edu.stanford.smi.protegex.owl.model.RDFSNamedClass;

public class ExcludedClasses {
	
	private final static Logger log = Logger.getLogger(ExcludedClasses.class);
			
	//Primary care relevant entities
	final public static String EXCLUDED_TOP_CLASS1 = "http://who.int/icd#780_378b7cb7_5b17_422d_b68b_e793f5e48664";
	
	//Special tabulation lists for mortality and morbidity
	final public static String EXCLUDED_TOP_CLASS2 = "http://who.int/icd#3264_98ae51f9_6af8_41ef_b732_06c5fb864693";
	
	//Multisystem diseases
	final public static String EXCLUDED_TOP_CLASS3 = "http://who.int/icd#15884_07368d94_d861_421b_b2a0_264c49b96108";

	//Supplementary codes
	final public static String EXCLUDED_TOP_CLASS4 = "http://who.int/icd#15235_07368d94_d861_421b_b2a0_264c49b96108";

	//to be retired
	final public static String EXCLUDED_TOP_CLASS5 = "http://who.int/icd#1575_52a94b8b_65cc_439a_b8e5_57058c37b3ab";

	//ICTM Signs and symptoms
	final public static String EXCLUDED_TOP_CLASS6 = "http://who.int/ictm#1428_63a13dac_fbb6_4e21_9a3c_f0e312d405a2";
	
	final public static String EXCLUDED_CLASSES[] = new String[]{
			EXCLUDED_TOP_CLASS1, EXCLUDED_TOP_CLASS2, EXCLUDED_TOP_CLASS3,
			EXCLUDED_TOP_CLASS4, EXCLUDED_TOP_CLASS5, EXCLUDED_TOP_CLASS6
	};
	
	final public static String TO_BE_RETIRED_STR = "to be retired";
	final public static String DECISION_TO_BE_MADE_STR = "needing a decision";
	
	
	//TODO: Also exclude:
	// - all obsolete classes
	// - all classes and subclasses of "to be retired" and "needing decision to be made"
	
	private OWLModel owlModel;
	private ICDContentModel cm;
	
	private Set<RDFSNamedClass> excludedClses = new HashSet<RDFSNamedClass>();
	
	public ExcludedClasses(OWLModel owlModel, ICDContentModel cm) {
		this.owlModel = owlModel;
		this.cm = cm;
		initExcludedClasses();
	}

	private void initExcludedClasses() {
		for (int i = 0; i < EXCLUDED_CLASSES.length; i++) {
			RDFSNamedClass cls = owlModel.getRDFSNamedClass(EXCLUDED_CLASSES[i]);
			if (cls == null) {
				log.warn("Could not find excluded class: " + EXCLUDED_CLASSES[i]);
			} else {
				excludedClses.add(cls);
			}
		}
	}
	
	public boolean isExcludedTopClass(RDFSNamedClass cls) {
		if (excludedClses.contains(cls)) {
			return true;
		}
		
		String title = cm.getTitleLabel(cls);
		if (title != null) {
			title = title.toLowerCase();
			
			if (title.contains(TO_BE_RETIRED_STR) || title.contains(DECISION_TO_BE_MADE_STR)) {
				return true;
			}
		}
		
		return false;
	}
	
}

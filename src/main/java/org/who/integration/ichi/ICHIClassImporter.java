package org.who.integration.ichi;

import edu.stanford.bmir.whofic.icd.ICDContentModel;
import edu.stanford.smi.protegex.owl.model.OWLModel;
import edu.stanford.smi.protegex.owl.model.RDFResource;
import edu.stanford.smi.protegex.owl.model.RDFSNamedClass;

public class ICHIClassImporter {

	private OWLModel owlModel;
	private ICDContentModel cm;
	private RDFSNamedClass cls;

	public ICHIClassImporter(OWLModel owlModel, RDFSNamedClass cls) {
		this.owlModel = owlModel;
		this.cm = new ICDContentModel(owlModel);
		this.cls = cls;
	}

	public void importCls(RDFSNamedClass superCls, String action, String title, String definition,
			String indexTerms, String exclusion, String codeAlso) {
		
		importICDCode(action);
		importTitle(title);
		importDefinition(definition);
		importIndexTerms(indexTerms); //TODO: maybe this is a coding note, not inclusion, to be clarified
		importExclusion(exclusion);
		importCodeAlso(codeAlso); //TODO - not sure how to import
		importPublicId(); //TODO - need more info
		
		addSuperCls(superCls);
	}


	private void importICDCode(String action) {
		cls.addPropertyValue(cm.getIcdCodeProperty(), action);
	}

	private void importTitle(String title) {
		if (title == null || title.length() == 0) {
			return;
		}
		
		RDFResource term = createTerm(cm.getTermTitleClass(), title, "en");
		cls.addPropertyValue(cm.getIcdTitleProperty(), term);
	}

	private void importDefinition(String definition) {
		if (definition == null || definition.length() == 0) {
			return;
		}
		
		RDFResource term = createTerm(cm.getTermDefinitionClass(), definition, "en");
		cls.addPropertyValue(cm.getDefinitionProperty(), term);
	}

	private void importIndexTerms(String indexTerms) {
		if (indexTerms == null || indexTerms.length() == 0) {
			return;
		}
		
		String[] inclArray = indexTerms.split(ICHIImporter.VALUE_SEPARATOR);
		
		for (int i = 0; i < inclArray.length; i++) {
			String indexTerm = inclArray[i];
			indexTerm = indexTerm.trim();
			
			if (indexTerm.length() == 0) {
				continue;
			}
			
			RDFResource term = createTerm(cm.getTermBaseInclusionClass(), indexTerm, "en");
			//cm.addBaseInclusionTermToClass(cls, term);
			 //TT - this is needed for the current ICD CM; not necessary in the merged CM
			cm.addBaseIndexTermToClass(cls, term);
		}
	}


	private void importExclusion(String exclusions) {
		if (exclusions == null || exclusions.length() == 0) {
			return;
		}

		String[] exclArray = exclusions.split(ICHIImporter.VALUE_SEPARATOR);
		
		for (int i = 0; i < exclArray.length; i++) {
			String excl = exclArray[i];
			excl = excl.trim();
			
			if (excl.length() > 0) {
				ICHIUtil.addExclusion(cls, excl);
			}
		}
	}

	private void importCodeAlso(String codeAlso) {
		// TODO Auto-generated method stub
		
	}
	
	private void addSuperCls(RDFSNamedClass superCls) {
		cls.addSuperclass(superCls);
		cls.removeSuperclass(owlModel.getOWLThingClass());
		
		//Add the sibling ordering. The classes come already sorted in the tsv file, 
		//so it should be fine to just add them to the index
		cm.addChildToIndex(superCls, cls, true);
	}
	
	private void importPublicId() {
		// TODO Auto-generated method stub
		//TT - need more info from WHO
	}
	
	private RDFResource createTerm(RDFSNamedClass termCls, String label, String lang) {
		RDFResource term = cm.createTerm(termCls);
		term.addPropertyValue(cm.getLabelProperty(), label);
		term.addPropertyValue(cm.getLangProperty(), lang);
		return term;
	}

}

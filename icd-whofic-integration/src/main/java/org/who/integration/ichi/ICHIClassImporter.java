package org.who.integration.ichi;

import java.util.ArrayList;
import java.util.Collection;

import org.apache.log4j.Logger;

import edu.stanford.bmir.whofic.icd.ICDContentModel;
import edu.stanford.smi.protegex.owl.model.OWLModel;
import edu.stanford.smi.protegex.owl.model.RDFResource;
import edu.stanford.smi.protegex.owl.model.RDFSNamedClass;

public class ICHIClassImporter {
	private static transient Logger log = Logger.getLogger(ICHIClassImporter.class);
			
	private OWLModel owlModel;
	private ICDContentModel cm;
	private RDFSNamedClass cls;

	public ICHIClassImporter(OWLModel owlModel, RDFSNamedClass cls) {
		this.owlModel = owlModel;
		this.cm = new ICDContentModel(owlModel);
		this.cls = cls;
	}

	public void importAtmCls(RDFSNamedClass superCls, String action, String title, String definition,
			String indexTerms, String exclusion, String codeAlso) {
		
		importICDCode(action);
		importTitle(title);
		importDefinition(definition);
		importIndexTerms(indexTerms);
		importExclusion(exclusion);
		importCodeAlso(codeAlso); //TODO - not sure how to import
		
		importPublicId(); //TODO - need more info
		importICHILin();
		
		addSuperCls(superCls);
	}

	
	public void importTargetCls(RDFSNamedClass superCls, String target, String title, String definition,
			String indexTerms, String exclusion, String icfMap) {
		
		importICDCode(target);
		importTitle(title);
		importDefinition(definition);
		importIndexTerms(indexTerms);
		importExclusion(exclusion);
		importIcfMap(icfMap); //TODO - not sure how to import
		
		importPublicId(); //TODO - need more info
		importICHILin();
		
		addSuperCls(superCls);
	}

	protected void importICDCode(String action) {
		cls.addPropertyValue(cm.getIcdCodeProperty(), action);
	}

	protected void importTitle(String title) {
		if (title == null || title.length() == 0) {
			return;
		}
		
		RDFResource term = createTerm(cm.getTermTitleClass(), title, "en");
		cls.addPropertyValue(cm.getIcdTitleProperty(), term);
	}

	protected void importDefinition(String definition) {
		if (definition == null || definition.length() == 0) {
			return;
		}
		
		RDFResource term = createTerm(cm.getTermDefinitionClass(), definition, "en");
		cls.addPropertyValue(cm.getDefinitionProperty(), term);
	}

	protected void importIndexTerms(String indexTerms) {
		Collection<String> terms = getTerms(indexTerms, "INDEX_TERM");
		
		for (String term : terms) {
			RDFResource termRes = createTerm(cm.getTermBaseIndexClass(), term, "en");
			//cm.addBaseInclusionTermToClass(cls, termRes);
			 //TT - this is needed for the current ICD CM; not necessary in the merged CM
			cm.addBaseIndexTermToClass(cls, termRes);
		}
	}


	protected void importExclusion(String exclusions) {
		Collection<String> terms = getTerms(exclusions, "EXCLUSION");
		
		for (String term : terms) {
			ICHIUtil.addExclusion(cls, term);
		}
	}
	
	protected Collection<String> getTerms(String fullText, String errorType) {
		Collection<String> terms = new ArrayList<String>();
		
		if (fullText == null || fullText.length() == 0) {
			return terms;
		}
		
		if (fullText.contains(":;") && fullText.contains("\\n")) {
			log.warn("CHECK " + errorType + ":\t" + getCls().getPropertyValue(cm.getIcdCodeProperty()) +
					"\t" + fullText);
		}
		
		String[] exclArray = fullText.split(ICHIImporter.VALUE_SEPARATOR);
		
		String prefix = "";
		for (int i = 0; i < exclArray.length; i++) {
			String term = exclArray[i];
			term = term.trim();
			
			if (term.length() == 0) {
				continue;
			}
			
			if (term.endsWith(":")) {
				prefix = term.substring(0, term.length() - 1);
				continue;
			}
			
			term = prefix.length() > 0 ? prefix + " " + term : term;
			
			terms.add(term);
		}
		
		return terms;
	}


	protected void importCodeAlso(String codeAlso) {
		// TODO Auto-generated method stub
		
	}
	
	protected void importInclusionNotes(String inclNotes) {
		// TODO Auto-generated method stub
		
	}

	private void importIcfMap(String icfMap) {
		if (icfMap == null || icfMap.length() == 0) {
			return;
		}
		
		RDFSNamedClass icfCls = ICHIUtil.getIcfClass(owlModel, icfMap);
		if (icfCls != null) {
			cls.addPropertyValue(ICHIUtil.getIcfMapProperty(owlModel), icfCls);
		} else {
			cls.addPropertyValue(ICHIUtil.getIcfMapProperty(owlModel), icfMap);
		}
	}

	
	protected void addSuperCls(RDFSNamedClass superCls) {
		cls.addSuperclass(superCls);
		cls.removeSuperclass(owlModel.getOWLThingClass());
		
		//Add the sibling ordering. The classes come already sorted in the tsv file, 
		//so it should be fine to just add them to the index
		cm.addChildToIndex(superCls, cls, true);
	}
	
	protected void importPublicId() {
		// TODO Auto-generated method stub
		//TT - need more info from WHO
	}
	
	protected void importICHILin() {
		ICHIUtil.addICHILin(getCm(), getCls(), true, false);
	}
	
	protected RDFResource createTerm(RDFSNamedClass termCls, String label, String lang) {
		RDFResource term = cm.createTerm(termCls);
		term.addPropertyValue(cm.getLabelProperty(), label);
		term.addPropertyValue(cm.getLangProperty(), lang);
		return term;
	}

	public RDFSNamedClass getCls() {
		return cls;
	}
	
	public ICDContentModel getCm() {
		return cm;
	}
	
	public OWLModel getOwlModel() {
		return owlModel;
	}
}

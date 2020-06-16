package org.who.integration.ictm;

import java.util.Collection;

import edu.stanford.bmir.whofic.IcdIdGenerator;
import edu.stanford.bmir.whofic.icd.ICDContentModel;
import edu.stanford.bmir.whofic.icd.ICDContentModelConstants;
import edu.stanford.smi.protege.util.Log;
import edu.stanford.smi.protegex.owl.model.OWLClass;
import edu.stanford.smi.protegex.owl.model.OWLModel;
import edu.stanford.smi.protegex.owl.model.OWLNamedClass;
import edu.stanford.smi.protegex.owl.model.RDFProperty;
import edu.stanford.smi.protegex.owl.model.RDFResource;
import edu.stanford.smi.protegex.owl.model.RDFSNamedClass;

public class ClassImporter {

	private OWLModel sourceOnt;
	private OWLModel targetOnt;
    
    private ICDContentModel sourceCM;
    private ICDContentModel targetCM;
    
    private RDFSNamedClass sourceCls;
    
    private boolean isICTM = false;
    

	public ClassImporter(RDFSNamedClass cls, OWLModel sourceOnt, OWLModel targetOnt, 
			ICDContentModel sourceCM, ICDContentModel targetCM, boolean isICTM) {
		this.sourceCls = cls;
		
		this.sourceOnt = sourceOnt;
		this.targetOnt = targetOnt;
		
		this.sourceCM = sourceCM;
		this.targetCM = targetCM;
		
		this.isICTM = isICTM;
	}
    
	public RDFSNamedClass importCls() {
		RDFSNamedClass cls = ICTMUtil.getOrCreateCls(targetOnt, sourceCls.getName());
		
		addTitle(cls);
		addDefinition(cls);
		addLongDefinition(cls);
		
		addFullySpecifiedTitle(cls);
		addCodingHint(cls);
		addNote(cls);
		
		addPublicId(cls);
		
		addSyns(cls);
		addNarrower(cls);
		
		addInclusions(cls);
		addExclusions(cls);
		
		addExternalCodes(cls);
		addLinearizations(cls);
		//addIsObosolte(cls);
		
		//addLogicalDefinition(cls);
		
		return cls;
	}


	private void addLinearizations(RDFSNamedClass cls) {
		Collection<RDFResource> linViews = ICTMUtil.getTopLevelLinViews(targetCM);
		for (RDFResource linView : linViews) {
			RDFResource linSpec = targetCM.getLinearizationSpecificationClass().createInstance(IcdIdGenerator.getNextUniqueId(targetOnt));
            linSpec.setPropertyValue(targetCM.getLinearizationViewProperty(), linView);
           
            linSpec.setPropertyValue(targetCM.getIsAuxiliaryAxisChildProperty(), Boolean.FALSE);

            if (linView.getName().equals(ICDContentModelConstants.LINEARIZATION_VIEW_MORBIDITY)) {
                linSpec.setPropertyValue(targetCM.getIsIncludedInLinearizationProperty(), Boolean.TRUE);
                linSpec.setPropertyValue(targetCM.getIsGroupingProperty(), isGroupingMorbidity());
            } else if (linView.getName().equals(ICDContentModelConstants.LINEARIZATION_VIEW_MORTALITY)) {
                linSpec.setPropertyValue(targetCM.getIsIncludedInLinearizationProperty(), Boolean.FALSE);
            }
            
            cls.addPropertyValue(targetCM.getLinearizationProperty(), linSpec);
		}
	}

	private Boolean isGroupingMorbidity() {
		Collection<RDFResource> lins = sourceCls.getPropertyValues(sourceCM.getLinearizationProperty());
		for (RDFResource lin : lins) {
			RDFResource linView = (RDFResource) lin.getPropertyValue(sourceCM.getLinearizationViewProperty());
			if (linView.getName().equals(ICDContentModelConstants.LINEARIZATION_VIEW_MORBIDITY)) {
				return (Boolean) lin.getPropertyValue(sourceCM.getIsGroupingProperty());
			}
		}
		return null;
	}

	private void addExternalCodes(RDFSNamedClass cls) {
		addExtRefTermAnnotations(cls, targetCM.getExternalReferenceProperty(), ICTMUtil.getCodesProp(sourceOnt));
	}

	@SuppressWarnings("deprecation")
	private void addExclusions(RDFSNamedClass cls) {
		if (isICTM == true) {
			addStringTermAnnotations(cls, targetCM.getBaseExclusionProperty(), sourceCM.getExclusionProperty());
		} else {
			//addReferenceAnnotations(cls, targetCM.getBaseExclusionProperty(), sourceCM.getBaseExclusionProperty());
		}
	}

	/**
	 * Putting together here the indexBaseInclusions and the subclassBaseInclusions, for 
	 * consistency purpose. The value of the annotation will be the text,
	 * either the actual text for the indexBaseInclusions, or the title for the
	 * subclassBaseInclusions. For the latter, we will also add an annotation on the
	 * annotation with the foundationReference as a link to the actual subclass, as
	 * is in the API.
	 * 
	 * The two types of inclusions can be easily split.
	 * 
	 * @param cls
	 */
	private void addInclusions(RDFSNamedClass cls) {
		if (isICTM == true) {
			addSimpleInclusions(cls);
		} else {
			addIndexBaseInclusions(cls);
			addBaseIndexTerm(cls); //TT - this is needed for the current ICD CM; not necessary in the merged CM
			addSubclassBaseInclusions(cls);
		}
	}


	@SuppressWarnings("deprecation")
	private void addSimpleInclusions(RDFSNamedClass cls) {
		addStringTermAnnotations(cls, targetCM.getIndexBaseInclusionProperty(), sourceCM.getInclusionProperty());
		 //TT - this is needed for the current ICD CM; not necessary in the merged CM
		addStringTermAnnotations(cls, targetCM.getBaseIndexProperty(), sourceCM.getInclusionProperty());
	}

	private void addSubclassBaseInclusions(RDFSNamedClass cls) {
		//addReferenceAnnotations(cls, targetCM.getSubclassBaseInclusionProperty(), sourceCM.getSubclassBaseInclusionProperty());
	}

	private void addIndexBaseInclusions(RDFSNamedClass cls) {
		addStringTermAnnotations(cls, targetCM.getIndexBaseInclusionProperty(), sourceCM.getIndexBaseInclusionProperty());
	}

	private void addBaseIndexTerm(RDFSNamedClass cls) {
		addStringTermAnnotations(cls, targetCM.getBaseIndexProperty(), sourceCM.getIndexBaseInclusionProperty());
	}
	
	private void addNarrower(RDFSNamedClass cls) {
		addStringTermAnnotations(cls, targetCM.getNarrowerProperty(), sourceCM.getNarrowerProperty());
	}

	private void addSyns(RDFSNamedClass cls) {
		addStringTermAnnotations(cls, targetCM.getSynonymProperty(), sourceCM.getSynonymProperty());
	}

	private void addPublicId(RDFSNamedClass cls) {
		//addStringAnnotation(cls, targetCM.getPublicIdProperty(), sourceCM.getPublicIdProperty());
		//TT: per requirements, replace "ictm" with "icd" in the public id
		String val = (String) sourceCls.getPropertyValue(sourceCM.getPublicIdProperty());
		if (val == null) {
			return;
		}
		val = val.replace("ictm", "icd");
		
		cls.setPropertyValue(targetCM.getPublicIdProperty(), val);
	}

	private void addNote(RDFSNamedClass cls) {
		addStringTermAnnotation(cls, targetCM.getNoteProperty(), sourceCM.getNoteProperty());
	}

	private void addCodingHint(RDFSNamedClass cls) {
		addStringTermAnnotation(cls, targetCM.getCodingHintProperty(), sourceCM.getCodingHintProperty());
	}

	private void addFullySpecifiedTitle(RDFSNamedClass cls) {
		addStringTermAnnotation(cls, targetCM.getFullySpecifiedNameProperty(), sourceCM.getFullySpecifiedNameProperty());
	}

	private void addTitle(RDFSNamedClass cls) {
		addStringTermAnnotations(cls, targetCM.getIcdTitleProperty(), sourceCM.getIcdTitleProperty());
	}
	
	private void addDefinition(RDFSNamedClass cls) {
		addStringTermAnnotations(cls, targetCM.getDefinitionProperty(), sourceCM.getDefinitionProperty());
	}

	private void addLongDefinition(RDFSNamedClass cls) {
		addStringTermAnnotation(cls, targetCM.getLongDefinitionProperty(), sourceCM.getLongDefinitionProperty());
	}
	
	
	private void addLogicalDefinition(OWLClass cls) {
		//logDefCreator.createLogicalAxioms(sourceCls, cls);
	}

	
	/******************* Generic methods ********************/
	
	private void addStringAnnotation(RDFSNamedClass cls, RDFProperty targetProp, RDFProperty sourceProp) {
		String val = (String) sourceCls.getPropertyValue(sourceProp);
		if (val == null) {
			return;
		}
		
		cls.setPropertyValue(targetProp, val);
	}
	
	private void addStringTermAnnotations(RDFSNamedClass cls, RDFProperty targetProp, RDFProperty sourceProp) {
		Collection<RDFResource> terms = sourceCM.getTerms(sourceCls, sourceProp);
		for (RDFResource term : terms) {
			addStringTermAnnotationFromTerm(cls, targetProp, sourceProp, term);
		}
	}
	
	private void addStringTermAnnotation(RDFSNamedClass cls, RDFProperty targetProp, RDFProperty sourceProp) {
		RDFResource termInst = sourceCM.getTerm(sourceCls, sourceProp);
		if (termInst == null) {
			return;
		}
		
		if (isAppropriateTerm (termInst) == true) {
			addStringTermAnnotationFromTerm(cls, targetProp, sourceProp, termInst);
		}
	}
	

	private void addStringTermAnnotationFromTerm(RDFSNamedClass cls, RDFProperty targetProp, RDFProperty sourceProp, RDFResource termInst) {
		String label = (String) termInst.getPropertyValue(sourceCM.getLabelProperty());
		if (label == null) {
			return;
		}
		String lang = (String) termInst.getPropertyValue(sourceCM.getLangProperty());
		
		if (isAppropriateTerm (termInst) == true) {
			String icdTypeName = ICTMUtil.getICDCorrespondentType(termInst.getRDFType().getName());
			
			RDFResource targetTerm = createTerm(targetOnt.getRDFSNamedClass(icdTypeName), termInst.getName());
			fillTerm(targetTerm, label, lang);
			cls.addPropertyValue(targetProp, targetTerm);
		}
	}
	
	private void addExtRefTermAnnotations(RDFSNamedClass cls, RDFProperty targetProp, RDFProperty sourceProp) {
		Collection<RDFResource> terms = sourceCM.getTerms(sourceCls, sourceProp);
		OWLNamedClass extRefTermCls = targetOnt.getOWLNamedClass(ICTMUtil.EXT_REF_TERM_CLASS);
		
		for (RDFResource term : terms) {
			//RDFResource targetTerm = extRefTermCls.createOWLIndividual(term.getName());
			RDFResource targetTerm = createTerm(extRefTermCls, term.getName());
			targetTerm.setPropertyValue(targetCM.getOntologyIdProperty(), term.getPropertyValue(sourceCM.getOntologyIdProperty()));
			targetTerm.setPropertyValue(targetCM.getTermIdProperty(), term.getPropertyValue(sourceCM.getTermIdProperty()));
			targetTerm.setPropertyValue(targetCM.getLabelProperty(), term.getPropertyValue(sourceCM.getLabelProperty()));
			targetTerm.setPropertyValue(targetCM.getLangProperty(), term.getPropertyValue(sourceCM.getLangProperty()));
			cls.addPropertyValue(targetProp, targetTerm);
		}
	}
		
	private RDFResource createTerm(RDFSNamedClass type, String name) {
		RDFResource term = targetOnt.getRDFResource(name);
		if (term != null) {
			Log.getLogger().warning("Term already exists: " + term);
			return term;
		}
		term = type.createRDFIndividual(name);
		return term;
	}
	
	private void fillTerm(RDFResource term, String label, String lang) {
		lang = lang == null || lang.length() == 0 ? "en" : lang;
		term.setPropertyValue(targetCM.getLabelProperty(), label);
		term.setPropertyValue(targetCM.getLangProperty(), lang);
	}
	
	
	/**
	 * Only terms with language "en" should be exported.
	 * The rest of the translations will come from the translation tool.
	 * 
	 * @param termInst
	 * @return
	 */
	private boolean isAppropriateTerm(RDFResource termInst) {
		RDFProperty langProp = sourceCM.getLangProperty();
		
		String lang = (String) termInst.getPropertyValue(langProp);
		
		return lang == null || lang.length() == 0 || "en".equals(lang);
	}
	
}

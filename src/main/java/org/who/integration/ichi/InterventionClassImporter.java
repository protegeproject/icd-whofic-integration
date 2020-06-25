package org.who.integration.ichi;

import org.apache.log4j.Logger;

import edu.stanford.smi.protegex.owl.model.OWLIntersectionClass;
import edu.stanford.smi.protegex.owl.model.OWLModel;
import edu.stanford.smi.protegex.owl.model.OWLSomeValuesFrom;
import edu.stanford.smi.protegex.owl.model.RDFSNamedClass;

public class InterventionClassImporter extends ICHIClassImporter {
	
	private static transient Logger log = Logger.getLogger(InterventionClassImporter.class);

	private RDFSNamedClass topCls;
	
	public InterventionClassImporter(OWLModel owlModel, RDFSNamedClass cls, RDFSNamedClass topCls) {
		super(owlModel, cls);
		this.topCls = topCls;
	}
	
	public void importInterventionsCls(RDFSNamedClass superCls, String ichiCode, String title, String definition,
			String indexTerms, String inclNotes, String codeAlso, String exclusion) {
		
		importICDCode(ichiCode);
		importTitle(title);
		importDefinition(definition);
		importIndexTerms(indexTerms);
		importExclusion(exclusion);
		importCodeAlso(codeAlso);
		importInclusionNotes(inclNotes);
		
		importPublicId(); //TODO - need more info
		
		importICHILin();
		
		addSuperCls(superCls);
		importLogicalDef(ichiCode);
	}

	
	private void importLogicalDef(String ichiCode) {
		//TODO: maybe we should take the top level Intervention class
		//RDFSNamedClass supercls = (RDFSNamedClass) getCls().getSuperclasses(false).iterator().next();
		getCls().setPropertyValue(getCm().getPrecoordinationSuperclassProperty(), topCls);
		
		OWLModel owlModel = getOwlModel();
		
		OWLIntersectionClass inters = owlModel.createOWLIntersectionClass();
		inters.addOperand(topCls);
		
		String[] codes = ichiCode.split("\\.");
		if (codes.length != 3) {
			log.warn("Invalid intervention code: " + ichiCode);
			return;
		}
		
		RDFSNamedClass targetCls = ICHIUtil.getTarget(codes[0]);
		if (targetCls == null) {
			log.warn("LOG DEF\t" + ichiCode + "\tCan't find target class\t" + codes[0]);
		} else {
			OWLSomeValuesFrom targetOp = owlModel.createOWLSomeValuesFrom();
			targetOp.setOnProperty(ICHIUtil.getHasTargetProperty(owlModel));
			targetOp.setSomeValuesFrom(targetCls);
			//getCls().addSuperclass(targetOp);
			inters.addOperand(targetOp);
		}
		
		RDFSNamedClass actionCls = ICHIUtil.getAction(codes[1]);
		if (actionCls == null) {
			log.warn("LOG DEF\t" + ichiCode + "\tCan't find action class\t" + codes[1]);
		} else {
			OWLSomeValuesFrom actionOp = owlModel.createOWLSomeValuesFrom();
			actionOp.setOnProperty(ICHIUtil.getHasActionProperty(owlModel));
			actionOp.setSomeValuesFrom(actionCls);
			//getCls().addSuperclass(actionOp);
			inters.addOperand(actionOp);
		}
		
		RDFSNamedClass meansCls = ICHIUtil.getMeans(codes[2]);
		if (meansCls == null) {
			log.warn("LOG DEF\t" + ichiCode + "\tCan't find means class\t" + codes[2]);
		} else {
			OWLSomeValuesFrom meansOp = owlModel.createOWLSomeValuesFrom();
			meansOp.setOnProperty(ICHIUtil.getHasMeansProperty(owlModel));
			meansOp.setSomeValuesFrom(meansCls);
			//getCls().addSuperclass(meansOp);
			inters.addOperand(meansOp);
		}
		
		getCls().addSuperclass(inters);
		
	}

}

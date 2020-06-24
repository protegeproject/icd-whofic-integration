package org.who.integration.ichi;

import edu.stanford.smi.protegex.owl.model.OWLModel;
import edu.stanford.smi.protegex.owl.model.RDFSNamedClass;

public class InterventionClassImporter extends ICHIClassImporter {

	public InterventionClassImporter(OWLModel owlModel, RDFSNamedClass cls) {
		super(owlModel, cls);
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
		
		addSuperCls(superCls);// TODO Auto-generated method stub
	}

}

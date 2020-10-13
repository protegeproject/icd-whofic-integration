package org.who.integration.icf;

import java.util.ArrayList;
import java.util.Collection;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.who.integration.util.KBUtil;

import edu.stanford.bmir.whofic.icd.ICDContentModel;
import edu.stanford.smi.protege.model.Project;
import edu.stanford.smi.protegex.owl.model.OWLModel;
import edu.stanford.smi.protegex.owl.model.RDFResource;
import edu.stanford.smi.protegex.owl.model.RDFSNamedClass;

public class RemoveGFDLinParents {
private static transient Logger log = Logger.getLogger(RemoveGFDLinParents.class);
	
	private static String GFD_TOP_CLS = "http://who.int/icd#3756_837c4a6c_ab18_4db4_9125_c5358501b8f8";

	private static OWLModel owlModel;
	private static ICDContentModel cm;
	
	private static RDFSNamedClass gfdTopCls;
	private static RDFResource mmsLinView;
	
	public static void main(String[] args) {
		
		if (args.length < 1) {
			log.error("Needs 1 argument: Path to pprj file");
		}
		
		String log4jConfPath="log4j.properties";
	    PropertyConfigurator.configure(log4jConfPath);
		
		Project prj = Project.loadProjectFromFile(args[0], new ArrayList());
		owlModel = (OWLModel) prj.getKnowledgeBase();
		cm = new ICDContentModel(owlModel);
		
		gfdTopCls = owlModel.getRDFSNamedClass(GFD_TOP_CLS);
		mmsLinView = owlModel.getRDFResource("http://who.int/icd#Morbidity");
		
		removeLinParents();
		
		log.info("Saving..");
		//prj.save(new ArrayList<>());
	}

	private static void removeLinParents() {
		Collection<RDFSNamedClass> gfdClses = KBUtil.getNamedSubclasses(gfdTopCls, true);
		
		for (RDFSNamedClass gfdCls : gfdClses) {
			removeLinParent(gfdCls);
		}
	}

	private static void removeLinParent(RDFSNamedClass gfdCls) {
		RDFResource linSpec = cm.getLinearizationSpecificationForView(gfdCls, mmsLinView);
		if (linSpec == null) {
			log.warn("Could not find MMS lin spec for: " + gfdCls.getBrowserText());
			return;
		}
		
		RDFSNamedClass linParent = (RDFSNamedClass) linSpec.getPropertyValue(cm.getLinearizationParentProperty());
		
		if (linParent == null) {
			return;
		}
		
		if (linParent.equals(gfdTopCls) == false && linParent.hasSuperclass(gfdTopCls) == false) {
			log.info("Removing linearization parent: " + linParent.getBrowserText() + " from " + gfdCls.getBrowserText());
			linSpec.setPropertyValue(cm.getLinearizationParentProperty(), null);
		}
		
	}


	
}

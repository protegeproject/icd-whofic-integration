package org.who.integration.ichi;

import java.util.ArrayList;
import java.util.Collection;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.who.integration.util.KBUtil;

import edu.stanford.bmir.whofic.icd.ICDContentModel;
import edu.stanford.smi.protege.model.Project;
import edu.stanford.smi.protege.util.Log;
import edu.stanford.smi.protegex.owl.model.OWLModel;
import edu.stanford.smi.protegex.owl.model.RDFProperty;
import edu.stanford.smi.protegex.owl.model.RDFSNamedClass;

public class ReplaceICFMapForTarget {
private static transient Logger log = Logger.getLogger(ReplaceICFMapForTarget.class);

	private static String TARGET_CLS = "http://who.int/icd#Target";
	
	private static OWLModel owlModel;
	private static ICDContentModel cm;
	
	private static RDFProperty icfMapProp;
	private static RDFSNamedClass targetCls;
	
	public static void main(String[] args) {
		
		if (args.length < 1) {
			log.error("Needs 1 argument: Path to pprj file");
		}
		
		String log4jConfPath="log4j.properties";
	    PropertyConfigurator.configure(log4jConfPath);
		
		Project prj = Project.loadProjectFromFile(args[0], new ArrayList());
		owlModel = (OWLModel) prj.getKnowledgeBase();
		cm = new ICDContentModel(owlModel);
		
		icfMapProp = ICHIUtil.getIcfMapProperty(owlModel);
		targetCls = owlModel.getRDFSNamedClass(TARGET_CLS);
		
		fixIcfMap();
		
		log.info("Saving..");
		//prj.save(new ArrayList<>());
	}

	private static void fixIcfMap() {
		Collection<RDFSNamedClass> targetClses = KBUtil.getNamedSubclasses(targetCls, true);

		for (RDFSNamedClass targetCls : targetClses) {
			fixCls(targetCls);
		}
	}

	private static void fixCls(RDFSNamedClass targetCls) {
		String icfMap = (String) targetCls.getPropertyValue(icfMapProp);
		
		if (icfMap == null) {
			return;
		}
		
		RDFSNamedClass icfMapCls = ICHIUtil.getIcfClass(owlModel, icfMap);
		
		if (icfMapCls == null) {
			Log.getLogger().warning("Could not find ICF class: " + icfMap + " mapped from cls: " + targetCls.getBrowserText());
			return;
		}
		
		cm.createICFReference(targetCls, icfMapCls);
		targetCls.removePropertyValue(icfMapProp, icfMap);
	}

	
}

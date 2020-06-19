package org.who.integration.icf;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.apache.log4j.Logger;
import org.who.integration.util.KBUtil;

import edu.stanford.bmir.whofic.icd.ICDContentModel;
import edu.stanford.smi.protege.model.Project;
import edu.stanford.smi.protegex.owl.model.OWLModel;
import edu.stanford.smi.protegex.owl.model.RDFIndividual;
import edu.stanford.smi.protegex.owl.model.RDFResource;
import edu.stanford.smi.protegex.owl.model.RDFSNamedClass;

public class SetICFGrouping {
	
	private static transient Logger log = Logger.getLogger(SetICFGrouping.class);
	
	private static String ICF_CAT = "http://who.int/icf#ICFCategory";
	private static String ICF_LIN_VIEW = "http://who.int/icd#ICFLinearizationView";
	
	private static OWLModel owlModel;
	private static ICDContentModel cm;
	private static RDFSNamedClass icfCat;
	private static RDFIndividual icfLinView;

	public static void main(String[] args) {
				
		if (args.length < 1) {
			log.error("Needs 1 argument: Path to pprj file");
		}
		
		Project prj = Project.loadProjectFromFile(args[0], new ArrayList());
		owlModel = (OWLModel) prj.getKnowledgeBase();
		cm = new ICDContentModel(owlModel);
		
		icfCat = owlModel.getRDFSNamedClass(ICF_CAT);
		icfLinView = owlModel.getRDFIndividual(ICF_LIN_VIEW);			
		
		HashSet<RDFSNamedClass> rangeAnc = new HashSet<RDFSNamedClass>();
		findGroupingClses(icfCat, icfCat, rangeAnc);
		
		addFirstTwoLevels(rangeAnc);
		
		log.info("Found " + rangeAnc.size() + " classes that should be groupings. Adding groupings ..");

		System.out.println(rangeAnc);
		
		setGrouping(rangeAnc);
		
		log.info("Saving..");
		//prj.save(new ArrayList<>());
		
	}


	private static void addFirstTwoLevels(HashSet<RDFSNamedClass> rangeAnc) {
		for (RDFSNamedClass subcls : KBUtil.getNamedSubclasses(icfCat, false)) {
			if (subcls.getName().contains("http://who.int/icf")) {
				rangeAnc.add(subcls);
				for (RDFSNamedClass subsubcls : KBUtil.getNamedSubclasses(subcls, false)) {
					rangeAnc.add(subsubcls);
				}
			}
		}
	}


	private static void findGroupingClses(RDFSNamedClass cls, RDFSNamedClass parent, Set<RDFSNamedClass> superClses) {
		
		Collection<RDFSNamedClass> subclses = KBUtil.getNamedSubclasses(cls);
		for (RDFSNamedClass subcls : subclses) {
			if (isRange(subcls)) {
				superClses.add(cls);
				superClses.add(parent);
				superClses.add(subcls);
			} else {
				findGroupingClses(subcls, cls, superClses);
			}
		}
	}

	private static boolean isRange(RDFSNamedClass subcls) {
		if (subcls.getName().contains("-")) {
			return true;
		}
		return false;
	}
	
	private static void setGrouping(HashSet<RDFSNamedClass> rangeAnc) {
		for (RDFSNamedClass cls : rangeAnc) {
			RDFResource linSpec = cm.getLinearizationSpecificationForView(cls, icfLinView);
			if (linSpec == null) {
				log.warn("Could not find ICF linearization specification for " + cls);
			} else {
				linSpec.setPropertyValue(cm.getIsGroupingProperty(), true);
			}
		}
		
	}
	
	
}

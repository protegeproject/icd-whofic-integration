package org.who.integration.ichi;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import edu.stanford.bmir.whofic.IcdIdGenerator;
import edu.stanford.bmir.whofic.icd.ICDContentModel;
import edu.stanford.smi.protegex.owl.model.OWLModel;
import edu.stanford.smi.protegex.owl.model.RDFResource;
import edu.stanford.smi.protegex.owl.model.RDFSNamedClass;

public class ICHIUtil {
	
	public static final String DEFAULT_NS = "http://who.int/icd#";
	
	private static Collection<RDFSNamedClass> metaclses = new ArrayList<RDFSNamedClass>();
	private static Map<String, RDFSNamedClass> name2supercls = new HashMap<String, RDFSNamedClass>();
	
	private static Map<RDFSNamedClass, List<String>> cls2exclusions = new HashMap<RDFSNamedClass, List<String>>();
	
	
	public static Collection<RDFSNamedClass> getMetaclasses(OWLModel owlModel) {
		if (metaclses.size() > 0) {
			return metaclses;
		}
		metaclses = new ArrayList<RDFSNamedClass>();
		metaclses.add(owlModel.getRDFSNamedClass("http://who.int/icd#DefinitionSection"));
		metaclses.add(owlModel.getRDFSNamedClass("http://who.int/icd#TermSection"));
		metaclses.add(owlModel.getRDFSNamedClass("http://who.int/icd#LinearizationSection"));
		metaclses.add(owlModel.getRDFSNamedClass("http://who.int/icd#ValueMetaClass"));
		metaclses.add(owlModel.getRDFSNamedClass("http://who.int/icd#ICHIAxesSection"));
		return metaclses;
	}
	
	public static RDFSNamedClass getOrCreateCls(OWLModel owlModel, String name) {
		name = name == null ? IcdIdGenerator.getNextUniqueId(owlModel) : DEFAULT_NS + name;
				
		RDFSNamedClass cls = owlModel.getRDFSNamedClass(name);
		if (cls != null) {
			return cls;
		}
		cls = owlModel.createOWLNamedClass(name);
		
		for (RDFSNamedClass metacls : getMetaclasses(owlModel)) {
			cls.addRDFType(metacls);
		}
		
		return cls;
	}
	
	
	public static void addExclusion(RDFSNamedClass cls, String excl) {
		List<String> exclusions = cls2exclusions.get(cls);
		if (exclusions == null) {
			exclusions = new ArrayList<String>();
		}
		exclusions.add(excl);
		cls2exclusions.put(cls, exclusions);
	}
	
	public static RDFSNamedClass getSupercls(OWLModel owlModel, RDFSNamedClass topCls, String name) {
		name = name.replaceAll("^\\d \\- ", "");
		name = name.replaceAll(", ", "_");
		name = name.replaceAll(",", "_");
		name = name.replaceAll(" " , "_");
		
		RDFSNamedClass supercls = name2supercls.get(name);
		
		if (supercls == null) {
			supercls = getOrCreateCls(owlModel, name);
			supercls.addSuperclass(topCls);
			supercls.removeSuperclass(owlModel.getOWLThingClass());
			
			ICDContentModel cm = new ICDContentModel(owlModel);
			RDFResource titleTerm = cm.createTitleTerm();
			titleTerm.setPropertyValue(cm.getLabelProperty(), name);
			supercls.setPropertyValue(cm.getIcdTitleProperty(), titleTerm);
			
			name2supercls.put(name, supercls);
		}
		
		return supercls;
	}
	
}

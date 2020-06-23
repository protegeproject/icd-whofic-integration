package org.who.integration.ichi;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;

import edu.stanford.bmir.whofic.IcdIdGenerator;
import edu.stanford.bmir.whofic.icd.ICDContentModel;
import edu.stanford.smi.protegex.owl.model.OWLModel;
import edu.stanford.smi.protegex.owl.model.RDFResource;
import edu.stanford.smi.protegex.owl.model.RDFSNamedClass;

public class ICHIUtil {
	private static transient Logger log = Logger.getLogger(ICHIUtil.class);
			
	public static final String DEFAULT_NS = "http://who.int/icd#";
	
	//Atm stands for: Action-Target-Means
	
	private static Collection<RDFSNamedClass> atmMetaclses = new ArrayList<RDFSNamedClass>();
	private static Map<String, RDFSNamedClass> code2cls = new HashMap<String, RDFSNamedClass>();
	
	private static Map<RDFSNamedClass, List<String>> cls2exclusions = new HashMap<RDFSNamedClass, List<String>>();
	
	
	public static Collection<RDFSNamedClass> getAtmMetaclasses(OWLModel owlModel) {
		if (atmMetaclses.size() > 0) {
			return atmMetaclses;
		}
		atmMetaclses = new ArrayList<RDFSNamedClass>();
		atmMetaclses.add(owlModel.getRDFSNamedClass("http://who.int/icd#DefinitionSection"));
		atmMetaclses.add(owlModel.getRDFSNamedClass("http://who.int/icd#TermSection"));
		atmMetaclses.add(owlModel.getRDFSNamedClass("http://who.int/icd#LinearizationSection"));
		atmMetaclses.add(owlModel.getRDFSNamedClass("http://who.int/icd#ValueMetaClass"));
		atmMetaclses.add(owlModel.getRDFSNamedClass("http://who.int/icd#ICHIAxesSection"));
		return atmMetaclses;
	}
	
	public static RDFSNamedClass createAtmCls(OWLModel owlModel, String code) {
		RDFSNamedClass cls = createCls(owlModel, code);
		addMetaclasses(cls, getAtmMetaclasses(owlModel));
		return cls;
	}
	
	public static RDFSNamedClass createCls(OWLModel owlModel, String code) {
		RDFSNamedClass cls = code2cls.get(code);
		
		if (cls != null) {
			log.warn("Class with code: " + code + " already exists, id: " + cls.getName());
			return cls;
		}
		
		cls = owlModel.createOWLNamedClass(IcdIdGenerator.getNextUniqueId(owlModel));
		code2cls.put(code, cls);
		
		return cls;
	}
	
	public static void addMetaclasses(RDFSNamedClass cls, Collection<RDFSNamedClass> metaclses) {
		for (RDFSNamedClass metacls : metaclses) {
			if (cls.hasRDFType(metacls) == false) {
				cls.addRDFType(metacls);
			}
		}
	}
	
	public static RDFSNamedClass getCls(String code) {
		return code2cls.get(code);
	}
	
	public static void addExclusion(RDFSNamedClass cls, String excl) {
		List<String> exclusions = cls2exclusions.get(cls);
		if (exclusions == null) {
			exclusions = new ArrayList<String>();
		}
		exclusions.add(excl);
		cls2exclusions.put(cls, exclusions);
	}
	
	public static List<String> getExclusions(RDFSNamedClass cls) {
		return cls2exclusions.get(cls);
	}
	
	public static Set<RDFSNamedClass> getClsesWithExclusions() {
		return cls2exclusions.keySet();
	}
	
	public static RDFSNamedClass getAtmSupercls(ICDContentModel cm, RDFSNamedClass topCls, String title) {
		title = title.replaceAll("^\\d \\- ", "");
		
		RDFSNamedClass supercls = code2cls.get(title);
		
		if (supercls == null) {
			supercls = createAtmCls(cm.getOwlModel(), title);
			supercls.addSuperclass(topCls);
			supercls.removeSuperclass(cm.getOwlModel().getOWLThingClass());
			
			RDFResource titleTerm = cm.createTitleTerm();
			titleTerm.setPropertyValue(cm.getLabelProperty(), title);
			supercls.setPropertyValue(cm.getIcdTitleProperty(), titleTerm);
			
			cm.addChildToIndex(topCls, supercls, true);
			
			code2cls.put(title, supercls);
		}
		
		return supercls;
	}
	
}

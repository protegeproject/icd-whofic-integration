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
import edu.stanford.smi.protegex.owl.model.RDFProperty;
import edu.stanford.smi.protegex.owl.model.RDFResource;
import edu.stanford.smi.protegex.owl.model.RDFSNamedClass;

public class ICHIUtil {
	private static transient Logger log = Logger.getLogger(ICHIUtil.class);
			
	public static final String DEFAULT_NS = "http://who.int/icd#";
	public static final String ICF_NS = "http://who.int/icf#";
	
	public static final String ICF_MAP_PROP = "http://who.int/icd#icfMap";
	
	public static final String HAS_ACTION_PROP = "http://who.int/icd#hasAction";
	public static final String HAS_MEANS_PROP = "http://who.int/icd#hasMeans";
	public static final String HAS_TARGET_PROP = "http://who.int/icd#hasTarget";
	
	public static final String ICHI_LIN_VIEW = "http://who.int/icd#ICHILinearizationView";
	public static final String LIN_VIEW = "http://who.int/icd#LinearizationView";
	
	//Atm stands for: Action-Target-Means
	
	private static Collection<RDFSNamedClass> metaclasses = new ArrayList<RDFSNamedClass>();
	private static Map<String, RDFSNamedClass> code2cls = new HashMap<String, RDFSNamedClass>();
	
	private static Map<RDFSNamedClass, List<String>> cls2exclusions = new HashMap<RDFSNamedClass, List<String>>();
	
	private static Map<String, RDFSNamedClass> target2cls = new HashMap<String, RDFSNamedClass>();
	private static Map<String, RDFSNamedClass> means2cls = new HashMap<String, RDFSNamedClass>();
	private static Map<String, RDFSNamedClass> action2cls = new HashMap<String, RDFSNamedClass>();
	
	private static RDFProperty icfMapProp;
	
	private static RDFProperty hasActionProp;
	private static RDFProperty hasMeansProp;
	private static RDFProperty hasTargetProp;
	
	private static RDFResource ichiLinView;
	
	public static Collection<RDFSNamedClass> getAtmMetaclasses(OWLModel owlModel) {
		if (metaclasses.size() > 0) {
			return metaclasses;
		}
		metaclasses = new ArrayList<RDFSNamedClass>();
		metaclasses.add(owlModel.getRDFSNamedClass("http://who.int/icd#DefinitionSection"));
		metaclasses.add(owlModel.getRDFSNamedClass("http://who.int/icd#TermSection"));
		metaclasses.add(owlModel.getRDFSNamedClass("http://who.int/icd#LinearizationSection"));
		metaclasses.add(owlModel.getRDFSNamedClass("http://who.int/icd#ValueMetaClass"));
		metaclasses.add(owlModel.getRDFSNamedClass("http://who.int/icd#ICHIAxesSection"));
		return metaclasses;
	}
	
	//the metaclass methods are mutually exclusive
	public static Collection<RDFSNamedClass> getInterventionMetaclasses(OWLModel owlModel) {
		if (metaclasses.size() > 0) {
			return metaclasses;
		}
		metaclasses = new ArrayList<RDFSNamedClass>();
		metaclasses.add(owlModel.getRDFSNamedClass("http://who.int/icd#DefinitionSection"));
		metaclasses.add(owlModel.getRDFSNamedClass("http://who.int/icd#TermSection"));
		metaclasses.add(owlModel.getRDFSNamedClass("http://who.int/icd#LinearizationSection"));
		metaclasses.add(owlModel.getRDFSNamedClass("http://who.int/icd#ICHIPostcoordinationSection"));
		metaclasses.add(owlModel.getRDFSNamedClass("http://who.int/icd#ICHISection"));
		return metaclasses;
	}
	
	public static RDFSNamedClass createAtmCls(OWLModel owlModel, String code) {
		RDFSNamedClass cls = createCls(owlModel, code);
		addMetaclasses(cls, getAtmMetaclasses(owlModel));
		return cls;
	}
	
	public static RDFSNamedClass createInterventionCls(OWLModel owlModel, String code) {
		RDFSNamedClass cls = createCls(owlModel, code);
		addMetaclasses(cls, getInterventionMetaclasses(owlModel));
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
	
	public static RDFSNamedClass getSupercls(ICDContentModel cm, RDFSNamedClass topCls, String title) {
		int index = title.indexOf("-");
		if (index > 0) {
			title = title.substring(index + 1);
			title = title.trim();
		}
		//title = title.replaceAll("^\\d \\- ", "");
		
		RDFSNamedClass supercls = code2cls.get(title);
		
		if (supercls == null) {
			supercls = createAtmCls(cm.getOwlModel(), title);
			supercls.addSuperclass(topCls);
			supercls.removeSuperclass(cm.getOwlModel().getOWLThingClass());
			
			RDFResource titleTerm = cm.createTitleTerm();
			titleTerm.setPropertyValue(cm.getLabelProperty(), title);
			supercls.setPropertyValue(cm.getIcdTitleProperty(), titleTerm);
			
			addICHILin(cm, supercls, true, true);
			
			cm.addChildToIndex(topCls, supercls, true);
			
			code2cls.put(title, supercls);
		}
		
		return supercls;
	}
	
	public static RDFProperty getIcfMapProperty(OWLModel owlModel) {
		if (icfMapProp == null) {
			icfMapProp = owlModel.getRDFProperty(ICF_MAP_PROP);
		}
		
		return icfMapProp;
	}
	
	public static RDFProperty getHasTargetProperty(OWLModel owlModel) {
		if (hasTargetProp == null) {
			hasTargetProp = owlModel.getRDFProperty(HAS_TARGET_PROP);
		}
		
		return hasTargetProp;
	}
	
	public static RDFProperty getHasActionProperty(OWLModel owlModel) {
		if (hasActionProp == null) {
			hasActionProp = owlModel.getRDFProperty(HAS_ACTION_PROP);
		}
		
		return hasActionProp;
	}
	
	public static RDFProperty getHasMeansProperty(OWLModel owlModel) {
		if (hasMeansProp == null) {
			hasMeansProp = owlModel.getRDFProperty(HAS_MEANS_PROP);
		}
		
		return hasMeansProp;
	}
	
	public static RDFSNamedClass getIcfClass(OWLModel owlModel, String icfCode) {
		return owlModel.getRDFSNamedClass(ICF_NS + icfCode);
	}
	
	public static RDFResource getICHILinearizationView(OWLModel owlModel) {
		if (ichiLinView == null) {
			ichiLinView = owlModel.getRDFResource(ICHI_LIN_VIEW);
		}
		return ichiLinView;
	}
	
	public static void addICHILin(ICDContentModel cm, RDFSNamedClass cls, boolean included, boolean grouping) {
		RDFResource linSpec = cm.getLinearizationSpecificationClass().createInstance((IcdIdGenerator.getNextUniqueId(cm.getOwlModel())));
        linSpec.setPropertyValue(cm.getLinearizationViewProperty(), ICHIUtil.getICHILinearizationView(cm.getOwlModel()));
        linSpec.setPropertyValue(cm.getIsIncludedInLinearizationProperty(), included);
        linSpec.setPropertyValue(cm.getIsGroupingProperty(), grouping);
        cls.addPropertyValue(cm.getLinearizationProperty(), linSpec);
	}
	
	public static void initATMCodes2ClsesMaps(ICDContentModel cm) {
		OWLModel owlModel = cm.getOwlModel();
		
		initCode2ClsesMap(cm, target2cls, owlModel.getRDFSNamedClass("http://who.int/icd#Target"));
		initCode2ClsesMap(cm, action2cls, owlModel.getRDFSNamedClass("http://who.int/icd#Action"));
		initCode2ClsesMap(cm, means2cls, owlModel.getRDFSNamedClass("http://who.int/icd#Means"));
	}
	
	private static void initCode2ClsesMap(ICDContentModel cm, Map<String, RDFSNamedClass> map, RDFSNamedClass topCls) {
		Collection<RDFSNamedClass> subclses = topCls.getSubclasses(true);
		for (RDFSNamedClass subcls : subclses) {
			String code = (String) subcls.getPropertyValue(cm.getIcdCodeProperty());
			if (code != null) {
				map.put(code, subcls);
			}
		}
	}
	
	public static RDFSNamedClass getTarget(String code) {
		return target2cls.get(code);
	}
	
	public static RDFSNamedClass getMeans(String code) {
		return means2cls.get(code);
	}
	
	public static RDFSNamedClass getAction(String code) {
		return action2cls.get(code);
	}
	
	
}
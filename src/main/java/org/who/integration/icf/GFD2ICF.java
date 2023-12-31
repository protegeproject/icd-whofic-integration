package org.who.integration.icf;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.who.integration.util.KBUtil;

import edu.stanford.bmir.whofic.IcdIdGenerator;
import edu.stanford.bmir.whofic.icd.ICDContentModel;
import edu.stanford.smi.protege.model.Project;
import edu.stanford.smi.protege.util.Log;
import edu.stanford.smi.protegex.owl.model.OWLModel;
import edu.stanford.smi.protegex.owl.model.RDFIndividual;
import edu.stanford.smi.protegex.owl.model.RDFProperty;
import edu.stanford.smi.protegex.owl.model.RDFResource;
import edu.stanford.smi.protegex.owl.model.RDFSClass;
import edu.stanford.smi.protegex.owl.model.RDFSNamedClass;

/**
 * Script to harmonize "General Functioning Domains" (GFD) with ICF.
 * 
 * 
 * @author ttania
 *
 */
public class GFD2ICF {
	
	//TODO: +++ migrate sibling ordering for GFD top level only
	//TODO: +++ copy public id from old class, print out mapping
	//TODO: +++ check if we need to retain the title term id for translations; swap title terms
	//TODO: +++ Copy the post-coordination spec from GFD to ICF classes.

	private static transient Logger log = Logger.getLogger(GFD2ICF.class);
	
	//General functioning domains class
	private static String GFD = "http://who.int/icd#3756_837c4a6c_ab18_4db4_9125_c5358501b8f8";
	private static String GFD_RETIRED_CLS = "http://who.int/icd#35997_735007d5_2555_4eb5_a762_282e008a1468";

	private static String ICF_CAT = "http://who.int/icf#ICFCategory";
	private static String ICF_LIN_VIEW = "http://who.int/icd#ICFLinearizationView";
	
	private static String REPLACES_PROP = "http://who.int/icd#replaces";
	private static String ICF_MAP_PROP = "http://who.int/icd#icfMap";
	
	private static OWLModel owlModel;
	private static ICDContentModel cm;
	private static RDFSNamedClass icfCat;
	private static RDFSNamedClass gfdTopCls;
	private static RDFSNamedClass gfdRetiredCls;
	private static RDFIndividual icfLinView;
	
	private static RDFProperty replacesProp;
	private static RDFProperty icfMapProp;

	private static Map<String, RDFSNamedClass> name2icfcls = new HashMap<String, RDFSNamedClass>();
	
	//used only by checkGFDSubclassing
	private static Map<RDFSNamedClass, RDFSNamedClass> icf2gfdcls = new HashMap<RDFSNamedClass, RDFSNamedClass>();
	
	private static Map<RDFSNamedClass, RDFSNamedClass> gfd2icfcls = new HashMap<RDFSNamedClass, RDFSNamedClass>();
	
	public static void main(String[] args) {
				
		if (args.length < 1) {
			log.error("Needs 1 argument: Path to pprj file");
		}
		
		String log4jConfPath="log4j.properties";
	    PropertyConfigurator.configure(log4jConfPath);
		
		Project prj = Project.loadProjectFromFile(args[0], new ArrayList());
		owlModel = (OWLModel) prj.getKnowledgeBase();
		cm = new ICDContentModel(owlModel);
		
		icfCat = owlModel.getRDFSNamedClass(ICF_CAT);
		gfdTopCls = owlModel.getRDFSNamedClass(GFD);
		gfdRetiredCls = owlModel.getRDFSNamedClass(GFD_RETIRED_CLS);
		icfLinView = owlModel.getOWLIndividual(ICF_LIN_VIEW);
		
		replacesProp = getReplacesProp();
		icfMapProp = getIcfMapProp();
		
		cacheICF();
		
		log.info("Checking GFD - ICF mapping..");
		checkGFDMapping();
		
		//checkGFDSubclassing();
		
		log.info("Repacing classes...");
		replaceClasses();
		
		log.info("Replacing GFD top class sibling ordering..");
		replaceChildrenOrdering();

		log.info("Saving..");
		//prj.save(new ArrayList<>());
	}


	private static void replaceClasses() {
		for (RDFSNamedClass gfdCls : KBUtil.getNamedSubclasses(gfdTopCls, false)) {
			RDFSNamedClass icfCls = gfd2icfcls.get(gfdCls);
			
			if (icfCls == null) { //we already know about this case
				continue; 
			}
			
			replaceTopCls(gfdCls, icfCls);
		}
	}

	private static void replaceTopCls(RDFSNamedClass gfdCls, RDFSNamedClass icfCls) {
		//add ICF class under GFD top node
		Collection dirSuperClses = icfCls.getSuperclasses(false);
		if (dirSuperClses.contains(gfdTopCls) == false) {
			icfCls.addSuperclass(gfdTopCls);
		}
		
		replaceCls(gfdCls, icfCls);
		
		//retire GFD top class
		retireTopGFDClass(gfdCls);
	}


	private static void replaceGFDChildren(RDFSNamedClass gfdParent, RDFSNamedClass icfParent) {
		for (RDFSNamedClass gfdCls : KBUtil.getNamedSubclasses(gfdParent, true)) {
			RDFSNamedClass icfCls = gfd2icfcls.get(gfdCls);
			
			if (icfCls == null) { //no mapping to ICF; move it under ICF parent, remove old GFD parent
				log.warn("Found GFD child: " + gfdCls.getBrowserText() + " with no ICF mapping, keeping GFP parent: " + 
							gfdParent.getBrowserText() + ". Would be ICF parent: " + icfParent.getBrowserText());
				Log.getLogger().warning("Found GFD child: " + gfdCls.getBrowserText() + " with no ICF mapping, keeping GFP parent: " + 
						gfdParent.getBrowserText() + ". Would be ICF parent: " + icfParent.getBrowserText());
				//do nothing - keep the old GFD parent, which should become the survey parent
				//gfdCls.addSuperclass(icfParent);
				//gfdCls.removeSuperclass(gfdParent);
			} else {
				replaceCls(gfdCls, icfCls);
			}
		}
	}


	private static void retireTopGFDClass(RDFSNamedClass gfdCls) {
		//move under retired
		gfdCls.addSuperclass(gfdRetiredCls);
		gfdCls.removeSuperclass(gfdTopCls);
		
		//set title to Retired + title
		setRetiredTitle(gfdCls);
		
		//set retired title for all subclasses
		for (RDFSNamedClass subcls : KBUtil.getNamedSubclasses(gfdCls, true)) {
			setRetiredTitle(subcls);
		}
	}
	
	private static void replaceCls(RDFSNamedClass gfdCls, RDFSNamedClass icfCls) {
		//copy linearization from GFD to ICF class
		cm.copyLinearizationSpecificationsFromCls(icfCls, gfdCls);
		
		//will do with script
		//copy post-coordination specs from GFD to ICF class
		//cm.copyPostcoordinationSpecificationsFromCls(icfCls, gfdCls);
		//createICFLinAndPCSpecs(icfCls);
		
		//replace gfd children
		replaceGFDChildren(gfdCls, icfCls);
		
		//add link bw the two classes
		icfCls.setPropertyValue(replacesProp, gfdCls); 
		gfdCls.setPropertyValue(icfMapProp, icfCls);
		
		//replace public id
		replacePublicId(gfdCls, icfCls);
	}
	

	private static void createICFLinAndPCSpecs(RDFSNamedClass icfCls) {
		//Add ICF linearization with isIncluded = true
		RDFResource icfLinSpec = cm.getLinearizationSpecificationClass().createRDFIndividual(IcdIdGenerator.getNextUniqueId(owlModel));
		icfLinSpec.setPropertyValue(cm.getLinearizationViewProperty(), icfLinView);
		icfLinSpec.setPropertyValue(cm.getIsIncludedInLinearizationProperty(), true);
		icfCls.addPropertyValue(cm.getLinearizationViewProperty(), icfLinSpec);
		
		//Add ICF PC spec
		RDFResource icfPCSpec = cm.getPostcoordinationAxesSpecificationClass().createRDFIndividual(IcdIdGenerator.getNextUniqueId(owlModel));
		icfPCSpec.setPropertyValue(cm.getLinearizationViewProperty(), icfLinSpec);
		icfCls.addPropertyValue(cm.getAllowedPostcoordinationAxesProperty(), icfPCSpec);
	}


	//preserving the title term for the mapped ICF class; create new title term for the retired GFD class
	private static void setRetiredTitle(RDFSNamedClass gfdCls) {
		String title = cm.getTitleLabel(gfdCls);
		RDFResource gfdTitleTerm = (RDFResource) gfdCls.getPropertyValue(cm.getIcdTitleProperty());
		
		RDFSNamedClass icfCls = gfd2icfcls.get(gfdCls);
		if (icfCls == null) {
			log.warn("Expected that GFD class was mapped to ICF, but wasn't: " + gfdCls + " Title: " + title);
			gfdTitleTerm.setPropertyValue(cm.getLabelProperty(), "Retired - " + title);
		} else {
			RDFResource icfTitleTerm = (RDFResource) icfCls.getPropertyValue(cm.getIcdTitleProperty());
			gfdTitleTerm.setPropertyValue(cm.getLangProperty(), icfTitleTerm.getPropertyValue(cm.getLangProperty()));
			gfdTitleTerm.setPropertyValue(cm.getIdProperty(), icfTitleTerm.getPropertyValue(cm.getIdProperty()));
			icfCls.setPropertyValue(cm.getIcdTitleProperty(), gfdTitleTerm);
		
			RDFResource newGfdTitleTerm = cm.createTitleTerm();
			newGfdTitleTerm.setPropertyValue(cm.getLabelProperty(), "Retired - " + title);
			newGfdTitleTerm.setPropertyValue(cm.getLangProperty(), icfTitleTerm.getPropertyValue(cm.getLangProperty()));
			gfdCls.setPropertyValue(cm.getIcdTitleProperty(), newGfdTitleTerm);
		}
	}
	
	private static void replacePublicId(RDFSNamedClass gfdCls, RDFSNamedClass icfCls) {
		String publicId = cm.getPublicId(gfdCls);
		
		icfCls.setPropertyValue(cm.getPublicIdProperty(), publicId);
		gfdCls.setPropertyValue(cm.getPublicIdProperty(), "WAS: " + publicId);
		
		log.info("PUBLIC ID SWAP:\t" + publicId + "\t" + icfCls.getName() + "\t" + gfdCls.getName());
		Log.getLogger().info("PUBLIC ID SWAP:\t" + publicId + "\t" + icfCls.getName() + "\t" + gfdCls.getName());
	}


	private static void checkGFDMapping() {
		for (RDFSNamedClass gfdCls : KBUtil.getNamedSubclasses(gfdTopCls, true)) {
			String gfdTitle = cm.getTitleLabel(gfdCls);
			
			RDFSNamedClass icfCls = name2icfcls.get(gfdTitle.toLowerCase());
			if (icfCls == null) {
				log.warn("Could not find ICF mapping for: " + gfdCls + " title: " + gfdTitle);
				//Collection<RDFSNamedClass> superclses = (Collection<RDFSNamedClass>) gfdCls.getSuperclasses(false);
				//System.out.println(gfdCls + "\t" + superclses + "\t" + gfdTitle + "\t" + KBUtil.getTitles(cm, superclses));
			} else {
				icf2gfdcls.put(icfCls, gfdCls);
				gfd2icfcls.put(gfdCls, icfCls);
			}
		}
	}
	

	private static RDFProperty getReplacesProp() {
		RDFProperty prop = owlModel.getRDFProperty(REPLACES_PROP);
		if (prop == null) {
			prop = owlModel.createAnnotationProperty(REPLACES_PROP);
		}
		return prop;
	}
	
	private static RDFProperty getIcfMapProp() {
		RDFProperty prop = owlModel.getRDFProperty(ICF_MAP_PROP);
		if (prop == null) {
			prop = owlModel.createAnnotationProperty(ICF_MAP_PROP);
		}
		return prop;
	}
	
	private static void cacheICF() {
		Collection<RDFSNamedClass> icfClasses = KBUtil.getNamedSubclasses(icfCat, true);
		for (RDFSNamedClass cls : icfClasses) {
			String title = cm.getTitleLabel(cls);
			title = title == null ? cls.getName() : title;
			title = title.toLowerCase();
			
			name2icfcls.put(title, cls);
		}
	}

	//This method is not called by the script, but has been used to make sure that
	//the GFD and ICF hierarchies are in sync. They are.
	private static void checkGFDSubclassing() {
		for (RDFSNamedClass gfdCls : KBUtil.getNamedSubclasses(gfdTopCls, true)) {
			String gfdTitle = cm.getTitleLabel(gfdCls);
			RDFSNamedClass icfCls = gfd2icfcls.get(gfdCls);
			if (icfCls != null) {
				Collection<RDFSClass> gfdParents = gfdCls.getSuperclasses(true);
				Collection<RDFSClass> icfParents = icfCls.getSuperclasses(true);
				boolean found = false;
				
				for (RDFSClass icfParent : icfParents) {
					RDFSNamedClass gfdParent = icf2gfdcls.get(icfParent);
					if (gfdParent != null) {
						found = true;
						break;
					}
				}
				
				if (found == false) {
					log.warn("Unmapped parents hierarchy. GFD class: " + gfdCls +
							" ICF class: " + icfCls + " Title: " + gfdTitle);
				}
			}
		}
	}

	private static void replaceChildrenOrdering() {
		Collection<RDFResource> childOrderingRes = gfdTopCls.getPropertyValues(cm.getChildrenOrderProperty());
		for (RDFResource childOrdering : childOrderingRes) {
			RDFSNamedClass orderedChild = (RDFSNamedClass) childOrdering.getPropertyValue(cm.getOrderedChildProperty());
			RDFSNamedClass icfCls = gfd2icfcls.get(orderedChild);
			if (icfCls != null) {
				childOrdering.setPropertyValue(cm.getOrderedChildProperty(), icfCls);
			}
		}
	}
	
}

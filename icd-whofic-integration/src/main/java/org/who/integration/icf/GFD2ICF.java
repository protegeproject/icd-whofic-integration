package org.who.integration.icf;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.who.integration.util.KBUtil;

import edu.stanford.bmir.whofic.icd.ICDContentModel;
import edu.stanford.smi.protege.model.Project;
import edu.stanford.smi.protegex.owl.model.OWLModel;
import edu.stanford.smi.protegex.owl.model.RDFIndividual;
import edu.stanford.smi.protegex.owl.model.RDFProperty;
import edu.stanford.smi.protegex.owl.model.RDFResource;
import edu.stanford.smi.protegex.owl.model.RDFSClass;
import edu.stanford.smi.protegex.owl.model.RDFSNamedClass;

/**
 * Script to harmonize "General Functioning Domains" with ICF
 * 
 * @author ttania
 *
 */
public class GFD2ICF {
	private static transient Logger log = Logger.getLogger(GFD2ICF.class);
	
	//General functioning domains class
	private static String GFD = "http://who.int/icd#3756_837c4a6c_ab18_4db4_9125_c5358501b8f8";
	private static String GFD_RETIRED_CLS = "http://who.int/icd#35997_735007d5_2555_4eb5_a762_282e008a1468";

	private static String ICF_CAT = "http://who.int/icf#ICFCategory";
	private static String ICF_LIN_VIEW = "http://who.int/icd#ICFLinearizationView";
	private static String REPLACES_PROP = "http://who.int/icd#replaces";
	
	private static OWLModel owlModel;
	private static ICDContentModel cm;
	private static RDFSNamedClass icfCat;
	private static RDFIndividual icfLinView;
	private static RDFSNamedClass gfdTopCls;
	private static RDFSNamedClass gfdRetiredCls;
	
	private static RDFProperty replacesProp;

	private static Map<String, RDFSNamedClass> name2icfcls = new HashMap<String, RDFSNamedClass>();
	private static Map<RDFSNamedClass, RDFSNamedClass> icf2gfdcls = new HashMap<RDFSNamedClass, RDFSNamedClass>();
	
	public static void main(String[] args) {
				
		if (args.length < 1) {
			log.error("Needs 1 argument: Path to pprj file");
		}
		
		Project prj = Project.loadProjectFromFile(args[0], new ArrayList());
		owlModel = (OWLModel) prj.getKnowledgeBase();
		cm = new ICDContentModel(owlModel);
		
		icfCat = owlModel.getRDFSNamedClass(ICF_CAT);
		icfLinView = owlModel.getRDFIndividual(ICF_LIN_VIEW);	
		gfdTopCls = owlModel.getRDFSNamedClass(GFD);
		gfdRetiredCls = owlModel.getRDFSNamedClass(GFD_RETIRED_CLS);
		
		replacesProp = getReplacesProp();
		
		cacheICF();
		
		log.info("Checking GFD - ICF mapping..");
		checkGFDMapping();
		
		//checkGFDSubclassing();
		
		log.info("Repacing classes...");
		replaceClasses();

		log.info("Saving..");
		//prj.save(new ArrayList<>());
	}

	//TODO: migrate sibling ordering for GFD top level only
	//TODO: unmapped children are not kept , check
	//TODO: copy public id from old class, print out mapping
	//TODO: check if we need to retain the title term id for translations
	//TODO: multiparent them also under the survey classes..
	//TODO: add replacedBy


	private static void replaceClasses() {
		for (RDFSNamedClass gfdCls : KBUtil.getNamedSubclasses(gfdTopCls, false)) {
			String gfdTitle = cm.getTitleLabel(gfdCls);
			
			RDFSNamedClass icfCls = name2icfcls.get(gfdTitle.toLowerCase());
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
		
		//copy linearization from GFD to ICF class
		cm.copyLinearizationSpecificationsFromCls(icfCls, gfdCls);
		
		//replace gfd children
		replaceGFDChildren(gfdCls);
		
		//add link bw the two classes
		icfCls.setPropertyValue(replacesProp, gfdCls);
		//retire GFD top class
		retireTopGFDClass(gfdCls);
	}
	

	private static void replaceGFDChildren(RDFSNamedClass gfdParent) {
		for (RDFSNamedClass gfdCls : KBUtil.getNamedSubclasses(gfdParent, true)) {
			String gfdTitle = cm.getTitleLabel(gfdCls);
			
			RDFSNamedClass icfCls = name2icfcls.get(gfdTitle.toLowerCase());
			if (icfCls == null) { //we already know about this case
				continue; 
			}
			
			cm.copyLinearizationSpecificationsFromCls(icfCls, gfdCls);
			//add link bw the two classes
			icfCls.setPropertyValue(replacesProp, gfdCls);
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
	


	private static void setRetiredTitle(RDFSNamedClass gfdCls) {
		String title = cm.getTitleLabel(gfdCls);
		
		RDFResource titleTerm = (RDFResource) gfdCls.getPropertyValue(cm.getIcdTitleProperty());
		titleTerm.setPropertyValue(cm.getLabelProperty(), "Retired - " + title);
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
			RDFSNamedClass icfCls = name2icfcls.get(gfdTitle.toLowerCase());
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
}

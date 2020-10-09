package org.who.integration.icf;

import java.util.ArrayList;
import java.util.Collection;

import org.apache.log4j.Logger;
import org.who.integration.util.KBUtil;

import edu.stanford.bmir.whofic.IcdIdGenerator;
import edu.stanford.bmir.whofic.icd.ICDContentModel;
import edu.stanford.smi.protege.model.Project;
import edu.stanford.smi.protegex.owl.model.OWLModel;
import edu.stanford.smi.protegex.owl.model.RDFResource;
import edu.stanford.smi.protegex.owl.model.RDFSNamedClass;

public class FixSurveyClasses {
private static transient Logger log = Logger.getLogger(FixSurveyClasses.class);
	
	private static String GFD_RETIRED_CLS = "http://who.int/icd#35997_735007d5_2555_4eb5_a762_282e008a1468";

	private static String WHODAS_TOP_CLS = "http://who.int/icd#2786_837c4a6c_ab18_4db4_9125_c5358501b8f8";	
	private static String BMDS_TOP_CLS = "http://who.int/icd#2819_837c4a6c_ab18_4db4_9125_c5358501b8f8";
	
	private static String REPLACES_PROP = "http://who.int/icd#replaces";
	
	private static String WHODAS_POSTFIX = "[WHODAS]";
	private static String BMDS_POSTFIX = "[BMDS]";
	
	private static OWLModel owlModel;
	private static ICDContentModel cm;
	
	private static RDFSNamedClass gfdTopCls;
	private static RDFSNamedClass gfdRetiredCls;
	
	private static RDFSNamedClass whodasTopCls;
	private static RDFSNamedClass bmdsTopCls;
	
	public static void main(String[] args) {
		
		if (args.length < 1) {
			log.error("Needs 1 argument: Path to pprj file");
		}
		
		Project prj = Project.loadProjectFromFile(args[0], new ArrayList());
		owlModel = (OWLModel) prj.getKnowledgeBase();
		cm = new ICDContentModel(owlModel);
		
		whodasTopCls = owlModel.getRDFSNamedClass(WHODAS_TOP_CLS);
		bmdsTopCls = owlModel.getRDFSNamedClass(BMDS_TOP_CLS);
		gfdRetiredCls = owlModel.getRDFSNamedClass(GFD_RETIRED_CLS);
		
		//TODO: check class that disappeared, doing housework quickly
		
		fixWhodasSurveyClses();
		fixBmdsSurveyClses();
		
		log.info("Saving..");
		//prj.save(new ArrayList<>());
	}

	private static void fixWhodasSurveyClses() {
		log.info("Fixing WHODAS survey classes..");
		
		Collection<RDFSNamedClass> subclses = KBUtil.getNamedSubclasses(whodasTopCls, true);
		
		for (RDFSNamedClass subcls : subclses) {
			
			fixTitle(subcls, WHODAS_POSTFIX);
			deletePublicId(subcls);
			removeRetiredSupercls(subcls, bmdsTopCls);
			
			if (subcls.hasSuperclass(bmdsTopCls)) {
				System.out.println(cm.getTitleLabel(subcls));
			}
		}
	}

	
	private static void fixBmdsSurveyClses() {
		log.info("Fixing BMDS survey classes..");
		
		Collection<RDFSNamedClass> subclses = KBUtil.getNamedSubclasses(bmdsTopCls, true);
		
		for (RDFSNamedClass subcls : subclses) {
			if (subcls.hasSuperclass(whodasTopCls)) { //create a new class by cloning the old one
				System.out.println("Class in BDMS and also in WHODAS: " + cm.getTitleLabel(subcls));
				RDFSNamedClass newBdmsCls = cloneCls(subcls); //create the new class, parented only under BDMS;
				
				//remove the BDMS parent of WHODAS cls
				for (RDFSNamedClass parent : KBUtil.getNamedSuperclasses(subcls, false)) {
					if (parent.hasSuperclass(bmdsTopCls)) {
						System.out.println("- Remove from " + cm.getTitleLabel(subcls) + " BDMS parent: " + cm.getTitleLabel(parent));
						subcls.removeSuperclass(parent);
					}
				}

			} else {
				fixTitle(subcls, BMDS_POSTFIX);
				deletePublicId(subcls);
				removeRetiredSupercls(subcls, null);
				
				//remove parent from WHODAS
				for (RDFSNamedClass parent : KBUtil.getNamedSuperclasses(subcls, false)) {
					if (parent.hasSuperclass(whodasTopCls)) {
						System.out.println("-- Remove from " + cm.getTitleLabel(subcls) + " WHODAS parent: " + cm.getTitleLabel(parent));
						subcls.removeSuperclass(parent);
					}
				}
			}
		}
	}


	private static RDFSNamedClass cloneCls(RDFSNamedClass sourceCls) {
		
		Collection<RDFSNamedClass> parents = KBUtil.getNamedSuperclasses(sourceCls, false);
		for (RDFSNamedClass parent : new ArrayList<RDFSNamedClass>(parents)) {
			if (parent.hasSuperclass(whodasTopCls)) {
				parents.remove(parent);
			}
		}
		
		RDFSNamedClass newCls = owlModel.createRDFSNamedClass(IcdIdGenerator.getNextUniqueId(owlModel));
		for (RDFSNamedClass type : (Collection<RDFSNamedClass>) parents.iterator().next().getRDFTypes()) {
			newCls.addRDFType(type);
		}
		
		for (RDFSNamedClass parent : parents) {
			newCls.addSuperclass(parent);
		}
		
		newCls.removeSuperclass(owlModel.getOWLThingClass());
		
		String sourceTitle = cm.getTitleLabel(sourceCls);
		sourceTitle = sourceTitle.replaceAll("Retired - ", "");
		sourceTitle = sourceTitle.replaceAll("\\[WHODAS\\]", "").trim();
			
		RDFResource titleTerm = cm.createTitleTerm();
		titleTerm.setPropertyValue(cm.getLabelProperty(), sourceTitle + " " + BMDS_POSTFIX);
		titleTerm.setPropertyValue(cm.getLangProperty(), "en");
				
		newCls.setPropertyValue(cm.getIcdTitleProperty(), titleTerm);
		
		cm.copyLinearizationSpecificationsFromCls(newCls, sourceCls);
		cm.copyPostcoordinationSpecificationsFromCls(newCls, sourceCls);
		
		return newCls;
	}

	private static void fixTitle(RDFSNamedClass cls, String postfix) {
		RDFResource titleTerm = (RDFResource) cls.getPropertyValue(cm.getIcdTitleProperty());
		
		String titleLabel = (String) titleTerm.getPropertyValue(cm.getLabelProperty());
		titleLabel = titleLabel.replaceAll("Retired - ", "").trim();
		
		if (titleLabel.contains(postfix) == false) {
			titleLabel = titleLabel + " " + postfix;
		}
		
		titleTerm.setPropertyValue(cm.getLabelProperty(), titleLabel);
		System.out.println("New title: " + titleLabel);
	}
	
	private static void deletePublicId(RDFSNamedClass cls) {
		String publicId = (String) cls.getPropertyValue(cm.getPublicIdProperty());
		
		if (publicId != null) {
			cls.removePropertyValue(cm.getPublicIdProperty(), publicId);
			System.out.println("Needs public id:\t" + cls + "\t" + cm.getTitleLabel(cls));
		}
	}
	
	
	private static void removeRetiredSupercls(RDFSNamedClass cls, RDFSNamedClass badTopCls) {
		Collection<RDFSNamedClass> superclses = KBUtil.getNamedSuperclasses(cls, false);
		
		for (RDFSNamedClass supercls : superclses) {
			if (badTopCls != null && supercls.hasSuperclass(badTopCls)) { //keep it
				System.out.println("xxx Supercls: " + cm.getTitleLabel(supercls) + "for cls: " + cm.getTitleLabel(cls) + " in the other survey top cls");
			} else {
				if (supercls.hasSuperclass(gfdRetiredCls)) { //in retired, but not BMDS
					//remove supercls
					System.out.println("--- Remove supercls: " + cm.getTitleLabel(supercls) + " from cls: " + cm.getTitleLabel(cls));
					cls.removeSuperclass(supercls);
				} else {
					System.out.println("+++ Keep supercls: " + cm.getTitleLabel(supercls) + " for cls: " + cm.getTitleLabel(cls));
				}
			}
		}
	}
	
	
}

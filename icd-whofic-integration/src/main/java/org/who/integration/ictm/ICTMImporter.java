package org.who.integration.ictm;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import edu.stanford.bmir.whofic.SiblingReordering;
import edu.stanford.bmir.whofic.icd.ICDContentModel;
import edu.stanford.smi.protege.exception.OntologyLoadException;
import edu.stanford.smi.protege.model.Project;
import edu.stanford.smi.protege.util.SystemUtilities;
import edu.stanford.smi.protegex.owl.ProtegeOWL;
import edu.stanford.smi.protegex.owl.model.OWLModel;
import edu.stanford.smi.protegex.owl.model.RDFSNamedClass;

public class ICTMImporter {

	private final static Logger log = Logger.getLogger(ICTMImporter.class);

	private OWLModel sourceOnt;
	private OWLModel targetOnt;

	private RDFSNamedClass sourceTopClass;

	private ICDContentModel sourceCM;
	private ICDContentModel targetCM;
	
	private SiblingReordering sibReordering;
	
	private ExcludedClasses excludedClasses;

	private Set<RDFSNamedClass> traversed = new HashSet<RDFSNamedClass>();

	private int exportedClassesCount = 0;
	

	public ICTMImporter(OWLModel sourceOnt, RDFSNamedClass sourceTopClass, OWLModel targetOnt) {
		this.sourceOnt = sourceOnt;
		this.sourceTopClass = sourceTopClass;
		this.sourceCM = new ICDContentModel(sourceOnt);
		
		this.targetOnt = targetOnt;
		this.targetCM = new ICDContentModel(targetOnt);
		
		this.sibReordering = new SiblingReordering(targetCM);
		
		this.excludedClasses = new ExcludedClasses(sourceOnt, sourceCM);
	}

	public static void main(String[] args) {
		if (args.length < 3) {
			log.error("Requires 2 parameters: (1) ICD pprj or OWL file, " + 
						"(2) ICTM pprj and (3) ICTM top class to export]");
			return;
		}
		
		PropertyConfigurator.configure("log4j.properties");
		SystemUtilities.logSystemInfo();

		OWLModel targetOWLModel = openOWLFile(args[0]);
		if (targetOWLModel == null) {
			log.error("Could not open ICD project " + args[0]);
			System.exit(1);
		}

		OWLModel sourceOWLModel = openOWLFile(args[1]);
		if (sourceOWLModel == null) {
			log.error("Could not open ICTM project " + args[1]);
			System.exit(1);
		}

		RDFSNamedClass sourceICTMTopClass = sourceOWLModel.getRDFSNamedClass(args[2]);
		if (sourceICTMTopClass == null) {
			log.error("Could not find ICTM top class " + args[2]);
			System.exit(1);
		}

		exportOntology("ICTM", targetOWLModel, sourceOWLModel, sourceICTMTopClass);
		
		log.info("\n===== End export at " + new Date());
	}



	private static void exportOntology(String ontShortName, OWLModel targetOWLModel, OWLModel sourceOWLModel, 
			RDFSNamedClass sourceTopClass) {

		log.info("Started the " + ontShortName + " export");
		log.info("Top class: " + sourceTopClass.getBrowserText());

		ICTMImporter ictmImporter = new ICTMImporter(sourceOWLModel, sourceTopClass, targetOWLModel);

		try {
			ictmImporter.importClasses();
			ictmImporter.moveTopClass(sourceTopClass);
		} catch (Throwable t) {
			log.error(t.getMessage(), t);
		}

		log.info("Ended " + ontShortName + " export");
	}

	public void importClasses() {
		importClass(sourceTopClass, sourceOnt.getOWLThingClass());
	}

	private void importClass(RDFSNamedClass sourceCls, RDFSNamedClass sourceParent) {
		if (excludedClasses.isExcludedTopClass(sourceCls) == true) {
			return;
		}

		if (traversed.contains(sourceCls) == true) {
			addSuperCls(sourceCls, sourceParent);
			return;
		}

		traversed.add(sourceCls);

		try {
			ClassImporter clsExporter = new ClassImporter(sourceCls, sourceOnt, targetOnt, sourceCM, targetCM, true);
			RDFSNamedClass targetCls = clsExporter.importCls();

			addChildren(sourceCls);
		} catch (Throwable t) {
			log.error("Error at adding class: " + sourceCls, t);
		}

		exportedClassesCount++;

		if (exportedClassesCount % 100 == 0) {
			log.info("Imported " + exportedClassesCount + " classes.\t Last imported class: " + sourceCls + " \t on "
					+ new Date());
		}
	}

	private void addSuperCls(RDFSNamedClass sourceCls, RDFSNamedClass sourceParent) {
		RDFSNamedClass targetCls = ICTMUtil.getOrCreateCls(targetOnt, sourceCls.getName());
		RDFSNamedClass targetParent = ICTMUtil.getOrCreateCls(targetOnt, sourceParent.getName());
		
		targetCls.addSuperclass(targetParent);
		sibReordering.addChildToParentIndex(targetParent, targetCls, true);
		
		//remove owl:Thing
		if (targetCls.hasDirectSuperclass(targetOnt.getOWLThingClass())) {
			targetCls.removeSuperclass(targetOnt.getOWLThingClass());
		}
	}

	
	private void addChildren(RDFSNamedClass sourceCls) {
		List<RDFSNamedClass> subclses = ICTMUtil.getSortedNamedSubclasses(sourceCls);
		for (RDFSNamedClass subcls : subclses) {
			if (excludedClasses.isExcludedTopClass(subcls) == false) {
				addSuperCls(subcls, sourceCls);
				importClass(subcls, sourceCls);
			}
		}
	}

	public void moveTopClass(RDFSNamedClass sourceICTMTopClass) {
		RDFSNamedClass hangCls = targetOnt.getRDFSNamedClass(ICTMUtil.ICTM_HANG_CLASS);
		if (hangCls == null) {
			log.info("Could not find hang class: " + ICTMUtil.ICTM_HANG_CLASS);
			return;
		}
		RDFSNamedClass ictmTopCls = targetOnt.getRDFSNamedClass(sourceICTMTopClass.getName());
		ictmTopCls.addSuperclass(hangCls);
		ictmTopCls.removeSuperclass(targetOnt.getOWLThingClass());
		
		//add an entry in the parent for the ICTM top class
		targetCM.addChildToIndex(hangCls, sourceICTMTopClass, true);
	}
	
	// *************** Generic methods *************/


	private static OWLModel openOWLFile(String fileName) {
		OWLModel owlModel = null;

		if (fileName.endsWith(".pprj")) { // pprj file
			@SuppressWarnings("rawtypes")
			List errors = new ArrayList();
			Project prj = Project.loadProjectFromFile(fileName, errors);
			if (errors.size() > 0) {
				log.error("There were errors at loading project: " + fileName);
				return null;
			}
			owlModel = (OWLModel) prj.getKnowledgeBase();
		} else { // Assume OWL file
			try {
				owlModel = ProtegeOWL.createJenaOWLModelFromURI(fileName);
			} catch (OntologyLoadException e) {
				log.error(e.getMessage(), e);
			}
		}
		return owlModel;
	}


}

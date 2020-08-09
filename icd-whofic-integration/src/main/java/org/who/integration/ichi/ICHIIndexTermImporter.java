package org.who.integration.ichi;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import edu.stanford.bmir.whofic.icd.ICDContentModel;
import edu.stanford.smi.protege.model.Project;
import edu.stanford.smi.protegex.owl.model.OWLModel;
import edu.stanford.smi.protegex.owl.model.RDFResource;
import edu.stanford.smi.protegex.owl.model.RDFSNamedClass;

public class ICHIIndexTermImporter extends ICHIImporter {
	
	private static transient Logger log = Logger.getLogger(ICHIIndexTermImporter.class);
	
	private String lastIchiCode = null;
	private String lastIndexTerm = null;
	private boolean lastIndexTermToSynonym = false;
	private Map<String, RDFSNamedClass> indTerm2ClassMap = new HashMap<String, RDFSNamedClass>();
	private Map<String, RDFSNamedClass> inclNote2ClassMap = new HashMap<String, RDFSNamedClass>();
	
	private static String DELETE = "D";
	private static String INCLUDE = "I";
	private static String SYNONYM = "S";
	private static String REVIEW = "R";
	
	public static void main(String[] args) throws IOException {
		if (args.length < 2) {
			log.error("Expected 2 arguments: (1) PPRJ file to import into; (2) TSV file to import;");
			System.exit(1);
		}
		
		log.setLevel(Level.INFO);
		
		ICHIIndexTermImporter importer = new ICHIIndexTermImporter();
		importer.importTerms(args[0], args[1]);
		
	}

	private void importTerms(String pprjFileName, String tsvFileName) throws IOException {
		
		Project prj = Project.loadProjectFromFile(pprjFileName, new ArrayList<Object>());
		OWLModel owlModel = (OWLModel) prj.getKnowledgeBase();
		
		if (owlModel == null) {
			log.error("Could not load project file: " + pprjFileName);
			System.exit(1);
		}
	
		init(owlModel);

		importCSV(tsvFileName);
		
		log.info("Post-processing..");
		postprocess();
		
		log.info("Saving project..");
		prj.save(new ArrayList<Object>());
		
		log.info("Done with import.");
	}

	

	protected void init(OWLModel owlModel) {
		this.owlModel = owlModel;
		this.cm = new ICDContentModel(owlModel);
		
		this.topCls = owlModel.getRDFSNamedClass("http://who.int/icd#HealthIntervention");
		
		ICHIUtil.initInterventionCodes2ClsesMaps(cm);
	}
	
	@Override
	protected void processLine(String row) {
		String[] data = row.split(COL_SEPARATOR);
		
		String ichiCode = getString(data, 0);
		String title = getString(data, 1);
		String indexTerm = getString(data, 2);
		String inclNote = getString(data, 3);
		String action = getString(data, 4);
		String finalIndexTerm = getString(data, 5);
		//String autoWarning = getString(data, 6);
		//String actFlagComment = getString(data, 7);
		
		String origTitle = getString(data, 23); //col X
		//String origExpIndexTerm = getString(data, 24); //col Y
		
		if ( isEmptyString(action)
				&& (!isEmptyString(indexTerm) || !isEmptyString(inclNote))) {
			log.warn("Action flag is not set for row: " + row);
			return;
		}
		
		if (isEmptyString(ichiCode)) {
			ichiCode = lastIchiCode;
		}
		else {
			//update lastIchiCode and reset context
			lastIchiCode = ichiCode;
			lastIndexTerm = null;
			lastIndexTermToSynonym = false;				
			indTerm2ClassMap.clear();
			inclNote2ClassMap.clear();
			
			if (! title.equals(origTitle)) {
				updateTitle(ichiCode, origTitle, title);
			}
			
			return;
		}
		
		if (DELETE.equals(action) || REVIEW.equals(action)) {
			return;
		}
		
		RDFSNamedClass cls = ICHIUtil.getIntervention(ichiCode);
		if (cls == null) {
//			if (! DELETE.equals(action)) {
				log.warn("Couldn't find the Intervention class " + ichiCode + " for row: " + row);
//			}
			return;
		}
		//default superclass is the ICHI intervention class
		RDFSNamedClass superCls = cls;
		
		if (!isEmptyString(indexTerm)) {
			String indexTermTitle = (isEmptyString(finalIndexTerm) ? indexTerm : finalIndexTerm);
			if (SYNONYM.equals(action)) {
				//Create synonym of intervention
				addSynonymToClass( indexTermTitle, superCls);
				lastIndexTermToSynonym = true;
			}
			else if (INCLUDE.equals(action)) {
				//Create subclass of intervention
				cls = getOrCreateCls(cm, superCls, indexTermTitle);
				//add it to map
				indTerm2ClassMap.put(indexTerm, cls);
				lastIndexTermToSynonym = false;				
			}
			else {
				log.warn(String.format("Bad action flag %s. Row will be ignored: %s", action, row));
			}
			lastIndexTerm = indexTerm;
		}
		else if (!isEmptyString(inclNote)) {
			if (SYNONYM.equals(action)) {
				//Create synonym of intervention
				addSynonymToClass( finalIndexTerm, superCls);
			}
			else if (INCLUDE.equals(action)) {
				//Create subclass of intervention or index, OR synonym of appropriate inclusion note
				if (lastIndexTerm == null) {
					//Create subclass of intervention
					cls = getOrCreateCls(cm, superCls, finalIndexTerm);
					//add it to map
					inclNote2ClassMap.put(inclNote, cls);
				}
				else {
					if (lastIndexTermToSynonym) {
						//get appropriate class to add synonym
						superCls = inclNote2ClassMap.get(inclNote);
						if (superCls == null) {
							log.error(String.format("Superclass for synonym '%s' not found. Ignoring row: %s", inclNote, row));
							return;		//Instead of return, we could add the synonym to the intervention
						}
						//add synonym
						addSynonymToClass( finalIndexTerm, superCls);
					}
					else {
						//Create subclass of index
						superCls = indTerm2ClassMap.get(lastIndexTerm);
						cls = getOrCreateCls(cm, superCls, inclNote);
					}
				}
			}
			else {
				log.warn(String.format("Bad action flag %s. Row will be ignored: %s", action, row));
			}
		}
		else {
			log.error(String.format("Unexpected row content (either indexTerm or includeNotes should be set): %s", row));
		}
		
	}

	private void updateTitle(String ichiCode, String origTitle, String title) {
		RDFSNamedClass cls = ICHIUtil.getIntervention(ichiCode);
		if (cls == null) {
			log.warn("Couldn't retrive class for ICHI code: " + ichiCode);
			return;
		}
		RDFResource titleTerm = cm.getTerm(cls, cm.getIcdTitleProperty());

		String titleTermLabel = (String) titleTerm.getPropertyValue(cm.getLabelProperty());
		if (titleTermLabel == null || ! titleTermLabel.equals(origTitle) ) {
			log.warn(String.format("Old title of intervention %s does not match original title in spreadsheet. "
					+ "Old title: %s. Expected title: %s. New title set: %s.", 
					ichiCode, titleTermLabel, origTitle, title));
		}
		else if (titleTermLabel.equals(title)) {
			log.info(String.format("Title of intervention %s is already set to %s", ichiCode, title));
		}
		
		titleTerm.setPropertyValue(cm.getLabelProperty(), title);
	}

	private void addSynonymToClass(String synonym, RDFSNamedClass cls) {
		RDFResource synonymTerm = cm.createSynonymTerm();
		synonymTerm.setPropertyValue(cm.getLabelProperty(), synonym);
		cls.setPropertyValue(cm.getSynonymProperty(), synonymTerm);

	}

	/**
	 * Adatped from {@link ICHIUtil.getSupercls}
	 * @param cm
	 * @param topCls
	 * @param title
	 * @return
	 */
	public static RDFSNamedClass getOrCreateCls(ICDContentModel cm, RDFSNamedClass topCls, String title) {
		//title = title.replaceAll("^\\d+ \\- ", "");
		
		RDFSNamedClass supercls = ICHIUtil.getCls(title);
		
		if (supercls == null) {
			supercls = ICHIUtil.createInterventionCls(cm.getOwlModel(), title);
			supercls.addSuperclass(topCls);
			supercls.removeSuperclass(cm.getOwlModel().getOWLThingClass());
			
			RDFResource titleTerm = cm.createTitleTerm();
			titleTerm.setPropertyValue(cm.getLabelProperty(), title);
			supercls.setPropertyValue(cm.getIcdTitleProperty(), titleTerm);
			
			ICHIUtil.addICHILin(cm, supercls, false, false);
			ICHIUtil.addPostcoordinationSpecToIchiCls(cm, supercls);
			
			cm.addChildToIndex(topCls, supercls, true);
			
			ICHIUtil.addToCode2ClsMap(title, supercls);
		}
		
		return supercls;
	}

}

package org.who.integration.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import edu.stanford.bmir.whofic.icd.ICDContentModel;
import edu.stanford.smi.protegex.owl.model.RDFSNamedClass;

public class KBUtil {
	
	public static Collection<RDFSNamedClass> getNamedSubclasses(RDFSNamedClass cls) {
		return getNamedSubclasses(cls, false);
	}

	public static Collection<RDFSNamedClass> getNamedSubclasses(RDFSNamedClass cls, boolean transitive) {
		List<RDFSNamedClass> namedSubclses = new ArrayList<RDFSNamedClass>();
		
		for (Object subcls : cls.getSubclasses(transitive)) {
			if (subcls instanceof RDFSNamedClass) {
				namedSubclses.add((RDFSNamedClass) subcls);
			}
		}
		
		return namedSubclses;
	}
	
	public static Collection<String> getTitles(ICDContentModel cm, Collection<RDFSNamedClass> clses) {
		Collection<String> list = new ArrayList<String>();
		
		for (RDFSNamedClass cls : clses) {
			String title = cm.getTitleLabel(cls);
			title = title == null ? cls.getName() : title;
			list.add(title);
		}
		
		return list;
	}
	
	
}

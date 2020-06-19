package org.who.integration.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import edu.stanford.smi.protegex.owl.model.RDFSNamedClass;

public class KBUtil {

	public static Collection<RDFSNamedClass> getNamedSubclasses(RDFSNamedClass cls) {
		List<RDFSNamedClass> namedSubclses = new ArrayList<RDFSNamedClass>();
		
		for (Object subcls : cls.getSubclasses(false)) {
			if (subcls instanceof RDFSNamedClass) {
				namedSubclses.add((RDFSNamedClass) subcls);
			}
		}
		
		return namedSubclses;
	}
	
	
}

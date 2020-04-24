package org.who.integration.ictm;

import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;

import edu.stanford.bmir.whofic.icd.ICDContentModel;
import edu.stanford.smi.protegex.owl.model.RDFSNamedClass;

public class PublicIdCache {
	
	private final static Logger log = Logger.getLogger(PublicIdCache.class);


	private static Map<RDFSNamedClass,String> cls2publicId = new HashMap<RDFSNamedClass, String>();
	
	public static String getPublicId(ICDContentModel cm, RDFSNamedClass cls) {
		String publicId = cls2publicId.get(cls);
		if (publicId == null) {
			publicId = cm.getPublicId(cls);
			if (publicId == null) {
				log.warn("Null public ID for " + cls); //TODO: get it
				publicId = cls.getName(); //fallback
			}
			cls2publicId.put(cls, publicId);
		}
		return publicId;
	}
	
}

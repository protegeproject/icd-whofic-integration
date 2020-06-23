package org.who.integration.ichi;

import java.io.IOException;

import org.apache.log4j.Logger;

public class ICHITargetImporter extends ICHIImporter {
	private static transient Logger log = Logger.getLogger(ICHITargetImporter.class);
	
	public ICHITargetImporter() {
		super();
	}
	
	public static void main(String[] args) throws IOException {
		if (args.length < 3) {
			log.error("Expected 3 arguments: (1) PPRJ file to import into; (2) CSV file to import;"
					+ " (3) Top class under which to import content");
			System.exit(1);
		}
		
		
		ICHITargetImporter importer = new ICHITargetImporter();
		importer.importClses(args[0], args[2], args[1]);
	}
	
	
}

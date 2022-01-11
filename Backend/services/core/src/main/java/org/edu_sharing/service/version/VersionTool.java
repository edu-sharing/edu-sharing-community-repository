package org.edu_sharing.service.version;

import java.math.BigDecimal;

import org.alfresco.util.VersionNumber;



public class VersionTool {
	
	
	public String incrementVersion(String version) {
		//get new version label
		//use BigDecimal cause of rounding Problem with double
		//BigDecimal bd = BigDecimal.valueOf(Double.valueOf(version)).add(BigDecimal.valueOf(0.1));
		//String newVersion = bd.toString();
		SerialVersionLabel svl = new SerialVersionLabel(version);
		svl.minorIncrement();
		String newVersion = svl.toString();
		return newVersion;
	}
	
	public class SerialVersionLabel
    {
        /**
         * The version number delimiter
         */
        private static final String DELIMITER = ".";
        
        /**
         * The major revision number (default 1)
         */
        private int majorRevisionNumber;
        
        /**
         * The minor revision number (default 0)
         */
        private int minorRevisionNumber;        
        
        /**
         * Constructor
         * 
         * @param version  the vesion to take the version from
         */
        public SerialVersionLabel(String versionLabel)
        {
            if (versionLabel != null && versionLabel.length() != 0)
            {
                VersionNumber versionNumber = new VersionNumber(versionLabel);
                majorRevisionNumber = versionNumber.getPart(0);
                minorRevisionNumber = versionNumber.getPart(1);
            }
            else
            {
                majorRevisionNumber = 0;
                minorRevisionNumber = 0;
            }
        }
        
        /**
         * Increments the major revision numebr and sets the minor to 
         * zero.
         */
        public void majorIncrement()
        {
            this.majorRevisionNumber += 1;
            this.minorRevisionNumber = 0;
        }
        
        /**
         * Increments only the minor revision number
         */
        public void minorIncrement()
        {
            this.minorRevisionNumber += 1;
        }
        
        /**
         * Converts the serial version number into a string
         */
        public String toString()
        {
            return this.majorRevisionNumber + DELIMITER + this.minorRevisionNumber;
        }
    }

	
	public static void main(String args[]) {
		String version = "1.10";
		BigDecimal bd = BigDecimal.valueOf(Double.valueOf(version)).add(BigDecimal.valueOf(0.1));
		String newVersion = bd.toString();
		
		System.out.println(newVersion);
		
		
		
		String newVersionFixed = new VersionTool().incrementVersion(version);
		System.out.println("fixed:" + newVersionFixed);
		
		if(true) return;
	}
}

/**
 *
 *  
 * 
 * 
 *	
 *
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 *
 */
package de.metaventis.webservices.translate;

public class Test {

	public static void main(String[] args) {
		TranslateServiceLocator tsl = new TranslateServiceLocator();
		tsl.setTranslateEndpointAddress("http://127.0.0.1/esrender/application/esmain/translate.php?wsdl");
		try{
			Translate translate =  tsl.getTranslate();
			String[] result = translate.translate(null, new String[]{"280","Geschichte","Fremdsprachen"}, "1", "DE", "yes", "EAF_SYS", "1", "DE", "5");
			if(result != null){
				for(String res:result){
					System.out.println("res:"+res);
				}
			}
		}catch(Exception e){
			e.printStackTrace();
		}
	}

}

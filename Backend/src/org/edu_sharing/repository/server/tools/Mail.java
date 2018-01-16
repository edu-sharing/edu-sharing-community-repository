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
package org.edu_sharing.repository.server.tools;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Map;
import java.util.Properties;

import javax.servlet.ServletContext;

import org.apache.axis2.builder.unknowncontent.InputStreamDataSource;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.mail.EmailException;
import org.apache.commons.mail.HtmlEmail;
import org.apache.commons.mail.SimpleEmail;
import org.apache.log4j.Logger;
import org.edu_sharing.repository.server.AuthenticationToolAPI;
import org.edu_sharing.repository.server.authentication.Context;





public class Mail {
	

	public static String AUTH_TYPE_TLS = "tls";
	public static String mailConfigFile = "ccmail.properties.xml";
	private org.apache.log4j.Category logger = null;

	private Properties props = null;

	public Mail() {
		logger = Logger.getInstance(Mail.class);
		try{
			props = PropertiesHelper.getProperties(mailConfigFile, PropertiesHelper.XML);
		}catch(Exception e){
			logger.error(e.getMessage(),e);
		}
	}
	public Properties getProperties(){
		return props;
	}
	
	private void sendMail(String sender, String senderName, String receiver, String subject, String message) throws EmailException {
		logger.info("start mailing sender:" + sender + " receiver" + receiver + " message" + message);

		
		String mailServer = props.getProperty("mail.smtp.server");
		logger.info("props:" + props);
		Integer smtpPort = new Integer(props.getProperty("mail.smtp.port", "25"));
		String username = props.getProperty("mail.smtp.username");
		String password = props.getProperty("mail.smtp.passwd");
		String authType = props.getProperty("mail.authtype");
		logger.info("mailServer:" + mailServer + " smtpPort:" + smtpPort + "username:" + username + "password:" + password);
		SimpleEmail email = new SimpleEmail();
		
		
		email.setCharset("utf-8");
		
		email.setDebug(true);
		email.setHostName(mailServer);
		email.setSmtpPort(smtpPort.intValue());
		
		if(authType != null && !authType.trim().equals("")){
			if(authType.trim().equals(AUTH_TYPE_TLS)){
				email.setTLS(true);
			}else{
				logger.info("auth type "+authType +" not supported at the moment");
			}
		}
		
		

		if ((username != null && password != null) && (!username.trim().equals("") && !password.trim().equals(""))) {
			logger.info("email.setAuthentication()");
			email.setAuthentication(username, password);
		}
		try{
			logger.info("start sending mail...");
			logger.info("sender:" + sender);
			logger.info("receiver:" + receiver);
			logger.info("subject" + subject);
			logger.info("message:" + message);
			
			if(senderName != null){
				email.setFrom(sender, senderName);
			}else{
				email.setFrom(sender);
			}
			
			email.addTo(receiver);
			email.setSubject(subject);
			email.setMsg(message);
			email.send();
		}catch(EmailException e){
			if(e.getCause() != null && e.getCause().getClass().equals(javax.mail.SendFailedException.class)){
				logger.error("EmailException caused by javax.mail.SendFailedException: maybe wrong receiver");
				logger.error("sender:" + sender);
				logger.error("receiver:" + receiver);
				logger.error("subject" + subject);
				logger.error("message:" + message);
				logger.error("Exception Message:" + e.getMessage());
			}else throw e;
			
		}
		

	}
	private void sendMailHtml(ServletContext context, String sender, String senderName, String receiver, String subject, String message) throws EmailException {
		logger.info("start mailing sender:" + sender + " receiver" + receiver + " message" + message);

		
		String mailServer = props.getProperty("mail.smtp.server");
		logger.info("props:" + props);
		Integer smtpPort = new Integer(props.getProperty("mail.smtp.port", "25"));
		String username = props.getProperty("mail.smtp.username");
		String password = props.getProperty("mail.smtp.passwd");
		String authType = props.getProperty("mail.authtype");
		logger.info("mailServer:" + mailServer + " smtpPort:" + smtpPort + "username:" + username + "password:" + password);
		HtmlEmail email = new HtmlEmail();
		
		
		email.setCharset("utf-8");
		
		email.setDebug(true);
		email.setHostName(mailServer);
		email.setSmtpPort(smtpPort.intValue());
		
		if(authType != null && !authType.trim().equals("")){
			if(authType.toLowerCase().trim().equals(AUTH_TYPE_TLS)){
				email.setTLS(true);
			}else{
				logger.info("auth type "+authType +" not supported at the moment");
			}
		}
		
		

		if ((username != null && password != null) && (!username.trim().equals("") && !password.trim().equals(""))) {
			logger.info("email.setAuthentication()");
			email.setAuthentication(username, password);
		}
		try{
			logger.info("start sending mail...");
			logger.info("sender:" + sender);
			logger.info("receiver:" + receiver);
			logger.info("subject" + subject);
			logger.info("message:" + message);
			
			if(senderName != null){
				email.setFrom(sender, senderName);
			}else{
				email.setFrom(sender);
			}
			
			email.addTo(receiver);
			email.setSubject(subject);
			
			String[] data=StringUtils.splitByWholeSeparator(message,"{{logo}}");
			if(data.length>1){
				String logo = context.getRealPath("/images/logos/edu-sharing-mail.png");
				data[0]+="cid:"+email.embed(new File(logo));
				message=StringUtils.join(data,"");
			}
			
			
			email.setHtmlMsg(message);
			email.send();
		}catch(EmailException e){
			if(e.getCause() != null && e.getCause().getClass().equals(javax.mail.SendFailedException.class)){
				logger.error("EmailException caused by javax.mail.SendFailedException: maybe wrong receiver");
				logger.error("sender:" + sender);
				logger.error("receiver:" + receiver);
				logger.error("subject" + subject);
				logger.error("message:" + message);
				logger.error("Exception Message:" + e.getMessage());
			}else throw e;
			
		}
		

	}

	public void sendMail(String receiver, String subject, String message) throws EmailException {
		sendMail(props.getProperty("mail.smtp.from", "no-reply@edu-sharing.com"), null, receiver, subject, message);
	}
	
	public void sendMail(String senderName, String receiver, String subject, String message) throws EmailException {
		sendMail(props.getProperty("mail.smtp.from", "no-reply@edu-sharing.com"),senderName, receiver, subject, message);
	}
	public void sendMailHtml(ServletContext context, String senderName, String receiver,String subject,String message,Map<String,String> replace) throws Exception {
		subject=replaceString(subject,replace);
		message=replaceString(message,replace);
		sendMailHtml(context,props.getProperty("mail.smtp.from", "no-reply@edu-sharing.com"),senderName, receiver, subject, message);
	}
	public void sendMailHtml(ServletContext context,String receiver,String subject,String message,Map<String,String> replace) throws Exception {
		sendMailHtml(context,props.getProperty("mail.smtp.from", "no-reply@edu-sharing.com"), receiver, subject, message,replace);
	}

	private String replaceString(String string, Map<String, String> replace) throws Exception {
		if(replace!=null){
			for(String key : replace.keySet()){
				String value=replace.get(key);
				if(value==null)
					value="";
				string=string.replace("{{"+key+"}}",value);
			}
		}
		String[] conds=StringUtils.splitByWholeSeparator(string,"{{if ");
		try{
			for(int i=1;i<conds.length;i++){
				String cond=conds[i];
				boolean negate=false;
				if(cond.startsWith("!"))
					negate=true;
				if(negate)
					cond=cond.substring(1);
				int end=cond.indexOf("}}");
				String condVar=cond.substring(0,end);
				boolean isTrue=false;
				if(replace!=null){
					isTrue=replace.containsKey(condVar) && !replace.get(condVar).trim().isEmpty();
				}
				int endif=cond.indexOf("{{endif}}");
				String content=cond.substring(end+2,endif);
				conds[i]=isTrue ? content : "";
				conds[i]+=cond.substring(endif+9);
			}
		}catch(Throwable t){
			throw new Exception("Error evaluating if conditions in mail template, please check your template syntax",t);
		}
		string=StringUtils.join(conds,"");
		return string;
		
	}
}

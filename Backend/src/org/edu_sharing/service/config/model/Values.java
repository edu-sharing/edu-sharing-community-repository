package org.edu_sharing.service.config.model;
import javax.xml.bind.annotation.XmlElement;

public class Values{
	@XmlElement public String[] supportedLanguages;
	@XmlElement public String extension;
	@XmlElement public String registerUrl;
	@XmlElement public String recoverPasswordUrl;
	@XmlElement	public String imprintUrl;
	@XmlElement	public String helpUrl;
	@XmlElement	public String whatsNewUrl;
	@XmlElement	public String editProfileUrl;
	@XmlElement	public Boolean editProfile;
	@XmlElement	public String[] workspaceColumns;
	@XmlElement	public String[] hideMainMenu;
	@XmlElement	public LogoutInfo logout;
	@XmlElement	public MenuEntry[] menuEntries;
	@XmlElement	public ContextMenuEntry[] nodeOptions;
	@XmlElement	public ContextMenuEntry[] searchNodeOptions;
	@XmlElement	public ContextMenuEntry[] nodeStoreOptions;
	@XmlElement	public String[] allowedLicenses;
	@XmlElement	public Workflow[] workflows;
	@XmlElement	public Boolean licenseDialogOnUpload;
	@XmlElement	public Boolean nodeReport;
	@XmlElement	public Boolean branding;
	@XmlElement	public String userDisplayName;
	@XmlElement	public String defaultUsername;
	@XmlElement	public String defaultPassword;
	@XmlElement	public Banner bannerSearch;
	@XmlElement	public String[] availableMds;
	@XmlElement	public Integer searchViewType;
	@XmlElement	public Integer itemsPerRequest;
	@XmlElement	public Rendering rendering;
}

package org.edu_sharing.alfresco.service.config.model;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.xml.bind.annotation.XmlElement;
import java.io.Serializable;

public class Values implements Serializable {
	@Schema(description = "List of supported languages (e.g. 'de', 'en'). First entry is the fallback. Only one entry means fallback for all")
	@XmlElement public String[] supportedLanguages;
	@Schema(description = "Currently a dummy string, not used")
	@XmlElement public String extension;
	@Schema(description = "URL to redirect when login fails (401) or on initial login (e.g. for Shibboleth)")
	@XmlElement public String loginUrl;
	@Schema(description = "If loginUrl is set and true, local login mask is offered with link to loginUrl below for external + local logins")
	@XmlElement public Boolean loginAllowLocal;
	@Schema(description = "URL to a service providing a list of login providers (requires loginProviderTargetUrl to be set)")
	@XmlElement public String loginProvidersUrl;
	@Schema(description = "URL called when logging in with a provider. Supports placeholders: :target (server URL + loginUrl) and :entity (provider URL)")
	@XmlElement public String loginProviderTargetUrl;
	@Schema(description = "Silent login mode: 'none' (default), 'iframe', or 'redirect'")
	@XmlElement public LoginSilentMode loginSilentMode;
	@Schema(description = "Registration settings (local service, custom URLs, password recovery, required fields)")
	@XmlElement public Register register;
	@Schema(description = "URL for 'forgot password' link. Empty means button is not shown")
	@XmlElement public String recoverPasswordUrl;
	@Schema(description = "URL to imprint/legal page. Shows button if specified")
	@XmlElement public String imprintUrl;
	@Schema(description = "URL to privacy policy/data protection page. Shows button if specified")
	@XmlElement public String privacyInformationUrl;
	@Schema(description = "URL to custom help page (default is edu-sharing help). Empty string hides the button")
	@XmlElement public String helpUrl;
	@Schema(description = "URL to custom 'What's new' page (default is edu-sharing What's new). Empty string hides the button")
	@XmlElement public String whatsNewUrl;
	@Schema(description = "URL where users can edit their profile")
	@XmlElement public String editProfileUrl;
	@Schema(description = "Access denied URL for elements not accessible inside collections")
	@XmlElement public String accessDeniedInfoUrl;
	@Schema(description = "Whether user profiles can be edited within edu-sharing (not used if editProfileUrl is set)")
	@XmlElement public Boolean editProfile;
	@Schema(description = "Default displayed columns in workspace")
	@XmlElement public String[] workspaceColumns;
	@Schema(description = "Default view for shared materials: false = only direct shares, true = all shares")
	@XmlElement public boolean workspaceSharedToMeDefaultAll;
	@Schema(description = "Array of navigation items to hide (e.g. 'workspace', 'search', 'collections', 'login', 'permissions', 'safe', 'stream')")
	@XmlElement public String[] hideMainMenu;
	@Schema(description = "Logout configuration (URL, local/SSO-specific URLs, session destruction, AJAX)")
	@XmlElement public LogoutInfo logout;
	@Schema(description = "Additional custom menu entries in left sidebar (position, icon, name, URL/path, scope, etc.)")
	@XmlElement public MenuEntry[] menuEntries;
	@Schema(description = "Custom operations for right-click context menu/action bar (mode, icon, name, URL with placeholders, permissions)")
	@XmlElement public ContextMenuEntry[] customOptions;
	@Schema(description = "Custom options for the user menu (shown on username click in navigation bar)")
	@XmlElement public ContextMenuEntry[] userMenuOverrides;
	@Schema(description = "Filter license dialog to set of allowed licenses (CC_BY, CC_BY_SA, CC_BY_ND, CC_BY_NC, CC_0, PDM, etc.)")
	@XmlElement public String[] allowedLicenses;
	@Schema(description = "Define custom licenses (id, position, URL)")
	@XmlElement public License[] customLicenses;
	@Schema(description = "Workflow configuration (default receiver, default status, comment required, workflow states)")
	@XmlElement public ConfigWorkflow workflow;
	@Schema(description = "If true, show license dialog after file upload")
	@XmlElement public Boolean licenseDialogOnUpload;
	@Schema(description = "If true, show 'Report problem' option in search results (requires backend mail.report.receivers configured)")
	@XmlElement public Boolean nodeReport;
	@Schema(description = "If true (default), show edu-sharing logo top-left and 'Powered by edu-sharing' at login")
	@XmlElement public Boolean branding;
	@Schema(description = "Rating configuration")
	@XmlElement public ConfigRating rating;
	@Schema(description = "If true, show confirmation message when publishing to everyone")
	@XmlElement public Boolean publishingNotice;
	@XmlElement	public PublishingConfig publishing;
	@Schema(description = "HTML page title (displayed after environment name). Used if branding is true. Default is 'edu-sharing'")
	@XmlElement public String siteTitle;
	@Schema(description = "User display name format: 'fullName' (default), 'email', 'firstName', 'lastName', or 'authorityName'")
	@XmlElement public String userDisplayName;
	@Schema(description = "Secondary user name shown below primary name: null (default), 'authorityName', 'email', or 'email-domain'")
	@XmlElement public String userSecondaryDisplayName;
	@Schema(description = "If true (default), show user type (teacher, student, etc.) in invite dialog")
	@XmlElement public Boolean userAffiliation;
	@Schema(description = "Pre-fill login username for testing")
	@XmlElement public String defaultUsername;
	@Schema(description = "Pre-fill login password for testing")
	@XmlElement public String defaultPassword;
	@Schema(description = "Banner configuration (URL, href link, components where shown)")
	@XmlElement public Banner banner;
	@Schema(description = "Array of allowed metadata sets per repository")
	@XmlElement public AvailableMds[] availableMds;
	@Schema(description = "Array of allowed repository IDs. Use '-home-' for local repository")
	@XmlElement public String[] availableRepositories;
	@Schema(description = "Default search view type: 0 = list, 1 = tiles (default)")
	@XmlElement public Integer searchViewType;
	@Schema(description = "Default workspace view type: 0 = table (default), 1 = tiles")
	@XmlElement public Integer workspaceViewType;
	@Schema(description = "Number of elements fetched per request cycle (default: 25)")
	@XmlElement public Integer itemsPerRequest;
	@Schema(description = "Rendering settings (show preview, show download button, prerender content)")
	@XmlElement public Rendering rendering;
	@Schema(description = "Session expiration dialog configuration")
	@XmlElement public SessionExpiredDialog sessionExpiredDialog;
	@Schema(description = "Path to navigate to when accessing edu-sharing directly (default: 'login')")
	@XmlElement public String defaultLocation;
	@Schema(description = "Default landing page after login (default: 'workspace'). Can include query parameters like 'collections?scope=EDU_ALL'")
	@XmlElement public String loginDefaultLocation;
	@Schema(description = "If true, show repositories separately as lists in 'All' view")
	@XmlElement public Boolean searchGroupResults;
	@Schema(description = "Top navigation bar customization (icon, URL)")
	@XmlElement public Mainnav mainnav;
	@Schema(description = "Metadata search sidebar mode: 'never' (default), 'always', or 'auto' (desktop only)")
	@XmlElement public String searchSidenavMode;
	@Schema(description = "Right sidebar mode: 'Sidebar' (default with preview if RS2 active) or 'RenderingPage' (direct jump to render page)")
	@XmlElement public SearchPreviewMode searchPreviewMode;
	@Schema(description = "Collections configuration (allowed colors, special types like editorial)")
	@XmlElement public Collections collections;
	@Schema(description = "License agreement display settings (node IDs with HTML content per language)")
	@XmlElement public LicenseAgreement licenseAgreement;
	@Schema(description = "External services configuration")
	@XmlElement public Services services;
	@Schema(description = "Custom help menu options (key, icon, URL) - replaces helpUrl + whatsNewUrl")
	@XmlElement public HelpMenuOptions[] helpMenuOptions;
	@Schema(description = "Favicon URL")
	@XmlElement public String favicon;
	@Schema(description = "Apple touch icon URL for mobile home screen")
	@XmlElement public String appleTouchIcon;
	@Schema(description = "Array of image replacements (src match, replace with)")
	@XmlElement public Image[] images;
	@Schema(description = "Array of icon identifier replacements (original identifier, replace with)")
	@XmlElement public FontIcon[] icons;
	@Schema(description = "Stream/activity feed configuration (enabled)")
	@XmlElement public Stream stream;
	@Schema(description = "Admin panel configuration")
	@XmlElement public Admin admin;
	@Schema(description = "Quick edit dialog configuration")
	@XmlElement public SimpleEdit simpleEdit;
	@Schema(description = "Front page configuration")
	@XmlElement	public ConfigFrontpage frontpage = new ConfigFrontpage();
	@Schema(description = "File upload configuration")
	@XmlElement public ConfigUpload upload;
	@Schema(description = "Publishing configuration")
	@XmlElement public ConfigPublish publish;
	@Schema(description = "Remote repository configuration")
	@XmlElement public ConfigRemote remote;
	@Schema(description = "Problem reporting configuration")
	@XmlElement public ConfigReportProblem reportProblem;
	@Schema(description = "Custom CSS")
	@XmlElement public String customCSS;
	@Schema(description = "Theme color customization")
	@XmlElement public ConfigThemeColors themeColors;
	@Schema(description = "Privacy settings")
	@XmlElement public ConfigPrivacy privacy;
	@Schema(description = "GDPR configuration")
	@XmlElement public Gdpr gdpr;
	@XmlElement public Relations relations;

	@Schema(description = "Configuration for frontend tutorial (darkened area with highlighted element)")
	@XmlElement public ConfigTutorial tutorial;

}

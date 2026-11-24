package org.edu_sharing.repository.server;

import org.apache.commons.lang3.StringUtils;
import org.edu_sharing.alfresco.service.config.model.KeyValuePair;
import org.edu_sharing.alfresco.service.config.model.Language;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Static SEO head for entry pages (e.g. components/search, components/collections without id).
 * Texts are i18n keys from the client.config.xml (global language block, overridable per context):
 * SEO.&lt;PAGE&gt;.TITLE and SEO.&lt;PAGE&gt;.DESCRIPTION, where &lt;PAGE&gt; is the route name in upper case with "-" as "_"
 * (components/search → SEO.SEARCH, components/my-page → SEO.MY_PAGE).
 */
public class StaticSeoHead {
	public static final String DEFAULT_LANGUAGE = "de";
	/** pages which always get a canonical, even without configured texts */
	static final Set<String> CANONICAL_PAGES = Set.of("search", "collections");
	/** pages which are never enriched */
	static final Set<String> IGNORED_PAGES = Set.of("render", "error", "login", "workspace");
	private static final Pattern TITLE = Pattern.compile("<title>.*?</title>", Pattern.DOTALL | Pattern.CASE_INSENSITIVE);
	private static final Pattern PAGE = Pattern.compile("/components/([A-Za-z0-9_-]+)/?$");

	private StaticSeoHead() {
	}

	/**
	 * @return the page name of the given request path (e.g. "search"), or null if this is no static entry page
	 */
	static String getPage(String path, String query) {
		if (path == null) {
			return null;
		}
		Matcher matcher = PAGE.matcher(path);
		if (!matcher.find()) {
			return null;
		}
		String page = matcher.group(1);
		if (IGNORED_PAGES.contains(page)) {
			return null;
		}
		// a single collection is handled by the node based seo
		if ("collections".equals(page) && query != null && query.matches("(^|.*&)id=[^&]+.*")) {
			return null;
		}
		return page;
	}

	static String getKeyPrefix(String page) {
		return "SEO." + page.toUpperCase(Locale.ROOT).replace('-', '_');
	}

	/**
	 * first language of the Accept-Language header, default {@link #DEFAULT_LANGUAGE}
	 */
	static String getLanguage(String acceptLanguage) {
		if (StringUtils.isBlank(acceptLanguage)) {
			return DEFAULT_LANGUAGE;
		}
		String first = acceptLanguage.split(",")[0].split(";")[0].trim();
		first = first.split("[-_]")[0].trim().toLowerCase(Locale.ROOT);
		if (first.isEmpty() || "*".equals(first)) {
			return DEFAULT_LANGUAGE;
		}
		return first;
	}

	/**
	 * texts for the language, missing keys are filled from the default language
	 */
	static Map<String, String> getTexts(List<Language> languages, String language) {
		Map<String, String> texts = new HashMap<>();
		addTexts(texts, languages, DEFAULT_LANGUAGE);
		if (!DEFAULT_LANGUAGE.equalsIgnoreCase(language)) {
			addTexts(texts, languages, language);
		}
		return texts;
	}

	private static void addTexts(Map<String, String> texts, List<Language> languages, String language) {
		if (languages == null) {
			return;
		}
		for (Language entry : languages) {
			if (entry.language == null || !entry.language.equalsIgnoreCase(language) || entry.string == null) {
				continue;
			}
			for (KeyValuePair pair : entry.string) {
				if (pair.key != null && pair.key.startsWith("SEO.") && StringUtils.isNotBlank(pair.value)) {
					texts.put(pair.key, pair.value.trim());
				}
			}
		}
	}

	/**
	 * @param baseUrl e.g. https://repository.bycs.de/edu-sharing (without trailing slash)
	 * @return the enriched html, or the unchanged html if nothing is configured for this page
	 */
	static String enrich(String html, String page, Map<String, String> texts, String baseUrl) {
		if (page == null) {
			return html;
		}
		String prefix = getKeyPrefix(page);
		String title = texts.get(prefix + ".TITLE");
		String description = texts.get(prefix + ".DESCRIPTION");
		boolean hasTexts = title != null || description != null;
		if (!hasTexts && !CANONICAL_PAGES.contains(page)) {
			return html;
		}
		String url = baseUrl + "/components/" + page;
		StringBuilder head = new StringBuilder();
		head.append("<link rel=\"canonical\" href=\"").append(escape(url)).append("\">");
		if (hasTexts) {
			if (description != null) {
				head.append("<meta name=\"description\" content=\"").append(escape(description)).append("\">");
			}
			head.append("<meta property=\"og:type\" content=\"website\">");
			head.append("<meta property=\"og:url\" content=\"").append(escape(url)).append("\">");
			if (title != null) {
				head.append("<meta property=\"og:title\" content=\"").append(escape(title)).append("\">");
			}
			if (description != null) {
				head.append("<meta property=\"og:description\" content=\"").append(escape(description)).append("\">");
			}
			head.append("<meta name=\"twitter:card\" content=\"summary\">");
		}
		if ("search".equals(page) && title != null) {
			head.append("<script type=\"application/ld+json\">").append(getWebSiteJson(title, baseUrl)).append("</script>");
		}
		if (title != null) {
			html = setTitle(html, title);
		}
		return addToHead(head.toString(), html);
	}

	static String getWebSiteJson(String title, String baseUrl) {
		// the site name is the part after the last separator ("Suche | ByCS-Mediathek" → "ByCS-Mediathek")
		String name = title.contains("|") ? title.substring(title.lastIndexOf('|') + 1).trim() : title;
		JSONObject action = new JSONObject();
		action.put("@type", "SearchAction");
		action.put("target", baseUrl + "/components/search?q={search_term_string}");
		action.put("query-input", "required name=search_term_string");
		JSONObject site = new JSONObject();
		site.put("@context", "https://schema.org");
		site.put("@type", "WebSite");
		site.put("name", name);
		site.put("url", baseUrl + "/");
		site.put("potentialAction", action);
		// org.json escapes "</" as "<\/", so the json can't close the script tag
		return site.toString();
	}

	static String setTitle(String html, String title) {
		String tag = "<title>" + escape(title) + "</title>";
		Matcher matcher = TITLE.matcher(html);
		if (matcher.find()) {
			return matcher.replaceFirst(Matcher.quoteReplacement(tag));
		}
		return addToHead(tag, html);
	}

	static String addToHead(String head, String html) {
		int pos = html.indexOf("</head>");
		if (pos < 0) {
			return html;
		}
		return html.substring(0, pos) + head + html.substring(pos);
	}

	private static String escape(String value) {
		StringBuilder sb = new StringBuilder(value.length() + 16);
		for (char c : value.toCharArray()) {
			switch (c) {
				case '&' -> sb.append("&amp;");
				case '<' -> sb.append("&lt;");
				case '>' -> sb.append("&gt;");
				case '"' -> sb.append("&quot;");
				case '\'' -> sb.append("&#39;");
				default -> sb.append(c);
			}
		}
		return sb.toString();
	}
}

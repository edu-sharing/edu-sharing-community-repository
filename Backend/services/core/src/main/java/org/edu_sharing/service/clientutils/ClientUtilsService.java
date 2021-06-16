package org.edu_sharing.service.clientutils;

import java.net.URL;

import org.apache.commons.lang.StringEscapeUtils;
import org.edu_sharing.alfresco.lightbend.LightbendConfigLoader;
import org.edu_sharing.repository.server.tools.HttpQueryTool;
import org.edu_sharing.repository.server.tools.LRMITool;
import org.htmlparser.NodeFilter;
import org.htmlparser.Parser;
import org.htmlparser.Tag;
import org.htmlparser.filters.NodeClassFilter;
import org.htmlparser.filters.TagNameFilter;
import org.htmlparser.lexer.Lexer;
import org.htmlparser.tags.MetaTag;
import org.htmlparser.tags.ScriptTag;
import org.htmlparser.tags.TitleTag;
import org.htmlparser.util.NodeList;
import org.json.JSONObject;

public class ClientUtilsService {
	public static WebsiteInformation getWebsiteInformation(String url) {
		WebsiteInformation info = new WebsiteInformation();
		String page = url;
		if (page.startsWith("http://"))
			page = page.substring(7);
		if (page.startsWith("https://"))
			page = page.substring(8);
		info.setPage(page);
		boolean resolveWebsites = LightbendConfigLoader.get().getBoolean("repository.communication.resolveUrlInformation");
		if (!resolveWebsites) {
			return info;
		}
		try {
			new URL(url);
			HttpQueryTool httpQuery = new HttpQueryTool();
			String result = httpQuery.query(url);
			if (result == null)
				return null;
			Parser parser = new Parser(new Lexer(result));

			NodeFilter filter = new NodeClassFilter(TitleTag.class);

			NodeList list = parser.parse(filter);
			for (int i = 0; i < list.size(); i++) {
				TitleTag titleTag = (TitleTag) list.elementAt(i);
				String rawTagName = titleTag.getRawTagName().toLowerCase();
				if (rawTagName != null) {

					String title = titleTag.getTitle();
					if (title != null)
						title = StringEscapeUtils.unescapeHtml(title.trim());
					else
						title = null;
					info.setTitle(title);
					
				}
			}
			parser = new Parser(new Lexer(result));
			filter = new NodeClassFilter(ScriptTag.class);
			list = parser.parse(filter);
			for (int i = 0; i < list.size(); i++) {
				ScriptTag scriptTag = (ScriptTag) list.elementAt(i);
				String scriptType = scriptTag.getType();
				if (scriptType != null && scriptType.equals("application/ld+json")) {
					info.setLrmiProperties(LRMITool.fromLRMIJsonToProperties(new JSONObject(scriptTag.getScriptCode())));
				}
			}

			parser = new Parser(new Lexer(result));
			filter = new NodeClassFilter(MetaTag.class);
			list = parser.parse(filter);
			for (int i = 0; i < list.size(); i++) {
				MetaTag metaTag = (MetaTag) list.elementAt(i);
				String name = metaTag.getAttribute("name");
				String content = metaTag.getAttribute("content");
				if (name != null && content != null) {
					if (name.equals("description"))
						info.setDescription(content);
					if (name.equals("keywords"))
						info.setKeywords(content.split(","));

					if(name.equals("twitter:image")){
						info.setTwitterImage(content);
					}
				}


			}
			String[] licenses = new String[] { "by", "by-nd", "by-sa", "by-nc", "by-nc-nd", "by-nc-sa" };
			parser = new Parser(new Lexer(result));
			filter = new TagNameFilter("link");
			list = parser.parse(filter);
			for (int i = 0; i < list.size(); i++) {
				Tag tag = (Tag) list.elementAt(i);
				String rel = tag.getAttribute("rel");
				String href = tag.getAttribute("href");
				if (rel != null && href != null) {
					for (String license : licenses) {
						String search = "creativecommons.org/licenses/" + license + "/";
						int index = href.indexOf(search);
						if (index > -1) {
							String version = null;
							try {
								version = href.substring(index + search.length()).split("/")[0];
							} catch (Throwable t) {
							}
							info.setLicense(new WebsiteInformation.License("CC_" + license.toUpperCase().replace("-", "_"), version));
						}
					}
				}
			}
			return info;

		} catch (Throwable e) {
			e.printStackTrace();
		}
		return null;
	}
}

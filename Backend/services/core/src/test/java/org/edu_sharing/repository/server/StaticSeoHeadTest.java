package org.edu_sharing.repository.server;

import org.edu_sharing.alfresco.service.config.model.KeyValuePair;
import org.edu_sharing.alfresco.service.config.model.Language;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class StaticSeoHeadTest {
	private static final String BASE = "https://repository.bycs.de/edu-sharing";
	private static final String HTML = "<html><head><meta charset=\"utf-8\" /><title>ByCS</title></head><body><es-app></es-app></body></html>";

	@Test
	void getPage() {
		assertEquals("search", StaticSeoHead.getPage("/edu-sharing/components/search", null));
		assertEquals("search", StaticSeoHead.getPage("/edu-sharing/components/search/", "query=abc"));
		assertEquals("collections", StaticSeoHead.getPage("/edu-sharing/components/collections", "scope=MY"));
		assertEquals("my-page", StaticSeoHead.getPage("/edu-sharing/components/my-page", null));
		assertNull(StaticSeoHead.getPage("/edu-sharing/components/collections", "id=abc-123"));
		assertNull(StaticSeoHead.getPage("/edu-sharing/components/collections", "scope=MY&id=abc-123"));
		assertNull(StaticSeoHead.getPage("/edu-sharing/components/render/abc-123", null));
		assertNull(StaticSeoHead.getPage("/edu-sharing/components/login", null));
		assertNull(StaticSeoHead.getPage("/edu-sharing/components/workspace", null));
		assertNull(StaticSeoHead.getPage("/edu-sharing/index.html", null));
		assertNull(StaticSeoHead.getPage(null, null));
	}

	@Test
	void getKeyPrefix() {
		assertEquals("SEO.SEARCH", StaticSeoHead.getKeyPrefix("search"));
		assertEquals("SEO.MY_PAGE", StaticSeoHead.getKeyPrefix("my-page"));
	}

	@Test
	void getLanguage() {
		assertEquals("de", StaticSeoHead.getLanguage(null));
		assertEquals("de", StaticSeoHead.getLanguage(""));
		assertEquals("de", StaticSeoHead.getLanguage("*"));
		assertEquals("en", StaticSeoHead.getLanguage("en-US,en;q=0.9,de;q=0.8"));
		assertEquals("fr", StaticSeoHead.getLanguage("fr;q=0.9"));
	}

	@Test
	void getTextsFallsBackToDefaultLanguage() {
		List<Language> languages = List.of(
				language("de", "SEO.SEARCH.TITLE", "Suche", "SEO.SEARCH.DESCRIPTION", "Beschreibung", "SEARCH.TITLE", "ignored"),
				language("en", "SEO.SEARCH.TITLE", "Search")
		);
		Map<String, String> en = StaticSeoHead.getTexts(languages, "en");
		assertEquals("Search", en.get("SEO.SEARCH.TITLE"));
		assertEquals("Beschreibung", en.get("SEO.SEARCH.DESCRIPTION"));
		assertFalse(en.containsKey("SEARCH.TITLE"));
		assertEquals("Suche", StaticSeoHead.getTexts(languages, "fr").get("SEO.SEARCH.TITLE"));
		assertTrue(StaticSeoHead.getTexts(null, "de").isEmpty());
	}

	@Test
	void enrichSearch() {
		String html = StaticSeoHead.enrich(HTML, "search", Map.of(
				"SEO.SEARCH.TITLE", "Suche | ByCS-Mediathek",
				"SEO.SEARCH.DESCRIPTION", "Materialien finden"
		), BASE);
		assertTrue(html.contains("<title>Suche | ByCS-Mediathek</title>"));
		assertFalse(html.contains("<title>ByCS</title>"));
		assertTrue(html.contains("<link rel=\"canonical\" href=\"" + BASE + "/components/search\">"));
		assertTrue(html.contains("<meta name=\"description\" content=\"Materialien finden\">"));
		assertTrue(html.contains("<meta property=\"og:title\" content=\"Suche | ByCS-Mediathek\">"));
		assertTrue(html.contains("<meta property=\"og:url\" content=\"" + BASE + "/components/search\">"));
		assertTrue(html.indexOf("application/ld+json") < html.indexOf("</head>"));

		String json = html.substring(html.indexOf("application/ld+json\">") + "application/ld+json\">".length(), html.indexOf("</script>"));
		JSONObject site = new JSONObject(json);
		assertEquals("WebSite", site.getString("@type"));
		assertEquals("ByCS-Mediathek", site.getString("name"));
		assertEquals(BASE + "/", site.getString("url"));
		assertEquals(BASE + "/components/search?q={search_term_string}", site.getJSONObject("potentialAction").getString("target"));
	}

	@Test
	void enrichCollectionsWithoutJsonLd() {
		String html = StaticSeoHead.enrich(HTML, "collections", Map.of("SEO.COLLECTIONS.TITLE", "Sammlungen"), BASE);
		assertTrue(html.contains("<title>Sammlungen</title>"));
		assertTrue(html.contains("/components/collections\">"));
		assertFalse(html.contains("ld+json"));
		assertFalse(html.contains("name=\"description\""));
	}

	@Test
	void canonicalOnlyWithoutTexts() {
		String html = StaticSeoHead.enrich(HTML, "search", Map.of(), BASE);
		assertTrue(html.contains("<link rel=\"canonical\" href=\"" + BASE + "/components/search\">"));
		assertTrue(html.contains("<title>ByCS</title>"));
		assertFalse(html.contains("og:"));
		assertFalse(html.contains("ld+json"));
	}

	@Test
	void otherPagesOnlyWithTexts() {
		assertEquals(HTML, StaticSeoHead.enrich(HTML, "my-page", Map.of(), BASE));
		assertEquals(HTML, StaticSeoHead.enrich(HTML, null, Map.of("SEO.SEARCH.TITLE", "x"), BASE));
		String html = StaticSeoHead.enrich(HTML, "my-page", Map.of("SEO.MY_PAGE.DESCRIPTION", "Meine Seite"), BASE);
		assertTrue(html.contains("content=\"Meine Seite\""));
		assertTrue(html.contains("<title>ByCS</title>"));
	}

	@Test
	void escaping() {
		String html = StaticSeoHead.enrich(HTML, "search", Map.of(
				"SEO.SEARCH.TITLE", "<b>A & \"B\"</b> | X</script>",
				"SEO.SEARCH.DESCRIPTION", "\"><script>alert(1)</script>"
		), BASE);
		assertTrue(html.contains("<title>&lt;b&gt;A &amp; &quot;B&quot;&lt;/b&gt; | X&lt;/script&gt;</title>"));
		assertTrue(html.contains("content=\"&quot;&gt;&lt;script&gt;alert(1)&lt;/script&gt;\""));
		assertFalse(html.contains("<script>alert"));
		// only the closing tag of the json-ld block itself
		assertEquals(html.indexOf("</script>"), html.lastIndexOf("</script>"));
	}

	@Test
	void noHead() {
		assertEquals("<html></html>", StaticSeoHead.enrich("<html></html>", "search", Map.of("SEO.SEARCH.TITLE", "x"), BASE));
	}

	private static Language language(String lang, String... keyValues) {
		Language language = new Language();
		language.language = lang;
		language.string = new ArrayList<>();
		for (int i = 0; i < keyValues.length; i += 2) {
			KeyValuePair pair = new KeyValuePair();
			pair.key = keyValues[i];
			pair.value = keyValues[i + 1];
			language.string.add(pair);
		}
		return language;
	}
}

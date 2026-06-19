package org.edu_sharing.repository.server.tools;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathFactory;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.HashMap;
import java.util.Map;

public class VCardPIDMapper {

    private static final HttpClient client = HttpClient.newHttpClient();
    private static final ObjectMapper mapper = new ObjectMapper();

    public static Map<String, String> build(String idUrl) {
        Map<String, String> vcard = new HashMap<>();

        if (idUrl == null || idUrl.isBlank()) {
            return vcard;
        }

        vcard.put(CCConstants.VCARD_URL, idUrl);

        try {
            if (idUrl.contains("orcid.org")) {
                applyOrcid(vcard, idUrl);
            } else if (idUrl.contains("d-nb.info/gnd")) {
                applyGnd(vcard, idUrl);
            } else if (idUrl.contains("ror.org")) {
                applyRor(vcard, idUrl);
            }else {
                vcard.put(CCConstants.VCARD_SURNAME, idUrl);
            }
        } catch (Exception e) {
            // optional logging
        }

        return vcard;
    }

    // ---------------- ORCID ----------------

    private static void applyOrcid(Map<String, String> vcard, String url) throws Exception {
        String id = url.substring(url.lastIndexOf("/") + 1);

        vcard.put(CCConstants.VCARD_T_X_ORCID, url);

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create("https://pub.orcid.org/v3.0/" + id))
                .header("Accept", "application/json")
                .build();

        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());
        JsonNode root = mapper.readTree(resp.body());

        String given = root.at("/person/name/given-names/value").asText("");
        String family = root.at("/person/name/family-name/value").asText("");

        if (!given.isBlank()) {
            vcard.put(CCConstants.VCARD_GIVENNAME, given);
        }
        if (!family.isBlank()) {
            vcard.put(CCConstants.VCARD_SURNAME, family);
        }

        vcard.put(CCConstants.VCARD_T_FN, (given + " " + family).trim());
        //vcard.put(CCConstants.VCARD_SURNAME, (given + " " + family).trim());
    }

    // ---------------- GND ----------------

    private static void applyGnd(Map<String, String> vcard, String url) throws Exception {

        vcard.put(CCConstants.VCARD_T_X_GND_URI, url);

        String id = url.substring(url.lastIndexOf("/") + 1);

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create("https://lobid.org/gnd/" + id + ".json"))
                .header("Accept", "application/json")
                .GET()
                .build();

        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());

        if (resp.statusCode() != 200 || resp.body() == null || resp.body().isBlank()) {
            return;
        }

        JsonNode root = mapper.readTree(resp.body());

        String name = root.path("preferredName").asText("");

        if (!name.isBlank()) {
            vcard.put(CCConstants.VCARD_T_FN, name);

            // --- Typ-Check: Person vs Organization ---
            JsonNode types = root.path("type");

            boolean isPerson = false;
            if (types.isArray()) {
                for (JsonNode t : types) {
                    if ("Person".equalsIgnoreCase(t.asText())) {
                        isPerson = true;
                        break;
                    }
                }
            }

            if (isPerson) {
                applyPersonNameHeuristics(vcard, name);
            } else {
                // Organisation → kein SURNAME
                vcard.put(CCConstants.VCARD_ORG, name);
            }
        }
    }

    private static void applyPersonNameHeuristics(Map<String, String> vcard, String name) {

        if (name == null || name.isBlank()) return;

        String[] parts = name.trim().split("\\s+");

        if (parts.length == 1) {
            vcard.put(CCConstants.VCARD_SURNAME, parts[0]);
            return;
        }

        vcard.put(CCConstants.VCARD_GIVENNAME, parts[0]);
        vcard.put(CCConstants.VCARD_SURNAME, parts[parts.length - 1]);
    }

    // ---------------- ROR ----------------

    private static void applyRor(Map<String, String> vcard, String url) throws Exception {

        String id = url.substring(url.lastIndexOf("/") + 1);

        vcard.put(CCConstants.VCARD_T_X_ROR, url);

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create("https://api.ror.org/v2/organizations/" + id))
                .header("Accept", "application/json")
                .build();

        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());
        JsonNode root = mapper.readTree(resp.body());

        String name = extractRorDisplayName(root);

        if (!name.isBlank()) {
            vcard.put(CCConstants.VCARD_ORG, name);
            vcard.put(CCConstants.VCARD_T_ORG, name);

            vcard.put(CCConstants.VCARD_T_FN, name);
            //vcard.put(CCConstants.VCARD_SURNAME,name);

            // KEIN surname → bewusst leer lassen
        }
    }

    private static String extractRorDisplayName(JsonNode root) {

        JsonNode names = root.at("/names");

        if (names.isArray()) {
            for (JsonNode n : names) {
                JsonNode types = n.get("types");

                if (types != null) {
                    for (JsonNode t : types) {
                        if ("ror_display".equals(t.asText())) {
                            return n.get("value").asText("");
                        }
                    }
                }
            }

            // fallback: erstes label
            for (JsonNode n : names) {
                if (n.has("value")) {
                    return n.get("value").asText("");
                }
            }
        }

        return "";
    }
}

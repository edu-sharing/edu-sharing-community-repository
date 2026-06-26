package org.edu_sharing.service.contributor;

import org.apache.commons.lang3.StringUtils;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.client.tools.forms.VCardTool;
import org.edu_sharing.repository.server.tools.VCardConverter;
import org.edu_sharing.service.search.SearchService;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Converts between a contributor vcard string (as stored in the ccm:*contributer_* node properties)
 * and a {@link ContributorEntry}. Reuses {@link VCardConverter} for parsing and
 * {@link VCardTool} for building the canonical vcard string.
 */
public class ContributorVCardUtil {

    private ContributorVCardUtil() {
    }

    /**
     * Parse a single vcard string into a {@link ContributorEntry}.
     *
     * @return the entry, or {@code null} if the vcard could not be parsed or carries no persistent id
     *         (only contributors with at least one id are manageable).
     */
    public static ContributorEntry fromVCardString(String vcardString) {
        if (StringUtils.isBlank(vcardString)) {
            return null;
        }
        ArrayList<Map<String, Object>> parsed = VCardConverter.vcardToMap("", vcardString);
        if (parsed == null || parsed.isEmpty()) {
            return null;
        }
        Map<String, Object> data = parsed.get(0);

        String orcid = trimToNull(data.get(CCConstants.VCARD_T_X_ORCID));
        String gnduri = trimToNull(data.get(CCConstants.VCARD_T_X_GND_URI));
        String ror = trimToNull(data.get(CCConstants.VCARD_T_X_ROR));
        String wikidata = trimToNull(data.get(CCConstants.VCARD_T_X_WIKIDATA));
        String email = trimToNull(data.get(CCConstants.VCARD_EMAIL));

        // an entry is only part of a person/organization record if it carries at least one id
        if (orcid == null && gnduri == null && ror == null && wikidata == null && email == null) {
            return null;
        }

        // organizations are identified by ROR / Wikidata, persons by ORCID / GND
        SearchService.ContributorKind kind = (ror != null || wikidata != null) && orcid == null && gnduri == null
                ? SearchService.ContributorKind.ORGANIZATION
                : SearchService.ContributorKind.PERSON;

        return ContributorEntry.builder()
                .kind(kind)
                .title(trimToNull(data.get(CCConstants.VCARD_TITLE)))
                .givenname(trimToNull(data.get(CCConstants.VCARD_GIVENNAME)))
                .surname(trimToNull(data.get(CCConstants.VCARD_SURNAME)))
                .org(trimToNull(data.get(CCConstants.VCARD_ORG)))
                .email(email)
                .url(trimToNull(data.get(CCConstants.VCARD_URL)))
                .uid(trimToNull(data.get(CCConstants.VCARD_URN_UID)))
                .orcid(orcid)
                .gnduri(gnduri)
                .ror(ror)
                .wikidata(wikidata)
                .vcard(vcardString)
                .build();
    }

    /**
     * Build the canonical vcard string for the given entry.
     */
    public static String toVCardString(ContributorEntry entry) {
        Map<String, String> map = new HashMap<>();
        putIfNotBlank(map, CCConstants.VCARD_URN_UID, entry.getUid());
        putIfNotBlank(map, CCConstants.VCARD_SURNAME, entry.getSurname());
        putIfNotBlank(map, CCConstants.VCARD_GIVENNAME, entry.getGivenname());
        putIfNotBlank(map, CCConstants.VCARD_ORG, entry.getOrg());
        putIfNotBlank(map, CCConstants.VCARD_TITLE, entry.getTitle());
        putIfNotBlank(map, CCConstants.VCARD_EMAIL, entry.getEmail());
        putIfNotBlank(map, CCConstants.VCARD_URL, entry.getUrl());
        putIfNotBlank(map, CCConstants.VCARD_T_X_ORCID, entry.getOrcid());
        putIfNotBlank(map, CCConstants.VCARD_T_X_GND_URI, entry.getGnduri());
        putIfNotBlank(map, CCConstants.VCARD_T_X_ROR, entry.getRor());
        putIfNotBlank(map, CCConstants.VCARD_T_X_WIKIDATA, entry.getWikidata());
        return VCardTool.hashMap2VCard(map);
    }

    private static String trimToNull(Object value) {
        return value == null ? null : StringUtils.trimToNull(value.toString());
    }

    private static void putIfNotBlank(Map<String, String> map, String key, String value) {
        if (StringUtils.isNotBlank(value)) {
            map.put(key, value);
        }
    }
}

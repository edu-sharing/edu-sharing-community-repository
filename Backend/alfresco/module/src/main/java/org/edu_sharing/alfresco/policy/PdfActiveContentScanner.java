package org.edu_sharing.alfresco.policy;

import com.typesafe.config.Config;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.cos.COSArray;
import org.apache.pdfbox.cos.COSBase;
import org.apache.pdfbox.cos.COSDictionary;
import org.apache.pdfbox.cos.COSDocument;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.cos.COSObject;
import org.apache.pdfbox.cos.COSObjectKey;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.interactive.action.PDAction;
import org.apache.pdfbox.pdmodel.interactive.action.PDActionEmbeddedGoTo;
import org.apache.pdfbox.pdmodel.interactive.action.PDActionFactory;
import org.apache.pdfbox.pdmodel.interactive.action.PDActionJavaScript;
import org.apache.pdfbox.pdmodel.interactive.action.PDActionLaunch;

import java.io.File;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Deque;
import java.util.EnumSet;
import java.util.IdentityHashMap;
import java.util.Set;

/**
 * Detects active content inside a PDF without rendering or executing it.
 */
public class PdfActiveContentScanner {

    public enum Finding {
        JAVASCRIPT,
        LAUNCH_ACTION,
        EMBEDDED_FILE,
        XFA
    }

    /**
     * which findings are blocked, see security.fileManagement.contentVerification.pdf.block
     */
    public record BlockSettings(boolean javaScript, boolean launchActions, boolean embeddedFiles, boolean xfa) {
        public static BlockSettings fromConfig(Config block) {
            return new BlockSettings(
                    block.getBoolean("javaScript"),
                    block.getBoolean("launchActions"),
                    block.getBoolean("embeddedFiles"),
                    block.getBoolean("xfa")
            );
        }

        public Set<Finding> blocked() {
            Set<Finding> result = EnumSet.noneOf(Finding.class);
            if (javaScript) result.add(Finding.JAVASCRIPT);
            if (launchActions) result.add(Finding.LAUNCH_ACTION);
            if (embeddedFiles) result.add(Finding.EMBEDDED_FILE);
            if (xfa) result.add(Finding.XFA);
            return result;
        }
    }

    /**
     * pdf.js (like most viewers) accepts the header anywhere in the first 1024 bytes
     */
    public static final int HEADER_SEARCH_LENGTH = 1024;

    private PdfActiveContentScanner() {
    }

    /**
     * @param head the first bytes of a file (at least {@link #HEADER_SEARCH_LENGTH} if available)
     * @return true if the bytes contain a pdf header
     */
    public static boolean looksLikePdf(byte[] head, int length) {
        byte[] marker = {'%', 'P', 'D', 'F', '-'};
        outer:
        for (int i = 0; i <= Math.min(length, HEADER_SEARCH_LENGTH) - marker.length; i++) {
            for (int j = 0; j < marker.length; j++) {
                if (head[i + j] != marker[j]) {
                    continue outer;
                }
            }
            return true;
        }
        return false;
    }

    /**
     * @return all findings of the given pdf
     * @throws IOException if the file can not be parsed (e.g. broken or encrypted with a user password)
     */
    public static Set<Finding> scan(File file) throws IOException {
        try (PDDocument document = Loader.loadPDF(file)) {
            return scan(document.getDocument());
        }
    }

    static Set<Finding> scan(COSDocument document) {
        Set<Finding> findings = EnumSet.noneOf(Finding.class);
        Set<COSBase> visited = Collections.newSetFromMap(new IdentityHashMap<>());
        Deque<COSBase> queue = new ArrayDeque<>();
        queue.add(document.getTrailer());
        // also inspect objects which are not referenced from the trailer (i.e. orphaned, incremental updates)
        for (COSObjectKey key : document.getXrefTable().keySet()) {
            COSObject object = document.getObjectFromPool(key);
            if (object != null) {
                queue.add(object);
            }
        }
        while (!queue.isEmpty()) {
            COSBase base = queue.poll();
            if (base == null || !visited.add(base)) {
                continue;
            }
            if (base instanceof COSObject object) {
                queue.add(object.getObject());
            } else if (base instanceof COSDictionary dictionary) {
                inspect(dictionary, findings);
                queue.addAll(dictionary.getValues());
            } else if (base instanceof COSArray array) {
                for (COSBase element : array) {
                    queue.add(element);
                }
            }
        }
        return findings;
    }

    private static void inspect(COSDictionary dictionary, Set<Finding> findings) {
        PDAction action = PDActionFactory.createAction(dictionary);
        // /JS also covers rendition actions, /JavaScript the names tree of the catalog
        if (action instanceof PDActionJavaScript
                || dictionary.containsKey(COSName.JS) || dictionary.containsKey(COSName.JAVA_SCRIPT)) {
            findings.add(Finding.JAVASCRIPT);
        }
        if (action instanceof PDActionLaunch) {
            findings.add(Finding.LAUNCH_ACTION);
        }
        if (action instanceof PDActionEmbeddedGoTo
                || COSName.EMBEDDED_FILE.equals(dictionary.getCOSName(COSName.TYPE))
                || dictionary.containsKey(COSName.EMBEDDED_FILES) || dictionary.containsKey(COSName.EF)) {
            findings.add(Finding.EMBEDDED_FILE);
        }
        if (dictionary.containsKey(COSName.XFA)) {
            findings.add(Finding.XFA);
        }
    }
}

package org.edu_sharing.alfresco.transformer.executors;

import org.alfresco.transform.base.TransformManager;
import org.alfresco.transform.base.executors.AbstractCommandExecutor;
import org.alfresco.transform.base.executors.RuntimeExec;
import org.alfresco.transform.base.util.CustomTransformerFileAdaptor;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import org.edu_sharing.alfresco.transformer.executors.tools.Commands;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.PdfName;
import com.lowagie.text.pdf.PdfString;
import org.springframework.stereotype.Component;
import org.xhtmlrenderer.pdf.DefaultPDFCreationListener;
import org.xhtmlrenderer.pdf.ITextFontResolver;
import org.xhtmlrenderer.pdf.ITextRenderer;

@Component
public class HTML2PDFExecutor extends AbstractCommandExecutor implements CustomTransformerFileAdaptor {

    public static final String ID = "EduSharingHTML2PDFExecutor";

    /**
     * Liberation Sans (SIL OFL 1.1) is metric-compatible to Arial. Without registered fonts, Flying Saucer falls back
     * to the PDF base-14 fonts (Helvetica, Times, Courier) which are never embedded and only support WinAnsi.
     * Loaded from the classpath via BaseFont.getResourceStream
     */
    private static final String[] FONTS = {
            // https://github.com/liberationfonts/liberation-fonts/releases/tag/2.1.5
            "fonts/liberation/LiberationSans-Regular.ttf",
            "fonts/liberation/LiberationSans-Bold.ttf",
            "fonts/liberation/LiberationSans-Italic.ttf",
            "fonts/liberation/LiberationSans-BoldItalic.ttf"
    };
    // css font families mapped to the embedded fonts
    private static final String[] FONT_FAMILIES = {"Liberation Sans", "Arial"};

    private static final String PRODUCER = "edu-sharing";

    @Override
    public String getTransformerName() {
        return ID;
    }

    @Override
    protected RuntimeExec createTransformCommand() {
        /**
         * @TODO dummy command
         */
        return Commands.getFFMPegRuntimeExec();
    }

    @Override
    protected RuntimeExec createCheckCommand() {
        return createTransformCommand();
    }

    @Override
    public void transform(String sourceMimetype, String targetMimetype, Map<String, String> transformOptions, File sourceFile, File targetFile, TransformManager transformManager) throws Exception {
        String content = Files.readString(sourceFile.toPath(), StandardCharsets.UTF_8);
        ITextRenderer renderer = new ITextRenderer();
        ITextFontResolver fontResolver = renderer.getFontResolver();
        for (String font : FONTS) {
            for (String family : FONT_FAMILIES) {
                fontResolver.addFont(font, family, BaseFont.IDENTITY_H, BaseFont.EMBEDDED, null);
            }
        }
        // iText always writes its own version as producer, overwrite the info dictionary right before the document is closed
        renderer.setListener(new DefaultPDFCreationListener() {
            @Override
            public void onClose(ITextRenderer r) {
                r.getWriter().getInfo().put(PdfName.PRODUCER, new PdfString(PRODUCER));
            }
        });
        renderer.setDocumentFromString(content);
        renderer.layout();
        try (OutputStream os = new FileOutputStream(targetFile)) {
            renderer.createPDF(os);
        }
    }
}

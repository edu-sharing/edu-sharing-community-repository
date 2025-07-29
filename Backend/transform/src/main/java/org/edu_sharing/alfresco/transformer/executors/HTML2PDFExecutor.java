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
import org.springframework.stereotype.Component;
import org.xhtmlrenderer.pdf.ITextRenderer;

@Component
public class HTML2PDFExecutor extends AbstractCommandExecutor implements CustomTransformerFileAdaptor {

    public static final String ID = "EduSharingHTML2PDFExecutor";

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
        renderer.setDocumentFromString(content);
        renderer.layout();
        try (OutputStream os = new FileOutputStream(targetFile)) {
            renderer.createPDF(os);
        }
    }
}

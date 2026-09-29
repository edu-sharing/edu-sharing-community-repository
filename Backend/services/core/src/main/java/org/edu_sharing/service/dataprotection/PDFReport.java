package org.edu_sharing.service.dataprotection;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.LocaleUtils;
import org.edu_sharing.service.transform.TransformServiceStatic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;

@Slf4j
@Service
@RequiredArgsConstructor
public class PDFReport {

    private final TemplateEngine templateEngine;
    private final TransformServiceStatic transformService;

    @Value("${repository.dataprotection.templatePath:html/dataprotection/report.html}")
    private String templatePath;

    //@Value("${repository.dataprotection.links:http://edu-sharing.com}")
    @Value("#{'${repository.dataprotection.links:http://edu-sharing.com}'.split(',')}")
    private List<String> links;

    public File report(Data reportData, File dir){

        // @TODO get locale for exported user
        final Context ctx = new Context(LocaleUtils.toLocale(Locale.GERMANY));
        ctx.setVariable("data",reportData);
        ctx.setVariable("links", links);
        ctx.setVariable("titleKey", "dataprotection_header");
        ctx.setVariable("template",templatePath);
        ctx.setVariable("templateStyle","html/dataprotection/style");

        String content = templateEngine.process("html/baseLayout.html", ctx);

        byte[] result = transformService.callTransformer(
                IOUtils.toInputStream(content, StandardCharsets.UTF_8),
                content.length(),
                "text/html",
                "application/pdf",
                TransformServiceStatic.TransformerId.EDU_SHARING,
                byte[].class);
        File file = new File(dir.getPath() + "/report.pdf");
        try (FileOutputStream fos = new FileOutputStream(file)) {
            fos.write(result);
            return file;
        }catch (Exception e){
            log.error("Error while creating PDF file: {}", e.getMessage(), e);
            return null;
        }
    }


    @lombok.Data
    @lombok.Builder
    public static class Data{
        String userName;
        List<String> secondaryUserName;
        String firstName;
        String lastName;
        String email;
        List<String> roles;
        List<String> privateCollections;
        List<String> sharedCollections;
        List<String> ratings;
        List<String> feedbacks;
        List<String> comments;
        String schoolDisplayName;
        String schoolName;
        List<String> groupList;
        List<String> mediacenterList;
        String firstLogin;
        String lastLogin;
        String currentDate;
    }
}

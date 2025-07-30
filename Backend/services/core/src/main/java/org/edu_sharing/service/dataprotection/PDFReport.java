package org.edu_sharing.service.dataprotection;


import lombok.Data;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.LocaleUtils;
import org.edu_sharing.repository.client.rpc.EduGroup;
import org.edu_sharing.repository.client.rpc.User;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.service.authority.AuthorityService;
import org.edu_sharing.service.authority.AuthorityServiceFactory;
import org.edu_sharing.service.transform.TransformServiceStatic;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.io.File;
import java.io.FileOutputStream;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class PDFReport {


    @Autowired
    TemplateEngine templateEngine;

    @Autowired
    TransformServiceStatic transformService;

    @Value("${repository.dataprotection.templatePath:html/dataprotection/report.html}")
    String templatePath;

    @Value("${repository.dataprotection.link:http://edu-sharing.com}")
    String link;

    public void report(Data reportData, File dir){

        String userName = reportData.getUserName();

        // @TODO get locale for exported user
        final Context ctx = new Context(LocaleUtils.toLocale(Locale.GERMANY));
        ctx.setVariable("data",reportData);
        ctx.setVariable("link", link);
        ctx.setVariable("titleKey", "dataprotection_header");
        ctx.setVariable("template",templatePath);
        ctx.setVariable("templateStyle","html/dataprotection/style");

        String content = templateEngine.process("html/baseLayout.html", ctx);

        byte[] result = transformService.callTransformer(
                IOUtils.toInputStream(content),
                content.length(),
                "text/html",
                "application/pdf",
                TransformServiceStatic.TransformerId.EDU_SHARING,
                byte[].class);
        try (FileOutputStream fos = new FileOutputStream(dir.getPath() + "/report.pdf")) {
            fos.write(result);
        }catch (Exception e){
            e.printStackTrace();
        }
    }


    @lombok.Data
    @lombok.Builder
    public static class Data{
        String userName;
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
        String firstLogin;
        String lastLogin;
        String currentDate;
    }
}

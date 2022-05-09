/**
                    GNU LESSER GENERAL PUBLIC LICENSE
                       Version 3, 29 June 2007

 Copyright (C) 2007 Free Software Foundation, Inc. <https://fsf.org/>
 Everyone is permitted to copy and distribute verbatim copies
 of this license document, but changing it is not allowed.


  This version of the GNU Lesser General Public License incorporates
  the terms and conditions of version 3 of the GNU General Public
  License, supplemented by the additional permissions listed below.

  0. Additional Definitions.

  As used herein, "this License" refers to version 3 of the GNU Lesser
  General Public License, and the "GNU GPL" refers to version 3 of the GNU
  General Public License.

  "The Library" refers to a covered work governed by this License,
  other than an Application or a Combined Work as defined below.

  An "Application" is any work that makes use of an interface provided
  by the Library, but which is not otherwise based on the Library.
  Defining a subclass of a class defined by the Library is deemed a mode
  of using an interface provided by the Library.

  A "Combined Work" is a work produced by combining or linking an
  Application with the Library.  The particular version of the Library
  with which the Combined Work was made is also called the "Linked
  Version".

  The "Minimal Corresponding Source" for a Combined Work means the
  Corresponding Source for the Combined Work, excluding any source code
  for portions of the Combined Work that, considered in isolation, are
  based on the Application, and not on the Linked Version.

  The "Corresponding Application Code" for a Combined Work means the
  object code and/or source code for the Application, including any data
  and utility programs needed for reproducing the Combined Work from the
  Application, but excluding the System Libraries of the Combined Work.

  1. Exception to Section 3 of the GNU GPL.

  You may convey a covered work under sections 3 and 4 of this License
  without being bound by section 3 of the GNU GPL.

  2. Conveying Modified Versions.

  If you modify a copy of the Library, and, in your modifications, a
  facility refers to a function or data to be supplied by an Application
  that uses the facility (other than as an argument passed when the
  facility is invoked), then you may convey a copy of the modified
  version:

   a) under this License, provided that you make a good faith effort to
   ensure that, in the event an Application does not supply the
   function or data, the facility still operates, and performs
   whatever part of its purpose remains meaningful, or

   b) under the GNU GPL, with none of the additional permissions of
   this License applicable to that copy.

  3. Object Code Incorporating Material from Library Header Files.

  The object code form of an Application may incorporate material from
  a header file that is part of the Library.  You may convey such object
  code under terms of your choice, provided that, if the incorporated
  material is not limited to numerical parameters, data structure
  layouts and accessors, or small macros, inline functions and templates
  (ten or fewer lines in length), you do both of the following:

   a) Give prominent notice with each copy of the object code that the
   Library is used in it and that the Library and its use are
   covered by this License.

   b) Accompany the object code with a copy of the GNU GPL and this license
   document.

  4. Combined Works.

  You may convey a Combined Work under terms of your choice that,
  taken together, effectively do not restrict modification of the
  portions of the Library contained in the Combined Work and reverse
  engineering for debugging such modifications, if you also do each of
  the following:

   a) Give prominent notice with each copy of the Combined Work that
   the Library is used in it and that the Library and its use are
   covered by this License.

   b) Accompany the Combined Work with a copy of the GNU GPL and this license
   document.

   c) For a Combined Work that displays copyright notices during
   execution, include the copyright notice for the Library among
   these notices, as well as a reference directing the user to the
   copies of the GNU GPL and this license document.

   d) Do one of the following:

       0) Convey the Minimal Corresponding Source under the terms of this
       License, and the Corresponding Application Code in a form
       suitable for, and under terms that permit, the user to
       recombine or relink the Application with a modified version of
       the Linked Version to produce a modified Combined Work, in the
       manner specified by section 6 of the GNU GPL for conveying
       Corresponding Source.

       1) Use a suitable shared library mechanism for linking with the
       Library.  A suitable mechanism is one that (a) uses at run time
       a copy of the Library already present on the user's computer
       system, and (b) will operate properly with a modified version
       of the Library that is interface-compatible with the Linked
       Version.

   e) Provide Installation Information, but only if you would otherwise
   be required to provide such information under section 6 of the
   GNU GPL, and only to the extent that such information is
   necessary to install and execute a modified version of the
   Combined Work produced by recombining or relinking the
   Application with a modified version of the Linked Version. (If
   you use option 4d0, the Installation Information must accompany
   the Minimal Corresponding Source and Corresponding Application
   Code. If you use option 4d1, you must provide the Installation
   Information in the manner specified by section 6 of the GNU GPL
   for conveying Corresponding Source.)

  5. Combined Libraries.

  You may place library facilities that are a work based on the
  Library side by side in a single library together with other library
  facilities that are not Applications and are not covered by this
  License, and convey such a combined library under terms of your
  choice, if you do both of the following:

   a) Accompany the combined library with a copy of the same work based
   on the Library, uncombined with any other library facilities,
   conveyed under the terms of this License.

   b) Give prominent notice with the combined library that part of it
   is a work based on the Library, and explaining where to find the
   accompanying uncombined form of the same work.

  6. Revised Versions of the GNU Lesser General Public License.

  The Free Software Foundation may publish revised and/or new versions
  of the GNU Lesser General Public License from time to time. Such new
  versions will be similar in spirit to the present version, but may
  differ in detail to address new problems or concerns.

  Each version is given a distinguishing version number. If the
  Library as you received it specifies that a certain numbered version
  of the GNU Lesser General Public License "or any later version"
  applies to it, you have the option of following the terms and
  conditions either of that published version or of any later version
  published by the Free Software Foundation. If the Library as you
  received it does not specify a version number of the GNU Lesser
  General Public License, you may choose any version of the GNU Lesser
  General Public License ever published by the Free Software Foundation.

  If the Library as you received it specifies that a proxy can decide
  whether future versions of the GNU Lesser General Public License shall
  apply, that proxy's public statement of acceptance of any version is
  permanent authorization for you to choose that version for the
  Library.

  @see https://github.com/keensoft/alf-21970-repo

*/

package org.apache.tika.parser.pdf;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.Writer;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.apache.pdfbox.cos.COSBase;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentCatalog;
import org.apache.pdfbox.pdmodel.PDDocumentNameDictionary;
import org.apache.pdfbox.pdmodel.PDEmbeddedFilesNameTreeNode;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDResources;
import org.apache.pdfbox.pdmodel.common.COSObjectable;
import org.apache.pdfbox.pdmodel.common.PDNameTreeNode;
import org.apache.pdfbox.pdmodel.common.filespecification.PDComplexFileSpecification;
import org.apache.pdfbox.pdmodel.common.filespecification.PDEmbeddedFile;
import org.apache.pdfbox.pdmodel.graphics.xobject.PDCcitt;
import org.apache.pdfbox.pdmodel.graphics.xobject.PDJpeg;
import org.apache.pdfbox.pdmodel.graphics.xobject.PDPixelMap;
import org.apache.pdfbox.pdmodel.graphics.xobject.PDXObject;
import org.apache.pdfbox.pdmodel.graphics.xobject.PDXObjectForm;
import org.apache.pdfbox.pdmodel.graphics.xobject.PDXObjectImage;
import org.apache.pdfbox.pdmodel.interactive.action.type.PDAction;
import org.apache.pdfbox.pdmodel.interactive.action.type.PDActionURI;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotationLink;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotationMarkup;
import org.apache.pdfbox.pdmodel.interactive.digitalsignature.PDSignature;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.outline.PDDocumentOutline;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.outline.PDOutlineItem;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.outline.PDOutlineNode;
import org.apache.pdfbox.pdmodel.interactive.form.PDAcroForm;
import org.apache.pdfbox.pdmodel.interactive.form.PDField;
import org.apache.pdfbox.pdmodel.interactive.form.PDSignatureField;
import org.apache.pdfbox.util.PDFTextStripper;
import org.apache.pdfbox.util.TextPosition;
import org.apache.tika.exception.TikaException;
import org.apache.tika.extractor.EmbeddedDocumentExtractor;
import org.apache.tika.extractor.ParsingEmbeddedDocumentExtractor;
import org.apache.tika.io.IOExceptionWithCause;
import org.apache.tika.io.TikaInputStream;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.sax.EmbeddedContentHandler;
import org.apache.tika.sax.XHTMLContentHandler;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;

/**
 * Utility class that overrides the {@link PDFTextStripper} functionality
 * to produce a semi-structured XHTML SAX events instead of a plain text
 * stream.
 */
class PDF2XHTML extends PDFTextStripper {

    /**
     * format used for signature dates
     */
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");

    /**
     * Maximum recursive depth during AcroForm processing.
     * Prevents theoretical AcroForm recursion bomb.
     */
    private final static int MAX_ACROFORM_RECURSIONS = 10;


    // TODO: remove once PDFBOX-1130 is fixed:
    private boolean inParagraph = false;

    /**
     * Converts the given PDF document (and related metadata) to a stream
     * of XHTML SAX events sent to the given content handler.
     *
     * @param document PDF document
     * @param handler SAX content handler
     * @param metadata PDF metadata
     * @throws SAXException if the content handler fails to process SAX events
     * @throws TikaException if the PDF document can not be processed
     */
    public static void process(
            PDDocument document, ContentHandler handler, ParseContext context, Metadata metadata,
            PDFParserConfig config)
            throws SAXException, TikaException {
        try {
            // Extract text using a dummy Writer as we override the
            // key methods to output to the given content
            // handler.
            PDF2XHTML pdf2XHTML = new PDF2XHTML(handler, context, metadata, config);

            config.configure(pdf2XHTML);

            pdf2XHTML.writeText(document, new Writer() {
                @Override
                public void write(char[] cbuf, int off, int len) {
                }
                @Override
                public void flush() {
                }
                @Override
                public void close() {
                }
            });

        } catch (IOException e) {
            if (e.getCause() instanceof SAXException) {
                throw (SAXException) e.getCause();
            } else {
                throw new TikaException("Unable to extract PDF content", e);
            }
        }
    }

    private final ContentHandler originalHandler;
    private final ParseContext context;
    private final XHTMLContentHandler handler;
    private final PDFParserConfig config;

    private PDF2XHTML(ContentHandler handler, ParseContext context, Metadata metadata,
                      PDFParserConfig config)
            throws IOException {
        //source of config (derives from context or PDFParser?) is
        //already determined in PDFParser.  No need to check context here.
        this.config = config;
        this.originalHandler = handler;
        this.context = context;
        this.handler = new XHTMLContentHandler(handler, metadata);
    }

    void extractBookmarkText() throws SAXException {
        PDDocumentOutline outline = document.getDocumentCatalog().getDocumentOutline();
        if (outline != null) {
            extractBookmarkText(outline);
        }
    }

    void extractBookmarkText(PDOutlineNode bookmark) throws SAXException {
        PDOutlineItem current = bookmark.getFirstChild();
        if (current != null) {
            handler.startElement("ul");
            while (current != null) {
                handler.startElement("li");
                handler.characters(current.getTitle());
                handler.endElement("li");
                // Recurse:
                extractBookmarkText(current);
                current = current.getNextSibling();
            }
            handler.endElement("ul");
        }
    }

    @Override
    protected void startDocument(PDDocument pdf) throws IOException {
        try {
            handler.startDocument();
        } catch (SAXException e) {
            throw new IOExceptionWithCause("Unable to start a document", e);
        }
    }

    @Override
    protected void endDocument(PDDocument pdf) throws IOException {
        try {
            // Extract text for any bookmarks:
            extractBookmarkText();
            extractEmbeddedDocuments(pdf, originalHandler);

            //extract acroform data at end of doc
            if (config.getExtractAcroFormContent() == true) {
                extractAcroForm(pdf, handler);
            }
            handler.endDocument();
        } catch (TikaException e) {
            throw new IOExceptionWithCause("Unable to end a document", e);
        } catch (SAXException e) {
            throw new IOExceptionWithCause("Unable to end a document", e);
        }
    }

    @Override
    protected void startPage(PDPage page) throws IOException {
        try {
            handler.startElement("div", "class", "page");
        } catch (SAXException e) {
            throw new IOExceptionWithCause("Unable to start a page", e);
        }
        writeParagraphStart();
    }

    @Override
    protected void endPage(PDPage page) throws IOException {
        try {
            writeParagraphEnd();

            extractImages(page.getResources());

            // TODO: remove once PDFBOX-1143 is fixed:
            if (config.getExtractAnnotationText()) {
                for(Object o : page.getAnnotations()) {
                    if( o instanceof PDAnnotationLink ) {
                        PDAnnotationLink annotationlink = (PDAnnotationLink) o;
                        if (annotationlink.getAction()  != null) {
                            PDAction action = annotationlink.getAction();
                            if( action instanceof PDActionURI ) {
                                PDActionURI uri = (PDActionURI) action;
                                String link = uri.getURI();
                                if (link != null) {
                                    handler.startElement("div", "class", "annotation");
                                    handler.startElement("a", "href", link);
                                    handler.endElement("a");
                                    handler.endElement("div");
                                }
                            }
                        }
                    }

                    if (o instanceof PDAnnotationMarkup) {
                        PDAnnotationMarkup annot = (PDAnnotationMarkup) o;
                        String title = annot.getTitlePopup();
                        String subject = annot.getSubject();
                        String contents = annot.getContents();
                        // TODO: maybe also annot.getRichContents()?
                        if (title != null || subject != null || contents != null) {
                            handler.startElement("div", "class", "annotation");

                            if (title != null) {
                                handler.startElement("div", "class", "annotationTitle");
                                handler.characters(title);
                                handler.endElement("div");
                            }

                            if (subject != null) {
                                handler.startElement("div", "class", "annotationSubject");
                                handler.characters(subject);
                                handler.endElement("div");
                            }

                            if (contents != null) {
                                handler.startElement("div", "class", "annotationContents");
                                handler.characters(contents);
                                handler.endElement("div");
                            }

                            handler.endElement("div");
                        }
                    }
                }
            }

            handler.endElement("div");
        } catch (SAXException e) {
            throw new IOExceptionWithCause("Unable to end a page", e);
        }
    }

    private void extractImages(PDResources resources) throws SAXException, IOException {
        extractImages(resources, new HashSet<COSBase>());
    }

    private void extractImages(PDResources resources, Set<COSBase> seenThisPage) throws SAXException, IOException {

        if (resources == null) {
            return;
        }

        for (PDXObject object : resources.getXObjects().values()) {

            if (object == null) {
                continue;
            }
            COSBase cosStream = object.getCOSObject();
            if (seenThisPage.contains(cosStream)) {
                //avoid infinite recursion TIKA-1742
                continue;
            }
            seenThisPage.add(cosStream);

            if (object instanceof PDXObjectForm) {
                extractImages(((PDXObjectForm) object).getResources(), seenThisPage);
            } else if (object instanceof PDXObjectImage) {
                PDXObjectImage image = (PDXObjectImage) object;

                Metadata metadata = new Metadata();
                if (image instanceof PDJpeg) {
                    metadata.set(Metadata.CONTENT_TYPE, "image/jpeg");
                } else if (image instanceof PDCcitt) {
                    metadata.set(Metadata.CONTENT_TYPE, "image/tiff");
                } else if (image instanceof PDPixelMap) {
                    metadata.set(Metadata.CONTENT_TYPE, "image/png");
                }

                EmbeddedDocumentExtractor extractor =
                        getEmbeddedDocumentExtractor();
                if (extractor.shouldParseEmbedded(metadata)) {
                    ByteArrayOutputStream buffer = new ByteArrayOutputStream();
                    try {
                        image.write2OutputStream(buffer);
                        extractor.parseEmbedded(
                                new ByteArrayInputStream(buffer.toByteArray()),
                                new EmbeddedContentHandler(handler),
                                metadata, false);
                    } catch (IOException e) {
                        // could not extract this image, so just skip it...
                    }
                }
            }
        }
    }

    protected EmbeddedDocumentExtractor getEmbeddedDocumentExtractor() {
        EmbeddedDocumentExtractor extractor =
                context.get(EmbeddedDocumentExtractor.class);
        if (extractor == null) {
            extractor = new ParsingEmbeddedDocumentExtractor(context);
        }
        return extractor;
    }

    @Override
    protected void writeParagraphStart() throws IOException {
        // TODO: remove once PDFBOX-1130 is fixed
        if (inParagraph) {
            // Close last paragraph
            writeParagraphEnd();
        }
        assert !inParagraph;
        inParagraph = true;
        try {
            handler.startElement("p");
        } catch (SAXException e) {
            throw new IOExceptionWithCause("Unable to start a paragraph", e);
        }
    }

    @Override
    protected void writeParagraphEnd() throws IOException {
        // TODO: remove once PDFBOX-1130 is fixed
        if (!inParagraph) {
            writeParagraphStart();
        }
        assert inParagraph;
        inParagraph = false;
        try {
            handler.endElement("p");
        } catch (SAXException e) {
            throw new IOExceptionWithCause("Unable to end a paragraph", e);
        }
    }

    @Override
    protected void writeString(String text) throws IOException {
        try {
            handler.characters(text);
        } catch (SAXException e) {
            throw new IOExceptionWithCause(
                    "Unable to write a string: " + text, e);
        }
    }

    @Override
    protected void writeCharacters(TextPosition text) throws IOException {
        try {
            handler.characters(text.getCharacter());
        } catch (SAXException e) {
            throw new IOExceptionWithCause(
                    "Unable to write a character: " + text.getCharacter(), e);
        }
    }

    @Override
    protected void writeWordSeparator() throws IOException {
        try {
            handler.characters(getWordSeparator());
        } catch (SAXException e) {
            throw new IOExceptionWithCause(
                    "Unable to write a space character", e);
        }
    }

    @Override
    protected void writeLineSeparator() throws IOException {
        try {
            handler.newline();
        } catch (SAXException e) {
            throw new IOExceptionWithCause(
                    "Unable to write a newline character", e);
        }
    }

    private void extractEmbeddedDocuments(PDDocument document, ContentHandler handler)
            throws IOException, SAXException, TikaException {
        PDDocumentCatalog catalog = document.getDocumentCatalog();
        PDDocumentNameDictionary names = catalog.getNames();
        if (names == null) {
            return;
        }
        PDEmbeddedFilesNameTreeNode embeddedFiles = names.getEmbeddedFiles();

        if (embeddedFiles == null) {
            return;
        }

        Map<String, COSObjectable> embeddedFileNames = embeddedFiles.getNames();
        //For now, try to get the embeddedFileNames out of embeddedFiles or its kids.
        //This code follows: pdfbox/examples/pdmodel/ExtractEmbeddedFiles.java
        //If there is a need we could add a fully recursive search to find a non-null
        //Map<String, COSObjectable> that contains the doc info.
        if (embeddedFileNames != null) {
            processEmbeddedDocNames(embeddedFileNames);
        } else {
            List<PDNameTreeNode> kids = embeddedFiles.getKids();
            if (kids == null) {
                return;
            }
            for (PDNameTreeNode n : kids) {
                Map<String, COSObjectable> childNames = n.getNames();
                if (childNames != null) {
                    processEmbeddedDocNames(childNames);
                }
            }
        }
    }


    private void processEmbeddedDocNames(Map<String, COSObjectable> embeddedFileNames)
            throws IOException, SAXException, TikaException {
        if (embeddedFileNames == null || embeddedFileNames.isEmpty()) {
            return;
        }

        EmbeddedDocumentExtractor extractor = getEmbeddedDocumentExtractor();
        for (Map.Entry<String,COSObjectable> ent : embeddedFileNames.entrySet()) {
            PDComplexFileSpecification spec = (PDComplexFileSpecification) ent.getValue();
            PDEmbeddedFile file = spec.getEmbeddedFile();

            Metadata metadata = new Metadata();
            // TODO: other metadata?
            metadata.set(Metadata.RESOURCE_NAME_KEY, ent.getKey());
            metadata.set(Metadata.CONTENT_TYPE, file.getSubtype());
            metadata.set(Metadata.CONTENT_LENGTH, Long.toString(file.getSize()));

            if (extractor.shouldParseEmbedded(metadata)) {
                TikaInputStream stream = TikaInputStream.get(file.createInputStream());
                try {
                    extractor.parseEmbedded(
                            stream,
                            new EmbeddedContentHandler(handler),
                            metadata, false);
                } finally {
                    stream.close();
                }
            }
        }
    }

    private void extractAcroForm(PDDocument pdf, XHTMLContentHandler handler) throws IOException,
            SAXException {
        //Thank you, Ben Litchfield, for org.apache.pdfbox.examples.fdf.PrintFields
        //this code derives from Ben's code
        PDDocumentCatalog catalog = pdf.getDocumentCatalog();

        if (catalog == null)
            return;

        PDAcroForm form = catalog.getAcroForm();
        if (form == null)
            return;

        @SuppressWarnings("rawtypes")
        List fields = form.getFields();

        if (fields == null)
            return;

        @SuppressWarnings("rawtypes")
        ListIterator itr  = fields.listIterator();

        if (itr == null)
            return;

        handler.startElement("div", "class", "acroform");
        handler.startElement("ol");

        while (itr.hasNext()) {
            Object obj = itr.next();
            if (obj != null && obj instanceof PDField) {
                processAcroField((PDField)obj, handler, 0);
            }
        }
        handler.endElement("ol");
        handler.endElement("div");
    }

    private void processAcroField(PDField field, XHTMLContentHandler handler, final int recurseDepth)
            throws SAXException, IOException {

        if (recurseDepth >= MAX_ACROFORM_RECURSIONS) {
            return;
        }

        addFieldString(field, handler);

        @SuppressWarnings("rawtypes")
        List kids = field.getKids();
        if(kids != null) {

            @SuppressWarnings("rawtypes")
            Iterator kidsIter = kids.iterator();
            if (kidsIter == null) {
                return;
            }
            int r = recurseDepth+1;
            handler.startElement("ol");
            //TODO: can generate <ol/>. Rework to avoid that.
            while(kidsIter.hasNext()) {
                Object pdfObj = kidsIter.next();
                if(pdfObj != null && pdfObj instanceof PDField) {
                    PDField kid = (PDField)pdfObj;
                    //recurse
                    processAcroField(kid, handler, r);
                }
            }
            handler.endElement("ol");
        }
    }

    private void addFieldString(PDField field, XHTMLContentHandler handler) throws SAXException {
        //Pick partial name to present in content and altName for attribute
        //Ignoring FullyQualifiedName for now
        String partName = field.getPartialName();
        String altName = field.getAlternateFieldName();

        StringBuilder sb = new StringBuilder();
        AttributesImpl attrs = new AttributesImpl();

        if (partName != null) {
            sb.append(partName).append(": ");
        }
        if (altName != null) {
            attrs.addAttribute("", "altName", "altName", "CDATA", altName);
        }
        //return early if PDSignature field
        if (field instanceof PDSignatureField) {
            handleSignature(attrs, (PDSignatureField)field, handler);
            return;
        }
        try {
            //getValue can throw an IOException if there is no value
            String value = field.getValue();
            if (value != null && ! value.equals("null")) {
                sb.append(value);
            }
        } catch (IOException e) {
            //swallow
        }

        if (attrs.getLength() > 0 || sb.length() > 0) {
            handler.startElement("li", attrs);
            handler.characters(sb.toString());
            handler.endElement("li");
        }
    }

    private void handleSignature(AttributesImpl parentAttributes, PDSignatureField sigField,
                                 XHTMLContentHandler handler) throws SAXException {


        PDSignature sig = sigField.getSignature();
        if (sig == null) {
            return;
        }
        Map<String, String> vals= new TreeMap<String, String>();
        vals.put("name", sig.getName());
        vals.put("contactInfo", sig.getContactInfo());
        vals.put("location", sig.getLocation());
        vals.put("reason", sig.getReason());

        Calendar cal = sig.getSignDate();
        if (cal != null) {
            dateFormat.setTimeZone(cal.getTimeZone());
            vals.put("date", dateFormat.format(cal.getTime()));
        }
        //see if there is any data
        int nonNull = 0;
        for (String val : vals.keySet()) {
            if (val != null && ! val.equals("")) {
                nonNull++;
            }
        }
        //if there is, process it
        if (nonNull > 0) {
            handler.startElement("li", parentAttributes);

            AttributesImpl attrs = new AttributesImpl();
            attrs.addAttribute("", "type", "type", "CDATA", "signaturedata");

            handler.startElement("ol", attrs);
            for (Map.Entry<String, String> e : vals.entrySet()) {
                if (e.getValue() == null || e.getValue().equals("")) {
                    continue;
                }
                attrs = new AttributesImpl();
                attrs.addAttribute("", "signdata", "signdata", "CDATA", e.getKey());
                handler.startElement("li", attrs);
                handler.characters(e.getValue());
                handler.endElement("li");
            }
            handler.endElement("ol");
            handler.endElement("li");
        }
    }
}

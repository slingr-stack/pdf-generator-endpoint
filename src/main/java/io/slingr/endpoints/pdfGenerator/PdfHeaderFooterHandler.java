package io.slingr.endpoints.pdfGenerator;

import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import io.slingr.endpoints.utils.Json;
import io.slingr.endpoints.utils.Strings;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.*;

public class PdfHeaderFooterHandler {

    public static final String HEADER = "header";
    public static final String FOOTER = "footer";
    public static final String HEIGHT = "height";
    public static final String WIDTH = "width";
    public static final String HEADER_HTML_PATH = "headerHtmlPath";
    public static final String FOOTER_HTML_PATH = "footerHtmlPath";
    public static final String TEMP_HEADER_PATH = "tempHeaderPath";
    public static final String TEMP_FOOTER_PATH = "tempFooterPath";
    public static final String HTML = "html";
    public static final String DATA = "data";

    private static Logger logger = LoggerFactory.getLogger(PdfHeaderFooterHandler.class);

    private Map<String, String> tempFiles = new HashMap<>();


    public String setHeaderWithImage(InputStream report, String headerTemplate, float hHeight, float hWidth, String footerTemplate, float fHeight, float fWidth) {

        if (headerTemplate != null) {
            tempFiles.put(HEADER_HTML_PATH, getTempFileFromTemplate(headerTemplate));
        }
        if (footerTemplate != null) {
            tempFiles.put(FOOTER_HTML_PATH, getTempFileFromTemplate(footerTemplate));
        }

        try (final PDDocument document = PDDocument.load(report)) {

            File tempHeader = getImageFromTemplate(tempFiles.get(HEADER_HTML_PATH), document, hHeight);
            if (tempHeader != null) {
                tempFiles.put(TEMP_HEADER_PATH, tempHeader.getPath());
            }

            File tempFooter = getImageFromTemplate(tempFiles.get(FOOTER_HTML_PATH), document, hHeight);
            if (tempFooter != null) {
                tempFiles.put(TEMP_FOOTER_PATH, tempFooter.getPath());
            }

            for (int p = 0; p < document.getNumberOfPages(); ++p) {

                PDPage page = document.getPage(p);
                PDPageContentStream contents = new PDPageContentStream(document, page, PDPageContentStream.AppendMode.APPEND, true, true);

                if (tempHeader != null) {
                    hWidth = hWidth > 0 ? hWidth : page.getMediaBox().getWidth();
                    PDImageXObject imageHeader = PDImageXObject.createFromFile(tempHeader.getPath(), document);
                    contents.drawImage(imageHeader, 0, page.getMediaBox().getHeight() - hHeight, hWidth, hHeight);
                }
                if (tempFooter != null) {
                    fWidth = fWidth > 0 ? fWidth : page.getMediaBox().getWidth();
                    PDImageXObject imageFooter = PDImageXObject.createFromFile(tempFooter.getPath(), document);
                    contents.drawImage(imageFooter, 0, 0, fWidth, fHeight);
                }

                contents.close();

            }
            File pdfTemp = File.createTempFile("result-" + new Date().getTime(), ".pdf");
            tempFiles.put("pdfFilePath", pdfTemp.getPath());
            document.save(pdfTemp);
            document.close();

            return pdfTemp.getPath();

        } catch (IOException e) {
            System.err.println("Exception while trying to create pdf document - " + e);
        }

        return null;
    }

    private String getTempFileFromTemplate(String template) {

        try {
            File temp = File.createTempFile("html-" + Strings.randomUUIDString(), ".html");
            FileUtils.writeStringToFile(temp, template, "UTF-8");
            return temp.getAbsolutePath();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }

    private File getImageFromTemplate(String template, PDDocument document, float headerHeight) throws IOException {

        File tempHeader = null;

        if (template != null) {
            tempHeader = File.createTempFile("imag-" + new Date().getTime(), ".png");
            InputStream isHeader = getImage(template, getPageWidth(document, 0), headerHeight);
            FileUtils.copyInputStreamToFile(isHeader, tempHeader);
        }

        return tempHeader;
    }

    private File getImageFromInputStream(InputStream is) throws IOException {
        File tempHeader = File.createTempFile("imag-" + new Date().getTime(), ".png");
        FileUtils.copyInputStreamToFile(is, tempHeader);
        return tempHeader;
    }

    public String setHeaderWithImage(InputStream report, InputStream header, float hHeight, float hWidth, InputStream footer, int fHeight, float fWidth) {


        try (final PDDocument document = PDDocument.load(report)) {

            File tempHeader = null;
            if (header != null) {
                tempHeader = getImageFromInputStream(header);
                tempFiles.put(TEMP_HEADER_PATH, tempHeader.getPath());
            }

            File tempFooter = null;
            if (footer != null) {
                tempFooter = getImageFromInputStream(footer);
                tempFiles.put(TEMP_FOOTER_PATH, tempFooter.getPath());
            }

            for (int p = 0; p < document.getNumberOfPages(); ++p) {

                PDPage page = document.getPage(p);

                PDPageContentStream contents = new PDPageContentStream(document, page, PDPageContentStream.AppendMode.APPEND, true, true);

                if (tempHeader != null) {
                    PDImageXObject imageHeader = PDImageXObject.createFromFile(tempHeader.getPath(), document);
                    hWidth = hWidth > 0 ? hWidth : page.getMediaBox().getWidth();
                    hHeight = hHeight > 0 ? hHeight : imageHeader.getHeight();
                    contents.drawImage(imageHeader, 0, page.getMediaBox().getHeight() - hHeight, hWidth, hHeight);
                }

                if (tempFooter != null) {
                    PDImageXObject imageFooter = PDImageXObject.createFromFile(tempFooter.getPath(), document);
                    fWidth = fWidth > 0 ? fWidth : page.getMediaBox().getWidth();
                    fHeight = fHeight > 0 ? fHeight : imageFooter.getHeight();
                    contents.drawImage(imageFooter, 0, 0, fWidth, fHeight);
                }

                contents.close();

            }
            File pdfTemp = File.createTempFile("result-" + new Date().getTime(), ".pdf");
            tempFiles.put("pdfFilePath", pdfTemp.getPath());
            document.save(pdfTemp);
            document.close();

            return pdfTemp.getPath();

        } catch (IOException e) {
            System.err.println("Exception while trying to create pdf document - " + e);
        }

        return null;
    }

    public float getPageWidth(PDDocument pdf, int page) {

        if (pdf.getPages().getCount() > page) {
            return pdf.getPages().get(page).getMediaBox().getWidth();
        }

        return 0;
    }

    public float getPageHeight(InputStream document, int page) {
        try (final PDDocument pdf = PDDocument.load(document)) {
            if (pdf.getPages().getCount() > page) {
                return pdf.getPages().get(page).getMediaBox().getHeight();
            }
        } catch (IOException e) {
            System.err.println("Exception while trying to create pdf document - " + e);
        }

        return 0;
    }

    public InputStream getImage(String path, float width, float height) {

        String sourceTmpFile, targetTmpFile;

        List<String> commandParams = new ArrayList<>();
        commandParams.add("/usr/bin/wkhtmltoimage");

        commandParams.add("--width");
        commandParams.add(Integer.toString((int) width));

        commandParams.add("--height");
        commandParams.add(Integer.toString((int) height));

        sourceTmpFile = path;
        targetTmpFile = path.replaceAll("\\.html", ".png");
        tempFiles.put(path, targetTmpFile);

        commandParams.add(sourceTmpFile);
        commandParams.add(targetTmpFile);

        InputStream is = makeFile(commandParams, targetTmpFile);

        try {
            java.lang.Thread.sleep(1000);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return is;

    }


    public static InputStream makeFile(List<String> commandParams, String targetTmpFile) {

        ProcessBuilder pb;
        Process process = null;
        try {

            pb = new ProcessBuilder(commandParams);
            pb.inheritIO();
            process = pb.start();
            process.waitFor();

            return FileUtils.openInputStream(new File(targetTmpFile));

        } catch (InterruptedException | IOException e) {
            logger.info("HTML can not be converted. " + e.getMessage());

        } finally {
            if (process != null) {
                process.destroy();
            }
        }

        return null;
    }

    public String getHtmlFromTemplate(String template, Json data) {

        try {
            if (StringUtils.isNotEmpty(template)) {
                Configuration cfg = new Configuration();

                Template tpl = new Template("name", new StringReader(template), cfg);
                tpl.setAutoFlush(true);
                StringWriter sw = new StringWriter();
                if (data != null) {
                    tpl.process(data.toMap(), sw);
                }

                return sw.toString();

            }
        } catch (IOException | TemplateException ex) {
            logger.warn(String.format("Error to parse [%s] with [%s]", template, data.toString()));
        }

        return null;

    }

    public void cleanGeneratedFiles() {

        for (String key : tempFiles.keySet()) {
            if (tempFiles.get(key) != null) {
                (new File(tempFiles.get(key))).delete();
            }
        }

    }

    private int getJsonProperty(Json json, String prop) {
        return json != null && json.is(prop) ? json.integer(prop) : -1;
    }

    public String replaceHeaderAndFooterFromTemplate(InputStream file, Json settings) {

        Json header = settings.json(HEADER);
        String headerTemplate = header != null ? getHtmlFromTemplate(header.string(HTML), header.json(DATA)) : null;
        Json footer = settings.json(FOOTER);
        String footerTemplate = footer != null ? getHtmlFromTemplate(footer.string(HTML), footer.json(DATA)) : null;

        int hHeight = getJsonProperty(header, HEIGHT);
        int hWidth = getJsonProperty(header, WIDTH);
        int fHeight = getJsonProperty(footer, HEIGHT);
        int fWidth = getJsonProperty(footer, WIDTH);

        return setHeaderWithImage(file, headerTemplate, hHeight, hWidth, footerTemplate, fHeight, fWidth);

    }

    public String replaceHeaderAndFooterFromImages(InputStream file, InputStream headerIs, InputStream footerIs, Json settings) {

        Json header = settings.json(HEADER);
        Json footer = settings.json(FOOTER);

        int hHeight = getJsonProperty(header, HEIGHT);
        int hWidth = getJsonProperty(header, WIDTH);
        int fHeight = getJsonProperty(footer, HEIGHT);
        int fWidth = getJsonProperty(footer, WIDTH);

        return setHeaderWithImage(file, headerIs, hHeight, hWidth, footerIs, fHeight, fWidth);
    }
}


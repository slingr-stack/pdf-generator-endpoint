package io.slingr.endpoints.pdfGenerator;

import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import io.slingr.endpoints.Endpoint;
import io.slingr.endpoints.exceptions.EndpointException;
import io.slingr.endpoints.exceptions.ErrorCode;
import io.slingr.endpoints.framework.annotations.ApplicationLogger;
import io.slingr.endpoints.framework.annotations.EndpointFunction;
import io.slingr.endpoints.framework.annotations.SlingrEndpoint;
import io.slingr.endpoints.services.AppLogs;
import io.slingr.endpoints.services.rest.DownloadedFile;
import io.slingr.endpoints.utils.Json;
import io.slingr.endpoints.utils.Strings;
import io.slingr.endpoints.ws.exchange.FunctionRequest;
import org.apache.commons.lang.StringUtils;
import org.apache.pdfbox.cos.*;
import org.apache.pdfbox.multipdf.PDFMergerUtility;
import org.apache.pdfbox.multipdf.Splitter;
import org.apache.pdfbox.pdmodel.*;
import org.apache.pdfbox.pdmodel.graphics.PDXObject;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.ReentrantLock;

/**
 * <p>PDF Generator endpoint
 * <p>
 * <p>Created by hpacini on 24/10/17.
 */
@SlingrEndpoint(name = "pdf-generator", functionPrefix = "_")
public class PdfGenerator extends Endpoint {

    public static final String IMAGE_ID = "imageId";
    public static final String HTML = "html";
    private Logger logger = LoggerFactory.getLogger(PdfGenerator.class);
    private PdfFillForm pdfFillForm;

    @ApplicationLogger
    protected AppLogs appLogger;

    public void endpointStarted() {
        this.pdfFillForm = new PdfFillForm(appLogger);
        if (!properties().isLocalDeployment()) {
            try {
                PdfFilesUtils pdfFilesUtils = new PdfFilesUtils();
                pdfFilesUtils.executeCommands();
                pdfFilesUtils.exportResource("wkhtmltopdf");
                pdfFilesUtils.exportResource("wkhtmltoimage");
            } catch (Exception ex) {
                logger.error(ex.getMessage(), ex);
            }
        }


        Executors.newSingleThreadScheduledExecutor().execute(() -> {
            while (true) {
                generateAutoPdf();
                try {
                    Thread.sleep(3000);
                } catch (InterruptedException e) {
                    logger.info("Generate pdf thread was interrupted.");
                }
            }
        });

    }

    @EndpointFunction(name = "_generatePdf")
    public Json generatePdf(FunctionRequest request) {
        logger.info("Creating pdf from template");

        Json data = request.getJsonParams();
        Json resp = Json.map();
        String template = data.string("template");
        if (StringUtils.isBlank(template)) {
            throw EndpointException.permanent(ErrorCode.ARGUMENT, "Template can not be empty.");
        }
        Json jData = data.json("data");
        if (jData == null) {
            jData = Json.map();
        }

        Configuration cfg = new Configuration();
        Template tpl;
        StringWriter sw = null;
        try {
            tpl = new Template("name", new StringReader(template), cfg);
            tpl.setAutoFlush(true);
            sw = new StringWriter();
            tpl.process(jData.toMap(), sw);
            data.set("tpl", sw.toString());
            QueuePdf.getStreamInstance().add(request);
            resp.set("status", "ok");
        } catch (IOException e) {
            logger.error("Can not generate PDF, I/O exception", e);
            throw EndpointException.permanent(ErrorCode.GENERAL, "Failed to create file", e);
        } catch (TemplateException e) {
            logger.error("Can not generate PDF, template exception", e);
            throw EndpointException.permanent(ErrorCode.GENERAL, "Failed to parse template", e);
        } finally {
            try {
                if (sw != null) {
                    sw.flush();
                    sw.close();
                }
            } catch (IOException ioe) {
                logger.info("String writer can not be closed.");
            }
        }

        return resp;
    }

    @EndpointFunction(name = "_fillForm")
    public Json fillForm(FunctionRequest request) {

        Json data = request.getJsonParams();

        if (data.contains("sync") && data.bool("sync")) {
            fillForm(request, data);
        } else {
            Executors.newSingleThreadScheduledExecutor().execute(() -> {
                fillForm(request, data);
            });
        }

        return Json.map();
    }

    private void fillForm(FunctionRequest request, Json data) {
        String fileId = data.string("fileId");
        if (fileId == null) {
            appLogger.info("Can not find any pdf with null file id");
            return;
        }
        Json res = Json.map();
        InputStream tmpIs = null;
        try {

            if (data.contains("settings")) {
                Json settings = data.json("settings");

                File temp = pdfFillForm.fillForm(files(), fileId, settings);
                if (temp == null) {
                    appLogger.info("Can generate filled form. Contact the support.");
                    return;
                }

                String fileName = getFileName("pdf", settings);
                appLogger.info("Uploading file");
                tmpIs = new FileInputStream(temp);

                Json fileJson = files().upload(fileName, tmpIs, "application/pdf");

                res.set("status", "ok");
                res.set("file", fileJson);

                events().send("pdfResponse", res, request.getFunctionId());
            } else {
                events().send("pdfResponse", res, request.getFunctionId());
            }
        } catch (IOException e) {

            appLogger.error("Can not generate PDF, I/O exception", e);

            res.set("status", "error");
            res.set("message", "Failed to create file");

            events().send("pdfResponse", res, request.getFunctionId());
        } finally {
            if (tmpIs != null) {
                try {
                    tmpIs.close();
                } catch (IOException ioe) {
                    logger.error("Can not close temporal file stream", ioe.getMessage());
                }
            }
        }
    }

    @EndpointFunction(name = "_mergeDocuments")
    public Json mergeDocuments(FunctionRequest request) {

        Json data = request.getJsonParams();
        Json docs = data.json("documents");

        if (docs != null && docs.isList()) {

            Json res = Json.map();

            File temp;
            PDDocument newDocument;

            boolean isError = false;
            String errorMessage = null;

            try {

                PDFMergerUtility merger = new PDFMergerUtility();
                Splitter splitter = new Splitter();
                newDocument = new PDDocument();

                for (Object d : docs.objects()) {

                    Json doc = (Json) d;

                    String fileId = doc.string("file");
                    DownloadedFile downloadedFile = files().download(fileId);
                    InputStream is = downloadedFile.getFile();

                    PDDocument pdf = PDDocument.load(is);
                    List<PDDocument> splitDoc = splitter.split(pdf);

                    if (splitDoc.size() >= 0) {
                        int i = 1;
                        for (PDDocument page : splitDoc) {

                            if ((doc.is("start") && doc.is("end") && i >= doc.integer("start") && i <= doc.integer("end"))
                                    || (doc.is("start") && !doc.is("end") && i >= doc.integer("start"))
                                    || (!doc.is("start") && doc.is("end") && i <= doc.integer("end"))
                                    || (!doc.is("start") && !doc.is("end"))
                            ) {
                                merger.appendDocument(newDocument, page);
                            }
                            page.close();
                            i++;
                        }
                    } else {
                        for (PDDocument page : splitDoc) {
                            page.close();
                        }
                        isError = true;
                        errorMessage = "Start should be smaller than end.";
                        logger.info(errorMessage);
                    }

                    splitDoc.clear();
                    is.close();
                    pdf.close();

                }

                temp = File.createTempFile("merged-doc-", ".pdf");
                newDocument.save(temp);

                newDocument.close();
                Json fileJson = files().upload(temp.getName(), new FileInputStream(temp), "application/pdf");

                if (!isError) {
                    res.set("status", "ok");
                    res.set("file", fileJson);
                } else {
                    res.set("status", "error");
                    res.set("message", errorMessage);
                }

                events().send("pdfResponse", res, request.getFunctionId());

            } catch (IOException e) {
                logger.info("Error to create merged file. " + e.getMessage());
            }

        } else {
            Json res = Json.map();
            res.set("status", "error");
            res.set("message", "The property documents should be a valid list.");
            events().send("pdfResponse", res, request.getFunctionId());
        }

        return Json.map().set("status", "ok");

    }

    @EndpointFunction(name = "_splitDocument")
    public Json splitDocument(FunctionRequest request) {

        Json data = request.getJsonParams();
        String fileId = data.string("fileId");
        Integer interval = data.integer("interval");

        if (StringUtils.isBlank(fileId)) {
            throw EndpointException.permanent(ErrorCode.ARGUMENT, "File id can not be empty.");
        } else if (interval == null || interval <= 0) {
            throw EndpointException.permanent(ErrorCode.ARGUMENT, "Interval can not be empty. Should be a positive integer.");
        }

        try {

            List<File> documents = new ArrayList<>();

            PDFMergerUtility merger = new PDFMergerUtility();
            Splitter splitter = new Splitter();

            InputStream is = files().download(fileId).getFile();
            PDDocument pdf = PDDocument.load(is);

            List<PDDocument> splitDoc = splitter.split(pdf);

            if (splitDoc.size() > 0) {

                for (int i = 0; i < splitDoc.size(); i += interval) {

                    int end = (i + interval < splitDoc.size()) ? i + interval : splitDoc.size();
                    List<PDDocument> sp = splitDoc.subList(i, end);

                    PDDocument newDocument = new PDDocument();
                    for (PDDocument page : sp) {
                        merger.appendDocument(newDocument, page);
                        page.close();
                    }

                    int number = i / interval;
                    File temp = File.createTempFile("split-doc-" + number, ".pdf");
                    newDocument.save(temp);
                    newDocument.close();

                    documents.add(temp);

                }

            }

            Json files = Json.list();

            for (File doc : documents) {
                Json fileJson = files().upload(doc.getName(), new FileInputStream(doc), "application/pdf");
                files.push(fileJson);
            }

            Json res = Json.map();
            res.set("status", "ok");
            res.set("files", files);

            events().send("pdfResponse", res, request.getFunctionId());

        } catch (IOException e) {
            logger.info("Error to load file id " + fileId + ". " + e.getMessage());
        }

        return Json.map().set("status", "ok");

    }


    @EndpointFunction(name = "_replaceHeaderAndFooter")
    public Json replaceHeaderAndFooter(FunctionRequest request) {

        Json resp = Json.map();
        Json body = request.getJsonParams();

        Executors.newSingleThreadScheduledExecutor().execute(() -> {

            String generatedFilePath = null;
            PdfHeaderFooterHandler handler = new PdfHeaderFooterHandler();

            Json settings = body.json("settings");
            Json header = settings.json("header");
            Json footer = settings.json("footer");

            DownloadedFile pdf = files().download(body.string("fileId"));

            if (header != null && header.string(IMAGE_ID) != null || footer != null && footer.string(IMAGE_ID) != null) {

                InputStream headerIs = null;
                if (header.string(IMAGE_ID) != null) {
                    headerIs = files().download(header.string(IMAGE_ID)).getFile();
                }

                InputStream footerIs = null;
                if (footer.string(IMAGE_ID) != null) {
                    footerIs = files().download(footer.string(IMAGE_ID)).getFile();
                }

                generatedFilePath = handler.replaceHeaderAndFooterFromImages(pdf.getFile(), headerIs, footerIs, settings);

            } else if (header != null && header.string(HTML) != null || footer != null && footer.string(HTML) != null) {
                generatedFilePath = handler.replaceHeaderAndFooterFromTemplate(pdf.getFile(), settings);
            }

            if (generatedFilePath != null) {

                try {
                    InputStream is = new FileInputStream(generatedFilePath);
                    Json fileJson = files().upload("new-file-" + Strings.randomUUIDString(), is, "application/pdf");
                    handler.cleanGeneratedFiles();

                    Json res = Json.map();
                    res.set("status", "ok");
                    res.set("file", fileJson);

                    events().send("pdfResponse", res, request.getFunctionId());

                } catch (IOException ioe) {
                    logger.warn("Can not get generated file");
                }


            } else {

                Json res = Json.map();
                res.set("status", "error");
                res.set("message", "Should set images or templates for header and footer");

                events().send("pdfResponse", res, request.getFunctionId());
            }
        });

        resp.set("status", "ok");
        return resp;
    }

    private void createPdf(FunctionRequest req) {
        createPdf(req, true);
    }

    private void createPdf(FunctionRequest req, boolean retry) {
        logger.info("Creating pdf file");
        Json res = Json.map();
        InputStream is = null;
        try {
            Json data = req.getJsonParams();
            String template = data.string("tpl");
            Json settings = data.json("settings");
            PdfEngine pdfEngine = new PdfEngine(template, settings);
            is = pdfEngine.getPDF();
            if (is != null) {
                try {
                    logger.info("Uploading file to endpoint services");
                    Json fileJson = files().upload(pdfEngine.getFileName(), is, "application/pdf");
                    logger.info("Done uploading file to endpoint services");
                    res.set("status", "ok");
                    res.set("file", fileJson);
                } catch (Exception e) {
                    logger.error("Problems uploading file to endpoint services", e);
                    if (retry) {
                        logger.info("Retrying uploading");
                        createPdf(req, false);
                    }
                } finally {
                    pdfEngine.cleanTmpFiles();
                    try {
                        is.close();
                    } catch (IOException io) {
                        logger.info("Can not close stream", io);
                    }
                }
            } else {
                logger.warn("PDF file can not be generated");
                res.set("status", "error");
                res.set("message", EndpointException.json(ErrorCode.GENERAL, "PDF file was not generated."));
            }
        } catch (Exception ex) {
            logger.info("Failed to generate PDF", ex);
            res.set("status", "error");
            res.set("message", EndpointException.json(ErrorCode.GENERAL, "Failed to generate PDF: " + ex.getMessage(), ex));
        }
        logger.info("Pdf has been successfully created. Sending [pdfResponse] event to the app");
        events().send("pdfResponse", res, req.getFunctionId());
        logger.info("Done sending [pdfResponse] event to the app");
    }

    @EndpointFunction(name = "_replaceImages")
    public Json replaceImages(FunctionRequest request) {

        Json data = request.getJsonParams();

        Executors.newSingleThreadScheduledExecutor().execute(() -> {

            String requestId = request.getFunctionId();
            String fileId = data.string("fileId");
            Json res = Json.map();
            try {

                if (data.contains("settings")) {

                    InputStream is = files().download(fileId).getFile();
                    PDDocument pdf = PDDocument.load(is);

                    Json settings = data.json("settings");

                    if (settings.contains("images")) {
                        List<Json> settingsImages = settings.jsons("images");

                        for (Json image : settingsImages) {
                            if (image.contains("index") && image.contains("fileId")) {
                                int index = image.integer("index");
                                replaceImageInPdf(pdf, image.string("fileId"), index);
                            }
                        }
                    }

                    File temp = File.createTempFile("pdf-images-" + new Date().getTime(), ".pdf");
                    pdf.save(temp);
                    pdf.close();

                    String fileName = getFileName("pdf", settings);
                    Json fileJson = files().upload(fileName, new FileInputStream(temp), "application/pdf");

                    res.set("status", "ok");
                    res.set("file", fileJson);

                    events().send("pdfResponse", res, requestId);
                } else {
                    events().send("pdfResponse", res, requestId);
                }

            } catch (IOException e) {

                appLogger.info("Can not generate PDF, I/O exception", e);

                res.set("status", "error");
                res.set("message", "Failed to create file");

                events().send("pdfResponse", res, requestId);
            }

        });

        return Json.map();
    }

    private static void copyInputStreamToFile(InputStream inputStream, File file)
            throws IOException {

        try (FileOutputStream outputStream = new FileOutputStream(file)) {

            int read;
            byte[] bytes = new byte[1024];

            while ((read = inputStream.read(bytes)) != -1) {
                outputStream.write(bytes, 0, read);
            }

        }
    }

    private void replaceImageInPdf(PDDocument pdf, String imageId, int index) {

        try {

            int indexInDocument = 0;

            if (pdf.getNumberOfPages() > 0) {

                Json imageMetadata = files().metadata(imageId);
                String extension = ".jpg";
                if (imageMetadata.contains("contentType") && imageMetadata.string("contentType").equals("image/png")) {
                    extension = ".png";
                }

                InputStream imageIs = files().download(imageId).getFile();
                File img = File.createTempFile("pdf-img-" + UUID.randomUUID(), extension);
                copyInputStreamToFile(imageIs, img);

                PDResources resources = pdf.getPage(0).getResources();

                for (COSName xObjectName : resources.getXObjectNames()) {
                    PDXObject xObject = resources.getXObject(xObjectName);
                    if (xObject instanceof PDImageXObject) {

                        if (indexInDocument == index) {
                            PDImageXObject replacement_img = PDImageXObject.createFromFile(img.getPath(), pdf);
                            resources.put(xObjectName, replacement_img);
                            return;
                        }
                        indexInDocument++;
                    }
                }

                appLogger.info(String.format("Image not found for index [%s]", index));
            }

        } catch (IOException e) {
            appLogger.info("Can not when replace image", e);
        }
    }


    @EndpointFunction(name = "_addImages")
    public void addImages(FunctionRequest request) {

        Json data = request.getJsonParams();

        Executors.newSingleThreadScheduledExecutor().execute(() -> {

            String requestId = request.getFunctionId();
            String fileId = data.string("fileId");
            Json res = Json.map();

            try {
                InputStream is = files().download(fileId).getFile();
                PDDocument pdf = PDDocument.load(is);

                Json settings = data.json("settings");

                if (settings.contains("images")) {
                    List<Json> settingsImages = settings.jsons("images");

                    for (Json image : settingsImages) {
                        if (image.contains("pageIndex") && image.contains("fileId")) {

                            int pageIndex = image.integer("pageIndex");

                            if (pageIndex < pdf.getNumberOfPages()) {

                                String imageId = image.string("fileId");

                                PDPage page = pdf.getPage(pageIndex);

                                DownloadedFile downloadedFile = files().download(imageId);
                                InputStream imageIs = downloadedFile.getFile();

                                Json imageMetadata = files().metadata(imageId);
                                String extension = ".jpg";
                                if (imageMetadata.contains("contentType") && imageMetadata.string("contentType").equals("image/png")) {
                                    extension = ".png";
                                }

                                File img = File.createTempFile("pdf-img-" + UUID.randomUUID(), extension);
                                copyInputStreamToFile(imageIs, img);

                                PDImageXObject pdImage = PDImageXObject.createFromFileByContent(img, pdf);
                                PDPageContentStream contentStream = new PDPageContentStream(pdf, page, PDPageContentStream.AppendMode.APPEND, true);

                                int x = image.contains("x") ? image.integer("x") : 20;
                                int y = image.contains("y") ? image.integer("y") : 20;
                                int width = image.contains("width") ? image.integer("width") : 100;
                                int height = image.contains("height") ? image.integer("height") : 100;

                                contentStream.drawImage(pdImage, x, y, width, height);
                                contentStream.close();

                            }
                        }
                    }
                }

                String fileName = getFileName("pdf", settings);
                File temp = File.createTempFile(fileName, ".pdf");
                pdf.save(temp);
                pdf.close();

                Json fileJson = files().upload(fileName, new FileInputStream(temp), "application/pdf");

                res.set("status", "ok");
                res.set("file", fileJson);

                events().send("pdfResponse", res, requestId);
            } catch (IOException e) {

                appLogger.info("Can not generate PDF, I/O exception", e);
                res.set("status", "error");
                res.set("message", "Failed to create file");

                events().send("pdfResponse", res, requestId);
            }

        });
    }

    private final ReentrantLock pdfLock = new ReentrantLock();

    private void generateAutoPdf() {
        while (QueuePdf.getStreamInstance().getTotalSize() > 0) {
            createPdf(QueuePdf.getStreamInstance().poll());
        }
    }

    private String getFileName(String prefix, Json settings) {
        String fileName = prefix + "-" + new Date().getTime();
        if (settings.contains("name") && StringUtils.isNotBlank(settings.string("name"))) {
            fileName = settings.string("name");
        }
        return fileName;
    }

}

package io.slingr.endpoints.pdfGenerator;

import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import io.slingr.endpoints.Endpoint;
import io.slingr.endpoints.exceptions.EndpointException;
import io.slingr.endpoints.exceptions.ErrorCode;
import io.slingr.endpoints.framework.annotations.EndpointFunction;
import io.slingr.endpoints.framework.annotations.SlingrEndpoint;
import io.slingr.endpoints.services.rest.DownloadedFile;
import io.slingr.endpoints.utils.Json;
import io.slingr.endpoints.utils.Strings;
import io.slingr.endpoints.ws.exchange.FunctionRequest;
import org.apache.commons.lang.StringUtils;
import org.apache.pdfbox.multipdf.PDFMergerUtility;
import org.apache.pdfbox.multipdf.Splitter;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.ArrayList;
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


    public void endpointStarted() {

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

        Json data = request.getJsonParams();

        Json resp = Json.map();
        logger.info("Creating pdf from template");

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
            throw EndpointException.permanent(ErrorCode.GENERAL, "Failed to create file", e);
        } catch (TemplateException e) {
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

        Json res = Json.map();
        InputStream is = null;

        try {

            Json data = req.getJsonParams();

            String template = data.string("tpl");
            Json settings = data.json("settings");

            PdfEngine pdfEngine = new PdfEngine(template, settings);
            is = pdfEngine.getPDF();

            if (is != null) {
                Json fileJson = files().upload(pdfEngine.getFileName(), is, "application/pdf");
                pdfEngine.cleanTmpFiles();
                res.set("status", "ok");
                res.set("file", fileJson);
            } else {
                res.set("status", "error");
                res.set("message", EndpointException.json(ErrorCode.GENERAL, "PDF file was not generated."));
            }


        } catch (Exception ex) {

            res.set("status", "error");
            res.set("message", EndpointException.json(ErrorCode.GENERAL, "Failed to generate PDF: " + ex.getMessage(), ex));

        } finally {

            if (is != null) {
                try {
                    is.close();
                } catch (IOException io) {
                    logger.info("Close was failed");
                }

            }

        }

        events().send("pdfResponse", res, req.getFunctionId());

    }

    private final ReentrantLock pdfLock = new ReentrantLock();

    private void generateAutoPdf() {
        pdfLock.lock();

        try {

            while (QueuePdf.getStreamInstance().getTotalSize() > 0) {
                createPdf(QueuePdf.getStreamInstance().poll());
            }

        } finally {
            pdfLock.unlock();
        }
    }
}

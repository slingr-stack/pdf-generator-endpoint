package io.slingr.endpoints.pdfGenerator;

import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import io.slingr.endpoints.Endpoint;
import io.slingr.endpoints.exceptions.EndpointException;
import io.slingr.endpoints.exceptions.ErrorCode;
import io.slingr.endpoints.framework.annotations.ApplicationLogger;
import io.slingr.endpoints.framework.annotations.EndpointFunction;
import io.slingr.endpoints.framework.annotations.EndpointProperty;
import io.slingr.endpoints.framework.annotations.SlingrEndpoint;
import io.slingr.endpoints.pdfGenerator.workers.*;
import io.slingr.endpoints.services.AppLogs;
import io.slingr.endpoints.utils.Json;
import io.slingr.endpoints.ws.exchange.FunctionRequest;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.ReentrantLock;

/**
 * <p>PDF Generator endpoint
 * <p>
 * <p>Created by hpacini on 24/10/17.
 */
@SlingrEndpoint(name = "pdf-generator", functionPrefix = "_")
public class PdfGenerator extends Endpoint {

    private Logger logger = LoggerFactory.getLogger(PdfGenerator.class);

    @ApplicationLogger
    protected AppLogs appLogger;

    @EndpointProperty
    private String maxThreadPool;

    private final int MAX_THREADS_POOL = 3;

    protected ExecutorService executorService;

    public void endpointStarted() {

        int maxTreads = MAX_THREADS_POOL;
        try {
            maxTreads = Integer.valueOf(maxThreadPool);
        } catch (Exception ex) {
        }

        this.executorService = Executors.newFixedThreadPool(maxTreads);

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
        FillFormWorker worker = new FillFormWorker(events(), files(), appLogger, request);
        this.executorService.submit(worker);
        return Json.map();
    }

    @EndpointFunction(name = "_mergeDocuments")
    public Json mergeDocuments(FunctionRequest request) {
        MergeDocumentsWorker worker = new MergeDocumentsWorker(events(), files(), appLogger, request);
        this.executorService.submit(worker);
        return Json.map().set("status", "ok");
    }

    @EndpointFunction(name = "_splitDocument")
    public Json splitDocument(FunctionRequest request) {
        SplitDocumentWorker worker = new SplitDocumentWorker(events(), files(), appLogger, request);
        this.executorService.submit(worker);
        return Json.map().set("status", "ok");
    }

    @EndpointFunction(name = "_replaceHeaderAndFooter")
    public Json replaceHeaderAndFooter(FunctionRequest request) {
        ReplaceHeaderAndFooterWorker worker = new ReplaceHeaderAndFooterWorker(events(), files(), appLogger, request);
        this.executorService.submit(worker);
        return Json.map().set("status", "ok");
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
        ReplaceImagesWorker worker = new ReplaceImagesWorker(events(), files(), appLogger, request);
        this.executorService.submit(worker);
        return Json.map();
    }

    @EndpointFunction(name = "_addImages")
    public Json addImages(FunctionRequest request) {
        AddImagesWorker worker = new AddImagesWorker(events(), files(), appLogger, request);
        this.executorService.submit(worker);
        return Json.map();
    }

    private final ReentrantLock pdfLock = new ReentrantLock();

    private void generateAutoPdf() {
        while (QueuePdf.getStreamInstance().getTotalSize() > 0) {
            createPdf(QueuePdf.getStreamInstance().poll());
        }
    }

}

package io.slingr.endpoints.pdfGenerator.workers;

import io.slingr.endpoints.services.AppLogs;
import io.slingr.endpoints.services.Events;
import io.slingr.endpoints.services.Files;
import io.slingr.endpoints.services.rest.DownloadedFile;
import io.slingr.endpoints.utils.Json;
import io.slingr.endpoints.ws.exchange.FunctionRequest;
import org.apache.pdfbox.multipdf.PDFMergerUtility;
import org.apache.pdfbox.multipdf.Splitter;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

public class MergeDocumentsWorker extends PdfWorker {

    private Logger logger = LoggerFactory.getLogger(MergeDocumentsWorker.class);

    public MergeDocumentsWorker(Events events, Files files, AppLogs appLogger, FunctionRequest request) {
        super(events, files, appLogger, request);
    }

    @Override
    public void run() {

        Json data = request.getJsonParams();
        Json docs = null;
        if (data != null && data.contains("documents")) {
            docs = data.json("documents");
        }

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
                    DownloadedFile downloadedFile = files.download(fileId);
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
                Json fileJson = files.upload(temp.getName(), new FileInputStream(temp), "application/pdf");

                if (!isError) {
                    res.set("status", "ok");
                    res.set("file", fileJson);
                } else {
                    res.set("status", "error");
                    res.set("message", errorMessage);
                }

                events.send("pdfResponse", res, request.getFunctionId());

            } catch (IOException e) {
                logger.info("Error to create merged file. " + e.getMessage());
            }

        } else {
            Json res = Json.map();
            res.set("status", "error");
            res.set("message", "The property documents should be a valid list.");
            events.send("pdfResponse", res, request.getFunctionId());
        }

    }
}

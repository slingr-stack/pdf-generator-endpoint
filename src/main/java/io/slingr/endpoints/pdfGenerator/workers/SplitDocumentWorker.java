package io.slingr.endpoints.pdfGenerator.workers;

import io.slingr.endpoints.exceptions.EndpointException;
import io.slingr.endpoints.exceptions.ErrorCode;
import io.slingr.endpoints.services.AppLogs;
import io.slingr.endpoints.services.Events;
import io.slingr.endpoints.services.Files;
import io.slingr.endpoints.utils.Json;
import io.slingr.endpoints.ws.exchange.FunctionRequest;
import org.apache.commons.lang.StringUtils;
import org.apache.pdfbox.multipdf.PDFMergerUtility;
import org.apache.pdfbox.multipdf.Splitter;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;


public class SplitDocumentWorker extends PdfWorker {

    private Logger logger = LoggerFactory.getLogger(SplitDocumentWorker.class);

    public SplitDocumentWorker(Events events, Files files, AppLogs appLogger, FunctionRequest request) {
        super(events, files, appLogger, request);
    }

    @Override
    public void run() {

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

            InputStream is = files.download(fileId).getFile();
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

            Json splitFiles = Json.list();

            for (File doc : documents) {
                Json fileJson = files.upload(doc.getName(), new FileInputStream(doc), "application/pdf");
                splitFiles.push(fileJson);
            }

            Json res = Json.map();
            res.set("status", "ok");
            res.set("files", splitFiles);

            events.send("pdfResponse", res, request.getFunctionId());

        } catch (IOException e) {
            logger.info("Error to load file id " + fileId + ". " + e.getMessage());
        }
    }
}

package io.slingr.endpoints.pdfGenerator.workers;

import io.slingr.endpoints.pdfGenerator.PdfFilesUtils;
import io.slingr.endpoints.services.AppLogs;
import io.slingr.endpoints.services.Events;
import io.slingr.endpoints.services.Files;
import io.slingr.endpoints.utils.Json;
import io.slingr.endpoints.ws.exchange.FunctionRequest;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.List;

public class ReplaceImagesWorker extends PdfImageWorker {

    private Logger logger = LoggerFactory.getLogger(ReplaceImagesWorker.class);

    public ReplaceImagesWorker(Events events, Files files, AppLogs appLogger, FunctionRequest request) {
        super(events, files, appLogger, request);
    }

    @Override
    public void run() {

        Json data = request.getJsonParams();

        String requestId = request.getFunctionId();
        String fileId = data.string("fileId");
        Json res = Json.map();
        try {

            if (data.contains("settings")) {

                InputStream is = files.download(fileId).getFile();
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

                String fileName = PdfFilesUtils.getFileName("pdf", settings);
                Json fileJson = files.upload(fileName, new FileInputStream(temp), "application/pdf");

                res.set("status", "ok");
                res.set("file", fileJson);

                events.send("pdfResponse", res, requestId);
            } else {
                events.send("pdfResponse", res, requestId);
            }

        } catch (IOException e) {

            appLogger.info("Can not generate PDF, I/O exception", e);

            res.set("status", "error");
            res.set("message", "Failed to create file");

            events.send("pdfResponse", res, requestId);
        }

    }
}

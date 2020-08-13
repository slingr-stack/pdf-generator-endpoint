package io.slingr.endpoints.pdfGenerator.workers;

import io.slingr.endpoints.services.AppLogs;
import io.slingr.endpoints.services.Events;
import io.slingr.endpoints.services.Files;
import io.slingr.endpoints.utils.Json;
import io.slingr.endpoints.ws.exchange.FunctionRequest;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDResources;
import org.apache.pdfbox.pdmodel.graphics.PDXObject;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

public abstract class PdfImageWorker extends PdfWorker {

    public PdfImageWorker(Events events, Files files, AppLogs appLogger, FunctionRequest request) {
        super(events, files, appLogger, request);
    }

    protected static void copyInputStreamToFile(InputStream inputStream, File file)
            throws IOException {

        try (FileOutputStream outputStream = new FileOutputStream(file)) {

            int read;
            byte[] bytes = new byte[1024];

            while ((read = inputStream.read(bytes)) != -1) {
                outputStream.write(bytes, 0, read);
            }

        }
    }

    protected void replaceImageInPdf(PDDocument pdf, String imageId, int index) {

        try {

            int indexInDocument = 0;

            if (pdf.getNumberOfPages() > 0) {

                Json imageMetadata = files.metadata(imageId);
                String extension = ".jpg";
                if (imageMetadata.contains("contentType") && imageMetadata.string("contentType").equals("image/png")) {
                    extension = ".png";
                }

                InputStream imageIs = files.download(imageId).getFile();
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
}

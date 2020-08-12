package io.slingr.endpoints.pdfGenerator.workers;

import io.slingr.endpoints.pdfGenerator.PdfFillForm;
import io.slingr.endpoints.services.AppLogs;
import io.slingr.endpoints.services.Events;
import io.slingr.endpoints.services.Files;
import io.slingr.endpoints.ws.exchange.FunctionRequest;

public abstract class PdfWorker implements Runnable {
    protected Events events;
    protected Files files;
    protected PdfFillForm pdfFillForm;
    protected AppLogs appLogger;
    protected FunctionRequest request;

    PdfWorker(Events events, Files files, AppLogs appLogger, FunctionRequest request) {
        this.events = events;
        this.files = files;
        this.appLogger = appLogger;
        this.request = request;
    }
}

package io.slingr.endpoints.pdfGenerator;

import com.itextpdf.forms.PdfAcroForm;
import com.itextpdf.forms.fields.PdfFormField;
import com.itextpdf.io.font.PdfEncodings;
import com.itextpdf.kernel.colors.Color;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfReader;
import com.itextpdf.kernel.pdf.PdfWriter;
import io.slingr.endpoints.services.AppLogs;
import io.slingr.endpoints.services.Files;
import io.slingr.endpoints.utils.Json;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class PdfFillForm {

    private Logger logger = LoggerFactory.getLogger(PdfFillForm.class);

    private Map<String, String> fonts;
    private AppLogs appLogger;

    public PdfFillForm(AppLogs appLogger) {
        fonts = new HashMap<>();
        this.appLogger = appLogger;
    }


    public File fillForm(Files files, String pdfFileId, Json settings) throws IOException {
        appLogger.info(String.format("Filling up form [%s]", pdfFileId));

        InputStream is = null;
        PdfDocument pdfDoc = null;
        File tmp = null;
        try {
            appLogger.info(String.format("Downloading form [%s]", pdfFileId));
            is = files.download(pdfFileId).getFile();
            appLogger.info(String.format("Done downloading form [%s]", pdfFileId));

            tmp = File.createTempFile("pdf-filled-" + new Date().getTime(), ".pdf");
            PdfWriter desPdf = new PdfWriter(tmp);
            PdfReader srcPdf = new PdfReader(is);

            pdfDoc = new PdfDocument(srcPdf, desPdf);
            PdfAcroForm form = PdfAcroForm.getAcroForm(pdfDoc, true);
            form.setGenerateAppearance(true);

            if (settings.contains("data")) {
                Json settingsData = settings.json("data");
                for (String givenFormField : settingsData.keys()) {
                    PdfFormField formField = form.getField(givenFormField);
                    if (formField != null) {
                        if (settingsData.object(givenFormField) instanceof String) {
                            formField.setValue(settingsData.string(givenFormField));
                        } else {
                            Json fieldSettings = settingsData.json(givenFormField);
                            if (fieldSettings != null) {
                                if (fieldSettings.contains("fontFileId")) {
                                    String fontFileId = fieldSettings.string("fontFileId");
                                    String font = fonts.get(fontFileId);
                                    if (font == null) {
                                        InputStream fontIs = null;
                                        try {
                                            appLogger.info(String.format("Downloading font [%s]", fontFileId));
                                            fontIs = files.download(fontFileId).getFile();
                                            File tmpFont = File.createTempFile("font", ".ttf");
                                            FileUtils.copyInputStreamToFile(fontIs, tmpFont);
                                            font = tmpFont.getPath();
                                            fonts.put(fontFileId, font);
                                            appLogger.info(String.format("Done downloading font [%s]", fontFileId));
                                        } catch (Exception ex) {
                                            appLogger.error("Can not copy font. ", ex);
                                        } finally {

                                            try {
                                                if (fontIs != null) {
                                                    fontIs.close();
                                                }

                                            } catch (IOException ioe) {
                                                appLogger.error("Can not close font. ", ioe);
                                            }
                                        }
                                    }
                                    if (font != null) {
                                        PdfFont pdfFont = PdfFontFactory.createFont(font, PdfEncodings.IDENTITY_H);
                                        formField.setFont(pdfFont);
                                    } else {
                                        appLogger.error(String.format("Can not find font for %s", fontFileId));
                                    }
                                }
                                if (fieldSettings.contains("value")) {
                                    formField.setValue(fieldSettings.string("value"));
                                }
                                if (fieldSettings.contains("textSize")) {
                                    formField.setFontSize(fieldSettings.integer("textSize"));
                                }
                                if (fieldSettings.contains("backgroundColor")) {
                                    formField.setBackgroundColor(hex2Rgb(fieldSettings.string("backgroundColor")));
                                }
                                if (fieldSettings.contains("textColor")) {
                                    formField.setColor(hex2Rgb(fieldSettings.string("textColor")));
                                }
                                if (fieldSettings.contains("textAlignment")) {
                                    int textAlign = PdfFormField.ALIGN_LEFT;
                                    if ("CENTER".equals(fieldSettings.string("textAlignment"))) {
                                        textAlign = PdfFormField.ALIGN_CENTER;
                                    } else if ("RIGHT".equals(fieldSettings.string("textAlignment"))) {
                                        textAlign = PdfFormField.ALIGN_RIGHT;
                                    }
                                    formField.setJustification(textAlign);
                                }

                                boolean readOnly = false;
                                if (fieldSettings.contains("readOnly")) {
                                    readOnly = fieldSettings.bool("readOnly");
                                }
                                formField.setReadOnly(readOnly);

                            }
                        }
                    } else {
                        appLogger.info(String.format("Can not find field %s for pdf file %s", givenFormField, pdfFileId));
                    }
                }
            }

            appLogger.info(String.format("Form [%s] was filled up successfully", pdfFileId));

            return tmp;
        } catch (Exception ex) {
            appLogger.error(String.format("Can not fill pdf file [%s]: " + ex.getMessage(), pdfFileId), ex);
            logger.error(String.format("Can not fill pdf file [%s]", pdfFileId), ex);
        } finally {
            try {
                if (is != null) {
                    is.close();
                }
                if (pdfDoc != null) {
                    pdfDoc.close();
                }
            } catch (IOException ioe) {
                appLogger.error("Can not close PDF document. ", ioe);
            }
        }
        return null;

    }

    private Color hex2Rgb(String colorStr) {
        StringUtils.replace(colorStr, "#", "");
        return new DeviceRgb(
                Integer.valueOf(colorStr.substring(1, 3), 16),
                Integer.valueOf(colorStr.substring(3, 5), 16),
                Integer.valueOf(colorStr.substring(5, 7), 16));
    }

}

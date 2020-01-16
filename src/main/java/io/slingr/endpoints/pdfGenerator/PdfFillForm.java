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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

public class PdfFillForm {

    private Map<String, String> fonts;
    private AppLogs appLogger;

    public PdfFillForm(AppLogs appLogger) {
        fonts = new HashMap<>();
        this.appLogger = appLogger;
    }


    public File fillForm(Files files, String pdfFileId, Json settings) throws IOException {

        InputStream is = files.download(pdfFileId).getFile();

        File tmp = File.createTempFile("pdf-tmp", ".pdf");

        PdfWriter desPdf = new PdfWriter(tmp);
        PdfReader srcPdf = new PdfReader(is);

        PdfDocument pdfDoc = new PdfDocument(srcPdf, desPdf);

        PdfAcroForm form = PdfAcroForm.getAcroForm(pdfDoc, true);
        form.setGenerateAppearance(true);

        if (settings.contains("data")) {
            Json settingsData = settings.json("data");

            for (String givenFormField : settingsData.keys()) {

                PdfFormField formField = form.getField(givenFormField);
                if (formField != null) {

                    Json fieldSettings = settingsData.json(givenFormField);
                    if (fieldSettings != null) {

                        if (fieldSettings.contains("fontFileId")) {

                            String fontFileId = fieldSettings.string("fontFileId");
                            String font = fonts.get(fontFileId);
                            if (font == null) {
                                InputStream fontIs = files.download(fontFileId).getFile();
                                File tmpFont = File.createTempFile("font", ".ttf");
                                FileUtils.copyInputStreamToFile(fontIs, tmpFont);
                                font = tmpFont.getPath();
                                fonts.put(fontFileId, font);
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

                } else {
                    appLogger.info(String.format("Can not find field %s for pdf file %s", givenFormField, pdfFileId));
                }
            }
        }

        pdfDoc.close();

        return tmp;

    }

    private Color hex2Rgb(String colorStr) {
        StringUtils.replace(colorStr, "#", "");
        return new DeviceRgb(
                Integer.valueOf(colorStr.substring(1, 3), 16),
                Integer.valueOf(colorStr.substring(3, 5), 16),
                Integer.valueOf(colorStr.substring(5, 7), 16));
    }

}

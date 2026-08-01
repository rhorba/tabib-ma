package com.tabibma.prescription;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Renders a plain, deterministic single-page PDF — no vendor/template dependency, just PDFBox
 * (Architecture doc §3 PrescriptionPdfGenerator). Content only, no digital signature: "signed" in
 * this codebase means "immutable record with a signedAt timestamp" (see Prescription), not a
 * PKI/PAdES cryptographic signature — that's out of scope until a real requirement for one exists.
 */
@Component
public class PrescriptionPdfGenerator {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")
            .withZone(ZoneOffset.UTC);

    public byte[] generate(String doctorName, String doctorSpecialty, String patientName, Instant signedAt,
                            List<PrescriptionItem> items) {
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);
            PDFont regular = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
            PDFont bold = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);

            try (PDPageContentStream stream = new PDPageContentStream(document, page)) {
                float margin = 50;
                float y = page.getMediaBox().getHeight() - margin;
                float leading = 18;

                y = writeLine(stream, bold, 16, margin, y, "tabib.ma — E-Prescription");
                y -= leading;
                y = writeLine(stream, regular, 11, margin, y, "Doctor: " + doctorName + " (" + doctorSpecialty + ")");
                y = writeLine(stream, regular, 11, margin, y, "Patient: " + patientName);
                y = writeLine(stream, regular, 11, margin, y, "Issued: " + DATE_FORMAT.format(signedAt));
                y -= leading;
                y = writeLine(stream, bold, 12, margin, y, "Medications");
                y -= 4;

                for (PrescriptionItem item : items) {
                    y = writeLine(stream, bold, 11, margin, y, "- " + item.getMedicationName() + " — " + item.getDosage());
                    if (item.getInstructions() != null && !item.getInstructions().isBlank()) {
                        y = writeLine(stream, regular, 10, margin + 14, y, item.getInstructions());
                    }
                }
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            document.save(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to generate prescription PDF.", e);
        }
    }

    private float writeLine(PDPageContentStream stream, PDFont font, float fontSize, float x, float y, String text)
            throws IOException {
        stream.beginText();
        stream.setFont(font, fontSize);
        stream.newLineAtOffset(x, y);
        stream.showText(text);
        stream.endText();
        return y - fontSize - 6;
    }
}

package com.tabibma.prescription;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PrescriptionPdfGeneratorTest {

    private final PrescriptionPdfGenerator generator = new PrescriptionPdfGenerator();

    @Test
    void generate_producesAOnePageValidPdfContainingTheDoctorPatientAndMedications() throws IOException {
        List<PrescriptionItem> items = List.of(
                new PrescriptionItem("Amoxicillin", "500mg", "3 times a day for 7 days"),
                new PrescriptionItem("Paracetamol", "1000mg", null));

        byte[] pdf = generator.generate("Dr. Amina Tazi", "Cardiology", "Youssef Alami", Instant.now(), items);

        assertThat(pdf).isNotEmpty();
        try (PDDocument document = Loader.loadPDF(pdf)) {
            assertThat(document.getNumberOfPages()).isEqualTo(1);
            String text = new PDFTextStripper().getText(document);
            assertThat(text).contains("Dr. Amina Tazi", "Cardiology", "Youssef Alami",
                    "Amoxicillin", "500mg", "3 times a day for 7 days", "Paracetamol", "1000mg");
        }
    }
}

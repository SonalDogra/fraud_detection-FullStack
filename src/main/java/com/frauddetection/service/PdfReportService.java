package com.frauddetection.service;

import com.frauddetection.model.Transaction;
import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.borders.Border;
import com.itextpdf.layout.borders.SolidBorder;
import com.itextpdf.layout.element.*;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.time.format.DateTimeFormatter;

@Service
public class PdfReportService {

    private static final DeviceRgb BG_DARK     = new DeviceRgb(10, 13, 20);
    private static final DeviceRgb BG_CARD     = new DeviceRgb(20, 27, 45);
    private static final DeviceRgb BG_SECTION  = new DeviceRgb(17, 24, 39);
    private static final DeviceRgb ACCENT_BLUE = new DeviceRgb(59, 130, 246);
    private static final DeviceRgb TEXT_PRIMARY   = new DeviceRgb(226, 232, 240);
    private static final DeviceRgb TEXT_SECONDARY = new DeviceRgb(136, 146, 164);
    private static final DeviceRgb RISK_LOW      = new DeviceRgb(16, 185, 129);
    private static final DeviceRgb RISK_MEDIUM   = new DeviceRgb(245, 158, 11);
    private static final DeviceRgb RISK_HIGH     = new DeviceRgb(239, 68, 68);
    private static final DeviceRgb RISK_CRITICAL = new DeviceRgb(220, 38, 38);
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm:ss");

    public byte[] generateReport(Transaction tx) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PdfWriter writer = new PdfWriter(baos);
        PdfDocument pdf = new PdfDocument(writer);
        Document doc = new Document(pdf, PageSize.A4);
        doc.setMargins(40, 40, 40, 40);
        doc.setBackgroundColor(BG_DARK);

        PdfFont bold   = PdfFontFactory.createFont(com.itextpdf.io.font.constants.StandardFonts.HELVETICA_BOLD);
        PdfFont normal = PdfFontFactory.createFont(com.itextpdf.io.font.constants.StandardFonts.HELVETICA);

        // ── Header ──
        Table header = new Table(UnitValue.createPercentArray(new float[]{1, 2})).useAllAvailableWidth();
        header.setBorder(Border.NO_BORDER);

        Cell brandCell = new Cell().setBorder(Border.NO_BORDER).setBackgroundColor(BG_CARD).setPadding(16);
        brandCell.add(new Paragraph("FraudGuard AI").setFont(bold).setFontSize(18).setFontColor(TEXT_PRIMARY));
        brandCell.add(new Paragraph("Investigation Report").setFont(normal).setFontSize(10).setFontColor(TEXT_SECONDARY));
        header.addCell(brandCell);

        Cell txCell = new Cell().setBorder(Border.NO_BORDER).setBackgroundColor(BG_CARD).setPadding(16).setTextAlignment(TextAlignment.RIGHT);
        txCell.add(new Paragraph(tx.getTransactionId()).setFont(bold).setFontSize(14).setFontColor(ACCENT_BLUE));
        txCell.add(new Paragraph("Generated: " + java.time.LocalDateTime.now().format(FMT))
                .setFont(normal).setFontSize(8).setFontColor(TEXT_SECONDARY));
        header.addCell(txCell);
        doc.add(header);
        doc.add(new Paragraph(" "));

        // ── Risk Score Banner ──
        DeviceRgb riskColor = getRiskColor(tx.getRiskLevel());
        Table riskBanner = new Table(UnitValue.createPercentArray(new float[]{1, 1, 1, 1})).useAllAvailableWidth();
        riskBanner.setBorder(new SolidBorder(riskColor, 1));
        riskBanner.setBackgroundColor(BG_CARD);

        riskBanner.addCell(scoreCell("RISK SCORE", tx.getRiskScore() + " / 100", riskColor, bold, normal));
        riskBanner.addCell(scoreCell("RISK LEVEL", tx.getRiskLevel() != null ? tx.getRiskLevel().name() : "UNKNOWN", riskColor, bold, normal));
        riskBanner.addCell(scoreCell("CONFIDENCE", tx.getConfidence() != null ? String.format("%.0f%%", tx.getConfidence() * 100) : "N/A", ACCENT_BLUE, bold, normal));
        riskBanner.addCell(scoreCell("STATUS", tx.getStatus() != null ? tx.getStatus().name() : "UNKNOWN", TEXT_SECONDARY, bold, normal));
        doc.add(riskBanner);
        doc.add(new Paragraph(" "));

        // ── Recommended Action ──
        if (tx.getRecommendedAction() != null) {
            Table actionTable = new Table(1).useAllAvailableWidth();
            Cell actionCell = new Cell().setBackgroundColor(BG_SECTION)
                    .setBorder(new SolidBorder(ACCENT_BLUE, 2)).setPadding(12);
            actionCell.add(new Paragraph("RECOMMENDED ACTION").setFont(bold).setFontSize(8).setFontColor(TEXT_SECONDARY));
            actionCell.add(new Paragraph(tx.getRecommendedAction()).setFont(bold).setFontSize(10).setFontColor(TEXT_PRIMARY));
            actionTable.addCell(actionCell);
            doc.add(actionTable);
            doc.add(new Paragraph(" "));
        }

        // ── Two-column details ──
        Table details = new Table(UnitValue.createPercentArray(new float[]{1, 1})).useAllAvailableWidth();
        details.setBorder(Border.NO_BORDER);

        // Customer
        Cell custCard = new Cell().setBorder(Border.NO_BORDER).setPadding(0).setPaddingRight(8);
        custCard.add(sectionCard("CUSTOMER DETAILS",
                new String[]{"Name", "Customer ID", "Email"},
                new String[]{
                        nvl(tx.getCustomerName()),
                        nvl(tx.getCustomerId()),
                        nvl(tx.getCustomerEmail())
                }, bold, normal));
        details.addCell(custCard);

        // Merchant
        Cell merCard = new Cell().setBorder(Border.NO_BORDER).setPadding(0).setPaddingLeft(8);
        merCard.add(sectionCard("MERCHANT & TRANSACTION",
                new String[]{"Merchant", "Category", "Amount", "Currency"},
                new String[]{
                        nvl(tx.getMerchantName()),
                        nvl(tx.getMerchantCategory()),
                        tx.getAmount() != null ? tx.getAmount().toPlainString() : "N/A",
                        nvl(tx.getCurrency())
                }, bold, normal));
        details.addCell(merCard);

        doc.add(details);
        doc.add(new Paragraph(" "));

        // Device
        Table details2 = new Table(UnitValue.createPercentArray(new float[]{1, 1})).useAllAvailableWidth();
        details2.setBorder(Border.NO_BORDER);

        Cell devCard = new Cell().setBorder(Border.NO_BORDER).setPadding(0).setPaddingRight(8);
        devCard.add(sectionCard("DEVICE & LOCATION",
                new String[]{"Country", "City", "IP Address", "Device Type", "Browser", "OS"},
                new String[]{
                        nvl(tx.getCountry()), nvl(tx.getCity()),
                        nvl(tx.getIpAddress()), nvl(tx.getDeviceType()),
                        nvl(tx.getBrowser()), nvl(tx.getOs())
                }, bold, normal));
        details2.addCell(devCard);

        // Risk flags
        StringBuilder flags = new StringBuilder();
        if (tx.isVpn()) flags.append("✓ VPN/Proxy Detected\n");
        if (tx.isNewDevice()) flags.append("✓ New Device\n");
        if (tx.isHighRiskCountry()) flags.append("✓ High-Risk Country\n");
        if (tx.isForeignCurrency()) flags.append("✓ Foreign Currency\n");
        if (tx.isRoundAmount()) flags.append("✓ Round Amount\n");
        if (flags.isEmpty()) flags.append("No risk flags triggered");

        Cell flagCard = new Cell().setBorder(Border.NO_BORDER).setPadding(0).setPaddingLeft(8);
        flagCard.add(sectionCard("RISK FLAGS",
                new String[]{"Active Flags"},
                new String[]{flags.toString().trim()},
                bold, normal));
        details2.addCell(flagCard);
        doc.add(details2);
        doc.add(new Paragraph(" "));

        // ── Triggered Rules ──
        if (tx.getTriggeredRules() != null && !tx.getTriggeredRules().isBlank()) {
            doc.add(labeledSection("TRIGGERED FRAUD RULES", tx.getTriggeredRules(), riskColor, bold, normal));
            doc.add(new Paragraph(" "));
        }

        // ── Behaviour Findings ──
        if (tx.getBehaviourFindings() != null && !tx.getBehaviourFindings().isBlank()) {
            doc.add(labeledSection("BEHAVIOUR ANALYSIS", tx.getBehaviourFindings(), ACCENT_BLUE, bold, normal));
            doc.add(new Paragraph(" "));
        }

        // ── AI Investigation Report ──
        if (tx.getAiSummary() != null && !tx.getAiSummary().isBlank()) {
            Table aiTable = new Table(1).useAllAvailableWidth();
            Cell aiCell = new Cell().setBackgroundColor(BG_CARD)
                    .setBorder(new SolidBorder(ACCENT_BLUE, 1)).setPadding(14);
            aiCell.add(new Paragraph("AI INVESTIGATION REPORT")
                    .setFont(bold).setFontSize(8).setFontColor(ACCENT_BLUE).setMarginBottom(8));
            aiCell.add(new Paragraph(tx.getAiSummary())
                    .setFont(normal).setFontSize(9).setFontColor(TEXT_PRIMARY).setFixedLeading(14));
            aiTable.addCell(aiCell);
            doc.add(aiTable);
            doc.add(new Paragraph(" "));
        }

        // ── Footer ──
        Table footer = new Table(1).useAllAvailableWidth();
        Cell footerCell = new Cell().setBorder(new SolidBorder(BG_SECTION, 1))
                .setBackgroundColor(BG_SECTION).setPadding(10).setTextAlignment(TextAlignment.CENTER);
        footerCell.add(new Paragraph("FraudGuard AI — Confidential Investigation Report — " +
                (tx.getAnalysedAt() != null ? "Analysed: " + tx.getAnalysedAt().format(FMT) : ""))
                .setFont(normal).setFontSize(7).setFontColor(TEXT_SECONDARY));
        footerCell.add(new Paragraph("This report is generated by an AI-assisted system and must be reviewed by an authorised fraud analyst before any action is taken.")
                .setFont(normal).setFontSize(7).setFontColor(TEXT_SECONDARY));
        footer.addCell(footerCell);
        doc.add(footer);

        doc.close();
        return baos.toByteArray();
    }

    private Cell scoreCell(String label, String value, DeviceRgb valueColor, PdfFont bold, PdfFont normal) {
        Cell cell = new Cell().setBorder(Border.NO_BORDER).setBackgroundColor(BG_CARD).setPadding(14).setTextAlignment(TextAlignment.CENTER);
        cell.add(new Paragraph(label).setFont(normal).setFontSize(7).setFontColor(TEXT_SECONDARY).setMarginBottom(4));
        cell.add(new Paragraph(value).setFont(bold).setFontSize(14).setFontColor(valueColor));
        return cell;
    }

    private Table sectionCard(String title, String[] labels, String[] values, PdfFont bold, PdfFont normal) {
        Table t = new Table(1).useAllAvailableWidth();
        Cell outer = new Cell().setBorder(Border.NO_BORDER).setBackgroundColor(BG_CARD).setPadding(12);
        outer.add(new Paragraph(title).setFont(bold).setFontSize(8).setFontColor(TEXT_SECONDARY).setMarginBottom(8));
        for (int i = 0; i < labels.length; i++) {
            Table row = new Table(UnitValue.createPercentArray(new float[]{1, 1.5f})).useAllAvailableWidth();
            row.setBorder(Border.NO_BORDER);
            Cell lbl = new Cell().setBorder(Border.NO_BORDER).setPaddingBottom(4);
            lbl.add(new Paragraph(labels[i]).setFont(normal).setFontSize(8).setFontColor(TEXT_SECONDARY));
            Cell val = new Cell().setBorder(Border.NO_BORDER).setPaddingBottom(4);
            val.add(new Paragraph(values[i]).setFont(bold).setFontSize(8).setFontColor(TEXT_PRIMARY));
            row.addCell(lbl);
            row.addCell(val);
            outer.add(row);
        }
        t.addCell(outer);
        return t;
    }

    private Table labeledSection(String title, String content, DeviceRgb accentColor, PdfFont bold, PdfFont normal) {
        Table t = new Table(1).useAllAvailableWidth();
        Cell cell = new Cell().setBackgroundColor(BG_SECTION)
                .setBorder(new SolidBorder(accentColor, 1)).setPadding(12);
        cell.add(new Paragraph(title).setFont(bold).setFontSize(8).setFontColor(accentColor).setMarginBottom(6));
        cell.add(new Paragraph(content).setFont(normal).setFontSize(9).setFontColor(TEXT_PRIMARY).setFixedLeading(14));
        t.addCell(cell);
        return t;
    }

    private DeviceRgb getRiskColor(Transaction.RiskLevel level) {
        if (level == null) return TEXT_SECONDARY;
        return switch (level) {
            case CRITICAL -> RISK_CRITICAL;
            case HIGH     -> RISK_HIGH;
            case MEDIUM   -> RISK_MEDIUM;
            case LOW      -> RISK_LOW;
        };
    }

    private String nvl(String s) {
        return s != null && !s.isBlank() ? s : "—";
    }
}

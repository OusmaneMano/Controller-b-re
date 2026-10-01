package com.controller.service;

import com.controller.dto.RecordDTO;
import com.controller.entity.Membership;
import com.controller.entity.Record;
import com.controller.entity.TableColumn;
import com.controller.repository.TableColumnRepository;
import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Element;
import com.itextpdf.text.Font;
import com.itextpdf.text.PageSize;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.Phrase;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class PdfExportService {

    private final RecordService recordService;
    private final MembershipService membershipService;
    private final TableColumnRepository tableColumnRepository;

    public byte[] export(String email, Long companyId, Long employeeIdFilter,
                         LocalDateTime from, LocalDateTime to, Map<String, String> rawParams) {
        Membership m = membershipService.require(email, companyId);
        List<Record> rows = recordService.loadFiltered(m, employeeIdFilter, from, to, rawParams);
        RecordDTO.RecordListResponse summary = recordService.toListResponse(m, rows);
        List<TableColumn> columns = tableColumnRepository.findByCompanyIdOrderByOrderIndexAsc(m.getCompany().getId());

        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            Document doc = new Document(PageSize.A4.rotate(), 24, 24, 24, 24);
            PdfWriter.getInstance(doc, out);
            doc.open();
            Font title = new Font(Font.FontFamily.HELVETICA, 16, Font.BOLD);
            Font small = new Font(Font.FontFamily.HELVETICA, 9);
            Font head = new Font(Font.FontFamily.HELVETICA, 9, Font.BOLD);

            doc.add(new Paragraph(m.getCompany().getName(), title));
            doc.add(new Paragraph("Records export · " + LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME), small));
            if (summary.getTotalAmount() != null) {
                doc.add(new Paragraph("Total " + (summary.getAmountFieldName() != null ? summary.getAmountFieldName() : "Amount")
                        + ": " + summary.getTotalAmount() + "   ·   " + summary.getTotalCount() + " rows", small));
            } else {
                doc.add(new Paragraph(summary.getTotalCount() + " rows", small));
            }
            doc.add(new Paragraph(" ", small));

            int cols = Math.max(1, columns.size() + 2);
            PdfPTable table = new PdfPTable(cols);
            table.setWidthPercentage(100);
            addCell(table, "When", head);
            addCell(table, "By", head);
            for (TableColumn c : columns) addCell(table, c.getFieldName(), head);

            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
            for (Record r : rows) {
                addCell(table, r.getCreatedAt() != null ? r.getCreatedAt().format(fmt) : "", small);
                addCell(table, r.getCreatedBy().getFirstName() + " " + r.getCreatedBy().getLastName(), small);
                Map<String, Object> data = r.getData() != null ? r.getData() : Map.of();
                for (TableColumn c : columns) {
                    Object v = data.get(c.getFieldName());
                    addCell(table, v == null ? "" : v.toString(), small);
                }
            }
            doc.add(table);
            doc.close();
            return out.toByteArray();
        } catch (DocumentException e) {
            throw new IllegalStateException("Could not build PDF", e);
        }
    }

    private void addCell(PdfPTable table, String text, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(text == null ? "" : text, font));
        cell.setHorizontalAlignment(Element.ALIGN_LEFT);
        cell.setPadding(4);
        table.addCell(cell);
    }
}

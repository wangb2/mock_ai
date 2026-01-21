package com.example.mock.parser.service;

import com.example.mock.parser.model.ParsedDocument;
import com.example.mock.parser.model.Section;
import com.example.mock.parser.model.TableData;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.hwpf.HWPFDocument;
import org.apache.poi.hwpf.usermodel.Range;
import org.apache.poi.hwpf.usermodel.TableIterator;
import org.apache.poi.hwpf.usermodel.TableCell;
import org.apache.poi.hwpf.usermodel.TableRow;
import org.apache.poi.xwpf.usermodel.IBodyElement;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class DocumentParserService {

    private static final Pattern HEADING_PATTERN = Pattern.compile("^\\s*(\\d+(?:\\.\\d+)*)\\s+(.+)$");

    public ParsedDocument parse(MultipartFile file) throws IOException {
        String fileName = file.getOriginalFilename();
        String lower = fileName == null ? "" : fileName.toLowerCase(Locale.ROOT);

        ParsedDocument parsedDocument = new ParsedDocument();
        parsedDocument.setFileName(fileName);

        if (lower.endsWith(".docx")) {
            parsedDocument.setFileType("docx");
            try (InputStream in = file.getInputStream()) {
                parsedDocument.setSections(parseDocx(in));
            }
            return parsedDocument;
        }

        if (lower.endsWith(".doc")) {
            parsedDocument.setFileType("doc");
            try (InputStream in = file.getInputStream()) {
                parsedDocument.setSections(parseDoc(in));
            }
            return parsedDocument;
        }

        if (lower.endsWith(".pdf")) {
            parsedDocument.setFileType("pdf");
            try (InputStream in = file.getInputStream()) {
                parsedDocument.setSections(parsePdf(in));
            }
            return parsedDocument;
        }

        throw new IllegalArgumentException("Unsupported file type: " + fileName);
    }

    private List<Section> parseDocx(InputStream in) throws IOException {
        List<Section> sections = new ArrayList<>();
        Section current = null;

        try (XWPFDocument document = new XWPFDocument(in)) {
            for (IBodyElement element : document.getBodyElements()) {
                switch (element.getElementType()) {
                    case PARAGRAPH:
                        XWPFParagraph paragraph = (XWPFParagraph) element;
                        String text = safeText(paragraph.getText());
                        if (text.isEmpty()) {
                            continue;
                        }
                        int level = headingLevel(paragraph.getStyle());
                        if (level > 0 || looksLikeHeading(text)) {
                            current = new Section(text, level > 0 ? level : 1);
                            sections.add(current);
                        } else {
                            current = ensureSection(sections, current, "Document", 0);
                            current.setContent(appendText(current.getContent(), text));
                        }
                        break;
                    case TABLE:
                        XWPFTable table = (XWPFTable) element;
                        current = ensureSection(sections, current, "Document", 0);
                        current.getTables().add(convertTable(table));
                        break;
                    default:
                        break;
                }
            }
        }

        return sections;
    }

    private List<Section> parseDoc(InputStream in) throws IOException {
        List<Section> sections = new ArrayList<>();
        Section current = null;

        try (HWPFDocument document = new HWPFDocument(in)) {
            Range range = document.getRange();
            for (int i = 0; i < range.numParagraphs(); i++) {
                String text = safeText(range.getParagraph(i).text());
                if (text.isEmpty()) {
                    continue;
                }
                if (looksLikeHeading(text)) {
                    current = new Section(text.trim(), 1);
                    sections.add(current);
                } else {
                    current = ensureSection(sections, current, "Document", 0);
                    current.setContent(appendText(current.getContent(), text));
                }
            }

            TableIterator iterator = new TableIterator(range);
            while (iterator.hasNext()) {
                org.apache.poi.hwpf.usermodel.Table table = iterator.next();
                current = ensureSection(sections, current, "Document", 0);
                current.getTables().add(convertTable(table));
            }
        }

        return sections;
    }

    private List<Section> parsePdf(InputStream in) throws IOException {
        List<Section> sections = new ArrayList<>();
        Section current = null;

        try (PDDocument pdfDocument = PDDocument.load(in)) {
            int totalPages = pdfDocument.getNumberOfPages();
            PDFTextStripper stripper = new PDFTextStripper();

            for (int page = 1; page <= totalPages; page++) {
                stripper.setStartPage(page);
                stripper.setEndPage(page);
                String pageText = safeText(stripper.getText(pdfDocument));
                List<Section> pageSections = parseTextSections(pageText);
                if (!pageSections.isEmpty()) {
                    sections.addAll(pageSections);
                    current = pageSections.get(pageSections.size() - 1);
                }

            }
        }

        return sections;
    }

    private List<Section> parseTextSections(String text) {
        List<Section> sections = new ArrayList<>();
        if (text == null || text.trim().isEmpty()) {
            return sections;
        }

        String[] lines = text.split("\\r?\\n");
        Section current = null;
        for (String line : lines) {
            String cleaned = safeText(line);
            if (cleaned.isEmpty()) {
                continue;
            }
            if (looksLikeHeading(cleaned)) {
                current = new Section(cleaned.trim(), 1);
                sections.add(current);
            } else {
                current = ensureSection(sections, current, "Document", 0);
                current.setContent(appendText(current.getContent(), cleaned));
            }
        }
        return sections;
    }

    private Section ensureSection(List<Section> sections, Section current, String title, int level) {
        if (current != null) {
            return current;
        }
        Section created = new Section(title, level);
        sections.add(created);
        return created;
    }

    private String appendText(String base, String extra) {
        if (base == null || base.isEmpty()) {
            return extra;
        }
        return base + "\n" + extra;
    }

    private String safeText(String text) {
        if (text == null) {
            return "";
        }
        return text.replace("\u0007", "").trim();
    }

    private int headingLevel(String style) {
        if (style == null) {
            return 0;
        }
        String lower = style.toLowerCase(Locale.ROOT);
        if (lower.startsWith("heading")) {
            String number = lower.replace("heading", "").trim();
            try {
                return Integer.parseInt(number);
            } catch (NumberFormatException ignored) {
                return 1;
            }
        }
        return 0;
    }

    private boolean looksLikeHeading(String text) {
        if (text.length() <= 2) {
            return false;
        }
        Matcher matcher = HEADING_PATTERN.matcher(text);
        if (matcher.matches()) {
            return true;
        }
        return text.startsWith("接口") || text.startsWith("API") || text.startsWith("Interface");
    }

    private TableData convertTable(XWPFTable table) {
        TableData data = new TableData();
        List<XWPFTableRow> rows = table.getRows();
        if (rows.isEmpty()) {
            return data;
        }

        List<String> headers = new ArrayList<>();
        for (int i = 0; i < rows.get(0).getTableCells().size(); i++) {
            headers.add(safeText(rows.get(0).getCell(i).getText()));
        }
        data.setHeaders(headers);

        for (int i = 1; i < rows.size(); i++) {
            XWPFTableRow row = rows.get(i);
            List<String> values = new ArrayList<>();
            row.getTableCells().forEach(cell -> values.add(safeText(cell.getText())));
            data.getRows().add(values);
        }
        return data;
    }

    private TableData convertTable(org.apache.poi.hwpf.usermodel.Table table) {
        TableData data = new TableData();
        if (table.numRows() == 0) {
            return data;
        }

        TableRow headerRow = table.getRow(0);
        List<String> headers = new ArrayList<>();
        for (int c = 0; c < headerRow.numCells(); c++) {
            TableCell cell = headerRow.getCell(c);
            headers.add(safeText(cell.text()));
        }
        data.setHeaders(headers);

        for (int r = 1; r < table.numRows(); r++) {
            TableRow row = table.getRow(r);
            List<String> values = new ArrayList<>();
            for (int c = 0; c < row.numCells(); c++) {
                values.add(safeText(row.getCell(c).text()));
            }
            data.getRows().add(values);
        }
        return data;
    }

}

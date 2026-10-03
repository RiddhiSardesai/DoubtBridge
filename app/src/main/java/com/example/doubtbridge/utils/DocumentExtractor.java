package com.example.doubtbridge.utils;

import android.content.Context;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.tom_roush.pdfbox.android.PDFBoxResourceLoader;
import com.tom_roush.pdfbox.pdmodel.PDDocument;
import com.tom_roush.pdfbox.text.PDFTextStripper;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.zip.*;

public class DocumentExtractor {

    private static final String TAG = "DocExtractor";
    /** Separator inserted between PPTX slides so KeywordMatcher can find the right slide */
    public static final String SLIDE_SEP = "\n---SLIDE---\n";

    private static boolean pdfBoxInit = false;

    public interface ExtractionCallback {
        void onSuccess(String text, int pageCount);
        void onError(String message);
    }

    public static void extract(Context ctx, Uri uri, String fileName, ExtractionCallback cb) {
        new Thread(() -> {
            try {
                String lo = fileName.toLowerCase().trim();
                String text;
                int pages = 1;

                if (lo.endsWith(".pdf")) {
                    Object[] r = extractPdf(ctx, uri);
                    text  = (String) r[0];
                    pages = (int)    r[1];
                } else if (lo.endsWith(".pptx")) {
                    Object[] r = extractPptx(ctx, uri);
                    text  = (String) r[0];
                    pages = (int)    r[1];
                } else if (lo.endsWith(".docx")) {
                    text = extractDocx(ctx, uri);
                } else if (lo.endsWith(".ppt")) {
                    postError(cb,
                        "Legacy .ppt cannot be read on Android.\n\n" +
                        "Open in PowerPoint → Save As .pptx → upload again.");
                    return;
                } else {
                    text = extractTxt(ctx, uri);
                }

                // For non-PPTX: clean normally. For PPTX: preserve slide separators.
                String clean = lo.endsWith(".pptx") ? cleanPptx(text) : cleanText(text);

                if (clean.length() < 30) {
                    postError(cb,
                        "Could not extract readable text.\n" +
                        "This PDF may be scanned (image-only). Try uploading a PPTX or DOCX instead.");
                    return;
                }

                final String t = clean;
                final int p    = pages;
                new Handler(Looper.getMainLooper()).post(() -> cb.onSuccess(t, p));

            } catch (Exception e) {
                Log.e(TAG, "extract", e);
                postError(cb, "Error: " + (e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName()));
            }
        }).start();
    }

    // ── PDF ──────────────────────────────────────────────────────

    private static Object[] extractPdf(Context ctx, Uri uri) throws Exception {
        if (!pdfBoxInit) {
            PDFBoxResourceLoader.init(ctx.getApplicationContext());
            pdfBoxInit = true;
        }
        try (InputStream in = ctx.getContentResolver().openInputStream(uri)) {
            if (in == null) throw new IOException("Cannot open PDF");
            PDDocument doc = PDDocument.load(in);
            int pages = doc.getNumberOfPages();
            PDFTextStripper s = new PDFTextStripper();
            s.setSortByPosition(true);
            String text = s.getText(doc);
            doc.close();
            return new Object[]{text, pages};
        }
    }

    // ── PPTX ─────────────────────────────────────────────────────
    // Each slide is stored as "SLIDE_TITLE\nSlide body text\n---SLIDE---\n"
    // This lets KeywordMatcher score each slide independently.

    private static Object[] extractPptx(Context ctx, Uri uri) throws Exception {
        // slide number → full text of that slide
        TreeMap<Integer, String> slides = new TreeMap<>();

        try (InputStream raw = ctx.getContentResolver().openInputStream(uri)) {
            if (raw == null) throw new IOException("Cannot open PPTX");
            try (ZipInputStream zip = new ZipInputStream(raw)) {
                ZipEntry entry;
                while ((entry = zip.getNextEntry()) != null) {
                    String name = entry.getName();
                    if (name.startsWith("ppt/slides/slide") && name.endsWith(".xml")
                            && !name.contains("/_rels/")) {
                        // Extract slide number from filename e.g. "ppt/slides/slide3.xml" → 3
                        int slideNum = slideNumber(name);
                        String xml  = readZipEntry(zip);
                        // Extract text preserving paragraph breaks within the slide
                        String text = xmlTextWithBreaks(xml);
                        if (!text.trim().isEmpty()) slides.put(slideNum, text);
                    }
                    zip.closeEntry();
                }
            }
        }

        StringBuilder sb = new StringBuilder();
        for (Map.Entry<Integer, String> e : slides.entrySet()) {
            sb.append(e.getValue().trim());
            sb.append(SLIDE_SEP);
        }
        return new Object[]{sb.toString(), slides.size()};
    }

    private static int slideNumber(String name) {
        // ppt/slides/slide3.xml → 3
        try {
            String base = name.substring(name.lastIndexOf('/') + 1); // "slide3.xml"
            String num  = base.replaceAll("[^0-9]", "");
            return num.isEmpty() ? 999 : Integer.parseInt(num);
        } catch (Exception e) { return 999; }
    }

    // ── DOCX ─────────────────────────────────────────────────────

    private static String extractDocx(Context ctx, Uri uri) throws Exception {
        try (InputStream raw = ctx.getContentResolver().openInputStream(uri)) {
            if (raw == null) throw new IOException("Cannot open DOCX");
            try (ZipInputStream zip = new ZipInputStream(raw)) {
                ZipEntry entry;
                while ((entry = zip.getNextEntry()) != null) {
                    if ("word/document.xml".equals(entry.getName()))
                        return xmlTextWithBreaks(readZipEntry(zip));
                    zip.closeEntry();
                }
            }
        }
        return "";
    }

    // ── TXT ──────────────────────────────────────────────────────

    private static String extractTxt(Context ctx, Uri uri) throws IOException {
        try (InputStream in = ctx.getContentResolver().openInputStream(uri);
             BufferedReader br = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) sb.append(line).append('\n');
            return sb.toString();
        }
    }

    // ── XML helpers ───────────────────────────────────────────────

    /**
     * Extracts text from <a:t> / <w:t> tags.
     * Inserts a newline after every paragraph (<a:p> / <w:p>) so sentences stay separate.
     */
    private static String xmlTextWithBreaks(String xml) {
        StringBuilder sb = new StringBuilder();
        try {
            XmlPullParserFactory f = XmlPullParserFactory.newInstance();
            f.setNamespaceAware(false);
            XmlPullParser p = f.newPullParser();
            p.setInput(new StringReader(xml));
            boolean inT = false;
            int evt = p.getEventType();
            while (evt != XmlPullParser.END_DOCUMENT) {
                if (evt == XmlPullParser.START_TAG) {
                    String n = p.getName();
                    inT = "t".equals(n) || n.endsWith(":t");
                } else if (evt == XmlPullParser.TEXT && inT) {
                    String t = p.getText();
                    if (t != null && !t.isEmpty()) sb.append(t);
                    inT = false;
                } else if (evt == XmlPullParser.END_TAG) {
                    String n = p.getName();
                    // Add newline at paragraph end (a:p or w:p)
                    if ("p".equals(n) || n.endsWith(":p")) {
                        if (sb.length() > 0 && sb.charAt(sb.length() - 1) != '\n')
                            sb.append('\n');
                    }
                    inT = false;
                }
                evt = p.next();
            }
        } catch (Exception e) {
            Log.e(TAG, "xmlTextWithBreaks", e);
        }
        return sb.toString().trim();
    }

    // ── Cleaning ─────────────────────────────────────────────────

    private static String cleanText(String s) {
        if (s == null || s.isEmpty()) return "";
        return s.replaceAll("[ \t]+", " ")
                .replaceAll("(\n\\s*){3,}", "\n\n")
                .replaceAll("[\u0000-\u0008\u000B\u000C\u000E-\u001F\u007F]", "")
                .trim();
    }

    /** Clean PPTX text but preserve the SLIDE_SEP markers */
    private static String cleanPptx(String s) {
        if (s == null || s.isEmpty()) return "";
        // Split by slide separator, clean each slide individually, rejoin
        String[] slides = s.split("---SLIDE---");
        StringBuilder sb = new StringBuilder();
        for (String slide : slides) {
            String cleaned = cleanText(slide);
            if (!cleaned.isEmpty()) {
                sb.append(cleaned);
                sb.append(SLIDE_SEP);
            }
        }
        return sb.toString().trim();
    }

    // ── Utilities ─────────────────────────────────────────────────

    private static String readZipEntry(ZipInputStream zip) throws IOException {
        return new String(readBytes(zip), StandardCharsets.UTF_8);
    }

    private static byte[] readBytes(InputStream is) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buf = new byte[8192]; int n;
        while ((n = is.read(buf)) != -1) out.write(buf, 0, n);
        return out.toByteArray();
    }

    private static void postError(ExtractionCallback cb, String msg) {
        new Handler(Looper.getMainLooper()).post(() -> cb.onError(msg));
    }

    // Unused — kept for compatibility
    private static String xmlText(String xml) { return xmlTextWithBreaks(xml); }
}

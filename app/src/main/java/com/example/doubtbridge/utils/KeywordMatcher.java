package com.example.doubtbridge.utils;

import com.example.doubtbridge.models.Doubt;
import com.example.doubtbridge.models.Note;

import java.util.*;

public class KeywordMatcher {

    private static final Set<String> STOP = new HashSet<>(Arrays.asList(
        "a","an","the","is","it","in","on","of","to","and","or","for","with",
        "at","by","from","up","about","into","through","during","before","after",
        "this","that","these","those","i","we","you","he","she","they","my","your",
        "can","do","does","did",
        "be","been","being","have","has","had","will","would","could","should","may",
        "me","him","her","us","them","am","are","was","were","but","if",
        "so","as","its","our","also","more","than","then","just","like","very","get",
        "got","let","per","via","etc","vs","see","use","used","using","slide","page"
    ));

    // ── Public API ────────────────────────────────────────────────

    public static List<String> extractKeywords(String text) {
        List<String> kw = new ArrayList<>();
        if (text == null || text.isEmpty()) return kw;
        
        // 1. Keep original and split CamelCase
        String originalClean = text.toLowerCase().replaceAll("[^a-z0-9+#\\s]", " ");
        String camelSplit = text.replaceAll("([a-z])([A-Z])", "$1 $2");
        String splitClean = camelSplit.toLowerCase().replaceAll("[^a-z0-9+#\\s]", " ");
        
        Set<String> seen = new HashSet<>();
        // Process both original and split
        for (String source : new String[]{originalClean, splitClean}) {
            String[] words = source.split("\\s+");
            for (String w : words) {
                if (w.length() > 1 && !STOP.contains(w)) {
                    if (seen.add(w)) kw.add(w);
                }
            }
            // Bigrams
            for (int i = 0; i < words.length - 1; i++) {
                String w1 = words[i]; String w2 = words[i+1];
                if (w1.length() > 1 && w2.length() > 1 && !STOP.contains(w1) && !STOP.contains(w2)) {
                    String bigram = w1 + " " + w2;
                    if (seen.add(bigram)) kw.add(bigram);
                }
            }
        }
        return kw;
    }

    public static int scoreMatch(Doubt doubt, Note note) {
        if (doubt == null || note == null) return 0;
        
        String dSub = nvl(doubt.getSubject()).toLowerCase();
        String nSub = nvl(note.getSubject()).toLowerCase();
        if (!dSub.isEmpty() && !nSub.isEmpty() && !dSub.equals(nSub)) {
             boolean isCS1 = dSub.contains("computer science") || dSub.equals("cs");
             boolean isCS2 = nSub.contains("computer science") || nSub.equals("cs");
             if (!(isCS1 && isCS2)) return 0;
        }

        String titleStr = nvl(doubt.getTitle()).toLowerCase();
        String descStr = nvl(doubt.getDescription()).toLowerCase();
        String combined = titleStr + " " + descStr;
        boolean seeksDef = combined.contains("what is") || combined.contains("define") || 
                          combined.contains("definition") || combined.contains("meaning") ||
                          combined.contains("explain") || combined.contains("describe") ||
                          combined.contains("error") || combined.contains("exception") ||
                          combined.contains("how to") || combined.contains("why does") ||
                          combined.contains("what does") || combined.contains("tell me about");

        List<String> dkw = extractKeywords(combined);
        if (dkw.isEmpty()) return 0;

        int hits = 0;
        String noteTitle = nvl(note.getTitle()).toLowerCase();
        String noteContent = nvl(note.getContent()).toLowerCase();

        for (String k : dkw) {
            if (k.contains(" ")) { 
                if (noteTitle.contains(k)) hits += 30; // Increased weight for exact title phrase match
            } else if (k.length() >= 3) { // Ignore very short words
                if (noteTitle.contains(k)) hits += 15;
            }
        }

        Set<String> ns = (note.getKeywords() != null && !note.getKeywords().isEmpty())
            ? new HashSet<>(note.getKeywords()) 
            : new HashSet<>(extractKeywords(noteContent));

        for (String k : dkw) {
            if (ns.contains(k)) {
                hits += k.contains(" ") ? 15 : 8; // Increased weight for content keyword hits
            }
        }

        if (seeksDef && hits > 10) { 
            if (noteTitle.contains("intro") || noteTitle.contains("what") || 
                noteTitle.contains("basic") || noteTitle.contains("concept") ||
                noteTitle.contains("overview") || noteTitle.contains("guide")) {
                hits += 15;
            }
        }

        // Boost for PPTX files as they represent official lecture slides
        if ("pptx".equalsIgnoreCase(note.getFileType())) {
            hits += 10;
        }
        
        int maxPossibleHits = 0;
        for (String k : dkw) {
            maxPossibleHits += k.contains(" ") ? 45 : 20;
        }

        if (maxPossibleHits == 0) return 0;
        return Math.min(100, (int)(hits * 100.0 / maxPossibleHits));
    }

    public static String extractRAGContext(Doubt doubt, List<Note> notes) {
        StringBuilder contextBuilder = new StringBuilder();
        notes.sort((n1, n2) -> Integer.compare(scoreMatch(doubt, n2), scoreMatch(doubt, n1)));

        int includedCount = 0;
        for (Note note : notes) {
            int score = scoreMatch(doubt, note);
            if (score < AppConstants.MATCH_THRESHOLD) continue; 

            String relevantSnippet = extractRelevantAnswer(doubt, note);
            if (!relevantSnippet.isEmpty()) {
                contextBuilder.append("--- SOURCE: ").append(note.getTitle()).append(" ---\n")
                        .append(relevantSnippet).append("\n\n");
                includedCount++;
            }
            if (includedCount >= 3) break; 
        }
        return contextBuilder.toString();
    }

    public static String extractRelevantAnswer(Doubt doubt, Note note) {
        String content = note.getContent();
        if (content == null || content.trim().isEmpty()) return "";

        String doubtText = (nvl(doubt.getTitle()) + " " + nvl(doubt.getDescription())).toLowerCase();
        boolean seeksDef = doubtText.contains("what is") || doubtText.contains("define") || 
                          doubtText.contains("definition") || doubtText.contains("meaning") ||
                          doubtText.contains("explain") || doubtText.contains("describe") ||
                          doubtText.contains("tell me") || doubtText.contains("meaning of") ||
                          doubtText.contains("error") || doubtText.contains("exception") ||
                          doubtText.contains("how to");

        List<String> dkw = extractKeywords(doubtText);
        Set<String> kwSet = new HashSet<>(dkw);
        Set<String> commonPhrases = detectRepeatedPhrases(content);

        String result = "";
        if (content.contains(DocumentExtractor.SLIDE_SEP)) {
            result = bestSlide(content, kwSet, commonPhrases, seeksDef);
        } else {
            result = bestSentenceWindow(content, kwSet, commonPhrases, seeksDef);
        }

        return result;
    }

    private static Set<String> detectRepeatedPhrases(String content) {
        Set<String> repeated = new HashSet<>();
        String[] parts = content.split(DocumentExtractor.SLIDE_SEP + "|\\n{2,}");
        if (parts.length < 4) return repeated;

        Map<String, Integer> counts = new HashMap<>();
        for (String p : parts) {
            String[] lines = p.trim().split("\\n");
            if (lines.length > 0) {
                String firstLine = lines[0].toLowerCase().replaceAll("[^a-z0-9\\s]", "").trim();
                if (firstLine.length() > 5) counts.put(firstLine, counts.getOrDefault(firstLine, 0) + 1);
            }
        }
        for (Map.Entry<String, Integer> e : counts.entrySet()) {
            if (e.getValue() > parts.length * 0.4) repeated.add(e.getKey());
        }
        return repeated;
    }

    private static String bestSlide(String content, Set<String> kwSet, Set<String> repeated, boolean seeksDef) {
        String[] slides = content.split(DocumentExtractor.SLIDE_SEP);
        if (slides.length == 0) return "";

        List<SlideScore> scoredSlides = new ArrayList<>();
        for (int i = 0; i < slides.length; i++) {
            int score = scoreChunk(slides[i], kwSet, repeated, seeksDef);
            scoredSlides.add(new SlideScore(i, score));
        }

        Collections.sort(scoredSlides, (a, b) -> Integer.compare(b.score, a.score));

        Set<Integer> toInclude = new TreeSet<>();
        int matches = 0;
        for (SlideScore ss : scoredSlides) {
            if (ss.score > 20) { 
                toInclude.add(ss.idx);
                if (ss.idx > 0) toInclude.add(ss.idx - 1);
                if (ss.idx < slides.length - 1) toInclude.add(ss.idx + 1);
                matches++;
            }
            if (matches >= 3) break;
        }

        if (toInclude.isEmpty()) {
            for (int i = 0; i < slides.length && toInclude.size() < 3; i++) {
                if (!isLikelyTOC(slides[i])) toInclude.add(i);
            }
        }

        StringBuilder sb = new StringBuilder();
        for (int idx : toInclude) {
            String s = slides[idx].trim();
            if (s.isEmpty()) continue;
            sb.append("--- SEGMENT (Slide ").append(idx + 1).append(") ---\n")
              .append(s).append("\n\n");
            if (sb.length() > 5000) break;
        }
        return sb.toString().trim();
    }

    private static String bestSentenceWindow(String content, Set<String> kwSet, Set<String> repeated, boolean seeksDef) {
        String[] raw = content.split("(?<=[.!?])\\s+|\\n+");
        List<String> sentences = new ArrayList<>();
        for (String s : raw) {
            s = s.trim();
            if (s.length() > 3) sentences.add(s);
        }
        if (sentences.isEmpty()) return "";

        int[] scores = new int[sentences.size()];
        for (int i = 0; i < sentences.size(); i++)
            scores[i] = scoreChunk(sentences.get(i), kwSet, repeated, seeksDef);

        int top = 0;
        for (int i = 1; i < scores.length; i++) if (scores[i] > scores[top]) top = i;

        int start = Math.max(0, top - 10);
        int end   = Math.min(sentences.size() - 1, top + 10);
        StringBuilder sb = new StringBuilder();
        for (int i = start; i <= end; i++) {
            if (sb.length() > 0) sb.append(' ');
            sb.append(sentences.get(i));
            if (sb.length() >= 4000) break;
        }
        return sb.toString().trim();
    }

    private static int scoreChunk(String chunk, Set<String> kwSet, Set<String> repeated, boolean seeksDef) {
        String lo = chunk.toLowerCase();
        String cleanLo = " " + lo.replaceAll("[^a-z0-9+#\\s]", " ") + " ";

        if (isLikelyTOC(chunk)) return -200;

        int score = 0;
        String[] lines = lo.split("\\n");
        String firstLine = lines.length > 0 ? lines[0].replaceAll("[^a-z0-9\\s]", "").trim() : "";

        if (!firstLine.isEmpty() && repeated.contains(firstLine)) score -= 15;

        if (seeksDef) {
            boolean hasDefinitionPattern = cleanLo.contains(" is a ") || cleanLo.contains(" is an ") || cleanLo.contains(" is the ") ||
                    cleanLo.contains(" refers to ") || cleanLo.contains(" defined as ") || 
                    cleanLo.contains(" means ") || cleanLo.contains(" process of ") || 
                    cleanLo.contains(" stands for ") || cleanLo.contains(" is known as ") ||
                    cleanLo.contains(" consisting of ") || cleanLo.contains(" characterized by ");

            if (hasDefinitionPattern) {
                // Only give a high bonus if at least one keyword is also in this chunk
                boolean hasKeyword = false;
                for(String k : kwSet) if(cleanLo.contains(" " + k + " ")) { hasKeyword = true; break; }
                if (hasKeyword) score += 60;
            }
            if (firstLine.contains("definition") || firstLine.contains("what is") || 
                firstLine.contains("overview") || firstLine.contains("intro") || firstLine.endsWith("?")) {
                score += 35;
            }
            for (String k : kwSet) {
                if (k.length() > 2) {
                    if (cleanLo.contains(" " + k + " is") || cleanLo.contains(" " + k + " refers") || 
                        cleanLo.contains(" " + k + " defined") || cleanLo.contains(" " + k + " means")) {
                        score += 40;
                    }
                }
            }
        }

        int matchCount = 0;
        for (String k : kwSet) {
            if (k.contains(" ")) { 
                if (cleanLo.contains(" " + k + " ")) {
                    score += 30; matchCount++;
                    if (firstLine.contains(k)) score += 15;
                }
            } else {
                if (cleanLo.contains(" " + k + " ")) {
                    score += 15; matchCount++;
                } else if (cleanLo.contains(k)) score += 5;
            }
        }
        if (matchCount > 2) score += 20;

        int listLines = 0;
        for (String line : lines) if (line.trim().matches("^[0-9\\.\\-\u2022].*")) listLines++;
        if (lines.length > 3 && (double)listLines / lines.length > 0.85) score -= 25;

        return score;
    }

    private static boolean isLikelyTOC(String text) {
        if (text == null) return false;
        String lo = text.toLowerCase();
        return (lo.contains("table of contents") || lo.contains("index") ||
            lo.contains("agenda") || lo.contains("outline")) && 
            (lo.contains("....") || lo.contains("page "));
    }

    public static String trimContent(String content, int maxLen) {
        if (content == null) return "";
        String t = content.trim();
        if (t.length() <= maxLen) return t;
        int cut = t.lastIndexOf('.', maxLen);
        if (cut > maxLen / 2) return t.substring(0, cut + 1);
        return t.substring(0, maxLen) + "…";
    }

    private static String nvl(String s) { return s != null ? s : ""; }
    
    public static Note findBestMatch(Doubt doubt, List<Note> notes, int threshold) {
        Note best = null; int bestScore = threshold;
        for (Note n : notes) { int s = scoreMatch(doubt,n); if (s>=bestScore){bestScore=s;best=n;} }
        return best;
    }

    private static class SlideScore {
        int idx; int score;
        SlideScore(int i, int s) { idx=i; score=s; }
    }
}

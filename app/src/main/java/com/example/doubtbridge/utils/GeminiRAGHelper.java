package com.example.doubtbridge.utils;

import android.content.Context;
import com.google.ai.client.generativeai.GenerativeModel;
import com.google.ai.client.generativeai.java.GenerativeModelFutures;
import com.google.ai.client.generativeai.type.Content;
import com.google.ai.client.generativeai.type.GenerateContentResponse;
import com.google.ai.client.generativeai.type.GenerationConfig;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class GeminiRAGHelper {

 
    private static final String API_KEY = local.properies.API_KEY;

    private final GenerativeModelFutures model;
    private final Executor executor = Executors.newSingleThreadExecutor();

    public GeminiRAGHelper() {
        GenerationConfig.Builder configBuilder = new GenerationConfig.Builder();
        configBuilder.temperature = 0.1f; // Lowered for maximum accuracy and fidelity to PPT content
        configBuilder.topK = 40;
        configBuilder.topP = 0.95f;
        GenerationConfig config = configBuilder.build();

        GenerativeModel gm = new GenerativeModel("gemini-1.5-flash", API_KEY, config);
        this.model = GenerativeModelFutures.from(gm);
    }

    public interface RAGCallback {
        void onSuccess(String answer);
        void onFailure(Throwable t);
    }

    /**
     * RAG: Retrieval Augmented Generation
     * @param question The student's doubt
     * @param context The retrieved notes/snippets
     * @param callback Result handler
     */
    public void generateAnswer(String question, String context, RAGCallback callback) {
        if (API_KEY.isEmpty() || API_KEY.startsWith("AIzaSyCYnTr")) {
            callback.onFailure(new Exception("Gemini API Key not configured correctly."));
            return;
        }

        String prompt = "You are a Precise Academic Assistant. Your task is to provide a correct, high-fidelity answer based ONLY on the provided lecture slides and study notes.\n\n" +
                "--- STUDENT QUESTION ---\n" + question + "\n\n" +
                "--- SOURCE CONTEXT (PPT SLIDES/NOTES) ---\n" + context + "\n\n" +
                "--- MANDATORY RESPONSE GUIDELINES ---\n" +
                "1. **PPT FIDELITY**: If the context contains '--- SEGMENT (Slide X) ---', these are direct lecture slides. Use the exact terminology, definitions, and logic found in these slides.\n" +
                "2. **NO FILLER**: Do not use 'Based on the slides' or 'I found...'. Start immediately with the answer.\n" +
                "3. **SOURCE CITATION**: End the answer with 'Source: [Note/PPT Title]'.\n" +
                "4. **TECHNICAL PRECISION**: Format all code, math, or technical terms in `inline code blocks`. Use bold for primary concepts.\n" +
                "5. **GAP HANDLING**: If the PPT content does NOT contain the answer, state: 'The provided lecture material does not specify this.' then offer a brief '[Expert Note]' based on general knowledge.\n" +
                "6. **COMPREHENSIVENESS**: If the answer spans multiple slides, synthesize them into a coherent, structured explanation using bullet points.";

        Content content = new Content.Builder()
                .addText(prompt)
                .build();
        ListenableFuture<GenerateContentResponse> response = model.generateContent(content);
        
        Futures.addCallback(response, new com.google.common.util.concurrent.FutureCallback<GenerateContentResponse>() {
            @Override
            public void onSuccess(GenerateContentResponse result) {
                callback.onSuccess(result.getText());
            }

            @Override
            public void onFailure(Throwable t) {
                callback.onFailure(t);
            }
        }, executor);
    }
}


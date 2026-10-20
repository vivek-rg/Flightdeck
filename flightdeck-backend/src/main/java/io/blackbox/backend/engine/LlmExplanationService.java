package io.blackbox.backend.engine;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.blackbox.backend.model.IncidentEntity;
import io.blackbox.backend.model.RootCauseCandidateEntity;
import io.blackbox.backend.repository.IncidentRepository;
import io.blackbox.backend.repository.RootCauseCandidateRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Service
public class LlmExplanationService {

    private static final Logger log = LoggerFactory.getLogger(LlmExplanationService.class);

    @Value("${groq.api.key:${GROQ_API_KEY:}}")
    private String groqApiKey;

    @Value("${groq.api.url:https://api.groq.com/openai/v1/chat/completions}")
    private String groqApiUrl;

    @Value("${groq.model:llama-3.1-70b-versatile}")
    private String groqModel;

    private final IncidentRepository incidentRepository;
    private final RootCauseCandidateRepository candidateRepository;
    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate;

    public LlmExplanationService(IncidentRepository incidentRepository,
                                 RootCauseCandidateRepository candidateRepository,
                                 ObjectMapper objectMapper) {
        this.incidentRepository = incidentRepository;
        this.candidateRepository = candidateRepository;
        this.objectMapper = objectMapper;
        this.restTemplate = new RestTemplate();
    }

    public Map<String, Object> generateExplanation(String incidentId) {
        IncidentEntity incident = incidentRepository.findById(incidentId)
                .orElseThrow(() -> new NoSuchElementException("Incident not found: " + incidentId));

        List<RootCauseCandidateEntity> candidates = candidateRepository.findByIncidentIdOrderByScoreDesc(incidentId);

        if (candidates.isEmpty() || incident.getRootCauseConfidence() == null || incident.getRootCauseConfidence() < 0.20) {
            return Map.of(
                    "incidentId", incidentId,
                    "explanation", "Insufficient evidence to determine a reliable root cause.",
                    "llmModelUsed", "none",
                    "status", "INSUFFICIENT_EVIDENCE"
            );
        }

        RootCauseCandidateEntity topCandidate = candidates.get(0);

        // Prepare strict structured RCA payload for LLM
        Map<String, Object> structuredRca = new LinkedHashMap<>();
        structuredRca.put("incidentId", incidentId);
        structuredRca.put("rootCause", topCandidate.getCandidateName());
        structuredRca.put("triggerService", topCandidate.getServiceName());
        structuredRca.put("confidence", topCandidate.getConfidence());
        structuredRca.put("scoringBreakdown", Map.of(
                "temporalScore", topCandidate.getTemporalScore(),
                "dependencyScore", topCandidate.getDependencyScore(),
                "anomalyScore", topCandidate.getAnomalyScore(),
                "propagationScore", topCandidate.getPropagationScore(),
                "retryScore", topCandidate.getRetryScore(),
                "impactScore", topCandidate.getImpactScore()
        ));

        List<String> evidence = parseJsonList(topCandidate.getEvidenceJson());
        structuredRca.put("evidence", evidence);
        structuredRca.put("affectedServices", parseJsonList(topCandidate.getAffectedServicesJson()));
        structuredRca.put("downstreamEffects", parseJsonList(topCandidate.getDownstreamEffectsJson()));

        // Call Groq / Llama if API key is provided
        if (groqApiKey != null && !groqApiKey.isBlank() && !groqApiKey.equals("dummy")) {
            try {
                String llmResponse = callGroqLlama(structuredRca);
                if (llmResponse != null && !llmResponse.isBlank()) {
                    return Map.of(
                            "incidentId", incidentId,
                            "structuredRca", structuredRca,
                            "explanation", llmResponse,
                            "llmModelUsed", groqModel,
                            "isAiGenerated", true
                    );
                }
            } catch (Exception ex) {
                log.warn("Groq API call failed or timed out: {}. Using deterministic narrative fallback.", ex.getMessage());
            }
        }

        // High-fidelity deterministic narrative fallback
        String fallbackNarrative = buildDeterministicNarrative(topCandidate, evidence);
        return Map.of(
                "incidentId", incidentId,
                "structuredRca", structuredRca,
                "explanation", fallbackNarrative,
                "llmModelUsed", "deterministic-rules-engine",
                "isAiGenerated", false
        );
    }

    private String callGroqLlama(Map<String, Object> structuredRca) throws Exception {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(groqApiKey);

        String systemPrompt = "You are BLACKBOX incident explainer. "
                + "You NEVER determine or speculate on the root cause yourself. "
                + "The deterministic engine has ALREADY computed the root cause and evidence. "
                + "Your ONLY job is to explain the provided structured evidence clearly in markdown with these exact sections:\n"
                + "### 1. What Happened\n"
                + "### 2. Probable Root Cause\n"
                + "### 3. Supporting Evidence\n"
                + "### 4. Downstream Impact\n"
                + "### 5. Suggested Investigation & Remediation\n"
                + "Do NOT invent evidence not present in the payload.";

        String userPrompt = "Explain the following deterministic RCA results for this incident:\n"
                + objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(structuredRca);

        Map<String, Object> body = Map.of(
                "model", groqModel,
                "messages", List.of(
                        Map.of("role", "system", "content", systemPrompt),
                        Map.of("role", "user", "content", userPrompt)
                ),
                "temperature", 0.2
        );

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
        ResponseEntity<Map> response = restTemplate.postForEntity(groqApiUrl, request, Map.class);

        if (response.getBody() != null && response.getBody().containsKey("choices")) {
            List<?> choices = (List<?>) response.getBody().get("choices");
            if (!choices.isEmpty()) {
                Map<?, ?> firstChoice = (Map<?, ?>) choices.get(0);
                Map<?, ?> message = (Map<?, ?>) firstChoice.get("message");
                return (String) message.get("content");
            }
        }
        return null;
    }

    private String buildDeterministicNarrative(RootCauseCandidateEntity candidate, List<String> evidence) {
        StringBuilder sb = new StringBuilder();
        sb.append("### 1. What Happened\n");
        sb.append("A cascading failure occurred across the e-commerce application initiated by performance degradation on **")
          .append(candidate.getServiceName()).append("**. Incoming requests experienced excessive delays, leading to upstream HTTP timeouts and downstream order rejection.\n\n");

        sb.append("### 2. Probable Root Cause\n");
        sb.append("**").append(candidate.getCandidateName()).append("** (Confidence: ")
          .append(Math.round(candidate.getConfidence() * 100)).append("%)\n")
          .append("Deterministic analysis identified that the initial fault condition originated at the dependency level before propagating upward to caller services.\n\n");

        sb.append("### 3. Supporting Evidence\n");
        for (String ev : evidence) {
            sb.append("- ").append(ev).append("\n");
        }
        sb.append("\n");

        sb.append("### 4. Downstream Impact\n");
        sb.append("- Callers received HTTP 504 / 500 errors.\n");
        sb.append("- Automated client retry storms intensified thread pool consumption.\n");
        sb.append("- Inventory reservations and order state commits were blocked.\n\n");

        sb.append("### 5. Suggested Investigation & Remediation\n");
        sb.append("1. Inspect connection pool saturation, memory pressure, or slow keys on `").append(candidate.getServiceName()).append("`.\n");
        sb.append("2. Implement circuit breakers (e.g. Resilience4j) with tight fallback timeouts between order-service and payment-service.\n");
        sb.append("3. Verify cache TTL policies to avoid thundering herd conditions.\n");
        return sb.toString();
    }

    @SuppressWarnings("unchecked")
    private List<String> parseJsonList(String json) {
        if (json == null || json.isBlank()) return Collections.emptyList();
        try {
            return objectMapper.readValue(json, List.class);
        } catch (Exception e) {
            return List.of(json);
        }
    }
}

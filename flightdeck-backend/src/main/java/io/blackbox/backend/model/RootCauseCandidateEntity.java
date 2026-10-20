package io.blackbox.backend.model;

import jakarta.persistence.*;

@Entity
@Table(name = "root_cause_candidates", indexes = {
        @Index(name = "idx_rca_incident_id", columnList = "incident_id"),
        @Index(name = "idx_rca_score", columnList = "score")
})
public class RootCauseCandidateEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "incident_id", nullable = false, length = 64)
    private String incidentId;

    @Column(name = "candidate_name", nullable = false, length = 256)
    private String candidateName;

    @Column(name = "service_name", nullable = false, length = 128)
    private String serviceName;

    @Column(name = "score", nullable = false)
    private double score;

    @Column(name = "confidence", nullable = false)
    private double confidence; // 0.0 to 1.0

    @Column(name = "temporal_score")
    private double temporalScore;

    @Column(name = "dependency_score")
    private double dependencyScore;

    @Column(name = "anomaly_score")
    private double anomalyScore;

    @Column(name = "propagation_score")
    private double propagationScore;

    @Column(name = "retry_score")
    private double retryScore;

    @Column(name = "impact_score")
    private double impactScore;

    @Column(name = "is_primary_root_cause")
    private boolean primaryRootCause;

    @Lob
    @Column(name = "evidence_json", columnDefinition = "TEXT")
    private String evidenceJson;

    @Lob
    @Column(name = "affected_services_json", columnDefinition = "TEXT")
    private String affectedServicesJson;

    @Lob
    @Column(name = "downstream_effects_json", columnDefinition = "TEXT")
    private String downstreamEffectsJson;

    public RootCauseCandidateEntity() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getIncidentId() { return incidentId; }
    public void setIncidentId(String incidentId) { this.incidentId = incidentId; }

    public String getCandidateName() { return candidateName; }
    public void setCandidateName(String candidateName) { this.candidateName = candidateName; }

    public String getServiceName() { return serviceName; }
    public void setServiceName(String serviceName) { this.serviceName = serviceName; }

    public double getScore() { return score; }
    public void setScore(double score) { this.score = score; }

    public double getConfidence() { return confidence; }
    public void setConfidence(double confidence) { this.confidence = confidence; }

    public double getTemporalScore() { return temporalScore; }
    public void setTemporalScore(double temporalScore) { this.temporalScore = temporalScore; }

    public double getDependencyScore() { return dependencyScore; }
    public void setDependencyScore(double dependencyScore) { this.dependencyScore = dependencyScore; }

    public double getAnomalyScore() { return anomalyScore; }
    public void setAnomalyScore(double anomalyScore) { this.anomalyScore = anomalyScore; }

    public double getPropagationScore() { return propagationScore; }
    public void setPropagationScore(double propagationScore) { this.propagationScore = propagationScore; }

    public double getRetryScore() { return retryScore; }
    public void setRetryScore(double retryScore) { this.retryScore = retryScore; }

    public double getImpactScore() { return impactScore; }
    public void setImpactScore(double impactScore) { this.impactScore = impactScore; }

    public boolean isPrimaryRootCause() { return primaryRootCause; }
    public void setPrimaryRootCause(boolean primaryRootCause) { this.primaryRootCause = primaryRootCause; }

    public String getEvidenceJson() { return evidenceJson; }
    public void setEvidenceJson(String evidenceJson) { this.evidenceJson = evidenceJson; }

    public String getAffectedServicesJson() { return affectedServicesJson; }
    public void setAffectedServicesJson(String affectedServicesJson) { this.affectedServicesJson = affectedServicesJson; }

    public String getDownstreamEffectsJson() { return downstreamEffectsJson; }
    public void setDownstreamEffectsJson(String downstreamEffectsJson) { this.downstreamEffectsJson = downstreamEffectsJson; }
}

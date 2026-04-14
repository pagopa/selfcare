package it.pagopa.selfcare.document.service;

import com.microsoft.applicationinsights.TelemetryClient;
import com.microsoft.applicationinsights.telemetry.EventTelemetry;
import jakarta.enterprise.context.ApplicationScoped;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;

/**
 * Centralized service for emitting custom Application Insights events from document-ms.
 * <p>
 * Since document-ms is an internal service (not exposed via APIM), the standard opex
 * dashboard approach used for BFFs is not applicable. This service provides the equivalent
 * observability via Application Insights custom events, queryable via KQL in Log Analytics.
 * </p>
 * Events emitted (queryable via {@code customEvents | where cloud_RoleName == "document-ms"}):
 * <ul>
 *   <li>DOCUMENT-MS-PDF-CONTRACT-CREATED</li>
 *   <li>DOCUMENT-MS-PDF-ATTACHMENT-CREATED</li>
 *   <li>DOCUMENT-MS-SIGNED-CONTRACT-UPLOADED</li>
 *   <li>DOCUMENT-MS-SIGNATURE-VERIFIED</li>
 *   <li>DOCUMENT-MS-SIGNATURE-FAILED</li>
 *   <li>DOCUMENT-MS-DOCUMENT-SAVED</li>
 *   <li>DOCUMENT-MS-DOCUMENT-IMPORTED</li>
 * </ul>
 */
@Slf4j
@ApplicationScoped
public class DocumentMsTelemetryService {

    static final String EVENT_PDF_CONTRACT_CREATED     = "DOCUMENT-MS-PDF-CONTRACT-CREATED";
    static final String EVENT_PDF_ATTACHMENT_CREATED   = "DOCUMENT-MS-PDF-ATTACHMENT-CREATED";
    static final String EVENT_SIGNED_CONTRACT_UPLOADED = "DOCUMENT-MS-SIGNED-CONTRACT-UPLOADED";
    static final String EVENT_SIGNATURE_VERIFIED       = "DOCUMENT-MS-SIGNATURE-VERIFIED";
    static final String EVENT_SIGNATURE_FAILED         = "DOCUMENT-MS-SIGNATURE-FAILED";
    static final String EVENT_DOCUMENT_SAVED           = "DOCUMENT-MS-DOCUMENT-SAVED";
    static final String EVENT_DOCUMENT_IMPORTED        = "DOCUMENT-MS-DOCUMENT-IMPORTED";
    static final String EVENT_ATTACHMENT_UPLOADED      = "DOCUMENT-MS-ATTACHMENT-UPLOADED";
    static final String EVENT_CONTRACT_DELETED         = "DOCUMENT-MS-CONTRACT-DELETED";
    static final String EVENT_AGGREGATES_CSV_UPLOADED  = "DOCUMENT-MS-AGGREGATES-CSV-UPLOADED";
    static final String EVENT_VISURA_SAVED             = "DOCUMENT-MS-VISURA-SAVED";

    private final TelemetryClient telemetryClient;

    public DocumentMsTelemetryService() {
        this.telemetryClient = new TelemetryClient();
    }


    /**
     * Tracks the successful generation and upload of a contract PDF.
     *
     * @param onboardingId onboarding identifier
     * @param productId    product identifier
     * @param durationMs   end-to-end duration in milliseconds
     */
    public void trackContractPdfCreated(String onboardingId, String productId, long durationMs) {
        Map<String, String> props = new HashMap<>();
        props.put("onboardingId", onboardingId);
        props.put("productId", productId);

        Map<String, Double> metrics = new HashMap<>();
        metrics.put("durationMs", (double) durationMs);

        track(EVENT_PDF_CONTRACT_CREATED, props, metrics);
    }

    /**
     * Tracks the successful generation and upload of an attachment PDF.
     *
     * @param onboardingId   onboarding identifier
     * @param attachmentName attachment name / type
     * @param durationMs     end-to-end duration in milliseconds
     */
    public void trackAttachmentPdfCreated(String onboardingId, String attachmentName, long durationMs) {
        Map<String, String> props = new HashMap<>();
        props.put("onboardingId", onboardingId);
        props.put("attachmentName", attachmentName);

        Map<String, Double> metrics = new HashMap<>();
        metrics.put("durationMs", (double) durationMs);

        track(EVENT_PDF_ATTACHMENT_CREATED, props, metrics);
    }

    /**
     * Tracks the successful upload of a signed contract.
     *
     * @param onboardingId onboarding identifier
     * @param durationMs   end-to-end duration in milliseconds
     */
    public void trackSignedContractUploaded(String onboardingId, long durationMs) {
        Map<String, String> props = new HashMap<>();
        props.put("onboardingId", onboardingId);

        Map<String, Double> metrics = new HashMap<>();
        metrics.put("durationMs", (double) durationMs);

        track(EVENT_SIGNED_CONTRACT_UPLOADED, props, metrics);
    }

    /**
     * Tracks the outcome of a contract signature verification.
     *
     * @param onboardingId onboarding identifier
     * @param success      {@code true} if verification passed, {@code false} if it failed
     * @param durationMs   duration of the verification in milliseconds
     */
    public void trackSignatureVerification(String onboardingId, boolean success, long durationMs) {
        String eventName = success ? EVENT_SIGNATURE_VERIFIED : EVENT_SIGNATURE_FAILED;

        Map<String, String> props = new HashMap<>();
        props.put("onboardingId", onboardingId);
        props.put("success", String.valueOf(success));

        Map<String, Double> metrics = new HashMap<>();
        metrics.put("durationMs", (double) durationMs);

        track(eventName, props, metrics);
    }

    /**
     * Tracks the first persistence of a document (contract or attachment) in MongoDB.
     * Called only when the document does not yet exist in the DB (first save).
     *
     * @param onboardingId onboarding identifier
     * @param documentType document type: INSTITUTION, USER or ATTACHMENT
     * @param productId    product identifier
     */
    public void trackDocumentSaved(String onboardingId, String documentType, String productId) {
        Map<String, String> props = new HashMap<>();
        props.put("onboardingId", onboardingId);
        props.put("documentType", documentType);
        props.put("productId", productId);

        track(EVENT_DOCUMENT_SAVED, props, new HashMap<>());
    }

    /**
     * Tracks the import of a document during a data-import operation.
     *
     * @param onboardingId onboarding identifier
     * @param productId    product identifier
     */
    public void trackDocumentImported(String onboardingId, String productId) {
        Map<String, String> props = new HashMap<>();
        props.put("onboardingId", onboardingId);
        props.put("productId", productId);

        track(EVENT_DOCUMENT_IMPORTED, props, new HashMap<>());
    }

    /**
     * Tracks the successful upload of an attachment (signed p7m or pdf),
     * including digest verification and Azure Blob Storage persistence.
     *
     * @param onboardingId   onboarding identifier
     * @param productId      product identifier
     * @param attachmentName attachment name / type
     * @param durationMs     end-to-end duration in milliseconds
     */
    public void trackAttachmentUploaded(String onboardingId, String productId, String attachmentName, long durationMs) {
        Map<String, String> props = new HashMap<>();
        props.put("onboardingId", onboardingId);
        props.put("productId", productId);
        props.put("attachmentName", attachmentName);

        Map<String, Double> metrics = new HashMap<>();
        metrics.put("durationMs", (double) durationMs);

        track(EVENT_ATTACHMENT_UPLOADED, props, metrics);
    }

    /**
     * Tracks the deletion (soft-delete / move to delete path) of a contract from Azure Blob Storage.
     * Critical audit event — contract files are never permanently deleted, only moved.
     *
     * @param onboardingId onboarding identifier
     */
    public void trackContractDeleted(String onboardingId) {
        Map<String, String> props = new HashMap<>();
        props.put("onboardingId", onboardingId);

        track(EVENT_CONTRACT_DELETED, props, new HashMap<>());
    }

    /**
     * Tracks the successful upload of an aggregates CSV file to Azure Blob Storage.
     *
     * @param onboardingId onboarding identifier
     * @param productId    product identifier
     */
    public void trackAggregatesCsvUploaded(String onboardingId, String productId) {
        Map<String, String> props = new HashMap<>();
        props.put("onboardingId", onboardingId);
        props.put("productId", productId);

        track(EVENT_AGGREGATES_CSV_UPLOADED, props, new HashMap<>());
    }

    /**
     * Tracks the successful save of a Visura document for a merchant to Azure Blob Storage.
     *
     * @param onboardingId onboarding identifier
     * @param filename     visura filename
     */
    public void trackVisuraSaved(String onboardingId, String filename) {
        Map<String, String> props = new HashMap<>();
        props.put("onboardingId", onboardingId);
        props.put("filename", filename);

        track(EVENT_VISURA_SAVED, props, new HashMap<>());
    }

    // -------------------------------------------------------------------------

    private void track(String eventName, Map<String, String> properties, Map<String, Double> metrics) {
        try {
            EventTelemetry telemetry = new EventTelemetry(eventName);
            telemetry.getProperties().putAll(properties);
            telemetry.getMetrics().putAll(metrics);
            telemetryClient.trackEvent(telemetry);
        } catch (Exception e) {
            // Telemetry must never affect business logic
            log.warn("Failed to track telemetry event '{}': {}", eventName, e.getMessage());
        }
    }
}







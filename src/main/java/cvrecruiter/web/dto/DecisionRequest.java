package cvrecruiter.web.dto;

/** @param decision NEW, SHORTLISTED hoặc REJECTED */
public record DecisionRequest(String decision, String note) {
}

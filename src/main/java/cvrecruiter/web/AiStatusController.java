package cvrecruiter.web;

import cvrecruiter.config.AiSettings;
import cvrecruiter.config.ScoringWeights;
import cvrecruiter.web.dto.AiStatusView;

/** Cho giao diện biết AI đang bật hay tắt và trọng số chấm điểm. */
public class AiStatusController implements Controller {

    private final SessionAuthenticator sessions;
    private final AiStatusView status;

    public AiStatusController(SessionAuthenticator sessions, AiSettings ai, ScoringWeights weights) {
        this.sessions = sessions;
        this.status = AiStatusView.of(ai, weights);
    }

    @Override
    public void register(Router router) {
        router.add("GET", "/api/admin/ai-status", (ex, params) -> {
            sessions.requireAuth(ex);
            Http.sendJson(ex, 200, status);
        });
    }
}

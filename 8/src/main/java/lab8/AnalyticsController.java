package lab8;

import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// Nhóm API phân tích & đánh giá.
@RestController
@RequestMapping("/analytics")
public class AnalyticsController {

    private final AnalyticsService service;

    public AnalyticsController(AnalyticsService service) {
        this.service = service;
    }

    @GetMapping("/unusual-promotions")
    public List<AnalyticsService.UnusualPromotion> unusualPromotions() {
        return service.unusualPromotions();
    }

    @GetMapping("/promotion-proposals")
    public List<AnalyticsService.PromotionProposal> promotionProposals() {
        return service.promotionProposals();
    }
}
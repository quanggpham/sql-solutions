package lab8;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

// Phân tích thăng tiến bất thường và đề xuất thăng chức.
@Service
public class AnalyticsService {

    // Thứ bậc chức danh để so "giáng chức" / "cao hơn chức vụ khởi điểm".
    private static final Map<String, Integer> TITLE_RANK = Map.of(
            "Staff", 0,
            "Senior Staff", 1,
            "Assistant Engineer", 2,
            "Engineer", 3,
            "Senior Engineer", 4,
            "Technique Leader", 5,
            "Manager", 6);

    private final JdbcTemplate jdbc;

    public AnalyticsService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    // --- GET /analytics/unusual-promotions ---------------------------------

    public List<UnusualPromotion> unusualPromotions() {
        // Gom toàn bộ lịch sử chức danh theo từng nhân viên (đã sắp theo from_date).
        Map<Integer, CareerData> careers = new LinkedHashMap<>();
        jdbc.query("""
                SELECT t.emp_no, e.first_name, e.last_name, t.title, t.from_date, t.to_date
                FROM titles t
                JOIN employees e ON e.emp_no = t.emp_no
                ORDER BY t.emp_no, t.from_date
                """, rs -> {
            int empNo = rs.getInt("emp_no");
            String firstName = rs.getString("first_name");
            String lastName = rs.getString("last_name");
            CareerData c = careers.computeIfAbsent(empNo, k -> new CareerData(firstName, lastName, new ArrayList<>()));
            c.events().add(new TitleEvent(rs.getDate("from_date").toLocalDate(),
                    rs.getDate("to_date").toLocalDate(), rs.getString("title")));
        });

        List<UnusualPromotion> result = new ArrayList<>();
        for (Map.Entry<Integer, CareerData> e : careers.entrySet()) {
            TitleCareer a = analyze(e.getValue().events());
            if (a.changes() >= 4 && a.demoted() && a.recoveryDays() >= 0) {
                result.add(new UnusualPromotion(e.getKey(),
                        e.getValue().firstName() + " " + e.getValue().lastName(),
                        a.changes(), a.recoveryDays(), a.currentTitle()));
            }
        }
        result.sort(Comparator.comparingInt(UnusualPromotion::recoveryDays));
        return result;
    }

    record CareerData(String firstName, String lastName, List<TitleEvent> events) {
    }

    record TitleEvent(LocalDate fromDate, LocalDate toDate, String title) {
    }

    record TitleCareer(int changes, boolean demoted, int recoveryDays, String currentTitle) {
    }

    // Phân tích thuần túy (có thể unit-test): events phải sắp xếp theo from_date.
    // recoveryDays = số ngày từ lần giáng chức đầu tiên tới lúc vượt qua chức vụ
    // khởi điểm; trả -1 nếu không có giáng chức hoặc chưa phục hồi.
    static TitleCareer analyze(List<TitleEvent> events) {
        int changes = Math.max(0, events.size() - 1);
        String original = events.get(0).title();
        String current = events.get(events.size() - 1).title();
        boolean demoted = false;
        int recoveryDays = -1;
        for (int i = 1; i < events.size(); i++) {
            if (rank(events.get(i).title()) < rank(events.get(i - 1).title())) {
                demoted = true;
                recoveryDays = recovery(events, i);
                break;
            }
        }
        return new TitleCareer(changes, demoted, recoveryDays, current);
    }

    private static int recovery(List<TitleEvent> events, int demoteAt) {
        // Ngày phục hồi: lần đầu tiên sau giáng chức đạt chức vụ cao hơn chức vụ khởi điểm.
        String original = events.get(0).title();
        for (int j = demoteAt + 1; j < events.size(); j++) {
            if (rank(events.get(j).title()) > rank(original)) {
                return (int) java.time.temporal.ChronoUnit.DAYS.between(
                        events.get(demoteAt).fromDate(), events.get(j).fromDate());
            }
        }
        return -1;
    }

    private static int rank(String title) {
        return TITLE_RANK.getOrDefault(title, -1);
    }

    public record UnusualPromotion(
            int empNo, String fullName, int changes, int recoveryDays, String currentTitle) {
    }

    // --- GET /analytics/promotion-proposals ---------------------------------

    public List<PromotionProposal> promotionProposals() {
        // mỗi nhân viên: chức danh + lương hiện tại, số năm giữ chức vụ, mức lương
        // tương đối trong phòng ban + cùng cấp, số người mới vào phòng sau họ,
        // và số lần chuyển phòng ban.
        List<ProposalRaw> raws = jdbc.query("""
                SELECT e.emp_no,
                       CONCAT(e.first_name, ' ', e.last_name) AS full_name,
                       d.dept_name, t.title, s.salary,
                       DATEDIFF(CURDATE(), t.from_date) AS title_days,
                       (SELECT COUNT(*) FROM dept_emp dc WHERE dc.emp_no = e.emp_no) AS dept_rows,
                       (SELECT COUNT(*)
                          FROM dept_emp de3
                          JOIN employees e3 ON e3.emp_no = de3.emp_no
                         WHERE de3.dept_no = de.dept_no AND de3.to_date = '9999-01-01'
                           AND e3.hire_date > e.hire_date) AS lead_count,
                       (SELECT AVG(s2.salary)
                          FROM salaries s2
                          JOIN dept_emp de2 ON de2.emp_no = s2.emp_no
                                           AND de2.dept_no = de.dept_no AND de2.to_date = '9999-01-01'
                          JOIN titles t2 ON t2.emp_no = s2.emp_no
                                          AND t2.to_date = '9999-01-01' AND t2.title = t.title
                         WHERE s2.to_date = '9999-01-01') AS peer_avg
                FROM employees e
                JOIN dept_emp de ON de.emp_no = e.emp_no AND de.to_date = '9999-01-01'
                JOIN departments d ON d.dept_no = de.dept_no
                JOIN titles t ON t.emp_no = e.emp_no AND t.to_date = '9999-01-01'
                JOIN salaries s ON s.emp_no = e.emp_no AND s.to_date = '9999-01-01'
                """, (rs, i) -> {
            double peerAvg = rs.getDouble("peer_avg");
            double peer = peerAvg <= 0 ? 1.0 : Math.min(rs.getDouble("salary") / peerAvg, 2.0);
            return new ProposalRaw(
                    rs.getInt("emp_no"), rs.getString("full_name"),
                    rs.getString("dept_name"), rs.getString("title"), rs.getLong("salary"),
                    rs.getInt("title_days") / 30.44,
                    peer,
                    rs.getInt("lead_count"),
                    1.0 / (1 + Math.max(0, rs.getInt("dept_rows") - 1)));
        });

        // Chuẩn hóa từng tiêu chí về [0,1] so với người cao nhất, rồi tính điểm trọng số.
        double maxTenure = raws.stream().mapToDouble(ProposalRaw::tenureMonths).max().orElse(1);
        double maxPeer = raws.stream().mapToDouble(ProposalRaw::peerRatio).max().orElse(1);
        double maxLead = raws.stream().mapToDouble(ProposalRaw::leadership).max().orElse(1);
        double maxStable = raws.stream().mapToDouble(ProposalRaw::stability).max().orElse(1);

        List<PromotionProposal> proposals = new ArrayList<>();
        for (ProposalRaw r : raws) {
            double tenure = maxTenure == 0 ? 0 : r.tenureMonths() / maxTenure;
            double peer = maxPeer == 0 ? 0 : r.peerRatio() / maxPeer;
            double lead = maxLead == 0 ? 0 : r.leadership() / maxLead;
            double stable = maxStable == 0 ? 0 : r.stability() / maxStable;
            double score = combinedScore(tenure, peer, lead, stable);
            proposals.add(new PromotionProposal(r.empNo(), r.fullName(), r.deptName(), r.title(),
                    r.salary(), r.tenureMonths(), r.peerRatio(), r.leadership(),
                    Math.round(score * 10000.0) / 10000.0));
        }
        proposals.sort(Comparator.comparingDouble(PromotionProposal::score).reversed());
        return proposals.stream().limit(10).toList();
    }

    record ProposalRaw(int empNo, String fullName, String deptName, String title, long salary,
                       double tenureMonths, double peerRatio, double leadership, double stability) {
    }

    // Điểm tổng hợp theo 4 tiêu chí trọng số (giá trị đầu vào đã chuẩn hóa [0,1]).
    static double combinedScore(double tenure, double peer, double lead, double stable) {
        return 0.25 * tenure + 0.30 * peer + 0.20 * lead + 0.25 * stable;
    }

    public record PromotionProposal(
            int empNo, String fullName, String deptName, String title, long salary,
            double tenureMonths, double peerRatio, double leadership, double score) {
    }
}
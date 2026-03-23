package dev.promise4.GgUd.service.kakao;

import dev.promise4.GgUd.controller.dto.SettlementTransferResponse;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 정산 전용 카카오 리스트 메시지 템플릿 빌더
 */
@Component
public class SettlementMessageTemplateBuilder {

    private static final NumberFormat NUMBER_FORMAT = NumberFormat.getNumberInstance(Locale.KOREA);

    /**
     * 정산에서 빌더에 전달하는 개인별 데이터
     */
    public record SettlementPersonalData(
            String promiseName,
            BigDecimal paidAmount,
            BigDecimal perPersonAmount,
            BigDecimal balanceAmount,
            List<SettlementTransferResponse> myTransfers
    ) {}

    /**
     * 보낼 사람용 메시지
     */
    public Map<String, Object> buildSenderMessage(SettlementPersonalData data) {
        String headerTitle = "💸 " + data.promiseName() + " 정산 완료";

        List<String> toNicknames = data.myTransfers().stream()
                .filter(t -> t.getFromUserId() != null)
                .map(SettlementTransferResponse::getToNickname)
                .distinct()
                .collect(Collectors.toList());

        List<Map<String, Object>> contents = new ArrayList<>();
        contents.add(makeContent("내가 낸 금액", formatAmount(data.paidAmount()) + "원"));
        contents.add(makeContent("1인당 금액", formatAmount(data.perPersonAmount()) + "원"));
        contents.add(makeContent("내가 보내야 할 금액", formatAmount(data.balanceAmount().abs()) + "원"));
        if (!toNicknames.isEmpty()) {
            contents.add(makeContent("받는 사람", String.join(", ", toNicknames)));
        }

        return buildTemplate(headerTitle, contents);
    }

    /**
     * 받을 사람용 메시지
     */
    public Map<String, Object> buildReceiverMessage(SettlementPersonalData data) {
        String headerTitle = "✅ " + data.promiseName() + " 정산 완료";

        List<String> fromNicknames = data.myTransfers().stream()
                .map(SettlementTransferResponse::getFromNickname)
                .distinct()
                .collect(Collectors.toList());

        List<Map<String, Object>> contents = new ArrayList<>();
        contents.add(makeContent("내가 낸 금액", formatAmount(data.paidAmount()) + "원"));
        contents.add(makeContent("1인당 금액", formatAmount(data.perPersonAmount()) + "원"));
        contents.add(makeContent("내가 받을 금액", "+" + formatAmount(data.balanceAmount()) + "원"));
        if (!fromNicknames.isEmpty()) {
            contents.add(makeContent("보내는 사람", String.join(", ", fromNicknames)));
        }

        return buildTemplate(headerTitle, contents);
    }

    /**
     * 정확히 낸 사람용 메시지
     */
    public Map<String, Object> buildSettledMessage(SettlementPersonalData data) {
        String headerTitle = "🎉 " + data.promiseName() + " 정산 완료";

        List<Map<String, Object>> contents = new ArrayList<>();
        contents.add(makeContent("내가 낸 금액", formatAmount(data.paidAmount()) + "원"));
        contents.add(makeContent("1인당 금액", formatAmount(data.perPersonAmount()) + "원"));
        contents.add(makeContent("결과", "추가 송금 없음 ✓"));

        return buildTemplate(headerTitle, contents);
    }

    private Map<String, Object> buildTemplate(String headerTitle, List<Map<String, Object>> contents) {
        Map<String, Object> template = new LinkedHashMap<>();
        template.put("object_type", "list");
        template.put("header_title", headerTitle);
        template.put("header_link", new HashMap<>());
        // 카카오 리스트 메시지 최대 5개 항목
        template.put("contents", contents.size() > 5 ? contents.subList(0, 5) : contents);
        return template;
    }

    private Map<String, Object> makeContent(String title, String description) {
        Map<String, Object> content = new LinkedHashMap<>();
        content.put("title", title);
        content.put("description", description);
        content.put("link", new HashMap<>());
        return content;
    }

    private String formatAmount(BigDecimal amount) {
        return NUMBER_FORMAT.format(amount);
    }
}

package dev.promise4.GgUd.service;

import dev.promise4.GgUd.common.exception.BusinessException;
import dev.promise4.GgUd.common.exception.ErrorCode;
import dev.promise4.GgUd.controller.dto.ExpenseRecordResponse;
import dev.promise4.GgUd.controller.dto.SettlementResponse;
import dev.promise4.GgUd.controller.dto.SettlementTransferResponse;
import dev.promise4.GgUd.entity.ExpenseRecordsEntity;
import dev.promise4.GgUd.entity.Participant;
import dev.promise4.GgUd.entity.Promise;
import dev.promise4.GgUd.entity.PromiseStatus;
import dev.promise4.GgUd.entity.User;
import dev.promise4.GgUd.repository.ExpenseRecordRepository;
import dev.promise4.GgUd.repository.ParticipantRepository;
import dev.promise4.GgUd.repository.PromiseRepository;
import dev.promise4.GgUd.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ExpenseService {

    private final PromiseRepository promiseRepository;
    private final ParticipantRepository participantRepository;
    private final UserRepository userRepository;
    private final ExpenseRecordRepository expenseRecordRepository;
    private final KakaoMessageService kakaoMessageService;

    /**
     * 정산 현황 조회
     */
    @Transactional(readOnly = true)
    public SettlementResponse getSettlement(Long promiseId, Long userId) {
        Promise promise = findPromise(promiseId);
        validateSettlementAvailable(promise);
        validateParticipant(promiseId, userId);

        List<Participant> participants = participantRepository.findByPromiseId(promiseId);
        List<ExpenseRecordsEntity> expenses = expenseRecordRepository.findByPromiseId(promiseId);

        return buildSettlementResponse(promise, participants, expenses);
    }

    /**
     * 내 결제 금액 입력/수정 (upsert)
     */
    @Transactional
    public SettlementResponse updateMyExpense(Long promiseId, Long userId, BigDecimal amount) {
        Promise promise = findPromise(promiseId);
        validateSettlementAvailable(promise);
        validateParticipant(promiseId, userId);

        if (promise.isSettlementCompleted()) {
            throw new BusinessException(ErrorCode.SETTLEMENT_ALREADY_COMPLETED);
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다"));

        ExpenseRecordsEntity expense = expenseRecordRepository
                .findByPromiseIdAndUserId(promiseId, userId)
                .orElseGet(() -> ExpenseRecordsEntity.builder()
                        .promise(promise)
                        .user(user)
                        .amount(BigDecimal.ZERO)
                        .build());

        expense.setAmount(amount);
        expenseRecordRepository.save(expense);

        log.info("지출 기록 갱신: promiseId={}, userId={}, amount={}", promiseId, userId, amount);

        List<Participant> participants = participantRepository.findByPromiseId(promiseId);
        List<ExpenseRecordsEntity> expenses = expenseRecordRepository.findByPromiseId(promiseId);
        return buildSettlementResponse(promise, participants, expenses);
    }

    /**
     * 정산 완료 (호스트 전용)
     */
    @Transactional
    public SettlementResponse completeSettlement(Long promiseId, Long userId) {
        Promise promise = findPromise(promiseId);
        validateSettlementAvailable(promise);

        if (!promise.getHost().getId().equals(userId)) {
            throw new BusinessException(ErrorCode.HANDLE_ACCESS_DENIED);
        }

        if (promise.isSettlementCompleted()) {
            throw new BusinessException(ErrorCode.SETTLEMENT_ALREADY_COMPLETED);
        }

        promise.completeSettlement();

        List<Participant> participants = participantRepository.findByPromiseId(promiseId);
        List<ExpenseRecordsEntity> expenses = expenseRecordRepository.findByPromiseId(promiseId);
        SettlementResponse response = buildSettlementResponse(promise, participants, expenses);

        // 카카오톡 알림 전송 (실패해도 정산 완료 유지)
        try {
            kakaoMessageService.sendSettlementMessages(response, participants);
        } catch (Exception e) {
            log.warn("카카오톡 정산 알림 전송 중 오류: promiseId={}, error={}", promiseId, e.getMessage());
        }

        log.info("정산 완료: promiseId={}, hostId={}", promiseId, userId);
        return response;
    }

    private Promise findPromise(Long promiseId) {
        return promiseRepository.findById(promiseId)
                .orElseThrow(() -> new IllegalArgumentException("약속을 찾을 수 없습니다"));
    }

    private void validateSettlementAvailable(Promise promise) {
        PromiseStatus status = promise.getStatus();
        if (status != PromiseStatus.IN_PROGRESS && status != PromiseStatus.COMPLETED) {
            throw new BusinessException(ErrorCode.SETTLEMENT_NOT_AVAILABLE);
        }
    }

    private void validateParticipant(Long promiseId, Long userId) {
        if (!participantRepository.existsByPromiseIdAndUserId(promiseId, userId)) {
            throw new BusinessException(ErrorCode.HANDLE_ACCESS_DENIED);
        }
    }

    private SettlementResponse buildSettlementResponse(
            Promise promise,
            List<Participant> participants,
            List<ExpenseRecordsEntity> expenses) {

        int participantCount = participants.size();

        // 지출 Map: userId → amount
        Map<Long, BigDecimal> expenseMap = expenses.stream()
                .collect(Collectors.toMap(
                        e -> e.getUser().getId(),
                        ExpenseRecordsEntity::getAmount
                ));

        BigDecimal totalAmount = expenseMap.values().stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal perPersonAmount = participantCount > 0
                ? totalAmount.divide(BigDecimal.valueOf(participantCount), 0, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        // 참여자별 잔액 계산
        List<ParticipantBalance> balances = participants.stream()
                .map(p -> {
                    BigDecimal paid = expenseMap.getOrDefault(p.getUser().getId(), BigDecimal.ZERO);
                    BigDecimal balance = paid.subtract(perPersonAmount);
                    String status = balance.compareTo(BigDecimal.ZERO) > 0 ? "RECEIVER"
                            : balance.compareTo(BigDecimal.ZERO) < 0 ? "SENDER"
                            : "SETTLED";
                    return new ParticipantBalance(
                            p.getUser().getId(),
                            p.getUser().getNickname(),
                            p.getUser().getProfileImageUrl(),
                            paid,
                            balance,
                            status
                    );
                })
                .collect(Collectors.toList());

        List<ExpenseRecordResponse> expenseResponses = balances.stream()
                .map(b -> ExpenseRecordResponse.builder()
                        .userId(b.userId)
                        .nickname(b.nickname)
                        .profileImageUrl(b.profileImageUrl)
                        .paidAmount(b.paidAmount)
                        .balanceAmount(b.balance)
                        .status(b.status)
                        .build())
                .collect(Collectors.toList());

        List<SettlementTransferResponse> transfers = calculateTransfers(balances);

        return SettlementResponse.builder()
                .promiseId(promise.getId())
                .promiseName(promise.getTitle())
                .totalAmount(totalAmount)
                .perPersonAmount(perPersonAmount)
                .participantCount(participantCount)
                .isSettlementCompleted(promise.isSettlementCompleted())
                .settlementCompletedAt(promise.getSettlementCompletedAt())
                .expenses(expenseResponses)
                .transfers(transfers)
                .build();
    }

    /**
     * 그리디 최소 이체 알고리즘
     */
    private List<SettlementTransferResponse> calculateTransfers(List<ParticipantBalance> balances) {
        List<SettlementTransferResponse> transfers = new ArrayList<>();

        // 보낼 사람 (음수): 오름차순 (가장 적게 낸 사람 먼저)
        List<ParticipantBalance> senders = balances.stream()
                .filter(b -> b.balance.compareTo(BigDecimal.ZERO) < 0)
                .sorted(Comparator.comparing(b -> b.balance))
                .collect(Collectors.toList());

        // 받을 사람 (양수): 내림차순 (가장 많이 낸 사람 먼저)
        List<ParticipantBalance> receivers = balances.stream()
                .filter(b -> b.balance.compareTo(BigDecimal.ZERO) > 0)
                .sorted(Comparator.comparing((ParticipantBalance b) -> b.balance).reversed())
                .collect(Collectors.toList());

        int si = 0, ri = 0;
        BigDecimal[] senderBalances = senders.stream()
                .map(b -> b.balance.abs())
                .toArray(BigDecimal[]::new);
        BigDecimal[] receiverBalances = receivers.stream()
                .map(b -> b.balance)
                .toArray(BigDecimal[]::new);

        while (si < senders.size() && ri < receivers.size()) {
            BigDecimal sendAmt = senderBalances[si];
            BigDecimal receiveAmt = receiverBalances[ri];
            BigDecimal transferAmt = sendAmt.min(receiveAmt);

            if (transferAmt.compareTo(BigDecimal.ONE) >= 0) {
                transfers.add(SettlementTransferResponse.builder()
                        .fromUserId(senders.get(si).userId)
                        .fromNickname(senders.get(si).nickname)
                        .toUserId(receivers.get(ri).userId)
                        .toNickname(receivers.get(ri).nickname)
                        .amount(transferAmt)
                        .build());
            }

            senderBalances[si] = senderBalances[si].subtract(transferAmt);
            receiverBalances[ri] = receiverBalances[ri].subtract(transferAmt);

            if (senderBalances[si].compareTo(BigDecimal.ZERO) == 0) si++;
            if (receiverBalances[ri].compareTo(BigDecimal.ZERO) == 0) ri++;
        }

        return transfers;
    }

    private record ParticipantBalance(
            Long userId,
            String nickname,
            String profileImageUrl,
            BigDecimal paidAmount,
            BigDecimal balance,
            String status
    ) {}
}

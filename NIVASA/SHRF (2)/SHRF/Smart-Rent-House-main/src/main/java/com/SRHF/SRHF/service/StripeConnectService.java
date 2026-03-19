package com.SRHF.SRHF.service;

import com.SRHF.SRHF.entity.Payment;
import com.SRHF.SRHF.entity.User;
import com.SRHF.SRHF.repository.UserRepository;
import com.stripe.exception.StripeException;
import com.stripe.model.Account;
import com.stripe.model.AccountLink;
import com.stripe.model.Payout;
import com.stripe.model.Transfer;
import com.stripe.net.RequestOptions;
import com.stripe.param.AccountCreateParams;
import com.stripe.param.AccountLinkCreateParams;
import com.stripe.param.PayoutCreateParams;
import com.stripe.param.TransferCreateParams;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class StripeConnectService {

    private final UserRepository userRepository;

    @Value("${app.connect.return-url:http://localhost:8085/payment/connect/return}")
    private String connectReturnUrl;

    @Value("${app.connect.refresh-url:http://localhost:8085/payment/connect/refresh}")
    private String connectRefreshUrl;

    @Value("${app.connect.platform-fee-percent:0}")
    private Double platformFeePercent;

    public StripeConnectService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public String createOrFetchConnectedAccount(User landlord) throws StripeException {
        if (landlord.getStripeConnectedAccountId() != null && !landlord.getStripeConnectedAccountId().isBlank()) {
            return landlord.getStripeConnectedAccountId();
        }

        AccountCreateParams params = AccountCreateParams.builder()
                .setType(AccountCreateParams.Type.EXPRESS)
                .setCountry("IN")
                .setEmail(landlord.getEmail())
                .setBusinessType(AccountCreateParams.BusinessType.INDIVIDUAL)
                .putMetadata("landlordId", String.valueOf(landlord.getId()))
                .build();

        Account account = Account.create(params);
        landlord.setStripeConnectedAccountId(account.getId());
        landlord.setStripeOnboardingComplete(false);
        landlord.setStripePayoutsEnabled(false);
        userRepository.save(landlord);
        return account.getId();
    }

    public String createOnboardingLink(User landlord) throws StripeException {
        String accountId = createOrFetchConnectedAccount(landlord);
        AccountLinkCreateParams params = AccountLinkCreateParams.builder()
                .setAccount(accountId)
                .setType(AccountLinkCreateParams.Type.ACCOUNT_ONBOARDING)
                .setRefreshUrl(connectRefreshUrl)
                .setReturnUrl(connectReturnUrl)
                .build();
        AccountLink link = AccountLink.create(params);
        return link.getUrl();
    }

    public User syncConnectedAccountStatus(User landlord) throws StripeException {
        if (landlord.getStripeConnectedAccountId() == null || landlord.getStripeConnectedAccountId().isBlank()) {
            return landlord;
        }

        Account account = Account.retrieve(landlord.getStripeConnectedAccountId());
        landlord.setStripeOnboardingComplete(Boolean.TRUE.equals(account.getDetailsSubmitted()));
        landlord.setStripePayoutsEnabled(Boolean.TRUE.equals(account.getPayoutsEnabled()));
        return userRepository.save(landlord);
    }

    public SettlementResult settlePaymentToLandlordBank(Payment payment, User landlord) throws StripeException {
        if (landlord.getStripeConnectedAccountId() == null || landlord.getStripeConnectedAccountId().isBlank()) {
            throw new IllegalArgumentException("Landlord is not connected to Stripe payouts");
        }

        long amountPaise = Math.round((payment.getAmount() != null ? payment.getAmount() : 0.0) * 100);
        if (amountPaise <= 0) {
            throw new IllegalArgumentException("Invalid payout amount");
        }

        long feePaise = Math.round(amountPaise * ((platformFeePercent != null ? platformFeePercent : 0.0) / 100.0));
        long transferAmount = Math.max(0, amountPaise - feePaise);

        TransferCreateParams transferParams = TransferCreateParams.builder()
                .setAmount(transferAmount)
                .setCurrency("inr")
                .setDestination(landlord.getStripeConnectedAccountId())
                .putMetadata("paymentId", String.valueOf(payment.getId()))
                .putMetadata("landlordId", String.valueOf(landlord.getId()))
                .build();
        Transfer transfer = Transfer.create(transferParams);

        Payout payout = null;
        try {
            PayoutCreateParams payoutParams = PayoutCreateParams.builder()
                    .setAmount(transferAmount)
                    .setCurrency("inr")
                    .putMetadata("paymentId", String.valueOf(payment.getId()))
                    .build();

            RequestOptions options = RequestOptions.builder()
                    .setStripeAccount(landlord.getStripeConnectedAccountId())
                    .build();
            payout = Payout.create(payoutParams, options);
        } catch (Exception ignored) {
            // If immediate payout fails due to balance/schedule constraints, webhook/scheduler can settle later.
        }

        return new SettlementResult(transfer.getId(), payout != null ? payout.getId() : null,
                payout != null ? payout.getStatus() : "pending");
    }

    public record SettlementResult(String transferId, String payoutId, String payoutStatus) {
    }
}

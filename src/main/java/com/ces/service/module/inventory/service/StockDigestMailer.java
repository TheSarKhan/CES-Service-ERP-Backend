package com.ces.service.module.inventory.service;

import com.ces.service.module.inventory.dto.InventoryItemResponse;
import com.ces.service.module.inventory.entity.InventorySettings;
import com.ces.service.module.inventory.enums.StockLevel;
import com.ces.service.module.inventory.repository.InventorySettingsRepository;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Sends each branch a morning list of what has run low.
 *
 * <p>Deliberately quiet when unconfigured: no SMTP host, no recipients, or nothing below its
 * threshold all mean nothing is sent. A digest that arrives every morning saying "everything is
 * fine" trains people to delete it unread, and then the one that matters goes with it.
 */
@Component
public class StockDigestMailer {

    private static final Logger log = LoggerFactory.getLogger(StockDigestMailer.class);

    /** How many products the mail lists before it just gives the remaining count. */
    private static final int MAX_LISTED = 40;

    private final InventorySettingsRepository settingsRepository;
    private final InventorySettingsService settingsService;
    private final StockAlertService alertService;
    private final ObjectProvider<JavaMailSender> mailSender;
    private final String fromAddress;

    public StockDigestMailer(
            InventorySettingsRepository settingsRepository,
            InventorySettingsService settingsService,
            StockAlertService alertService,
            ObjectProvider<JavaMailSender> mailSender,
            @Value("${spring.mail.username:}") String fromAddress) {
        this.settingsRepository = settingsRepository;
        this.settingsService = settingsService;
        this.alertService = alertService;
        this.mailSender = mailSender;
        this.fromAddress = fromAddress;
    }

    /** 08:00 every morning, server time — before the first orders of the day are placed. */
    @Scheduled(cron = "${ces.inventory.digest-cron:0 0 8 * * *}")
    public void sendDailyDigest() {
        if (!settingsService.isMailConfigured()) {
            return;
        }
        JavaMailSender sender = mailSender.getIfAvailable();
        if (sender == null) {
            return;
        }
        for (InventorySettings settings : settingsRepository.findByDeletedAtIsNull()) {
            if (!Boolean.TRUE.equals(settings.getDailyDigestEnabled())) {
                continue;
            }
            List<String> recipients = settingsService.recipientsOf(settings);
            if (recipients.isEmpty()) {
                continue;
            }
            try {
                sendFor(sender, settings, recipients);
            } catch (Exception e) {
                // One branch's mail server trouble must not stop the others.
                log.warn("Anbar xülasəsi göndərilmədi (filial {}): {}", settings.getBranchId(), e.getMessage());
            }
        }
    }

    private void sendFor(JavaMailSender sender, InventorySettings settings, List<String> recipients) {
        List<InventoryItemResponse> items = alertService
                .listFor(
                        settings.getBranchId(),
                        false,
                        // The listing query no longer orders itself, so the digest has to ask for
                        // the order it wants: worst first, same as the screen.
                        PageRequest.of(0, MAX_LISTED + 1, StockAlertService.SHORTFALL_FIRST))
                .getContent();
        if (items.isEmpty()) {
            return;
        }

        SimpleMailMessage message = new SimpleMailMessage();
        if (fromAddress != null && !fromAddress.isBlank()) {
            message.setFrom(fromAddress);
        }
        message.setTo(recipients.toArray(String[]::new));
        message.setSubject("Anbar — stok həddindən aşağı (" + Math.min(items.size(), MAX_LISTED) + " məhsul)");
        message.setText(body(items));
        sender.send(message);
    }

    private String body(List<InventoryItemResponse> items) {
        StringBuilder text = new StringBuilder("Aşağıdakı məhsulların qalığı təyin edilmiş həddən aşağıdır.\n\n");
        items.stream().limit(MAX_LISTED).forEach(item -> {
            String mark = item.getStockLevel() == StockLevel.CRITICAL ? "[KRİTİK] " : "";
            text.append(mark)
                    .append(item.getName())
                    .append(" (").append(item.getSku()).append(")")
                    .append(" — qalıq ").append(item.getTotalQuantity()).append(" ").append(item.getUnit())
                    .append(", hədd ")
                    .append(item.getCriticalQuantity() != null ? item.getCriticalQuantity() : item.getMinQuantity())
                    .append('\n');
        });
        if (items.size() > MAX_LISTED) {
            text.append("\n... və daha ").append(items.size() - MAX_LISTED).append(" məhsul.\n");
        }
        text.append("\nCES Service — Anbar");
        return text.toString();
    }
}

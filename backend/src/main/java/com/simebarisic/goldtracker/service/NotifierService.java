package com.simebarisic.goldtracker.service;

import com.simebarisic.goldtracker.dto.ChangeDto;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class NotifierService {

    private static final Logger log = LoggerFactory.getLogger(NotifierService.class);

    private static final Map<String, String> TYPE_LABELS = Map.of(
            "new_product", "\uD83C\uDD95 Novi proizvod",
            "price_change", "\uD83D\uDCB0 Promjena cijene",
            "removed_product", "\u274C Uklonjen proizvod",
            "out_of_stock", "\u26A0\uFE0F Nedostupno",
            "back_in_stock", "\u2705 Ponovno dostupno"
    );

    private final JavaMailSender mailSender;
    private final boolean emailEnabled;
    private final String emailFrom;
    private final String emailTo;

    public NotifierService(
            JavaMailSender mailSender,
            @Value("${app.email.enabled:true}") boolean emailEnabled,
            @Value("${app.email.from:}") String emailFrom,
            @Value("${app.email.to:}") String emailTo) {
        this.mailSender = mailSender;
        this.emailEnabled = emailEnabled;
        this.emailFrom = emailFrom;
        this.emailTo = emailTo;
    }

    public void sendChangesEmail(List<ChangeDto> changes) {
        if (!emailEnabled) return;
        if (changes == null || changes.isEmpty()) return;

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, false, "UTF-8");
            helper.setFrom(emailFrom);
            helper.setTo(emailTo);
            helper.setSubject("Centar Zlata: " + changes.size() + " promjena danas");
            helper.setText(formatChangesHtml(changes), true);
            mailSender.send(message);
            log.info("Email poslan: {} promjena.", changes.size());
        } catch (Exception e) {
            log.error("Slanje emaila neuspjelo: {}", e.getMessage());
        }
    }

    private String formatChangesHtml(List<ChangeDto> changes) {
        StringBuilder rows = new StringBuilder();
        for (ChangeDto c : changes) {
            String label = TYPE_LABELS.getOrDefault(c.getType(), c.getType());
            String detail = "";
            String diffCell = "";

            if ("price_change".equals(c.getType())) {
                detail = String.format(Locale.US, "%.2f € \u2192 <b>%.2f €</b>", c.getOldValue(), c.getNewValue());
                double diff = c.getNewValue() - c.getOldValue();
                String sign = diff > 0 ? "+" : "";
                String color = diff > 0 ? "#c62828" : diff < 0 ? "#2e7d32" : "#999";
                diffCell = String.format(Locale.US, "<span style=\"color:%s;font-weight:600\">%s%.2f €</span>", color, sign, diff);
            } else if ("new_product".equals(c.getType()) && c.getNewValue() != null) {
                detail = String.format(Locale.US, "%.2f €", c.getNewValue());
            }

            rows.append("<tr><td>").append(label).append("</td><td>")
                    .append(escape(c.getName())).append("</td><td>")
                    .append(detail).append("</td><td>")
                    .append(diffCell).append("</td></tr>");
        }

        return "<h2>Centar Zlata - dnevne promjene</h2>"
                + "<table border=\"1\" cellpadding=\"6\" cellspacing=\"0\" style=\"border-collapse:collapse;font-family:sans-serif;font-size:14px\">"
                + "<tr style=\"background:#f0f0f0\"><th>Tip</th><th>Proizvod</th><th>Detalji</th><th>Razlika</th></tr>"
                + rows
                + "</table>";
    }

    private String escape(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}

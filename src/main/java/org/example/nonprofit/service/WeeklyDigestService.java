package org.example.nonprofit.service;

import java.time.LocalDate;
import java.util.List;
import org.example.nonprofit.domain.WeeklyDigest;
import org.example.nonprofit.domain.WeeklyDigest.DonorReceipt;
import org.example.nonprofit.domain.WeeklyDigest.VolunteerReminder;

public final class WeeklyDigestService {
    public WeeklyDigest prepare(
            LocalDate weekEnding,
            List<DonorReceipt> receipts,
            List<VolunteerReminder> reminders,
            WeeklyDigest.CampaignReport campaignReport) {
        LocalDate weekStart = weekEnding.minusDays(6);
        List<DonorReceipt> settledThisWeek = receipts.stream()
                .filter(r -> r.amountCents() > 0)
                .filter(r -> !r.receivedOn().isBefore(weekStart) && !r.receivedOn().isAfter(weekEnding))
                .toList();
        List<VolunteerReminder> unconfirmedNextWeek = reminders.stream()
                .filter(r -> !r.confirmed())
                .filter(r -> r.shiftDate().isAfter(weekEnding))
                .filter(r -> !r.shiftDate().isAfter(weekEnding.plusDays(7)))
                .toList();
        return new WeeklyDigest(weekEnding, settledThisWeek, unconfirmedNextWeek, campaignReport);
    }
}

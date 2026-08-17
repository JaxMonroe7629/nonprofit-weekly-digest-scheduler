package org.example.nonprofit.service;

import java.time.LocalDate;
import java.util.List;
import org.example.nonprofit.domain.WeeklyDigest;
import org.example.nonprofit.domain.WeeklyDigest.CampaignReport;
import org.example.nonprofit.domain.WeeklyDigest.DonorReceipt;
import org.example.nonprofit.domain.WeeklyDigest.VolunteerReminder;

public final class WeeklyDigestServiceTest {
    public static void main(String[] args) {
        LocalDate friday = LocalDate.of(2026, 8, 14);
        WeeklyDigest digest = new WeeklyDigestService().prepare(
                friday,
                List.of(
                        new DonorReceipt("donor-104", 12_500, friday.minusDays(2)),
                        new DonorReceipt("donor-older", 8_000, friday.minusDays(8)),
                        new DonorReceipt("donor-void", 0, friday)),
                List.of(
                        new VolunteerReminder("vol-21", friday.plusDays(2), false),
                        new VolunteerReminder("vol-22", friday.plusDays(3), true),
                        new VolunteerReminder("vol-later", friday.plusDays(9), false)),
                new CampaignReport("school-meals", 75_000, 100_000));

        check(digest.donorReceipts().stream().map(DonorReceipt::donorReference).toList()
                .equals(List.of("donor-104")), "only settled receipts from the closing week belong in the digest");
        check(digest.volunteerReminders().stream().map(VolunteerReminder::volunteerReference).toList()
                .equals(List.of("vol-21")), "only unconfirmed shifts in the next seven days need reminders");
        check(digest.campaignReport().completionPercent() == 75, "campaign completion must be deterministic");
        System.out.println("WeeklyDigestServiceTest passed");
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}

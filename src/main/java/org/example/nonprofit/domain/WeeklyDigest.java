package org.example.nonprofit.domain;

import java.time.LocalDate;
import java.util.List;

public record WeeklyDigest(
        LocalDate weekEnding,
        List<DonorReceipt> donorReceipts,
        List<VolunteerReminder> volunteerReminders,
        CampaignReport campaignReport) {

    public record DonorReceipt(String donorReference, long amountCents, LocalDate receivedOn) {}

    public record VolunteerReminder(String volunteerReference, LocalDate shiftDate, boolean confirmed) {}

    public record CampaignReport(String campaignReference, long raisedCents, long targetCents) {
        public int completionPercent() {
            if (targetCents <= 0) return 0;
            return (int) Math.min(100, raisedCents * 100 / targetCents);
        }
    }
}

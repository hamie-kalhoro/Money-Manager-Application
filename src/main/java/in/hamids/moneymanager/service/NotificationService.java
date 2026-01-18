package in.hamids.moneymanager.service;

import in.hamids.moneymanager.dto.ExpenseDTO;
import in.hamids.moneymanager.entity.ProfileEntity;
import in.hamids.moneymanager.repository.ProfileRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class NotificationService {

    private final ProfileRepository profileRepository;
    private final EmailService emailService;
    private final ExpenseService expenseService;

    @Value("${money.manager.frontend.url}")
    private String frontendUrl;

    @Scheduled(cron = "0 0 5 * * *", zone = "UTC")
    public void sendDailyIncomeExpenseReminder() {
        log.info("Job started: sendDailyIncomeExpenseReminder()");
        List<ProfileEntity> profiles = profileRepository.findAll();
        for (ProfileEntity profile : profiles) {
            String body = "Hi " + profile.getFullName() + ",<br/><br/>"
                    + "This is a friendly reminder to add your income and expenses for today in Money Manager. <br/><br/>"
                    + "<a href=" + frontendUrl + " style='display: inline-block; padding: 10px 20px; font-size: 16px; color: #fff; background-color: #4CAF50; text-decoration: none; border-radius: 5px;font-weight:bold;'>Go to Money Manager</a>"
                    + "<br/><br/>Best regards,<br/>Money Manager Team";
            emailService.sendEmail(profile.getEmail(), "Daily Reminder: Add your income and expenses", body);
        }
        log.info("Job completed: sendDailyIncomeExpenseReminder()");
    }

    @Transactional
    @Scheduled(cron = "0 0 18 * * *", zone = "UTC")
    public void sendDailyExpenseSummary() {
        log.info("Job started: sendDailyExpenseSummary()");
        List<ProfileEntity> profiles = profileRepository.findAll();
        for (ProfileEntity profile : profiles) {
            List<ExpenseDTO> todaysExpenses = expenseService.getExpensesForUserOnDate(profile.getId(), LocalDate.now());
            if (!todaysExpenses.isEmpty()) {
                StringBuilder table = new StringBuilder();
                table.append("<table style='width:100%; border-collapse: collapse;'>");
                table.append("<tr style='background-color: #f2f2f2;'>")
                        .append("<th style='border: 1px solid #ddd; padding: 8px;'>S.No</th>")
                        .append("<th style='border: 1px solid #ddd; padding: 8px;'>Name</th>")
                        .append("<th style='border: 1px solid #ddd; padding: 8px;'>Category</th>")
                        .append("<th style='border: 1px solid #ddd; padding: 8px;'>Amount</th>")
                        .append("<th style='border: 1px solid #ddd; padding: 8px;'>Date</th>")
                        .append("</tr>");
                int i = 1;
                for (ExpenseDTO expense : todaysExpenses) {
                    table.append("<tr>");
                    table.append("<td style='border: 1px solid #ddd; padding: 8px; text-align: center;'>")
                            .append(i++)
                            .append("</td>");
                    table.append("<td style='border: 1px solid #ddd; padding: 8px;'>")
                            .append(expense.getName())
                            .append("</td>");
                    table.append("<td style='border: 1px solid #ddd; padding: 8px;'>")
                            .append(expense.getCategoryName() != null ? expense.getCategoryName() : "N/A")
                            .append("</td>");
                    table.append("<td style='border: 1px solid #ddd; padding: 8px; text-align: right;'>$")
                            .append(expense.getAmount())
                            .append("</td>");
                    table.append("<td style='border: 1px solid #ddd; padding: 8px; text-align: center;'>")
                            .append(expense.getDate())
                            .append("</td>");
                    table.append("</tr>");
                }
                table.append("</table>");
                String body = "Hi " + profile.getFullName() + ",<br/><br/>"
                        + "Here is the summary of your expenses for today:<br/><br/>"
                        + table
                        + "<br/>Keep tracking your expenses to manage your finances better!<br/><br/>"
                        + "Best regards,<br/>Money Manager Team";
                emailService.sendEmail(profile.getEmail(), "Your daily Expense Summary", body);
                log.info("Sent daily expense summary to {}", profile.getEmail());
            }
        }
        log.info("Job completed: sendDailyExpenseSummary()");
    }
}

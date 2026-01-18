package in.hamids.moneymanager.service;

import in.hamids.moneymanager.dto.ExpenseDTO;
import in.hamids.moneymanager.dto.IncomeDTO;
import in.hamids.moneymanager.entity.CategoryEntity;
import in.hamids.moneymanager.entity.ExpenseEntity;
import in.hamids.moneymanager.entity.IncomeEntity;
import in.hamids.moneymanager.entity.ProfileEntity;
import in.hamids.moneymanager.repository.CategoryRepository;
import in.hamids.moneymanager.repository.IncomeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class IncomeService {

    private final CategoryRepository categoryRepository;
    private final IncomeRepository incomeRepository;
    private final ProfileService profileService;

    // Adds a new expense to the database
    public IncomeDTO addIncome(IncomeDTO dto) {
        ProfileEntity profile = profileService.getCurrentProfile();
        CategoryEntity category = categoryRepository.findById(dto.getCategoryId())
                .orElseThrow(() -> new RuntimeException("Category not found"));
        IncomeEntity newExpense = toEntity(dto, profile, category);
        newExpense = incomeRepository.save(newExpense);
        return toDto(newExpense);
    }

    // Retrieves all incomes for current month/based on the start and end date
    public List<IncomeDTO> getCurrentMonthIncomesForCurrentUser() {
        ProfileEntity profile = profileService.getCurrentProfile();
        LocalDate now = LocalDate.now();
        LocalDate startDate = now.withDayOfMonth(1);
        LocalDate endDate = now.withDayOfMonth(now.lengthOfMonth());
        List<IncomeEntity> list = incomeRepository
                .findByProfileIdAndDateBetween(profile.getId(), startDate, endDate);
        return list.stream().map(this::toDto).toList();
    }

    // delete income by id for current user
    public void deleteIncome(Long incomeId) {
        ProfileEntity profile = profileService.getCurrentProfile();
        IncomeEntity expense = incomeRepository.findById(incomeId)
                .orElseThrow(() -> new RuntimeException("Expense not found"));
        if (!expense.getProfile().getId().equals(profile.getId())) {
            throw new RuntimeException("You are not authorized to delete this expense");
        }
        incomeRepository.delete(expense);
    }

    // Get latest 5 expenses for current user
    public List<IncomeDTO> getLatest5IncomesForCurrentUser() {
        ProfileEntity profile = profileService.getCurrentProfile();
        List<IncomeEntity> list = incomeRepository.findTop5ByProfileIdOrderByDateDesc(profile.getId());
        return list.stream().map(this::toDto).toList();
    }

    // Get total expenses for current user
    public BigDecimal getTotalIncomeForCurrentUser() {
        ProfileEntity profile = profileService.getCurrentProfile();
        BigDecimal total = incomeRepository.findTotalExpensesByProfileId(profile.getId());
        return total != null ? total : BigDecimal.ZERO;
    }

    // filter incomes
    public List<IncomeDTO> filterIncomes(LocalDate startDate,
                                           LocalDate endDate,
                                           String keyword,
                                           Sort sort
    ) {
        ProfileEntity profile = profileService.getCurrentProfile();
        List<IncomeEntity> list = incomeRepository
                .findByProfileIdAndDateBetweenAndNameContainingIgnoreCase(profile.getId(),
                        startDate,
                        endDate,
                        keyword,
                        sort
                );
        return list.stream().map(this::toDto).toList();
    }

    //--------------------------HELPER METHODS--------------------------
    private IncomeEntity toEntity(
                                    IncomeDTO dto,
                                    ProfileEntity profile,
                                    CategoryEntity category
    ) {
        return IncomeEntity.builder()
                .name(dto.getName())
                .icon(dto.getIcon())
                .amount(dto.getAmount())
                .date(dto.getDate())
                .profile(profile)
                .category(category)
                .build();
    }

    private IncomeDTO toDto(IncomeEntity entity) {
        return IncomeDTO.builder()
                .id(entity.getId())
                .name(entity.getName())
                .icon(entity.getIcon())
                .categoryId(entity.getCategory() != null ? entity.getCategory().getId() : null)
                .categoryName(entity.getCategory() != null ? entity.getCategory().getName() : "N/A")
                .amount(entity.getAmount())
                .date(entity.getDate())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}

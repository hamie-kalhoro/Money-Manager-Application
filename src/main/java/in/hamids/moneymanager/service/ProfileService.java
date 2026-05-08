package in.hamids.moneymanager.service;

import in.hamids.moneymanager.dto.AuthDTO;
import in.hamids.moneymanager.dto.ProfileDTO;
import in.hamids.moneymanager.entity.ProfileEntity;
import in.hamids.moneymanager.repository.ProfileRepository;
import in.hamids.moneymanager.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProfileService {

    private final ProfileRepository profileRepository;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;

    @Value("${spring.profiles.active:dev}")
    private String activeProfile;

    @Value("${app.backend.url}")
    private String activationUrl;

    public ProfileDTO registerProfile(ProfileDTO profileDTO) {
        // Check if user already exists
        ProfileEntity existingProfile = profileRepository.findByEmail(profileDTO.getEmail()).orElse(null);
        
        ProfileEntity profileToProcess;
        if (existingProfile != null) {
            if (existingProfile.getIsActive()) {
                throw new RuntimeException("Email already registered and active. Please login.");
            }
            // If exists but not active, refresh the token and resend email
            existingProfile.setActivationToken(UUID.randomUUID().toString());
            profileToProcess = profileRepository.save(existingProfile);
        } else {
            // New registration
            profileToProcess = toEntity(profileDTO);
            profileToProcess.setActivationToken(UUID.randomUUID().toString());
            profileToProcess.setIsActive(false);
            profileToProcess = profileRepository.save(profileToProcess);
        }
        
        // Send activation email
        String activationLink = activationUrl + "/api/v1.0/activate?token=" + profileToProcess.getActivationToken();
        String subject = "Activate your Money Manager account";
        String body = "Click on the following link to activate your account: " + activationLink;
        
        try {
            emailService.sendEmail(profileToProcess.getEmail(), subject, body);
        } catch (Exception e) {
            System.err.println("Email sending failed for " + profileToProcess.getEmail() + ": " + e.getMessage());
            // We don't delete the user here, allowing them to try again (which will trigger the 'resend' logic above)
            throw new RuntimeException("Email sending failed. Please check your SMTP credentials or try again later."); 
        }
        
        return toDTO(profileToProcess);
    }


    public ProfileEntity toEntity(ProfileDTO profileDTO) {
        return ProfileEntity.builder()
                .id(profileDTO.getId())
                .fullName(profileDTO.getFullName())
                .email(profileDTO.getEmail())
                .password(passwordEncoder.encode(profileDTO.getPassword()))
                .profileImgUrl(profileDTO.getProfileImgUrl())
                .createdAt(profileDTO.getCreatedAt())
                .updatedAt(profileDTO.getUpdatedAt())
                .build();
    }

    public ProfileDTO toDTO(ProfileEntity profileEntity) {
        return ProfileDTO.builder()
                .id(profileEntity.getId())
                .fullName(profileEntity.getFullName())
                .email(profileEntity.getEmail())
                .profileImgUrl(profileEntity.getProfileImgUrl())
                .createdAt(profileEntity.getCreatedAt())
                .updatedAt(profileEntity.getUpdatedAt())
                .build();
    }

    public boolean activateProfile(String activationToken) {
        return profileRepository.findByActivationToken(activationToken)
                .map(profile -> {
                    profile.setIsActive(true);
                    profileRepository.save(profile);
                    return true;
                })
                .orElse(false);
    }

    public boolean isAccountActive(String email) {
        return profileRepository.findByEmail(email)
                .map(ProfileEntity::getIsActive)
                .orElse(false);
    }

    public ProfileEntity getCurrentProfile() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes != null) {
            HttpServletRequest request = attributes.getRequest();
            ProfileEntity cached = (ProfileEntity) request.getAttribute("CACHED_PROFILE");
            if (cached != null) return cached;
            
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            ProfileEntity profile = profileRepository.findByEmail(authentication.getName()).orElseThrow(() ->
                    new UsernameNotFoundException("Profile not found with email: " + authentication.getName()));
            
            request.setAttribute("CACHED_PROFILE", profile);
            return profile;
        }
        
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return profileRepository.findByEmail(authentication.getName()).orElseThrow(() ->
                new UsernameNotFoundException("Profile not found with email: " + authentication.getName()));
    }

    public ProfileDTO getPublicProfile(String email) {
        ProfileEntity currentUser = null;
        if(email==null){
            currentUser = getCurrentProfile();
        } else {
            currentUser = profileRepository.findByEmail(email)
                    .orElseThrow(() -> new UsernameNotFoundException("Profile not found with email: " + email));
        }
        return ProfileDTO.builder()
                .id(currentUser.getId())
                .fullName(currentUser.getFullName())
                .email(currentUser.getEmail())
                .profileImgUrl(currentUser.getProfileImgUrl())
                .createdAt(currentUser.getCreatedAt())
                .updatedAt(currentUser.getUpdatedAt())
                .build();
    }

    public Map<String, Object> authenticateAndGenerateToken(AuthDTO authDTO) {
        try {
            authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(authDTO.getEmail(), authDTO.getPassword()));
            String token = jwtUtil.generateToken(authDTO.getEmail());
            return Map.of(
                    "token", token,
                    "user", getPublicProfile(authDTO.getEmail())
            );
        } catch (Exception e) {
            throw new RuntimeException("Invalid email or password.");
        }
    }
}

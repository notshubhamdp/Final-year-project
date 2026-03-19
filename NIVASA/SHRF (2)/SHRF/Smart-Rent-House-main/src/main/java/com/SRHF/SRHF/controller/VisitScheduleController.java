package com.SRHF.SRHF.controller;

import com.SRHF.SRHF.entity.Property;
import com.SRHF.SRHF.entity.User;
import com.SRHF.SRHF.entity.VisitSchedule;
import com.SRHF.SRHF.repository.PropertyRepository;
import com.SRHF.SRHF.repository.UserRepository;
import com.SRHF.SRHF.service.VisitScheduleService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/visits")
public class VisitScheduleController {

    private final VisitScheduleService visitScheduleService;
    private final UserRepository userRepository;
    private final PropertyRepository propertyRepository;

    public VisitScheduleController(VisitScheduleService visitScheduleService,
                                   UserRepository userRepository,
                                   PropertyRepository propertyRepository) {
        this.visitScheduleService = visitScheduleService;
        this.userRepository = userRepository;
        this.propertyRepository = propertyRepository;
    }

    @GetMapping
    public String list(Authentication authentication, Model model) {
        User user = userRepository.findByemail(authentication.getName())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        List<VisitSchedule> visits = "LANDLORD".equalsIgnoreCase(user.getRole())
                ? visitScheduleService.getForLandlord(user.getId())
                : visitScheduleService.getForTenant(user.getId());

        Map<Long, String> propertyPublicIdMap = new HashMap<>();
        for (VisitSchedule visit : visits) {
            if (!propertyPublicIdMap.containsKey(visit.getPropertyId())) {
                String publicId = propertyRepository.findById(visit.getPropertyId())
                        .map(Property::getPropertyId)
                        .filter(id -> id != null && !id.isBlank())
                        .orElse(String.valueOf(visit.getPropertyId()));
                propertyPublicIdMap.put(visit.getPropertyId(), publicId);
            }
        }

        model.addAttribute("user", user);
        model.addAttribute("visits", visits);
        model.addAttribute("propertyPublicIdMap", propertyPublicIdMap);
        model.addAttribute("properties",
                propertyRepository.findByVerificationStatusAndAvailabilityStatusOrderByCreatedAtDesc("APPROVED", "AVAILABLE"));
        return "visit-list";
    }

    @PostMapping("/request")
    public String requestVisit(@RequestParam Long propertyId,
                               @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
                               @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end,
                               @RequestParam(required = false) String note,
                               Authentication authentication,
                               RedirectAttributes redirectAttributes) {
        User tenant = userRepository.findByemail(authentication.getName())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        Property property = propertyRepository.findById(propertyId)
                .orElseThrow(() -> new IllegalArgumentException("Property not found"));

        if (!"TENANT".equalsIgnoreCase(tenant.getRole())) {
            redirectAttributes.addFlashAttribute("error", "Only tenants can request visits.");
            return "redirect:/visits";
        }

        visitScheduleService.createRequest(propertyId, tenant.getId(), property.getLandlordId(), start, end, note);
        redirectAttributes.addFlashAttribute("message", "Visit request submitted.");
        return "redirect:/visits";
    }

    @PostMapping("/{id}/approve")
    public String approve(@PathVariable Long id,
                          @RequestParam(required = false) String note,
                          Authentication authentication,
                          RedirectAttributes redirectAttributes) {
        User landlord = userRepository.findByemail(authentication.getName())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        VisitSchedule visit = visitScheduleService.getById(id)
                .orElseThrow(() -> new IllegalArgumentException("Visit not found"));
        visitScheduleService.approve(visit, landlord.getId(), note);
        redirectAttributes.addFlashAttribute("message", "Visit approved.");
        return "redirect:/visits";
    }

    @PostMapping("/{id}/reschedule")
    public String reschedule(@PathVariable Long id,
                             @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
                             @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end,
                             @RequestParam(required = false) String note,
                             Authentication authentication,
                             RedirectAttributes redirectAttributes) {
        User landlord = userRepository.findByemail(authentication.getName())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        VisitSchedule visit = visitScheduleService.getById(id)
                .orElseThrow(() -> new IllegalArgumentException("Visit not found"));
        visitScheduleService.reschedule(visit, landlord.getId(), start, end, note);
        redirectAttributes.addFlashAttribute("message", "Visit rescheduled.");
        return "redirect:/visits";
    }

    @PostMapping("/{id}/reject")
    public String reject(@PathVariable Long id,
                         @RequestParam(required = false) String note,
                         Authentication authentication,
                         RedirectAttributes redirectAttributes) {
        User landlord = userRepository.findByemail(authentication.getName())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        VisitSchedule visit = visitScheduleService.getById(id)
                .orElseThrow(() -> new IllegalArgumentException("Visit not found"));
        visitScheduleService.reject(visit, landlord.getId(), note);
        redirectAttributes.addFlashAttribute("message", "Visit rejected.");
        return "redirect:/visits";
    }

    @PostMapping("/{id}/cancel")
    public String cancel(@PathVariable Long id,
                         @RequestParam(required = false) String note,
                         Authentication authentication,
                         RedirectAttributes redirectAttributes) {
        User tenant = userRepository.findByemail(authentication.getName())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        VisitSchedule visit = visitScheduleService.getById(id)
                .orElseThrow(() -> new IllegalArgumentException("Visit not found"));
        visitScheduleService.cancelByTenant(visit, tenant.getId(), note);
        redirectAttributes.addFlashAttribute("message", "Visit cancelled.");
        return "redirect:/visits";
    }
}

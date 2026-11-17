package com.fraud.detector.web;

import com.fraud.detector.security.AppUserPrincipal;
import com.fraud.detector.service.AdminModelService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/model")
public class AdminModelController {

    private final AdminModelService adminModelService;

    public AdminModelController(AdminModelService adminModelService) {
        this.adminModelService = adminModelService;
    }

    @GetMapping
    public String listele(Model model) {
        model.addAttribute("modeller", adminModelService.hepsi());
        return "admin/model";
    }

    @PostMapping("/{hesapId}/yeniden-egit")
    public String yenidenEgit(@PathVariable int hesapId, @AuthenticationPrincipal AppUserPrincipal admin,
                               RedirectAttributes ra) {
        adminModelService.yenidenEgit(hesapId, admin.getId());
        ra.addFlashAttribute("bilgi", "Hesap " + hesapId + " için model geçmiş işlemlerden yeniden eğitildi.");
        return "redirect:/admin/model";
    }
}

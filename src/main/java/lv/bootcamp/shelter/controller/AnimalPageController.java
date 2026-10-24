package lv.bootcamp.shelter.controller;

import jakarta.validation.Valid;
import lv.bootcamp.shelter.form.AnimalForm;
import lv.bootcamp.shelter.service.AnimalService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
public class AnimalPageController {

    private final AnimalService animalService;

    public AnimalPageController(AnimalService animalService) {
        this.animalService = animalService;
    }

    @GetMapping("/")
    public String home() {
        return "index";
    }

    @GetMapping("/animals")
    public String allAnimals(Model model) {
        model.addAttribute("animals", animalService.findAll());
        return "animals";
    }

    @GetMapping("/animals/new")
    public String newAnimalForm(Model model) {
        model.addAttribute("form", new AnimalForm(null, null, null, null, null, null));
        return "animals-new";
    }

    @PostMapping("/animals")
    public String createAnimal(@Valid @ModelAttribute("form") AnimalForm form,
                               BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            return "animals-new";
        }

        animalService.createFromForm(form);
        return "redirect:/animals";
    }
}

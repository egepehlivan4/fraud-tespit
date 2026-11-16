package com.fraud.detector.common;

import org.springframework.dao.DataAccessException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.ModelAndView;

/**
 * CRUD ekranlarinda (Asama 3/4) FK ihlali, benzersizlik ihlali ya da
 * trigger/procedure'den gelen RAISE EXCEPTION hatalarini yakalayip
 * kullaniciya anlasilir bir mesaj gosterir.
 */
@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(DataAccessException.class)
    public ModelAndView veriHatasi(DataAccessException ex) {
        ModelAndView mav = new ModelAndView("hata");
        mav.addObject("mesaj", VeriHatasiCozumleyici.anlasilirMesaj(ex));
        return mav;
    }
}

package org.pmiops.workbench.exceptions;

import java.util.logging.Level;
import java.util.logging.Logger;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;

@ControllerAdvice
public class ExceptionAdvice {

  private static final Logger log = Logger.getLogger(ExceptionAdvice.class.getName());

  private static final String DEFAULT_ERROR_VIEW = "error";

  @ExceptionHandler({Exception.class})
  public ResponseEntity<?> serverError(Exception e) {
    int statusCode = 500;
    if (e.getClass().getPackage().getName().equals(
        ExceptionAdvice.class.getPackage().getName())) {
      ResponseStatus responseStatus = e.getClass().getAnnotation(ResponseStatus.class);
      if (responseStatus != null) {
        statusCode = responseStatus.code().value();
        log.log(Level.WARNING, "[{0}] {1}: {2}",
            new Object[]{statusCode, e.getClass().getSimpleName(), e.getMessage()});
        if (responseStatus.code().value() < 500) {
          return ResponseEntity.status(statusCode).body(e.getMessage());
        }
      }
    }
    log.log(Level.SEVERE, "Server error", e);
    return ResponseEntity.status(statusCode).body(e.getMessage());
  }
}

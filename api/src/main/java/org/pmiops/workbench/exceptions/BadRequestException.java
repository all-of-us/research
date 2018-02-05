package org.pmiops.workbench.exceptions;

import org.pmiops.workbench.model.ErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class BadRequestException extends RuntimeException {

  private ErrorResponse errorResponse;

  public BadRequestException(String message) {
    this(ExceptionUtils.errorResponse(message));
  }

  public BadRequestException(ErrorResponse errorResponse) {
    super(errorResponse.getMessage());
    this.errorResponse = errorResponse;
  }

  public BadRequestException(Throwable t) {
    super(t);
  }

  public ErrorResponse getErrorResponse() {
    return errorResponse;
  }


}

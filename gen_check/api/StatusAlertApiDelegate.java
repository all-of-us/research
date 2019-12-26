package org.pmiops.workbench.api;

import org.pmiops.workbench.model.StatusAlert;

import io.swagger.annotations.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * A delegate to be called by the {@link StatusAlertApiController}}.
 * Should be implemented as a controller but without the {@link org.springframework.stereotype.Controller} annotation.
 * Instead, use spring to autowire this class into the {@link StatusAlertApiController}.
 */
@javax.annotation.Generated(value = "io.swagger.codegen.languages.SpringCodegen", date = "2019-12-26T14:42:01.876-06:00")

public interface StatusAlertApiDelegate {

    /**
     * @see StatusAlertApi#getStatusAlert
     */
    ResponseEntity<StatusAlert> getStatusAlert();

}

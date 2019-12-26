package org.pmiops.workbench.api;

import org.pmiops.workbench.model.EmptyResponse;
import org.pmiops.workbench.model.ErrorResponse;
import org.pmiops.workbench.model.UpdateUserDisabledRequest;

import io.swagger.annotations.*;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

import javax.validation.constraints.*;
import javax.validation.Valid;
@javax.annotation.Generated(value = "io.swagger.codegen.languages.SpringCodegen", date = "2019-12-26T15:08:16.594-06:00")

@Controller
public class AuthDomainApiController implements AuthDomainApi {
    private final AuthDomainApiDelegate delegate;

    @org.springframework.beans.factory.annotation.Autowired
    public AuthDomainApiController(AuthDomainApiDelegate delegate) {
        this.delegate = delegate;
    }


    public ResponseEntity<EmptyResponse> createAuthDomain(@ApiParam(value = "groupName",required=true ) @PathVariable("groupName") String groupName) {
        // do some magic!
        return delegate.createAuthDomain(groupName);
    }

    public ResponseEntity<Void> updateUserDisabledStatus(@ApiParam(value = "Request containing user email to update and a disabled status to update the user to."  )  @Valid @RequestBody UpdateUserDisabledRequest request) {
        // do some magic!
        return delegate.updateUserDisabledStatus(request);
    }

}

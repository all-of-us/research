/**
 * NOTE: This class is auto generated by the swagger code generator program (2.2.3).
 * https://github.com/swagger-api/swagger-codegen
 * Do not edit the class manually.
 */
package org.pmiops.workbench.api;

import org.pmiops.workbench.model.AccessBypassRequest;
import org.pmiops.workbench.model.BillingProjectMembership;
import org.pmiops.workbench.model.CreateAccountRequest;
import org.pmiops.workbench.model.EmptyResponse;
import org.pmiops.workbench.model.ErrorResponse;
import org.pmiops.workbench.model.InvitationVerificationRequest;
import org.pmiops.workbench.model.NihToken;
import org.pmiops.workbench.model.PageVisit;
import org.pmiops.workbench.model.Profile;
import org.pmiops.workbench.model.ResendWelcomeEmailRequest;
import org.pmiops.workbench.model.UpdateContactEmailRequest;
import org.pmiops.workbench.model.UserListResponse;
import org.pmiops.workbench.model.UsernameTakenResponse;

import io.swagger.annotations.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import javax.validation.constraints.*;
import javax.validation.Valid;
@javax.annotation.Generated(value = "io.swagger.codegen.languages.SpringCodegen", date = "2020-01-05T11:48:19.506-05:00")

@Api(value = "Profile", description = "the Profile API")
public interface ProfileApi {

    @ApiOperation(value = "", notes = "Updates the given user to bypass the request access requirement, e.g. \"eraCommons\", or \"twoFactorAuth\" ", response = EmptyResponse.class, authorizations = {
        @Authorization(value = "aou_oauth", scopes = {
            
            })
    }, tags={ "profile", })
    @ApiResponses(value = { 
        @ApiResponse(code = 200, message = "success", response = EmptyResponse.class),
        @ApiResponse(code = 400, message = "No module exists with name submitted", response = ErrorResponse.class),
        @ApiResponse(code = 403, message = "User doesn't have the ACCESS_CONTROL_ADMIN authority", response = ErrorResponse.class) })
    
    @RequestMapping(value = "/v1/admin/users/{userId}/bypass-access-requirement",
        produces = { "application/json" }, 
        consumes = { "application/json" },
        method = RequestMethod.POST)
    ResponseEntity<EmptyResponse> bypassAccessRequirement(@ApiParam(value = "",required=true ) @PathVariable("userId") Long userId,@ApiParam(value = "Whether the requirement should be bypassed or not. Defaults to true, so an empty body  will cause the requirement to be bypassed. "  )  @Valid @RequestBody AccessBypassRequest bypassed);


    @ApiOperation(value = "", notes = "Creates an account in the researchallofus.org domain.", response = Profile.class, tags={ "profile", })
    @ApiResponses(value = { 
        @ApiResponse(code = 200, message = "Account created successfully.", response = Profile.class),
        @ApiResponse(code = 400, message = "Error occurred while creating account.", response = ErrorResponse.class) })
    
    @RequestMapping(value = "/v1/google-account",
        produces = { "application/json" }, 
        consumes = { "application/json" },
        method = RequestMethod.POST)
    ResponseEntity<Profile> createAccount(@ApiParam(value = ""  )  @Valid @RequestBody CreateAccountRequest createAccountRequest);


    @ApiOperation(value = "", notes = "Deletes the user's profile and gsuite account, does not clean up in firecloud.", response = Void.class, authorizations = {
        @Authorization(value = "aou_oauth", scopes = {
            
            })
    }, tags={ "profile", })
    @ApiResponses(value = { 
        @ApiResponse(code = 204, message = "Request Received.", response = Void.class) })
    
    @RequestMapping(value = "/v1/profile",
        produces = { "application/json" }, 
        method = RequestMethod.DELETE)
    ResponseEntity<Void> deleteProfile();


    @ApiOperation(value = "", notes = "Returns a list of profiles for users to be reviewed. Requires ACCESS_CONTROL_ADMIN authority. ", response = UserListResponse.class, authorizations = {
        @Authorization(value = "aou_oauth", scopes = {
            
            })
    }, tags={ "profile", })
    @ApiResponses(value = { 
        @ApiResponse(code = 200, message = "A list of users", response = UserListResponse.class),
        @ApiResponse(code = 403, message = "User doesn't have the ACCESS_CONTROL_ADMIN authority", response = ErrorResponse.class) })
    
    @RequestMapping(value = "/v1/admin/users/list",
        produces = { "application/json" }, 
        method = RequestMethod.GET)
    ResponseEntity<UserListResponse> getAllUsers();


    @ApiOperation(value = "List billing projects for a user", notes = "", response = BillingProjectMembership.class, responseContainer = "List", authorizations = {
        @Authorization(value = "aou_oauth", scopes = {
            
            })
    }, tags={ "profile", })
    @ApiResponses(value = { 
        @ApiResponse(code = 200, message = "OK", response = BillingProjectMembership.class, responseContainer = "List"),
        @ApiResponse(code = 404, message = "User Not Found", response = Void.class),
        @ApiResponse(code = 500, message = "Internal Server Error", response = Void.class) })
    
    @RequestMapping(value = "/v1/billingProjects",
        produces = { "application/json" }, 
        method = RequestMethod.GET)
    ResponseEntity<List<BillingProjectMembership>> getBillingProjects();


    @ApiOperation(value = "", notes = "Returns the user's profile information", response = Profile.class, authorizations = {
        @Authorization(value = "aou_oauth", scopes = {
            
            })
    }, tags={ "profile", })
    @ApiResponses(value = { 
        @ApiResponse(code = 200, message = "The user's profile.", response = Profile.class) })
    
    @RequestMapping(value = "/v1/profile",
        produces = { "application/json" }, 
        method = RequestMethod.GET)
    ResponseEntity<Profile> getMe();


    @ApiOperation(value = "", notes = "Returns a user's profile for review.  Requires ACCESS_CONTROL_ADMIN authority. ", response = Profile.class, authorizations = {
        @Authorization(value = "aou_oauth", scopes = {
            
            })
    }, tags={ "profile", })
    @ApiResponses(value = { 
        @ApiResponse(code = 200, message = "A user's profile", response = Profile.class),
        @ApiResponse(code = 403, message = "User doesn't have the ACCESS_CONTROL_ADMIN authority", response = ErrorResponse.class) })
    
    @RequestMapping(value = "/v1/admin/users/{userId}",
        produces = { "application/json" }, 
        method = RequestMethod.GET)
    ResponseEntity<Profile> getUser(@ApiParam(value = "",required=true ) @PathVariable("userId") Long userId);


    @ApiOperation(value = "", notes = "Verifies invitation key.", response = Void.class, tags={ "profile", })
    @ApiResponses(value = { 
        @ApiResponse(code = 200, message = "Invitation Key verified.", response = Void.class),
        @ApiResponse(code = 400, message = "Error occurred while verifying Invitation Key.", response = ErrorResponse.class) })
    
    @RequestMapping(value = "/v1/invitation-key-verification",
        produces = { "application/json" }, 
        consumes = { "application/json" },
        method = RequestMethod.POST)
    ResponseEntity<Void> invitationKeyVerification(@ApiParam(value = ""  )  @Valid @RequestBody InvitationVerificationRequest invitationVerificationRequest);


    @ApiOperation(value = "", notes = "Checks to see if the given username is not available.", response = UsernameTakenResponse.class, tags={ "profile", })
    @ApiResponses(value = { 
        @ApiResponse(code = 200, message = "The answer.", response = UsernameTakenResponse.class) })
    
    @RequestMapping(value = "/v1/is-username-taken",
        produces = { "application/json" }, 
        method = RequestMethod.GET)
    ResponseEntity<UsernameTakenResponse> isUsernameTaken( @NotNull@ApiParam(value = "", required = true) @RequestParam(value = "username", required = true) String username);


    @ApiOperation(value = "", notes = "Request betaAccess.", response = Profile.class, authorizations = {
        @Authorization(value = "aou_oauth", scopes = {
            
            })
    }, tags={ "profile", })
    @ApiResponses(value = { 
        @ApiResponse(code = 200, message = "The user's profile.", response = Profile.class) })
    
    @RequestMapping(value = "/v1/beta-access",
        produces = { "application/json" }, 
        method = RequestMethod.POST)
    ResponseEntity<Profile> requestBetaAccess();


    @ApiOperation(value = "", notes = "Resend welcome email", response = Void.class, tags={ "profile", })
    @ApiResponses(value = { 
        @ApiResponse(code = 200, message = "Welcome Email sent.", response = Void.class),
        @ApiResponse(code = 500, message = "Internal Server Error", response = Void.class) })
    
    @RequestMapping(value = "/v1/resend-welcome-email",
        produces = { "application/json" }, 
        consumes = { "application/json" },
        method = RequestMethod.POST)
    ResponseEntity<Void> resendWelcomeEmail(@ApiParam(value = ""  )  @Valid @RequestBody ResendWelcomeEmailRequest resendWelcomeEmail);


    @ApiOperation(value = "", notes = "Submits consent to the data use agreement for researchers.", response = Profile.class, authorizations = {
        @Authorization(value = "aou_oauth", scopes = {
            
            })
    }, tags={ "profile", })
    @ApiResponses(value = { 
        @ApiResponse(code = 200, message = "The user's profile.", response = Profile.class) })
    
    @RequestMapping(value = "/v1/account/submit-data-use-agreement",
        produces = { "application/json" }, 
        method = RequestMethod.POST)
    ResponseEntity<Profile> submitDataUseAgreement( @NotNull@ApiParam(value = "Version \\# of the Data Use Agreement that was signed.", required = true) @RequestParam(value = "dataUseAgreementSignedVersion", required = true) Integer dataUseAgreementSignedVersion, @NotNull@ApiParam(value = "Initials of the user on the form.", required = true) @RequestParam(value = "initials", required = true) String initials);


    @ApiOperation(value = "", notes = "Submits demographic survey responses.", response = Profile.class, authorizations = {
        @Authorization(value = "aou_oauth", scopes = {
            
            })
    }, tags={ "profile", })
    @ApiResponses(value = { 
        @ApiResponse(code = 200, message = "The user's profile.", response = Profile.class) })
    
    @RequestMapping(value = "/v1/account/submit-demographic-survey",
        produces = { "application/json" }, 
        method = RequestMethod.POST)
    ResponseEntity<Profile> submitDemographicsSurvey();


    @ApiOperation(value = "Sync compliance training status", notes = "Retrieves moodleId(either from DB or call from Moodle API) and gets Training status on the basis of that", response = Profile.class, authorizations = {
        @Authorization(value = "aou_oauth", scopes = {
            
            })
    }, tags={ "profile", })
    @ApiResponses(value = { 
        @ApiResponse(code = 200, message = "The user's profile.", response = Profile.class),
        @ApiResponse(code = 404, message = "User not found", response = Void.class),
        @ApiResponse(code = 500, message = "Internal Error", response = ErrorResponse.class) })
    
    @RequestMapping(value = "/v1/account/sync-training-status",
        produces = { "application/json" }, 
        method = RequestMethod.POST)
    ResponseEntity<Profile> syncComplianceTrainingStatus();


    @ApiOperation(value = "Sync eRA Commons status", notes = "Retrieves and stores the current user's NIH / eRA Commons linkage status, fetched via the FireCloud API.", response = Profile.class, authorizations = {
        @Authorization(value = "aou_oauth", scopes = {
            
            })
    }, tags={ "profile", })
    @ApiResponses(value = { 
        @ApiResponse(code = 200, message = "The user's profile.", response = Profile.class),
        @ApiResponse(code = 404, message = "User not found", response = Void.class),
        @ApiResponse(code = 500, message = "Internal Error", response = ErrorResponse.class) })
    
    @RequestMapping(value = "/v1/account/sync-era-commons-status",
        produces = { "application/json" }, 
        method = RequestMethod.POST)
    ResponseEntity<Profile> syncEraCommonsStatus();


    @ApiOperation(value = "Sync two factor auth status", notes = "Syncs a user's 2FA status from google", response = Profile.class, authorizations = {
        @Authorization(value = "aou_oauth", scopes = {
            
            })
    }, tags={ "profile", })
    @ApiResponses(value = { 
        @ApiResponse(code = 200, message = "The user's profile.", response = Profile.class),
        @ApiResponse(code = 404, message = "User not found", response = Void.class),
        @ApiResponse(code = 500, message = "Internal Error", response = ErrorResponse.class) })
    
    @RequestMapping(value = "/v1/account/sync-two-factor-auth-status",
        produces = { "application/json" }, 
        method = RequestMethod.POST)
    ResponseEntity<Profile> syncTwoFactorAuthStatus();


    @ApiOperation(value = "", notes = "Updates the given user to bypass the request access requirement ", response = EmptyResponse.class, authorizations = {
        @Authorization(value = "aou_oauth", scopes = {
            
            })
    }, tags={ "profile", })
    @ApiResponses(value = { 
        @ApiResponse(code = 200, message = "success", response = EmptyResponse.class),
        @ApiResponse(code = 400, message = "No module exists with name submitted", response = ErrorResponse.class),
        @ApiResponse(code = 403, message = "Self bypass is disallowed in this environment", response = ErrorResponse.class) })
    
    @RequestMapping(value = "/v1/admin/unsafe-self-bypass-access-requirement",
        produces = { "application/json" }, 
        consumes = { "application/json" },
        method = RequestMethod.POST)
    ResponseEntity<EmptyResponse> unsafeSelfBypassAccessRequirement(@ApiParam(value = "Whether the requirement should be bypassed or not. Defaults to true, so an empty body  will cause the requirement to be bypassed. "  )  @Valid @RequestBody AccessBypassRequest bypassed);


    @ApiOperation(value = "", notes = "Only for accounts that have not logged in yet, update contact email.", response = Void.class, tags={ "profile", })
    @ApiResponses(value = { 
        @ApiResponse(code = 200, message = "Contact Email Updated", response = Void.class),
        @ApiResponse(code = 400, message = "Invalid contact email address", response = Void.class),
        @ApiResponse(code = 403, message = "Unable to process this request", response = Void.class),
        @ApiResponse(code = 500, message = "Internal Server Error", response = Void.class) })
    
    @RequestMapping(value = "/v1/update-contact-email",
        produces = { "application/json" }, 
        consumes = { "application/json" },
        method = RequestMethod.POST)
    ResponseEntity<Void> updateContactEmail(@ApiParam(value = ""  )  @Valid @RequestBody UpdateContactEmailRequest updateContactEmailRequest);


    @ApiOperation(value = "", notes = "Updates a users NIH token", response = Profile.class, authorizations = {
        @Authorization(value = "aou_oauth", scopes = {
            
            })
    }, tags={ "profile", })
    @ApiResponses(value = { 
        @ApiResponse(code = 200, message = "The user's updated profile.", response = Profile.class),
        @ApiResponse(code = 400, message = "Bad request", response = ErrorResponse.class) })
    
    @RequestMapping(value = "/v1/update-nih-token",
        produces = { "application/json" }, 
        consumes = { "application/json" },
        method = RequestMethod.POST)
    ResponseEntity<Profile> updateNihToken(@ApiParam(value = "the token retrieved from NIH"  )  @Valid @RequestBody NihToken token);


    @ApiOperation(value = "", notes = "Updates a users page visits", response = Profile.class, authorizations = {
        @Authorization(value = "aou_oauth", scopes = {
            
            })
    }, tags={ "profile", })
    @ApiResponses(value = { 
        @ApiResponse(code = 200, message = "The user's profile.", response = Profile.class),
        @ApiResponse(code = 400, message = "Bad request", response = ErrorResponse.class) })
    
    @RequestMapping(value = "/v1/page-visits",
        produces = { "application/json" }, 
        consumes = { "application/json" },
        method = RequestMethod.POST)
    ResponseEntity<Profile> updatePageVisits(@ApiParam(value = "the users pageVisits"  )  @Valid @RequestBody PageVisit pageVisit);


    @ApiOperation(value = "", notes = "Updates a users profile", response = Void.class, authorizations = {
        @Authorization(value = "aou_oauth", scopes = {
            
            })
    }, tags={ "profile", })
    @ApiResponses(value = { 
        @ApiResponse(code = 204, message = "Request received.", response = Void.class),
        @ApiResponse(code = 400, message = "Bad request", response = ErrorResponse.class) })
    
    @RequestMapping(value = "/v1/profile",
        produces = { "application/json" }, 
        consumes = { "application/json" },
        method = RequestMethod.PATCH)
    ResponseEntity<Void> updateProfile(@ApiParam(value = "the new profile to use"  )  @Valid @RequestBody Profile updatedProfile);

}

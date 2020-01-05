package org.pmiops.workbench.api;

import org.pmiops.workbench.model.CheckClustersResponse;

import io.swagger.annotations.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * A delegate to be called by the {@link OfflineClusterApiController}}.
 * Should be implemented as a controller but without the {@link org.springframework.stereotype.Controller} annotation.
 * Instead, use spring to autowire this class into the {@link OfflineClusterApiController}.
 */
@javax.annotation.Generated(value = "io.swagger.codegen.languages.SpringCodegen", date = "2020-01-05T11:49:26.055-05:00")

public interface OfflineClusterApiDelegate {

    /**
     * @see OfflineClusterApi#checkClusters
     */
    ResponseEntity<CheckClustersResponse> checkClusters();

}

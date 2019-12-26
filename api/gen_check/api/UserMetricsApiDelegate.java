package org.pmiops.workbench.api;

import org.pmiops.workbench.model.EmptyResponse;
import org.pmiops.workbench.model.RecentResource;
import org.pmiops.workbench.model.RecentResourceRequest;
import org.pmiops.workbench.model.RecentResourceResponse;

import io.swagger.annotations.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * A delegate to be called by the {@link UserMetricsApiController}}.
 * Should be implemented as a controller but without the {@link org.springframework.stereotype.Controller} annotation.
 * Instead, use spring to autowire this class into the {@link UserMetricsApiController}.
 */
@javax.annotation.Generated(value = "io.swagger.codegen.languages.SpringCodegen", date = "2019-12-26T15:08:16.594-06:00")

public interface UserMetricsApiDelegate {

    /**
     * @see UserMetricsApi#deleteRecentResource
     */
    ResponseEntity<EmptyResponse> deleteRecentResource(String workspaceNamespace,
        String workspaceId,
        RecentResourceRequest recentResourceRequest);

    /**
     * @see UserMetricsApi#getUserRecentResources
     */
    ResponseEntity<RecentResourceResponse> getUserRecentResources();

    /**
     * @see UserMetricsApi#updateRecentResource
     */
    ResponseEntity<RecentResource> updateRecentResource(String workspaceNamespace,
        String workspaceId,
        RecentResourceRequest recentResourceRequest);

}

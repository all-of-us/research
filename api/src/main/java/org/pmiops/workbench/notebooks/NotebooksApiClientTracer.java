package org.pmiops.workbench.notebooks;

import com.squareup.okhttp.Call;
import com.squareup.okhttp.Response;
import io.opencensus.common.Scope;
import io.opencensus.trace.Tracer;
import io.opencensus.trace.Tracing;
import java.io.IOException;
import java.lang.reflect.Type;

public class NotebooksApiClientTracer extends ApiClient {
  private static final Tracer tracer = Tracing.getTracer();

  @Override
  public <T> ApiResponse<T> execute(Call call, Type returnType) throws ApiException {
    Response response;
    T data;
    try (Scope ss =
        tracer
            .spanBuilderWithExplicitParent("NotebooksApiCall", tracer.getCurrentSpan())
            .startScopedSpan()) {
      response = call.execute();
      data = handleResponseWithTracing(response, returnType);
    } catch (IOException e) {
      throw new ApiException(e);
    }
    return new ApiResponse<>(response.code(), response.headers().toMultimap(), data);
  }

  private <T> T handleResponseWithTracing(Response response, Type returnType) throws ApiException {
    String targetUrl = response.request().httpUrl().encodedPath();
    try (Scope urlSpan =
        tracer
            .spanBuilderWithExplicitParent(
                "Response Received: ".concat(targetUrl), tracer.getCurrentSpan())
            .startScopedSpan()) {
      return super.handleResponse(response, returnType);
    }
  }
}

package org.pmiops.workbench.config;

import java.util.logging.Level;
import java.util.logging.Logger;
import org.pmiops.workbench.exceptions.ExceptionUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.RetryContext;
import org.springframework.retry.backoff.BackOffPolicy;
import org.springframework.retry.backoff.ExponentialRandomBackOffPolicy;
import org.springframework.retry.backoff.Sleeper;
import org.springframework.retry.backoff.ThreadWaitSleeper;
import org.springframework.retry.policy.SimpleRetryPolicy;

@Configuration
public class RetryConfig {

  public static abstract class ResponseCodeRetryPolicy
      extends SimpleRetryPolicy {

    private static final Logger logger = Logger.getLogger(ResponseCodeRetryPolicy.class.getName());

    private final String serviceName;

    public ResponseCodeRetryPolicy(String serviceName) {
      this.serviceName = serviceName;
    }

    @Override
    public boolean canRetry(RetryContext context) {
      // canRetry is (counter-intuitively) invoked before the first attempt;
      // in that scenario, getLastThrowable() returns null and we should proceed.
      if (context.getLastThrowable() == null) {
        return true;
      }
      Throwable lastException = context.getLastThrowable();
      if (canRetry(getResponseCode(lastException))) {
        if (context.getRetryCount() < getMaxAttempts()) {
          logRetry(context.getRetryCount(), lastException);
          return true;
        } else {
          logGivingUp(context.getRetryCount(), lastException);
          return false;
        }
      } else {
        logNoRetry(lastException);
        return false;
      }
    }

    protected boolean canRetry(int code) {
      return ExceptionUtils.isServiceUnavailable(code);
    }

    protected void logRetry(int retryCount, Throwable t) {
      logger.log(Level.WARNING,
          String.format("%s unavailable, retrying after %d attempts", serviceName, retryCount), t);
    }

    protected void logGivingUp(int retryCount, Throwable t) {
      logger.log(Level.WARNING,
          String.format("%s unavailable, giving up after %d attempts", serviceName, retryCount), t);
    }

    protected void logNoRetry(Throwable t) {
      logger.log(Level.SEVERE, String.format("Exception calling %s", serviceName), t);
    }

    protected abstract int getResponseCode(Throwable lastException);
  }

  @Bean
  public Sleeper sleeper() {
    return new ThreadWaitSleeper();
  }

  @Bean
  public BackOffPolicy backOffPolicy(Sleeper sleeper) {
    // Defaults to 100ms initial interval, doubling each time, with some random multiplier.
    ExponentialRandomBackOffPolicy policy = new ExponentialRandomBackOffPolicy();
    // Set max interval of 20 seconds.
    policy.setMaxInterval(20000);
    policy.setSleeper(sleeper);
    return policy;
  }
}

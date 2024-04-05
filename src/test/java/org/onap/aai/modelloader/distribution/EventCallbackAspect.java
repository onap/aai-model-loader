package org.onap.aai.modelloader.distribution;

import java.util.concurrent.CountDownLatch;

import org.aspectj.lang.annotation.After;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

import lombok.Getter;
import lombok.Setter;

@Aspect
@Getter
@Setter
@Component
/**
 * Aspect to asynchronously wait for the EventCallback being called after a distribution event from sdc was published
 */
public class EventCallbackAspect {

  private CountDownLatch countDownLatch;

  @After(
      "execution(* org.onap.aai.modelloader.notification.EventCallback.activateCallback(..))")
  private void afterEventCallbackCalled() {
    if (countDownLatch != null) {
      countDownLatch.countDown();
    }
  }
}
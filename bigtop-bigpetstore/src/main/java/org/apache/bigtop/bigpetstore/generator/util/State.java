package org.apache.bigtop.bigpetstore.generator.util;

import java.util.Random;


/**
 * Each "state" has a pet store , with a certain "proportion" of the
 * transactions.
 */
public enum State {
  // Each state is associated with a relative probability.
  AZ(.1f),
  AK(.1f),
  CT(.1f),
  OK(.1f),
  CO(.1f),
  CA(.3f),
  NY(.2f);

  public static Random rand = new Random();
  public float probability;

  private State(float probability) {
    this.probability = probability;
  }
}

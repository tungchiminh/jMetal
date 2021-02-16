package org.uma.jmetal.util.point;

import org.uma.jmetal.solution.Solution;

import java.util.*;

/**
 * Solution used to wrap a {@link Point} object. Only objectives are used.
 *
 * @author Antonio J. Nebro
 */
@SuppressWarnings("serial")
public class PointSolution implements Solution<Double> {
  private int numberOfObjectives;
  private double[] objectives;
  protected Map<Object, Object> attributes;

  @Override
  public List<Double> variables() {
    return null;
  }

  @Override
  public double[] objectives() {
    return objectives;
  }

  @Override
  public double[] constraints() {
    return null;
  }

  @Override
  public Map<Object, Object> attributes() {
    return attributes;
  }

  /**
   * Constructor
   *
   * @param numberOfObjectives
   */
  public PointSolution(int numberOfObjectives) {
    this.numberOfObjectives = numberOfObjectives;
    objectives = new double[numberOfObjectives];
    attributes = new HashMap<>();
  }

  /**
   * Constructor
   *
   * @param point
   */
  public PointSolution(Point point) {
    this.numberOfObjectives = point.getDimension();
    objectives = new double[numberOfObjectives];

    for (int i = 0; i < numberOfObjectives; i++) {
      this.objectives[i] = point.getValue(i);
    }
  }

  /**
   * Constructor
   *
   * @param solution
   */
  public PointSolution(Solution<?> solution) {
    this.numberOfObjectives = solution.objectives().length;
    objectives = new double[numberOfObjectives];

    for (int i = 0; i < numberOfObjectives; i++) {
      this.objectives[i] = solution.getObjective(i);
    }
  }

  /**
   * Copy constructor
   *
   * @param point
   */
  public PointSolution(PointSolution point) {
    this(point.objectives().length);

    for (int i = 0; i < numberOfObjectives; i++) {
      this.objectives[i] = point.getObjective(i);
    }
  }

  @Override
  public void setObjective(int index, double value) {
    objectives[index] = value;
  }

  @Override
  public double getObjective(int index) {
    return objectives[index];
  }

  @Override
  public Double getVariable(int index) {
    return null;
  }

  @Override
  public void setVariable(int index, Double value) {
    // This method is an intentionally-blank override.
  }

  @Override
  public void setConstraint(int index, double value) {}

  @Override
  public PointSolution copy() {
    return new PointSolution(this);
  }

  @Override
  public Object getAttribute(Object id) {
    return attributes.get(id);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    PointSolution that = (PointSolution) o;

    if (numberOfObjectives != that.numberOfObjectives) return false;
    if (!Arrays.equals(objectives, that.objectives)) return false;

    return true;
  }

  @Override
  public int hashCode() {
    return Arrays.hashCode(objectives);
  }

  @Override
  public String toString() {
    return Arrays.toString(objectives);
  }
}

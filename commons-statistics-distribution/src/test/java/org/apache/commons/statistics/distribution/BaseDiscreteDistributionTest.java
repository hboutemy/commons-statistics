/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.commons.statistics.distribution;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Properties;
import java.util.function.Function;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import java.util.stream.Stream.Builder;
import org.apache.commons.math3.util.MathArrays;
import org.apache.commons.rng.simple.RandomSource;
import org.apache.commons.statistics.distribution.DistributionTestData.DiscreteDistributionTestData;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Abstract base class for {@link DiscreteDistribution} tests.
 *
 * <p>This class uses parameterized tests that are repeated for instances of a
 * distribution. The distribution, test input and expected values are generated
 * dynamically from properties files loaded from resources.
 *
 * <p>The class has a single instance (see {@link Lifecycle#PER_CLASS}) that loads properties
 * files from resources on creation. Resource files are assumed to be in the corresponding package
 * for the class and named sequentially from 1:
 * <pre>
 * test.distname.1.properties
 * test.distname.2.properties
 * </pre>
 * <p>Where {@code distname} is the name of the distribution. The name is dynamically
 * created in {@link #getDistributionName()} and can be overridden by implementing classes.
 * A single parameterization of a distribution is tested using a single properties file.
 *
 * <p>To test a distribution create a sub-class and override the following methods:
 * <ul>
 * <li>{@link #makeDistribution(Object...) makeDistribution(Object...)} - Creates the distribution from the parameters
 * <li>{@link #makeInvalidParameters()} - Generate invalid parameters for the distribution
 * <li>{@link #getParameterNames()} - Return the names of parameter accessors
 * </ul>
 *
 * <p>The distribution is created using
 * {@link #makeDistribution(Object...) makeDistribution(Object...)}. This should
 * create an instance of the distribution using parameters defined in the properties file.
 * The parameters are parsed from String values to the appropriate parameter object. Currently
 * this supports Double and Integer; numbers can be unboxed and used to create the distribution.
 *
 * <p>Illegal arguments for the distribution are tested from all combinations provided by
 * {@link #makeInvalidParameters()}. If there are no illegal arguments this method may return
 * null to skip the test.
 *
 * <p>If the distribution provides parameter accessors then the child test class can return
 * the accessor names using {@link #getParameterNames()}. The distribution method accessors
 * will be detected and invoked using reflection. This method may return
 * null to skip the test.
 *
 * <p>The properties file must contain parameters for the distribution, properties of the
 * distribution (moments and bounds) and points to test the CDF and PMF with the expected values.
 * This information can be used to evaluate the distribution CDF and PMF but also the survival
 * function, consistency of the probability computations and random sampling.
 *
 * <p>Optionally:
 * <ul>
 * <li>Points for the PMF (and log PMF) can be specified. The default will use the CDF points.
 * Note: It is not expected that evaluation of the PMF will require different points to the CDF.
 * <li>Points and expected values for the inverse CDF can be specified. These are used in
 * addition to a test of the inverse mapping of the CDF values to the CDF test points. The
 * inverse mapping test can be disabled.
 * <li>Expected values for the log PMF can be specified. The default will use
 * {@link Math#log(double)} on the PMF values.
 * <li>Points and expected values for the survival function can be specified. The default will use
 * the expected CDF values (SF = 1 - CDF).
 * <li>A tolerance for equality assertions. The default is set by {@link #getTolerance()}.
 * <li>A flag to indicate the returned value for {@link DiscreteDistribution#isSupportConnected()}.
 * The default is set by {@link #isSupportConnected()}.
 * </ul>
 *
 * <p>If the distribution provides higher precision implementations of
 * cumulative probability and/or survival probability as the values approach zero, then test
 * points and expected values can be provided with a tolerance for equality assertions of
 * high-precision computations. The default is set by {@link #getHighPrecisionTolerance()}.
 *
 * <p>Note: All properties files are read during test initialization. Any errors in a single
 * property file will throw an exception, invalidating the initialization and no tests
 * will be executed.
 *
 * <p>The parameterized tests in this class are inherited. The tests are final and cannot be
 * changed. This ensures each instance of a distribution is tested for all functionality in
 * the {@link DiscreteDistribution} interface. Arguments to the parameterized tests are
 * generated dynamically using methods of the same name. These can be over-ridden in child
 * classes to alter parameters. Throwing a
 * {@link org.opentest4j.TestAbortedException TestAbortedException} in this method will
 * skip the test as the arguments will not be generated.
 *
 * <p>Each parameterized test is effectively static; it uses no instance data.
 * To implement additional test cases with a specific distribution instance and test
 * data, create a test in the child class and call the relevant test case to verify
 * results. Note that it is recommended to use the properties file as this ensures the
 * entire functionality of the distribution is tested for that parameterization.
 *
 * <p>Test data should be validated against reference tables or other packages where
 * possible, and the source of the reference data and/or validation should be documented
 * in the properties file or additional test cases as appropriate.
 *
 * <p>The properties file uses {@code key=value} pairs loaded using a
 * {@link java.util.Properties} object. Values will be read as String and then parsed to
 * numeric data, and data arrays. Multi-line values can use a {@code \} character.
 * Data in the properties file will be converted to numbers using standard parsing
 * functions appropriate to the primitive type, e.g. {@link Double#parseDouble(String)}.
 * Special double values should use NaN, Infinity and -Infinity.
 *
 * <p>The following is a complete properties file for a distribution:
 * <pre>
 * parameters = 0.5 1.0
 * # Computed using XYZ
 * mean = 1.0
 * variance = NaN
 * # optional (default -Infinity)
 * lower = 0
 * # optional (default Infinity)
 * upper = Infinity
 * # optional (default true or over-ridden in isSupportConnected())
 * connected = false
 * # optional (default 1e-4 or over-ridden in getTolerance())
 * tolerance = 1e-9
 * # optional (default 1e-22 or over-ridden in getHighPrecisionTolerance())
 * tolerance.hp = 1e-30
 * cdf.points = 0, 0.2
 * cdf.values = 0.0, 0.5
 * # optional (default uses cdf.values)
 * pmf.points = 0, 40000
 * pmf.values = 0.0,\
 *  0.0
 * # optional (default uses log pmf.values)
 * logpmf.values = -1900.123, -Infinity
 * # optional (default uses cdf.points and 1 - cdf.values)
 * sf.points = 400
 * sf.values = 0.0
 * # optional high-precision CDF test
 * cdf.hp.points = 1e-16
 * cdf.hp.values = 1.23e-17
 * # optional high-precision survival function test
 * sf.hp.points = 9
 * sf.hp.values = 2.34e-18
 * # optional inverse CDF test (defaults to ignore)
 * icdf.values = 0.0, 0.5
 * ipmf.values = 0.0, 0.2
 * # CDF inverse mapping test (default false)
 * disable.cdf.inverse = false
 * # Sampling test (default false)
 * disable.sample = false
 * # Sampling PMF values test (default false)
 * disable.pmf = false
 * # Sampling CDF values test (default false)
 * disable.cdf = false
 * </pre>
 *
 * <p>See {@link BinomialDistributionTest} for an example and the resource file {@code test.binomial.0.properties}.
 */
@TestInstance(Lifecycle.PER_CLASS)
abstract class BaseDiscreteDistributionTest
    extends BaseDistributionTest<DiscreteDistribution, DiscreteDistributionTestData> {

    @Override
    DiscreteDistributionTestData makeDistributionData(Properties properties) {
        return new DiscreteDistributionTestData(properties);
    }

    //------------------------ Methods to stream the test data -----------------------------

    // The @MethodSource annotation will default to a no arguments method of the same name
    // as the @ParameterizedTest method. These can be overridden by child classes to
    // stream different arguments to the test case.

    /**
     * Create a stream of arguments containing the distribution to test, the CDF test points and
     * the test tolerance.
     *
     * @return the stream
     */
    Stream<Arguments> streamCdfTestPoints() {
        final Builder<Arguments> b = Stream.builder();
        final int[] size = {0};
        data.forEach(d -> {
            final int[] p = d.getCdfPoints();
            if (TestUtils.getLength(p) == 0) {
                return;
            }
            size[0]++;
            b.accept(Arguments.of(namedDistribution(d.getParameters()),
                    namedArray("points", p),
                    createTestTolerance(d)));
        });
        Assumptions.assumeTrue(size[0] != 0, () -> "Distribution has no data for test points");
        return b.build();
    }

    /**
     * Create a stream of arguments containing the distribution to test, the PMF test points
     * and values, and the test tolerance.
     *
     * @return the stream
     */
    Stream<Arguments> testProbability() {
        return stream(DiscreteDistributionTestData::isDisablePmf,
                      DiscreteDistributionTestData::getPmfPoints,
                      DiscreteDistributionTestData::getPmfValues,
                      this::createTestTolerance, "pmf");
    }

    /**
     * Create a stream of arguments containing the distribution to test, the log PMF test points
     * and values, and the test tolerance.
     *
     * @return the stream
     */
    Stream<Arguments> testLogProbability() {
        return stream(DiscreteDistributionTestData::isDisableLogPmf,
                      DiscreteDistributionTestData::getPmfPoints,
                      DiscreteDistributionTestData::getLogPmfValues,
                      this::createTestTolerance, "logpmf");
    }

    /**
     * Create a stream of arguments containing the distribution to test, the CDF test points
     * and values, and the test tolerance.
     *
     * @return the stream
     */
    Stream<Arguments> testCumulativeProbability() {
        return stream(DiscreteDistributionTestData::isDisableCdf,
                      DiscreteDistributionTestData::getCdfPoints,
                      DiscreteDistributionTestData::getCdfValues,
                      this::createTestTolerance, "cdf");
    }

    /**
     * Create a stream of arguments containing the distribution to test, the survival function
     * test points and values, and the test tolerance.
     *
     * <p>This defaults to using the CDF points. The survival function is tested as 1 - CDF.
     *
     * @return the stream
     */
    Stream<Arguments> testSurvivalProbability() {
        return stream(DiscreteDistributionTestData::isDisableSf,
                      DiscreteDistributionTestData::getSfPoints,
                      DiscreteDistributionTestData::getSfValues,
                      this::createTestTolerance, "sf");
    }

    /**
     * Create a stream of arguments containing the distribution to test, the CDF test points
     * and values, and the test tolerance for high-precision computations.
     *
     * @return the stream
     */
    Stream<Arguments> testCumulativeProbabilityHighPrecision() {
        return stream(DiscreteDistributionTestData::getCdfHpPoints,
                      DiscreteDistributionTestData::getCdfHpValues,
                      this::createTestHighPrecisionTolerance, "cdf.hp");
    }

    /**
     * Create a stream of arguments containing the distribution to test, the survival function
     * test points and values, and the test tolerance for high-precision computations.
     *
     * @return the stream
     */
    Stream<Arguments> testSurvivalProbabilityHighPrecision() {
        return stream(DiscreteDistributionTestData::getSfHpPoints,
                      DiscreteDistributionTestData::getSfHpValues,
                      this::createTestHighPrecisionTolerance, "sf.hp");
    }

    /**
     * Create a stream of arguments containing the distribution to test, the inverse CDF test points
     * and values, and the test tolerance.
     *
     * @return the stream
     */
    Stream<Arguments> testInverseCumulativeProbability() {
        return stream(DiscreteDistributionTestData::getIcdfPoints,
                      DiscreteDistributionTestData::getIcdfValues, "icdf");
    }

    /**
     * Create a stream of arguments containing the distribution to test and the CDF test points.
     *
     * @return the stream
     */
    Stream<Arguments> testCumulativeProbabilityInverseMapping() {
        final Builder<Arguments> b = Stream.builder();
        final int[] size = {0};
        data.forEach(d -> {
            final int[] p = d.getCdfPoints();
            if (d.isDisableCdfInverse() || TestUtils.getLength(p) == 0) {
                return;
            }
            size[0]++;
            b.accept(Arguments.of(namedDistribution(d.getParameters()),
                     namedArray("points", p)));
        });
        Assumptions.assumeTrue(size[0] != 0, () -> "Distribution has no data for cdf test points");
        return b.build();
    }

    /**
     * Create a stream of arguments containing the distribution to test, the test points
     * to evaluate the CDF and survival function, and the test tolerance. CDF + SF must equal 1.
     *
     * @return the stream
     */
    Stream<Arguments> testSurvivalAndCumulativeProbabilityComplement() {
        // This is not disabled based on isDisableCdf && isDisableSf.
        // Those flags are intended to ignore tests against reference values.
        return streamCdfTestPoints();
    }

    /**
     * Create a stream of arguments containing the distribution to test, the test points
     * to evaluate the CDF and probability in a range, and the test tolerance.
     * Used to test CDF(x1) - CDF(x0) = probability(x0, x1).
     *
     * @return the stream
     */
    Stream<Arguments> testConsistency() {
        // This is not disabled based on isDisableCdf.
        // That flags is intended to ignore tests against reference values.
        return streamCdfTestPoints();
    }

    /**
     * Create a stream of arguments containing the distribution to test and the test tolerance.
     *
     * @return the stream
     */
    Stream<Arguments> testOutsideSupport() {
        return data.stream().map(d -> Arguments.of(namedDistribution(d.getParameters()), createTestTolerance(d)));
    }

    /**
     * Create a stream of arguments containing the distribution to test, the PMF test points
     * and values. The sampled PMF should sum to more than 50% of the distribution.
     *
     * @return the stream
     */
    Stream<Arguments> testSampling() {
        return stream(DiscreteDistributionTestData::isDisableSample,
                      DiscreteDistributionTestData::getPmfPoints,
                      DiscreteDistributionTestData::getPmfValues, "pmf sampling");
    }

    /**
     * Stream the arguments to test the probability sums. The test
     * sums the probability mass function between consecutive test points for the cumulative
     * density function. The default tolerance is 1e-9. Override this method to change
     * the tolerance.
     *
     * <p>This is disabled by {@link DiscreteDistributionTestData#isDisablePmf()}. If
     * the distribution cannot compute the PMF to match reference values then it
     * is assumed a sum of the PMF will fail to match reference CDF values.
     *
     * @return the stream
     */
    Stream<Arguments> testProbabilitySums() {
        // TODO: Revise tolerance (e.g. using relative error)
        // Use a higher tolerance than the default of 1e-4 for the sums
        final Function<DiscreteDistributionTestData, DoubleTolerance> tolerance =
            d -> DoubleTolerances.absolute(1e-9);
        return stream(DiscreteDistributionTestData::isDisablePmf,
                      DiscreteDistributionTestData::getCdfPoints,
                      DiscreteDistributionTestData::getCdfValues,
                      tolerance, "pmf sums");
    }

    /**
     * Create a stream of arguments containing the distribution to test, the support
     * lower and upper bound, and the support connect flag.
     *
     * @return the stream
     */
    Stream<Arguments> testSupport() {
        return data.stream().map(d -> {
            return Arguments.of(namedDistribution(d.getParameters()), d.getLower(), d.getUpper(), d.isConnected());
        });
    }

    /**
     * Create a stream of arguments containing the distribution to test, the mean
     * and variance, and the test tolerance.
     *
     * @return the stream
     */
    Stream<Arguments> testMoments() {
        return data.stream().map(d -> {
            return Arguments.of(namedDistribution(d.getParameters()), d.getMean(), d.getVariance(), createTestTolerance(d));
        });
    }

    //------------------------ Tests -----------------------------

    // Tests are final. It is expected that the test can be modified by overriding
    // the method used to stream the arguments, for example to use a specific tolerance
    // for a test in preference to the tolerance defined in the properties file.

    // Extract the tests from the previous abstract test

    /**
     * Test that probability calculations match expected values.
     */
    @ParameterizedTest
    @MethodSource
    final void testProbability(DiscreteDistribution dist,
                               int[] points,
                               double[] values,
                               DoubleTolerance tolerance) {
        for (int i = 0; i < points.length; i++) {
            final int x = points[i];
            TestUtils.assertEquals(values[i],
                dist.probability(x), tolerance,
                () -> "Incorrect probability mass value returned for " + x);
        }
    }

    /**
     * Test that logarithmic probability calculations match expected values.
     */
    @ParameterizedTest
    @MethodSource
    final void testLogProbability(DiscreteDistribution dist,
                                  int[] points,
                                  double[] values,
                                  DoubleTolerance tolerance) {
        for (int i = 0; i < points.length; i++) {
            final int x = points[i];
            TestUtils.assertEquals(values[i],
                dist.logProbability(x), tolerance,
                () -> "Incorrect log probability mass value returned for " + x);
        }
    }

    /**
     * Test that cumulative probability density calculations match expected values.
     */
    @ParameterizedTest
    @MethodSource
    final void testCumulativeProbability(DiscreteDistribution dist,
                                         int[] points,
                                         double[] values,
                                         DoubleTolerance tolerance) {
        // verify cumulativeProbability(double)
        for (int i = 0; i < points.length; i++) {
            final int x = points[i];
            TestUtils.assertEquals(values[i],
                dist.cumulativeProbability(x),
                tolerance,
                () -> "Incorrect cumulative probability value returned for " + x);
        }
        // verify probability(double, double)
        for (int i = 0; i < points.length; i++) {
            final int x0 = points[i];
            for (int j = 0; j < points.length; j++) {
                final int x1 = points[j];
                if (x0 <= x1) {
                    TestUtils.assertEquals(
                        values[j] - values[i],
                        dist.probability(x0, x1),
                        tolerance);
                } else {
                    Assertions.assertThrows(IllegalArgumentException.class,
                        () -> dist.probability(x0, x1),
                        "distribution.probability(int, int) should have thrown an exception that first argument is too large");
                }
            }
        }
    }

    /**
     * Test that survival probability density calculations match expected values.
     */
    @ParameterizedTest
    @MethodSource
    final void testSurvivalProbability(DiscreteDistribution dist,
                                       int[] points,
                                       double[] values,
                                       DoubleTolerance tolerance) {
        for (int i = 0; i < points.length; i++) {
            final double x = points[i];
            TestUtils.assertEquals(
                values[i],
                dist.survivalProbability(points[i]),
                tolerance,
                () -> "Incorrect survival probability value returned for " + x);
        }
    }

    /**
     * Test that CDF is simply not 1-survival function by testing values that would result
     * with inaccurate results if simply calculating 1-survival function.
     */
    @ParameterizedTest
    @MethodSource
    final void testCumulativeProbabilityHighPrecision(DiscreteDistribution dist,
                                                      int[] points,
                                                      double[] values,
                                                      DoubleTolerance tolerance) {
        for (int i = 0; i < points.length; i++) {
            final int x = points[i];
            TestUtils.assertEquals(
                values[i],
                dist.cumulativeProbability(x),
                tolerance,
                () -> "cumulative probability is not precise for value " + x);
        }
    }

    /**
     * Test that survival is simply not 1-cdf by testing calculations that would underflow
     * that calculation and result in an inaccurate answer.
     */
    @ParameterizedTest
    @MethodSource
    final void testSurvivalProbabilityHighPrecision(DiscreteDistribution dist,
                                                    int[] points,
                                                    double[] values,
                                                    DoubleTolerance tolerance) {
        for (int i = 0; i < points.length; i++) {
            final int x = points[i];
            TestUtils.assertEquals(
                values[i],
                dist.survivalProbability(x),
                tolerance,
                () -> "survival probability is not precise for value " + x);
        }
    }

    /**
     * Test that inverse cumulative probability density calculations match expected values.
     *
     * <p>Note: Any expected values outside the support of the distribution are ignored.
     */
    @ParameterizedTest
    @MethodSource
    final void testInverseCumulativeProbability(DiscreteDistribution dist,
                                                double[] points,
                                                int[] values) {
        final int lower = dist.getSupportLowerBound();
        final int upper = dist.getSupportUpperBound();
        for (int i = 0; i < points.length; i++) {
            final double x = values[i];
            if (x < lower || x > upper) {
                continue;
            }
            final double p = points[i];
            Assertions.assertEquals(
                x,
                dist.inverseCumulativeProbability(p),
                () -> "Incorrect inverse cumulative probability value returned for " + p);
        }
    }

    /**
     * Test that an inverse mapping of the cumulative probability density values matches
     * the original point, {@code x = icdf(cdf(x))}.
     *
     * <p>Note: It is possible for two points to compute the same CDF value. In this
     * case the mapping is not a bijection. Any points computing a CDF=1 are ignored
     * as this is expected to be inverted to the domain bound.
     *
     * <p>Note: Any points outside the support of the distribution are ignored.
     */
    @ParameterizedTest
    @MethodSource
    final void testCumulativeProbabilityInverseMapping(DiscreteDistribution dist,
                                                       int[] points) {
        final int lower = dist.getSupportLowerBound();
        final int upper = dist.getSupportUpperBound();
        for (int i = 0; i < points.length; i++) {
            final int x = points[i];
            if (x < lower || x > upper) {
                continue;
            }
            final double p = dist.cumulativeProbability(x);
            if (p == 1.0) {
                // Assume mapping not a bijection and ignore
                continue;
            }
            final double x1 = dist.inverseCumulativeProbability(p);
            Assertions.assertEquals(
                x,
                x1,
                () -> "Incorrect CDF inverse value returned for " + p);
        }
    }

    /**
     * Test that cumulative probability density and survival probability calculations
     * sum to approximately 1.0.
     */
    @ParameterizedTest
    @MethodSource
    final void testSurvivalAndCumulativeProbabilityComplement(DiscreteDistribution dist,
                                                              int[] points,
                                                              DoubleTolerance tolerance) {
        for (final int x : points) {
            TestUtils.assertEquals(
                1.0,
                dist.survivalProbability(x) + dist.cumulativeProbability(x),
                tolerance,
                () -> "survival + cumulative probability were not close to 1.0 for " + x);
        }
    }

    /**
     * Test that probability computations are consistent.
     * This checks CDF(x, x) = 0 and CDF(x1) - CDF(x0) = probability(x0, x1).
     */
    @ParameterizedTest
    @MethodSource
    final void testConsistency(DiscreteDistribution dist,
                               int[] points,
                               DoubleTolerance tolerance) {
        for (int i = 1; i < points.length; i++) {

            // check that cdf(x, x) = 0
            final int x = points[i];
            Assertions.assertEquals(
                0.0,
                dist.probability(x, x),
                () -> "Non-zero probability(x, x) for " + x);

            // check that P(a < X <= b) = P(X <= b) - P(X <= a)
            final int upper = Math.max(points[i], points[i - 1]);
            final int lower = Math.min(points[i], points[i - 1]);
            final double diff = dist.cumulativeProbability(upper) -
                                dist.cumulativeProbability(lower);
            final double direct = dist.probability(lower, upper);
            TestUtils.assertEquals(diff, direct, tolerance,
                () -> "Inconsistent probability for (" + lower + "," + upper + ")");
        }
    }

    /**
     * Test CDF and inverse CDF values at the edge of the support of the distribution return
     * expected values and the CDF outside the support returns consistent values.
     */
    @ParameterizedTest
    @MethodSource
    final void testOutsideSupport(DiscreteDistribution dist,
                                  DoubleTolerance tolerance) {
        // Test various quantities when the variable is outside the support.
        final int lo = dist.getSupportLowerBound();
        TestUtils.assertEquals(dist.probability(lo), dist.cumulativeProbability(lo), tolerance, () -> "pmf(lower) != cdf(lower) for " + lo);
        Assertions.assertEquals(lo, dist.inverseCumulativeProbability(0.0), "icdf(0.0)");

        if (lo != Integer.MIN_VALUE) {
            final int below = lo - 1;
            Assertions.assertEquals(0.0, dist.probability(below), "pmf(x < lower)");
            Assertions.assertEquals(Double.NEGATIVE_INFINITY, dist.logProbability(below), "logpmf(x < lower)");
            Assertions.assertEquals(0.0, dist.cumulativeProbability(below), "cdf(x < lower)");
            Assertions.assertEquals(1.0, dist.survivalProbability(below), "sf(x < lower)");
        }

        final int hi = dist.getSupportUpperBound();
        Assertions.assertTrue(lo <= hi, "lower <= upper");
        Assertions.assertEquals(1.0, dist.cumulativeProbability(hi), "cdf(upper)");
        Assertions.assertEquals(0.0, dist.survivalProbability(hi), "sf(upper)");
        TestUtils.assertEquals(dist.probability(hi), dist.survivalProbability(hi - 1), tolerance, "sf(upper - 1)");
        Assertions.assertEquals(hi, dist.inverseCumulativeProbability(1.0), "icdf(1.0)");
        if (hi != Integer.MAX_VALUE) {
            final int above = hi + 1;
            Assertions.assertEquals(0.0, dist.probability(above), "pmf(x > upper)");
            Assertions.assertEquals(Double.NEGATIVE_INFINITY, dist.logProbability(above), "logpmf(x > upper)");
            Assertions.assertEquals(1.0, dist.cumulativeProbability(above), "cdf(x > upper)");
            Assertions.assertEquals(0.0, dist.survivalProbability(above), "sf(x > upper)");
        }

        // Test the logProbability at the support bound. This hits edge case coverage for logProbability.
        // It is assumed the log probability may support a value when the plain probability will be zero.
        // So do not test Math.log(dist.probability(x)) == dist.logProbability(x)
        TestUtils.assertEquals(dist.probability(lo), Math.exp(dist.logProbability(lo)), tolerance, "pmf(lower) != exp(logpmf(lower))");
        TestUtils.assertEquals(dist.probability(hi), Math.exp(dist.logProbability(hi)), tolerance, "pmf(upper) != exp(logpmf(upper))");
    }

    /**
     * Test invalid probabilities passed to computations that require a p-value in {@code [0, 1]}
     * or a range where {@code p1 <= p2}.
     */
    @ParameterizedTest
    @MethodSource(value = "streamDistrbution")
    final void testInvalidProbabilities(DiscreteDistribution dist) {
        final int lo = dist.getSupportLowerBound();
        final int hi = dist.getSupportUpperBound();
        if (lo < hi) {
            Assertions.assertThrows(DistributionException.class, () -> dist.probability(hi, lo), "x0 > x1");
        }
        Assertions.assertThrows(DistributionException.class, () -> dist.inverseCumulativeProbability(-1), "p < 0.0");
        Assertions.assertThrows(DistributionException.class, () -> dist.inverseCumulativeProbability(2), "p > 1.0");
    }

    /**
     * Test sampling from the distribution.
     */
    @ParameterizedTest
    @MethodSource
    final void testSampling(DiscreteDistribution dist,
                            int[] points,
                            double[] values) {
        // This test uses the points that are used to test the distribution PMF.
        // The sum of the probability values does not have to be 1 (or very close to 1).
        // Any value generated by the sampler that is not an expected point will
        // be ignored. If the sum of probabilities is above 0.5 then at least half
        // of the samples should be counted and the test will verify these occur with
        // the expected relative frequencies. Note: The expected values are normalised
        // to 1 (i.e. relative frequencies) by the Chi-square test.
        points = points.clone();
        values = values.clone();
        final int length = TestUtils.eliminateZeroMassPoints(points, values);
        final double[] expected = Arrays.copyOf(values, length);

        // This test will not be valid if the points do not represent enough of the PMF.
        // Require at least 50%.
        final double sum = Arrays.stream(expected).sum();
        Assumptions.assumeTrue(sum > 0.5,
            () -> "Not enough of the PMF is tested during sampling: " + sum);

        // Use fixed seed.
        final DiscreteDistribution.Sampler sampler =
                dist.createSampler(RandomSource.XO_SHI_RO_256_PP.create(1234567890L));

        // Edge case for distributions with all mass in a single point
        if (length == 1) {
            final int point = points[0];
            for (int i = 0; i < 20; i++) {
                Assertions.assertEquals(point, sampler.sample());
            }
            return;
        }

        final int sampleSize = 1000;
        MathArrays.scaleInPlace(sampleSize, expected);

        final int[] sample = TestUtils.sample(sampleSize, sampler);

        final long[] counts = new long[length];
        for (int i = 0; i < sampleSize; i++) {
            final int x = sample[i];
            for (int j = 0; j < length; j++) {
                if (x == points[j]) {
                    counts[j]++;
                    break;
                }
            }
        }

        TestUtils.assertChiSquareAccept(points, expected, counts, 0.001);
    }

    /**
     * Test that probability sums match the distribution.
     * The (filtered, sorted) points array is used to source
     * summation limits. The sum of the probability mass function
     * is compared with the probability over the same interval.
     * Test points outside of the domain of the probability function
     * are discarded and large intervals are ignored.
     */
    @ParameterizedTest
    @MethodSource
    final void testProbabilitySums(DiscreteDistribution dist,
                                   int[] points,
                                   double[] values,
                                   DoubleTolerance tolerance) {
        final ArrayList<Integer> integrationTestPoints = new ArrayList<>();
        for (int i = 0; i < points.length; i++) {
            if (Double.isNaN(values[i]) ||
                values[i] < 1e-5 ||
                values[i] > 1 - 1e-5) {
                continue; // exclude sums outside domain.
            }
            integrationTestPoints.add(points[i]);
        }
        Collections.sort(integrationTestPoints);
        for (int i = 1; i < integrationTestPoints.size(); i++) {
            final int x0 = integrationTestPoints.get(i - 1);
            final int x1 = integrationTestPoints.get(i);
            // Ignore large ranges
            if (x1 - x0 > 50) {
                continue;
            }
            final double sum = IntStream.rangeClosed(x0 + 1, x1).mapToDouble(dist::probability).sum();
            TestUtils.assertEquals(dist.probability(x0, x1), sum, tolerance,
                () -> "Invalid probability sum: " + (x0 + 1) + " to " + x1);
        }
    }

    /**
     * Test the support of the distribution matches the expected values.
     */
    @ParameterizedTest
    @MethodSource
    final void testSupport(DiscreteDistribution dist, double lower, double upper, boolean connected) {
        Assertions.assertEquals(lower, dist.getSupportLowerBound(), "lower bound");
        Assertions.assertEquals(upper, dist.getSupportUpperBound(), "upper bound");
        Assertions.assertEquals(connected, dist.isSupportConnected(), "is connected");
    }

    /**
     * Test the moments of the distribution matches the expected values.
     */
    @ParameterizedTest
    @MethodSource
    final void testMoments(DiscreteDistribution dist, double mean, double variance, DoubleTolerance tolerance) {
        TestUtils.assertEquals(mean, dist.getMean(), tolerance, "mean");
        TestUtils.assertEquals(variance, dist.getVariance(), tolerance, "variance");
    }
}

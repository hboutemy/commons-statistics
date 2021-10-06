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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Test cases for {@link ChiSquaredDistribution}.
 * Extends {@link BaseContinuousDistributionTest}. See javadoc of that class for details.
 */
class ChiSquaredDistributionTest extends BaseContinuousDistributionTest {
    @Override
    ContinuousDistribution makeDistribution(Object... parameters) {
        final double df = (Double) parameters[0];
        return new ChiSquaredDistribution(df);
    }

    @Override
    protected double getAbsoluteTolerance() {
        return 1e-9;
    }

    @Override
    Object[][] makeInvalidParameters() {
        return new Object[][] {
            {0.0},
            {-0.1}
        };
    }

    @Override
    String[] getParameterNames() {
        return new String[] {"DegreesOfFreedom"};
    }

    //-------------------- Additional test cases -------------------------------

    @Test
    void testAdditionalDensity() {
        final double[] x = new double[]{-0.1, 1e-6, 0.5, 1, 2, 5};
        // R 2.5:
        // x <- c(-0.1, 1e-6, 0.5, 1, 2, 5)
        // print(dchisq(x, df=1), digits=17)
        checkDensity(1, x, new double[] {
            0.0, 398.942080930342626743, 0.439391289467722435, 0.241970724519143365,
            0.103776874355148693, 0.014644982561926489});
        // print(dchisq(x, df=0.1), digits=17)
        checkDensity(0.1, x, new double[]{
            0, 2.4864539972849805e+04, 7.4642387316120481e-02,
            3.0090777182393683e-02, 9.4472991589506262e-03, 8.8271993957607896e-04});
        // print(dchisq(x, df=2), digits=17)
        checkDensity(2, x, new double[]{0,
            0.49999975000006253, 0.38940039153570244,
            0.30326532985631671, 0.18393972058572117, 0.04104249931194940});
        // print(dchisq(x, df=10), digits=17)
        checkDensity(10, x, new double[]{0,
            1.3020826822918329e-27, 6.3378969976514082e-05,
            7.8975346316749191e-04, 7.6641550244050524e-03, 6.6800942890542614e-02});
        // print(dchisq(x, df=100), digits=17)
        checkDensity(100, x, new double[]{0,
            0.0000000000000000e+00, 2.0200026568141969e-93,
            8.8562141121618944e-79, 3.0239224849774644e-64, 2.1290671364111626e-45});

        // TODO:
        // Add more density checks with large DF and x points around the mean
        // and into overflow for the underlying Gamma distribution.
    }

    private void checkDensity(double df, double[] points, double[] values) {
        final ChiSquaredDistribution dist = new ChiSquaredDistribution(df);
        // Values have many digits above the decimal point so use relative tolerance
        final double tol = 1e-9;
        for (int i = 0; i < points.length; i++) {
            final double x = points[i];
            Assertions.assertEquals(values[i],
                dist.density(x), values[i] * tol,
                () -> "Incorrect probability density value returned for " + x);
        }

    }
}

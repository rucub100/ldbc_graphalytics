/*
 * Copyright 2015 - 2017 Atlarge Research Team,
 * operating at Technische Universiteit Delft
 * and Vrije Universiteit Amsterdam, the Netherlands.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package science.atlarge.graphalytics.util;

/**
 * Memory Utility to track runtime memory usage.
 * See: org.apache.giraph.utils.MemoryUtils
 *
 * @author Wing Lung Ngai
 * @author Ruslan Curbanov, ruslan.curbanov@uni-duesseldorf.de, December 23, 2018
 */
public class MemoryUtil {

    public static String getMemoryStats() {
        double freeMemoryMB = megaBytes(Runtime.getRuntime().freeMemory());
        double totalMemoryMB = megaBytes(Runtime.getRuntime().totalMemory());
        double maxMemoryMB = megaBytes(Runtime.getRuntime().maxMemory());

        return String.format("Memory (free/total/max) = %.2fM / %.2fM / %.2fM",
                freeMemoryMB, totalMemoryMB, maxMemoryMB);
    }

    private static double megaBytes(long bytes) {
        return bytes / 1024.0 / 1024.0;
    }

    public static long getMaxMemoryMB() {
        return Math.round(megaBytes(Runtime.getRuntime().maxMemory()));
    }
}




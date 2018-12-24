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

import org.apache.commons.configuration.Configuration;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import science.atlarge.graphalytics.configuration.ConfigurationUtil;
import science.atlarge.graphalytics.configuration.GraphalyticsExecutionException;
import science.atlarge.graphalytics.execution.BenchmarkRunStatus;
import science.atlarge.graphalytics.execution.RunnerService;

import java.io.*;
import java.lang.management.ManagementFactory;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static java.nio.file.Files.readAllBytes;
import static java.nio.file.Paths.get;

/**
 * @author Wing Lung Ngai
 * @author Ruslan Curbanov, ruslan.curbanov@uni-duesseldorf.de, December 23, 2018
 */
public class ProcessUtil {

    private static final Logger LOG = LogManager.getLogger();

    public static Process initRunner(Class mainClass, List<String> args) {

        Process process = null;
        try {

            String jvm = System.getProperty("java.home") + File.separator + "bin" + File.separator + "java";
            String classpath = System.getProperty("java.class.path");


            List<String> command = new ArrayList<>();
            command.add(jvm);
            command.addAll(getJavaOptions());
            command.add("-Xmx"+ setMaxMemory());
            command.add(mainClass.getCanonicalName());
            command.addAll(args);

            ProcessBuilder processBuilder = new ProcessBuilder(command);
            processBuilder.redirectErrorStream(true);

            Map<String, String> environment = processBuilder.environment();
            environment.put("CLASSPATH", classpath);

            process = processBuilder.redirectOutput(ProcessBuilder.Redirect.PIPE).start();


            return process;
        } catch (IOException e) {
            LOG.error(e);
            e.printStackTrace();
            return null;
        }
    }

    public static void terminateProcess(Process process) {
        process.destroy();
    }

    public static void terminateProcess(Integer processId) throws Exception {
        LOG.warn("Terminating process " + processId + " focibly.");
        Runtime runtime = Runtime.getRuntime();
        if (System.getProperty("os.name").toLowerCase().indexOf("windows") > -1) {
            LOG.warn(String.format("Executing command \"taskkill %s\"", processId));
            runtime.exec("taskkill " + processId);
        } else {
            LOG.warn(String.format("Executing command \"kill -9 %s\"", processId));
            runtime.exec("kill -9 " + processId);
        }
    }

    public static void monitorProcess(Process process, String runId)  {

        final String rId = runId;
        final Process runnerProcess = process;

        Thread thread = new Thread() {
            public void run() {
                InputStream is = runnerProcess.getInputStream();
                InputStreamReader isr = new InputStreamReader(is);
                BufferedReader br = new BufferedReader(isr);
                String line;

                try {
                    while ((line = br.readLine()) != null) {
                        LOG.debug("[Runner "+rId+"] => " + line);

                    }
                } catch (IOException e) {
                    LOG.error(String.format("[Runner %s] => Failed to read from the benchmark runner.", rId));
                }
                try {
                    runnerProcess.waitFor();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

        };
        thread.start();
    }


    public static int getProcessId() {
        String processName = ManagementFactory.getRuntimeMXBean().getName();
        return Integer.parseInt(processName.split("@")[0]);

    }

    public static boolean isProcessAlive(int processId) {

        boolean isAlive = false;

        Runtime runtime = Runtime.getRuntime();
        try {
            Process process = runtime.exec("kill -0 " + processId);
            process.waitFor();
            isAlive =  process.exitValue() == 0;
        } catch (Exception e) {
            LOG.error("Failed to determine if a process is alive.");
            throw new GraphalyticsExecutionException("Benchmark is aborted.", e);
        }
        return isAlive;
    }

    public static boolean isNetworkPortAvailable(int port) {
        try (Socket ignored = new Socket("localhost", port)) {
            return false;
        } catch (IOException ignored) {
            return true;
        }
    }

    /**
     * Set maximum memory size for benchmark runner.
     * Use the configured value if possible, otherwise use 3x the benchmark suite value.
     * By default, the maximum memory of the benchmark suite is set by "MaxHeapSize",
     * roughly equals to 1 / 4 of available memory in Linux systems.
     * @return maximum memory size
     */
    private static String setMaxMemory() {

        String benchmarkSuiteMaxMemory = (MemoryUtil.getMaxMemoryMB() * 3) + "m";

        String configuredMaxMemory = null;
        String BENCHMARK_PROPERTIES_FILE = "benchmark.properties";
        String MAX_MEMORY_KEY = "benchmark.runner.max-memory";

        try {
            Configuration benchmarkConfiguration = ConfigurationUtil.loadConfiguration(BENCHMARK_PROPERTIES_FILE);
            configuredMaxMemory = ConfigurationUtil.getString(benchmarkConfiguration, MAX_MEMORY_KEY);

            if (configuredMaxMemory.trim().isEmpty()) {
                return benchmarkSuiteMaxMemory;
            } else if (!(configuredMaxMemory.endsWith("g") ||
                    configuredMaxMemory.endsWith("m") ||
                    configuredMaxMemory.endsWith("k"))) {
                LOG.error("Failed to parse configuration " + MAX_MEMORY_KEY + ": " + configuredMaxMemory);
                return benchmarkSuiteMaxMemory;
            } else {
                return configuredMaxMemory;
            }

        } catch (Exception e) {
            LOG.error("Failed to found configuration " + MAX_MEMORY_KEY);
            return benchmarkSuiteMaxMemory;
        }
    }

    private static List<String> getJavaOptions() {
        List<String> javaOpts = new ArrayList<String>();

        String BENCHMARK_PROPERTIES_FILE = "benchmark.properties";

        String XX_USE_MEMBAR_KEY = "benchmark.runner.xx-use-membar";
        String LOG4J_CONFIG_FILE_KEY = "benchmark.runner.log4j.configuration.file";


        try {
            Configuration benchmarkConfiguration = ConfigurationUtil.loadConfiguration(BENCHMARK_PROPERTIES_FILE);

            try {
                boolean xxusemembar = ConfigurationUtil.getBoolean(benchmarkConfiguration, XX_USE_MEMBAR_KEY);

                if (xxusemembar) {
                    javaOpts.add("-XX:+UseMembar");
                }
            } catch (Exception e) {
                LOG.warn(e.getMessage());
                javaOpts.add("-XX:+UseMembar");
            }

            try {
                String log4jConfigFile = ConfigurationUtil.getString(benchmarkConfiguration, LOG4J_CONFIG_FILE_KEY);
                javaOpts.add(String.format("-Dlog4j.configurationFile=%s", log4jConfigFile));
            } catch (Exception e) {
                LOG.warn(e.getMessage());
                javaOpts.add("-Dlog4j.configurationFile=config/log4j2.xml");
            }

            // TODO: remove this
            javaOpts.add("-agentlib:jdwp=transport=dt_socket,server=n,suspend=y,address=localhost:1044");

            // TODO: resolve hard coded solution
            javaOpts.add("-Ddxram.config=config/dxram.json");
            javaOpts.add("-Ddxram.m_engineConfig.m_address.m_ip=127.0.0.1");
            javaOpts.add("-Ddxram.m_engineConfig.m_address.m_port=22222");
            javaOpts.add("-Ddxram.m_engineConfig.m_role=Peer");
            //javaOpts.add("-Ddxram.m_componentConfigs[ZookeeperBootComponent].m_connection.m_ip=127.0.0.1");
            //javaOpts.add("-Ddxram.m_componentConfigs[ZookeeperBootComponent].m_connection.m_port=2181");
            javaOpts.add("-Ddxram.m_componentConfigs[BackupComponent].m_backupActive=false");
            javaOpts.add("-Ddxram.m_componentConfigs[BackupComponent].m_availableForBackup=false");
            //javaOpts.add("-Ddxram.m_componentConfigs[NetworkComponent].m_coreConfig.m_device=ethernet");
            //javaOpts.add("-Ddxram.m_componentConfigs[NetworkComponent].m_coreConfig.m_numMessageHandlerThreads=2");
            javaOpts.add("-Ddxram.m_serviceConfigs[MasterSlaveComputeServiceConfig].m_role=none");
            //javaOpts.add("-Ddxram.m_componentConfigs[ChunkComponent].m_keyValueStoreSize.m_value=256");
            //javaOpts.add("-Ddxram.m_componentConfigs[ChunkComponent].m_keyValueStoreSize.m_unit=mb");
            javaOpts.add("-Ddxramdeployscript");
        } catch (Exception e) {
            LOG.error(e.getMessage());
        }

        return javaOpts;
    }
}

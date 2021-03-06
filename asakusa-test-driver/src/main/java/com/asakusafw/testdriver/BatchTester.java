/**
 * Copyright 2011-2012 Asakusa Framework Team.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.asakusafw.testdriver;

import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.asakusafw.compiler.batch.BatchDriver;
import com.asakusafw.compiler.flow.Location;
import com.asakusafw.compiler.testing.BatchInfo;
import com.asakusafw.compiler.testing.DirectBatchCompiler;
import com.asakusafw.compiler.testing.DirectFlowCompiler;
import com.asakusafw.compiler.testing.JobflowInfo;
import com.asakusafw.testdriver.core.VerifyContext;
import com.asakusafw.vocabulary.batch.BatchDescription;

/**
 * バッチ用のテストドライバクラス。
 */
public class BatchTester extends TestDriverBase {

    static final Logger LOG = LoggerFactory.getLogger(BatchTester.class);

    private final Map<String, JobFlowTester> jobFlowMap = new HashMap<String, JobFlowTester>();

    /**
     * コンストラクタ。
     *
     * @param callerClass 呼出元クラス
     */
    public BatchTester(Class<?> callerClass) {
        super(callerClass);
    }

    /**
     * バッチに含まれるジョブフローを指定する。
     *
     * @param name ジョブフロー名。ジョブフロークラスのアノテーションnameの値を指定する。
     * @return ジョブフローテストドライバ。
     */
    public JobFlowTester jobflow(String name) {
        JobFlowTester driver = jobFlowMap.get(name);
        if (driver == null) {
            driver = new JobFlowTester(driverContext.getCallerClass());
            jobFlowMap.put(name, driver);
        }
        return driver;
    }

    /**
     * バッチのテストを実行し、テスト結果を検証します。
     * @param batchDescriptionClass ジョブフロークラスのクラスオブジェクト
     * @throws IllegalStateException バッチのコンパイル、入出力や検査ルールの用意に失敗した場合
     */
    public void runTest(Class<? extends BatchDescription> batchDescriptionClass) {
        try {
            runTestInternal(batchDescriptionClass);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private void runTestInternal(Class<? extends BatchDescription> batchDescriptionClass) throws IOException {
        LOG.info("テストを開始しています: {}", driverContext.getCallerClass().getName());

        // 初期化
        LOG.info("バッチをコンパイルしています: {}", batchDescriptionClass.getName());

        // バッチコンパイラの実行
        BatchDriver batchDriver = BatchDriver.analyze(batchDescriptionClass);
        assertFalse(batchDriver.getDiagnostics().toString(), batchDriver.hasError());

        File compileWorkDir = driverContext.getCompilerWorkingDirectory();
        if (compileWorkDir.exists()) {
            FileUtils.forceDelete(compileWorkDir);
        }

        File compilerOutputDir = new File(compileWorkDir, "output");
        File compilerLocalWorkingDir = new File(compileWorkDir, "build");

        BatchInfo batchInfo = DirectBatchCompiler.compile(
                batchDescriptionClass,
                "test.batch",
                Location.fromPath(driverContext.getClusterWorkDir(), '/'),
                compilerOutputDir,
                compilerLocalWorkingDir,
                Arrays.asList(new File[] {
                        DirectFlowCompiler.toLibraryPath(batchDescriptionClass)
                }),
                batchDescriptionClass.getClassLoader(),
                driverContext.getOptions());

        // ジョブフロー名の検査
        for (String flowId : jobFlowMap.keySet()) {
            if (batchInfo.findJobflow(flowId) == null) {
                throw new IllegalStateException(MessageFormat.format(
                        "ジョブフロー{1}はバッチ{0}に定義されていません",
                        driverContext.getCallerClass().getName(),
                        flowId));
            }
        }

        LOG.info("テスト環境を初期化しています: {}", driverContext.getCallerClass().getName());
        JobflowExecutor executor = new JobflowExecutor(driverContext);
        executor.cleanWorkingDirectory();
        for (JobflowInfo jobflowInfo : batchInfo.getJobflows()) {
            driverContext.prepareCurrentJobflow(jobflowInfo);
            executor.cleanInputOutput(jobflowInfo);
        }

        for (JobflowInfo jobflowInfo : batchInfo.getJobflows()) {
            driverContext.prepareCurrentJobflow(jobflowInfo);
            String flowId = jobflowInfo.getJobflow().getFlowId();
            JobFlowTester tester = jobFlowMap.get(flowId);
            if (tester != null) {
                LOG.debug("ジョブフローの入出力を初期化しています: {}#{}",
                        batchDescriptionClass.getName(), flowId);
                executor.prepareInput(jobflowInfo, tester.inputs);
                executor.prepareOutput(jobflowInfo, tester.outputs);

                LOG.info("ジョブフローを実行しています: {}#{}",
                        batchDescriptionClass.getName(), flowId);
                VerifyContext verifyContext = new VerifyContext(driverContext);
                executor.runJobflow(jobflowInfo);
                verifyContext.testFinished();

                LOG.info("ジョブフローの実行結果を検証しています: {}#{}",
                        batchDescriptionClass.getName(), flowId);
                executor.verify(jobflowInfo, verifyContext, tester.outputs);
            }
        }
    }
}

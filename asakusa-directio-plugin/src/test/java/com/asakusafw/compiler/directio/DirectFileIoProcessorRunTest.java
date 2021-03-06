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
package com.asakusafw.compiler.directio;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Scanner;

import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.Text;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import com.asakusafw.compiler.directio.testing.model.Line1;
import com.asakusafw.compiler.directio.testing.model.Line2;
import com.asakusafw.runtime.directio.DataFormat;
import com.asakusafw.vocabulary.directio.DirectFileInputDescription;
import com.asakusafw.vocabulary.directio.DirectFileOutputDescription;
import com.asakusafw.vocabulary.external.ImporterDescription.DataSize;
import com.asakusafw.vocabulary.flow.In;
import com.asakusafw.vocabulary.flow.Out;

/**
 * Test for {@link DirectFileIoProcessor}.
 */
@RunWith(Parameterized.class)
public class DirectFileIoProcessorRunTest {

    /**
     * Compiler tester.
     */
    @Rule
    public CompilerTester tester = new CompilerTester();

    private final Class<? extends DataFormat<Text>> format;

    /**
     * Returns the parameters.
     * @return the parameters
     */
    @Parameters
    public static List<Object[]> data() {
        return Arrays.asList(new Object[][] {
                { LineFormat.class },
                { LineFileFormat.class },
        });
    }

    /**
     * Creates a new instance.
     * @param format the format.
     */
    public DirectFileIoProcessorRunTest(Class<? extends DataFormat<Text>> format) {
        this.format = format;
    }

    /**
     * simple.
     * @throws Exception if failed
     */
    @Test
    public void simple() throws Exception {
        put("input/input.txt", "1Hello", "2Hello", "3Hello");
        In<Line1> in = tester.input("in1", new Input(format, "input", "*"));
        Out<Line1> out = tester.output("out1", new Output(format, "output", "output.txt"));
        assertThat(tester.runFlow(new IdentityFlow<Line1>(in, out)), is(true));

        List<String> list = get("output/output.txt");
        assertThat(list.size(), is(3));
        assertThat(list, hasItem("1Hello"));
        assertThat(list, hasItem("2Hello"));
        assertThat(list, hasItem("3Hello"));
    }

    /**
     * simple.
     * @throws Exception if failed
     */
    @Test
    public void tiny() throws Exception {
        put("input/input.txt", "1Hello", "2Hello", "3Hello");
        In<Line1> in = tester.input("in1",
                new Input(Line1.class, format, "input", "*", DataSize.TINY));
        Out<Line1> out = tester.output("out1", new Output(format, "output", "output.txt"));
        assertThat(tester.runFlow(new IdentityFlow<Line1>(in, out)), is(true));

        List<String> list = get("output/output.txt");
        assertThat(list.size(), is(3));
        assertThat(list, hasItem("1Hello"));
        assertThat(list, hasItem("2Hello"));
        assertThat(list, hasItem("3Hello"));
    }

    /**
     * multiple input.
     * @throws Exception if failed
     */
    @Test
    public void input_multi() throws Exception {
        put("input/input-1.txt", "1Hello");
        put("input/input-2.txt", "2Hello");
        put("input/input-3.txt", "3Hello");
        put("input/other.txt", "4Hello");
        In<Line1> in = tester.input("in1", new Input(format, "input", "input-*"));
        Out<Line1> out = tester.output("out1", new Output(format, "output", "output.txt"));
        assertThat(tester.runFlow(new IdentityFlow<Line1>(in, out)), is(true));

        List<String> list = get("output/output.txt");
        assertThat(list.size(), is(3));
        assertThat(list, hasItem("1Hello"));
        assertThat(list, hasItem("2Hello"));
        assertThat(list, hasItem("3Hello"));
    }

    /**
     * ordering.
     * @throws Exception if failed
     */
    @Test
    public void order() throws Exception {
        put("input/input.txt", "1Hello", "2Hello", "3Hello");
        In<Line1> in = tester.input("in1", new Input(format, "input", "*"));
        Out<Line1> out = tester.output("out1", new Output(format, "output", "output.txt", "-value"));
        assertThat(tester.runFlow(new IdentityFlow<Line1>(in, out)), is(true));

        assertThat(get("output/output.txt"), is(list("3Hello", "2Hello", "1Hello")));
    }

    /**
     * file partitioning.
     * @throws Exception if failed
     */
    @Test
    public void partition() throws Exception {
        put("input/input.txt", "a1", "b1", "b2", "c1", "c2", "c3");
        In<Line1> in = tester.input("in1", new Input(format, "input", "*"));
        Out<Line1> out = tester.output("out1", new Output(format, "output", "{first}-output.txt", "+value"));
        assertThat(tester.runFlow(new IdentityFlow<Line1>(in, out)), is(true));

        assertThat(get("output/a-output.txt"), is(list("a1")));
        assertThat(get("output/b-output.txt"), is(list("b1", "b2")));
        assertThat(get("output/c-output.txt"), is(list("c1", "c2", "c3")));
    }

    /**
     * file partitioning by random.
     * @throws Exception if failed
     */
    @Test
    public void random() throws Exception {
        List<String> lines = new ArrayList<String>();
        for (int i = 0; i < 1000; i++) {
            lines.add(String.format("%03d", i));
        }
        put("input/input.txt", lines.toArray(new String[lines.size()]));
        In<Line1> in = tester.input("in1", new Input(format, "input", "*"));
        Out<Line1> out = tester.output("out1", new Output(format, "output", "output-[1..4].txt", "+value"));
        assertThat(tester.runFlow(new IdentityFlow<Line1>(in, out)), is(true));

        List<String> o1 = get("output/output-1.txt");
        List<String> o2 = get("output/output-2.txt");
        List<String> o3 = get("output/output-3.txt");
        List<String> o4 = get("output/output-4.txt");

        // probably ok
        assertThat(o1.size(), is(greaterThan(0)));
        assertThat(o2.size(), is(greaterThan(0)));
        assertThat(o3.size(), is(greaterThan(0)));
        assertThat(o4.size(), is(greaterThan(0)));

        List<String> results = new ArrayList<String>();
        results.addAll(o1);
        results.addAll(o2);
        results.addAll(o3);
        results.addAll(o4);

        Collections.sort(results);
        assertThat(results, is(lines));
    }

    /**
     * use variables.
     * @throws Exception if failed
     */
    @Test
    public void variable() throws Exception {
        put("input/input-1.txt", "1Hello");
        put("input/input-2.txt", "2Hello");
        put("input/input-3.txt", "3Hello");
        put("input/other.txt", "4Hello");
        In<Line1> in = tester.input("in1", new Input(format, "${input-dir}", "${input-pattern}"));
        Out<Line1> out = tester.output("out1", new Output(format, "${output-dir}", "${output-pattern}"));

        tester.hadoop.getVariables().defineVariable("input-dir", "input");
        tester.hadoop.getVariables().defineVariable("input-pattern", "input-*");
        tester.hadoop.getVariables().defineVariable("output-dir", "output");
        tester.hadoop.getVariables().defineVariable("output-pattern", "output.txt");
        assertThat(tester.runFlow(new IdentityFlow<Line1>(in, out)), is(true));

        List<String> list = get("output/output.txt");
        assertThat(list.size(), is(3));
        assertThat(list, hasItem("1Hello"));
        assertThat(list, hasItem("2Hello"));
        assertThat(list, hasItem("3Hello"));
    }

    /**
     * dual in/out.
     * @throws Exception if failed
     */
    @Test
    public void dual_io() throws Exception {
        put("input/input-1.txt", "Hello1");
        put("input/input-2.txt", "Hello2");
        In<Line1> in1 = tester.input("in1",
                new Input(Line1.class, format, "input", "input-1.txt", DataSize.LARGE));
        In<Line2> in2 = tester.input("in2",
                new Input(Line2.class, format, "input", "input-2.txt", DataSize.TINY));
        Out<Line1> out1 = tester.output("out1",
                new Output(Line1.class, format, "output-1", "output.txt"));
        Out<Line2> out2 = tester.output("out2",
                new Output(Line2.class, format, "output-2", "output.txt"));
        assertThat(tester.runFlow(new DualIdentityFlow<Line1, Line2>(in1, in2, out1, out2)), is(true));

        assertThat(get("output-1/output.txt"), is(list("Hello1")));
        assertThat(get("output-2/output.txt"), is(list("Hello2")));
    }

    /**
     * input is missing.
     * @throws Exception if failed
     */
    @Test
    public void input_missing() throws Exception {
        In<Line1> in = tester.input("in1", new Input(format, "input", "*"));
        Out<Line1> out = tester.output("out1", new Output(format, "output", "output.txt"));
        assertThat(tester.runFlow(new IdentityFlow<Line1>(in, out)), is(false));
    }

    private List<String> list(String... values) {
        return Arrays.asList(values);
    }

    private Path getPath(String target) {
        return new Path("target/testing/directio-fs", target);
    }

    private List<String> get(String target) throws IOException {
        FileSystem fs = FileSystem.get(tester.hadoop.getConfiguration());
        InputStream input = fs.open(getPath(target));
        try {
            Scanner s = new Scanner(new InputStreamReader(input, "UTF-8"));
            List<String> results = new ArrayList<String>();
            while (s.hasNextLine()) {
                results.add(s.nextLine());
            }
            s.close();
            return results;
        } finally {
            input.close();
        }
    }

    private void put(String target, String... contents) throws IOException {
        FileSystem fs = FileSystem.get(tester.hadoop.getConfiguration());
        OutputStream output = fs.create(getPath(target), true);
        try {
            PrintWriter w = new PrintWriter(new OutputStreamWriter(output, "UTF-8"));
            for (String line : contents) {
                w.println(line);
            }
            w.close();
        } finally {
            output.close();
        }
    }


    private static class Input extends DirectFileInputDescription {

        private final Class<?> modelType;
        private final Class<? extends DataFormat<?>> format;
        private final String basePath;
        private final String resourcePattern;
        private final DataSize dataSize;

        Input(
                Class<?> modelType,
                Class<? extends DataFormat<?>> format,
                String basePath,
                String resourcePattern,
                DataSize dataSize) {
            this.modelType = modelType;
            this.basePath = basePath;
            this.resourcePattern = resourcePattern;
            this.format = format;
            this.dataSize = dataSize;
        }

        Input(
                Class<? extends DataFormat<?>> format,
                String basePath,
                String resourcePattern) {
            this.modelType = Line1.class;
            this.basePath = basePath;
            this.resourcePattern = resourcePattern;
            this.format = format;
            this.dataSize = DataSize.UNKNOWN;
        }

        @Override
        public Class<?> getModelType() {
            return modelType;
        }

        @Override
        public Class<? extends DataFormat<?>> getFormat() {
            return format;
        }

        @Override
        public String getBasePath() {
            return basePath;
        }

        @Override
        public String getResourcePattern() {
            return resourcePattern;
        }

        @Override
        public DataSize getDataSize() {
            return dataSize;
        }
    }

    private static class Output extends DirectFileOutputDescription {

        private final Class<?> modelType;
        private final Class<? extends DataFormat<?>> format;
        private final String basePath;
        private final String resourcePattern;
        private final String[] order;

        Output(
                Class<?> modelType,
                Class<? extends DataFormat<?>> format,
                String basePath,
                String resourcePattern,
                String... order) {
            this.modelType = modelType;
            this.format = format;
            this.basePath = basePath;
            this.resourcePattern = resourcePattern;
            this.order = order;
        }

        Output(
                Class<? extends DataFormat<?>> format,
                String basePath,
                String resourcePattern,
                String... order) {
            this.modelType = Line1.class;
            this.format = format;
            this.basePath = basePath;
            this.resourcePattern = resourcePattern;
            this.order = order;
        }

        @Override
        public Class<?> getModelType() {
            return modelType;
        }

        @Override
        public Class<? extends DataFormat<?>> getFormat() {
            return format;
        }

        @Override
        public String getBasePath() {
            return basePath;
        }

        @Override
        public String getResourcePattern() {
            return resourcePattern;
        }

        @Override
        public List<String> getOrder() {
            return Arrays.asList(order);
        }
    }
}

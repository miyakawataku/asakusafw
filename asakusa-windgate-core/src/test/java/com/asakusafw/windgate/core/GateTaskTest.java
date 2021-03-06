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
package com.asakusafw.windgate.core;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.asakusafw.windgate.core.process.BasicProcessProvider;
import com.asakusafw.windgate.core.process.ProcessProfile;
import com.asakusafw.windgate.core.resource.ResourceProfile;
import com.asakusafw.windgate.core.session.SessionProfile;
import com.asakusafw.windgate.core.vocabulary.FileProcess;
import com.asakusafw.windgate.file.resource.FileResourceProvider;
import com.asakusafw.windgate.file.session.FileSessionProvider;

/**
 * Test for {@link GateTask}.
 */
public class GateTaskTest {

    /**
     * Temporary folder.
     */
    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    /**
     * Simple execution.
     * @throws Exception if failed
     */
    @Test
    public void execute() throws Exception {
        File in = folder.newFile("in");
        File out = folder.newFile("out");
        put(in, "aaa", "bbb", "ccc");

        new GateTask(
                profile(),
                script(p("testing", "fs1", in, "fs2", out)),
                "testing",
                true,
                true,
                new ParameterList()).execute();

        List<String> results = get(out);
        assertThat(results, is(Arrays.asList("aaa", "bbb", "ccc")));
    }

    /**
     * Simple execution.
     * @throws Exception if failed
     */
    @Test
    public void execute_multiple() throws Exception {
        File in1 = folder.newFile("in1");
        File in2 = folder.newFile("in2");
        File out1 = folder.newFile("out1");
        File out2 = folder.newFile("out2");
        put(in1, "aaa", "bbb", "ccc");
        put(in2, "ddd", "eee", "fff");

        new GateTask(
                profile(),
                script(
                        p("testing1", "fs1", in1, "fs1", out1),
                        p("testing2", "fs2", in2, "fs2", out2)),
                "testing",
                true,
                true,
                new ParameterList()).execute();

        assertThat(get(out1), is(Arrays.asList("aaa", "bbb", "ccc")));
        assertThat(get(out2), is(Arrays.asList("ddd", "eee", "fff")));
    }

    /**
     * Import and export.
     * @throws Exception if failed
     */
    @Test
    public void execute_dual() throws Exception {
        File in = folder.newFile("in");
        File temp = folder.newFile("temp");
        File out = folder.newFile("out");
        put(in, "aaa", "bbb", "ccc");

        GateProfile profile = profile();
        GateScript importer = script(p("testing", "fs1", in, "fs2", temp));
        new GateTask(
                profile,
                importer,
                "testing",
                true,
                false,
                new ParameterList()).execute();

        GateScript exporter = script(p("testing", "fs2", temp, "fs1", out));
        new GateTask(
                profile,
                exporter,
                "testing",
                false,
                true,
                new ParameterList()).execute();

        List<String> results = get(out);
        assertThat(results, is(Arrays.asList("aaa", "bbb", "ccc")));
    }

    /**
     * Failed to open a session.
     * @throws Exception if failed
     */
    @Test(expected = IOException.class)
    public void execute_missing_session() throws Exception {
        File in = folder.newFile("in");
        File out = folder.newFile("out");
        new GateTask(
                profile(),
                script(p("testing", "fs1", in, "fs2", out)),
                "testing",
                false,
                false,
                new ParameterList()).execute();
    }

    /**
     * Failed to open the input.
     * @throws Exception if failed
     */
    @Test(expected = IOException.class)
    public void execute_missing_input() throws Exception {
        File in = folder.newFile("in");
        File out = folder.newFile("out");
        in.delete();
        new GateTask(
                profile(),
                script(p("testing", "fs1", in, "fs2", out)),
                "testing",
                true,
                true,
                new ParameterList()).execute();
    }

    private GateProfile profile() {
        CoreProfile core = new CoreProfile(2);
        SessionProfile session = new SessionProfile(
                FileSessionProvider.class,
                ProfileContext.system(FileSessionProvider.class.getClassLoader()),
                Collections.singletonMap(
                        FileSessionProvider.KEY_DIRECTORY,
                        folder.newFolder("session").getAbsolutePath()));
        List<ProcessProfile> processes = Arrays.asList(new ProcessProfile(
                "default",
                BasicProcessProvider.class,
                ProfileContext.system(BasicProcessProvider.class.getClassLoader()),
                Collections.<String, String>emptyMap()));
        List<ResourceProfile> resources = Arrays.asList(new ResourceProfile[] {
                new ResourceProfile(
                        "fs1",
                        FileResourceProvider.class,
                        ProfileContext.system(FileResourceProvider.class.getClassLoader()),
                        Collections.<String, String>emptyMap()),
                new ResourceProfile(
                        "fs2",
                        FileResourceProvider.class,
                        ProfileContext.system(FileResourceProvider.class.getClassLoader()),
                        Collections.<String, String>emptyMap()),
        });
        return new GateProfile("default", core, session, processes, resources);
    }

    private File put(File file, String...values) throws IOException {
        FileOutputStream out = new FileOutputStream(file);
        try {
            ObjectOutputStream output = new ObjectOutputStream(out);
            for (String string : values) {
                output.writeObject(string);
            }
            output.close();
        } finally {
            out.close();
        }
        return file;
    }

    private List<String> get(File file) throws IOException {
        FileInputStream in = new FileInputStream(file);
        try {
            List<String> results = new ArrayList<String>();
            ObjectInputStream input = new ObjectInputStream(in);
            while (true) {
                try {
                    String value = (String) input.readObject();
                    results.add(value);
                } catch (ClassNotFoundException e) {
                    throw new AssertionError(e);
                } catch (EOFException e) {
                    return results;
                }
            }
        } finally {
            in.close();
        }
    }

    private GateScript script(ProcessScript<?>... processes) {
        return new GateScript("testing", Arrays.asList(processes));
    }

    private ProcessScript<?> p(String name, String sourceName, File sourceFile, String drainName, File drainFile) {
        return new ProcessScript<String>(
                name, "default", String.class,
                d(sourceName, sourceFile),
                d(drainName, drainFile));
    }

    private DriverScript d(String name, File file) {
        return new DriverScript(name, Collections.singletonMap(FileProcess.FILE.key(), file.getPath()));
    }
}

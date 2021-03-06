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
package com.asakusafw.runtime.stage.collector;

import java.io.IOException;

import org.apache.hadoop.io.Writable;
import org.apache.hadoop.mapreduce.Reducer;

import com.asakusafw.runtime.core.Result;
import com.asakusafw.runtime.stage.output.StageOutputDriver;

/**
 * スロットごとに出力を振り分ける{@link Reducer}の骨格。
 */
public abstract class SlotSorter extends Reducer<
        SortableSlot, WritableSlot,
        Object, Object> {

    /**
     * {@link #getOutputNames()}のメソッド名。
     */
    public static final String NAME_GET_OUTPUT_NAMES = "getOutputNames";

    /**
     * {@link #createSlotObjects()}のメソッド名。
     */
    public static final String NAME_CREATE_SLOT_OBJECTS = "createSlotObjects";

    private StageOutputDriver output;

    private Writable[] objects;

    private Result<Writable>[] results;

    /**
     * スロット番号毎のオブジェクトの一覧を返す。
     * @return スロット番号毎のオブジェクトの一覧
     */
    protected abstract Writable[] createSlotObjects();

    /**
     * スロット番号毎の出力の名前を返す。
     * @return スロット番号毎の出力の名前の一覧
     */
    protected abstract String[] getOutputNames();

    @Override
    @SuppressWarnings("unchecked")
    protected void setup(Context context) throws IOException, InterruptedException {
        this.objects = createSlotObjects();
        String[] names = getOutputNames();
        if (objects.length != names.length) {
            throw new AssertionError("inconsistent slot object and output");
        }
        this.output = new StageOutputDriver(context);
        this.results = new Result[objects.length];
        for (int i = 0; i < objects.length; i++) {
            String name = names[i];
            if (name != null) {
                results[i] = output.getResultSink(name);
            }
        }
    }

    @Override
    protected void cleanup(Context context) throws IOException, InterruptedException {
        this.output.close();
        this.output = null;
        this.objects = null;
        this.results = null;
    }

    @Override
    protected void reduce(
            SortableSlot key,
            Iterable<WritableSlot> values,
            Context context) throws IOException, InterruptedException {
        int slot = key.getSlot();
        Writable cache = objects[slot];
        Result<Writable> result = results[slot];
        for (WritableSlot holder : values) {
            holder.loadTo(cache);
            result.add(cache);
        }
    }
}

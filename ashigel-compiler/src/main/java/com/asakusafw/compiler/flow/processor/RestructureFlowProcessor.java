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
package com.asakusafw.compiler.flow.processor;

import com.asakusafw.compiler.common.TargetOperator;
import com.asakusafw.compiler.flow.DataClass;
import com.asakusafw.compiler.flow.DataClass.Property;
import com.asakusafw.compiler.flow.LinePartProcessor;
import com.asakusafw.vocabulary.flow.graph.FlowElementPortDescription;
import com.asakusafw.vocabulary.operator.Restructure;
import com.ashigeru.lang.java.model.syntax.Expression;

/**
 * {@link Restructure 再構築演算子}を処理する。
 */
@TargetOperator(Restructure.class)
public class RestructureFlowProcessor extends LinePartProcessor {

    @Override
    public void emitLinePart(Context context) {
        FlowElementPortDescription input = context.getInputPort(Restructure.ID_INPUT);
        FlowElementPortDescription output = context.getOutputPort(Restructure.ID_OUTPUT);
        DataObjectMirror cache = context.createModelCache(output.getDataType());
        context.setOutput(cache.get());

        DataClass sourceType = loadChecked(input);
        DataClass sinkType = loadChecked(output);
        if (sourceType == null || sinkType == null) {
            return;
        }

        context.add(cache.createReset());
        Expression inputObject = context.getInput();
        Expression outputObject = cache.get();
        for (DataClass.Property sourceProperty : sourceType.getProperties()) {
            Property sinkProperty = sinkType.findProperty(sourceProperty.getName());
            if (sinkProperty == null) {
                // skip this property
            } else if (sourceProperty.getType().equals(sinkProperty.getType()) == false) {
                getEnvironment().error(
                        "{0}において、{1}.{2}と{3}.{4}のプロパティ型が一致しません",
                        context.getOperatorDescription().getName(),
                        sourceType,
                        sourceProperty.getName(),
                        sinkType,
                        sinkProperty.getName());
            } else {
                context.add(sinkProperty.createSetter(outputObject,
                        sourceProperty.createGetter(inputObject)));
            }
        }
    }

    private DataClass loadChecked(FlowElementPortDescription port) {
        DataClass resolved = getEnvironment().getDataClasses().load(port.getDataType());
        if (resolved == null) {
            getEnvironment().error(
                    "型{0}を解決できませんでした",
                    port.getDataType());
        }
        return resolved;
    }
}

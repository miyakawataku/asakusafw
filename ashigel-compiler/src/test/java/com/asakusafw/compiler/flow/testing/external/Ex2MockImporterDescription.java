/**
 * Copyright 2011 Asakusa Framework Team.
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
package com.asakusafw.compiler.flow.testing.external;

import java.util.Collections;
import java.util.Set;

import com.asakusafw.compiler.flow.testing.model.Ex2;
import com.asakusafw.vocabulary.external.FileImporterDescription;


/**
 * {@link Ex2}のモックインポーター記述。
 */
public class Ex2MockImporterDescription extends FileImporterDescription {

    @Override
    public Class<?> getModelType() {
        return Ex2.class;
    }

    @Override
    public Set<String> getPaths() {
        return Collections.singleton("target/testing/in/" + getModelType().getSimpleName());
    }
}

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
package test.modelgen.table.io;
import java.io.IOException;

import javax.annotation.Generated;

import test.modelgen.table.model.TempImportTarget2Df;

import com.asakusafw.runtime.io.ModelInput;
import com.asakusafw.runtime.io.RecordParser;
/**
 * TSVファイルなどのレコードを表すファイルを入力として{@link TempImportTarget2Df}を読み出す。
 */
@Generated("ModelInputEmitter:0.0.1")@SuppressWarnings("deprecation") public final class TempImportTarget2DfModelInput
        implements ModelInput<TempImportTarget2Df> {
    /**
     * 内部で利用するパーサー
     */
    private final RecordParser parser;
    /**
     * インスタンスを生成する。
     * @param parser 利用するパーサー
     * @throws IllegalArgumentException 引数にnullが指定された場合
     */
    public TempImportTarget2DfModelInput(RecordParser parser) {
        if(parser == null) {
            throw new IllegalArgumentException();
        }
        this.parser = parser;
    }
    @Override public boolean readTo(TempImportTarget2Df model) throws IOException {
        if(parser.next()== false) {
            return false;
        }
        parser.fill(model.getTempSidOption());
        return true;
    }
    @Override public void close() throws IOException {
        parser.close();
    }
}
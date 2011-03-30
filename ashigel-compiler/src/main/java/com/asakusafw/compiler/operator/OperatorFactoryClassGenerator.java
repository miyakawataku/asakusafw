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
package com.asakusafw.compiler.operator;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.VariableElement;

import com.asakusafw.compiler.common.JavaName;
import com.asakusafw.compiler.common.NameGenerator;
import com.asakusafw.compiler.operator.OperatorProcessor.Context;
import com.asakusafw.vocabulary.flow.Operator;
import com.asakusafw.vocabulary.flow.graph.FlowElementResolver;
import com.asakusafw.vocabulary.flow.graph.OperatorDescription;
import com.asakusafw.vocabulary.flow.graph.ShuffleKey;
import com.ashigeru.lang.java.model.syntax.ConstructorDeclaration;
import com.ashigeru.lang.java.model.syntax.Expression;
import com.ashigeru.lang.java.model.syntax.FieldDeclaration;
import com.ashigeru.lang.java.model.syntax.FormalParameterDeclaration;
import com.ashigeru.lang.java.model.syntax.Javadoc;
import com.ashigeru.lang.java.model.syntax.MethodDeclaration;
import com.ashigeru.lang.java.model.syntax.ModelFactory;
import com.ashigeru.lang.java.model.syntax.NamedType;
import com.ashigeru.lang.java.model.syntax.SimpleName;
import com.ashigeru.lang.java.model.syntax.Statement;
import com.ashigeru.lang.java.model.syntax.Type;
import com.ashigeru.lang.java.model.syntax.TypeBodyDeclaration;
import com.ashigeru.lang.java.model.syntax.TypeDeclaration;
import com.ashigeru.lang.java.model.syntax.TypeParameterDeclaration;
import com.ashigeru.lang.java.model.util.AttributeBuilder;
import com.ashigeru.lang.java.model.util.ExpressionBuilder;
import com.ashigeru.lang.java.model.util.ImportBuilder;
import com.ashigeru.lang.java.model.util.JavadocBuilder;
import com.ashigeru.lang.java.model.util.Models;
import com.ashigeru.lang.java.model.util.TypeBuilder;

/**
 * 演算子ファクトリークラスの情報を構築するジェネレータ。
 */
public class OperatorFactoryClassGenerator extends OperatorClassGenerator {

    /**
     * {@link FlowElementResolver}を保持するフィールド名。
     */
    static final String RESOLVER_FIELD_NAME = "$";

    /**
     * インスタンスを生成する。
     * @param environment 環境オブジェクト
     * @param factory DOMを構築するためのファクトリ
     * @param importer インポート宣言を構築するビルダー
     * @param operatorClass 演算子クラスの情報
     * @throws IllegalArgumentException 引数に{@code null}が含まれる場合
     */
    public OperatorFactoryClassGenerator(
            OperatorCompilingEnvironment environment,
            ModelFactory factory,
            ImportBuilder importer,
            OperatorClass operatorClass) {
        super(environment, factory, importer, operatorClass);
    }

    @Override
    public TypeDeclaration generate() {
        // 先に演算子用の名前空間を退避しておく
        for (OperatorMethod method : operatorClass.getMethods()) {
            importer.resolvePackageMember(Models.append(
                    factory,
                    getClassName(),
                    getObjectClassName(method.getElement())));
        }
        return super.generate();
    }

    @Override
    protected SimpleName getClassName() {
        return util.getFactoryName(operatorClass.getElement());
    }

    @Override
    protected Javadoc createJavadoc() {
        return new JavadocBuilder(factory)
            .linkType(util.t(operatorClass.getElement()))
            .text("に関する演算子ファクトリークラス。")
            .seeType(util.t(operatorClass.getElement()))
            .toJavadoc();
    }

    @Override
    protected List<TypeBodyDeclaration> createMembers() {
        NameGenerator names = new NameGenerator(factory);
        List<TypeBodyDeclaration> results = new ArrayList<TypeBodyDeclaration>();
        for (OperatorMethod method : operatorClass.getMethods()) {
            OperatorProcessor.Context context = new OperatorProcessor.Context(
                    method.getAnnotation(),
                    method.getElement(),
                    factory,
                    importer,
                    names);
            OperatorProcessor processor = method.getProcessor();
            OperatorMethodDescriptor descriptor = processor.describe(context);

            if (descriptor == null) {
                continue;
            }

            TypeDeclaration objectClass = createObjectClass(context, descriptor);
            if (objectClass == null) {
                continue;
            }

            NamedType objectType = (NamedType) importer.resolvePackageMember(
                    Models.append(factory, getClassName(), objectClass.getName()));
            MethodDeclaration factoryMethod = createFactoryMethod(
                    context,
                    descriptor,
                    objectType);
            if (factoryMethod == null) {
                continue;
            }

            results.add(objectClass);
            results.add(factoryMethod);
        }
        return results;
    }

    private TypeDeclaration createObjectClass(
            Context context,
            OperatorMethodDescriptor descriptor) {
        assert context != null;
        assert descriptor != null;
        SimpleName name = getObjectClassName(context.element);
        NamedType objectType = (NamedType) importer.resolvePackageMember(
                Models.append(factory, getClassName(), name));
        List<TypeBodyDeclaration> members = createObjectMembers(
                context,
                descriptor,
                objectType);
        return factory.newClassDeclaration(
                new JavadocBuilder(factory)
                    .inline(descriptor.getDocumentation())
                    .toJavadoc(),
                new AttributeBuilder(factory)
                    .Public()
                    .Static()
                    .Final()
                    .toAttributes(),
                name,
                Collections.<TypeParameterDeclaration>emptyList(),
                null,
                Collections.singletonList(util.t(Operator.class)),
                members);
    }

    private SimpleName getObjectClassName(ExecutableElement element) {
        assert element != null;
        return factory.newSimpleName(JavaName.of(element.getSimpleName().toString()).toTypeName());
    }

    private List<TypeBodyDeclaration> createObjectMembers(
            Context context,
            OperatorMethodDescriptor descriptor,
            NamedType objectType) {
        assert context != null;
        assert descriptor != null;
        assert objectType != null;
        List<TypeBodyDeclaration> results = new ArrayList<TypeBodyDeclaration>();
        results.add(createResolverField(context));
        for (OperatorPortDeclaration var : descriptor.getOutputPorts()) {
            results.add(createObjectOutputField(context, var));
        }
        results.add(createObjectConstructor(context, descriptor, objectType));
        results.add(createRenamer(context, objectType));
        return results;
    }

    private MethodDeclaration createRenamer(Context context, NamedType objectType) {
        assert context != null;
        assert objectType != null;
        SimpleName newName = context.names.create("newName");
        return factory.newMethodDeclaration(
                new JavadocBuilder(factory)
                    .text("この演算子の名前を設定する。")
                    .param(newName)
                        .text("設定する名前")
                    .returns()
                        .text("この演算子オブジェクト (this)")
                    .exception(util.t(IllegalArgumentException.class))
                        .text("引数に")
                        .code("null")
                        .text("が指定された場合")
                    .toJavadoc(),
                new AttributeBuilder(factory)
                    .Public()
                    .toAttributes(),
                objectType,
                factory.newSimpleName("as"),
                Collections.singletonList(factory.newFormalParameterDeclaration(
                        util.t(String.class),
                        newName)),
                Arrays.asList(new Statement[] {
                        new ExpressionBuilder(factory, factory.newThis())
                            .field(RESOLVER_FIELD_NAME)
                            .method("setName", newName)
                            .toStatement(),
                        new ExpressionBuilder(factory, factory.newThis())
                            .toReturnStatement(),
                }));
    }

    private FieldDeclaration createResolverField(Context context) {
        assert context != null;
        return factory.newFieldDeclaration(
                null,
                new AttributeBuilder(factory)
                    .Private()
                    .Final()
                    .toAttributes(),
                util.t(FlowElementResolver.class),
                factory.newSimpleName(RESOLVER_FIELD_NAME),
                null);
    }

    private FieldDeclaration createObjectOutputField(
            Context context,
            OperatorPortDeclaration var) {
        assert context != null;
        assert var != null;
        return factory.newFieldDeclaration(
                new JavadocBuilder(factory)
                    .inline(var.getDocumentation())
                    .toJavadoc(),
                new AttributeBuilder(factory)
                    .Public()
                    .Final()
                    .toAttributes(),
                util.toSourceType(var.getType()),
                factory.newSimpleName(context.names.reserve(var.getName())),
                null);
    }

    private ConstructorDeclaration createObjectConstructor(
            Context context,
            OperatorMethodDescriptor descriptor,
            NamedType objectType) {
        assert context != null;
        assert descriptor != null;
        assert objectType != null;

        List<FormalParameterDeclaration> parameters = createParametersForConstructor(context, descriptor);
        List<Statement> statements = createBodyForConstructor(context, descriptor, parameters);
        return factory.newConstructorDeclaration(
                null,
                new AttributeBuilder(factory).toAttributes(),
                objectType.getName().getLastSegment(),
                parameters,
                statements);
    }

    private List<Statement> createBodyForConstructor(
            Context context,
            OperatorMethodDescriptor descriptor,
            List<FormalParameterDeclaration> parameters) {
        assert context != null;
        assert descriptor != null;
        assert parameters != null;
        List<Statement> statements = new ArrayList<Statement>();
        SimpleName builderName = context.names.create("builder");
        statements.add(new TypeBuilder(factory, util.t(OperatorDescription.Builder.class))
            .newObject(factory.newClassLiteral(util.t(descriptor.getAnnotationType())))
            .toLocalVariableDeclaration(
                    util.t(OperatorDescription.Builder.class),
                    builderName));
        statements.add(new ExpressionBuilder(factory, builderName)
            .method("declare",
                    factory.newClassLiteral(util.t(operatorClass.getElement())),
                    factory.newClassLiteral(factory.newNamedType(
                            util.getImplementorName(operatorClass.getElement()))),
                    util.v(descriptor.getName()))
            .toStatement());
        for (VariableElement parameter : context.element.getParameters()) {
            statements.add(new ExpressionBuilder(factory, builderName)
                .method("declareParameter",
                        new TypeBuilder(factory, util.t(environment.getErasure(parameter.asType())))
                            .dotClass()
                            .toExpression())
                .toStatement());
        }

        for (OperatorPortDeclaration var : descriptor.getInputPorts()) {
            ShuffleKey key = var.getShuffleKey();
            if (key == null) {
                statements.add(new ExpressionBuilder(factory, builderName)
                    .method("addInput",
                            util.v(var.getName()),
                            factory.newClassLiteral(util.t(var.getType())))
                    .toStatement());
            } else {
                statements.add(new ExpressionBuilder(factory, builderName)
                    .method("addInput",
                            util.v(var.getName()),
                            factory.newClassLiteral(util.t(var.getType())),
                            toSource(key))
                    .toStatement());
            }
        }
        for (OperatorPortDeclaration var : descriptor.getOutputPorts()) {
            statements.add(new ExpressionBuilder(factory, builderName)
                .method("addOutput",
                        util.v(var.getName()),
                        factory.newClassLiteral(util.t(var.getType())))
                .toStatement());
        }
        for (OperatorPortDeclaration var : descriptor.getParameters()) {
            statements.add(new ExpressionBuilder(factory, builderName)
                .method("addParameter",
                        util.v(var.getName()),
                        factory.newClassLiteral(util.t(var.getType())),
                        factory.newSimpleName(var.getName()))
                .toStatement());
        }
        for (Expression attr : descriptor.getAttributes()) {
            statements.add(new ExpressionBuilder(factory, builderName)
                .method("addAttribute", attr)
                .toStatement());
        }
        Expression resolver = new ExpressionBuilder(factory, factory.newThis())
            .field(RESOLVER_FIELD_NAME)
            .toExpression();
        statements.add(new ExpressionBuilder(factory, resolver)
            .assignFrom(new ExpressionBuilder(factory, builderName)
                .method("toResolver")
                .toExpression())
            .toStatement());
        for (OperatorPortDeclaration var : descriptor.getInputPorts()) {
            statements.add(new ExpressionBuilder(factory, resolver)
                .method("resolveInput",
                        util.v(var.getName()),
                        factory.newSimpleName(var.getName()))
                .toStatement());
        }
        for (OperatorPortDeclaration var : descriptor.getOutputPorts()) {
            statements.add(new ExpressionBuilder(factory, factory.newThis())
                .field(var.getName())
                .assignFrom(new ExpressionBuilder(factory, resolver)
                    .method("resolveOutput", util.v(var.getName()))
                    .toExpression())
                .toStatement());
        }
        return statements;
    }

    private Expression toSource(ShuffleKey key) {
        assert key != null;
        List<Expression> group = new ArrayList<Expression>();
        for (String property : key.getGroupProperties()) {
            group.add(Models.toLiteral(factory, property));
        }
        List<Expression> order = new ArrayList<Expression>();
        for (ShuffleKey.Order o : key.getOrderings()) {
            order.add(new TypeBuilder(factory, util.t(ShuffleKey.Order.class))
                .newObject(
                        Models.toLiteral(factory, o.getProperty()),
                        new TypeBuilder(factory, util.t(ShuffleKey.Direction.class))
                            .field(o.getDirection().name())
                            .toExpression())
                .toExpression());
        }

        return new TypeBuilder(factory, util.t(ShuffleKey.class))
            .newObject(
                    toList(util.t(String.class), group),
                    toList(util.t(ShuffleKey.Order.class), order))
            .toExpression();
    }

    private Expression toList(Type type, List<Expression> expressions) {
        assert type != null;
        assert expressions != null;
        return new TypeBuilder(factory, util.t(Arrays.class))
            .method("asList", factory.newArrayCreationExpression(
                    factory.newArrayType(type),
                    Collections.<Expression>emptyList(),
                    factory.newArrayInitializer(expressions)))
            .toExpression();
    }

    private List<FormalParameterDeclaration> createParametersForConstructor(
            Context context,
            OperatorMethodDescriptor descriptor) {
        assert context != null;
        assert descriptor != null;
        List<FormalParameterDeclaration> parameters = new ArrayList<FormalParameterDeclaration>();
        for (OperatorPortDeclaration var : descriptor.getInputPorts()) {
            SimpleName name = factory.newSimpleName(context.names.reserve(var.getName()));
            parameters.add(factory.newFormalParameterDeclaration(
                    util.toSourceType(var.getType()),
                    name));
        }
        for (OperatorPortDeclaration var : descriptor.getParameters()) {
            SimpleName name = factory.newSimpleName(context.names.reserve(var.getName()));
            parameters.add(factory.newFormalParameterDeclaration(
                    util.t(var.getType()),
                    name));
        }
        return parameters;
    }

    private MethodDeclaration createFactoryMethod(
            Context context,
            OperatorMethodDescriptor descriptor,
            NamedType objectType) {
        assert context != null;
        assert descriptor != null;
        assert objectType != null;
        JavadocBuilder javadoc = new JavadocBuilder(factory);
        javadoc.inline(descriptor.getDocumentation());
        List<FormalParameterDeclaration> parameters =
                new ArrayList<FormalParameterDeclaration>();
        List<Expression> arguments = new ArrayList<Expression>();
        for (OperatorPortDeclaration var : descriptor.getInputPorts()) {
            SimpleName name = factory.newSimpleName(var.getName());
            javadoc.param(name).inline(var.getDocumentation());
            parameters.add(factory.newFormalParameterDeclaration(
                    util.toSourceType(var.getType()),
                    name));
            arguments.add(name);
        }
        for (OperatorPortDeclaration var : descriptor.getParameters()) {
            SimpleName name = factory.newSimpleName(var.getName());
            javadoc.param(name).inline(var.getDocumentation());
            parameters.add(factory.newFormalParameterDeclaration(
                    util.t(var.getType()),
                    name));
            arguments.add(name);
        }
        javadoc.returns().text("生成した演算子オブジェクト");

        List<Type> rawParameterTypes = new ArrayList<Type>();
        for (VariableElement var : context.element.getParameters()) {
            rawParameterTypes.add(util.t(environment.getErasure(var.asType())));
        }
        javadoc.seeMethod(
                util.t(operatorClass.getElement()),
                descriptor.getName(),
                rawParameterTypes);

        return factory.newMethodDeclaration(
                javadoc.toJavadoc(),
                new AttributeBuilder(factory)
                    .Public()
                    .toAttributes(),
                objectType,
                factory.newSimpleName(JavaName.of(descriptor.getName()).toMemberName()),
                parameters,
                Collections.singletonList(
                        new TypeBuilder(factory, objectType)
                            .newObject(arguments)
                            .toReturnStatement()));
    }
}

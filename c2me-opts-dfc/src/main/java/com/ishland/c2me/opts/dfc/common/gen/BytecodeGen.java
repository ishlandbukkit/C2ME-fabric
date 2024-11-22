package com.ishland.c2me.opts.dfc.common.gen;

import com.ishland.c2me.opts.dfc.common.ast.AstNode;
import com.ishland.c2me.opts.dfc.common.ast.EvalType;
import com.ishland.c2me.opts.dfc.common.ast.McToAst;
import com.ishland.c2me.opts.dfc.common.ast.misc.ConstantNode;
import com.ishland.c2me.opts.dfc.common.ast.misc.RootNode;
import com.ishland.c2me.opts.dfc.common.ast.misc.YClampedGradientNode;
import com.ishland.c2me.opts.dfc.common.ast.noise.DFTNoiseNode;
import com.ishland.c2me.opts.dfc.common.ast.noise.DFTShiftANode;
import com.ishland.c2me.opts.dfc.common.ast.noise.DFTShiftBNode;
import com.ishland.c2me.opts.dfc.common.ast.noise.DFTShiftNode;
import com.ishland.c2me.opts.dfc.common.ast.opto.OptoPasses;
import com.ishland.c2me.opts.dfc.common.util.ArrayCache;
import com.ishland.c2me.opts.dfc.common.vif.AstVanillaInterface;
import it.unimi.dsi.fastutil.Hash;
import it.unimi.dsi.fastutil.Pair;
import it.unimi.dsi.fastutil.ints.IntObjectPair;
import it.unimi.dsi.fastutil.objects.Object2ReferenceMap;
import it.unimi.dsi.fastutil.objects.Object2ReferenceMaps;
import it.unimi.dsi.fastutil.objects.Object2ReferenceOpenCustomHashMap;
import it.unimi.dsi.fastutil.objects.Object2ReferenceOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Reference2ReferenceMap;
import net.minecraft.util.math.Spline;
import net.minecraft.world.gen.densityfunction.DensityFunction;
import net.minecraft.world.gen.densityfunction.DensityFunctionTypes;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.AnalyzerAdapter;
import org.objectweb.asm.commons.InstructionAdapter;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.IntConsumer;
import java.util.stream.Collectors;

public class BytecodeGen {

    private static final File exportDir = new File("./cache/c2me-dfc");

    private static final AtomicLong ordinal = new AtomicLong();

    static {
        try {
            org.spongepowered.asm.util.Files.deleteRecursively(exportDir);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static final Hash.Strategy<AstNode> RELAXED_STRATEGY = new Hash.Strategy<>() {
        @Override
        public int hashCode(AstNode o) {
            return o.relaxedHashCode();
        }

        @Override
        public boolean equals(AstNode a, AstNode b) {
            return a.relaxedEquals(b);
        }
    };
    private static final Object2ReferenceMap<AstNode, Class<?>> compilationCache = Object2ReferenceMaps.synchronize(new Object2ReferenceOpenCustomHashMap<>(RELAXED_STRATEGY));

    public static DensityFunction compile(DensityFunction densityFunction, Reference2ReferenceMap<DensityFunction, DensityFunction> tempCache) {
        DensityFunction cached = tempCache.get(densityFunction);
        if (cached != null) {
            return cached;
        }
        if (densityFunction instanceof AstVanillaInterface vif) {
            AstNode ast = vif.getAstNode();
            return new CompiledDensityFunction(compile0(ast), vif.getBlendingFallback());
        }
        AstNode ast = OptoPasses.optimize(McToAst.toAst(densityFunction));
        if (ast instanceof ConstantNode constantNode) {
            return DensityFunctionTypes.constant(constantNode.getValue());
        } else if (ast instanceof DFTNoiseNode ||
                ast instanceof DFTShiftANode ||
                ast instanceof DFTShiftBNode ||
                ast instanceof DFTShiftNode ||
                ast instanceof YClampedGradientNode) {
            return densityFunction;
        }
        CompiledDensityFunction compiled = new CompiledDensityFunction(compile0(ast), densityFunction);
        tempCache.put(densityFunction, compiled);
        return compiled;
    }

    public static synchronized CompiledEntry compile0(AstNode node) {
        Class<?> cached = compilationCache.get(node);

        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
        String name = cached != null ? String.format("DfcCompiled_discarded") : String.format("DfcCompiled_%d", ordinal.getAndIncrement());
        writer.visit(Opcodes.V21, Opcodes.ACC_PUBLIC | Opcodes.ACC_FINAL, name, null, Type.getInternalName(Object.class), new String[]{Type.getInternalName(CompiledEntry.class)});

        RootNode rootNode = new RootNode(node);

        Context genContext = new Context(writer, name);
        genContext.newSingleMethod0((adapter, localVarConsumer) -> rootNode.doBytecodeGenSingle(genContext, adapter, localVarConsumer), "evalSingle", true);
        genContext.newMultiMethod0((adapter, localVarConsumer) -> rootNode.doBytecodeGenMulti(genContext, adapter, localVarConsumer), "evalMulti", true);

        List<Object> args = genContext.args.entrySet().stream()
                .sorted(Comparator.comparingInt(o -> o.getValue().ordinal()))
                .map(Map.Entry::getKey)
                .collect(Collectors.toCollection(ArrayList::new));

        if (cached != null) {
            try {
                return (CompiledEntry) cached.getConstructor(List.class).newInstance(args);
            } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
                throw new RuntimeException(e);
            }
        }

        genConstructor(genContext);
        genGetArgs(genContext);
        genNewInstance(genContext);
//        genFields(genContext);

        ListIterator<Object> iterator = args.listIterator();
        while (iterator.hasNext()) {
            Object next = iterator.next();
//            if (next instanceof DensityFunctionTypes.Wrapping wrapping && wrapping.type() == DensityFunctionTypes.Wrapping.Type.FLAT_CACHE) {
//                iterator.set(new DensityFunctionTypes.Wrapping(wrapping.type(), compile(wrapping.wrapped())));
//            }
        }

        byte[] bytes = writer.toByteArray();
        dumpClass(genContext.className, bytes);
        Class<?> defined = defineClass(genContext.className, bytes);
        compilationCache.put(node, defined);
        try {
            return (CompiledEntry) defined.getConstructor(List.class).newInstance(args);
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    private static void genConstructor(Context context) {
        InstructionAdapter m = new InstructionAdapter(
                new AnalyzerAdapter(
                        context.className,
                        Opcodes.ACC_PUBLIC,
                        "<init>",
                        Type.getMethodDescriptor(Type.VOID_TYPE, Type.getType(List.class)),
                        context.classWriter.visitMethod(
                                Opcodes.ACC_PUBLIC,
                                "<init>",
                                Type.getMethodDescriptor(Type.VOID_TYPE, Type.getType(List.class)),
                                null,
                                null
                        )
                )
        );

        Label start = new Label();
        Label end = new Label();
        m.visitLabel(start);

        m.load(0, InstructionAdapter.OBJECT_TYPE);
        m.invokespecial(Type.getInternalName(Object.class), "<init>", Type.getMethodDescriptor(Type.VOID_TYPE), false);

        for (Map.Entry<Object, Context.FieldRecord> entry : context.args.entrySet().stream().sorted(Comparator.comparingInt(o -> o.getValue().ordinal())).toList()) {
            String name = entry.getValue().name();
            Class<?> type = entry.getValue().type();
            int ordinal = entry.getValue().ordinal();

            m.load(0, InstructionAdapter.OBJECT_TYPE);
            m.load(1, InstructionAdapter.OBJECT_TYPE);
            m.iconst(ordinal);
            m.invokeinterface(Type.getInternalName(List.class), "get", Type.getMethodDescriptor(InstructionAdapter.OBJECT_TYPE, Type.INT_TYPE));
            m.checkcast(Type.getType(type));
            m.putfield(context.className, name, Type.getDescriptor(type));
        }

        for (String postProcessingMethod : context.postProcessMethods.stream().sorted().toList()) {
            m.load(0, InstructionAdapter.OBJECT_TYPE);
            m.invokevirtual(context.className, postProcessingMethod, "()V", false);
        }

        m.areturn(Type.VOID_TYPE);
        m.visitLabel(end);
        m.visitLocalVariable("this", context.classDesc, null, start, end, 0);
        m.visitLocalVariable("list", Type.getDescriptor(List.class), null, start, end, 1);
        m.visitMaxs(0, 0);
    }

    private static void genGetArgs(Context context) {
        InstructionAdapter m = new InstructionAdapter(
                new AnalyzerAdapter(
                        context.className,
                        Opcodes.ACC_PUBLIC | Opcodes.ACC_FINAL,
                        "getArgs",
                        Type.getMethodDescriptor(Type.getType(List.class)),
                        context.classWriter.visitMethod(
                                Opcodes.ACC_PUBLIC | Opcodes.ACC_FINAL,
                                "getArgs",
                                Type.getMethodDescriptor(Type.getType(List.class)),
                                null,
                                null
                        )
                )
        );

        Label start = new Label();
        Label end = new Label();
        m.visitLabel(start);

        m.anew(Type.getType(ArrayList.class));
        m.dup();
        m.iconst(context.args.size());
        m.invokespecial(Type.getInternalName(ArrayList.class), "<init>", Type.getMethodDescriptor(Type.VOID_TYPE, Type.INT_TYPE), false);
        m.store(1, InstructionAdapter.OBJECT_TYPE);

        for (Map.Entry<Object, Context.FieldRecord> entry : context.args.entrySet().stream().sorted(Comparator.comparingInt(o -> o.getValue().ordinal())).toList()) {
            String name = entry.getValue().name();
            Class<?> type = entry.getValue().type();

            m.load(1, InstructionAdapter.OBJECT_TYPE);
            m.load(0, InstructionAdapter.OBJECT_TYPE);
            m.getfield(context.className, name, Type.getDescriptor(type));
            m.invokeinterface(Type.getInternalName(List.class), "add", Type.getMethodDescriptor(Type.BOOLEAN_TYPE, InstructionAdapter.OBJECT_TYPE));
            m.pop();
        }

        m.load(1, InstructionAdapter.OBJECT_TYPE);
        m.areturn(InstructionAdapter.OBJECT_TYPE);
        m.visitLabel(end);
        m.visitLocalVariable("this", context.classDesc, null, start, end, 0);
        m.visitLocalVariable("list", Type.getDescriptor(List.class), null, start, end, 1);
        m.visitMaxs(0, 0);
    }

    private static void genNewInstance(Context context) {
        InstructionAdapter m = new InstructionAdapter(
                new AnalyzerAdapter(
                        context.className,
                        Opcodes.ACC_PUBLIC | Opcodes.ACC_FINAL,
                        "newInstance",
                        Type.getMethodDescriptor(Type.getType(CompiledEntry.class), Type.getType(List.class)),
                        context.classWriter.visitMethod(
                                Opcodes.ACC_PUBLIC | Opcodes.ACC_FINAL,
                                "newInstance",
                                Type.getMethodDescriptor(Type.getType(CompiledEntry.class), Type.getType(List.class)),
                                null,
                                null
                        )
                )
        );
        Label start = new Label();
        Label end = new Label();
        m.visitLabel(start);

        m.anew(Type.getType(context.classDesc));
        m.dup();
        m.load(1, InstructionAdapter.OBJECT_TYPE);
        m.invokespecial(context.className, "<init>", Type.getMethodDescriptor(Type.VOID_TYPE, Type.getType(List.class)), false);
        m.areturn(InstructionAdapter.OBJECT_TYPE);

        m.visitLabel(end);
        m.visitLocalVariable("this", context.classDesc, null, start, end, 0);
        m.visitLocalVariable("list", Type.getDescriptor(List.class), null, start, end, 1);
        m.visitMaxs(0, 0);
    }

//    private static void genFields(Context context) {
//        for (Map.Entry<Object, Context.FieldRecord> entry : context.args.entrySet().stream().sorted(Comparator.comparingInt(o -> o.getValue().ordinal())).toList()) {
//            String name = entry.getValue().name();
//            Class<?> type = entry.getValue().type();
//
//            context.classWriter.visitField(
//                    Opcodes.ACC_PRIVATE | Opcodes.ACC_FINAL,
//                    name,
//                    Type.getDescriptor(type),
//                    null,
//                    null
//            );
//        }
//    }

    private static void dumpClass(String className, byte[] bytes) {
        File outputFile = new File(exportDir, className + ".class");
        outputFile.getParentFile().mkdirs();
        try {
            com.google.common.io.Files.write(bytes, outputFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static Class<?> defineClass(String className, byte[] bytes) {
        ClassLoader classLoader = new ClassLoader(BytecodeGen.class.getClassLoader()) {
            @Override
            public Class<?> loadClass(String name) throws ClassNotFoundException {
                if (name.equals(className)) {
                    return super.defineClass(name, bytes, 0, bytes.length);
                }

                return super.loadClass(name);
            }
        };

        try {
            return classLoader.loadClass(className);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    public static class Context {
        public static final String SINGLE_DESC = Type.getMethodDescriptor(Type.getType(double.class), Type.getType(int.class), Type.getType(int.class), Type.getType(int.class), Type.getType(EvalType.class));
        public static final String MULTI_DESC = Type.getMethodDescriptor(Type.VOID_TYPE, Type.getType(double[].class), Type.getType(int[].class), Type.getType(int[].class), Type.getType(int[].class), Type.getType(EvalType.class), Type.getType(ArrayCache.class));
        public final ClassWriter classWriter;
        public final String className;
        public final String classDesc;
        private int methodIdx = 0;
        private final Object2ReferenceOpenHashMap<AstNode, String> singleMethods = new Object2ReferenceOpenHashMap<>();
        private final Object2ReferenceOpenHashMap<AstNode, String> multiMethods = new Object2ReferenceOpenHashMap<>();
        private final Object2ReferenceOpenHashMap<Spline<DensityFunctionTypes.Spline.SplinePos, DensityFunctionTypes.Spline.DensityFunctionWrapper>, String> splineMethods = new Object2ReferenceOpenHashMap<>();
        private final ObjectOpenHashSet<String> postProcessMethods = new ObjectOpenHashSet<>();
        private final Reference2ObjectOpenHashMap<Object, FieldRecord> args = new Reference2ObjectOpenHashMap<>();

        public Context(ClassWriter classWriter, String className) {
            this.classWriter = Objects.requireNonNull(classWriter);
            this.className = Objects.requireNonNull(className);
            this.classDesc = String.format("L%s;", this.className);
        }

        public String nextMethodName() {
            return String.format("method_%d", methodIdx++);
        }

        public String nextMethodName(String suffix) {
            return String.format("method_%d_%s", methodIdx++, suffix);
        }

        public ValuesMethodDefD newSingleMethod(AstNode node) {
            if (node instanceof ConstantNode constantNode) {
                return new ValuesMethodDefD(constantNode.getValue());
            } else {
                String generated = this.newSingleMethodUnoptimized(node);
                return new ValuesMethodDefD(generated);
            }
        }

        public String newSingleMethodUnoptimized(AstNode node) {
            return this.singleMethods.computeIfAbsent(node, (AstNode node1) -> this.newSingleMethod((adapter, localVarConsumer) -> node1.doBytecodeGenSingle(this, adapter, localVarConsumer), nextMethodName(node.getClass().getSimpleName())));
        }

        private String newSingleMethod(BiConsumer<InstructionAdapter, LocalVarConsumer> generator) {
            return newSingleMethod(generator, nextMethodName());
        }

        private String newSingleMethod(BiConsumer<InstructionAdapter, LocalVarConsumer> generator, String name) {
            newSingleMethod0(generator, name, false);
            return name;
        }

        private void newSingleMethod0(BiConsumer<InstructionAdapter, LocalVarConsumer> generator, String name, boolean isPublic) {
            InstructionAdapter adapter = new InstructionAdapter(
                    new AnalyzerAdapter(
                            this.className,
                            (isPublic ? Opcodes.ACC_PUBLIC : Opcodes.ACC_PRIVATE) | Opcodes.ACC_FINAL,
                            name,
                            SINGLE_DESC,
                            classWriter.visitMethod(
                                    (isPublic ? Opcodes.ACC_PUBLIC : Opcodes.ACC_PRIVATE) | Opcodes.ACC_FINAL,
                                    name,
                                    SINGLE_DESC,
                                    null,
                                    null
                            )
                    )
            );
            List<IntObjectPair<Pair<String, String>>> extraLocals = new ArrayList<>();
            Label start = new Label();
            Label end = new Label();
            adapter.visitLabel(start);
            generator.accept(adapter, (localName, localDesc) -> {
                int ordinal = extraLocals.size() + 5;
                extraLocals.add(IntObjectPair.of(ordinal, Pair.of(localName, localDesc)));
                return ordinal;
            });
            adapter.visitLabel(end);
            adapter.visitLocalVariable("this", this.classDesc, null, start, end, 0);
            adapter.visitLocalVariable("x", Type.INT_TYPE.getDescriptor(), null, start, end, 1);
            adapter.visitLocalVariable("y", Type.INT_TYPE.getDescriptor(), null, start, end, 2);
            adapter.visitLocalVariable("z", Type.INT_TYPE.getDescriptor(), null, start, end, 3);
            adapter.visitLocalVariable("evalType", Type.getType(EvalType.class).getDescriptor(), null, start, end, 4);
            for (IntObjectPair<Pair<String, String>> local : extraLocals) {
                adapter.visitLocalVariable(local.right().left(), local.right().right(), null, start, end, local.leftInt());
            }
            adapter.visitMaxs(0, 0);
        }

        public ValuesMethodDefD newMultiMethod(AstNode node) {
            if (node instanceof ConstantNode constantNode) {
                return new ValuesMethodDefD(constantNode.getValue());
            } else {
                String generated = newMultiMethodUnoptimized(node);
                return new ValuesMethodDefD(generated);
            }
        }

        public String newMultiMethodUnoptimized(AstNode node) {
            return this.multiMethods.computeIfAbsent(node, (AstNode node1) -> this.newMultiMethod((adapter, localVarConsumer) -> node1.doBytecodeGenMulti(this, adapter, localVarConsumer), nextMethodName(node.getClass().getSimpleName())));
        }

        private String newMultiMethod(BiConsumer<InstructionAdapter, LocalVarConsumer> generator) {
            return newMultiMethod(generator, nextMethodName());
        }

        private String newMultiMethod(BiConsumer<InstructionAdapter, LocalVarConsumer> generator, String name) {
            newMultiMethod0(generator, name, false);
            return name;
        }

        private void newMultiMethod0(BiConsumer<InstructionAdapter, LocalVarConsumer> generator, String name, boolean isPublic) {
            InstructionAdapter adapter = new InstructionAdapter(
                    new AnalyzerAdapter(
                            this.className,
                            (isPublic ? Opcodes.ACC_PUBLIC : Opcodes.ACC_PRIVATE) | Opcodes.ACC_FINAL,
                            name,
                            MULTI_DESC,
                            classWriter.visitMethod(
                                    (isPublic ? Opcodes.ACC_PUBLIC : Opcodes.ACC_PRIVATE) | Opcodes.ACC_FINAL,
                                    name,
                                    MULTI_DESC,
                                    null,
                                    null
                            )
                    )
            );
            List<IntObjectPair<Pair<String, String>>> extraLocals = new ArrayList<>();
            Label start = new Label();
            Label end = new Label();
            adapter.visitLabel(start);
            generator.accept(adapter, (localName, localDesc) -> {
                int ordinal = extraLocals.size() + 7;
                extraLocals.add(IntObjectPair.of(ordinal, Pair.of(localName, localDesc)));
                return ordinal;
            });
            adapter.visitLabel(end);
            adapter.visitLocalVariable("this", this.classDesc, null, start, end, 0);
            adapter.visitLocalVariable("res", Type.getType(double[].class).getDescriptor(), null, start, end, 1);
            adapter.visitLocalVariable("x", Type.getType(double[].class).getDescriptor(), null, start, end, 2);
            adapter.visitLocalVariable("y", Type.getType(double[].class).getDescriptor(), null, start, end, 3);
            adapter.visitLocalVariable("z", Type.getType(double[].class).getDescriptor(), null, start, end, 4);
            adapter.visitLocalVariable("evalType", Type.getType(EvalType.class).getDescriptor(), null, start, end, 5);
            adapter.visitLocalVariable("arrayCache", Type.getType(ArrayCache.class).getDescriptor(), null, start, end, 6);
            for (IntObjectPair<Pair<String, String>> local : extraLocals) {
                adapter.visitLocalVariable(local.right().left(), local.right().right(), null, start, end, local.leftInt());
            }
            adapter.visitMaxs(0, 0);
        }

        public String getCachedSplineMethod(Spline<DensityFunctionTypes.Spline.SplinePos, DensityFunctionTypes.Spline.DensityFunctionWrapper> spline) {
            return this.splineMethods.get(spline);
        }

        public void cacheSplineMethod(Spline<DensityFunctionTypes.Spline.SplinePos, DensityFunctionTypes.Spline.DensityFunctionWrapper> spline, String method) {
            this.splineMethods.put(spline, method);
        }

        public void callDelegateSingle(InstructionAdapter m, ValuesMethodDefD target) {
            if (target.isConst()) {
                m.dconst(target.constValue());
            } else {
                m.load(0, InstructionAdapter.OBJECT_TYPE);
                m.load(1, Type.INT_TYPE);
                m.load(2, Type.INT_TYPE);
                m.load(3, Type.INT_TYPE);
                m.load(4, InstructionAdapter.OBJECT_TYPE);
                m.invokevirtual(this.className, target.generatedMethod(), SINGLE_DESC, false);
            }
        }

        public void callDelegateSingleFromMulti(InstructionAdapter m, ValuesMethodDefD target, int indexLocal) {
            if (target.isConst()) {
                m.dconst(target.constValue());
            } else {
                m.load(0, InstructionAdapter.OBJECT_TYPE);
                m.load(2, InstructionAdapter.OBJECT_TYPE);
                m.load(indexLocal, Type.INT_TYPE);
                m.aload(Type.INT_TYPE);
                m.load(3, InstructionAdapter.OBJECT_TYPE);
                m.load(indexLocal, Type.INT_TYPE);
                m.aload(Type.INT_TYPE);
                m.load(4, InstructionAdapter.OBJECT_TYPE);
                m.load(indexLocal, Type.INT_TYPE);
                m.aload(Type.INT_TYPE);
                m.load(5, InstructionAdapter.OBJECT_TYPE);

                m.invokevirtual(
                        this.className,
                        target.generatedMethod(),
                        BytecodeGen.Context.SINGLE_DESC,
                        false
                );
            }
        }

        public void callDelegateMulti(InstructionAdapter m, ValuesMethodDefD target) {
            callDelegateMulti(m, target, 1);
        }

        public void callDelegateMulti(InstructionAdapter m, ValuesMethodDefD target, int arrayLocalIndex) {
            if (target.isConst()) {
                m.load(arrayLocalIndex, InstructionAdapter.OBJECT_TYPE);
                m.dconst(target.constValue());
                m.invokestatic(Type.getInternalName(Arrays.class), "fill", Type.getMethodDescriptor(Type.VOID_TYPE, Type.getType(double[].class), Type.DOUBLE_TYPE), false);
            } else {
                m.load(0, InstructionAdapter.OBJECT_TYPE);
                m.load(arrayLocalIndex, InstructionAdapter.OBJECT_TYPE);
                m.load(2, InstructionAdapter.OBJECT_TYPE);
                m.load(3, InstructionAdapter.OBJECT_TYPE);
                m.load(4, InstructionAdapter.OBJECT_TYPE);
                m.load(5, InstructionAdapter.OBJECT_TYPE);
                m.load(6, InstructionAdapter.OBJECT_TYPE);
                m.invokevirtual(this.className, target.generatedMethod(), MULTI_DESC, false);
            }
        }

        public <T> String newField(Class<T> type, T data) {
            FieldRecord existing = this.args.get(data);
            if (existing != null) {
                return existing.name();
            }
            int size = this.args.size();
            String name = String.format("field_%d", size);
            classWriter.visitField(Opcodes.ACC_PRIVATE, name, Type.getDescriptor(type), null, null);
            this.args.put(data, new FieldRecord(name, size, type));
            return name;
        }

        public String newField(Object data) {
            return this.newField((Class) data.getClass(), data);
        }

        public void doCountedLoop(InstructionAdapter m, LocalVarConsumer localVarConsumer, IntConsumer bodyGenerator) {
            int loopIdx = localVarConsumer.createLocalVariable("loopIdx", Type.INT_TYPE.getDescriptor());
            m.iconst(0);
            m.store(loopIdx, Type.INT_TYPE);

            Label start = new Label();
            Label end = new Label();

            m.visitLabel(start);
            m.load(loopIdx, Type.INT_TYPE);
            m.load(1, InstructionAdapter.OBJECT_TYPE);
            m.arraylength();
            m.ificmpge(end);

            bodyGenerator.accept(loopIdx);

            m.iinc(loopIdx, 1);
            m.goTo(start);
            m.visitLabel(end);
        }

        public void delegateAllToSingle(InstructionAdapter m, BytecodeGen.Context.LocalVarConsumer localVarConsumer, AstNode current) {
            ValuesMethodDefD singleMethod = this.newSingleMethod(current);
            this.doCountedLoop(m, localVarConsumer, idx -> {
                m.load(1, InstructionAdapter.OBJECT_TYPE);
                m.load(idx, Type.INT_TYPE);

                this.callDelegateSingleFromMulti(m, singleMethod, idx);

                m.astore(Type.DOUBLE_TYPE);
            });
        }

        public void genPostprocessingMethod(String name, Consumer<InstructionAdapter> generator) {
            if (this.postProcessMethods.contains(name)) {
                return;
            }
            InstructionAdapter adapter = new InstructionAdapter(
                    new AnalyzerAdapter(
                            this.className,
                            Opcodes.ACC_PRIVATE | Opcodes.ACC_FINAL,
                            name,
                            "()V",
                            classWriter.visitMethod(
                                    Opcodes.ACC_PRIVATE | Opcodes.ACC_FINAL,
                                    name,
                                    "()V",
                                    null,
                                    null
                            )
                    )
            );
            Label start = new Label();
            Label end = new Label();
            adapter.visitLabel(start);
            generator.accept(adapter);
            adapter.visitLabel(end);
            adapter.visitMaxs(0, 0);
            adapter.visitLocalVariable("this", this.classDesc, null, start, end, 0);
            this.postProcessMethods.add(name);
        }

        public static interface LocalVarConsumer {
            int createLocalVariable(String name, String descriptor);
        }

        public record ValuesMethodDefF(boolean isConst, String generatedMethod, float constValue) {

            public ValuesMethodDefF(String generatedMethod) {
                this(false, generatedMethod, Float.NaN);
            }
            
            public ValuesMethodDefF(float constValue) {
                this(true, null, constValue);
            }
            
        }

        public record ValuesMethodDefD(boolean isConst, String generatedMethod, double constValue) {
            
            public ValuesMethodDefD(String generatedMethod) {
                this(false, generatedMethod, Double.NaN);
            }
            
            public ValuesMethodDefD(double constValue) {
                this(true, null, constValue);
            }
            
        }

        private static record FieldRecord(String name, int ordinal, Class<?> type) {
        }
    }

    @FunctionalInterface
    public interface EvalSingleInterface {
        double evalSingle(int x, int y, int z, EvalType type);
    }

    @FunctionalInterface
    public interface EvalMultiInterface {
        void evalMulti(double[] res, int[] x, int[] y, int[] z, EvalType type);
    }

}

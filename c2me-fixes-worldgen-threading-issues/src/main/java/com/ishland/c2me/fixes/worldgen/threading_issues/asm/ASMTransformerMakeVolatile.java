package com.ishland.c2me.fixes.worldgen.threading_issues.asm;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.stream.Stream;

public class ASMTransformerMakeVolatile {

    static final Logger LOGGER = LoggerFactory.getLogger("C2ME (c2me-fixes-worldgen-threading-issues) ASM Transformer");

    public static void transform(ClassNode classNode) {
        classNode.fields.stream()
                .filter(fieldNode ->
                        Stream.concat(Stream.ofNullable(fieldNode.visibleAnnotations), Stream.ofNullable(fieldNode.invisibleAnnotations))
                                .flatMap(Collection::stream)
                                .anyMatch(annotationNode -> Type.getDescriptor(MakeVolatile.class).equals(annotationNode.desc))
                )
                .forEach(fieldNode -> {
                    LOGGER.debug("Making field L{};{}:{} volatile", classNode.name, fieldNode.name, fieldNode.desc);
                    fieldNode.access |= Opcodes.ACC_VOLATILE;
                });
    }

    private record KeyValue<K, V>(K key, V value) {
    }

}

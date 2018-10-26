package io.github.kasiafi.airlift.listconfigs;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.objectweb.asm.*;

import java.io.FileInputStream;
import java.util.zip.GZIPInputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Prints a list of annotation values.
 * It only print values of annotations matching a specific annotation descriptor.
 * <p>
 * input: a filepath of a tar.gz file containing jar files
 * output: a list of annotation values (without annotation names)
 */
public class AirliftConfigsListing {

    private static final String ANNOTATION_DESC = "Lio/airlift/configuration/Config;";

    public static void main(String[] args) throws Exception {

        try (FileInputStream fileIS = new FileInputStream(args[0]);
             GZIPInputStream tarGzIS = new GZIPInputStream(fileIS);
             TarArchiveInputStream tarIS = new TarArchiveInputStream(tarGzIS)) {

            while (true) {
                TarArchiveEntry tarEntry = tarIS.getNextTarEntry();

                if (tarEntry == null) {
                    // no more data in tar archive
                    return;
                }

                String tarEntryName = tarEntry.getName();
                if (tarEntryName.endsWith(".jar")) {
                    ZipInputStream jarIS = new ZipInputStream(tarIS);

                    while (true) {
                        ZipEntry jarEntry = jarIS.getNextEntry();

                        if (jarEntry == null) {
                            // no more data in jar
                            break;
                        }

                        String jarEntryName = jarEntry.getName();
                        if (jarEntryName.endsWith(".class")) {
                            ClassReader classReader = new ClassReader(jarIS);
                            classReader.accept(new AnnotationSearch(ANNOTATION_DESC), 0);
                        }
                    }
                }
            }
        }
    }

    static class AnnotationSearch extends ClassVisitor {

        String searchedDescriptor;

        AnnotationSearch(String desc) {
            super(Opcodes.ASM6);
            searchedDescriptor = desc;
        }

        @Override
        public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
            return new MethodAnnotationSearch(searchedDescriptor);
        }
    }

    static class MethodAnnotationSearch extends MethodVisitor {

        String searchedDescriptor;

        MethodAnnotationSearch(String desc) {
            super(Opcodes.ASM6);
            searchedDescriptor = desc;
        }

        @Override
        public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
            if (desc.equals(searchedDescriptor)) {
                return new AnnotationValuePrinter();
            }
            return super.visitAnnotation(desc, visible);
        }
    }

    static class AnnotationValuePrinter extends AnnotationVisitor {

        AnnotationValuePrinter() {
            super(Opcodes.ASM6);
        }

        @Override
        public void visit(final String name, final Object value) {
            System.out.printf("%s%n", value);
        }
    }
}

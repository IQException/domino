package com.ctrip.train.tieyouflight.domino.support;

import javassist.*;
import javassist.bytecode.AnnotationsAttribute;
import javassist.bytecode.ClassFile;
import javassist.bytecode.ConstPool;
import javassist.bytecode.annotation.Annotation;
import javassist.bytecode.annotation.MemberValue;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.util.ClassUtils;


/**
 * @author wang.wei
 * @since 2019/5/16
 */
public class ClassGenerator {

    public static CtClass makeEmptyClass(String className) throws Exception {
        ClassPool pool = ClassPool.getDefault();
        CtClass ctClass = pool.makeClass(className);
        //添加构造函数
        CtConstructor ctConstructor = new CtConstructor(new CtClass[]{}, ctClass);
        //把构造函数添加到新的类中
        ctClass.addConstructor(ctConstructor);
        return ctClass;
    }

    public static CtClass makeInterface(String className) {
        ClassPool pool = ClassPool.getDefault();
        return pool.makeInterface(className);
    }

    public static CtMethod addEmptyMethod(CtClass ctClass, String methodName, Class<?>[] parameterTypes) throws Exception {
        CtClass[] ctClasses = new CtClass[]{};
        if (parameterTypes != null && parameterTypes.length != 0) {
            ctClasses = new CtClass[parameterTypes.length];
            int i = 0;
            for (Class<?> parameterType : parameterTypes) {
                ctClasses[i++] = ClassPool.getDefault().get(parameterType.getName());
            }
        }
        CtMethod ctMethod = new CtMethod(CtClass.voidType, methodName, ctClasses, ctClass);
        ctMethod.setModifiers(Modifier.PUBLIC);
        ctClass.addMethod(ctMethod);
        return ctMethod;

    }

    public static CtField addField(CtClass ctClass, String fieldName, Class<?> type) throws Exception {
        CtClass ctType = ClassPool.getDefault().get(type.getName());
        CtField ctField = new CtField(ctType, fieldName, ctClass);
        ctField.setModifiers(Modifier.PRIVATE);
        ctClass.addField(ctField);
        return ctField;

    }

    public static Annotation addMethodAnnotation(CtClass ctClass, CtMethod ctMethod, Class<?> annotation, Pair<String, MemberValue>[] memberValuePairs) {

        ClassFile ccFile = ctClass.getClassFile();
        ConstPool constpool = ccFile.getConstPool();
        AnnotationsAttribute attr = new AnnotationsAttribute(constpool, AnnotationsAttribute.visibleTag);
        Annotation annot = new Annotation(annotation.getName(), constpool);
        if (memberValuePairs != null && memberValuePairs.length > 0) {
            for (Pair<String, MemberValue> memberValuePair : memberValuePairs) {
                annot.addMemberValue(memberValuePair.getKey(), memberValuePair.getValue());
            }
        }
        attr.addAnnotation(annot);
        ctMethod.getMethodInfo().addAttribute(attr);

        return annot;
    }

    public static Annotation addTypeAnnotation(CtClass ctClass, Class<?> annotation, Pair<String, MemberValue>[] memberValuePairs) {

        ClassFile ccFile = ctClass.getClassFile();
        ConstPool constpool = ccFile.getConstPool();
        AnnotationsAttribute attr = new AnnotationsAttribute(constpool, AnnotationsAttribute.visibleTag);
        Annotation annot = new Annotation(annotation.getName(), constpool);
        if (memberValuePairs != null && memberValuePairs.length > 0) {
            for (Pair<String, MemberValue> memberValuePair : memberValuePairs) {
                annot.addMemberValue(memberValuePair.getKey(), memberValuePair.getValue());
            }
        }
        attr.addAnnotation(annot);
        ccFile.addAttribute(attr);

        return annot;
    }

    public static Annotation addFieldAnnotation(CtClass ctClass, CtField ctField, Class<?> annotation, Pair<String, MemberValue>[] memberValuePairs) {

        ClassFile ccFile = ctClass.getClassFile();
        ConstPool constpool = ccFile.getConstPool();
        AnnotationsAttribute attr = new AnnotationsAttribute(constpool, AnnotationsAttribute.visibleTag);
        Annotation annot = new Annotation(annotation.getName(), constpool);
        if (memberValuePairs != null && memberValuePairs.length > 0) {
            for (Pair<String, MemberValue> memberValuePair : memberValuePairs) {
                annot.addMemberValue(memberValuePair.getKey(), memberValuePair.getValue());
            }
        }
        attr.addAnnotation(annot);
        ctField.getFieldInfo().addAttribute(attr);

        return annot;
    }

    public static Class generate(CtClass ctClass) throws Exception {
        return ctClass.toClass(ClassUtils.getDefaultClassLoader(), null);
    }


}

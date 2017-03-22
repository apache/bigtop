/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.bigtop.itest.hadoop.api;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.classification.InterfaceAudience;
import org.apache.hadoop.classification.InterfaceStability;
import org.codehaus.jackson.annotate.JsonIgnore;
import org.codehaus.jackson.map.ObjectMapper;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A tool that generates API conformance tests for Hadoop libraries
 */
public class ApiExaminer {

    private static final Log LOG = LogFactory.getLog(ApiExaminer.class.getName());

    static private Set<String> unloadableClasses;

    private List<String> errors;
    private List<String> warnings;

    static {
        unloadableClasses = new HashSet<>();
        unloadableClasses.add("org.apache.hadoop.security.JniBasedUnixGroupsMapping");
        unloadableClasses.add("org.apache.hadoop.security.JniBasedUnixGroupsNetgroupMapping");
        unloadableClasses.add("org.apache.hadoop.io.compress.lz4.Lz4Compressor");
        unloadableClasses.add("org.apache.hadoop.record.compiler.ant.RccTask");

    }

    public static void main(String[] args) {
        Options options = new Options();

        options.addOption("c", "compare", true,
                "Compare against a spec, argument is the json file containing spec");
        options.addOption("h", "help", false, "You're looking at it");
        options.addOption("j", "jar", true, "Jar to examine");
        options.addOption("p", "prepare-spec", true,
                "Prepare the spec, argument is the directory to write the spec to");

        try {
            CommandLine cli = new GnuParser().parse(options, args);

            if (cli.hasOption('h')) {
                usage(options);
                return;
            }

            if ((!cli.hasOption('c') && !cli.hasOption('p')) ||
                    (cli.hasOption('c') && cli.hasOption('p'))) {
                System.err.println("You must choose either -c or -p");
                usage(options);
                return;
            }

            if (!cli.hasOption('j')) {
                System.err.println("You must specify the jar to prepare or compare");
                usage(options);
                return;
            }

            String jar = cli.getOptionValue('j');
            ApiExaminer examiner = new ApiExaminer();

            if (cli.hasOption('c')) {
                examiner.compareAgainstStandard(cli.getOptionValue('c'), jar);
            } else if (cli.hasOption('p')) {
                examiner.prepareExpected(jar, cli.getOptionValue('p'));
            }
        } catch (Exception e) {
            System.err.println("Received exception while processing");
            e.printStackTrace();
        }
    }

    private static void usage(Options options) {
        HelpFormatter help = new HelpFormatter();
        help.printHelp("api-examiner", options);

    }

    private ApiExaminer() {
    }

    private void prepareExpected(String jarFile, String outputDir) throws IOException,
            ClassNotFoundException {
        JarInfo jarInfo = new JarInfo(jarFile, this);
        jarInfo.dumpToFile(new File(outputDir));
    }

    private void compareAgainstStandard(String json, String jarFile) throws IOException,
            ClassNotFoundException {
        errors = new ArrayList<>();
        warnings = new ArrayList<>();
        JarInfo underTest = new JarInfo(jarFile, this);
        JarInfo standard = jarInfoFromFile(new File(json));
        standard.compareAndReport(underTest);

        if (errors.size() > 0) {
            System.err.println("Found " + errors.size() + " incompatibilities:");
            for (String error : errors) {
                System.err.println(error);
            }
        }

        if (warnings.size() > 0) {
            System.err.println("Found " + warnings.size() + " possible issues: ");
            for (String warning : warnings) {
                System.err.println(warning);
            }
        }


    }

    private JarInfo jarInfoFromFile(File inputFile) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        JarInfo jarInfo = mapper.readValue(inputFile, JarInfo.class);
        jarInfo.patchUpClassBackPointers(this);
        return jarInfo;
    }

    private static class JarInfo {
        String name;
        String version;
        ApiExaminer container;
        Map<String, ClassInfo> classes;

        // For use by Jackson
        public JarInfo() {

        }

        JarInfo(String jarFile, ApiExaminer container) throws IOException, ClassNotFoundException {
            this.container = container;
            LOG.info("Processing jar " + jarFile);
            File f = new File(jarFile);
            Pattern pattern = Pattern.compile("(hadoop-[a-z\\-]+)-([0-9]\\.[0-9]\\.[0-9]).*");
            Matcher matcher = pattern.matcher(f.getName());
            if (!matcher.matches()) {
                String msg = "Unable to determine name and version from " + f.getName();
                LOG.error(msg);
                throw new RuntimeException(msg);
            }
            name = matcher.group(1);
            version = matcher.group(2);
            classes = new HashMap<>();

            JarFile jar = new JarFile(jarFile);
            Enumeration<JarEntry> entries = jar.entries();
            while (entries.hasMoreElements()) {
                String name = entries.nextElement().getName();
                if (name.endsWith(".class")) {
                    name = name.substring(0, name.length() - 6);
                    name = name.replace('/', '.');
                    if (!unloadableClasses.contains(name)) {
                        LOG.debug("Processing class " + name);
                        Class<?> clazz = Class.forName(name);
                        if (clazz.getAnnotation(InterfaceAudience.Public.class) != null &&
                                clazz.getAnnotation(InterfaceStability.Stable.class) != null) {
                            classes.put(name, new ClassInfo(this, clazz));
                        }
                    }
                }
            }
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getVersion() {
            return version;
        }

        public void setVersion(String version) {
            this.version = version;
        }

        public Map<String, ClassInfo> getClasses() {
            return classes;
        }

        public void setClasses(Map<String, ClassInfo> classes) {
            this.classes = classes;
        }

        void compareAndReport(JarInfo underTest) {
            Set<ClassInfo> underTestClasses = new HashSet<>(underTest.classes.values());
            for (ClassInfo classInfo : classes.values()) {
                if (underTestClasses.contains(classInfo)) {
                    classInfo.compareAndReport(underTest.classes.get(classInfo.name));
                    underTestClasses.remove(classInfo);
                } else {
                    container.errors.add(underTest + " does not contain class " + classInfo);
                }
            }

            if (underTestClasses.size() > 0) {
                for (ClassInfo extra : underTestClasses) {
                    container.warnings.add(underTest + " contains extra class " + extra);
                }
            }
        }

        void dumpToFile(File outputDir) throws IOException {
            File output = new File(outputDir, name + "-" + version + "-api-report.json");
            ObjectMapper mapper = new ObjectMapper();
            mapper.writeValue(output, this);
        }

        void patchUpClassBackPointers(ApiExaminer container) {
            this.container = container;
            for (ClassInfo classInfo : classes.values()) {
                classInfo.setJar(this);
                classInfo.patchUpBackMethodBackPointers();
            }
        }

        @Override
        public boolean equals(Object other) {
            if (!(other instanceof JarInfo)) return false;
            JarInfo that = (JarInfo) other;
            return name.equals(that.name) && version.equals(that.version);
        }

        @Override
        public String toString() {
            return name + "-" + version;
        }
    }

    private static class ClassInfo {
        @JsonIgnore
        JarInfo jar;
        String name;
        Map<String, MethodInfo> methods;

        // For use by Jackson
        public ClassInfo() {

        }

        ClassInfo(JarInfo jar, Class<?> clazz) {
            this.jar = jar;
            this.name = clazz.getName();
            methods = new HashMap<>();

            for (Method method : clazz.getMethods()) {
                if (method.getDeclaringClass().equals(clazz)) {
                    LOG.debug("Processing method " + method.getName());
                    MethodInfo mi = new MethodInfo(this, method);
                    methods.put(mi.toString(), mi);
                }
            }
        }

        public JarInfo getJar() {
            return jar;
        }

        public void setJar(JarInfo jar) {
            this.jar = jar;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public Map<String, MethodInfo> getMethods() {
            return methods;
        }

        public void setMethods(Map<String, MethodInfo> methods) {
            this.methods = methods;
        }

        void compareAndReport(ClassInfo underTest) {
            // Make a copy so we can remove them as we match them, making it easy to find additional ones
            Set<MethodInfo> underTestMethods = new HashSet<>(underTest.methods.values());
            for (MethodInfo methodInfo : methods.values()) {
                if (underTestMethods.contains(methodInfo)) {
                    methodInfo.compareAndReport(underTest.methods.get(methodInfo.toString()));
                    underTestMethods.remove(methodInfo);
                } else {
                    jar.container.errors.add(underTest + " does not contain method " + methodInfo);
                }
            }

            if (underTestMethods.size() > 0) {
                for (MethodInfo extra : underTestMethods) {
                    jar.container.warnings.add(underTest + " contains extra method " + extra);
                }
            }
        }

        void patchUpBackMethodBackPointers() {
            for (MethodInfo methodInfo : methods.values()) methodInfo.setContainingClass(this);
        }

        @Override
        public boolean equals(Object other) {
            if (!(other instanceof ClassInfo)) return false;
            ClassInfo that = (ClassInfo) other;
            return name.equals(that.name);  // Classes can be compared just on names
        }

        @Override
        public int hashCode() {
            return name.hashCode();
        }

        @Override
        public String toString() {
            return jar + " " + name;
        }
    }

    private static class MethodInfo {
        @JsonIgnore
        ClassInfo containingClass;
        String name;
        String returnType;
        List<String> args;
        Set<String> exceptions;

        // For use by Jackson
        public MethodInfo() {

        }

        MethodInfo(ClassInfo containingClass, Method method) {
            this.containingClass = containingClass;
            this.name = method.getName();
            args = new ArrayList<>();
            for (Class<?> argClass : method.getParameterTypes()) {
                args.add(argClass.getName());
            }
            returnType = method.getReturnType().getName();
            exceptions = new HashSet<>();
            for (Class<?> exception : method.getExceptionTypes()) {
                exceptions.add(exception.getName());
            }
        }

        public ClassInfo getContainingClass() {
            return containingClass;
        }

        public void setContainingClass(ClassInfo containingClass) {
            this.containingClass = containingClass;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getReturnType() {
            return returnType;
        }

        public void setReturnType(String returnType) {
            this.returnType = returnType;
        }

        public List<String> getArgs() {
            return args;
        }

        public void setArgs(List<String> args) {
            this.args = args;
        }

        public Set<String> getExceptions() {
            return exceptions;
        }

        public void setExceptions(Set<String> exceptions) {
            this.exceptions = exceptions;
        }

        void compareAndReport(MethodInfo underTest) {
            // Check to see if they've added or removed exceptions
            // Make a copy so I can remove them as I check them off and easily find any that have been
            // added.
            Set<String> underTestExceptions = new HashSet<>(underTest.exceptions);
            for (String exception : exceptions) {
                if (underTest.exceptions.contains(exception)) {
                    underTestExceptions.remove(exception);
                } else {
                    containingClass.jar.container.warnings.add(underTest.containingClass.jar + " " +
                            underTest.containingClass + "." + name + " removes exception " + exception);
                }
            }
            if (underTestExceptions.size() > 0) {
                for (String underTestException : underTest.exceptions) {
                    containingClass.jar.container.warnings.add(underTest.containingClass.jar + " " +
                            underTest.containingClass + "." + name + " adds exception " + underTestException);
                }
            }
        }

        @Override
        public boolean equals(Object other) {
            if (!(other instanceof MethodInfo)) return false;
            MethodInfo that = (MethodInfo) other;

            return containingClass.equals(that.containingClass) && name.equals(that.name) &&
                    returnType.equals(that.returnType) && args.equals(that.args);
        }

        @Override
        public int hashCode() {
            return ((containingClass.hashCode() * 31 + name.hashCode()) * 31 + returnType.hashCode()) * 31 +
                    args.hashCode();
        }

        @Override
        public String toString() {
            StringBuilder buf = new StringBuilder(returnType)
                    .append(" ")
                    .append(name)
                    .append('(');
            boolean first = true;
            for (String arg : args) {
                if (first) first = false;
                else buf.append(", ");
                buf.append(arg);
            }
            buf.append(")");
            if (exceptions.size() > 0) {
                buf.append(" throws ");
                first = true;
                for (String exception : exceptions) {
                    if (first) first = false;
                    else buf.append(", ");
                    buf.append(exception);
                }
            }
            return buf.toString();
        }
    }
}

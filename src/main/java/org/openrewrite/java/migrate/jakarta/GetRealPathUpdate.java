/*
 * Copyright 2023 the original author or authors.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.openrewrite.java.migrate.jakarta;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.jetbrains.annotations.NotNull;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Option;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.internal.lang.NonNull;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.JavaVisitor;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;

@Value
@EqualsAndHashCode(callSuper = false)
public class GetRealPathUpdate extends Recipe {
    @Option(displayName = "Method Pattern",
            description = "A `jakarta.servlet.ServletRequest` or `jakarta.servlet.ServletRequestWrapper getRealPath(String)` matching required",
            example = "jakarta.servlet.ServletRequest getRealPath(String)")
    @NonNull String methodPattern;

    @Override
    public @NotNull String getDisplayName() {
        return "Remove methods calls";
    }

    @Override
    public @NotNull String getDescription() {
        return "Updates `getRealPath()` for `jakarta.servlet.ServletRequest` and `jakarta.servlet.ServletRequestWrapper`";
    }

    @Override
    public @NotNull TreeVisitor<?, ExecutionContext> getVisitor() {
        return new MethodInvocationVisitor(methodPattern);
    }

    private static class MethodInvocationVisitor extends JavaVisitor<ExecutionContext> {
        private final MethodMatcher METHOD_PATTERN;

        private MethodInvocationVisitor(String methodPattern) {
            METHOD_PATTERN = new MethodMatcher(methodPattern, false);
        }

        @Nullable
        @Override
        public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext ec) {
            if (METHOD_PATTERN.matches(method)) {
                maybeAddImport("jakarta.servlet.ServletContext");
                String newMethodName = "getContext()." + method.getSimpleName();
                JavaType.Method type = method.getMethodType();
                if (type != null) {
                    type = type.withName(newMethodName);
                }
                method = method.withName(method.getName().withSimpleName(newMethodName)).withMethodType(type);
            }
            return method;
        }
    }
}
package com.intellij.lang.javascript.flex.build;

import com.intellij.compiler.impl.BuildTargetScopeProvider;
import com.intellij.flex.build.FlexResourceBuildTargetType;
import com.intellij.lang.javascript.flex.projectStructure.model.FlexBuildConfiguration;
import com.intellij.openapi.compiler.CompileScope;
import com.intellij.openapi.compiler.CompilerFilter;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Pair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jps.api.CmdlineProtoUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.jetbrains.jps.api.CmdlineRemoteProto.Message.ControllerMessage.ParametersMessage.TargetTypeBuildScope;

public class FlexResourceBuildTargetScopeProvider extends BuildTargetScopeProvider {

  @NotNull
  public List<TargetTypeBuildScope> getBuildTargetScopes(@NotNull final CompileScope baseScope,
                                                         @NotNull final CompilerFilter filter,
                                                         @NotNull final Project project,
                                                         boolean forceBuild) {
    final FlexCompiler flexCompiler = FlexCompiler.getInstance(project);
    if (!filter.acceptCompiler(flexCompiler)) return Collections.emptyList();

    List<String> moduleNames = new ArrayList<String>();
    try {
      for (Pair<Module, FlexBuildConfiguration> moduleAndBC : FlexCompiler.getModulesAndBCsToCompile(baseScope)) {
        moduleNames.add(moduleAndBC.first.getName());
      }
    }
    catch (ConfigurationException e) {
      // can't happen because checked in ValidateFlashConfigurationsPrecompileTask
    }

    return Arrays.asList(
      CmdlineProtoUtil.createTargetsScope(FlexResourceBuildTargetType.PRODUCTION.getTypeId(), moduleNames, forceBuild),
      CmdlineProtoUtil.createTargetsScope(FlexResourceBuildTargetType.TEST.getTypeId(), moduleNames, forceBuild)
    );
  }
}

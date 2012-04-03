package com.intellij.lang.javascript.flex.build;

import com.intellij.lang.javascript.flex.FlexBundle;
import com.intellij.lang.javascript.flex.projectStructure.FlexBuildConfigurationsExtension;
import com.intellij.lang.javascript.flex.projectStructure.model.FlexIdeBuildConfiguration;
import com.intellij.lang.javascript.flex.projectStructure.options.BCUtils;
import com.intellij.lang.javascript.flex.projectStructure.ui.CompositeConfigurable;
import com.intellij.lang.javascript.flex.projectStructure.ui.FlexIdeBCConfigurable;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleType;
import com.intellij.openapi.options.ShowSettingsUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ui.configuration.ProjectStructureConfigurable;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.MasterDetailsComponent;
import com.intellij.openapi.util.IconLoader;
import com.intellij.openapi.util.Ref;
import com.intellij.ui.ColoredTreeCellRenderer;
import com.intellij.ui.navigation.Place;
import com.intellij.ui.treeStructure.Tree;
import com.intellij.util.ui.tree.TreeUtil;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Enumeration;

public class FlashProjectStructureErrorsDialog extends DialogWrapper {
  private static final Icon ERROR_ICON = IconLoader.getIcon("/ide/errorSign.png");

  private JPanel myMainPanel;
  private Tree myTree;

  private final Project myProject;
  private boolean myContainsProblems;

  public FlashProjectStructureErrorsDialog(final Project project) {
    super(project);
    myProject = project;

    myTree.setRootVisible(false);
    myTree.setModel(new DefaultTreeModel(new DefaultMutableTreeNode()));

    myTree.setCellRenderer(new ColoredTreeCellRenderer() {
      public void customizeCellRenderer(JTree tree, Object value, boolean selected, boolean expanded, boolean leaf, int row,
                                        boolean hasFocus) {
        final DefaultMutableTreeNode treeNode = (DefaultMutableTreeNode)value;
        final Object userObject = treeNode.getUserObject();
        if (userObject instanceof Module) {
          setIcon(ModuleType.get((Module)userObject).getNodeIcon(expanded));
          append(((Module)userObject).getName());
        }
        else if (userObject instanceof FlexIdeBuildConfiguration) {
          setIcon(((FlexIdeBuildConfiguration)userObject).getIcon());
          BCUtils.renderBuildConfiguration((FlexIdeBuildConfiguration)userObject, null).appendToComponent(this);
        }
        else if (userObject instanceof FlashProjectStructureProblem) {
          setIcon(ERROR_ICON);
          append(((FlashProjectStructureProblem)userObject).errorMessage);
        }
      }
    });

    myTree.addKeyListener(new KeyAdapter() {
      public void keyPressed(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_ENTER) {
          openProjectStructure();
        }
      }
    });

    myTree.addMouseListener(new MouseAdapter() {
      public void mouseClicked(final MouseEvent e) {
        if (MouseEvent.BUTTON1 == e.getButton() && e.getClickCount() == 2) {
          openProjectStructure();
        }
      }
    });

    setTitle(FlexBundle.message("project.setup.problem.title"));
    setOKButtonText(FlexBundle.message("open.project.structure"));

    init();
  }

  protected JComponent createCenterPanel() {
    return myMainPanel;
  }

  public JComponent getPreferredFocusedComponent() {
    TreeUtil.expandAll(myTree);
    return myTree;
  }

  public void addProblem(final Module module, final FlexIdeBuildConfiguration bc, final FlashProjectStructureProblem problem) {
    myContainsProblems = true;

    final DefaultMutableTreeNode rootNode = (DefaultMutableTreeNode)myTree.getModel().getRoot();
    final DefaultMutableTreeNode moduleNode = getOrCreateChildNode(rootNode, module);
    final DefaultMutableTreeNode bcNode = getOrCreateChildNode(moduleNode, bc);
    bcNode.add(new DefaultMutableTreeNode(problem));
  }

  public boolean containsProblems() {
    return myContainsProblems;
  }

  @NotNull
  private static DefaultMutableTreeNode getOrCreateChildNode(final DefaultMutableTreeNode parentNode, final Object userObject) {
    final Enumeration children = parentNode.children();
    while (children.hasMoreElements()) {
      final DefaultMutableTreeNode childNode = (DefaultMutableTreeNode)children.nextElement();
      if (userObject.equals(childNode.getUserObject())) {
        return childNode;
      }
    }

    final DefaultMutableTreeNode childNode = new DefaultMutableTreeNode(userObject);
    parentNode.add(childNode);
    return childNode;
  }

  private void openProjectStructure() {
    final TreePath selectionPath = myTree.getSelectionPath();
    DefaultMutableTreeNode node = selectionPath == null ? null : (DefaultMutableTreeNode)selectionPath.getLastPathComponent();
    Object userObject = node == null ? null : node.getUserObject();
    if (userObject == null) return;

    final Ref<Module> moduleRef = new Ref<Module>();
    final Ref<FlexIdeBuildConfiguration> bcRef = new Ref<FlexIdeBuildConfiguration>();
    final Ref<FlashProjectStructureProblem> problemRef = new Ref<FlashProjectStructureProblem>();

    if (userObject instanceof FlashProjectStructureProblem) {
      problemRef.set((FlashProjectStructureProblem)userObject);
      node = (DefaultMutableTreeNode)node.getParent();
      userObject = node.getUserObject();
    }

    if (userObject instanceof FlexIdeBuildConfiguration) {
      bcRef.set((FlexIdeBuildConfiguration)userObject);
      node = (DefaultMutableTreeNode)node.getParent();
      userObject = node.getUserObject();
    }

    if (userObject instanceof Module) {
      moduleRef.set((Module)userObject);
    }

    close(CANCEL_EXIT_CODE);

    final ProjectStructureConfigurable configurable = ProjectStructureConfigurable.getInstance(myProject);
    ShowSettingsUtil.getInstance().editConfigurable(myProject, configurable, new Runnable() {
      public void run() {
        final Place place;
        if (moduleRef.isNull()) {
          place = new Place()
            .putPath(ProjectStructureConfigurable.CATEGORY, configurable.getModulesConfig());
        }
        else if (bcRef.isNull()) {
          place = new Place()
            .putPath(ProjectStructureConfigurable.CATEGORY, configurable.getModulesConfig())
            .putPath(MasterDetailsComponent.TREE_OBJECT, moduleRef.get());
        }
        else {
          place = FlexBuildConfigurationsExtension.getInstance().getConfigurator().getPlaceFor(moduleRef.get(), bcRef.get());
          if (!problemRef.isNull()) {
            place.putPath(CompositeConfigurable.TAB_NAME, problemRef.get().tabName);
            place.putPath(FlexIdeBCConfigurable.LOCATION_ON_TAB, problemRef.get().locationOnTab);
          }
        }

        configurable.navigateTo(place, true);
      }
    });
  }
}

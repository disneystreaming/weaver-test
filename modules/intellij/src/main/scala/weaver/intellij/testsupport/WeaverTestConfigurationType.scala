package weaver.intellij.testsupport

import javax.swing.Icon

import com.intellij.execution.configurations.{
  ConfigurationFactory,
  ConfigurationType,
  RunConfiguration
}
import com.intellij.openapi.project.Project
import com.intellij.ui.IconManager

class WeaverTestConfigurationType extends ConfigurationType {
  override val getDisplayName: String = "weaver-test"

  override val getConfigurationTypeDescription: String =
    "Weaver-test framework run configuration"

  override val getIcon: Icon = IconManager.getInstance.getIcon(
    "/logo.png",
    getClass());

  override val getId: String = "WeaverTestConfigurationType"

  private val that = this

  val configurationFactory: ConfigurationFactory =
    new ConfigurationFactory(this) {

      override def getId: String = that.getId

      override def createTemplateConfiguration(
          project: Project): RunConfiguration =
        new WeaverTestRunConfiguration(project, this)

    }

  override val getConfigurationFactories: Array[ConfigurationFactory] =
    List(configurationFactory).toArray
}

object WeaverTestConfigurationType extends WeaverTestConfigurationType

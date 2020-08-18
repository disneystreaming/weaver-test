package org.jetbrains.plugins.scala.testingSupport.test

import com.intellij.execution.configurations.{ConfigurationFactory, ConfigurationType, RunConfiguration}
import com.intellij.openapi.project.Project
import com.intellij.ui.IconManager
import javax.swing.Icon

class WeaverTestConfigurationType extends ConfigurationType {
  override val getDisplayName: String = "weaver-test"

  override val getConfigurationTypeDescription: String = "Run weaver test"

  override val getIcon: Icon = IconManager.getInstance.getIcon("/logo.png", classOf[WeaverTestConfigurationType]);

  override val getId: String = "weaver-test"

  private val that = this

  val configurationFactory: ConfigurationFactory = new ConfigurationFactory(this){

    override def getId: String = that.getId

    override def createTemplateConfiguration(project: Project): RunConfiguration = new WeaverTestRunConfiguration(project,this)

  }

  override val getConfigurationFactories: Array[ConfigurationFactory] = List(configurationFactory).toArray
}

object WeaverTestConfigurationType extends WeaverTestConfigurationType

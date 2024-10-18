package org.jetbrains.compose.hotreload

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.material.darkColors
import androidx.compose.material.lightColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.ComposePanel
import androidx.compose.ui.awt.SwingPanel
import androidx.compose.ui.unit.dp
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.JBColor
import com.intellij.ui.content.ContentFactory
import java.io.File
import java.net.URLClassLoader

class AppViewToolWindowFactory : ToolWindowFactory {
    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val contentFactory = ContentFactory.getInstance()
        val composePanel = ComposePanel().apply {
            setContent { AppView() }
        }
        toolWindow.contentManager.addContent(
            contentFactory.createContent(composePanel, "", false)
        )
    }
}

@Composable
private fun AppView() {
    val colors = if (JBColor.isBright()) lightColors() else darkColors()
    MaterialTheme(colors = colors) {
        Surface {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                var appJarPath by remember { mutableStateOf("/Users/Konstantin.Tskhovrebov/Workspace/compose-hot-reload/sample-app/build/compose/jars/Multiplatform App-macos-arm64-1.0.0.jar") }
                var funName by remember { mutableStateOf("App") }
                val (panel, setPanel) = remember { mutableStateOf<ComposePanel?>(null) }

                Column {
                    TextField(
                        value = appJarPath,
                        onValueChange = { appJarPath = it },
                        singleLine = true,
                        label = { Text("App Uber JAR path") }
                    )
                    TextField(
                        value = funName,
                        onValueChange = { funName = it },
                        singleLine = true,
                        label = { Text("Composable function") }
                    )
                    Button(
                        onClick = { setPanel(loadComposePanel(appJarPath, funName)) }
                    ) { Text("Load") }
                }
                if (panel != null) {
                    SwingPanel(
                        background = MaterialTheme.colors.background,
                        factory = { panel },
                        modifier = Modifier
                            .aspectRatio(0.46f)
                            .fillMaxSize()
                            .padding(40.dp)
                            .background(MaterialTheme.colors.primary)
                            .padding(4.dp)
                    )
                }
            }
        }
    }
}

private fun loadComposePanel(jarPath: String, funName: String): ComposePanel {
    val file = File(jarPath)
    val classLoader = URLClassLoader(arrayOf(file.toURI().toURL()), AppViewToolWindowFactory::class.java.classLoader)
    val clazz = Class.forName("HotReloadFunctionsKt", true, classLoader)
    val m = clazz.getDeclaredMethod("hotreload_$funName")
    val panel = m.invoke(null) as ComposePanel
    return panel
}
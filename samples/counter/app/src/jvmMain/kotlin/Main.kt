import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import io.sellmair.evas.Events
import io.sellmair.evas.States
import io.sellmair.evas.compose.installEvas
import org.jetbrains.compose.reload.DevelopmentEntryPoint
import org.slf4j.LoggerFactory
import java.io.File
import kotlin.random.Random

fun main() {
    val cp = System.getProperty("java.class.path").split(File.pathSeparator)
    LoggerFactory.getLogger("App").info("Class path:\n${cp.joinToString("\n")}")


    val events = Events()
    val states = States()

    application {
        Window(
            onCloseRequest = ::exitApplication,
            alwaysOnTop = true,
            state = rememberWindowState(
                width = 600.dp, height = 800.dp, position = WindowPosition.Aligned(Alignment.TopEnd)
            )
        ) {
            installEvas(events, states) {
                DevelopmentEntryPoint {
                    App()
                }
            }
        }
    }
}

@DevelopmentEntryPoint(600, 400)
@Composable
fun Dev() {
    Column {
        var txt by remember { mutableStateOf("init") }
        Text(txt)
        Button(
            onClick = { txt += " " + Random.nextBoolean().toString() }
        ) {
            Text("Click ME")
        }
    }
}

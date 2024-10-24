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

fun main() {
    val cp = System.getProperty("java.class.path").split(File.pathSeparator)
    LoggerFactory.getLogger("App").info("Class path:\n${cp.joinToString("\n")}")


    val events = Events()
    val states = States()

    application {
        Window(
            onCloseRequest = ::exitApplication,
            state = rememberWindowState(
                width = 600.dp, height = 800.dp, position = WindowPosition.Aligned(Alignment.Center)
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

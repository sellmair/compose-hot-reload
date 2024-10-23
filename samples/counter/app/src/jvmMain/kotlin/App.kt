import androidx.compose.foundation.layout.*
import androidx.compose.material.Button
import androidx.compose.material.Card
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.sellmair.evas.compose.EvasLaunching
import io.sellmair.evas.compose.composeValue
import io.sellmair.evas.set
import org.jetbrains.compose.reload.DevEntryPoint

@DevEntryPoint
@Composable
fun App() {
    val applicationStateCounter = CounterState.composeValue()
    var uiStateCounter by remember { mutableStateOf(0) }

    Column(modifier = Modifier.padding(16.dp)) {
        Row {
            Text("Hot Reload Example", fontSize = 48.sp, fontWeight = FontWeight.Bold)
        }

        Row {
            Text("Press 'command + s' in IntelliJ to see changes")
        }

        Spacer(Modifier.height(32.dp))

        Card(elevation = 8.dp) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row {
                    Text(
                        "🔥(Hot/Application State). This state will be retained during hot-reload!",
                        fontSize = 12.sp
                    )
                }

                Row {
                    Text("🔥Hot State Counter: ${applicationStateCounter.value}", fontSize = 24.sp)
                }
            }
        }

        Spacer(Modifier.height(32.dp))


        Card(elevation = 8.dp) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row {
                    Text(
                        "❄️(Cold/UI State). UI State is rebuilt during hot reload!",
                        fontSize = 12.sp
                    )
                }

                Row {
                    Text("❄️Cold State Counter: $uiStateCounter", fontSize = 24.sp)
                }
            }
        }

        Spacer(Modifier.height(32.dp))


        Card(elevation = 8.dp) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row {
                    Text(
                        "Hot Reload also works across modules\nEdit 'widgets/src/commonUI/SomeWidget.kt",
                        fontSize = 12.sp
                    )
                }

                Spacer(Modifier.height(32.dp))

                Row {
                    Card(elevation = 16.dp) {
                        Box(modifier = Modifier.padding(16.dp)) {
                            SomeWidget()
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(32.dp))

        Row(modifier = Modifier.fillMaxWidth()) {
            Button(onClick = EvasLaunching {
                uiStateCounter++
                CounterState.set(CounterState(applicationStateCounter.value + 1))
            }) {
                Text("Inc", fontSize = 48.sp)
            }
        }
    }
}


package cat.martori.sesam

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import cat.martori.sesam.ui.theme.ObretSesamTheme
import io.ktor.client.HttpClient
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.get
import io.ktor.server.application.call
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val logs = MutableStateFlow(emptyList<String>())

    private val client = HttpClient {
        install(Logging) {
            level = LogLevel.BODY
            logger = object : Logger {
                override fun log(message: String) {
                    logs.update { it + message }
                    Log.d("Http", message)
                }
            }
        }
        defaultRequest {
            url("http://localhost:8080/")
        }
    }

    init {
        embeddedServer(Netty, port = 8080) {
            routing {
                get("/abrir") {
                    call.respondText("obrint portes")
                }
                get("/cerrar") {
                    call.respondText("tancant portes")
                }
            }
        }.start(false)
    }

    @OptIn(ExperimentalFoundationApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {


        super.onCreate(savedInstanceState)
        setContent {
            ObretSesamTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    HorizontalPager(state = rememberPagerState(initialPage = 1) { 3 }) {
                        when (it) {
                            0 -> SettingsScreen()
                            1 -> MainScreen({ get("abrir") }, { get("cerrar") })
                            2 -> LogsScreen(logs)
                        }
                    }
                }
            }
        }
    }

    private fun get(endpoint: String) = lifecycleScope.launch {
        runCatching { client.get(endpoint) }.onFailure { error -> logs.update { it + error.message.orEmpty() } }
    }
}

@Composable
fun SettingsScreen() {
    Text(text = "TODO SETTINGS")
}

@Composable
fun LogsScreen(logsFlow: MutableStateFlow<List<String>>) {
    val logs by logsFlow.collectAsState()
    Column(verticalArrangement = Arrangement.Top, modifier = Modifier.fillMaxSize()) {
        Text(text = "Logs", style = MaterialTheme.typography.headlineMedium)
        LazyColumn {
            items(logs) {
                HorizontalDivider(Modifier.padding(vertical = 8.dp))
                Text(text = it)
                HorizontalDivider(Modifier.padding(vertical = 8.dp))
            }
        }
    }
}

@Composable
fun MainScreen(open: () -> Unit, close: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        verticalArrangement = Arrangement.SpaceEvenly,
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.fillMaxSize()
    ) {
        Button(onClick = { open() }, modifier = Modifier.fillMaxWidth(0.5f)) {
            Text(text = "Abril", fontSize = 24.sp)
        }
        Button(onClick = { close() }, modifier = Modifier.fillMaxWidth(0.5f)) {
            Text(text = "Cerral", fontSize = 24.sp)
        }
    }
}
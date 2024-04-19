package cat.martori.sesam

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.lifecycleScope
import cat.martori.sesam.ui.theme.ObretSesamTheme
import io.ktor.client.HttpClient
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
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

val OPEN_URL = stringPreferencesKey("open")
val CLOSE_URL = stringPreferencesKey("close")

class MainActivity : ComponentActivity() {

    private val logsFlow = MutableStateFlow(emptyList<String>())

    private val client = HttpClient {
        install(Logging) {
            level = LogLevel.BODY
            logger = object : Logger {
                override fun log(message: String) {
                    logsFlow.update { it + message }
                    Log.d("Http", message)
                }
            }
        }
    }

    private val preferences by preferencesDataStore("urls")

    private var loading by mutableStateOf(false)

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
                    val endpoints by preferences.data.map { it[OPEN_URL] to it[CLOSE_URL] }
                        .collectAsState(null to null)
                    val (open, close) =
                        (endpoints.first ?: "http://localhost:8080/abrir") to
                                (endpoints.second ?: "http://localhost:8080/cerrar")
                    HorizontalPager(state = rememberPagerState(initialPage = 1) { 3 }) { page ->
                        when (page) {
                            0 -> SettingsScreen(open, close) { newOpen, newClose ->
                                lifecycleScope.launch {
                                    preferences.edit {
                                        it[OPEN_URL] = newOpen
                                        it[CLOSE_URL] = newClose
                                    }
                                }
                            }

                            1 -> MainScreen(open, close)
                            2 -> LogsScreen()
                        }
                    }
                }
            }
        }
    }

    private fun get(endpoint: String) = lifecycleScope.launch {
        loading = true
        runCatching { client.get(endpoint) }.onFailure { error -> logsFlow.update { it + error.message.orEmpty() } }
        loading = false
    }

    @Composable
    fun SettingsScreen(
        openEndpoint: String,
        closeEndpoint: String,
        updateEndpoints: (String, String) -> Unit
    ) {
        Column(
            verticalArrangement = Arrangement.SpaceEvenly,
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxSize()
        ) {
            TextField(
                value = openEndpoint,
                onValueChange = { updateEndpoints(it, closeEndpoint) },
                label = { Text(text = "Url para abrir") },
                singleLine = true,
            )
            TextField(
                value = closeEndpoint,
                onValueChange = { updateEndpoints(openEndpoint, it) },
                label = { Text(text = "Url para cerrar") },
                singleLine = true,
            )

        }
    }

    @Composable
    fun LogsScreen() {
        val logs by logsFlow.collectAsState()
        Column(verticalArrangement = Arrangement.Top, modifier = Modifier.fillMaxSize()) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(4.dp)
            ) {
                Text(text = "Logs", style = MaterialTheme.typography.headlineMedium)
                IconButton(
                    onClick = {
                        logsFlow.update { emptyList() }
                    },
                ) {
                    Icon(
                        painterResource(R.drawable.delete_logs),
                        contentDescription = "clear Logs"
                    )
                }
            }
            LazyColumn {
                items(logs) {
                    HorizontalDivider(Modifier.padding(vertical = 8.dp))
                    Text(text = it, modifier = Modifier.padding(horizontal = 8.dp))
                    HorizontalDivider(Modifier.padding(vertical = 8.dp))
                }
            }
        }
    }

    @Composable
    fun MainScreen(openEndpoint: String, closeEndpoint: String, modifier: Modifier = Modifier) {
        Box(Modifier.fillMaxSize()) {
            if (loading) CircularProgressIndicator(
                modifier = Modifier
                    .size(86.dp)
                    .align(Alignment.Center)
            ) else Column(
                verticalArrangement = Arrangement.SpaceEvenly,
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = modifier.fillMaxSize()
            ) {
                Button(
                    onClick = { get(openEndpoint) },
                    modifier = Modifier.fillMaxWidth(0.5f)
                ) {
                    Text(text = "Abril", fontSize = 24.sp)
                }
                Button(
                    onClick = { get(closeEndpoint) },
                    modifier = Modifier.fillMaxWidth(0.5f)
                ) {
                    Text(text = "Cerral", fontSize = 24.sp)
                }
            }
        }
    }

}

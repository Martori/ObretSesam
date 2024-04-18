package cat.martori.sesam

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import cat.martori.sesam.ui.theme.ObretSesamTheme
import io.ktor.client.HttpClient
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.ANDROID
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.get
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val client = HttpClient {
        install(Logging) {
            level = LogLevel.ALL
            logger = Logger.ANDROID
        }
        defaultRequest {
            url("http://192.168.1.69/")
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ObretSesamTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainScreen({
                        lifecycleScope.launch { client.get("abrir") }
                    }, {
                        lifecycleScope.launch { client.get("cerrar") }
                    })
                }
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
// file: app/src/main/java/com/plum/dnsscanner/MainActivity.kt
package com.plum.dnsscanner

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

private val BgBlack = Color(0xFF000000)
private val CardDark = Color(0xFF141414)
private val Accent = Color(0xFF00FF88)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(colorScheme = darkColorScheme(background = BgBlack, surface = CardDark)) {
                DnsScreen()
            }
        }
    }
}

@Composable
fun DnsScreen() {
    var isTesting by remember { mutableStateOf(false) }
    var currentProgress by remember { mutableStateOf(0) }
    var totalProgress by remember { mutableStateOf(0) }
    var currentServer by remember { mutableStateOf("") }
    var results by remember { mutableStateOf<List<TestResult>>(emptyList()) }
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier.fillMaxSize().background(BgBlack).padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(32.dp))
        Text("DNS SCANNER", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
        Text("YouTube Domains Test", color = Color.Gray, fontSize = 14.sp)
        
        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = {
                if (!isTesting) {
                    isTesting = true
                    results = emptyList()
                    scope.launch(Dispatchers.IO) {
                        val tempResults = mutableListOf<TestResult>()
                        for (server in DnsTester.dnsServers) {
                            currentServer = server.name
                            val result = DnsTester.testServer(server) { current, total ->
                                currentProgress = current
                                totalProgress = total
                            }
                            tempResults.add(result)
                            results = tempResults.toList()
                        }
                        isTesting = false
                    }
                }
            },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = if (isTesting) CardDark else Accent),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(if (isTesting) "ТЕСТИРОВАНИЕ..." else "ЗАПУСТИТЬ ТЕСТ", color = if (isTesting) Accent else BgBlack, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }

        if (isTesting) {
            Spacer(modifier = Modifier.height(16.dp))
            Text("Проверка: $currentServer", color = Accent, fontSize = 14.sp)
            Text("Домен $currentProgress из $totalProgress", color = Color.Gray, fontSize = 12.sp)
            Spacer(modifier = Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = if (totalProgress > 0) currentProgress.toFloat() / totalProgress else 0f,
                modifier = Modifier.fillMaxWidth(),
                color = Accent,
                trackColor = CardDark
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        LazyColumn(modifier = Modifier.fillMaxWidth()) {
            items(results) { res ->
                Card(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                    colors = CardDefaults.cardColors(containerColor = CardDark),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(res.serverName, color = Color.White, fontWeight = FontWeight.Bold)
                            Text("${res.avgTimeMs} ms", color = if (res.avgTimeMs > 0) Accent else Color.Red)
                        }
                        Text("${res.serverIp}", color = Color.Gray, fontSize = 12.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Разрешено: ${res.successCount} / ${res.totalDomains}", color = if (res.successCount == res.totalDomains) Accent else Color.Yellow, fontSize = 14.sp)
                    }
                }
            }
        }
    }
}

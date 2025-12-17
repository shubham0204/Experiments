package io.shubham0204.webgpudemo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.webgpu.BackendType
import io.shubham0204.webgpudemo.ui.theme.WebGPUDemoTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.coroutines.CoroutineContext
import kotlin.math.sqrt
import kotlin.random.Random

class MainActivity : ComponentActivity() {

    private val webGpuComputeShader = WGPUComputeShader()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            WebGPUDemoTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Column(modifier = Modifier.padding(innerPadding)) {
                        GPUInfo()
                        ExecuteOnGPU()
                    }
                }
            }
        }

        val t1 = System.currentTimeMillis()
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun ExecuteOnGPU() {
        var numVectors by rememberSaveable { mutableStateOf(4096) }
        var vectorDims by rememberSaveable { mutableStateOf(64) }
        var computeTimeMillis by rememberSaveable { mutableStateOf<Long?>(null) }
        var isComputing by rememberSaveable { mutableStateOf(false) }
        Column(
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp, start = 16.dp, end = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            TextField(
                modifier = Modifier.fillMaxWidth(),
                value = numVectors.toString(),
                onValueChange = { numVectors = it.toIntOrNull() ?: 0 },
                label = { Text("Number of vectors") },
                keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number),
            )
            Spacer(modifier = Modifier.height(4.dp))
            TextField(
                modifier = Modifier.fillMaxWidth(),
                value = vectorDims.toString(),
                onValueChange = { vectorDims = it.toIntOrNull() ?: 0 },
                label = { Text("Dimensions of each vector") },
                keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number),
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(horizontalArrangement = Arrangement.SpaceAround) {
                Button(
                    onClick = {
                        val vectors =
                            Array(numVectors) { FloatArray(vectorDims) { Random.nextFloat() } }
                        val t1 = System.currentTimeMillis()
                        isComputing = true
                        webGpuComputeShader.execute(vectors) {
                            isComputing = false
                            computeTimeMillis = System.currentTimeMillis() - t1
                        }
                    }
                ) {
                    Text("GPU Compute")
                }
                Button(
                    onClick = {
                        val vectors =
                            Array(numVectors) { FloatArray(vectorDims) { Random.nextFloat() } }
                        val t1 = System.currentTimeMillis()
                        isComputing = true
                        cpuCompute(vectors) {
                            isComputing = false
                            computeTimeMillis = System.currentTimeMillis() - t1
                        }
                    }
                ) {
                    Text("CPU Compute")
                }
            }

        }
        if (computeTimeMillis != null) {
            BasicAlertDialog(onDismissRequest = { computeTimeMillis = null }) {
                Surface(modifier = Modifier.background(Color.White)) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            "Compute time: $computeTimeMillis ms",
                            style = MaterialTheme.typography.headlineMedium,
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedButton(onClick = { computeTimeMillis = null }) { Text("Dismiss") }
                    }
                }
            }
        }
        if (isComputing) {
            Dialog(onDismissRequest = {}) {
                Box(modifier = Modifier.background(Color.White).padding(32.dp)) {
                    CircularProgressIndicator()
                }
            }
        }
    }

    @Composable
    private fun GPUInfo() {
        val info = webGpuComputeShader.getGPUDeviceInfo()
        Card(
            modifier = Modifier.padding(top = 16.dp, start = 16.dp, end = 16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(text = "GPU Device Info", style = MaterialTheme.typography.headlineSmall)
                Text(text = "Vendor: ${info.vendor}")
                Text(text = "Architecture: ${info.architecture}")
                Text(text = "Device: ${info.device}")
                Text(text = "Description: ${info.description}")
                Text(text = "Adapter Type: ${info.adapterType}")
                Text(text = "Vendor ID: ${info.vendorID}")
                Text(text = "Device ID: ${info.deviceID}")
                Text(text = "Subgroup Min Size: ${info.subgroupMinSize}")
                Text(text = "Subgroup Max Size: ${info.subgroupMaxSize}")
                Text(text = "Backend Type: ${BackendType.Companion.toString(info.backendType)}")
            }
        }
    }


    private fun cpuCompute(vectors: Array<FloatArray>, onComplete: (Array<FloatArray>) -> Unit) {
        CoroutineScope(Dispatchers.Default).launch {
            val results = Array(vectors.size) { FloatArray(vectors.size) }
            for (i in 0..<vectors.size) {
                for (j in 0..<vectors.size) {
                    if (i < j) continue
                    var mag1 = 0.0f
                    var mag2 = 0.0f
                    var dotProd = 0.0f
                    for (k in 0..<vectors[0].size) {
                        dotProd += vectors[i][k] * vectors[j][k]
                        mag1 += (vectors[i][k] * vectors[i][k])
                        mag2 += (vectors[j][k] * vectors[j][k])
                    }
                    mag1 = sqrt(mag1)
                    mag2 = sqrt(mag2)
                    results[i][j] = dotProd / (mag1 * mag2)
                }
            }
            onComplete(results)
        }
    }
}

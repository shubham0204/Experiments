package io.shubham0204.webgpudemo

import android.annotation.SuppressLint
import android.util.Log
import androidx.webgpu.AdapterInfo
import androidx.webgpu.BindGroupDescriptor
import androidx.webgpu.BindGroupEntry
import androidx.webgpu.BindGroupLayoutDescriptor
import androidx.webgpu.BindGroupLayoutEntry
import androidx.webgpu.BufferBindingLayout
import androidx.webgpu.BufferBindingType
import androidx.webgpu.BufferDescriptor
import androidx.webgpu.BufferMapCallback
import androidx.webgpu.BufferUsage
import androidx.webgpu.CommandEncoderDescriptor
import androidx.webgpu.ComputePassDescriptor
import androidx.webgpu.ComputePipelineDescriptor
import androidx.webgpu.ComputeState
import androidx.webgpu.DeviceDescriptor
import androidx.webgpu.DeviceLostCallback
import androidx.webgpu.GPUBindGroup
import androidx.webgpu.GPUBindGroupLayout
import androidx.webgpu.GPUBuffer
import androidx.webgpu.GPUComputePipeline
import androidx.webgpu.GPUDevice
import androidx.webgpu.GPUPipelineLayout
import androidx.webgpu.GPUShaderModule
import androidx.webgpu.MapMode
import androidx.webgpu.PipelineLayoutDescriptor
import androidx.webgpu.ShaderModuleDescriptor
import androidx.webgpu.ShaderSourceWGSL
import androidx.webgpu.ShaderStage
import androidx.webgpu.UncapturedErrorCallback
import androidx.webgpu.helper.createWebGpu
import kotlinx.coroutines.runBlocking
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.Executors
import kotlin.math.ceil

/**
 * A WebGPU-based compute shader that calculates pair-wise cosine similarity
 * in a set of given vectors. This class uses the Kotlin WebGPU API, now a part of AndroidX.
 * See https://developer.android.com/jetpack/androidx/releases/webgpu
 */
class WGPUComputeShader {

    private val webGpu = runBlocking { createWebGpu() }
    private val executor = Executors.newSingleThreadExecutor()
    private val logTag = "WGPUComputeShader"
    private val code =
        """
        struct Params {
          numVectors: u32,
          dimension: u32,
        }

        @group(0) @binding(0) var<storage, read> vectors: array<f32>;
        @group(0) @binding(1) var<storage, read_write> similarities: array<f32>;
        @group(0) @binding(2) var<uniform> params: Params;

        @compute @workgroup_size(16, 16)
        fn main(@builtin(global_invocation_id) global_id: vec3<u32>) {
          let i = global_id.x;
          let j = global_id.y;
          let n = params.numVectors;
          let d = params.dimension;
          
          // Only compute upper triangle (including diagonal)
          if (i >= n || j >= n || i > j) {
            return;
          }
          
          var dotProduct: f32 = 0.0;
          var normA: f32 = 0.0;
          var normB: f32 = 0.0;
          
          // Compute dot product and norms
          for (var k: u32 = 0u; k < d; k = k + 1u) {
            let a = vectors[i * d + k];
            let b = vectors[j * d + k];
            dotProduct += a * b;
            normA += a * a;
            normB += b * b;
          }
          
          // Calculate cosine similarity
          let denominator = sqrt(normA) * sqrt(normB);
          var similarity: f32 = 0.0;
          
          if (denominator > 0.0) {
            similarity = dotProduct / denominator;
          }
          
          // Store in both positions for symmetric matrix
          similarities[i * n + j] = similarity;
          similarities[j * n + i] = similarity;
        }
        """
            .trimIndent()

    fun getGPUDeviceInfo(): AdapterInfo = runBlocking {
        val adapter = webGpu.instance.requestAdapter()
        return@runBlocking adapter.getInfo()
    }

    fun execute(vectors: Array<FloatArray>, onComplete: (Array<FloatArray>) -> Unit) = runBlocking {
        val adapter = webGpu.instance.requestAdapter()
        val deviceDescriptor =
            DeviceDescriptor(
                deviceLostCallbackExecutor = executor,
                deviceLostCallback =
                    DeviceLostCallback { device, reason, message ->
                        Log.e(
                            logTag,
                            "GPU device $device lost with reason=$reason and message =$message",
                        )
                    },
                uncapturedErrorCallbackExecutor = executor,
                uncapturedErrorCallback =
                    UncapturedErrorCallback { device, type, message ->
                        Log.e(
                            logTag,
                            "Uncaptured error for $device with type=$type and message =$message",
                        )
                    },
            )
        val device = adapter.requestDevice(deviceDescriptor)
        val shaderModule =
            device.createShaderModule(
                ShaderModuleDescriptor(shaderSourceWGSL = ShaderSourceWGSL(code = code))
            )

        val numVectors = vectors.size.toLong()
        val vectorDims = vectors[0].size.toLong()
        val vectorsBuffer = createVectorsBuffer(device, numVectors, vectorDims)
        val similaritiesBuffer = createSimilaritiesBuffer(device, numVectors)
        val paramsBuffer = createParamsBuffer(device)

        val vectorsData = vectors.flatMap { it.toList() }
        val vectorsByteBuffer =
            ByteBuffer.allocateDirect((numVectors * vectorDims * 4).toInt())
                .order(ByteOrder.nativeOrder())
        vectorsData.forEach(vectorsByteBuffer::putFloat)
        vectorsByteBuffer.flip()
        device.queue.writeBuffer(vectorsBuffer, 0, vectorsByteBuffer)

        val paramsByteBuffer = ByteBuffer.allocateDirect(8).order(ByteOrder.nativeOrder())
        paramsByteBuffer.putInt(numVectors.toInt())
        paramsByteBuffer.putInt(vectorDims.toInt())
        paramsByteBuffer.flip()
        device.queue.writeBuffer(paramsBuffer, 0, paramsByteBuffer)

        val bindGroupLayout = createBindGroupLayout(device)
        val bindGroup =
            createBindGroup(
                device,
                bindGroupLayout,
                vectorsBuffer,
                similaritiesBuffer,
                paramsBuffer,
            )

        val pipelineLayout = createPipelineLayout(device, bindGroupLayout)
        val pipeline = createPipeline(device, pipelineLayout, shaderModule)

        val commandEncoder = device.createCommandEncoder(CommandEncoderDescriptor())
        val computePass = commandEncoder.beginComputePass(ComputePassDescriptor())
        computePass.setPipeline(pipeline)
        computePass.setBindGroup(0, bindGroup)

        val workGroupSize = ceil(numVectors.toFloat() / 16f).toInt()
        computePass.dispatchWorkgroups(workGroupSize, workGroupSize, 1)
        computePass.end()

        val resultsBuffer = createResultsBuffer(device, numVectors)
        commandEncoder.copyBufferToBuffer(
            similaritiesBuffer,
            0,
            resultsBuffer,
            0,
            similaritiesBuffer.size,
        )

        val commandBuffer = commandEncoder.finish()
        device.queue.submit(arrayOf(commandBuffer))

        resultsBuffer.mapAsync(
            MapMode.Read,
            0,
            resultsBuffer.size,
            executor,
            BufferMapCallback { status, message ->
                Log.i(logTag, "resultsBuffer mapped with status=$status and message=$message")
                val resultData = resultsBuffer.getConstMappedRange(0, resultsBuffer.size)
                val resultArray = Array(numVectors.toInt()) { FloatArray(numVectors.toInt()) }
                for (i in 0 until numVectors.toInt()) {
                    for (j in 0 until numVectors.toInt()) {
                        resultArray[i][j] = resultData.getFloat()
                    }
                }
                onComplete(resultArray)
            },
        )
    }

    @SuppressLint("RestrictedApi")
    private fun createVectorsBuffer(
        device: GPUDevice,
        numVectors: Long,
        vectorDims: Long,
    ): GPUBuffer {
        return device.createBuffer(
            BufferDescriptor(
                label = "vectors",
                size = numVectors * vectorDims * 4,
                usage = BufferUsage.Storage or BufferUsage.CopyDst,
                mappedAtCreation = false,
            )
        )
    }

    @SuppressLint("RestrictedApi")
    private fun createSimilaritiesBuffer(device: GPUDevice, numVectors: Long): GPUBuffer {
        return device.createBuffer(
            BufferDescriptor(
                label = "similarities",
                size = numVectors * numVectors * 4,
                usage = BufferUsage.Storage or BufferUsage.CopySrc,
                mappedAtCreation = false,
            )
        )
    }

    @SuppressLint("RestrictedApi")
    private fun createParamsBuffer(device: GPUDevice): GPUBuffer {
        return device.createBuffer(
            BufferDescriptor(
                label = "params",
                size = 8,
                usage = BufferUsage.Uniform or BufferUsage.CopyDst,
                mappedAtCreation = false,
            )
        )
    }

    @SuppressLint("RestrictedApi")
    private fun createResultsBuffer(device: GPUDevice, numVectors: Long): GPUBuffer {
        return device.createBuffer(
            BufferDescriptor(
                label = "results",
                size = numVectors * numVectors * 4,
                usage = BufferUsage.MapRead or BufferUsage.CopyDst,
                mappedAtCreation = false,
            )
        )
    }

    @SuppressLint("RestrictedApi")
    private fun createBindGroupLayout(device: GPUDevice): GPUBindGroupLayout {
        return device.createBindGroupLayout(
            BindGroupLayoutDescriptor(
                entries =
                    arrayOf(
                        BindGroupLayoutEntry(
                            binding = 0,
                            visibility = ShaderStage.Compute,
                            buffer =
                                BufferBindingLayout(
                                    type = BufferBindingType.ReadOnlyStorage,
                                    hasDynamicOffset = false,
                                    minBindingSize = 4,
                                ),
                        ),
                        BindGroupLayoutEntry(
                            binding = 1,
                            visibility = ShaderStage.Compute,
                            buffer =
                                BufferBindingLayout(
                                    type = BufferBindingType.Storage,
                                    hasDynamicOffset = false,
                                    minBindingSize = 4,
                                ),
                        ),
                        BindGroupLayoutEntry(
                            binding = 2,
                            visibility = ShaderStage.Compute,
                            buffer =
                                BufferBindingLayout(
                                    type = BufferBindingType.Uniform,
                                    hasDynamicOffset = false,
                                    minBindingSize = 8,
                                ),
                        ),
                    )
            )
        )
    }

    private fun createBindGroup(
        device: GPUDevice,
        bindGroupLayout: GPUBindGroupLayout,
        vectorsBuffer: GPUBuffer,
        similaritiesBuffer: GPUBuffer,
        paramsBuffer: GPUBuffer,
    ): GPUBindGroup {
        return device.createBindGroup(
            BindGroupDescriptor(
                layout = bindGroupLayout,
                entries =
                    arrayOf(
                        BindGroupEntry(binding = 0, buffer = vectorsBuffer),
                        BindGroupEntry(binding = 1, buffer = similaritiesBuffer),
                        BindGroupEntry(binding = 2, buffer = paramsBuffer),
                    ),
            )
        )
    }

    private fun createPipelineLayout(
        device: GPUDevice,
        bindGroupLayout: GPUBindGroupLayout,
    ): GPUPipelineLayout {
        return device.createPipelineLayout(
            PipelineLayoutDescriptor(bindGroupLayouts = arrayOf(bindGroupLayout))
        )
    }

    private fun createPipeline(
        device: GPUDevice,
        pipelineLayout: GPUPipelineLayout,
        shaderModule: GPUShaderModule,
    ): GPUComputePipeline {
        return device.createComputePipeline(
            ComputePipelineDescriptor(
                layout = pipelineLayout,
                compute = ComputeState(module = shaderModule, entryPoint = "main"),
            )
        )
    }
}

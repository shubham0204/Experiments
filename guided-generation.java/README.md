# Guided Generation With llama.cpp

> Generating Java Data Structures With LLMs Like Apple's Foundation Models Framework

## Setup


### Prerequisites

- OpenJDK 21+ compatible JDK
- Maven
- [`llama.cpp`](https://github.com/ggml-org/llama.cpp), specifically `llama-server`

### Steps

1. Download `Llama-3.2-3B-Instruct-Q4_0.gguf` from [HuggingFace](https://huggingface.co/bartowski/Llama-3.2-3B-Instruct-GGUF/blob/main/Llama-3.2-3B-Instruct-Q4_0.gguf)
2. Run the following command:

    ```bash
    llama-server -m Llama-3.2-3B-Instruct-Q4_0.gguf
    ```
3. Compile the project and execute the `Main` class:

    ```bash
    mvn compile exec:java -Dexec.mainClass="io.shubham0204.Main"
    ```
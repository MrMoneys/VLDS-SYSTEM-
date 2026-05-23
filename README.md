# VLDS — Verified License & Device System

<div align="center">

![VLDS Banner](https://img.shields.io/badge/VLDS-v1.0-00CCFF?style=for-the-badge&logo=android)
![Platform](https://img.shields.io/badge/Platform-Android-green?style=for-the-badge&logo=android)
![Language](https://img.shields.io/badge/Language-Java%20%7C%20C%2B%2B%20%7C%20PHP-blue?style=for-the-badge)

**A robust 2-step license validation system for Android mod menus.**  
**Um sistema robusto de validação de licença em 2 etapas para mod menus Android.**

by [@MrMoneys_ofc](https://t.me/MrMoneys_ofc)

</div>

---

## 📋 Table of Contents / Índice

- [Overview / Visão Geral](#overview)
- [Architecture / Arquitetura](#architecture)
- [Security Layers / Camadas de Segurança](#security-layers)
- [How It Works / Como Funciona](#how-it-works)
- [Server Side / Lado Servidor](#server-side)
- [Java Side / Lado Java](#java-side)
  - [Loading Native Libraries / Carregando as Libs Nativas](#loading-native-libraries--carregando-as-libs-nativas)
- [Native Side / Lado Nativo](#native-side)
- [Admin Panel / Painel Admin](#admin-panel)
- [API Reference](#api-reference)
- [Security Tips / Dicas de Segurança](#security-tips)
- [FAQ](#faq)

---

## Overview / Visão Geral

<table>
<tr>
<td width="50%">

### 🇧🇷 Português

O **VLDS** é um sistema de validação de licenças em 2 etapas para mod menus Android. A ideia central é que a validação final **nunca acontece no Java** — sempre na camada nativa C++, que é muito mais difícil de manipular.

**Componentes:**
- `verify.php` — endpoint de validação no servidor
- `index.php` — painel de administração
- `Main.java` — tela de login + request ao servidor
- `Main.cpp` — validação final + proteções

</td>
<td width="50%">

### 🇺🇸 English

**VLDS** is a 2-step license validation system for Android mod menus. The core idea is that the final validation **never happens in Java** — always in the C++ native layer, which is much harder to manipulate.

**Components:**
- `verify.php` — server-side validation endpoint
- `index.php` — admin panel
- `Main.java` — login screen + server request
- `Main.cpp` — final validation + protections

</td>
</tr>
</table>

---

## Architecture / Arquitetura

```
┌─────────────────────────────────────────────────────┐
│                    VLDS FLOW                        │
├─────────────────────────────────────────────────────┤
│                                                     │
│  [Java] static block → System.loadLibrary()         │
│       ↓                                             │
│  [Java] Anti-checks (root / emulator / GG)          │
│       ↓                                             │
│  [Java] POST → server (key + device info)           │
│       ↓                                             │
│  [PHP] Validation chain                             │
│  └── blacklist → key → device → HMAC token         │
│       ↓                                             │
│  [Java] Parse JSON → extract tokens                 │
│       ↓                                             │
│  [C++] Native validates token format                │
│       ↓                                             │
│  [C++] Protections loop                             │
│       ↓                                             │
│  ✓ g_keyOk = true → Menu unlocked                  │
│                                                     │
└─────────────────────────────────────────────────────┘
```

---

## Security Layers / Camadas de Segurança

| # | Layer | EN | PT |
|---|-------|----|----|
| 1 | Anti-Root | Detects Magisk, KernelSU, SuperSU | Detecta Magisk, KernelSU, SuperSU |
| 2 | Anti-Emulator | Detects AVD, Genymotion | Detecta AVD, Genymotion |
| 3 | Anti-GG | Detects Game Guardian | Detecta Game Guardian |
| 4 | Server Validation | Key + device + blacklist | Key + device + blacklist |
| 5 | HMAC Token | Rotating token (1h) | Token rotativo (1h) |
| 6 | Native Validation | C++ validates token | C++ valida token |
| 7 | Protections | Continuous loop | Loop contínuo |
| 8 | Obfuscation | ProGuard + dex2c | ProGuard + dex2c |

---

## How It Works / Como Funciona

<table>
<tr>
<td width="50%">

### 🇧🇷 Português

**1. Java carrega as libs nativas**  
O bloco `static` executa `System.loadLibrary()` assim que a classe é carregada — antes de qualquer outra coisa.

**2. Java roda os anti-checks**  
Antes de qualquer request, o app verifica se o ambiente é seguro.

**3. Java envia o POST**  
O app envia a key e identificador do dispositivo para o servidor.

**4. Servidor valida a cadeia completa**  
Verifica blacklist → key → dispositivo → expiração → gera token HMAC.

**5. Token HMAC rotativo**  
O servidor gera um token que muda a cada hora. Dois tokens são retornados — hora atual e hora anterior — para evitar falhas na virada da hora.

**6. Java passa para a nativa**  
O Java extrai os campos do JSON e passa para um método nativo com nome ofuscado.

**7. C++ faz a validação final**  
A nativa separa os campos, valida o formato e só então seta `g_keyOk = true`.

**8. `g_keyOk` é o guardião**  
`CheckOverlayPermission` tem `if (!g_keyOk) return;` no topo — sem validação, nada abre.

</td>
<td width="50%">

### 🇺🇸 English

**1. Java loads the native libs**  
The `static` block runs `System.loadLibrary()` as soon as the class is loaded — before anything else.

**2. Java runs anti-checks**  
Before any request, the app checks if the environment is safe.

**3. Java sends the POST**  
The app sends the key and device identifier to the server.

**4. Server validates the full chain**  
Checks blacklist → key → device → expiration → generates HMAC token.

**5. Rotating HMAC token**  
The server generates a token that changes every hour. Two tokens are returned — current and previous hour — to avoid failures at hour boundaries.

**6. Java passes to native**  
Java extracts the JSON fields and passes them to a native method with an obfuscated name.

**7. C++ does the final validation**  
The native splits the fields, validates the format, and only then sets `g_keyOk = true`.

**8. `g_keyOk` is the guardian**  
`CheckOverlayPermission` has `if (!g_keyOk) return;` at the top — without validation, nothing opens.

</td>
</tr>
</table>

---

## Server Side / Lado Servidor

<table>
<tr>
<td width="50%">

### 🇧🇷 Português

O servidor precisa de um endpoint PHP que receba os dados do app, valide e retorne um JSON com um token HMAC.

**Você decide os nomes dos parâmetros POST e dos campos JSON** — o importante é que Java e servidor estejam sincronizados.

**Estrutura mínima sugerida:**

```php
<?php
// Receba seus próprios parâmetros — escolha os nomes
$key      = $_POST['key']      ?? '';
$deviceId = $_POST['device']   ?? '';
$model    = $_POST['model']    ?? '';

if (empty($key) || empty($deviceId)) {
    echo json_encode(['result' => 'fail', 'info' => 'missing']);
    exit;
}

// Implemente sua lógica de validação aqui:
// - Checar blacklist de devices
// - Checar se a key existe e está ativa
// - Checar expiração
// - Checar limite de devices
// ...

// Se tudo ok, gere os tokens HMAC
$secret  = 'SEU_SECRET_LONGO'; // só no servidor!
$period  = floor(time() / (86400 * 2)); // rotaciona a cada 2 dias
$base    = hash('sha256', $secret . $period);

$token1 = hash_hmac('sha256', $key . $deviceId . date('YmdH'), $base);
$token2 = hash_hmac('sha256', $key . $deviceId . date('YmdH', time() - 3600), $base);

echo json_encode([
    'result' => 'ok',
    'info'   => 'valid',
    'user'   => $username,
    'type'   => $keyType,
    'days'   => $daysLeft,
    'a'      => $token1,  // nome curto — menos óbvio
    'b'      => $token2,
]);
```

> ⚠️ Os nomes dos campos JSON (`result`, `info`, `a`, `b`) são só sugestões. **Use os seus próprios nomes.**

</td>
<td width="50%">

### 🇺🇸 English

The server needs a PHP endpoint that receives data from the app, validates it, and returns a JSON with an HMAC token.

**You decide the POST parameter names and JSON field names** — what matters is that Java and server are in sync.

**Suggested minimum structure:**

```php
<?php
// Receive your own parameters — choose the names
$key      = $_POST['key']      ?? '';
$deviceId = $_POST['device']   ?? '';
$model    = $_POST['model']    ?? '';

if (empty($key) || empty($deviceId)) {
    echo json_encode(['result' => 'fail', 'info' => 'missing']);
    exit;
}

// Implement your validation logic here:
// - Check device blacklist
// - Check if key exists and is active
// - Check expiration
// - Check device limit
// ...

// If all ok, generate HMAC tokens
$secret  = 'YOUR_LONG_SECRET'; // server only!
$period  = floor(time() / (86400 * 2)); // rotates every 2 days
$base    = hash('sha256', $secret . $period);

$token1 = hash_hmac('sha256', $key . $deviceId . date('YmdH'), $base);
$token2 = hash_hmac('sha256', $key . $deviceId . date('YmdH', time() - 3600), $base);

echo json_encode([
    'result' => 'ok',
    'info'   => 'valid',
    'user'   => $username,
    'type'   => $keyType,
    'days'   => $daysLeft,
    'a'      => $token1,  // short name — less obvious
    'b'      => $token2,
]);
```

> ⚠️ The JSON field names (`result`, `info`, `a`, `b`) are just suggestions. **Use your own names.**

</td>
</tr>
</table>

---

## Java Side / Lado Java

<table>
<tr>
<td width="50%">

### 🇧🇷 Português

O Java tem **3 responsabilidades** — nada mais:

1. Carregar as libs nativas no bloco `static`
2. Rodar os anti-checks antes do request
3. Enviar o POST, receber o JSON e passar os tokens para a nativa

```java
// Declare o método nativo — escolha seu próprio nome
private static native void seuMetodo(String token);

// Parse do JSON — use os nomes que definiu no servidor
String result = json.optString("result", "fail");
String info   = json.optString("info",   "");
String tokenA = json.optString("a", "");
String tokenB = json.optString("b", "");

boolean ok = result.equals("ok");

// Concatena e passa para a nativa
if (ok && !tokenA.isEmpty())
    info = info + "|" + tokenA + "|" + tokenB;

seuMetodo(info);
```

**O Java não decide se o menu abre.**  
Essa decisão é 100% da nativa.

</td>
<td width="50%">

### 🇺🇸 English

Java has **3 responsibilities** — nothing more:

1. Load native libs in the `static` block
2. Run anti-checks before the request
3. Send the POST, receive the JSON, and pass tokens to native

```java
// Declare native method — choose your own name
private static native void yourMethod(String token);

// JSON parsing — use the names you defined on the server
String result = json.optString("result", "fail");
String info   = json.optString("info",   "");
String tokenA = json.optString("a", "");
String tokenB = json.optString("b", "");

boolean ok = result.equals("ok");

// Concatenate and pass to native
if (ok && !tokenA.isEmpty())
    info = info + "|" + tokenA + "|" + tokenB;

yourMethod(info);
```

**Java does not decide if the menu opens.**  
That decision is 100% native.

</td>
</tr>
</table>

---

### Loading Native Libraries / Carregando as Libs Nativas

> ⚠️ **Esta é uma adição ao seu `Main.java` existente — não uma substituição.**  
> ⚠️ **This is an addition to your existing `Main.java` — not a replacement.**

<table>
<tr>
<td width="50%">

### 🇧🇷 Português

O VLDS depende de métodos nativos C++ — e **nenhum deles funciona se as libs não forem carregadas primeiro**.

Adicione um bloco `static` no topo da sua classe `Main` (ou equivalente) para carregar as libs do seu mod menu. Esse bloco executa automaticamente quando a classe é carregada pelo Android, antes de qualquer método ser chamado.

**Como adicionar no seu `Main.java`:**

```java
public class Main extends Activity { // sua classe existente

    // ─── ADICIONE ESTE BLOCO ────────────────────────────
    static {
        // Substitua pelo nome real da sua lib (.so)
        // Sem "lib" no começo e sem ".so" no final
        System.loadLibrary("seumenu");
    }
    // ────────────────────────────────────────────────────

    // Declare o método nativo — escolha seu próprio nome
    private static native void seuMetodo(String token);

    // ... resto do seu código existente
}
```

**Se o seu projeto tiver múltiplas libs**, carregue na ordem de dependência — a lib que é dependência vem primeiro:

```java
static {
    System.loadLibrary("utils");   // dependência — carrega primeiro
    System.loadLibrary("seumenu"); // depende de utils — carrega depois
}
```

> ℹ️ O nome passado para `loadLibrary` é o nome do arquivo `.so` sem o prefixo `lib` e sem a extensão. Ex: `libseumenu.so` → `"seumenu"`.

**Erros comuns:**

| Erro | Causa |
|------|-------|
| `UnsatisfiedLinkError` | Lib não foi carregada ou nome errado |
| `java.lang.UnsatisfiedLinkError: no seumenu in java.library.path` | Nome incorreto no `loadLibrary` |
| Crash silencioso na chamada nativa | `loadLibrary` ausente ou na ordem errada |

</td>
<td width="50%">

### 🇺🇸 English

VLDS depends on C++ native methods — and **none of them work if the libs aren't loaded first**.

Add a `static` block at the top of your `Main` class (or equivalent) to load your mod menu's libs. This block executes automatically when the class is loaded by Android, before any method is called.

**How to add to your `Main.java`:**

```java
public class Main extends Activity { // your existing class

    // ─── ADD THIS BLOCK ─────────────────────────────────
    static {
        // Replace with your actual lib name (.so)
        // No "lib" prefix and no ".so" extension
        System.loadLibrary("yourmenu");
    }
    // ────────────────────────────────────────────────────

    // Declare native method — choose your own name
    private static native void yourMethod(String token);

    // ... rest of your existing code
}
```

**If your project has multiple libs**, load them in dependency order — the dependency lib comes first:

```java
static {
    System.loadLibrary("utils");    // dependency — loads first
    System.loadLibrary("yourmenu"); // depends on utils — loads after
}
```

> ℹ️ The name passed to `loadLibrary` is the `.so` filename without the `lib` prefix and without the extension. Ex: `libyourmenu.so` → `"yourmenu"`.

**Common errors:**

| Error | Cause |
|-------|-------|
| `UnsatisfiedLinkError` | Lib not loaded or wrong name |
| `java.lang.UnsatisfiedLinkError: no yourmenu in java.library.path` | Wrong name in `loadLibrary` |
| Silent crash on native call | `loadLibrary` missing or wrong order |

</td>
</tr>
</table>

---

## Native Side / Lado Nativo

<table>
<tr>
<td width="50%">

### 🇧🇷 Português

O C++ tem **2 responsabilidades**:

1. Validar o token recebido do Java
2. Setar `g_keyOk = true` só se tudo estiver correto

**Estrutura mínima:**

```cpp
// Flag global — guardião do menu
bool g_keyOk = false;

// Método de validação — escolha seu próprio nome
static void seuMetodo(JNIEnv* env, jclass, jstring jtoken) {
    if (!jtoken) return;

    const char* t = env->GetStringUTFChars(jtoken, nullptr);
    std::string full(t);
    env->ReleaseStringUTFChars(jtoken, t);

    // Separa os campos pelo delimitador que escolheu
    size_t sep1 = full.find('|');
    if (sep1 == std::string::npos) return;

    size_t sep2 = full.find('|', sep1 + 1);
    if (sep2 == std::string::npos) return;

    std::string info   = full.substr(0, sep1);
    std::string tkNow  = full.substr(sep1 + 1, sep2 - sep1 - 1);
    std::string tkPrev = full.substr(sep2 + 1);

    // Valide info e tamanho dos tokens (SHA256 = 64 chars)
    const char* expected = "sua_string_valida";
    bool validTk = (tkNow.length() == 64 || tkPrev.length() == 64);

    if (info == expected && validTk)
        g_keyOk = true;
}

// Guardião — nada abre sem validação
void CheckOverlayPermission(...) {
    if (!g_keyOk) return; // ← essencial
    // ... abre o menu
}
```

**Registro no JNI — obrigatório:**

```cpp
extern "C" JNIEXPORT jint JNICALL
JNI_OnLoad(JavaVM *vm, void *reserved) {
    JNIEnv *env;
    vm->GetEnv((void **) &env, JNI_VERSION_1_6);

    // Registre todos os seus métodos aqui
    // ex: RegisterMain(env), RegisterMenu(env)...

    return JNI_VERSION_1_6;
}

// Dentro do seu RegisterMain:
JNINativeMethod methods[] = {
    {"CheckOverlayPermission",
        "(Landroid/content/Context;)V",
        (void*)CheckOverlayPermission},
    {"seuMetodo",               // mesmo nome do Java
        "(Ljava/lang/String;)V",
        (void*)seuMetodo},
};
```

</td>
<td width="50%">

### 🇺🇸 English

C++ has **2 responsibilities**:

1. Validate the token received from Java
2. Set `g_keyOk = true` only if everything is correct

**Minimum structure:**

```cpp
// Global flag — menu guardian
bool g_keyOk = false;

// Validation method — choose your own name
static void yourMethod(JNIEnv* env, jclass, jstring jtoken) {
    if (!jtoken) return;

    const char* t = env->GetStringUTFChars(jtoken, nullptr);
    std::string full(t);
    env->ReleaseStringUTFChars(jtoken, t);

    // Split fields by your chosen delimiter
    size_t sep1 = full.find('|');
    if (sep1 == std::string::npos) return;

    size_t sep2 = full.find('|', sep1 + 1);
    if (sep2 == std::string::npos) return;

    std::string info   = full.substr(0, sep1);
    std::string tkNow  = full.substr(sep1 + 1, sep2 - sep1 - 1);
    std::string tkPrev = full.substr(sep2 + 1);

    // Validate info and token length (SHA256 = 64 chars)
    const char* expected = "your_valid_string";
    bool validTk = (tkNow.length() == 64 || tkPrev.length() == 64);

    if (info == expected && validTk)
        g_keyOk = true;
}

// Guardian — nothing opens without validation
void CheckOverlayPermission(...) {
    if (!g_keyOk) return; // ← essential
    // ... open menu
}
```

**JNI Registration — required:**

```cpp
extern "C" JNIEXPORT jint JNICALL
JNI_OnLoad(JavaVM *vm, void *reserved) {
    JNIEnv *env;
    vm->GetEnv((void **) &env, JNI_VERSION_1_6);

    // Register all your methods here
    // e.g.: RegisterMain(env), RegisterMenu(env)...

    return JNI_VERSION_1_6;
}

// Inside your RegisterMain:
JNINativeMethod methods[] = {
    {"CheckOverlayPermission",
        "(Landroid/content/Context;)V",
        (void*)CheckOverlayPermission},
    {"yourMethod",              // same name as Java
        "(Ljava/lang/String;)V",
        (void*)yourMethod},
};
```

</td>
</tr>
</table>

---

## Admin Panel / Painel Admin

<table>
<tr>
<td width="50%">

### 🇧🇷 Português

O painel é uma interface PHP protegida por login que gerencia:

- **Licenças** — criar, editar, ativar, banir, deletar
- **Dispositivos** — visualizar, banir globalmente, remover
- **Logs** — histórico de verificações com status e IP
- **Dashboard** — estatísticas gerais

**Campos sugeridos por licença:**
- Identificador único (key)
- Usuário vinculado
- Tipo (`free`, `premium`, `vip`, `lifetime`)
- Dias restantes + data de expiração
- Máximo de dispositivos
- Status ativo/banido
- Device fixo opcional (lock)

</td>
<td width="50%">

### 🇺🇸 English

The panel is a PHP interface protected by login that manages:

- **Licenses** — create, edit, activate, ban, delete
- **Devices** — view, globally ban, remove
- **Logs** — verification history with status and IP
- **Dashboard** — general statistics

**Suggested fields per license:**
- Unique identifier (key)
- Linked user
- Type (`free`, `premium`, `vip`, `lifetime`)
- Days remaining + expiration date
- Max devices
- Active/banned status
- Optional fixed device (lock)

</td>
</tr>
</table>

---

## API Reference

### POST `/verify.php`

> The field names below are **suggestions only**. Use your own names in your implementation.

**Request — suggested fields:**
| Field | Required | Description |
|-------|----------|-------------|
| `key` | ✅ | The license key |
| `device` | ✅ | Device identifier |
| `model` | ❌ | Device model |
| `brand` | ❌ | Device manufacturer |

**Success Response — suggested structure:**
```json
{
  "result": "ok",
  "info":   "valid",
  "user":   "username",
  "type":   "premium",
  "days":   30,
  "a":      "64-char HMAC token (current hour)",
  "b":      "64-char HMAC token (previous hour)"
}
```

**Error — suggested `info` values:**

| info | Cause |
|------|-------|
| `missing` | Empty key or device |
| `not_found` | Key doesn't exist |
| `inactive` | Key disabled |
| `banned` | Key banned |
| `expired` | Key expired |
| `wrong_device` | Device not authorized |
| `limit` | Max devices exceeded |
| `dev_banned` | Device blacklisted |
| `error` | Internal server error |

> You can use any string values you want — just keep Java and server in sync.

---

## Security Tips / Dicas de Segurança

<table>
<tr>
<td width="50%">

### 🇧🇷 Português

**🔒 Obfuscação de strings no C++**

A maioria das bases de mod menu já inclui o **AY_OBFUSCATE** — uma macro que ofusca strings em tempo de compilação, deixando-as invisíveis no binário. Se a sua base já tem, use-a nas strings sensíveis como o valor esperado da validação.

```cpp
// Se sua base tem AY_OBFUSCATE / OBFUSCATE:
const char* expected = OBFUSCATE("sua_string");

// Se não tiver, pode usar diretamente — mas
// considere adicionar uma biblioteca de ofuscação
const char* expected = "sua_string";
```

> Não é obrigatório — é uma camada extra de proteção.

---

**🔒 Ofuscação de Strings via XOR (C++)**

Se a sua base não tem AY_OBFUSCATE, você pode implementar uma ofuscação simples por XOR. A string fica cifrada no binário e só é decodificada em tempo de execução — invisível para ferramentas como `strings` ou IDA.

```cpp
// Utilitário XOR — adicione no seu header ou .cpp
#include <string>

static std::string xorDecrypt(const char* data, size_t len, uint8_t key) {
    std::string result(data, len);
    for (size_t i = 0; i < len; i++)
        result[i] ^= key;
    return result;
}

// Exemplo de uso no método de validação:
// String "valid" cifrada com XOR 0x5A: {0x2C,0x3B,0x36,0x37,0x3E}
// (gere os bytes com um script Python antes de compilar)
static void seuMetodo(JNIEnv* env, jclass, jstring jtoken) {
    // Bytes da string "valid" cifrada com XOR 0x5A
    const char enc[] = {0x2C, 0x3B, 0x36, 0x37, 0x3E};
    std::string expected = xorDecrypt(enc, sizeof(enc), 0x5A);

    // ... resto da validação
    if (info == expected && validTk)
        g_keyOk = true;
}
```

**Como gerar os bytes cifrados (Python):**

```python
# Rode isso uma vez para gerar os bytes da sua string
key = 0x5A
text = "sua_string_valida"
enc = [hex(ord(c) ^ key) for c in text]
print(", ".join(enc))
# Saída: 0x2c, 0x3b, ... → copie para o C++
```

> ⚠️ Use uma chave XOR diferente da usada em outros lugares do seu projeto. Combine com OBFUSCATE se disponível.

---

**🔒 Nomes de métodos ofuscados**

Use nomes difíceis de identificar para o método nativo. Caracteres ambíguos funcionam muito bem:

```java
// Difícil de ler — I, l, 1, O, 0 misturados
private static native void lI1OIlII(String t);
```

---

**🔒 dex2c no método Java**

Compile o método que chama a nativa com **dex2c** — ele vira código ARM64 nativo e some do dex completamente.

---

**🔒 ProGuard**

Ative com ofuscação máxima. Mantenha o método nativo com `@Keep` ou no `-keep` do ProGuard.

---

**🔒 Secret HMAC**

O secret nunca deve estar no APK. Coloque apenas no servidor. Use uma string longa e aleatória — `bin2hex(random_bytes(32))` gera uma boa opção.

---

**🔒 Proteções em loop**

Implemente verificações contínuas na `hack_thread` — rode a cada poucos segundos e encerre o processo se detectar ambiente comprometido.

</td>
<td width="50%">

### 🇺🇸 English

**🔒 String obfuscation in C++**

Most mod menu bases already include **AY_OBFUSCATE** — a macro that obfuscates strings at compile time, making them invisible in the binary. If your base already has it, use it on sensitive strings like the expected validation value.

```cpp
// If your base has AY_OBFUSCATE / OBFUSCATE:
const char* expected = OBFUSCATE("your_string");

// If it doesn't, you can use directly — but
// consider adding an obfuscation library
const char* expected = "your_string";
```

> Not mandatory — it's an extra protection layer.

---

**🔒 XOR String Obfuscation (C++)**

If your base doesn't have AY_OBFUSCATE, you can implement simple XOR obfuscation. The string is stored encrypted in the binary and only decoded at runtime — invisible to tools like `strings` or IDA.

```cpp
// XOR utility — add to your header or .cpp
#include <string>

static std::string xorDecrypt(const char* data, size_t len, uint8_t key) {
    std::string result(data, len);
    for (size_t i = 0; i < len; i++)
        result[i] ^= key;
    return result;
}

// Example usage in the validation method:
// String "valid" encrypted with XOR 0x5A: {0x2C,0x3B,0x36,0x37,0x3E}
// (generate bytes with a Python script before compiling)
static void yourMethod(JNIEnv* env, jclass, jstring jtoken) {
    // Bytes of string "valid" encrypted with XOR 0x5A
    const char enc[] = {0x2C, 0x3B, 0x36, 0x37, 0x3E};
    std::string expected = xorDecrypt(enc, sizeof(enc), 0x5A);

    // ... rest of validation
    if (info == expected && validTk)
        g_keyOk = true;
}
```

**How to generate the encrypted bytes (Python):**

```python
# Run this once to generate bytes for your string
key = 0x5A
text = "your_valid_string"
enc = [hex(ord(c) ^ key) for c in text]
print(", ".join(enc))
# Output: 0x2c, 0x3b, ... → copy to C++
```

> ⚠️ Use a XOR key different from others used in your project. Combine with OBFUSCATE if available.

---

**🔒 Obfuscated method names**

Use hard-to-identify names for the native method. Ambiguous characters work great:

```java
// Hard to read — I, l, 1, O, 0 mixed
private static native void lI1OIlII(String t);
```

---

**🔒 dex2c on the Java method**

Compile the method that calls native with **dex2c** — it becomes native ARM64 code and disappears from the dex completely.

---

**🔒 ProGuard**

Enable with maximum obfuscation. Keep the native method with `@Keep` or in ProGuard's `-keep` rule.

---

**🔒 HMAC Secret**

The secret must never be in the APK. Put it on the server only. Use a long random string — `bin2hex(random_bytes(32))` generates a good one.

---

**🔒 Protection loops**

Implement continuous checks in `hack_thread` — run every few seconds and kill the process if a compromised environment is detected.

</td>
</tr>
</table>

---

## FAQ

<details>
<summary><b>🇧🇷 Por que o Java não decide se o menu abre?</b></summary>

O Java é facilmente hookável via Frida — qualquer método pode ter seu retorno forçado. A decisão final no C++ com `g_keyOk` é muito mais resistente, especialmente com nomes ofuscados e dex2c.

</details>

<details>
<summary><b>🇺🇸 Why doesn't Java decide if the menu opens?</b></summary>

Java is easily hookable via Frida — any method can have its return forced. The final decision in C++ with `g_keyOk` is much more resilient, especially with obfuscated names and dex2c.

</details>

<details>
<summary><b>🇧🇷 Por que dois tokens?</b></summary>

O token muda a cada hora. Se o usuário valida às 23:59 e a nativa checa às 00:01, o token já mudou. O segundo token (hora anterior) garante que isso não quebre a validação.

</details>

<details>
<summary><b>🇺🇸 Why two tokens?</b></summary>

The token changes every hour. If the user validates at 23:59 and the native checks at 00:01, the token has already changed. The second token (previous hour) ensures this doesn't break validation.

</details>

<details>
<summary><b>🇧🇷 O JNI_OnLoad é obrigatório?</b></summary>

Sim! Sem o `JNI_OnLoad` registrando os métodos, o Java não consegue encontrar a função nativa e a chamada falha silenciosamente. Certifique-se de registrar **todos** os seus métodos nativos lá.

</details>

<details>
<summary><b>🇺🇸 Is JNI_OnLoad required?</b></summary>

Yes! Without `JNI_OnLoad` registering the methods, Java cannot find the native function and the call fails silently. Make sure to register **all** your native methods there.

</details>

<details>
<summary><b>🇧🇷 O System.loadLibrary precisa estar no Main.java?</b></summary>

Não necessariamente no `Main.java` — o importante é que esteja em um bloco `static` na classe que declara os métodos nativos, antes de qualquer chamada nativa. Na maioria dos projetos de mod menu, isso fica no `Main.java`, mas siga a estrutura do seu projeto.

</details>

<details>
<summary><b>🇺🇸 Does System.loadLibrary need to be in Main.java?</b></summary>

Not necessarily in `Main.java` — what matters is that it's in a `static` block in the class that declares the native methods, before any native call. In most mod menu projects this lives in `Main.java`, but follow your project's structure.

</details>

<details>
<summary><b>🇧🇷 O XOR é suficiente para proteger strings?</b></summary>

XOR é uma camada leve — não é criptografia forte. Um reverser experiente pode identificar o padrão. O valor real está em combinar XOR com AY_OBFUSCATE, dex2c e ProGuard. Quanto mais camadas, mais difícil e desmotivante fica o processo de reversão.

</details>

<details>
<summary><b>🇺🇸 Is XOR enough to protect strings?</b></summary>

XOR is a lightweight layer — not strong cryptography. An experienced reverser can identify the pattern. The real value is in combining XOR with AY_OBFUSCATE, dex2c, and ProGuard. The more layers, the harder and more discouraging the reversing process becomes.

</details>

<details>
<summary><b>🇧🇷 APKs antigos param de funcionar automaticamente?</b></summary>

Sim. Se a nativa exige o formato `"info|token1|token2"`, APKs que não enviam os tokens falham na separação e `g_keyOk` nunca é setado. Isso força update automático.

</details>

<details>
<summary><b>🇺🇸 Do old APKs stop working automatically?</b></summary>

Yes. If the native requires the format `"info|token1|token2"`, APKs that don't send the tokens fail at the split and `g_keyOk` is never set. This forces automatic updates.

</details>

<details>
<summary><b>🇧🇷 Posso usar outro banco de dados?</b></summary>

Sim! A lógica do servidor é independente. Você pode usar MySQL, SQLite, arquivos — o que importa é que o JSON de resposta contenha os campos que o Java espera.

</details>

<details>
<summary><b>🇺🇸 Can I use a different database?</b></summary>

Yes! The server logic is independent. You can use MySQL, SQLite, files — what matters is that the response JSON contains the fields Java expects.

</details>

---

<div align="center">

**VLDS — Verified License & Device System**  
Made with 🔒 by [@MrMoneys_ofc](https://t.me/MrMoneys_ofc)

*If this helped you, consider crediting the author!*  
*Se isso te ajudou, considere creditar o autor!*

</div>

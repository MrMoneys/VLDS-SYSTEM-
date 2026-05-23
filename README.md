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

**1. Java roda os anti-checks**  
Antes de qualquer coisa, o app verifica se o ambiente é seguro.

**2. Java envia o POST**  
O app envia a key e identificador do dispositivo para o servidor.

**3. Servidor valida a cadeia completa**  
Verifica blacklist → key → dispositivo → expiração → gera token HMAC.

**4. Token HMAC rotativo**  
O servidor gera um token que muda a cada hora. Dois tokens são retornados — hora atual e hora anterior — para evitar falhas na virada da hora.

**5. Java passa para a nativa**  
O Java extrai os campos do JSON e passa para um método nativo com nome ofuscado.

**6. C++ faz a validação final**  
A nativa separa os campos, valida o formato e só então seta `g_keyOk = true`.

**7. `g_keyOk` é o guardião**  
`CheckOverlayPermission` tem `if (!g_keyOk) return;` no topo — sem validação, nada abre.

</td>
<td width="50%">

### 🇺🇸 English

**1. Java runs anti-checks**  
Before anything, the app checks if the environment is safe.

**2. Java sends the POST**  
The app sends the key and device identifier to the server.

**3. Server validates the full chain**  
Checks blacklist → key → device → expiration → generates HMAC token.

**4. Rotating HMAC token**  
The server generates a token that changes every hour. Two tokens are returned — current and previous hour — to avoid failures at hour boundaries.

**5. Java passes to native**  
Java extracts the JSON fields and passes them to a native method with an obfuscated name.

**6. C++ does the final validation**  
The native splits the fields, validates the format, and only then sets `g_keyOk = true`.

**7. `g_keyOk` is the guardian**  
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

1. Rodar os anti-checks antes do request
2. Enviar o POST e receber o JSON
3. Passar os tokens para a nativa

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

1. Run anti-checks before the request
2. Send the POST and receive the JSON
3. Pass the tokens to native

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
// O JNI_OnLoad registra os métodos nativos.
// Sem isso, o Java não consegue chamar a nativa.

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
// JNI_OnLoad registers native methods.
// Without this, Java cannot call native methods.

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

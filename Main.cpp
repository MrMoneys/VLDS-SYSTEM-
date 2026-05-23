#include <jni.h>
#include <string>

// Flag global que serve como o guardião do Mod Menu
bool g_keyOk = false;

// Método de validação nativo
static void enviarTokensParaNativa(JNIEnv* env, jclass clazz, jstring jtoken) {
    if (!jtoken) return;

    // Converte jstring para std::string C++
    const char* nativeString = env->GetStringUTFChars(jtoken, nullptr);
    std::string full(nativeString);
    env->ReleaseStringUTFChars(jtoken, nativeString);

    // Separa os campos usando o delimitador '|'
    size_t sep1 = full.find('|');
    if (sep1 == std::string::npos) return;

    size_t sep2 = full.find('|', sep1 + 1);
    if (sep2 == std::string::npos) return;

    std::string info   = full.substr(0, sep1);
    std::string tkNow  = full.substr(sep1 + 1, sep2 - sep1 - 1);
    std::string tkPrev = full.substr(sep2 + 1);

    // Valida se a string de status está correta e se os tokens possuem tamanho de SHA256 (64 caracteres)
    // Dica: Use macros de ofuscação de string aqui no "valid" se sua base suportar
    bool validTk = (tkNow.length() == 64 || tkPrev.length() == 64);

    if (info == "valid" && validTk) {
        g_keyOk = true; // Sucesso: O menu está liberado para abrir
    } else {
        g_keyOk = false;
    }
}

// Exemplo de função que desenha ou gerencia a permissão do Menu
void CheckOverlayPermission(JNIEnv* env, jobject context) {
    // Se a validação falhou ou não rodou, o código encerra aqui e não abre a janela
    if (!g_keyOk) {
        return; 
    }
    
    // Lógica para abrir o Mod Menu/Overlay vai aqui...
}

/*
 * =================================================================
 * REGISTRO DINÂMICO JNI (Obrigatório)
 * =================================================================
 */
JNINativeMethod methods[] = {
    {
        "enviarTokensParaNativa", 
        "(Ljava/lang/String;)V", 
        (void*)enviarTokensParaNativa
    }
};

extern "C" JNIEXPORT jint JNICALL
JNI_OnLoad(JavaVM *vm, void *reserved) {
    JNIEnv *env;
    if (vm->GetEnv((void **) &env, JNI_VERSION_1_6) != JNI_OK) {
        return JNI_ERR;
    }

    // Localiza a classe Java onde o método nativo foi declarado
    jclass clazz = env->FindClass("com/exemplo/vlds/Main");
    if (clazz == nullptr) return JNI_ERR;

    // Registra os métodos vinculando o Java ao C++
    if (env->RegisterNatives(clazz, methods, sizeof(methods) / sizeof(methods[0])) < 0) {
        return JNI_ERR;
    }

    return JNI_VERSION_1_6;
}

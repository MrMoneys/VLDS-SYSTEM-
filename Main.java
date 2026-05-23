package com.exemplo.vlds;

import android.content.Context;
import android.os.Build;
import org.json.JSONObject;
import java.io.File;
import java.io.BufferedReader;
import java.io.InputStreamReader;

public class Main {

    // Declaração do método nativo (JNI)
    // Dica: No projeto final, altere esse nome para algo confuso (ex: lI1OIlII)
    private static native void enviarTokensParaNativa(String tokenData);

    /**
     * Executa todas as checagens de segurança e processa a resposta do servidor.
     * @param context Contexto da aplicação necessário para os checks.
     * @param jsonString Resposta em texto puro vinda do servidor PHP.
     */
    public static void verificarEProcessar(Context context, String jsonString) {
        
        // 1. Executa a cadeia de Camadas de Segurança (Anti-Checks)
        if (isRooted()) {
            enviarTokensParaNativa("banned_environment|root_detected");
            return;
        }

        if (isEmulator()) {
            enviarTokensParaNativa("banned_environment|emulator_detected");
            return;
        }

        if (isGameGuardianRunning(context)) {
            enviarTokensParaNativa("banned_environment|gg_detected");
            return;
        }

        // 2. Processamento do JSON se o ambiente estiver seguro
        try {
            JSONObject json = new JSONObject(jsonString);
            
            String result = json.optString("result", "fail");
            String info   = json.optString("info", "");
            String tokenA = json.optString("a", "");
            String tokenB = json.optString("b", "");

            boolean ok = result.equals("ok");

            // Se o servidor validou, concatena com o delimitador "|"
            if (ok && !tokenA.isEmpty()) {
                info = info + "|" + tokenA + "|" + tokenB;
            }

            // Envia o resultado final para o veredito do C++
            enviarTokensParaNativa(info);

        } catch (Exception e) {
            enviarTokensParaNativa("error|failed_to_parse");
        }
    }

    /*
     * =================================================================
     * CAMADA 1: ANTI-ROOT
     * =================================================================
     */
    private static boolean isRooted() {
        // Verifica caminhos comuns de binários do SU (SuperSU, Magisk, KernelSU)
        String[] paths = {
            "/system/app/Superuser.apk",
            "/sbin/su",
            "/system/bin/su",
            "/system/xbin/su",
            "/data/local/xbin/su",
            "/data/local/bin/su",
            "/system/sd/xbin/su",
            "/system/bin/failsafe/su",
            "/data/local/su"
        };
        for (String path : paths) {
            if (new File(path).exists()) return true;
        }

        // Executa o comando 'su' em background para ver se o binário responde
        Process process = null;
        try {
            process = Runtime.getRuntime().exec(new String[]{"/system/xbin/which", "su"});
            BufferedReader in = new BufferedReader(new InputStreamReader(process.getInputStream()));
            if (in.readLine() != null) return true;
        } catch (Throwable t) {
            // Ignora falhas de execução
        } finally {
            if (process != null) process.destroy();
        }
        return false;
    }

    /*
     * =================================================================
     * CAMADA 2: ANTI-EMULATOR
     * =================================================================
     */
    private static boolean isEmulator() {
        // Checagem baseada em propriedades conhecidas de emuladores (AVD, Genymotion, BlueStacks, Nox)
        return Build.FINGERPRINT.startsWith("generic")
                || Build.FINGERPRINT.startsWith("unknown")
                || Build.MODEL.contains("google_sdk")
                || Build.MODEL.contains("Emulator")
                || Build.MODEL.contains("Android SDK built for x86")
                || Build.BOARD.equals("QC_Reference_Phone")
                || Build.MANUFACTURER.contains("Genymotion")
                || Build.HOST.startsWith("Build") // Nox e outros emuladores de PC
                || (Build.BRAND.startsWith("generic") && Build.DEVICE.startsWith("generic"))
                || "google_sdk".equals(Build.PRODUCT);
    }

    /*
     * =================================================================
     * CAMADA 3: ANTI-GAME GUARDIAN (Anti-GG)
     * =================================================================
     */
    private static boolean isGameGuardianRunning(Context context) {
        try {
            // O Game Guardian frequentemente altera seu nome de pacote aleatoriamente para se esconder.
            // Contudo, seus diretórios virtuais e assinaturas de processos em execução no sistema 
            // costumam deixar rastros específicos de memória conhecidos como "catch_me".
            
            // Método Alternativo Rápido: Verificar processos contendo strings suspeitas no diretório /proc
            Process process = Runtime.getRuntime().exec("ps");
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                // Filtra por padrões comuns usados pelo daemon do Game Guardian (.gg ou nomes gerados)
                if (line.contains("com.gameguardian") || line.contains("daemon.gg")) {
                    return true;
                }
            }
        } catch (Exception e) {
            // Se falhar ao ler processos, continua de forma segura
        }
        return false;
    }
}

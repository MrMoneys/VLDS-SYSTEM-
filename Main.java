package com.exemplo.vlds;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.os.Build;
import org.json.JSONObject;
import java.io.File;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.security.MessageDigest;

public class Main {

    // INSIRA AQUI O HASH SHA-256 DA SUA ASSINATURA ORIGINAL DO APK (Letras maiúsculas)
    private static final String APP_SIGNATURE_SHA256 = "SUA_ASSINATURA_HEX_AQUI";
    private static final byte XOR_KEY = 0x5A;

    // Nome altamente ambíguo para dificultar ganchos (hooks) baseados em strings
    private static native void lI1OIlII(String tokenData);

    // Método auxiliar para ofuscar strings locais (XOR simples)
    private static String dec(int[] data) {
        char[] result = new char[data.length];
        for (int i = 0; i < data.length; i++) {
            result[i] = (char) (data[i] ^ XOR_KEY);
        }
        return new String(result);
    }

    public static void verificarEProcessar(Context context, String jsonString) {
        
        // Camada 0: Integridade do APK (Verifica se foi clonado/reassinado)
        if (!verificarAssinatura(context)) {
            lI1OIlII("compromised_integrity|signature_mismatch");
            return;
        }

        // Camada 1: Engenharia Reversa Dinâmica (Frida/Xposed)
        if (detectarFrida() || detectarXposed()) {
            lI1OIlII("compromised_integrity|reverse_tools");
            return;
        }

        // Camada 2: Ambiente (Root / Emulador)
        if (isRooted()) {
            lI1OIlII("banned_environment|root_detected");
            return;
        }

        if (isEmulator()) {
            lI1OIlII("banned_environment|emulator_detected");
            return;
        }

        if (isGameGuardianRunning(context)) {
            lI1OIlII("banned_environment|gg_detected");
            return;
        }

        // Camada 3: Validação dos Tokens
        try {
            JSONObject json = new JSONObject(jsonString);
            String result = json.optString("result", "fail");
            String info   = json.optString("info", "");
            String tokenA = json.optString("a", "");
            String tokenB = json.optString("b", "");

            if (result.equals("ok") && !tokenA.isEmpty()) {
                info = info + "|" + tokenA + "|" + tokenB;
            }

            lI1OIlII(info);
        } catch (Exception e) {
            lI1OIlII("error|failed_to_parse");
        }
    }

    private static boolean verificarAssinatura(Context context) {
        try {
            PackageInfo packageInfo = context.getPackageManager().getPackageInfo(
                context.getPackageName(), PackageManager.GET_SIGNATURES);
            for (Signature signature : packageInfo.signatures) {
                MessageDigest md = MessageDigest.getInstance("SHA-256");
                md.update(signature.toByteArray());
                byte[] digest = md.digest();
                StringBuilder sb = new StringBuilder();
                for (byte b : digest) {
                    sb.append(String.format("%02X", b));
                }
                return sb.toString().equals(APP_SIGNATURE_SHA256);
            }
        } catch (Exception e) {
            return false; // Se falhar ao ler, assume erro de integridade
        }
        return false;
    }

    private static boolean detectarFrida() {
        try {
            // Varre os mapas de memória em execução procurando rastros das bibliotecas do Frida
            BufferedReader br = new BufferedReader(new FileReader("/proc/self/maps"));
            String linha;
            while ((linha = br.readLine()) != null) {
                if (linha.contains("frida-agent") || linha.contains("gadget")) {
                    br.close();
                    return true;
                }
            }
            br.close();
        } catch (Exception e) {}
        return false;
    }

    private static boolean detectarXposed() {
        try {
            // Verifica se a stack trace contém chamadas nativas do ecossistema Xposed
            throw new Exception("DUMMY_EXCEPTION");
        } catch (Exception e) {
            for (StackTraceElement element : e.getStackTrace()) {
                if (element.getClassName().contains("de.robv.android.xposed")) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean isRooted() {
        // Exemplo de strings ofuscadas: "/sbin/su", "/system/bin/su", "/system/xbin/su"
        int[][] suPathsEncoded = {
            {0x75, 0x29, 0x32, 0x33, 0x34, 0x7F, 0x29, 0x2F}, // /sbin/su
            {0x75, 0x23, 0x23, 0x29, 0x3F, 0x37, 0x75, 0x3A, 0x33, 0x34, 0x75, 0x23, 0x2F}, // /system/bin/su
            {0x75, 0x23, 0x23, 0x29, 0x3F, 0x37, 0x75, 0x22, 0x3A, 0x33, 0x34, 0x75, 0x23, 0x2F} // /system/xbin/su
        };

        for (int[] encoded : suPathsEncoded) {
            if (new File(dec(encoded)).exists()) return true;
        }
        return false;
    }

    private static boolean isEmulator() {
        return Build.FINGERPRINT.startsWith("generic")
                || Build.FINGERPRINT.startsWith("unknown")
                || Build.MODEL.contains("google_sdk")
                || Build.MODEL.contains("Emulator")
                || Build.MODEL.contains("Android SDK built for x86")
                || Build.MANUFACTURER.contains("Genymotion")
                || Build.HARDWARE.contains("goldfish")
                || Build.HARDWARE.contains("ranchu");
    }

    private static boolean isGameGuardianRunning(Context context) {
        try {
            Process process = Runtime.getRuntime().exec("ps");
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            // "com.gameguardian" em XOR
            int[] ggPattern = {0x39, 0x35, 0x37, 0x74, 0x3D, 0x3B, 0x37, 0x3D, 0x2F, 0x3B, 0x3B, 0x28, 0x3E, 0x33, 0x34};
            String pattern = dec(ggPattern);
            
            while ((line = reader.readLine()) != null) {
                if (line.contains(pattern) || line.contains("daemon.gg")) {
                    return true;
                }
            }
            reader.close();
        } catch (Exception e) {}
        return false;
    }
}
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

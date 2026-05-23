package com.exemplo.vlds;

import org.json.JSONObject;

public class Main {

    // Declaração do método nativo (JNI)
    // Dica de segurança: Use nomes confusos como lI1OIlII no ambiente real
    private static native void enviarTokensParaNativa(String tokenData);

    /**
     * Processa a string JSON recebida do servidor PHP
     */
    public static void processarRespostaServidor(String jsonString) {
        try {
            JSONObject json = new JSONObject(jsonString);
            
            String result = json.optString("result", "fail");
            String info   = json.optString("info", "");
            String tokenA = json.optString("a", "");
            String tokenB = json.optString("b", "");

            boolean ok = result.equals("ok");

            // Se o servidor validou, concatena as informações com o delimitador "|"
            if (ok && !tokenA.isEmpty()) {
                info = info + "|" + tokenA + "|" + tokenB;
            }

            // Envia a string formatada para o C++ decidir o destino do app
            enviarTokensParaNativa(info);

        } catch (Exception e) {
            e.printStackTrace();
            enviarTokensParaNativa("error|failed_to_parse");
        }
    }
}

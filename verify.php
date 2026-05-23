<?php
header('Content-Type: application/json');

// Defina sua chave secreta longa (mantenha apenas no servidor)
define('HMAC_SECRET', 'SUA_CHAVE_SECRETA_SUPER_LONGA_E_CONFIAVEL_12345');

// Recebe os parâmetros do POST
$key      = $_POST['key']      ?? '';
$deviceId = $_POST['device']   ?? '';
$model    = $_POST['model']    ?? '';

// Validação básica de campos obrigatórios
if (empty($key) || empty($deviceId)) {
    echo json_encode(['result' => 'fail', 'info' => 'missing']);
    exit;
}

/*
 * =================================================================
 * ESPAÇO PARA SUA LÓGICA DE BANCO DE DADOS (Ex: MySQL / PDO)
 * =================================================================
 * - Verificar se a chave existe
 * - Verificar se o dispositivo está banido ou bate com o registrado
 * - Verificar expiração da licença
 */

// Simulando uma validação bem-sucedida para o exemplo:
$isValid = true; 
$username = "UsuarioExemplo";
$keyType = "premium";
$daysLeft = 30;

if (!$isValid) {
    echo json_encode(['result' => 'fail', 'info' => 'not_found']);
    exit;
}

// Geração dos tokens HMAC baseados no tempo (rotaciona a cada 2 dias)
$period = floor(time() / (86400 * 2));
$base   = hash('sha256', HMAC_SECRET . $period);

// Token da hora atual e da hora anterior (para evitar delays na virada de hora)
$token1 = hash_hmac('sha256', $key . $deviceId . date('YmdH'), $base);
$token2 = hash_hmac('sha256', $key . $deviceId . date('YmdH', time() - 3600), $base);

// Resposta JSON enviada para o aplicativo
echo json_encode([
    'result' => 'ok',
    'info'   => 'valid',
    'user'   => $username,
    'type'   => $keyType,
    'days'   => $daysLeft,
    'a'      => $token1, // Token atual
    'b'      => $token2  // Token anterior
]);

<?php
declare(strict_types=1);

header('Content-Type: application/json; charset=utf-8');
header('X-Content-Type-Options: nosniff');

$configPath = dirname(__DIR__) . '/config.php';
if (!is_file($configPath)) {
    respond(503, ['error' => ['code' => 'NOT_CONFIGURED', 'message' => 'Backend configuration is missing']]);
}
$config = require $configPath;
require_once dirname(__DIR__) . '/src/Security.php';
header('Access-Control-Allow-Origin: ' . ($config['allowed_origin'] ?? '*'));

$path = trim((string) parse_url($_SERVER['REQUEST_URI'] ?? '/', PHP_URL_PATH), '/');
$path = preg_replace('#^api/#', '', $path);

if ($path === 'health') {
    respond(200, ['status' => 'ok', 'service' => 'Indian Court Diary API']);
}
if ($path !== 'health') secure_request($config, 'case_lookup');

if (!preg_match('#^v1/cases/([A-Za-z]{4}[0-9]{12})$#', $path, $match)) {
    respond(404, ['error' => ['code' => 'NOT_FOUND', 'message' => 'Route not found']]);
}

$cnr = strtoupper($match[1]);
$cacheDir = (string) ($config['cache_dir'] ?? dirname(__DIR__) . '/storage/cache');
$cacheTtl = (int) ($config['cache_ttl_seconds'] ?? 21600);
if (!is_dir($cacheDir) && !mkdir($cacheDir, 0750, true) && !is_dir($cacheDir)) {
    respond(500, ['error' => ['code' => 'CACHE_ERROR', 'message' => 'Cache directory unavailable']]);
}
$cacheFile = $cacheDir . '/' . $cnr . '.json';
if (is_file($cacheFile) && time() - filemtime($cacheFile) < $cacheTtl) {
    header('X-Cache: HIT');
    readfile($cacheFile);
    exit;
}

$token = (string) ($config['ecourts_token'] ?? '');
if ($token === '' || $token === 'PASTE_TOKEN_ONLY_ON_SERVER') {
    respond(503, ['error' => ['code' => 'TOKEN_MISSING', 'message' => 'Provider token is not configured']]);
}

$curl = curl_init('https://webapi.ecourtsindia.com/api/partner/case/' . rawurlencode($cnr));
curl_setopt_array($curl, [
    CURLOPT_RETURNTRANSFER => true,
    CURLOPT_TIMEOUT => 20,
    CURLOPT_CONNECTTIMEOUT => 8,
    CURLOPT_HTTPHEADER => ['Authorization: Bearer ' . $token, 'Accept: application/json'],
]);
$body = curl_exec($curl);
$status = (int) curl_getinfo($curl, CURLINFO_HTTP_CODE);
$curlError = curl_error($curl);
curl_close($curl);

if ($body === false || $curlError !== '') {
    respond(502, ['error' => ['code' => 'PROVIDER_UNAVAILABLE', 'message' => 'Court service is temporarily unavailable']]);
}
if ($status < 200 || $status >= 300) {
    respond($status === 404 ? 404 : 502, ['error' => ['code' => 'PROVIDER_ERROR', 'message' => 'Case could not be retrieved']]);
}

$provider = json_decode($body, true);
$case = $provider['data']['courtCaseData'] ?? [];
$history = array_map(static function (array $h): array {
    return ['date'=>$h['hearingDate']??$h['date']??null,'purpose'=>$h['purposeOfHearing']??$h['businessOnDate']??null,'judge'=>$h['judge']??$h['judgeName']??null,'status'=>$h['stage']??$h['status']??null];
}, $case['historyOfCaseHearings'] ?? []);
$result = [
    'cnr' => $cnr,
    'caseTitle' => $case['caseTitle'] ?? $case['caseName'] ?? 'Court case',
    'courtName' => $case['courtName'] ?? 'Court unavailable',
    'nextHearingDate' => $case['nextHearingDate'] ?? null,
    'stage' => $case['stageOfCase'] ?? $case['caseStatus'] ?? 'Unknown',
    'hearingHistory' => $history,
];
$json = json_encode($result, JSON_UNESCAPED_UNICODE | JSON_UNESCAPED_SLASHES);
file_put_contents($cacheFile, $json, LOCK_EX);
header('X-Cache: MISS');
echo $json;

function respond(int $status, array $payload): never {
    http_response_code($status);
    echo json_encode($payload, JSON_UNESCAPED_UNICODE | JSON_UNESCAPED_SLASHES);
    exit;
}
